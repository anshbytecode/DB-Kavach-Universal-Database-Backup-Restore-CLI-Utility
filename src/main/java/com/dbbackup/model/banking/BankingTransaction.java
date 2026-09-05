package com.dbbackup.model.banking;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "banking_transactions", indexes = {
    @Index(name = "idx_tx_id", columnList = "transactionId"),
    @Index(name = "idx_tx_source", columnList = "source_account_id"),
    @Index(name = "idx_tx_target", columnList = "target_account_id"),
    @Index(name = "idx_tx_status", columnList = "status"),
    @Index(name = "idx_tx_timestamp", columnList = "timestamp")
})
public class BankingTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String transactionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_account_id")
    private BankAccount sourceAccount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_account_id")
    private BankAccount targetAccount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    private String type; // DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, REFUND, LOAN_EMI, INTEREST_CREDIT, FEE, CARD_PAYMENT

    @Column(nullable = false, length = 20)
    private String status = "SUCCESS"; // PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED

    @Column(length = 30)
    private String category = "OTHER"; // FOOD, SHOPPING, BILLS, TRAVEL, EDUCATION, ENTERTAINMENT, OTHER

    private String description;
    private String referenceNumber;

    @Column(unique = true, length = 100)
    private String idempotencyKey;

    private int riskScore = 0; // 0 - 100
    private String failureReason;

    private LocalDateTime timestamp = LocalDateTime.now();

    public BankingTransaction() {}

    public BankingTransaction(String transactionId, BankAccount sourceAccount, BankAccount targetAccount, BigDecimal amount, String type, String category, String description) {
        this.transactionId = transactionId;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public BankAccount getSourceAccount() { return sourceAccount; }
    public void setSourceAccount(BankAccount sourceAccount) { this.sourceAccount = sourceAccount; }

    public BankAccount getTargetAccount() { return targetAccount; }
    public void setTargetAccount(BankAccount targetAccount) { this.targetAccount = targetAccount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
