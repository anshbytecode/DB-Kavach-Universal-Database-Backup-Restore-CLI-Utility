package com.dbbackup.service.backup;

import com.dbbackup.dbms.DbmsAdapter;
import com.dbbackup.dbms.DbmsAdapterFactory;
import com.dbbackup.model.*;
import com.dbbackup.service.compression.CompressionService;
import com.dbbackup.service.logging.AuditLogService;
import com.dbbackup.service.notification.SlackNotificationService;
import com.dbbackup.service.security.DataMaskingService;
import com.dbbackup.service.security.EncryptionService;
import com.dbbackup.service.storage.StorageService;
import com.dbbackup.service.storage.StorageServiceFactory;
import com.dbbackup.util.ChecksumUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class BackupService {
    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    @Value("${backup.temp-dir:./temp-backups}")
    private String tempDir = "./temp-backups";

    private final DbmsAdapterFactory dbmsAdapterFactory;
    private final CompressionService compressionService;
    private final StorageServiceFactory storageServiceFactory;
    private final AuditLogService auditLogService;
    private final SlackNotificationService slackNotificationService;
    private final EncryptionService encryptionService;
    private final DataMaskingService dataMaskingService;
    private final ObjectMapper objectMapper;

    public BackupService(DbmsAdapterFactory dbmsAdapterFactory,
                         CompressionService compressionService,
                         StorageServiceFactory storageServiceFactory,
                         AuditLogService auditLogService,
                         SlackNotificationService slackNotificationService) {
        this(dbmsAdapterFactory, compressionService, storageServiceFactory, auditLogService, slackNotificationService, new EncryptionService(), new DataMaskingService());
    }

    @Autowired
    public BackupService(DbmsAdapterFactory dbmsAdapterFactory,
                          CompressionService compressionService,
                          StorageServiceFactory storageServiceFactory,
                          AuditLogService auditLogService,
                          SlackNotificationService slackNotificationService,
                          EncryptionService encryptionService,
                          DataMaskingService dataMaskingService) {
        this.dbmsAdapterFactory = dbmsAdapterFactory;
        this.compressionService = compressionService;
        this.storageServiceFactory = storageServiceFactory;
        this.auditLogService = auditLogService;
        this.slackNotificationService = slackNotificationService;
        this.encryptionService = encryptionService;
        this.dataMaskingService = dataMaskingService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public BackupMetadata executeBackup(BackupRequest request) throws Exception {
        String backupId = UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime startTime = LocalDateTime.now();
        String timestampStr = startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        DatabaseCredentials creds = request.getCredentials();
        DbmsAdapter adapter = dbmsAdapterFactory.getAdapter(creds.getDbmsType());
        StorageService storageService = storageServiceFactory.getStorageService(request.getStorageType());

        BackupHistoryRecord record = new BackupHistoryRecord();
        record.setBackupId(backupId);
        record.setOperation("BACKUP");
        record.setDbmsType(creds.getDbmsType());
        record.setDatabaseName(creds.getDatabaseName());
        record.setBackupType(request.getBackupType());
        record.setCompressionType(request.getCompressionType());
        record.setStorageType(request.getStorageType());
        record.setStartTime(startTime);

        log.info("Starting backup [{}] for DBMS: {}, Database: {}", backupId, creds.getDbmsType(), creds.getDatabaseName());

        File tempDirectory = new File(tempDir);
        if (!tempDirectory.exists()) {
            tempDirectory.mkdirs();
        }

        File rawOutputFile = new File(tempDirectory, "backup_" + creds.getDbmsType().name().toLowerCase() + "_" + creds.getDatabaseName() + "_" + timestampStr + ".raw");
        File maskedOutputFile = new File(tempDirectory, "backup_" + creds.getDbmsType().name().toLowerCase() + "_" + creds.getDatabaseName() + "_" + timestampStr + ".masked");
        File compressedOutputFile = new File(tempDirectory, "backup_" + creds.getDbmsType().name().toLowerCase() + "_" + creds.getDatabaseName() + "_" + timestampStr + request.getCompressionType().getExtension());
        File finalOutputFile = compressedOutputFile;

        try {
            // 1. Connection Test
            log.info("Validating database connection...");
            adapter.testConnection(creds);

            // 2. Perform DBMS Dump
            log.info("Executing database dump...");
            adapter.performBackup(request, rawOutputFile);

            File dumpSource = rawOutputFile;

            // 2b. Optional PII Data Masking
            if (request.isMaskPii()) {
                log.info("Masking PII sensitive data prior to compression...");
                dataMaskingService.maskDumpFile(rawOutputFile, maskedOutputFile);
                dumpSource = maskedOutputFile;
            }

            // 3. Compute Checksum & Metadata
            String checksum = ChecksumUtil.calculateSHA256(dumpSource);

            // 4. Compress
            log.info("Compressing backup output...");
            File finalCompressed = compressionService.compress(dumpSource, request.getCompressionType(), compressedOutputFile);
            finalOutputFile = finalCompressed;

            // 4b. Optional AES-256-GCM Encryption
            if (request.isEncrypted()) {
                if (request.getPassphrase() == null || request.getPassphrase().trim().isEmpty()) {
                    throw new IllegalArgumentException("Encryption passphrase must be specified when --encrypt is enabled.");
                }
                File encryptedFile = new File(tempDirectory, finalCompressed.getName() + ".enc");
                encryptionService.encryptFile(finalCompressed, encryptedFile, request.getPassphrase());
                finalOutputFile = encryptedFile;
            }

            // 5. Build Metadata Object
            BackupMetadata metadata = new BackupMetadata();
            metadata.setBackupId(backupId);
            metadata.setDbmsType(creds.getDbmsType());
            metadata.setDatabaseName(creds.getDatabaseName());
            metadata.setBackupType(request.getBackupType());
            metadata.setCompressionType(request.getCompressionType());
            metadata.setStorageType(request.getStorageType());
            metadata.setTimestamp(startTime);
            metadata.setSizeBytes(finalOutputFile.length());
            metadata.setSha256Checksum(checksum);
            metadata.setIncludedTables(request.getSelectiveTables() != null ? request.getSelectiveTables() : adapter.getTables(creds));

            // Write metadata JSON
            File metadataFile = new File(tempDirectory, "metadata_" + backupId + ".json");
            objectMapper.writeValue(metadataFile, metadata);

            // 6. Upload to Storage
            String storageKey = "backups/" + creds.getDbmsType().name().toLowerCase() + "/" + finalOutputFile.getName();
            String location = storageService.upload(finalOutputFile, storageKey);
            storageService.upload(metadataFile, "backups/" + creds.getDbmsType().name().toLowerCase() + "/" + metadataFile.getName());

            metadata.setStorageLocation(location);

            // 7. Cleanup temp files
            if (rawOutputFile.exists()) rawOutputFile.delete();
            if (maskedOutputFile.exists()) maskedOutputFile.delete();
            if (compressedOutputFile.exists()) compressedOutputFile.delete();
            if (finalOutputFile.exists() && !finalOutputFile.equals(compressedOutputFile)) finalOutputFile.delete();
            if (metadataFile.exists()) metadataFile.delete();

            // 8. Update Audit Record
            LocalDateTime endTime = LocalDateTime.now();
            record.setEndTime(endTime);
            record.setDurationMs(java.time.Duration.between(startTime, endTime).toMillis());
            record.setStatus("SUCCESS");
            record.setSizeBytes(metadata.getSizeBytes());
            record.setStorageLocation(location);

            auditLogService.recordActivity(record);
            slackNotificationService.sendNotification(record, request.getSlackWebhookUrl());

            log.info("Backup [{}] completed successfully in {}ms! Stored at: {}", backupId, record.getDurationMs(), location);
            return metadata;

        } catch (Exception e) {
            log.error("Backup [{}] failed: {}", backupId, e.getMessage(), e);

            LocalDateTime endTime = LocalDateTime.now();
            record.setEndTime(endTime);
            record.setDurationMs(java.time.Duration.between(startTime, endTime).toMillis());
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());

            auditLogService.recordActivity(record);
            slackNotificationService.sendNotification(record, request.getSlackWebhookUrl());

            // Clean up
            if (rawOutputFile.exists()) rawOutputFile.delete();
            if (maskedOutputFile.exists()) maskedOutputFile.delete();
            if (compressedOutputFile.exists()) compressedOutputFile.delete();
            if (finalOutputFile.exists()) finalOutputFile.delete();

            throw e;
        }
    }
}
