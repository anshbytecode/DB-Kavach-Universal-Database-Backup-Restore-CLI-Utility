package com.dbbackup.model.banking;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts", indexes = {
    @Index(name = "idx_acc_number", columnList = "accountNumber"),
    @Index(name = "idx_acc_customer", columnList = "customer_id"),
    @Index(name = "idx_acc_status", columnList = "status")
})
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 30)
    private String accountType; // SAVINGS, CURRENT, SALARY, FIXED_DEPOSIT, RECURRING_DEPOSIT

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(precision = 7, scale = 4)
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, PENDING, FROZEN, SUSPENDED, CLOSED

    private LocalDateTime createdAt = LocalDateTime.now();

    public BankAccount() {}

    public BankAccount(String accountNumber, Customer customer, String accountType, BigDecimal balance, BigDecimal interestRate) {
        this.accountNumber = accountNumber;
        this.customer = customer;
        this.accountType = accountType;
        this.balance = balance;
        this.availableBalance = balance;
        this.interestRate = interestRate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return "**** **** " + accountNumber.substring(accountNumber.length() - 4);
    }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
