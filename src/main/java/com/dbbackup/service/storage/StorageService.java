package com.dbbackup.service.storage;

import com.dbbackup.model.StorageType;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface StorageService {
    StorageType getType();
    String upload(File file, String destinationKey) throws IOException;
    File download(String sourceKey, File destinationFile) throws IOException;
    List<String> listBackups() throws IOException;
    boolean deleteBackup(String key) throws IOException;
}
