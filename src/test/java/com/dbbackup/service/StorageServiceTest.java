package com.dbbackup.service;

import com.dbbackup.model.StorageType;
import com.dbbackup.service.storage.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    @Test
    void testLocalStorageService(@TempDir Path tempDir) throws IOException {
        LocalStorageService localService = new LocalStorageService();
        ReflectionTestUtils.setField(localService, "localStoragePath", tempDir.toString());

        File testFile = tempDir.resolve("backup.dump").toFile();
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("SQL DUMP DATA");
        }

        String location = localService.upload(testFile, "sqlite/backup.dump");
        assertNotNull(location);
        assertTrue(new File(location).exists());

        File downloadedFile = tempDir.resolve("downloaded.dump").toFile();
        localService.download(location, downloadedFile);
        assertTrue(downloadedFile.exists());
    }

    @Test
    void testAwsS3StorageServiceMockMode(@TempDir Path tempDir) throws IOException {
        AwsS3StorageService s3Service = new AwsS3StorageService();
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "mockMode", true);

        File testFile = tempDir.resolve("s3_backup.dump").toFile();
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("S3 MOCK DATA");
        }

        String location = s3Service.upload(testFile, "backups/s3_backup.dump");
        assertTrue(location.startsWith("s3://test-bucket/"));
        assertEquals(StorageType.S3, s3Service.getType());
    }
}
