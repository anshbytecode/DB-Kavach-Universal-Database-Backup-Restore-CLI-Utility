package com.dbbackup.service;

import com.dbbackup.service.security.DataMaskingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class DataMaskingServiceTest {

    private DataMaskingService maskingService;

    @BeforeEach
    public void setUp() {
        maskingService = new DataMaskingService();
    }

    @Test
    public void testEmailMasking() {
        String input = "User email is anshul.bhilare@example.com and dhruv.patel@domain.org";
        String masked = maskingService.maskContent(input);

        assertFalse(masked.contains("anshul.bhilare@example.com"));
        assertFalse(masked.contains("dhruv.patel@domain.org"));
        assertTrue(masked.contains("@example.com"));
        assertTrue(masked.contains("@domain.org"));
    }

    @Test
    public void testCreditCardMasking() {
        String input = "Customer card: 4532-1234-5678-9010 on file.";
        String masked = maskingService.maskContent(input);

        assertFalse(masked.contains("4532-1234-5678-9010"));
        assertTrue(masked.contains("XXXX-XXXX-XXXX-9010"));
    }

    @Test
    public void testSSNAndPhoneMasking() {
        String input = "SSN: 123-45-6789 Phone: 555-123-4567";
        String masked = maskingService.maskContent(input);

        assertFalse(masked.contains("123-45-6789"));
        assertFalse(masked.contains("555-123-4567"));
        assertTrue(masked.contains("XXX-XX-6789"));
        assertTrue(masked.contains("XXX-XXX-4567"));
    }

    @Test
    public void testDumpFileMasking(@TempDir Path tempDir) throws Exception {
        File inputFile = tempDir.resolve("raw_dump.sql").toFile();
        File outputFile = tempDir.resolve("masked_dump.sql").toFile();

        String sql = "INSERT INTO users (id, name, email, card) VALUES (1, 'Admin', 'admin@company.com', '4111111111111111');";
        Files.writeString(inputFile.toPath(), sql);

        maskingService.maskDumpFile(inputFile, outputFile);

        assertTrue(outputFile.exists());
        String maskedSql = Files.readString(outputFile.toPath());

        assertFalse(maskedSql.contains("admin@company.com"));
        assertFalse(maskedSql.contains("4111111111111111"));
        assertTrue(maskedSql.contains("XXXX-XXXX-XXXX-1111"));
    }
}
