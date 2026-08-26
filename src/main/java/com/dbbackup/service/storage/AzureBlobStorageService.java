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
public class AzureBlobStorageService implements StorageService {
    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageService.class);

    @Value("${backup.cloud.azure.container-name:my-database-backups}")
    private String containerName;

    @Value("${backup.cloud.azure.mock-mode:true}")
    private boolean mockMode;

    @Override
    public StorageType getType() {
        return StorageType.AZURE;
    }

    @Override
    public String upload(File file, String destinationKey) throws IOException {
        log.info("[MOCK AZURE] Uploading {} to azure://{}/{}", file.getName(), containerName, destinationKey);
        File mockCloudFile = new File("./cloud-mock/azure/" + destinationKey);
        mockCloudFile.getParentFile().mkdirs();
        Files.copy(file.toPath(), mockCloudFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return "azure://" + containerName + "/" + destinationKey;
    }

    @Override
    public File download(String sourceKey, File destinationFile) throws IOException {
        String cleanKey = sourceKey.replace("azure://" + containerName + "/", "");
        log.info("[MOCK AZURE] Downloading azure://{}/{} to {}", containerName, cleanKey, destinationFile.getAbsolutePath());
        File mockCloudFile = new File("./cloud-mock/azure/" + cleanKey);
        if (!mockCloudFile.exists()) {
            throw new IOException("Azure mock file not found: " + cleanKey);
        }
        destinationFile.getParentFile().mkdirs();
        Files.copy(mockCloudFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destinationFile;
    }

    @Override
    public List<String> listBackups() throws IOException {
        File mockDir = new File("./cloud-mock/azure/");
        if (!mockDir.exists()) return Collections.emptyList();
        try (var walk = Files.walk(mockDir.toPath())) {
            return walk.filter(Files::isRegularFile).map(p -> "azure://" + containerName + "/" + mockDir.toPath().relativize(p)).toList();
        }
    }

    @Override
    public boolean deleteBackup(String key) throws IOException {
        String cleanKey = key.replace("azure://" + containerName + "/", "");
        File mockCloudFile = new File("./cloud-mock/azure/" + cleanKey);
        return Files.deleteIfExists(mockCloudFile.toPath());
    }
}
