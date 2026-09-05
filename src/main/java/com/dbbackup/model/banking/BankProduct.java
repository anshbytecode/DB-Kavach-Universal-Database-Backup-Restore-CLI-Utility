package com.dbbackup.model.banking;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "bank_products")
public class BankProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String productType; // SAVINGS, CURRENT, FD, LOAN, CARD

    @Column(precision = 7, scale = 4)
    private BigDecimal interestRate;

    @Column(precision = 19, scale = 4)
    private BigDecimal minAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal maxAmount;

    private Integer tenureMonths;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    public BankProduct() {}

    public BankProduct(String name, String productType, BigDecimal interestRate, BigDecimal minAmount, BigDecimal maxAmount, Integer tenureMonths, BigDecimal fee) {
        this.name = name;
        this.productType = productType;
        this.interestRate = interestRate;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.tenureMonths = tenureMonths;
        this.fee = fee;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
