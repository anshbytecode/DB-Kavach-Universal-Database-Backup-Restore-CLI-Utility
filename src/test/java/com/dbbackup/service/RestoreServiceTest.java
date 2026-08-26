package com.dbbackup.service;

import com.dbbackup.dbms.DbmsAdapterFactory;
import com.dbbackup.dbms.SQLiteAdapter;
import com.dbbackup.model.*;
import com.dbbackup.service.compression.CompressionService;
import com.dbbackup.service.logging.AuditLogService;
import com.dbbackup.service.logging.BackupHistoryRepository;
import com.dbbackup.service.notification.SlackNotificationService;
import com.dbbackup.service.restore.RestoreService;
import com.dbbackup.service.storage.LocalStorageService;
import com.dbbackup.service.storage.StorageServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestoreServiceTest {

    @Mock
    private BackupHistoryRepository repository;

    private RestoreService restoreService;

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

        restoreService = new RestoreService(dbmsAdapterFactory, compressionService, storageServiceFactory, auditLogService, slackNotificationService);
    }

    @Test
    void testRestoreExecution(@TempDir Path tempDir) throws Exception {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        File sourceSqlFile = tempDir.resolve("backup.sql").toFile();
        try (FileWriter writer = new FileWriter(sourceSqlFile)) {
            writer.write("CREATE TABLE sample (id INT, value TEXT);\nINSERT INTO sample VALUES (1, 'test');\n");
        }

        File targetDbFile = tempDir.resolve("target_restored.db").toFile();

        DatabaseCredentials creds = new DatabaseCredentials();
        creds.setDbmsType(DbmsType.SQLITE);
        creds.setDatabaseName("target_restored");
        creds.setFilePath(targetDbFile.getAbsolutePath());

        RestoreRequest request = new RestoreRequest();
        request.setTargetCredentials(creds);
        request.setBackupSourcePath(sourceSqlFile.getAbsolutePath());
        request.setSourceStorageType(StorageType.LOCAL);
        request.setCompressionType(CompressionType.NONE);

        boolean success = restoreService.executeRestore(request);
        assertTrue(success);
        assertTrue(targetDbFile.exists());
    }
}
