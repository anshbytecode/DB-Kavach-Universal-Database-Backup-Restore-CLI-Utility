package com.dbbackup.dbms;

import com.dbbackup.model.BackupRequest;
import com.dbbackup.model.DatabaseCredentials;
import com.dbbackup.model.DbmsType;
import com.dbbackup.model.RestoreRequest;
import com.dbbackup.util.ProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class MySQLAdapter implements DbmsAdapter {
    private static final Logger log = LoggerFactory.getLogger(MySQLAdapter.class);

    @Override
    public DbmsType getType() {
        return DbmsType.MYSQL;
    }

    @Override
    public boolean testConnection(DatabaseCredentials credentials) throws Exception {
        String url = buildJdbcUrl(credentials);
        try (Connection conn = DriverManager.getConnection(url, credentials.getUsername(), credentials.getPassword())) {
            return conn.isValid(5);
        } catch (SQLException e) {
            log.error("MySQL connection test failed: {}", e.getMessage());
            throw new SQLException("Failed to connect to MySQL database at " + credentials.getHost() + ":" + credentials.getPort() + "/" + credentials.getDatabaseName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getTables(DatabaseCredentials credentials) throws Exception {
        List<String> tables = new ArrayList<>();
        String url = buildJdbcUrl(credentials);
        try (Connection conn = DriverManager.getConnection(url, credentials.getUsername(), credentials.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }

    @Override
    public File performBackup(BackupRequest request, File rawOutputFile) throws Exception {
        DatabaseCredentials creds = request.getCredentials();

        if (ProcessRunner.isCommandAvailable("mysqldump")) {
            log.info("Using native 'mysqldump' tool for MySQL backup...");
            List<String> command = new ArrayList<>();
            command.add("mysqldump");
            command.add("-h" + creds.getHost());
            command.add("-P" + creds.getPort());
            if (creds.getUsername() != null && !creds.getUsername().isEmpty()) {
                command.add("-u" + creds.getUsername());
            }
            if (creds.getPassword() != null && !creds.getPassword().isEmpty()) {
                command.add("-p" + creds.getPassword());
            }
            command.add(creds.getDatabaseName());

            if (request.getSelectiveTables() != null && !request.getSelectiveTables().isEmpty()) {
                command.addAll(request.getSelectiveTables());
            }

            ProcessRunner.ProcessResult result = ProcessRunner.execute(command, rawOutputFile, null, 15);
            if (result.isSuccess() && rawOutputFile.length() > 0) {
                log.info("mysqldump completed successfully. File size: {} bytes", rawOutputFile.length());
                return rawOutputFile;
            }
            log.warn("mysqldump exited with error code {}. Falling back to JDBC dump engine.", result.getExitCode());
        }

        log.info("Executing Pure Java JDBC MySQL dump engine...");
        exportViaJdbc(creds, request.getSelectiveTables(), rawOutputFile);
        return rawOutputFile;
    }

    @Override
    public void performRestore(RestoreRequest request, File uncompressedSourceFile) throws Exception {
        DatabaseCredentials creds = request.getTargetCredentials();

        if (request.isDryRun()) {
            log.info("[DRY RUN] MySQL restore preview to {}:{}/{}, source file: {} bytes",
                    creds.getHost(), creds.getPort(), creds.getDatabaseName(), uncompressedSourceFile.length());
            return;
        }

        if (ProcessRunner.isCommandAvailable("mysql")) {
            log.info("Using native 'mysql' CLI tool for restore...");
            List<String> command = new ArrayList<>();
            command.add("mysql");
            command.add("-h" + creds.getHost());
            command.add("-P" + creds.getPort());
            if (creds.getUsername() != null && !creds.getUsername().isEmpty()) {
                command.add("-u" + creds.getUsername());
            }
            if (creds.getPassword() != null && !creds.getPassword().isEmpty()) {
                command.add("-p" + creds.getPassword());
            }
            command.add(creds.getDatabaseName());

            ProcessRunner.ProcessResult result = ProcessRunner.execute(command, null, uncompressedSourceFile, 15);
            if (result.isSuccess()) {
                log.info("mysql restore completed successfully.");
                return;
            }
            log.warn("mysql restore tool failed. Falling back to JDBC script execution.");
        }

        log.info("Executing Pure Java JDBC script execution for restore...");
        restoreViaJdbc(creds, uncompressedSourceFile);
    }

    private void exportViaJdbc(DatabaseCredentials creds, List<String> selectiveTables, File outputFile) throws Exception {
        String url = buildJdbcUrl(creds);
        try (Connection conn = DriverManager.getConnection(url, creds.getUsername(), creds.getPassword());
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            writer.write("-- MySQL Pure Java Dump\n");
            writer.write("SET FOREIGN_KEY_CHECKS=0;\n\n");

            List<String> tablesToDump = (selectiveTables != null && !selectiveTables.isEmpty())
                    ? selectiveTables : getTables(creds);

            for (String table : tablesToDump) {
                writer.write("-- Dumping table " + table + "\n");
                writer.write("DROP TABLE IF EXISTS `" + table + "`;\n");

                // Get Create Table SQL
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
                    if (rs.next()) {
                        writer.write(rs.getString(2) + ";\n\n");
                    }
                }

                // Get Table Data
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "`")) {
                    ResultSetMetaData md = rs.getMetaData();
                    int colCount = md.getColumnCount();
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO `" + table + "` VALUES (");
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
                    writer.write("\n");
                }
            }
            writer.write("SET FOREIGN_KEY_CHECKS=1;\n");
        }
    }

    private void restoreViaJdbc(DatabaseCredentials creds, File sqlFile) throws Exception {
        String url = buildJdbcUrl(creds);
        try (Connection conn = DriverManager.getConnection(url, creds.getUsername(), creds.getPassword());
             Statement stmt = conn.createStatement();
             BufferedReader reader = new BufferedReader(new FileReader(sqlFile))) {

            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("--") || line.trim().startsWith("/*") || line.trim().isEmpty()) continue;
                sql.append(line).append("\n");
                if (line.trim().endsWith(";")) {
                    try {
                        stmt.execute(sql.toString());
                    } catch (SQLException e) {
                        log.warn("Warning executing SQL line during restore: {}", e.getMessage());
                    }
                    sql.setLength(0);
                }
            }
        }
    }

    private String buildJdbcUrl(DatabaseCredentials creds) {
        if (creds.getConnectionUri() != null && !creds.getConnectionUri().isEmpty()) {
            return creds.getConnectionUri();
        }
        return "jdbc:mysql://" + creds.getHost() + ":" + creds.getPort() + "/" + creds.getDatabaseName() + "?allowPublicKeyRetrieval=true&useSSL=true";
    }
}
