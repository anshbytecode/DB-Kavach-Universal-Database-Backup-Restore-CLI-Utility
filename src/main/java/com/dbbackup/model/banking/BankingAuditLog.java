package com.dbbackup.model.banking;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "banking_audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "username"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class BankingAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 30)
    private String role;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 100)
    private String targetResource;

    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, length = 20)
    private String status = "SUCCESS";

    private LocalDateTime timestamp = LocalDateTime.now();

    public BankingAuditLog() {}

    public BankingAuditLog(String username, String role, String action, String targetResource, String details, String status) {
        this.username = username;
        this.role = role;
        this.action = action;
        this.targetResource = targetResource;
        this.details = details;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetResource() { return targetResource; }
    public void setTargetResource(String targetResource) { this.targetResource = targetResource; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
