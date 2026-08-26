package com.dbbackup.model;

import java.time.LocalDateTime;
import java.util.List;

public class BackupMetadata {
    private String backupId;
    private DbmsType dbmsType;
    private String databaseName;
    private BackupType backupType;
    private CompressionType compressionType;
    private StorageType storageType;
    private String storageLocation;
    private LocalDateTime timestamp;
    private long sizeBytes;
    private String sha256Checksum;
    private List<String> includedTables;

    public BackupMetadata() {}

    public String getBackupId() { return backupId; }
    public void setBackupId(String backupId) { this.backupId = backupId; }

    public DbmsType getDbmsType() { return dbmsType; }
    public void setDbmsType(DbmsType dbmsType) { this.dbmsType = dbmsType; }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public BackupType getBackupType() { return backupType; }
    public void setBackupType(BackupType backupType) { this.backupType = backupType; }

    public CompressionType getCompressionType() { return compressionType; }
    public void setCompressionType(CompressionType compressionType) { this.compressionType = compressionType; }

    public StorageType getStorageType() { return storageType; }
    public void setStorageType(StorageType storageType) { this.storageType = storageType; }

    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getSha256Checksum() { return sha256Checksum; }
    public void setSha256Checksum(String sha256Checksum) { this.sha256Checksum = sha256Checksum; }

    public List<String> getIncludedTables() { return includedTables; }
    public void setIncludedTables(List<String> includedTables) { this.includedTables = includedTables; }
}
