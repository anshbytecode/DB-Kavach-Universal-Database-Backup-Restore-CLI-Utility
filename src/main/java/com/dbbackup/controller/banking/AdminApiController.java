package com.dbbackup.controller.banking;

import com.dbbackup.model.banking.*;
import com.dbbackup.repository.banking.*;
import com.dbbackup.service.banking.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final CustomerRepository customerRepository;
    private final BankAccountRepository accountRepository;
    private final BankingTransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final KYCDocumentRepository kycRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final AccountService accountService;
    private final LoanAndFdService loanAndFdService;
    private final FraudDetectionService fraudDetectionService;
    private final KYCAndSupportService kycAndSupportService;
    private final BankingAuditService auditService;

    public AdminApiController(CustomerRepository customerRepository,
                             BankAccountRepository accountRepository,
                             BankingTransactionRepository transactionRepository,
                             LoanRepository loanRepository,
                             KYCDocumentRepository kycRepository,
                             FraudAlertRepository fraudAlertRepository,
                             UserRepository userRepository,
                             BranchRepository branchRepository,
                             AccountService accountService,
                             LoanAndFdService loanAndFdService,
                             FraudDetectionService fraudDetectionService,
                             KYCAndSupportService kycAndSupportService,
                             BankingAuditService auditService) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.loanRepository = loanRepository;
        this.kycRepository = kycRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.accountService = accountService;
        this.loanAndFdService = loanAndFdService;
        this.fraudDetectionService = fraudDetectionService;
        this.kycAndSupportService = kycAndSupportService;
        this.auditService = auditService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getAdminMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        long totalCustomers = customerRepository.count();
        long totalAccounts = accountRepository.count();
        BigDecimal totalDeposits = accountRepository.sumTotalDeposits();
        if (totalDeposits == null) totalDeposits = BigDecimal.ZERO;

        BigDecimal totalWithdrawals = transactionRepository.sumAmountByType("WITHDRAWAL");
        if (totalWithdrawals == null) totalWithdrawals = BigDecimal.ZERO;

        long totalTransactions = transactionRepository.count();
        long pendingKyc = kycRepository.countByStatus("UNDER_REVIEW");
        long activeLoansCount = loanRepository.countByStatus("ACTIVE");
        BigDecimal totalActiveLoanAmount = loanRepository.sumTotalActiveLoans();
        if (totalActiveLoanAmount == null) totalActiveLoanAmount = BigDecimal.ZERO;

        long suspiciousAlerts = fraudAlertRepository.countByStatus("INVESTIGATING") + fraudAlertRepository.countByStatus("NEW");

        metrics.put("totalCustomers", totalCustomers);
        metrics.put("totalAccounts", totalAccounts);
        metrics.put("totalDeposits", totalDeposits);
        metrics.put("totalWithdrawals", totalWithdrawals);
        metrics.put("totalTransactions", totalTransactions);
        metrics.put("pendingKyc", pendingKyc);
        metrics.put("activeLoansCount", activeLoansCount);
        metrics.put("totalActiveLoanAmount", totalActiveLoanAmount);
        metrics.put("suspiciousAlerts", suspiciousAlerts);
        metrics.put("systemSecurityScore", 98);

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/customers")
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerRepository.findAll());
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccount>> getAllAccounts() {
        return ResponseEntity.ok(accountRepository.findAll());
    }

    @PostMapping("/accounts/{accountNumber}/status")
    public ResponseEntity<?> updateAccountStatus(@PathVariable String accountNumber, @RequestParam String status, @RequestParam(defaultValue = "admin") String performedBy) {
        try {
            BankAccount acc = accountService.updateAccountStatus(accountNumber, status, performedBy);
            return ResponseEntity.ok(acc);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<BankingTransaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionRepository.findTop20ByOrderByTimestampDesc());
    }

    @GetMapping("/kyc")
    public ResponseEntity<List<KYCDocument>> getAllKYC() {
        return ResponseEntity.ok(kycRepository.findAll());
    }

    @PostMapping("/kyc/{id}/review")
    public ResponseEntity<?> reviewKYC(@PathVariable Long id, @RequestBody Map<String, String> req) {
        try {
            String status = req.get("status");
            String notes = req.get("notes");
            String reviewer = req.getOrDefault("reviewer", "admin");

            KYCDocument doc = kycAndSupportService.reviewKYC(id, status, notes, reviewer);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/loans")
    public ResponseEntity<List<Loan>> getAllLoans() {
        return ResponseEntity.ok(loanRepository.findAll());
    }

    @PostMapping("/loans/{id}/approve")
    public ResponseEntity<?> approveLoan(@PathVariable Long id, @RequestParam(defaultValue = "admin") String adminUser) {
        try {
            Loan loan = loanAndFdService.approveLoan(id, adminUser);
            return ResponseEntity.ok(loan);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/fraud/alerts")
    public ResponseEntity<List<FraudAlert>> getFraudAlerts() {
        return ResponseEntity.ok(fraudDetectionService.getAllAlerts());
    }

    @PostMapping("/fraud/alerts/{id}/status")
    public ResponseEntity<?> updateFraudAlertStatus(@PathVariable Long id, @RequestParam String status, @RequestParam(defaultValue = "admin") String adminUser) {
        try {
            FraudAlert alert = fraudDetectionService.updateAlertStatus(id, status, adminUser);
            return ResponseEntity.ok(alert);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/branches")
    public ResponseEntity<List<Branch>> getAllBranches() {
        return ResponseEntity.ok(branchRepository.findAll());
    }

    @PostMapping("/branches")
    public ResponseEntity<Branch> createBranch(@RequestBody Branch branch) {
        return ResponseEntity.ok(branchRepository.save(branch));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<BankingAuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditService.getRecentLogs());
    }

    @GetMapping("/reports/{type}")
    public ResponseEntity<String> generateReport(@PathVariable String type) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,ReportType,Timestamp,Status\n");
        csv.append("1,").append(type.toUpperCase()).append(",").append(java.time.LocalDateTime.now()).append(",GENERATED\n");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DB_Kavach_Banking_" + type + "_Report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }
}
