package com.dbbackup.cli;

import com.dbbackup.model.*;
import com.dbbackup.service.security.CredentialVaultService;
import com.dbbackup.service.security.DataMaskingService;
import com.dbbackup.service.security.EncryptionService;
import com.dbbackup.service.security.SecurityAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(
    name = "security",
    description = "Database Security Management Suite (Security Audit, AES-256 Encryption/Decryption, PII Masking, Credential Vault).",
    mixinStandardHelpOptions = true,
    subcommands = {
        SecurityCommand.AuditSubcommand.class,
        SecurityCommand.EncryptSubcommand.class,
        SecurityCommand.DecryptSubcommand.class,
        SecurityCommand.MaskSubcommand.class,
        SecurityCommand.VaultSubcommand.class
    }
)
public class SecurityCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("=========================================================================");
        System.out.println("                   DATABASE SECURITY MANAGEMENT SUITE                   ");
        System.out.println("=========================================================================");
        System.out.println("Usage: db-backup security [SUBCOMMAND] [OPTIONS]");
        System.out.println();
        System.out.println("Available Security Subcommands:");
        System.out.println("  audit    - Perform security vulnerability & compliance scan on target database");
        System.out.println("  encrypt  - Encrypt a backup or database file using AES-256-GCM");
        System.out.println("  decrypt  - Decrypt an AES-256-GCM encrypted backup file");
        System.out.println("  mask     - Sanitize and mask sensitive PII data in SQL/JSON database dumps");
        System.out.println("  vault    - Manage encrypted database connection profiles in secure vault");
        System.out.println("=========================================================================");
        System.out.println("Type 'db-backup security <subcommand> --help' for specific command details.");
        return 0;
    }

    // -------------------------------------------------------------------------
    // Subcommand: audit
    // -------------------------------------------------------------------------
    @Component
    @Command(
        name = "audit",
        description = "Perform a comprehensive database security compliance and vulnerability audit.",
        mixinStandardHelpOptions = true
    )
    public static class AuditSubcommand implements Callable<Integer> {

        @Option(names = {"--dbms"}, required = true, description = "DBMS Type: MYSQL, POSTGRESQL, MONGODB, SQLITE")
        private String dbmsStr;

        @Option(names = {"-h", "--host"}, description = "Database Host", defaultValue = "localhost")
        private String host;

        @Option(names = {"-P", "--port"}, description = "Database Port", defaultValue = "0")
        private int port;

        @Option(names = {"-d", "--database"}, required = true, description = "Database name or SQLite file path")
        private String database;

        @Option(names = {"-u", "--username"}, description = "Database username")
        private String username;

        @Option(names = {"-p", "--password"}, description = "Database password", interactive = true, arity = "0..1")
        private String password;

        @Option(names = {"--uri"}, description = "Database connection URI")
        private String uri;

        @Option(names = {"--backup-dir"}, description = "Local backup directory to audit for permissions", defaultValue = "./temp-backups")
        private String backupDir;

        private final SecurityAuditService auditService;

        @Autowired
        public AuditSubcommand(SecurityAuditService auditService) {
            this.auditService = auditService;
        }

        @Override
        public Integer call() {
            try {
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

                DatabaseCredentials creds = new DatabaseCredentials();
                creds.setDbmsType(dbmsType);
                creds.setHost(host);
                creds.setPort(effectivePort);
                creds.setDatabaseName(database);
                creds.setUsername(username);
                creds.setPassword(password);
                creds.setConnectionUri(uri);

                SecurityAuditReport report = auditService.performSecurityAudit(creds, backupDir);

                System.out.println("=========================================================================");
                System.out.println("                     DATABASE SECURITY AUDIT REPORT                      ");
                System.out.println("=========================================================================");
                System.out.println("DBMS Target     : " + report.getDbmsType() + " [" + report.getDatabaseName() + "]");
                System.out.println("Host:Port       : " + report.getHost() + ":" + report.getPort());
                System.out.println("Audit Timestamp : " + report.getTimestamp());
                System.out.println("-------------------------------------------------------------------------");
                System.out.println("Security Score  : " + report.getScore() + " / 100");
                System.out.println("Security Rating : [" + report.getRating() + "]");
                System.out.println("-------------------------------------------------------------------------");
                System.out.println("Audit Findings & Security Vulnerabilities:");
                if (report.getFindings().isEmpty()) {
                    System.out.println("  ✅ No security vulnerabilities detected!");
                } else {
                    for (int i = 0; i < report.getFindings().size(); i++) {
                        SecurityAuditReport.AuditFinding finding = report.getFindings().get(i);
                        System.out.println("  [" + (i + 1) + "] Severity: " + finding.getSeverity() + " | Category: " + finding.getCategory());
                        System.out.println("      Title: " + finding.getTitle());
                        System.out.println("      Issue: " + finding.getDescription());
                        System.out.println("      Remediation: " + finding.getRecommendation());
                        System.out.println();
                    }
                }
                System.out.println("=========================================================================");
                return 0;

            } catch (Exception e) {
                System.err.println("❌ Security audit failed: " + e.getMessage());
                return 1;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Subcommand: encrypt
    // -------------------------------------------------------------------------
    @Component
    @Command(
        name = "encrypt",
        description = "Encrypt a file using AES-256-GCM and a secure passphrase.",
        mixinStandardHelpOptions = true
    )
    public static class EncryptSubcommand implements Callable<Integer> {

        @Option(names = {"-f", "--file"}, required = true, description = "Target file path to encrypt")
        private String filePath;

        @Option(names = {"-p", "--passphrase"}, required = true, description = "Passphrase for encryption key derivation", interactive = true, arity = "0..1")
        private String passphrase;

        @Option(names = {"-o", "--out"}, description = "Output file path (default: <file>.enc)")
        private String outputPath;

        private final EncryptionService encryptionService;

        @Autowired
        public EncryptSubcommand(EncryptionService encryptionService) {
            this.encryptionService = encryptionService;
        }

        @Override
        public Integer call() {
            try {
                File inputFile = new File(filePath);
                if (!inputFile.exists()) {
                    System.err.println("❌ Input file not found: " + filePath);
                    return 1;
                }

                File outputFile = (outputPath != null) ? new File(outputPath) : new File(filePath + ".enc");
                encryptionService.encryptFile(inputFile, outputFile, passphrase);

                System.out.println("✅ AES-256-GCM Encryption Successful!");
                System.out.println("Original File : " + inputFile.getAbsolutePath());
                System.out.println("Encrypted File: " + outputFile.getAbsolutePath());
                System.out.println("Encrypted Size: " + outputFile.length() + " bytes");
                return 0;

            } catch (Exception e) {
                System.err.println("❌ Encryption failed: " + e.getMessage());
                return 1;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Subcommand: decrypt
    // -------------------------------------------------------------------------
    @Component
    @Command(
        name = "decrypt",
        description = "Decrypt an AES-256-GCM encrypted backup file.",
        mixinStandardHelpOptions = true
    )
    public static class DecryptSubcommand implements Callable<Integer> {

        @Option(names = {"-f", "--file"}, required = true, description = "Encrypted file path to decrypt")
        private String filePath;

        @Option(names = {"-p", "--passphrase"}, required = true, description = "Passphrase for decryption", interactive = true, arity = "0..1")
        private String passphrase;

        @Option(names = {"-o", "--out"}, description = "Output decrypted file path")
        private String outputPath;

        private final EncryptionService encryptionService;

        @Autowired
        public DecryptSubcommand(EncryptionService encryptionService) {
            this.encryptionService = encryptionService;
        }

        @Override
        public Integer call() {
            try {
                File inputFile = new File(filePath);
                if (!inputFile.exists()) {
                    System.err.println("❌ Encrypted file not found: " + filePath);
                    return 1;
                }

                File outputFile;
                if (outputPath != null) {
                    outputFile = new File(outputPath);
                } else if (filePath.endsWith(".enc")) {
                    outputFile = new File(filePath.substring(0, filePath.length() - 4));
                } else {
                    outputFile = new File(filePath + ".dec");
                }

                encryptionService.decryptFile(inputFile, outputFile, passphrase);

                System.out.println("✅ AES-256-GCM Decryption Successful!");
                System.out.println("Encrypted Source: " + inputFile.getAbsolutePath());
                System.out.println("Decrypted File  : " + outputFile.getAbsolutePath());
                System.out.println("Decrypted Size  : " + outputFile.length() + " bytes");
                return 0;

            } catch (Exception e) {
                System.err.println("❌ Decryption failed: " + e.getMessage());
                return 1;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Subcommand: mask
    // -------------------------------------------------------------------------
    @Component
    @Command(
        name = "mask",
        description = "Sanitize and mask sensitive PII data (emails, credit cards, SSNs, passwords) in database dump files.",
        mixinStandardHelpOptions = true
    )
    public static class MaskSubcommand implements Callable<Integer> {

        @Option(names = {"-f", "--file"}, required = true, description = "Target database dump file (SQL/JSON)")
        private String filePath;

        @Option(names = {"-o", "--out"}, description = "Output file path for masked dump (default: <file>.masked)")
        private String outputPath;

        private final DataMaskingService maskingService;

        @Autowired
        public MaskSubcommand(DataMaskingService maskingService) {
            this.maskingService = maskingService;
        }

        @Override
        public Integer call() {
            try {
                File inputFile = new File(filePath);
                if (!inputFile.exists()) {
                    System.err.println("❌ Dump file not found: " + filePath);
                    return 1;
                }

                File outputFile = (outputPath != null) ? new File(outputPath) : new File(filePath + ".masked");
                maskingService.maskDumpFile(inputFile, outputFile);

                System.out.println("✅ Data Masking & PII Sanitization Successful!");
                System.out.println("Raw Dump File   : " + inputFile.getAbsolutePath());
                System.out.println("Sanitized Output: " + outputFile.getAbsolutePath());
                return 0;

            } catch (Exception e) {
                System.err.println("❌ Data masking failed: " + e.getMessage());
                return 1;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Subcommand: vault
    // -------------------------------------------------------------------------
    @Component
    @Command(
        name = "vault",
        description = "Manage database connection profiles securely inside an encrypted vault store.",
        mixinStandardHelpOptions = true,
        subcommands = {
            VaultSaveSubcommand.class,
            VaultListSubcommand.class,
            VaultGetSubcommand.class,
            VaultRemoveSubcommand.class
        }
    )
    public static class VaultSubcommand implements Callable<Integer> {
        @Override
        public Integer call() {
            System.out.println("Usage: db-backup security vault [save|list|get|remove] --master-password=<pass>");
            return 0;
        }
    }

    @Component
    @Command(name = "save", description = "Save a database profile to the encrypted vault.")
    public static class VaultSaveSubcommand implements Callable<Integer> {
        @Option(names = {"--master-password"}, required = true, interactive = true, arity = "0..1", description = "Vault master password")
        private String masterPassword;

        @Option(names = {"--name"}, required = true, description = "Profile name (e.g. prod-db)")
        private String name;

        @Option(names = {"--dbms"}, required = true, description = "DBMS type (MYSQL, POSTGRESQL, MONGODB, SQLITE)")
        private String dbmsStr;

        @Option(names = {"-h", "--host"}, defaultValue = "localhost", description = "Host")
        private String host;

        @Option(names = {"-P", "--port"}, defaultValue = "0", description = "Port")
        private int port;

        @Option(names = {"-d", "--database"}, required = true, description = "Database name or file path")
        private String database;

        @Option(names = {"-u", "--username"}, description = "Username")
        private String username;

        @Option(names = {"-p", "--password"}, interactive = true, arity = "0..1", description = "Password")
        private String password;

        @Option(names = {"--uri"}, description = "Connection URI")
        private String uri;

        private final CredentialVaultService vaultService;

        @Autowired
        public VaultSaveSubcommand(CredentialVaultService vaultService) {
            this.vaultService = vaultService;
        }

        @Override
        public Integer call() {
            try {
                DbmsType dbmsType = DbmsType.fromString(dbmsStr);
                CredentialProfile profile = new CredentialProfile();
                profile.setProfileName(name);
                profile.setDbmsType(dbmsType);
                profile.setHost(host);
                profile.setPort(port);
                profile.setDatabaseName(database);
                profile.setUsername(username);
                profile.setPassword(password);
                profile.setConnectionUri(uri);

                vaultService.saveProfile(masterPassword, profile);
                System.out.println("✅ Saved profile [" + name + "] to encrypted credential vault.");
                return 0;
            } catch (Exception e) {
                System.err.println("❌ Vault save failed: " + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "list", description = "List stored profiles in vault.")
    public static class VaultListSubcommand implements Callable<Integer> {
        @Option(names = {"--master-password"}, required = true, interactive = true, arity = "0..1", description = "Vault master password")
        private String masterPassword;

        private final CredentialVaultService vaultService;

        @Autowired
        public VaultListSubcommand(CredentialVaultService vaultService) {
            this.vaultService = vaultService;
        }

        @Override
        public Integer call() {
            try {
                List<String> profiles = vaultService.listProfiles(masterPassword);
                System.out.println("Stored Vault Profiles (" + profiles.size() + "):");
                for (String p : profiles) {
                    System.out.println(" - " + p);
                }
                return 0;
            } catch (Exception e) {
                System.err.println("❌ Vault list failed: " + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "get", description = "Get profile details from vault.")
    public static class VaultGetSubcommand implements Callable<Integer> {
        @Option(names = {"--master-password"}, required = true, interactive = true, arity = "0..1", description = "Vault master password")
        private String masterPassword;

        @Option(names = {"--name"}, required = true, description = "Profile name")
        private String name;

        private final CredentialVaultService vaultService;

        @Autowired
        public VaultGetSubcommand(CredentialVaultService vaultService) {
            this.vaultService = vaultService;
        }

        @Override
        public Integer call() {
            try {
                CredentialProfile profile = vaultService.getProfile(masterPassword, name);
                System.out.println("Profile Name : " + profile.getProfileName());
                System.out.println("DBMS Type    : " + profile.getDbmsType());
                System.out.println("Host         : " + profile.getHost());
                System.out.println("Port         : " + profile.getPort());
                System.out.println("Database     : " + profile.getDatabaseName());
                System.out.println("Username     : " + profile.getUsername());
                System.out.println("Password     : " + (profile.getPassword() != null ? "********" : "NONE"));
                if (profile.getConnectionUri() != null) System.out.println("URI          : " + profile.getConnectionUri());
                return 0;
            } catch (Exception e) {
                System.err.println("❌ Vault get failed: " + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "remove", description = "Remove profile from vault.")
    public static class VaultRemoveSubcommand implements Callable<Integer> {
        @Option(names = {"--master-password"}, required = true, interactive = true, arity = "0..1", description = "Vault master password")
        private String masterPassword;

        @Option(names = {"--name"}, required = true, description = "Profile name")
        private String name;

        private final CredentialVaultService vaultService;

        @Autowired
        public VaultRemoveSubcommand(CredentialVaultService vaultService) {
            this.vaultService = vaultService;
        }

        @Override
        public Integer call() {
            try {
                boolean removed = vaultService.removeProfile(masterPassword, name);
                if (removed) {
                    System.out.println("✅ Profile [" + name + "] removed from vault.");
                    return 0;
                } else {
                    System.err.println("❌ Profile [" + name + "] not found in vault.");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("❌ Vault remove failed: " + e.getMessage());
                return 1;
            }
        }
    }
}
