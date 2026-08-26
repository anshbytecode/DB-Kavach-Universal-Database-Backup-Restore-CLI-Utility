package com.dbbackup.cli;

import com.dbbackup.model.*;
import com.dbbackup.service.restore.RestoreService;
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
    name = "restore",
    description = "Restore a database from a local file or cloud backup source with AES-256 decryption options.",
    mixinStandardHelpOptions = true
)
public class RestoreCommand implements Callable<Integer> {

    @Option(names = {"--dbms"}, description = "DBMS Type: MYSQL, POSTGRESQL, MONGODB, SQLITE")
    private String dbmsStr;

    @Option(names = {"-h", "--host"}, description = "Database Host", defaultValue = "localhost")
    private String host;

    @Option(names = {"-P", "--port"}, description = "Database Port", defaultValue = "0")
    private int port;

    @Option(names = {"-d", "--database"}, description = "Target Database name or SQLite file path")
    private String database;

    @Option(names = {"-u", "--username"}, description = "Database username")
    private String username;

    @Option(names = {"-p", "--password"}, description = "Database password", interactive = true, arity = "0..1")
    private String password;

    @Option(names = {"-f", "--file"}, required = true, description = "Backup file path or cloud key (e.g. ./backups/backup.sql.gz or s3://key)")
    private String sourcePath;

    @Option(names = {"--storage"}, description = "Source storage: LOCAL, S3, GCS, AZURE (default: LOCAL)", defaultValue = "LOCAL")
    private String storageStr;

    @Option(names = {"--compression"}, description = "Compression type: GZIP, ZIP, TAR_GZ, NONE (default: GZIP)", defaultValue = "GZIP")
    private String compressionStr;

    @Option(names = {"--passphrase"}, description = "Passphrase to decrypt AES-256 encrypted backup", interactive = true, arity = "0..1")
    private String passphrase;

    @Option(names = {"--profile"}, description = "Name of stored credential profile from vault")
    private String profileName;

    @Option(names = {"--vault-password"}, description = "Master password to unlock credential profile from vault", interactive = true, arity = "0..1")
    private String vaultPassword;

    @Option(names = {"--tables"}, description = "Comma-separated list of specific tables to restore")
    private String tablesStr;

    @Option(names = {"--dry-run"}, description = "Preview restore process without executing write operations")
    private boolean dryRun;

    private final RestoreService restoreService;
    private final CredentialVaultService vaultService;

    @Autowired
    public RestoreCommand(RestoreService restoreService, CredentialVaultService vaultService) {
        this.restoreService = restoreService;
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
                credentials.setFilePath(database);
            }

            StorageType storageType = StorageType.fromString(storageStr);
            CompressionType compressionType = CompressionType.fromString(compressionStr);

            RestoreRequest request = new RestoreRequest();
            request.setTargetCredentials(credentials);
            request.setBackupSourcePath(sourcePath);
            request.setSourceStorageType(storageType);
            request.setCompressionType(compressionType);
            request.setPassphrase(passphrase);
            request.setDryRun(dryRun);

            if (tablesStr != null && !tablesStr.trim().isEmpty()) {
                List<String> selectiveTables = Arrays.stream(tablesStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                request.setSelectiveTables(selectiveTables);
            }

            System.out.println("Initiating restore for " + credentials.getDbmsType() + " target [" + credentials.getDatabaseName() + "] from " + sourcePath + "...");
            restoreService.executeRestore(request);

            System.out.println("-------------------------------------------------------------------------");
            System.out.println("✅ RESTORE " + (dryRun ? "PREVIEW (DRY RUN) " : "") + "SUCCESSFUL!");
            System.out.println("-------------------------------------------------------------------------");
            return 0;

        } catch (Exception e) {
            System.err.println("❌ RESTORE FAILED: " + e.getMessage());
            return 1;
        }
    }
}
