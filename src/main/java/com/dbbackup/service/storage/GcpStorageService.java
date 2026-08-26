package com.dbbackup.service.storage;

import com.dbbackup.model.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

@Service
public class GcpStorageService implements StorageService {
    private static final Logger log = LoggerFactory.getLogger(GcpStorageService.class);

    @Value("${backup.cloud.gcp.bucket-name:my-database-backups}")
    private String bucketName;

    @Value("${backup.cloud.gcp.mock-mode:true}")
    private boolean mockMode;

    @Override
    public StorageType getType() {
        return StorageType.GCS;
    }

    @Override
    public String upload(File file, String destinationKey) throws IOException {
        log.info("[MOCK GCS] Uploading {} to gs://{}/{}", file.getName(), bucketName, destinationKey);
        File mockCloudFile = new File("./cloud-mock/gcs/" + destinationKey);
        mockCloudFile.getParentFile().mkdirs();
        Files.copy(file.toPath(), mockCloudFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return "gs://" + bucketName + "/" + destinationKey;
    }

    @Override
    public File download(String sourceKey, File destinationFile) throws IOException {
        String cleanKey = sourceKey.replace("gs://" + bucketName + "/", "");
        log.info("[MOCK GCS] Downloading gs://{}/{} to {}", bucketName, cleanKey, destinationFile.getAbsolutePath());
        File mockCloudFile = new File("./cloud-mock/gcs/" + cleanKey);
        if (!mockCloudFile.exists()) {
            throw new IOException("GCS mock file not found: " + cleanKey);
        }
        destinationFile.getParentFile().mkdirs();
        Files.copy(mockCloudFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destinationFile;
    }

    @Override
    public List<String> listBackups() throws IOException {
        File mockDir = new File("./cloud-mock/gcs/");
        if (!mockDir.exists()) return Collections.emptyList();
        try (var walk = Files.walk(mockDir.toPath())) {
            return walk.filter(Files::isRegularFile).map(p -> "gs://" + bucketName + "/" + mockDir.toPath().relativize(p)).toList();
        }
    }

    @Override
    public boolean deleteBackup(String key) throws IOException {
        String cleanKey = key.replace("gs://" + bucketName + "/", "");
        File mockCloudFile = new File("./cloud-mock/gcs/" + cleanKey);
        return Files.deleteIfExists(mockCloudFile.toPath());
    }
}
