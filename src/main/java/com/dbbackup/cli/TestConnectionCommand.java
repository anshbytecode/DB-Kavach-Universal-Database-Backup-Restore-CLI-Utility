package com.dbbackup.cli;

import com.dbbackup.dbms.DbmsAdapter;
import com.dbbackup.dbms.DbmsAdapterFactory;
import com.dbbackup.model.DatabaseCredentials;
import com.dbbackup.model.DbmsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(
    name = "test-connection",
    description = "Validate database reachability and credentials before running backup or restore.",
    mixinStandardHelpOptions = true
)
public class TestConnectionCommand implements Callable<Integer> {

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

    @Option(names = {"--uri"}, description = "Database Connection URI")
    private String uri;

    private final DbmsAdapterFactory dbmsAdapterFactory;

    @Autowired
    public TestConnectionCommand(DbmsAdapterFactory dbmsAdapterFactory) {
        this.dbmsAdapterFactory = dbmsAdapterFactory;
    }

    @Override
    public Integer call() {
        try {
            DbmsType dbmsType = DbmsType.fromString(dbmsStr);
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
            credentials.setConnectionUri(uri);
            credentials.setFilePath(database);

            DbmsAdapter adapter = dbmsAdapterFactory.getAdapter(dbmsType);

            System.out.println("Testing connection to " + dbmsType + " at " + host + ":" + effectivePort + " [" + database + "]...");
            boolean success = adapter.testConnection(credentials);

            if (success) {
                System.out.println("-------------------------------------------------------------------------");
                System.out.println("✅ CONNECTION TEST SUCCESSFUL!");
                System.out.println("Status: Database is reachable and credentials are valid.");
                try {
                    List<String> tables = adapter.getTables(credentials);
                    System.out.println("Tables/Collections found: " + tables.size() + " " + tables);
                } catch (Exception ignored) {}
                System.out.println("-------------------------------------------------------------------------");
                return 0;
            } else {
                System.err.println("❌ CONNECTION TEST FAILED: Connection rejected.");
                return 1;
            }
        } catch (Exception e) {
            System.err.println("❌ CONNECTION TEST ERROR: " + e.getMessage());
            return 1;
        }
    }
}
