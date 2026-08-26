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
public class PostgreSQLAdapter implements DbmsAdapter {
    private static final Logger log = LoggerFactory.getLogger(PostgreSQLAdapter.class);

    @Override
    public DbmsType getType() {
        return DbmsType.POSTGRESQL;
    }

    @Override
    public boolean testConnection(DatabaseCredentials credentials) throws Exception {
        String url = buildJdbcUrl(credentials);
        try (Connection conn = DriverManager.getConnection(url, credentials.getUsername(), credentials.getPassword())) {
            return conn.isValid(5);
        } catch (SQLException e) {
            log.error("PostgreSQL connection test failed: {}", e.getMessage());
            throw new SQLException("Failed to connect to PostgreSQL database at " + credentials.getHost() + ":" + credentials.getPort() + "/" + credentials.getDatabaseName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getTables(DatabaseCredentials credentials) throws Exception {
        List<String> tables = new ArrayList<>();
        String url = buildJdbcUrl(credentials);
        try (Connection conn = DriverManager.getConnection(url, credentials.getUsername(), credentials.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='public'")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }

    @Override
    public File performBackup(BackupRequest request, File rawOutputFile) throws Exception {
        DatabaseCredentials creds = request.getCredentials();

        if (ProcessRunner.isCommandAvailable("pg_dump")) {
            log.info("Using native 'pg_dump' tool for PostgreSQL backup...");
            List<String> command = new ArrayList<>();
            command.add("pg_dump");
            command.add("-h"); command.add(creds.getHost());
            command.add("-p"); command.add(String.valueOf(creds.getPort()));
            if (creds.getUsername() != null && !creds.getUsername().isEmpty()) {
                command.add("-U"); command.add(creds.getUsername());
            }
            command.add("-F"); command.add("p"); // Plain text SQL format
            command.add("-d"); command.add(creds.getDatabaseName());

            if (request.getSelectiveTables() != null && !request.getSelectiveTables().isEmpty()) {
                for (String t : request.getSelectiveTables()) {
                    command.add("-t"); command.add(t);
                }
            }

            ProcessRunner.ProcessResult result = ProcessRunner.execute(command, rawOutputFile, null, 15);
            if (result.isSuccess() && rawOutputFile.length() > 0) {
                log.info("pg_dump completed successfully. Output size: {} bytes", rawOutputFile.length());
                return rawOutputFile;
            }
            log.warn("pg_dump exited with code {}. Falling back to JDBC dump engine.", result.getExitCode());
        }

        log.info("Executing Pure Java JDBC PostgreSQL dump engine...");
        exportViaJdbc(creds, request.getSelectiveTables(), rawOutputFile);
        return rawOutputFile;
    }

    @Override
    public void performRestore(RestoreRequest request, File uncompressedSourceFile) throws Exception {
        DatabaseCredentials creds = request.getTargetCredentials();

        if (request.isDryRun()) {
            log.info("[DRY RUN] PostgreSQL restore preview to {}:{}/{}, source file size: {} bytes",
                    creds.getHost(), creds.getPort(), creds.getDatabaseName(), uncompressedSourceFile.length());
            return;
        }

        if (ProcessRunner.isCommandAvailable("psql")) {
            log.info("Using native 'psql' tool for PostgreSQL restore...");
            List<String> command = new ArrayList<>();
            command.add("psql");
            command.add("-h"); command.add(creds.getHost());
            command.add("-p"); command.add(String.valueOf(creds.getPort()));
            if (creds.getUsername() != null && !creds.getUsername().isEmpty()) {
                command.add("-U"); command.add(creds.getUsername());
            }
            command.add("-d"); command.add(creds.getDatabaseName());

            ProcessRunner.ProcessResult result = ProcessRunner.execute(command, null, uncompressedSourceFile, 15);
            if (result.isSuccess()) {
                log.info("psql restore completed successfully.");
                return;
            }
            log.warn("psql tool failed. Falling back to JDBC script execution.");
        }

        log.info("Executing Pure Java JDBC script execution for PostgreSQL restore...");
        restoreViaJdbc(creds, uncompressedSourceFile);
    }

    private void exportViaJdbc(DatabaseCredentials creds, List<String> selectiveTables, File outputFile) throws Exception {
        String url = buildJdbcUrl(creds);
        try (Connection conn = DriverManager.getConnection(url, creds.getUsername(), creds.getPassword());
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            writer.write("-- PostgreSQL Pure Java Dump\n\n");
            List<String> tablesToDump = (selectiveTables != null && !selectiveTables.isEmpty())
                    ? selectiveTables : getTables(creds);

            for (String table : tablesToDump) {
                writer.write("-- Dumping table " + table + "\n");
                writer.write("DROP TABLE IF EXISTS \"" + table + "\" CASCADE;\n");

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM \"" + table + "\"")) {
                    ResultSetMetaData md = rs.getMetaData();
                    int colCount = md.getColumnCount();

                    // Reconstruct table schema dynamically
                    StringBuilder createSql = new StringBuilder("CREATE TABLE \"" + table + "\" (");
                    for (int i = 1; i <= colCount; i++) {
                        createSql.append("\"").append(md.getColumnName(i)).append("\" ")
                                .append(md.getColumnTypeName(i));
                        if (i < colCount) createSql.append(", ");
                    }
                    createSql.append(");\n");
                    writer.write(createSql.toString());

                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO \"" + table + "\" VALUES (");
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
                if (line.trim().startsWith("--") || line.trim().isEmpty()) continue;
                sql.append(line).append("\n");
                if (line.trim().endsWith(";")) {
                    try {
                        stmt.execute(sql.toString());
                    } catch (SQLException e) {
                        log.warn("Warning executing SQL statement on PostgreSQL restore: {}", e.getMessage());
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
        return "jdbc:postgresql://" + creds.getHost() + ":" + creds.getPort() + "/" + creds.getDatabaseName();
    }
}
