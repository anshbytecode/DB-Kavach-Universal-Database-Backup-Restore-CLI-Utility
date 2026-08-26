package com.dbbackup.service.restore;

import com.dbbackup.dbms.DbmsAdapter;
import com.dbbackup.dbms.DbmsAdapterFactory;
import com.dbbackup.model.*;
import com.dbbackup.service.compression.CompressionService;
import com.dbbackup.service.logging.AuditLogService;
import com.dbbackup.service.notification.SlackNotificationService;
import com.dbbackup.service.security.EncryptionService;
import com.dbbackup.service.storage.StorageService;
import com.dbbackup.service.storage.StorageServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RestoreService {
    private static final Logger log = LoggerFactory.getLogger(RestoreService.class);

    @Value("${backup.temp-dir:./temp-backups}")
    private String tempDir = "./temp-backups";

    private final DbmsAdapterFactory dbmsAdapterFactory;
    private final CompressionService compressionService;
    private final StorageServiceFactory storageServiceFactory;
    private final AuditLogService auditLogService;
    private final SlackNotificationService slackNotificationService;
    private final EncryptionService encryptionService;

    public RestoreService(DbmsAdapterFactory dbmsAdapterFactory,
                          CompressionService compressionService,
                          StorageServiceFactory storageServiceFactory,
                          AuditLogService auditLogService,
                          SlackNotificationService slackNotificationService) {
        this(dbmsAdapterFactory, compressionService, storageServiceFactory, auditLogService, slackNotificationService, new EncryptionService());
    }

    @Autowired
    public RestoreService(DbmsAdapterFactory dbmsAdapterFactory,
                          CompressionService compressionService,
                          StorageServiceFactory storageServiceFactory,
                          AuditLogService auditLogService,
                          SlackNotificationService slackNotificationService,
                          EncryptionService encryptionService) {
        this.dbmsAdapterFactory = dbmsAdapterFactory;
        this.compressionService = compressionService;
        this.storageServiceFactory = storageServiceFactory;
        this.auditLogService = auditLogService;
        this.slackNotificationService = slackNotificationService;
        this.encryptionService = encryptionService;
    }

    public boolean executeRestore(RestoreRequest request) throws Exception {
        String restoreId = UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime startTime = LocalDateTime.now();

        DatabaseCredentials creds = request.getTargetCredentials();
        DbmsAdapter adapter = dbmsAdapterFactory.getAdapter(creds.getDbmsType());
        StorageService storageService = storageServiceFactory.getStorageService(request.getSourceStorageType());

        BackupHistoryRecord record = new BackupHistoryRecord();
        record.setBackupId(restoreId);
        record.setOperation("RESTORE");
        record.setDbmsType(creds.getDbmsType());
        record.setDatabaseName(creds.getDatabaseName());
        record.setStorageType(request.getSourceStorageType());
        record.setStorageLocation(request.getBackupSourcePath());
        record.setStartTime(startTime);

        log.info("Starting restore [{}] for DBMS: {}, Database: {}, Source: {}", restoreId, creds.getDbmsType(), creds.getDatabaseName(), request.getBackupSourcePath());

        File tempDirectory = new File(tempDir);
        if (!tempDirectory.exists()) {
            tempDirectory.mkdirs();
        }

        File downloadedFile = new File(tempDirectory, "downloaded_" + restoreId + "_" + new File(request.getBackupSourcePath()).getName());
        File decryptedFile = new File(tempDirectory, "decrypted_" + restoreId + "_" + new File(request.getBackupSourcePath()).getName().replace(".enc", ""));
        File decompressedDir = new File(tempDirectory, "decompressed_" + restoreId);

        try {
            // 1. Connection Test
            log.info("Validating target database connection...");
            adapter.testConnection(creds);

            // 2. Fetch File from Storage
            File sourceFile;
            if (request.getSourceStorageType() == StorageType.LOCAL && new File(request.getBackupSourcePath()).exists()) {
                sourceFile = new File(request.getBackupSourcePath());
            } else {
                log.info("Downloading backup source file from {} storage...", request.getSourceStorageType());
                sourceFile = storageService.download(request.getBackupSourcePath(), downloadedFile);
            }

            File fileToDecompress = sourceFile;

            // 2b. Check if encrypted and decrypt if necessary
            boolean isEncrypted = encryptionService.isEncryptedFile(sourceFile) || sourceFile.getName().endsWith(".enc");
            if (isEncrypted) {
                if (request.getPassphrase() == null || request.getPassphrase().trim().isEmpty()) {
                    throw new IllegalArgumentException("Backup file is AES-encrypted. Decryption passphrase must be supplied via --passphrase.");
                }
                log.info("Backup file is encrypted. Performing AES-256-GCM decryption...");
                encryptionService.decryptFile(sourceFile, decryptedFile, request.getPassphrase());
                fileToDecompress = decryptedFile;
            }

            // 3. Decompress
            log.info("Decompressing backup source...");
            File uncompressedSource = compressionService.decompress(fileToDecompress, request.getCompressionType(), decompressedDir);

            // 4. Perform DBMS Restore
            log.info("Executing DBMS restore process...");
            adapter.performRestore(request, uncompressedSource);

            // 5. Cleanup temp files
            if (downloadedFile.exists()) downloadedFile.delete();
            if (decryptedFile.exists()) decryptedFile.delete();
            deleteDirectory(decompressedDir);

            // 6. Record Audit
            LocalDateTime endTime = LocalDateTime.now();
            record.setEndTime(endTime);
            record.setDurationMs(java.time.Duration.between(startTime, endTime).toMillis());
            record.setStatus("SUCCESS");
            record.setSizeBytes(sourceFile.length());

            auditLogService.recordActivity(record);
            slackNotificationService.sendNotification(record, null);

            log.info("Restore [{}] completed successfully in {}ms!", restoreId, record.getDurationMs());
            return true;

        } catch (Exception e) {
            log.error("Restore [{}] failed: {}", restoreId, e.getMessage(), e);

            LocalDateTime endTime = LocalDateTime.now();
            record.setEndTime(endTime);
            record.setDurationMs(java.time.Duration.between(startTime, endTime).toMillis());
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());

            auditLogService.recordActivity(record);
            slackNotificationService.sendNotification(record, null);

            // Cleanup
            if (downloadedFile.exists()) downloadedFile.delete();
            if (decryptedFile.exists()) decryptedFile.delete();
            deleteDirectory(decompressedDir);

            throw e;
        }
    }

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}
