package com.dbbackup.service.banking;

import com.dbbackup.model.banking.BankAccount;
import com.dbbackup.model.banking.Customer;
import com.dbbackup.repository.banking.BankAccountRepository;
import com.dbbackup.repository.banking.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final BankAccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final BankingAuditService auditService;

    public AccountService(BankAccountRepository accountRepository,
                          CustomerRepository customerRepository,
                          BankingAuditService auditService) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    public List<BankAccount> getAccountsByCustomer(Long customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    public BankAccount getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
    }

    @Transactional
    public BankAccount createAccount(Long customerId, String accountType, BigDecimal initialDeposit, BigDecimal interestRate) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        String prefix = accountType.equalsIgnoreCase("SAVINGS") ? "SAV" :
                        accountType.equalsIgnoreCase("CURRENT") ? "CUR" :
                        accountType.equalsIgnoreCase("SALARY") ? "SAL" : "ACC";

        String accNo = prefix + (1000000000L + (long)(Math.random() * 9000000000L));
        BigDecimal deposit = initialDeposit != null ? initialDeposit : BigDecimal.ZERO;
        BigDecimal rate = interestRate != null ? interestRate : new BigDecimal("3.50");

        BankAccount account = new BankAccount(accNo, customer, accountType.toUpperCase(), deposit, rate);
        account = accountRepository.save(account);

        auditService.log(customer.getUser().getUsername(), "CUSTOMER", "ACCOUNT_CREATED", accNo, "Created " + accountType + " account with deposit " + deposit, "SUCCESS");
        return account;
    }

    @Transactional
    public BankAccount updateAccountStatus(String accountNumber, String status, String performedBy) {
        BankAccount account = getAccountByNumber(accountNumber);
        String oldStatus = account.getStatus();
        account.setStatus(status.toUpperCase());
        accountRepository.save(account);

        auditService.log(performedBy, "ADMIN", "ACCOUNT_STATUS_CHANGE", accountNumber, "Status changed from " + oldStatus + " to " + status, "SUCCESS");
        return account;
    }
}
