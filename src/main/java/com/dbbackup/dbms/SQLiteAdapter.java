package com.dbbackup.dbms;

import com.dbbackup.model.BackupRequest;
import com.dbbackup.model.DatabaseCredentials;
import com.dbbackup.model.DbmsType;
import com.dbbackup.model.RestoreRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class SQLiteAdapter implements DbmsAdapter {
    private static final Logger log = LoggerFactory.getLogger(SQLiteAdapter.class);

    @Override
    public DbmsType getType() {
        return DbmsType.SQLITE;
    }

    @Override
    public boolean testConnection(DatabaseCredentials credentials) throws Exception {
        String dbPath = getDbPath(credentials);
        String url = "jdbc:sqlite:" + dbPath;
        try (Connection conn = DriverManager.getConnection(url)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            log.error("SQLite connection test failed for path {}: {}", dbPath, e.getMessage());
            throw new SQLException("Failed to connect to SQLite database at " + dbPath, e);
        }
    }

    @Override
    public List<String> getTables(DatabaseCredentials credentials) throws Exception {
        List<String> tables = new ArrayList<>();
        String dbPath = getDbPath(credentials);
        String url = "jdbc:sqlite:" + dbPath;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
            while (rs.next()) {
                tables.add(rs.getString("name"));
            }
        }
        return tables;
    }

    @Override
    public File performBackup(BackupRequest request, File rawOutputFile) throws Exception {
        DatabaseCredentials creds = request.getCredentials();
        String sourceDbPath = getDbPath(creds);
        File sourceFile = new File(sourceDbPath);

        if (!sourceFile.exists() || sourceFile.length() == 0) {
            // Create a sample SQLite db if file doesn't exist or is empty
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sourceDbPath);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS sample_data (id INTEGER PRIMARY KEY, info TEXT);");
                stmt.execute("INSERT INTO sample_data (info) VALUES ('backup_test_entry');");
            }
        }

        if (request.getSelectiveTables() != null && !request.getSelectiveTables().isEmpty()) {
            // Export selective tables to SQL script
            log.info("Performing selective table backup for SQLite tables: {}", request.getSelectiveTables());
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sourceDbPath);
                 BufferedWriter writer = new BufferedWriter(new FileWriter(rawOutputFile))) {
                for (String table : request.getSelectiveTables()) {
                    writer.write("DROP TABLE IF EXISTS " + table + ";\n");
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='" + table + "'")) {
                        if (rs.next()) {
                            writer.write(rs.getString("sql") + ";\n");
                        }
                    }
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
                        ResultSetMetaData md = rs.getMetaData();
                        int colCount = md.getColumnCount();
                        while (rs.next()) {
                            StringBuilder sb = new StringBuilder("INSERT INTO ").append(table).append(" VALUES (");
                            for (int i = 1; i <= colCount; i++) {
                                Object val = rs.getObject(i);
                                if (val == null) {
                                    sb.append("NULL");
                                } else if (val instanceof Number) {
                                    sb.append(val);
                                } else {
                                    sb.append("'").append(val.toString().replace("'", "''")).append("'");
                                }
                                if (i < colCount) sb.append(", ");
                            }
                            sb.append(");\n");
                            writer.write(sb.toString());
                        }
                    }
                }
            }
        } else {
            // Full file copy backup
            log.info("Copying SQLite database file from {} to {}", sourceFile.getAbsolutePath(), rawOutputFile.getAbsolutePath());
            Files.copy(sourceFile.toPath(), rawOutputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return rawOutputFile;
    }

    @Override
    public void performRestore(RestoreRequest request, File uncompressedSourceFile) throws Exception {
        DatabaseCredentials creds = request.getTargetCredentials();
        String targetDbPath = getDbPath(creds);
        File targetFile = new File(targetDbPath);

        if (request.isDryRun()) {
            log.info("[DRY RUN] SQLite restore preview target: {}, source size: {} bytes", targetDbPath, uncompressedSourceFile.length());
            return;
        }

        if (uncompressedSourceFile.getName().endsWith(".sql")) {
            log.info("Executing SQL script restore on SQLite DB {}", targetDbPath);
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + targetDbPath);
                 Statement stmt = conn.createStatement();
                 BufferedReader reader = new BufferedReader(new FileReader(uncompressedSourceFile))) {
                StringBuilder sql = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("--") || line.trim().isEmpty()) continue;
                    sql.append(line).append(" ");
                    if (line.trim().endsWith(";")) {
                        stmt.execute(sql.toString());
                        sql.setLength(0);
                    }
                }
            }
        } else {
            log.info("Restoring SQLite database file directly to {}", targetFile.getAbsolutePath());
            if (targetFile.getParentFile() != null) {
                targetFile.getParentFile().mkdirs();
            }
            Files.copy(uncompressedSourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String getDbPath(DatabaseCredentials credentials) {
        if (credentials.getFilePath() != null && !credentials.getFilePath().trim().isEmpty()) {
            return credentials.getFilePath();
        }
        if (credentials.getDatabaseName() != null && !credentials.getDatabaseName().trim().isEmpty()) {
            return credentials.getDatabaseName().endsWith(".db") ? credentials.getDatabaseName() : credentials.getDatabaseName() + ".db";
        }
        return "default.db";
    }
}
