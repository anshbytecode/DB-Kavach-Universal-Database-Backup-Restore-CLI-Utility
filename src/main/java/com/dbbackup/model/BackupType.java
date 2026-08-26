package com.dbbackup.model;

public enum BackupType {
    FULL,
    INCREMENTAL,
    DIFFERENTIAL;

    public static BackupType fromString(String text) {
        if (text == null) return FULL;
        for (BackupType type : BackupType.values()) {
            if (type.name().equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        return FULL;
    }
}
