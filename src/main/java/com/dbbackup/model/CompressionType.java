package com.dbbackup.model;

public enum CompressionType {
    GZIP(".gz"),
    ZIP(".zip"),
    TAR_GZ(".tar.gz"),
    NONE("");

    private final String extension;

    CompressionType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public static CompressionType fromString(String text) {
        if (text == null) return GZIP;
        String normalized = text.trim().replace("-", "_").replace(".", "").toUpperCase();
        if ("TARGZ".equals(normalized) || "TAR_GZ".equals(normalized)) {
            return TAR_GZ;
        }
        for (CompressionType type : CompressionType.values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return GZIP;
    }
}
