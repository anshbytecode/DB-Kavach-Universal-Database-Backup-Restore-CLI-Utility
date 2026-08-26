package com.dbbackup.service.storage;

import com.dbbackup.model.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

@Service
public class AwsS3StorageService implements StorageService {
    private static final Logger log = LoggerFactory.getLogger(AwsS3StorageService.class);

    @Value("${backup.cloud.s3.bucket-name:my-database-backups}")
    private String bucketName;

    @Value("${backup.cloud.s3.region:us-east-1}")
    private String regionStr;

    @Value("${backup.cloud.s3.mock-mode:true}")
    private boolean mockMode;

    @Override
    public StorageType getType() {
        return StorageType.S3;
    }

    @Override
    public String upload(File file, String destinationKey) throws IOException {
        if (mockMode) {
            log.info("[MOCK S3] Uploading {} to s3://{}/{}", file.getName(), bucketName, destinationKey);
            File mockCloudFile = new File("./cloud-mock/s3/" + destinationKey);
            mockCloudFile.getParentFile().mkdirs();
            Files.copy(file.toPath(), mockCloudFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return "s3://" + bucketName + "/" + destinationKey;
        }

        try (S3Client s3Client = S3Client.builder().region(Region.of(regionStr)).build()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(destinationKey)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
            log.info("Successfully uploaded {} to S3 bucket {}", destinationKey, bucketName);
            return "s3://" + bucketName + "/" + destinationKey;
        } catch (Exception e) {
            log.error("Failed to upload to S3, falling back to local cloud mock", e);
            File mockCloudFile = new File("./cloud-mock/s3/" + destinationKey);
            mockCloudFile.getParentFile().mkdirs();
            Files.copy(file.toPath(), mockCloudFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return "s3://" + bucketName + "/" + destinationKey + " (Mock Fallback)";
        }
    }

    @Override
    public File download(String sourceKey, File destinationFile) throws IOException {
        String cleanKey = sourceKey.replace("s3://" + bucketName + "/", "");
        if (mockMode) {
            log.info("[MOCK S3] Downloading s3://{}/{} to {}", bucketName, cleanKey, destinationFile.getAbsolutePath());
            File mockCloudFile = new File("./cloud-mock/s3/" + cleanKey);
            if (!mockCloudFile.exists()) {
                throw new IOException("S3 mock file not found: " + cleanKey);
            }
            destinationFile.getParentFile().mkdirs();
            Files.copy(mockCloudFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return destinationFile;
        }

        try (S3Client s3Client = S3Client.builder().region(Region.of(regionStr)).build()) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanKey)
                    .build();
            destinationFile.getParentFile().mkdirs();
            s3Client.getObject(getObjectRequest, destinationFile.toPath());
            return destinationFile;
        } catch (Exception e) {
            log.error("Failed to download from AWS S3, attempting mock fallback", e);
            File mockCloudFile = new File("./cloud-mock/s3/" + cleanKey);
            if (mockCloudFile.exists()) {
                destinationFile.getParentFile().mkdirs();
                Files.copy(mockCloudFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return destinationFile;
            }
            throw new IOException("Failed to download from S3: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> listBackups() throws IOException {
        if (mockMode) {
            File mockDir = new File("./cloud-mock/s3/");
            if (!mockDir.exists()) return Collections.emptyList();
            try (var walk = Files.walk(mockDir.toPath())) {
                return walk.filter(Files::isRegularFile).map(p -> "s3://" + bucketName + "/" + mockDir.toPath().relativize(p)).toList();
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean deleteBackup(String key) throws IOException {
        String cleanKey = key.replace("s3://" + bucketName + "/", "");
        File mockCloudFile = new File("./cloud-mock/s3/" + cleanKey);
        return Files.deleteIfExists(mockCloudFile.toPath());
    }
}
