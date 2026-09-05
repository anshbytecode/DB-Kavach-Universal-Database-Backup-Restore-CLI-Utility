package com.dbbackup.model.banking;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(nullable = false, length = 30)
    private String accountNumber;

    @Column(nullable = false, length = 100)
    private String beneficiaryName;

    @Column(nullable = false, length = 100)
    private String bankName;

    @Column(nullable = false, length = 20)
    private String ifscCode;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // PENDING, ACTIVE, REJECTED

    private LocalDateTime coolingPeriodEnd = LocalDateTime.now();

    private LocalDateTime createdAt = LocalDateTime.now();

    public Beneficiary() {}

    public Beneficiary(Customer customer, String nickname, String accountNumber, String beneficiaryName, String bankName, String ifscCode) {
        this.customer = customer;
        this.nickname = nickname;
        this.accountNumber = accountNumber;
        this.beneficiaryName = beneficiaryName;
        this.bankName = bankName;
        this.ifscCode = ifscCode;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return "**** **** " + accountNumber.substring(accountNumber.length() - 4);
    }

    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCoolingPeriodEnd() { return coolingPeriodEnd; }
    public void setCoolingPeriodEnd(LocalDateTime coolingPeriodEnd) { this.coolingPeriodEnd = coolingPeriodEnd; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
