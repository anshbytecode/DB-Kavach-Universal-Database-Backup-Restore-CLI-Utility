package com.dbbackup.dbms;

import com.dbbackup.model.BackupRequest;
import com.dbbackup.model.DatabaseCredentials;
import com.dbbackup.model.DbmsType;
import com.dbbackup.model.RestoreRequest;
import com.dbbackup.util.ProcessRunner;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class MongoDbAdapter implements DbmsAdapter {
    private static final Logger log = LoggerFactory.getLogger(MongoDbAdapter.class);

    @Override
    public DbmsType getType() {
        return DbmsType.MONGODB;
    }

    @Override
    public boolean testConnection(DatabaseCredentials credentials) throws Exception {
        String uri = buildConnectionString(credentials);
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase db = mongoClient.getDatabase(credentials.getDatabaseName() != null ? credentials.getDatabaseName() : "admin");
            Document ping = db.runCommand(new Document("ping", 1));
            return ping.getDouble("ok") == 1.0 || ping.getInteger("ok", 0) == 1;
            
        } catch (Exception e) {
            log.error("MongoDB connection test failed: {}", e.getMessage());
            throw new Exception("Failed to connect to MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getTables(DatabaseCredentials credentials) throws Exception {
        
        List<String> collections = new ArrayList<>();
        String uri = buildConnectionString(credentials);
        try (MongoClient mongoClient = MongoClients.create(uri)) {

            
            MongoDatabase db = mongoClient.getDatabase(credentials.getDatabaseName());
            for (String name : db.listCollectionNames()) {
                collections.add(name);
            }
        }
        return collections;
    }

    @Override
    public File performBackup(BackupRequest request, File rawOutputFile) throws Exception {
        
        DatabaseCredentials creds = request.getCredentials();

        if (ProcessRunner.isCommandAvailable("mongodump")) {
            
            log.info("Using native 'mongodump' CLI tool for MongoDB backup...");
            List<String> command = new ArrayList<>();
            command.add("mongodump");
            command.add("--host=" + creds.getHost());
            command.add("--port=" + creds.getPort());
            if (creds.getUsername() != null && !creds.getUsername().isEmpty()) {
                
                command.add("--username=" + creds.getUsername());
                command.add("--password=" + creds.getPassword());
            }
            command.add("--db=" + creds.getDatabaseName());
            command.add("--archive=" + rawOutputFile.getAbsolutePath());

            if (request.getSelectiveTables() != null && !request.getSelectiveTables().isEmpty()) {
                command.add("--collection=" + request.getSelectiveTables().get(0));
            }

            ProcessRunner.ProcessResult result = ProcessRunner.execute(command, null, null, 15);
            if (result.isSuccess() && rawOutputFile.length() > 0) {
                log.info("mongodump completed successfully. File size: {} bytes", rawOutputFile.length());
                return rawOutputFile;
            }
            log.warn("mongodump exited with code {}. Falling back to Java Mongo JSON export.", result.getExitCode());
        }

        log.info("Executing Pure Java MongoDB JSON export engine...");
        exportViaMongoDriver(creds, request.getSelectiveTables(), rawOutputFile);
        return rawOutputFile;
    }

    @Override
    public void performRestore(RestoreRequest request, File uncompressedSourceFile) throws Exception {
        DatabaseCredentials creds = request.getTargetCredentials();

        if (request.isDryRun()) {
            log.info("[DRY RUN] MongoDB restore preview to {}:{}/{}, source size: {} bytes",
                    creds.getHost(), creds.getPort(), creds.getDatabaseName(), uncompressedSourceFile.length());
            return;
        }

        if (ProcessRunner.isCommandAvailable("mongorestore")) {
            log.info("Using native 'mongorestore' CLI tool...");
            List<String> command = new ArrayList<>();
            command.add("mongorestore");
            command.add("--host=" + creds.getHost());
            command.add("--port=" + creds.getPort());
            if (creds.getUsername() != null && !creds.getUsername().isEmpty()) {
                command.add("--username=" + creds.getUsername());
                command.add("--password=" + creds.getPassword());
            }
            command.add("--db=" + creds.getDatabaseName());
            command.add("--archive=" + uncompressedSourceFile.getAbsolutePath());

            ProcessRunner.ProcessResult result = ProcessRunner.execute(command, null, null, 15);
            if (result.isSuccess()) {
                log.info("mongorestore completed successfully.");
                return;
            }
            log.warn("mongorestore failed. Falling back to Java Mongo Driver JSON import.");
        }

        log.info("Executing Pure Java MongoDB JSON import engine...");
        restoreViaMongoDriver(creds, uncompressedSourceFile);
    }

    private void exportViaMongoDriver(DatabaseCredentials creds, List<String> selectiveCollections, File outputFile) throws Exception {
        String uri = buildConnectionString(creds);
        try (MongoClient mongoClient = MongoClients.create(uri);
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            MongoDatabase db = mongoClient.getDatabase(creds.getDatabaseName());

            List<String> collectionsToDump = (selectiveCollections != null && !selectiveCollections.isEmpty())
                    ? selectiveCollections : getTables(creds);

            for (String collName : collectionsToDump) {
                MongoCollection<Document> coll = db.getCollection(collName);
                writer.write("// COLLECTION:" + collName + "\n");
                for (Document doc : coll.find()) {
                    writer.write(doc.toJson() + "\n");
                }
            }
        }
    }

    private void restoreViaMongoDriver(DatabaseCredentials creds, File jsonFile) throws Exception {
        String uri = buildConnectionString(creds);
        try (MongoClient mongoClient = MongoClients.create(uri);
             BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            MongoDatabase db = mongoClient.getDatabase(creds.getDatabaseName());

            String currentCollName = "restored_collection";
            String line;
            List<Document> batch = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("// COLLECTION:")) {
                    if (!batch.isEmpty()) {
                        db.getCollection(currentCollName).insertMany(batch);
                        batch.clear();
                    }
                    currentCollName = line.replace("// COLLECTION:", "").trim();
                    continue;
                }
                if (!line.trim().isEmpty()) {
                    Document doc = Document.parse(line);
                    batch.add(doc);
                }
            }
            if (!batch.isEmpty()) {
                db.getCollection(currentCollName).insertMany(batch);
            }
        }
    }

    private String buildConnectionString(DatabaseCredentials creds) {
        if (creds.getConnectionUri() != null && !creds.getConnectionUri().isEmpty()) {
            return creds.getConnectionUri();
        }
        int port = creds.getPort() > 0 ? creds.getPort() : 27017;
        if (creds.getUsername() != null && !creds.getUsername().isEmpty()) {
            return "mongodb://" + creds.getUsername() + ":" + creds.getPassword() + "@" + creds.getHost() + ":" + port;
        }
        return "mongodb://" + creds.getHost() + ":" + port;
    }
}
