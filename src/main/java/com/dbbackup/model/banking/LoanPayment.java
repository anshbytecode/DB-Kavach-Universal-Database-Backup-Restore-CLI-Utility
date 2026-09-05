package com.dbbackup.model.banking;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_payments")
public class LoanPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id")
    private BankingTransaction transaction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal principalPaid;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal interestPaid;

    private LocalDateTime paymentDate = LocalDateTime.now();

    public LoanPayment() {}

    public LoanPayment(Loan loan, BankingTransaction transaction, BigDecimal amount, BigDecimal principalPaid, BigDecimal interestPaid) {
        this.loan = loan;
        this.transaction = transaction;
        this.amount = amount;
        this.principalPaid = principalPaid;
        this.interestPaid = interestPaid;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }

    public BankingTransaction getTransaction() { return transaction; }
    public void setTransaction(BankingTransaction transaction) { this.transaction = transaction; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getPrincipalPaid() { return principalPaid; }
    public void setPrincipalPaid(BigDecimal principalPaid) { this.principalPaid = principalPaid; }

    public BigDecimal getInterestPaid() { return interestPaid; }
    public void setInterestPaid(BigDecimal interestPaid) { this.interestPaid = interestPaid; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}
