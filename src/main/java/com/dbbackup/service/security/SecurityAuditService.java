package com.dbbackup.service.security;

import com.dbbackup.model.DatabaseCredentials;
import com.dbbackup.model.DbmsType;
import com.dbbackup.model.SecurityAuditReport;
import com.dbbackup.model.SecurityAuditReport.AuditFinding;
import com.dbbackup.model.SecurityAuditReport.AuditSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
public class SecurityAuditService {
    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    private static final List<String> WEAK_PASSWORDS = Arrays.asList(
        "", "root", "admin", "123456", "password", "pass", "1234", "mysql", "postgres"
    );

    /**
     * Conducts a security compliance audit on the provided database connection configuration.
     */
    public SecurityAuditReport performSecurityAudit(DatabaseCredentials credentials, String backupDir) {
        log.info("Running Database Security Compliance Audit for {} database [{}]...", credentials.getDbmsType(), credentials.getDatabaseName());

        SecurityAuditReport report = new SecurityAuditReport();
        report.setDbmsType(credentials.getDbmsType() != null ? credentials.getDbmsType().name() : "UNKNOWN");
        report.setDatabaseName(credentials.getDatabaseName());
        report.setHost(credentials.getHost() != null ? credentials.getHost() : "localhost");
        report.setPort(credentials.getPort());

        int score = 100;

        // 1. Password Strength Audit
        String password = credentials.getPassword();
        if (credentials.getDbmsType() != DbmsType.SQLITE) {
            if (password == null || password.trim().isEmpty()) {
                score -= 30;
                report.addFinding(new AuditFinding(
                    "AUTHENTICATION",
                    AuditSeverity.HIGH,
                    "Empty Database Password",
                    "Database user is configured without a password.",
                    "Set a strong password (minimum 12 characters with mixed case, numbers, and symbols) for database authentication."
                ));
            } else if (WEAK_PASSWORDS.contains(password.trim().toLowerCase())) {
                score -= 25;
                report.addFinding(new AuditFinding(
                    "AUTHENTICATION",
                    AuditSeverity.HIGH,
                    "Weak Default Password",
                    "Database user is using a common default or weak password: '" + password + "'.",
                    "Change the database user password immediately to a strong, randomly generated passphrase."
                ));
            } else if (password.length() < 8) {
                score -= 10;
                report.addFinding(new AuditFinding(
                    "AUTHENTICATION",
                    AuditSeverity.MEDIUM,
                    "Short Password Length",
                    "Database password is less than 8 characters in length.",
                    "Enforce a minimum password length of at least 12 characters."
                ));
            }
        }

        // 2. Superuser Privileges Audit
        String username = credentials.getUsername();
        if (username != null) {
            String lowerUser = username.toLowerCase();
            if (lowerUser.equals("root") || lowerUser.equals("postgres") || lowerUser.equals("sa")) {
                score -= 15;
                report.addFinding(new AuditFinding(
                    "AUTHORIZATION",
                    AuditSeverity.MEDIUM,
                    "Superuser Administrative Credential Usage",
                    "Backup operation is using administrative superuser ('" + username + "').",
                    "Create a dedicated backup user role with least-privilege SELECT, LOCK TABLES, and READ ONLY permissions."
                ));
            }
        }

        // 3. Transport Security / SSL Enforcement Audit
        if (credentials.getDbmsType() != DbmsType.SQLITE) {
            String uri = credentials.getConnectionUri();
            boolean sslExplicit = (uri != null && (uri.contains("ssl=true") || uri.contains("useSSL=true") || uri.contains("sslmode=require") || uri.contains("tls=true")));
            if (!sslExplicit) {
                score -= 15;
                report.addFinding(new AuditFinding(
                    "TRANSPORT_SECURITY",
                    AuditSeverity.MEDIUM,
                    "Unenforced SSL/TLS In-Transit Encryption",
                    "Connection configuration does not explicitly require SSL/TLS transport encryption.",
                    "Append SSL/TLS enforcement options to the database connection parameters (e.g. useSSL=true or sslmode=require)."
                ));
            }
        }

        // 4. Default Exposed Port Audit
        int port = credentials.getPort();
        if (credentials.getDbmsType() == DbmsType.MYSQL && port == 3306) {
            score -= 5;
            report.addFinding(new AuditFinding(
                "NETWORK_SECURITY",
                AuditSeverity.LOW,
                "Standard MySQL Port 3306 Usage",
                "MySQL is running on default port 3306.",
                "Consider remapping database service ports or securing default ports behind a local firewall / VPN."
            ));
        } else if (credentials.getDbmsType() == DbmsType.POSTGRESQL && port == 5432) {
            score -= 5;
            report.addFinding(new AuditFinding(
                "NETWORK_SECURITY",
                AuditSeverity.LOW,
                "Standard PostgreSQL Port 5432 Usage",
                "PostgreSQL is running on default port 5432.",
                "Ensure PostgreSQL default port is blocked from direct public internet exposure."
            ));
        } else if (credentials.getDbmsType() == DbmsType.MONGODB && port == 27017) {
            score -= 5;
            report.addFinding(new AuditFinding(
                "NETWORK_SECURITY",
                AuditSeverity.LOW,
                "Standard MongoDB Port 27017 Usage",
                "MongoDB is running on default port 27017.",
                "Verify bindIp settings to ensure MongoDB port is bound to localhost or protected network."
            ));
        }

        // 5. Backup Directory Storage Security Audit
        if (backupDir != null) {
            File dir = new File(backupDir);
            if (dir.exists() && dir.canWrite()) {
                report.addFinding(new AuditFinding(
                    "STORAGE_SECURITY",
                    AuditSeverity.INFO,
                    "Backup Directory Status",
                    "Local backup target directory [" + dir.getAbsolutePath() + "] is writeable.",
                    "Ensure filesystem ACLs restrict directory access strictly to the backup service account."
                ));
            }
        }

        // Ensure score bounds
        score = Math.max(0, Math.min(100, score));
        report.setScore(score);

        // Assign Grade Rating
        if (score >= 95) report.setRating("A+");
        else if (score >= 85) report.setRating("A");
        else if (score >= 70) report.setRating("B");
        else if (score >= 50) report.setRating("C");
        else report.setRating("F");

        log.info("Security Audit completed! Score: {}/100, Rating: {}", score, report.getRating());
        return report;
    }
}
