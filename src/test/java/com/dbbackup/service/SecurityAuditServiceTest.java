package com.dbbackup.service;

import com.dbbackup.model.DatabaseCredentials;
import com.dbbackup.model.DbmsType;
import com.dbbackup.model.SecurityAuditReport;
import com.dbbackup.service.security.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityAuditServiceTest {

    private SecurityAuditService auditService;

    @BeforeEach
    public void setUp() {
        auditService = new SecurityAuditService();
    }

    @Test
    public void testAuditInsecureConfiguration() {
        DatabaseCredentials creds = new DatabaseCredentials();
        creds.setDbmsType(DbmsType.MYSQL);
        creds.setHost("localhost");
        creds.setPort(3306);
        creds.setDatabaseName("prod_db");
        creds.setUsername("root");
        creds.setPassword("root"); // weak password & root user & default port & no ssl

        SecurityAuditReport report = auditService.performSecurityAudit(creds, "./temp-backups");

        assertNotNull(report);
        assertTrue(report.getScore() < 70);
        assertFalse(report.getFindings().isEmpty());
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getTitle().contains("Weak Default Password")));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getTitle().contains("Superuser Administrative Credential Usage")));
    }

    @Test
    public void testAuditSecureConfiguration() {
        DatabaseCredentials creds = new DatabaseCredentials();
        creds.setDbmsType(DbmsType.POSTGRESQL);
        creds.setHost("db.internal.cloud");
        creds.setPort(5433);
        creds.setDatabaseName("secure_db");
        creds.setUsername("backup_operator");
        creds.setPassword("C0mpl3x!P@ssw0rd#2026$Secured");
        creds.setConnectionUri("jdbc:postgresql://db.internal.cloud:5433/secure_db?sslmode=require");

        SecurityAuditReport report = auditService.performSecurityAudit(creds, "./temp-backups");

        assertNotNull(report);
        assertEquals(100, report.getScore());
        assertEquals("A+", report.getRating());
    }

    @Test
    public void testAuditSQLiteConfiguration() {
        DatabaseCredentials creds = new DatabaseCredentials();
        creds.setDbmsType(DbmsType.SQLITE);
        creds.setDatabaseName("app.db");
        creds.setFilePath("app.db");

        SecurityAuditReport report = auditService.performSecurityAudit(creds, "./temp-backups");

        assertNotNull(report);
        assertEquals(100, report.getScore());
        assertEquals("A+", report.getRating());
    }
}
