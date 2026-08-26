package com.dbbackup.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SecurityAuditReport {

    public enum AuditSeverity {
        HIGH, MEDIUM, LOW, INFO
    }

    public static class AuditFinding {
        private String category;
        private AuditSeverity severity;
        private String title;
        private String description;
        private String recommendation;

        public AuditFinding() {}

        public AuditFinding(String category, AuditSeverity severity, String title, String description, String recommendation) {
            this.category = category;
            this.severity = severity;
            this.title = title;
            this.description = description;
            this.recommendation = recommendation;
        }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public AuditSeverity getSeverity() { return severity; }
        public void setSeverity(AuditSeverity severity) { this.severity = severity; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }

    private String dbmsType;
    private String databaseName;
    private String host;
    private int port;
    private LocalDateTime timestamp;
    private int score; // 0 to 100
    private String rating; // A+, A, B, C, F
    private List<AuditFinding> findings = new ArrayList<>();

    public SecurityAuditReport() {
        this.timestamp = LocalDateTime.now();
    }

    public String getDbmsType() { return dbmsType; }
    public void setDbmsType(String dbmsType) { this.dbmsType = dbmsType; }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public List<AuditFinding> getFindings() { return findings; }
    public void setFindings(List<AuditFinding> findings) { this.findings = findings; }

    public void addFinding(AuditFinding finding) {
        this.findings.add(finding);
    }
}
