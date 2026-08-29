package com.dbbackup.service;

import com.dbbackup.dbms.DbmsAdapterFactory;
import com.dbbackup.dbms.SQLiteAdapter;
import com.dbbackup.model.*;
import com.dbbackup.service.backup.BackupService;
import com.dbbackup.service.compression.CompressionService;
import com.dbbackup.service.logging.AuditLogService;
import com.dbbackup.service.logging.BackupHistoryRepository;
import com.dbbackup.service.notification.SlackNotificationService;
import com.dbbackup.service.storage.LocalStorageService;
import com.dbbackup.service.storage.StorageServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {
    @Mock
    private BackupHistoryRepository repository;

    private BackupService backupService;

    @BeforeEach
    void setUp() {
        SQLiteAdapter sqLiteAdapter = new SQLiteAdapter();
        DbmsAdapterFactory dbmsAdapterFactory = new DbmsAdapterFactory(List.of(sqLiteAdapter));

        CompressionService compressionService = new CompressionService();

        LocalStorageService localStorageService = new LocalStorageService();
        
        ReflectionTestUtils.setField(localStorageService, "localStoragePath", "./target/test-backups");

        StorageServiceFactory storageServiceFactory = new StorageServiceFactory(List.of(localStorageService));

        AuditLogService auditLogService = new AuditLogService(repository);
        SlackNotificationService slackNotificationService = new SlackNotificationService();

        backupService = new BackupService(dbmsAdapterFactory, compressionService, storageServiceFactory, auditLogService, slackNotificationService);
    }

    @Test
    void testFullBackupExecution(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        File dbFile = tempDir.resolve("test_backup_db.db").toFile();

        DatabaseCredentials creds = new DatabaseCredentials();
        creds.setDbmsType(DbmsType.SQLITE);
        creds.setDatabaseName("test_backup_db");
        creds.setFilePath(dbFile.getAbsolutePath());

        BackupRequest request = new BackupRequest();
        
        request.setCredentials(creds);
        request.setBackupType(BackupType.FULL);
        request.setCompressionType(CompressionType.GZIP);
        request.setStorageType(StorageType.LOCAL);

        BackupMetadata metadata = backupService.executeBackup(request);

        assertNotNull(metadata);
        assertNotNull(metadata.getBackupId());
        assertEquals(DbmsType.SQLITE, metadata.getDbmsType());
        
        assertEquals(CompressionType.GZIP, metadata.getCompressionType());
        assertTrue(metadata.getSizeBytes() > 0);
        assertNotNull(metadata.getSha256Checksum());
    }
}
