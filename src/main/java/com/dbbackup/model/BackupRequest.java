package com.dbbackup.model;

import java.util.List;

public class BackupRequest {
    private DatabaseCredentials credentials;
    private BackupType backupType = BackupType.FULL;
    private CompressionType compressionType = CompressionType.GZIP;
    private StorageType storageType = StorageType.LOCAL;
    private String outputDir;
    private List<String> selectiveTables;
    private String slackWebhookUrl;
    private String customTag;
    private boolean encrypted = false;
    private String passphrase;
    private boolean maskPii = false;

    public BackupRequest() {}

    public DatabaseCredentials getCredentials() { return credentials; }
    public void setCredentials(DatabaseCredentials credentials) { this.credentials = credentials; }

    public BackupType getBackupType() { return backupType; }
    public void setBackupType(BackupType backupType) { this.backupType = backupType; }

    public CompressionType getCompressionType() { return compressionType; }
    public void setCompressionType(CompressionType compressionType) { this.compressionType = compressionType; }

    public StorageType getStorageType() { return storageType; }
    public void setStorageType(StorageType storageType) { this.storageType = storageType; }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

    public List<String> getSelectiveTables() { return selectiveTables; }
    public void setSelectiveTables(List<String> selectiveTables) { this.selectiveTables = selectiveTables; }

    public String getSlackWebhookUrl() { return slackWebhookUrl; }
    public void setSlackWebhookUrl(String slackWebhookUrl) { this.slackWebhookUrl = slackWebhookUrl; }

    public String getCustomTag() { return customTag; }
    public void setCustomTag(String customTag) { this.customTag = customTag; }

    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }

    public String getPassphrase() { return passphrase; }
    public void setPassphrase(String passphrase) { this.passphrase = passphrase; }

    public boolean isMaskPii() { return maskPii; }
    public void setMaskPii(boolean maskPii) { this.maskPii = maskPii; }
}
