package com.dbbackup.cli;

import com.dbbackup.service.security.CredentialVaultService;
import com.dbbackup.service.security.DataMaskingService;
import com.dbbackup.service.security.EncryptionService;
import com.dbbackup.service.security.SecurityAuditService;
import picocli.CommandLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityCommandTest {

    private SecurityAuditService auditService;
    private EncryptionService encryptionService;
    private DataMaskingService maskingService;
    private CredentialVaultService vaultService;

    @BeforeEach
    public void setUp() {
        auditService = new SecurityAuditService();
        encryptionService = new EncryptionService();
        maskingService = new DataMaskingService();
        vaultService = new CredentialVaultService(encryptionService);
    }

    @Test
    public void testSecurityAuditCommand() {
        SecurityCommand.AuditSubcommand cmd = new SecurityCommand.AuditSubcommand(auditService);
        CommandLine cl = new CommandLine(cmd);
        int exitCode = cl.execute("--dbms=sqlite", "-d", "test.db");
        assertEquals(0, exitCode);
    }

    @Test
    public void testSecurityEncryptAndDecryptCommands(@TempDir Path tempDir) throws Exception {
        File inputFile = tempDir.resolve("sample.dump").toFile();
        File encFile = tempDir.resolve("sample.dump.enc").toFile();
        File decFile = tempDir.resolve("sample.dump.dec").toFile();

        Files.writeString(inputFile.toPath(), "Sample Database Content");

        SecurityCommand.EncryptSubcommand encCmd = new SecurityCommand.EncryptSubcommand(encryptionService);
        CommandLine encCl = new CommandLine(encCmd);
        int encExit = encCl.execute("-f", inputFile.getAbsolutePath(), "-p", "TestPass123", "-o", encFile.getAbsolutePath());
        assertEquals(0, encExit);
        assertTrue(encFile.exists());

        SecurityCommand.DecryptSubcommand decCmd = new SecurityCommand.DecryptSubcommand(encryptionService);
        CommandLine decCl = new CommandLine(decCmd);
        int decExit = decCl.execute("-f", encFile.getAbsolutePath(), "-p", "TestPass123", "-o", decFile.getAbsolutePath());
        assertEquals(0, decExit);
        assertTrue(decFile.exists());
        assertEquals("Sample Database Content", Files.readString(decFile.toPath()));
    }

    @Test
    public void testSecurityMaskCommand(@TempDir Path tempDir) throws Exception {
        File inputFile = tempDir.resolve("raw.sql").toFile();
        File maskedFile = tempDir.resolve("masked.sql").toFile();

        Files.writeString(inputFile.toPath(), "INSERT INTO users VALUES ('user@domain.com');");

        SecurityCommand.MaskSubcommand maskCmd = new SecurityCommand.MaskSubcommand(maskingService);
        CommandLine cl = new CommandLine(maskCmd);
        int exitCode = cl.execute("-f", inputFile.getAbsolutePath(), "-o", maskedFile.getAbsolutePath());
        assertEquals(0, exitCode);
        assertTrue(maskedFile.exists());
        assertFalse(Files.readString(maskedFile.toPath()).contains("user@domain.com"));
    }
}
