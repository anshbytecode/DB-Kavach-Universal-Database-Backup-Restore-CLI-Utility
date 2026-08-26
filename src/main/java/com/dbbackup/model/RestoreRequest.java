package com.dbbackup.model;

import java.util.List;

public class RestoreRequest {
    private DatabaseCredentials targetCredentials;
    private String backupSourcePath; // File path or Cloud Key
    private StorageType sourceStorageType = StorageType.LOCAL;
    private CompressionType compressionType = CompressionType.GZIP;
    private List<String> selectiveTables;
    private boolean dryRun = false;
    private String passphrase;

    public RestoreRequest() {}

    public DatabaseCredentials getTargetCredentials() { return targetCredentials; }
    public void setTargetCredentials(DatabaseCredentials targetCredentials) { this.targetCredentials = targetCredentials; }

    public String getBackupSourcePath() { return backupSourcePath; }
    public void setBackupSourcePath(String backupSourcePath) { this.backupSourcePath = backupSourcePath; }

    public StorageType getSourceStorageType() { return sourceStorageType; }
    public void setSourceStorageType(StorageType sourceStorageType) { this.sourceStorageType = sourceStorageType; }

    public CompressionType getCompressionType() { return compressionType; }
    public void setCompressionType(CompressionType compressionType) { this.compressionType = compressionType; }

    public List<String> getSelectiveTables() { return selectiveTables; }
    public void setSelectiveTables(List<String> selectiveTables) { this.selectiveTables = selectiveTables; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public String getPassphrase() { return passphrase; }
    public void setPassphrase(String passphrase) { this.passphrase = passphrase; }
}
