package com.dbbackup.dbms;

import com.dbbackup.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DbmsAdapterTest {

    private SQLiteAdapter sqliteAdapter;

    @BeforeEach
    void setUp() {
        sqliteAdapter = new SQLiteAdapter();
    }

    @Test
    void testSQLiteAdapterLifecycle(@TempDir Path tempDir) throws Exception {
        File dbFile = tempDir.resolve("test_db.db").toFile();

        DatabaseCredentials creds = new DatabaseCredentials();
        creds.setDbmsType(DbmsType.SQLITE);
        creds.setDatabaseName("test_db");
        creds.setFilePath(dbFile.getAbsolutePath());

        // Test connection
        boolean connected = sqliteAdapter.testConnection(creds);
        assertTrue(connected, "SQLite connection test should succeed");

        // Test backup
        BackupRequest backupRequest = new BackupRequest();
        backupRequest.setCredentials(creds);
        backupRequest.setBackupType(BackupType.FULL);

        File rawBackupFile = tempDir.resolve("backup_output.raw").toFile();
        File result = sqliteAdapter.performBackup(backupRequest, rawBackupFile);

        assertTrue(result.exists());
        assertTrue(result.length() > 0);

        // Get tables
        List<String> tables = sqliteAdapter.getTables(creds);
        assertNotNull(tables);

        // Test Restore
        File restoredDbFile = tempDir.resolve("restored_db.db").toFile();
        DatabaseCredentials restoreCreds = new DatabaseCredentials();
        restoreCreds.setDbmsType(DbmsType.SQLITE);
        restoreCreds.setFilePath(restoredDbFile.getAbsolutePath());

        RestoreRequest restoreRequest = new RestoreRequest();
        restoreRequest.setTargetCredentials(restoreCreds);

        sqliteAdapter.performRestore(restoreRequest, result);
        assertTrue(restoredDbFile.exists());
    }
}
