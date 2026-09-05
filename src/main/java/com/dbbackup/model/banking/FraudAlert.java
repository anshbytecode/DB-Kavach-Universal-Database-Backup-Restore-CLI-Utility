package com.dbbackup.model.banking;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id")
    private BankingTransaction transaction;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private int riskScore; // 0 - 100

    @Column(nullable = false, length = 20)
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(nullable = false, columnDefinition = "TEXT")
    private String triggerReason;

    @Column(nullable = false, length = 30)
    private String status = "NEW"; // NEW, INVESTIGATING, RESOLVED, ESCALATED, FROZEN_ACCOUNT

    private LocalDateTime createdAt = LocalDateTime.now();

    public FraudAlert() {}

    public FraudAlert(BankingTransaction transaction, Customer customer, int riskScore, String riskLevel, String triggerReason) {
        this.transaction = transaction;
        this.customer = customer;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.triggerReason = triggerReason;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BankingTransaction getTransaction() { return transaction; }
    public void setTransaction(BankingTransaction transaction) { this.transaction = transaction; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getTriggerReason() { return triggerReason; }
    public void setTriggerReason(String triggerReason) { this.triggerReason = triggerReason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
