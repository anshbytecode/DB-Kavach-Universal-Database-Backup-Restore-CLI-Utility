package com.dbbackup.model.banking;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String cardNumberMasked;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    private BankAccount bankAccount;

    @Column(nullable = false, length = 20)
    private String cardType; // DEBIT, CREDIT

    @Column(nullable = false, length = 20)
    private String cardNetwork = "VISA"; // VISA, MASTERCARD, RUPAY

    private LocalDate expiryDate;

    @Column(precision = 19, scale = 4)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4)
    private BigDecimal dailyLimit = new BigDecimal("50000.00");

    @Column(nullable = false)
    private boolean frozen = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Card() {}

    public Card(String cardNumberMasked, Customer customer, BankAccount bankAccount, String cardType, String cardNetwork, LocalDate expiryDate, BigDecimal dailyLimit) {
        this.cardNumberMasked = cardNumberMasked;
        this.customer = customer;
        this.bankAccount = bankAccount;
        this.cardType = cardType;
        this.cardNetwork = cardNetwork;
        this.expiryDate = expiryDate;
        this.dailyLimit = dailyLimit;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCardNumberMasked() { return cardNumberMasked; }
    public void setCardNumberMasked(String cardNumberMasked) { this.cardNumberMasked = cardNumberMasked; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public BankAccount getBankAccount() { return bankAccount; }
    public void setBankAccount(BankAccount bankAccount) { this.bankAccount = bankAccount; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getCardNetwork() { return cardNetwork; }
    public void setCardNetwork(String cardNetwork) { this.cardNetwork = cardNetwork; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
