package com.dbbackup.service;

import com.dbbackup.model.CompressionType;
import com.dbbackup.service.compression.CompressionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CompressionServiceTest {

    private CompressionService compressionService;

    @BeforeEach
    void setUp() {
        compressionService = new CompressionService();
    }

    @Test
    void testGzipCompressionAndDecompression(@TempDir Path tempDir) throws IOException {
        File sampleFile = tempDir.resolve("sample.sql").toFile();
        try (FileWriter writer = new FileWriter(sampleFile)) {
            writer.write("CREATE TABLE test (id INT, name VARCHAR(50));\nINSERT INTO test VALUES (1, 'Alice');\n");
        }

        File compressedFile = tempDir.resolve("sample.sql.gz").toFile();
        compressionService.compress(sampleFile, CompressionType.GZIP, compressedFile);

        assertTrue(compressedFile.exists());
        assertTrue(compressedFile.length() > 0);

        File extractDir = tempDir.resolve("extracted_gzip").toFile();
        File decompressedFile = compressionService.decompress(compressedFile, CompressionType.GZIP, extractDir);

        assertTrue(decompressedFile.exists());
        String content = Files.readString(decompressedFile.toPath());
        assertTrue(content.contains("CREATE TABLE test"));
    }

    @Test
    void testZipCompressionAndDecompression(@TempDir Path tempDir) throws IOException {
        File sampleFile = tempDir.resolve("sample.sql").toFile();
        try (FileWriter writer = new FileWriter(sampleFile)) {
            writer.write("SELECT * FROM users;\n");
        }

        File compressedFile = tempDir.resolve("sample.zip").toFile();
        compressionService.compress(sampleFile, CompressionType.ZIP, compressedFile);

        assertTrue(compressedFile.exists());

        File extractDir = tempDir.resolve("extracted_zip").toFile();
        File decompressedFile = compressionService.decompress(compressedFile, CompressionType.ZIP, extractDir);

        assertTrue(decompressedFile.exists());
        String content = Files.readString(decompressedFile.toPath());
        assertTrue(content.contains("SELECT * FROM users;"));
    }

    @Test
    void testTarGzCompressionAndDecompression(@TempDir Path tempDir) throws IOException {
        File sampleFile = tempDir.resolve("sample.sql").toFile();
        try (FileWriter writer = new FileWriter(sampleFile)) {
            writer.write("DROP TABLE IF EXISTS orders;\n");
        }

        File compressedFile = tempDir.resolve("sample.tar.gz").toFile();
        compressionService.compress(sampleFile, CompressionType.TAR_GZ, compressedFile);

        assertTrue(compressedFile.exists());

        File extractDir = tempDir.resolve("extracted_targz").toFile();
        File decompressedFile = compressionService.decompress(compressedFile, CompressionType.TAR_GZ, extractDir);

        assertTrue(decompressedFile.exists());
        String content = Files.readString(decompressedFile.toPath());
        assertTrue(content.contains("DROP TABLE IF EXISTS orders;"));
    }
}
