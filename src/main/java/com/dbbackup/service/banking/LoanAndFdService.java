package com.dbbackup.service.banking;

import com.dbbackup.model.banking.*;
import com.dbbackup.repository.banking.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanAndFdService {

    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final FDAccountRepository fdAccountRepository;
    private final CustomerRepository customerRepository;
    private final BankAccountRepository accountRepository;
    private final FinancialMathService mathService;
    private final BankingAuditService auditService;

    public LoanAndFdService(LoanRepository loanRepository,
                           LoanPaymentRepository loanPaymentRepository,
                           FDAccountRepository fdAccountRepository,
                           CustomerRepository customerRepository,
                           BankAccountRepository accountRepository,
                           FinancialMathService mathService,
                           BankingAuditService auditService) {
        this.loanRepository = loanRepository;
        this.loanPaymentRepository = loanPaymentRepository;
        this.fdAccountRepository = fdAccountRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.mathService = mathService;
        this.auditService = auditService;
    }

    @Transactional
    public Loan applyForLoan(Long customerId, String loanType, BigDecimal amount, BigDecimal interestRate, int tenureMonths) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        BigDecimal rate = interestRate != null ? interestRate : new BigDecimal("9.50");
        BigDecimal emi = mathService.calculateMonthlyEmi(amount, rate, tenureMonths);

        String loanNo = "LN" + (1000000000L + (long)(Math.random() * 9000000000L));
        Loan loan = new Loan(loanNo, customer, loanType.toUpperCase(), amount, rate, tenureMonths, emi);
        loan = loanRepository.save(loan);

        auditService.log(customer.getUser().getUsername(), "CUSTOMER", "LOAN_APPLIED", loanNo, "Applied for " + loanType + " loan of " + amount, "SUCCESS");
        return loan;
    }

    @Transactional
    public Loan approveLoan(Long loanId, String adminUser) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found"));

        loan.setStatus("APPROVED");
        loan.setApprovedDate(LocalDateTime.now());
        loanRepository.save(loan);

        // Credit loan amount to customer's savings account if present
        List<BankAccount> accounts = accountRepository.findByCustomerId(loan.getCustomer().getId());
        if (!accounts.isEmpty()) {
            BankAccount mainAcc = accounts.get(0);
            mainAcc.setBalance(mainAcc.getBalance().add(loan.getPrincipalAmount()));
            mainAcc.setAvailableBalance(mainAcc.getAvailableBalance().add(loan.getPrincipalAmount()));
            accountRepository.save(mainAcc);
        }

        auditService.log(adminUser, "ADMIN", "LOAN_APPROVED", loan.getLoanNumber(), "Approved loan of " + loan.getPrincipalAmount(), "SUCCESS");
        return loan;
    }

    @Transactional
    public FDAccount createFD(Long customerId, BigDecimal principal, BigDecimal interestRate, int tenureMonths) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        List<BankAccount> accounts = accountRepository.findByCustomerId(customerId);
        if (accounts.isEmpty()) {
            throw new IllegalStateException("No active bank account to fund FD");
        }
        BankAccount acc = accounts.get(0);
        if (acc.getAvailableBalance().compareTo(principal) < 0) {
            throw new IllegalStateException("Insufficient funds to open Fixed Deposit");
        }

        // Deduct principal
        acc.setBalance(acc.getBalance().subtract(principal));
        acc.setAvailableBalance(acc.getAvailableBalance().subtract(principal));
        accountRepository.save(acc);

        BigDecimal rate = interestRate != null ? interestRate : new BigDecimal("7.25");
        BigDecimal maturityAmount = mathService.calculateFdMaturity(principal, rate, tenureMonths);
        LocalDate maturityDate = LocalDate.now().plusMonths(tenureMonths);

        String fdNo = "FD" + (1000000000L + (long)(Math.random() * 9000000000L));
        FDAccount fd = new FDAccount(fdNo, customer, principal, rate, tenureMonths, maturityAmount, maturityDate);
        fd = fdAccountRepository.save(fd);

        auditService.log(customer.getUser().getUsername(), "CUSTOMER", "FD_CREATED", fdNo, "Opened FD of " + principal + " maturing at " + maturityAmount, "SUCCESS");
        return fd;
    }

    public List<Loan> getCustomerLoans(Long customerId) {
        return loanRepository.findByCustomerId(customerId);
    }

    public List<FDAccount> getCustomerFDs(Long customerId) {
        return fdAccountRepository.findByCustomerId(customerId);
    }
}
