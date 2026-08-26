package com.dbbackup.dbms;

import com.dbbackup.model.DatabaseCredentials;
import com.dbbackup.model.DbmsType;
import com.dbbackup.model.BackupRequest;
import com.dbbackup.model.RestoreRequest;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface DbmsAdapter {
    DbmsType getType();
    boolean testConnection(DatabaseCredentials credentials) throws Exception;
    List<String> getTables(DatabaseCredentials credentials) throws Exception;
    File performBackup(BackupRequest request, File rawOutputFile) throws Exception;
    void performRestore(RestoreRequest request, File uncompressedSourceFile) throws Exception;
}
