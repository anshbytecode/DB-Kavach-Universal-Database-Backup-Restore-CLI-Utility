package com.dbbackup.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TableFilterUtil {

    public static File filterSqlScriptForTables(File rawSqlFile, List<String> targetTables, File filteredOutputFile) throws IOException {
        if (targetTables == null || targetTables.isEmpty()) {
            return rawSqlFile;
        }

        Set<String> tableSet = targetTables.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        boolean writing = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(rawSqlFile), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filteredOutputFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim().toLowerCase();

                // Check for table creation or insertion headers
                if (trimmed.startsWith("create table") || trimmed.startsWith("drop table") || trimmed.startsWith("insert into") || trimmed.startsWith("lock tables")) {
                    writing = tableSet.stream().anyMatch(t -> trimmed.contains("`" + t + "`") || trimmed.contains("\"" + t + "\"") || trimmed.contains(" " + t + " ") || trimmed.contains(" " + t + ";") || trimmed.contains(" " + t + "("));
                }

                if (writing) {
                    writer.write(line);
                    writer.newLine();
                }

                if (trimmed.startsWith("unlock tables;") || (writing && trimmed.endsWith(";") && !trimmed.startsWith("insert into"))) {
                    // end of block for current table
                }
            }
        }

        return filteredOutputFile;
    }
}
