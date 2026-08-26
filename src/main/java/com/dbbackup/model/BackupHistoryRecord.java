package com.dbbackup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "backup_history")
public class BackupHistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String backupId;
    private String operation; // BACKUP, RESTORE, TEST_CONNECTION

    @Enumerated(EnumType.STRING)
    private DbmsType dbmsType;

    private String databaseName;

    @Enumerated(EnumType.STRING)
    private BackupType backupType;

    @Enumerated(EnumType.STRING)
    private CompressionType compressionType;

    @Enumerated(EnumType.STRING)
    private StorageType storageType;

    private String storageLocation;
    private String status; // SUCCESS, FAILED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;
    private long sizeBytes;

    @Column(length = 2000)
    private String errorMessage;

    public BackupHistoryRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBackupId() { return backupId; }
    public void setBackupId(String backupId) { this.backupId = backupId; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
