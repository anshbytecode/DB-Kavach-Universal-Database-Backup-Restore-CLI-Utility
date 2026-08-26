package com.dbbackup.model;

public enum StorageType {
    LOCAL,
    S3,
    GCS,
    AZURE;

    public static StorageType fromString(String text) {
        if (text == null) return LOCAL;
        for (StorageType type : StorageType.values()) {
            if (type.name().equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        return LOCAL;
    }
}
