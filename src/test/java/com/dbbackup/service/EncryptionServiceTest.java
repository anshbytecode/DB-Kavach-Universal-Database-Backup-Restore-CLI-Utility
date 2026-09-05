package com.dbbackup.service;

import com.dbbackup.service.security.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    public void setUp() {
        encryptionService = new EncryptionService();
    }

    @Test
    public void testTextEncryptionAndDecryption() throws Exception {
        String originalText = "SecretDatabasePassword123!@#";
        String passphrase = "MasterPassphrase456";

        String encryptedBase64 = encryptionService.encryptText(originalText, passphrase);
        assertNotNull(encryptedBase64);
        assertNotEquals(originalText, encryptedBase64);

        String decryptedText = encryptionService.decryptText(encryptedBase64, passphrase);
        assertEquals(originalText, decryptedText);
    }

    @Test
    public void testTextDecryptionWithWrongPassphrase() throws Exception {
        String originalText = "SecretData";
        String passphrase = "CorrectPassphrase";
        String wrongPassphrase = "WrongPassphrase";

        String encryptedBase64 = encryptionService.encryptText(originalText, passphrase);

        assertThrows(IllegalArgumentException.class, () -> {
            encryptionService.decryptText(encryptedBase64, wrongPassphrase);
        });
    }

    @Test
    public void testFileEncryptionAndDecryption(@TempDir Path tempDir) throws Exception {
        File inputFile = tempDir.resolve("original.txt").toFile();
        File encryptedFile = tempDir.resolve("original.txt.enc").toFile();
        File decryptedFile = tempDir.resolve("restored.txt").toFile();

        String fileContent = "SELECT * FROM users; INSERT INTO users VALUES (1, 'anshul', 'secret');";
        Files.writeString(inputFile.toPath(), fileContent);

        String passphrase = "FileEncryptionPassphrase123";

        // Encrypt
        encryptionService.encryptFile(inputFile, encryptedFile, passphrase);
        assertTrue(encryptedFile.exists());
        assertTrue(encryptionService.isEncryptedFile(encryptedFile));
        assertFalse(encryptionService.isEncryptedFile(inputFile));

        // Decrypt
        encryptionService.decryptFile(encryptedFile, decryptedFile, passphrase);
        assertTrue(decryptedFile.exists());

        String restoredContent = Files.readString(decryptedFile.toPath());
        assertEquals(fileContent, restoredContent);
    }

    @Test
    public void testFileDecryptionFailsWithWrongPassphrase(@TempDir Path tempDir) throws Exception {
        File inputFile = tempDir.resolve("secret.dump").toFile();
        File encryptedFile = tempDir.resolve("secret.dump.enc").toFile();
        File decryptedFile = tempDir.resolve("failed.dump").toFile();

        Files.writeString(inputFile.toPath(), "Database dump payload");

        encryptionService.encryptFile(inputFile, encryptedFile, "CorrectPass");

        assertThrows(IllegalArgumentException.class, () -> {
            encryptionService.decryptFile(encryptedFile, decryptedFile, "WrongPass");
        });
    }
}
