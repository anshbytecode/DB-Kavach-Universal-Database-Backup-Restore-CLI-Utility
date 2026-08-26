package com.dbbackup.cli;

import com.dbbackup.model.*;
import com.dbbackup.service.backup.BackupService;
import com.dbbackup.service.security.CredentialVaultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(
    name = "backup",
    description = "Back up a database with options for DBMS, compression, storage, AES encryption, and PII masking.",
    mixinStandardHelpOptions = true
)
public class BackupCommand implements Callable<Integer> {

    @Option(names = {"--dbms"}, description = "DBMS Type: MYSQL, POSTGRESQL, MONGODB, SQLITE")
    private String dbmsStr;

    @Option(names = {"-h", "--host"}, description = "Database Host (default: localhost)", defaultValue = "localhost")
    private String host;

    @Option(names = {"-P", "--port"}, description = "Database Port (default: 3306 for MySQL, 5432 for Postgres, 27017 for Mongo)", defaultValue = "0")
    private int port;

    @Option(names = {"-d", "--database"}, description = "Database name or SQLite file path")
    private String database;

    @Option(names = {"-u", "--username"}, description = "Database username")
    private String username;

    @Option(names = {"-p", "--password"}, description = "Database password", interactive = true, arity = "0..1")
    private String password;

    @Option(names = {"--uri"}, description = "Database connection URI string")
    private String uri;

    @Option(names = {"--file-path"}, description = "File path for SQLite database")
    private String filePath;

    @Option(names = {"--type"}, description = "Backup type: FULL, INCREMENTAL, DIFFERENTIAL (default: FULL)", defaultValue = "FULL")
    private String backupTypeStr;

    @Option(names = {"--compression"}, description = "Compression type: GZIP, ZIP, TAR_GZ, NONE (default: GZIP)", defaultValue = "GZIP")
    private String compressionStr;

    @Option(names = {"--storage"}, description = "Storage destination: LOCAL, S3, GCS, AZURE (default: LOCAL)", defaultValue = "LOCAL")
    private String storageStr;

    @Option(names = {"--slack-webhook"}, description = "Optional Slack webhook URL for notifications")
    private String slackWebhookUrl;

    @Option(names = {"--tables"}, description = "Comma-separated list of specific tables/collections to back up (selective restore/backup)")
    private String tablesStr;

    @Option(names = {"--encrypt"}, description = "Encrypt backup output using AES-256-GCM authenticated encryption")
    private boolean encrypt = false;

    @Option(names = {"--passphrase"}, description = "Passphrase for AES-256 backup encryption", interactive = true, arity = "0..1")
    private String passphrase;

    @Option(names = {"--mask-pii"}, description = "Sanitize personally identifiable information (PII) before storage")
    private boolean maskPii = false;

    @Option(names = {"--profile"}, description = "Name of stored credential profile from vault")
    private String profileName;

    @Option(names = {"--vault-password"}, description = "Master password to unlock credential profile from vault", interactive = true, arity = "0..1")
    private String vaultPassword;

    private final BackupService backupService;
    private final CredentialVaultService vaultService;

    @Autowired
    public BackupCommand(BackupService backupService, CredentialVaultService vaultService) {
        this.backupService = backupService;
        this.vaultService = vaultService;
    }

    @Override
    public Integer call() {
        try {
            DatabaseCredentials credentials = new DatabaseCredentials();

            if (profileName != null && !profileName.trim().isEmpty()) {
                if (vaultPassword == null || vaultPassword.trim().isEmpty()) {
                    System.err.println("❌ Vault master password (--vault-password) is required to unlock profile [" + profileName + "].");
                    return 1;
                }
                CredentialProfile profile = vaultService.getProfile(vaultPassword, profileName);
                credentials = profile.toDatabaseCredentials();
            } else {
                if (dbmsStr == null || database == null) {
                    System.err.println("❌ Parameters --dbms and --database are required when --profile is not specified.");
                    return 1;
                }
                DbmsType dbmsType = DbmsType.fromString(dbmsStr);
                int effectivePort = port;
                if (effectivePort == 0) {
                    effectivePort = switch (dbmsType) {
                        case MYSQL -> 3306;
                        case POSTGRESQL -> 5432;
                        case MONGODB -> 27017;
                        case SQLITE -> 0;
                    };
                }
                credentials.setDbmsType(dbmsType);
                credentials.setHost(host);
                credentials.setPort(effectivePort);
                credentials.setDatabaseName(database);
                credentials.setUsername(username);
                credentials.setPassword(password);
                credentials.setConnectionUri(uri);
                credentials.setFilePath(filePath != null ? filePath : database);
            }

            BackupType backupType = BackupType.fromString(backupTypeStr);
            CompressionType compressionType = CompressionType.fromString(compressionStr);
            StorageType storageType = StorageType.fromString(storageStr);

            BackupRequest request = new BackupRequest();
            request.setCredentials(credentials);
            request.setBackupType(backupType);
            request.setCompressionType(compressionType);
            request.setStorageType(storageType);
            request.setSlackWebhookUrl(slackWebhookUrl);
            request.setEncrypted(encrypt);
            request.setPassphrase(passphrase);
            request.setMaskPii(maskPii);

            if (tablesStr != null && !tablesStr.trim().isEmpty()) {
                List<String> selectiveTables = Arrays.stream(tablesStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                request.setSelectiveTables(selectiveTables);
            }

            System.out.println("Initiating database backup for " + credentials.getDbmsType() + " [" + credentials.getDatabaseName() + "]...");
            if (maskPii) System.out.println("🔒 PII Data Sanitization enabled.");
            if (encrypt) System.out.println("🔒 AES-256-GCM Encryption enabled.");

            BackupMetadata metadata = backupService.executeBackup(request);

            System.out.println("-------------------------------------------------------------------------");
            System.out.println("✅ BACKUP SUCCESSFUL!");
            System.out.println("Backup ID        : " + metadata.getBackupId());
            System.out.println("DBMS             : " + metadata.getDbmsType());
            System.out.println("Database         : " + metadata.getDatabaseName());
            System.out.println("Backup Type      : " + metadata.getBackupType());
            System.out.println("Compression      : " + metadata.getCompressionType());
            System.out.println("Encrypted AES-256: " + (encrypt ? "YES" : "NO"));
            System.out.println("PII Data Masked  : " + (maskPii ? "YES" : "NO"));
            System.out.println("Storage Location : " + metadata.getStorageLocation());
            System.out.println("Size             : " + metadata.getSizeBytes() + " bytes");
            System.out.println("SHA-256 Checksum : " + metadata.getSha256Checksum());
            System.out.println("-------------------------------------------------------------------------");
            return 0;

        } catch (Exception e) {
            System.err.println("❌ BACKUP FAILED: " + e.getMessage());
            return 1;
        }
    }
}
