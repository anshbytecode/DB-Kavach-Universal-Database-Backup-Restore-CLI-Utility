package com.dbbackup.controller.banking;

import com.dbbackup.model.banking.*;
import com.dbbackup.repository.banking.*;
import com.dbbackup.service.banking.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerApiController {

    private final CustomerRepository customerRepository;
    private final AccountService accountService;
    private final TransferService transferService;
    private final LoanAndFdService loanAndFdService;
    private final CardAndBillService cardAndBillService;
    private final KYCAndSupportService kycAndSupportService;
    private final BankingTransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;

    public CustomerApiController(CustomerRepository customerRepository,
                                 AccountService accountService,
                                 TransferService transferService,
                                 LoanAndFdService loanAndFdService,
                                 CardAndBillService cardAndBillService,
                                 KYCAndSupportService kycAndSupportService,
                                 BankingTransactionRepository transactionRepository,
                                 NotificationRepository notificationRepository) {
        this.customerRepository = customerRepository;
        this.accountService = accountService;
        this.transferService = transferService;
        this.loanAndFdService = loanAndFdService;
        this.cardAndBillService = cardAndBillService;
        this.kycAndSupportService = kycAndSupportService;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam Long customerId) {
        return customerRepository.findById(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccount>> getAccounts(@RequestParam Long customerId) {
        return ResponseEntity.ok(accountService.getAccountsByCustomer(customerId));
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> createAccount(@RequestBody Map<String, Object> req) {
        try {
            Long customerId = Long.valueOf(req.get("customerId").toString());
            String type = req.get("accountType").toString();
            BigDecimal deposit = req.get("initialDeposit") != null ? new BigDecimal(req.get("initialDeposit").toString()) : BigDecimal.ZERO;
            BigDecimal rate = req.get("interestRate") != null ? new BigDecimal(req.get("interestRate").toString()) : new BigDecimal("3.5");

            BankAccount account = accountService.createAccount(customerId, type, deposit, rate);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<BankingTransaction>> getTransactions(@RequestParam Long customerId) {
        return ResponseEntity.ok(transactionRepository.findByCustomerId(customerId));
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> executeTransfer(@RequestBody Map<String, Object> req) {
        try {
            String source = req.get("sourceAccount").toString();
            String target = req.get("targetAccount").toString();
            BigDecimal amount = new BigDecimal(req.get("amount").toString());
            String category = req.get("category") != null ? req.get("category").toString() : "OTHER";
            String desc = req.get("description") != null ? req.get("description").toString() : "Transfer";
            String key = req.get("idempotencyKey") != null ? req.get("idempotencyKey").toString() : null;

            BankingTransaction tx = transferService.executeTransfer(source, target, amount, category, desc, key);
            return ResponseEntity.ok(tx);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/beneficiaries")
    public ResponseEntity<List<Beneficiary>> getBeneficiaries(@RequestParam Long customerId) {
        return ResponseEntity.ok(transferService.getBeneficiaries(customerId));
    }

    @PostMapping("/beneficiaries")
    public ResponseEntity<?> addBeneficiary(@RequestBody Map<String, String> req) {
        try {
            Long customerId = Long.valueOf(req.get("customerId"));
            String nickname = req.get("nickname");
            String accNo = req.get("accountNumber");
            String name = req.get("beneficiaryName");
            String bank = req.get("bankName");
            String ifsc = req.get("ifscCode");

            Beneficiary b = transferService.addBeneficiary(customerId, nickname, accNo, name, bank, ifsc);
            return ResponseEntity.ok(b);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/loans")
    public ResponseEntity<List<Loan>> getLoans(@RequestParam Long customerId) {
        return ResponseEntity.ok(loanAndFdService.getCustomerLoans(customerId));
    }

    @PostMapping("/loans/apply")
    public ResponseEntity<?> applyLoan(@RequestBody Map<String, Object> req) {
        try {
            Long customerId = Long.valueOf(req.get("customerId").toString());
            String type = req.get("loanType").toString();
            BigDecimal amount = new BigDecimal(req.get("amount").toString());
            int tenure = Integer.parseInt(req.get("tenureMonths").toString());
            BigDecimal rate = req.get("interestRate") != null ? new BigDecimal(req.get("interestRate").toString()) : new BigDecimal("9.5");

            Loan loan = loanAndFdService.applyForLoan(customerId, type, amount, rate, tenure);
            return ResponseEntity.ok(loan);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/fds")
    public ResponseEntity<List<FDAccount>> getFDs(@RequestParam Long customerId) {
        return ResponseEntity.ok(loanAndFdService.getCustomerFDs(customerId));
    }

    @PostMapping("/fds/create")
    public ResponseEntity<?> createFD(@RequestBody Map<String, Object> req) {
        try {
            Long customerId = Long.valueOf(req.get("customerId").toString());
            BigDecimal principal = new BigDecimal(req.get("principalAmount").toString());
            int tenure = Integer.parseInt(req.get("tenureMonths").toString());
            BigDecimal rate = req.get("interestRate") != null ? new BigDecimal(req.get("interestRate").toString()) : new BigDecimal("7.25");

            FDAccount fd = loanAndFdService.createFD(customerId, principal, rate, tenure);
            return ResponseEntity.ok(fd);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/cards")
    public ResponseEntity<List<Card>> getCards(@RequestParam Long customerId) {
        return ResponseEntity.ok(cardAndBillService.getCustomerCards(customerId));
    }

    @PostMapping("/cards/{id}/freeze")
    public ResponseEntity<?> toggleCardFreeze(@PathVariable Long id, @RequestParam String username) {
        try {
            Card card = cardAndBillService.toggleCardFreeze(id, username);
            return ResponseEntity.ok(card);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/billers")
    public ResponseEntity<List<Biller>> getBillers() {
        return ResponseEntity.ok(cardAndBillService.getAllBillers());
    }

    @PostMapping("/bills/pay")
    public ResponseEntity<?> payBill(@RequestBody Map<String, Object> req) {
        try {
            Long customerId = Long.valueOf(req.get("customerId").toString());
            Long accountId = Long.valueOf(req.get("accountId").toString());
            Long billerId = Long.valueOf(req.get("billerId").toString());
            String consumerNo = req.get("consumerNumber").toString();
            BigDecimal amount = new BigDecimal(req.get("amount").toString());

            BillPayment payment = cardAndBillService.payBill(customerId, accountId, billerId, consumerNo, amount);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<SupportTicket>> getTickets(@RequestParam Long customerId) {
        return ResponseEntity.ok(kycAndSupportService.getCustomerTickets(customerId));
    }

    @PostMapping("/tickets")
    public ResponseEntity<?> createTicket(@RequestBody Map<String, String> req) {
        try {
            Long customerId = Long.valueOf(req.get("customerId"));
            String subject = req.get("subject");
            String category = req.get("category");
            String priority = req.get("priority");
            String message = req.get("message");

            SupportTicket ticket = kycAndSupportService.createTicket(customerId, subject, category, priority, message);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications(@RequestParam Long userId) {
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByTimestampDesc(userId));
    }

    @PostMapping("/kyc")
    public ResponseEntity<?> submitKYC(@RequestBody Map<String, String> req) {
        try {
            Long customerId = Long.valueOf(req.get("customerId"));
            String docType = req.get("documentType");
            String docNum = req.get("documentNumber");

            KYCDocument doc = kycAndSupportService.submitKYC(customerId, docType, docNum);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
