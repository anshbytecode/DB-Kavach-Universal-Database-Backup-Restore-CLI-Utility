package com.dbbackup.cli;

import com.dbbackup.model.BackupHistoryRecord;
import com.dbbackup.model.DbmsType;
import com.dbbackup.service.logging.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(
    name = "history",
    description = "View audit history of recent backup and restore operations.",
    mixinStandardHelpOptions = true
)
public class HistoryCommand implements Callable<Integer> {

    @Option(names = {"--status"}, description = "Filter by status: SUCCESS, FAILED")
    private String status;

    @Option(names = {"--dbms"}, description = "Filter by DBMS: MYSQL, POSTGRESQL, MONGODB, SQLITE")
    private String dbmsStr;

    private final AuditLogService auditLogService;

    @Autowired
    public HistoryCommand(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public Integer call() {
        try {
            List<BackupHistoryRecord> records;
            if (status != null && !status.isEmpty()) {
                records = auditLogService.getHistoryByStatus(status.toUpperCase());
            } else if (dbmsStr != null && !dbmsStr.isEmpty()) {
                records = auditLogService.getHistoryByDbms(DbmsType.fromString(dbmsStr));
            } else {
                records = auditLogService.getRecentHistory();
            }

            System.out.println("========================================================================================================================");
            System.out.println(String.format("%-10s | %-8s | %-10s | %-15s | %-8s | %-8s | %-10s | %-12s",
                    "ID", "OPERATION", "DBMS", "DATABASE", "STATUS", "SIZE(B)", "DURATION(ms)", "START TIME"));
            System.out.println("========================================================================================================================");

            if (records.isEmpty()) {
                System.out.println("No backup history records found.");
            } else {
                for (BackupHistoryRecord r : records) {
                    String timeStr = r.getStartTime() != null ? r.getStartTime().toString().replace("T", " ").substring(0, 19) : "N/A";
                    System.out.println(String.format("%-10s | %-8s | %-10s | %-15s | %-8s | %-8d | %-12d | %-12s",
                            r.getBackupId() != null ? r.getBackupId() : "-",
                            r.getOperation() != null ? r.getOperation() : "-",
                            r.getDbmsType() != null ? r.getDbmsType() : "-",
                            r.getDatabaseName() != null ? r.getDatabaseName() : "-",
                            r.getStatus() != null ? r.getStatus() : "-",
                            r.getSizeBytes(),
                            r.getDurationMs(),
                            timeStr));
                }
            }
            System.out.println("========================================================================================================================");
            return 0;

        } catch (Exception e) {
            System.err.println("Error fetching history: " + e.getMessage());
            return 1;
        }
    }
}
