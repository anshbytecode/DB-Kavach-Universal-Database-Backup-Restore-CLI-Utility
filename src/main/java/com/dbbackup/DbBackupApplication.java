package com.dbbackup;

import com.dbbackup.cli.MainCommand;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DbBackupApplication implements CommandLineRunner, ExitCodeGenerator {

    private final MainCommand mainCommand;
    private final IFactory factory;
    private int exitCode = 0;

    public DbBackupApplication(MainCommand mainCommand, IFactory factory) {
        this.mainCommand = mainCommand;
        this.factory = factory;
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            System.exit(SpringApplication.exit(SpringApplication.run(DbBackupApplication.class, args)));
        } else {
            SpringApplication.run(DbBackupApplication.class, args);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        if (args != null && args.length > 0) {
            exitCode = new CommandLine(mainCommand, factory).execute(args);
        } else {
            System.out.println("=========================================================================");
            System.out.println(" 🛡️  DB KAVACH - UNIVERSAL DATABASE SECURITY & BACKUP UTILITY          ");
            System.out.println("=========================================================================");
            String activePort = System.getProperty("server.port", System.getenv("PORT") != null ? System.getenv("PORT") : "8080");
            System.out.println(" 🌐 Web Server Started: http://localhost:" + activePort + "/");
            System.out.println(" 🚀 Keep this terminal window open to access the Web Dashboard UI.");
            System.out.println(" 💡 Press Ctrl+C anytime to stop the Web Server.");
            System.out.println(" 💡 To run CLI commands, execute: java -jar target/db-backup-cli-1.0.0.jar <command>");
            System.out.println("=========================================================================");
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
