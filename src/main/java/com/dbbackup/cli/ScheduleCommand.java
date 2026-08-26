package com.dbbackup.cli;

import com.dbbackup.model.*;
import com.dbbackup.service.scheduler.BackupSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Component
@Command(
    name = "schedule",
    description = "Schedule automated recurring database backup jobs with cron expressions.",
    mixinStandardHelpOptions = true
)
public class ScheduleCommand implements Callable<Integer> {

    @Option(names = {"--cron"}, required = true, description = "Cron expression (e.g., '0 0 2 * * ?' for daily 2am, or '*/10 * * * * ?' for every 10 seconds)")
    private String cronExpression;

    @Option(names = {"--dbms"}, required = true, description = "DBMS Type: MYSQL, POSTGRESQL, MONGODB, SQLITE")
    private String dbmsStr;

    @Option(names = {"-h", "--host"}, description = "Database Host", defaultValue = "localhost")
    private String host;

    @Option(names = {"-P", "--port"}, description = "Database Port", defaultValue = "0")
    private int port;

    @Option(names = {"-d", "--database"}, required = true, description = "Database Name or SQLite path")
    private String database;

    @Option(names = {"-u", "--username"}, description = "Database Username")
    private String username;

    @Option(names = {"-p", "--password"}, description = "Database Password", interactive = true, arity = "0..1")
    private String password;

    @Option(names = {"--type"}, description = "Backup type: FULL, INCREMENTAL, DIFFERENTIAL (default: FULL)", defaultValue = "FULL")
    private String backupTypeStr;

    @Option(names = {"--compression"}, description = "Compression type: GZIP, ZIP, TAR_GZ, NONE (default: GZIP)", defaultValue = "GZIP")
    private String compressionStr;

    @Option(names = {"--storage"}, description = "Storage destination: LOCAL, S3, GCS, AZURE (default: LOCAL)", defaultValue = "LOCAL")
    private String storageStr;

    private final BackupSchedulerService schedulerService;

    @Autowired
    public ScheduleCommand(BackupSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Override
    public Integer call() {
        try {
            DbmsType dbmsType = DbmsType.fromString(dbmsStr);
            BackupType backupType = BackupType.fromString(backupTypeStr);
            CompressionType compressionType = CompressionType.fromString(compressionStr);
            StorageType storageType = StorageType.fromString(storageStr);

            int effectivePort = port;
            if (effectivePort == 0) {
                effectivePort = switch (dbmsType) {
                    case MYSQL -> 3306;
                    case POSTGRESQL -> 5432;
                    case MONGODB -> 27017;
                    case SQLITE -> 0;
                };
            }

            DatabaseCredentials credentials = new DatabaseCredentials();
            credentials.setDbmsType(dbmsType);
            credentials.setHost(host);
            credentials.setPort(effectivePort);
            credentials.setDatabaseName(database);
            credentials.setUsername(username);
            credentials.setPassword(password);
            credentials.setFilePath(database);

            BackupRequest request = new BackupRequest();
            request.setCredentials(credentials);
            request.setBackupType(backupType);
            request.setCompressionType(compressionType);
            request.setStorageType(storageType);

            String jobId = schedulerService.scheduleBackup(request, cronExpression);

            System.out.println("-------------------------------------------------------------------------");
            System.out.println("⏰ BACKUP SCHEDULER STARTED!");
            System.out.println("Job ID         : " + jobId);
            System.out.println("Cron Schedule  : " + cronExpression);
            System.out.println("DBMS Target    : " + dbmsType + " [" + database + "]");
            System.out.println("Storage        : " + storageType);
            System.out.println("-------------------------------------------------------------------------");
            System.out.println("Scheduler is active in background context...");
            return 0;

        } catch (Exception e) {
            System.err.println("❌ SCHEDULER ERROR: " + e.getMessage());
            return 1;
        }
    }
}
