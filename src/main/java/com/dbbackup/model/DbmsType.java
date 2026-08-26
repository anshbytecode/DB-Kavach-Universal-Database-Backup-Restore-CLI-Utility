package com.dbbackup.model;

public enum DbmsType {
    MYSQL,
    POSTGRESQL,
    MONGODB,
    SQLITE;

    public static DbmsType fromString(String text) {
        if (text == null) return null;
        for (DbmsType type : DbmsType.values()) {
            if (type.name().equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported DBMS type: " + text);
    }
}
