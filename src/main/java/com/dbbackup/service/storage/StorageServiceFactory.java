package com.dbbackup.service.storage;

import com.dbbackup.model.StorageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StorageServiceFactory {
    private final Map<StorageType, StorageService> storageServiceMap;

    @Autowired
    public StorageServiceFactory(List<StorageService> services) {
        this.storageServiceMap = services.stream()
                .collect(Collectors.toMap(StorageService::getType, Function.identity()));
    }

    public StorageService getStorageService(StorageType storageType) {
        StorageService service = storageServiceMap.get(storageType);
        if (service == null) {
            throw new IllegalArgumentException("No storage service available for type: " + storageType);
        }
        return service;
    }
}
