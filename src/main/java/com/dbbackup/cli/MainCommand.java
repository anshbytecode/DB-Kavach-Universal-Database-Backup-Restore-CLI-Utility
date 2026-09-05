package com.dbbackup.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
@Command(
    name = "db-backup",
    mixinStandardHelpOptions = true,
    version = "Database Backup Utility CLI 1.0.0",
    description = "Universal Database Backup and Restore CLI Utility supporting MySQL, PostgreSQL, MongoDB, SQLite, and Multi-Cloud Storage.",
    subcommands = {
        BackupCommand.class,
        RestoreCommand.class,
        TestConnectionCommand.class,
        HistoryCommand.class,
        ScheduleCommand.class,
        SecurityCommand.class,
        StatusCommand.class,
        DatabaseCommand.class,
        AuditCommand.class,
        VerifyBackupCommand.class,
        HelpCommand.class
    }
)
public class MainCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("=========================================================================");
        System.out.println("      DB-KAVACH BANKING & UNIVERSAL DATABASE UTILITY CLI                 ");
        System.out.println("=========================================================================");
        System.out.println("Usage: db-backup [COMMAND] [OPTIONS]");
        System.out.println();
        System.out.println("Available Subcommands:");
        System.out.println("  status          - Check system, database, and disaster recovery status");
        System.out.println("  database        - Manage and inspect connected database inventory");
        System.out.println("  audit           - View administrative security & banking audit trail");
        System.out.println("  verify-backup   - Verify integrity and checksum of backup archives");
        System.out.println("  backup          - Create a database backup with compression & storage options");
        System.out.println("  restore         - Restore a database from local or cloud backup");
        System.out.println("  security        - Database Security suite (audit, AES encryption, PII masking, vault)");
        System.out.println("  test-connection - Test reachability and credentials for target database");
        System.out.println("  history         - Display past backup and restore audit activity");
        System.out.println("  schedule        - Schedule recurring backup cron jobs");
        System.out.println("  help            - Display detailed command usage and help manual");
        System.out.println("=========================================================================");
        System.out.println("Type 'db-backup <command> --help' for specific command details.");
        return 0;
    }
}
