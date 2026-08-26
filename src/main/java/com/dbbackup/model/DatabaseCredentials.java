package com.dbbackup.model;

public class DatabaseCredentials {
    private DbmsType dbmsType;
    private String host = "localhost";
    private int port = 3306;
    private String databaseName;
    private String username;
    private String password;
    private String connectionUri;
    private String filePath; // For SQLite or local DB files

    public DatabaseCredentials() {}

    public DatabaseCredentials(DbmsType dbmsType, String host, int port, String databaseName, String username, String password) {
        this.dbmsType = dbmsType;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
    }

    public DbmsType getDbmsType() { return dbmsType; }
    public void setDbmsType(DbmsType dbmsType) { this.dbmsType = dbmsType; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConnectionUri() { return connectionUri; }
    public void setConnectionUri(String connectionUri) { this.connectionUri = connectionUri; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    @Override
    public String toString() {
        return "DatabaseCredentials{" +
                "dbmsType=" + dbmsType +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", databaseName='" + databaseName + '\'' +
                ", username='" + username + '\'' +
                ", connectionUri='" + (connectionUri != null ? "[CONFIGURED]" : "null") + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
