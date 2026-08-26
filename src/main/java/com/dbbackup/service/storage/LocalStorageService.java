package com.dbbackup.service.storage;

import com.dbbackup.model.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocalStorageService implements StorageService {
    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    @Value("${backup.local-storage-path:./backups}")
    private String localStoragePath;

    @Override
    public StorageType getType() {
        return StorageType.LOCAL;
    }

    @Override
    public String upload(File file, String destinationKey) throws IOException {
        File baseDir = new File(localStoragePath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        File targetFile = new File(baseDir, destinationKey);
        if (targetFile.getParentFile() != null) {
            targetFile.getParentFile().mkdirs();
        }

        Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        log.info("Saved backup locally to: {}", targetFile.getAbsolutePath());
        return targetFile.getAbsolutePath();
    }

    @Override
    public File download(String sourceKey, File destinationFile) throws IOException {
        File sourceFile = new File(sourceKey);
        if (!sourceFile.exists()) {
            sourceFile = new File(localStoragePath, sourceKey);
        }
        if (!sourceFile.exists()) {
            throw new IOException("Local backup file not found: " + sourceKey);
        }

        if (destinationFile.getParentFile() != null) {
            destinationFile.getParentFile().mkdirs();
        }
        Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destinationFile;
    }

    @Override
    public List<String> listBackups() throws IOException {
        File baseDir = new File(localStoragePath);
        if (!baseDir.exists()) return new ArrayList<>();

        try (var walk = Files.walk(baseDir.toPath())) {
            return walk.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public boolean deleteBackup(String key) throws IOException {
        File target = new File(key);
        if (!target.exists()) {
            target = new File(localStoragePath, key);
        }
        return Files.deleteIfExists(target.toPath());
    }
}
