package com.dbbackup.config;

import com.dbbackup.model.banking.*;
import com.dbbackup.repository.banking.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final BankAccountRepository accountRepository;
    private final BankingTransactionRepository transactionRepository;
    private final KYCDocumentRepository kycRepository;
    private final LoanRepository loanRepository;
    private final FDAccountRepository fdAccountRepository;
    private final CardRepository cardRepository;
    private final BillerRepository billerRepository;
    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final BankingAuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      CustomerRepository customerRepository,
                      EmployeeRepository employeeRepository,
                      BranchRepository branchRepository,
                      BankAccountRepository accountRepository,
                      BankingTransactionRepository transactionRepository,
                      KYCDocumentRepository kycRepository,
                      LoanRepository loanRepository,
                      FDAccountRepository fdAccountRepository,
                      CardRepository cardRepository,
                      BillerRepository billerRepository,
                      SupportTicketRepository ticketRepository,
                      TicketMessageRepository messageRepository,
                      FraudAlertRepository fraudAlertRepository,
                      BankingAuditLogRepository auditLogRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.kycRepository = kycRepository;
        this.loanRepository = loanRepository;
        this.fdAccountRepository = fdAccountRepository;
        this.cardRepository = cardRepository;
        this.billerRepository = billerRepository;
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            // Guarantee no legacy foreign names remain in existing persistent store
            customerRepository.findAll().forEach(c -> {
                if (c.getFirstName().equalsIgnoreCase("John") || c.getFirstName().equalsIgnoreCase("Jane")) {
                    c.setFirstName("Anshul");
                    c.setLastName("Bhilare");
                    customerRepository.save(c);
                }
            });
            return;
        }

        System.out.println("🌱 Seeding DB-Kavach Banking initial demo data...");

        // 1. Seed Branch
        Branch branch = new Branch("Main Financial Center Branch", "BR001", "100 Banking Boulevard", "Mumbai", "KAVC0001001", "Rajesh Verma", "+91-22-5550199");
        branch = branchRepository.save(branch);

        // 2. Super Admin User
        User superAdminUser = new User("superadmin", "superadmin@dbkavach.bank", passwordEncoder.encode("Admin123"), Role.SUPER_ADMIN);
        userRepository.save(superAdminUser);

        // 3. Bank Admin User
        User bankAdminUser = new User("admin", "admin@dbkavach.bank", passwordEncoder.encode("Admin123"), Role.BANK_ADMIN);
        userRepository.save(bankAdminUser);

        // 4. Employee User
        User employeeUser = new User("staff", "staff@dbkavach.bank", passwordEncoder.encode("Staff123"), Role.BANK_EMPLOYEE);
        userRepository.save(employeeUser);
        Employee employee = new Employee(employeeUser, branch, "EMP1001", "Senior Relationship Officer", "Customer Operations");
        employeeRepository.save(employee);

        // 5. Auditor User
        User auditorUser = new User("auditor1", "auditor@dbkavach.bank", passwordEncoder.encode("Auditor123"), Role.AUDITOR);
        userRepository.save(auditorUser);

        // 6. Customer 1: Anshul
        User anshulUser = new User("anshul", "anshul@example.com", passwordEncoder.encode("Anshul123"), Role.CUSTOMER);
        userRepository.save(anshulUser);
        Customer anshul = new Customer(anshulUser, "Anshul", "Bhilare", "+91-98765-01420", LocalDate.of(1990, 5, 15), "742 Financial Avenue, New Delhi");
        anshul.setKycStatus("APPROVED");
        anshul = customerRepository.save(anshul);

        // Accounts for Anshul
        BankAccount anshulSavings = new BankAccount("SAV1009876543", anshul, "SAVINGS", new BigDecimal("124500.00"), new BigDecimal("4.00"));
        accountRepository.save(anshulSavings);

        BankAccount anshulCurrent = new BankAccount("CUR2009876543", anshul, "CURRENT", new BigDecimal("45800.00"), new BigDecimal("0.00"));
        accountRepository.save(anshulCurrent);

        // 7. Customer 2: Dhruv Patel
        User dhruvUser = new User("dhruv_patel", "dhruv.patel@example.com", passwordEncoder.encode("Dhruv123"), Role.CUSTOMER);
        userRepository.save(dhruvUser);
        Customer dhruv = new Customer(dhruvUser, "Dhruv", "Patel", "+91-98765-43210", LocalDate.of(1992, 11, 20), "104 MG Road, Mumbai");
        dhruv.setKycStatus("APPROVED");
        dhruv = customerRepository.save(dhruv);

        BankAccount dhruvSavings = new BankAccount("SAV1001234567", dhruv, "SAVINGS", new BigDecimal("89200.00"), new BigDecimal("4.00"));
        accountRepository.save(dhruvSavings);

        // 8. Transactions
        BankingTransaction tx1 = new BankingTransaction("TXN9001", null, anshulSavings, new BigDecimal("50000.00"), "DEPOSIT", "OTHER", "Initial Account Funding");
        transactionRepository.save(tx1);

        BankingTransaction tx2 = new BankingTransaction("TXN9002", anshulSavings, dhruvSavings, new BigDecimal("12500.00"), "TRANSFER", "OTHER", "Project Consultation Payment");
        transactionRepository.save(tx2);

        BankingTransaction tx3 = new BankingTransaction("TXN9003", anshulSavings, null, new BigDecimal("3500.00"), "PAYMENT", "SHOPPING", "Electronics Store Purchase");
        transactionRepository.save(tx3);

        // 9. KYC Document
        KYCDocument kyc1 = new KYCDocument(anshul, "PASSPORT", "****5678", "/docs/passport_sample.pdf");
        kyc1.setStatus("APPROVED");
        kycRepository.save(kyc1);

        KYCDocument kyc2 = new KYCDocument(dhruv, "DRIVERS_LICENSE", "****1234", "/docs/dl_sample.pdf");
        kyc2.setStatus("UNDER_REVIEW");
        kycRepository.save(kyc2);

        // 10. Loan
        Loan loan1 = new Loan("LN500100", anshul, "PERSONAL", new BigDecimal("250000.00"), new BigDecimal("9.50"), 24, new BigDecimal("11477.50"));
        loan1.setStatus("ACTIVE");
        loanRepository.save(loan1);

        // 11. FD
        FDAccount fd1 = new FDAccount("FD800100", anshul, new BigDecimal("100000.00"), new BigDecimal("7.25"), 12, new BigDecimal("107450.00"), LocalDate.now().plusYears(1));
        fdAccountRepository.save(fd1);

        // 12. Cards
        Card card1 = new Card("4532 **** **** 8921", anshul, anshulSavings, "DEBIT", "VISA", LocalDate.now().plusYears(4), new BigDecimal("50000.00"));
        cardRepository.save(card1);

        Card card2 = new Card("5241 **** **** 3341", anshul, anshulSavings, "CREDIT", "MASTERCARD", LocalDate.now().plusYears(3), new BigDecimal("100000.00"));
        card2.setCreditLimit(new BigDecimal("150000.00"));
        cardRepository.save(card2);

        // 13. Billers
        Biller biller1 = new Biller("City Electric Power Corp", "ELECTRICITY", "BLR_ELEC01");
        billerRepository.save(biller1);

        Biller biller2 = new Biller("Metro Broadband Fiber", "INTERNET", "BLR_NET01");
        billerRepository.save(biller2);

        // 14. Support Ticket
        SupportTicket ticket1 = new SupportTicket("TCK1001", anshul, "Inquiry about Credit Card Points", "Cards", "MEDIUM");
        ticketRepository.save(ticket1);
        TicketMessage msg1 = new TicketMessage(ticket1, "anshul", "CUSTOMER", "Hello, how do I redeem reward points on my Mastercard?");
        messageRepository.save(msg1);

        // 15. Fraud Alert
        FraudAlert alert1 = new FraudAlert(tx2, anshul, 65, "HIGH", "High value transfer to new beneficiary outside regular business hours");
        alert1.setStatus("INVESTIGATING");
        fraudAlertRepository.save(alert1);

        // 16. Audit Log
        BankingAuditLog audit1 = new BankingAuditLog("admin", "BANK_ADMIN", "SECURITY_SCAN", "SYSTEM", "Initial automated banking system security scan completed", "SUCCESS");
        auditLogRepository.save(audit1);

        System.out.println("✅ Initial DB-Kavach Banking demo data successfully seeded!");
    }
}
