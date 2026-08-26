package com.dbbackup.model;

public class CredentialProfile {
    private String profileName;
    private DbmsType dbmsType;
    private String host;
    private int port;
    private String databaseName;
    private String username;
    private String password;
    private String connectionUri;
    private String filePath;

    public CredentialProfile() {}

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

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

    public DatabaseCredentials toDatabaseCredentials() {
        DatabaseCredentials creds = new DatabaseCredentials();
        creds.setDbmsType(this.dbmsType);
        creds.setHost(this.host);
        creds.setPort(this.port);
        creds.setDatabaseName(this.databaseName);
        creds.setUsername(this.username);
        creds.setPassword(this.password);
        creds.setConnectionUri(this.connectionUri);
        creds.setFilePath(this.filePath != null ? this.filePath : this.databaseName);
        return creds;
    }
}
