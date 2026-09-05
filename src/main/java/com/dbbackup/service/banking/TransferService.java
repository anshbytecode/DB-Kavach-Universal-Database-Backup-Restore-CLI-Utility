package com.dbbackup.service.banking;

import com.dbbackup.model.banking.*;
import com.dbbackup.repository.banking.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {

    private final BankAccountRepository accountRepository;
    private final BankingTransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final FraudDetectionService fraudDetectionService;
    private final BankingAuditService auditService;
    private final NotificationRepository notificationRepository;

    public TransferService(BankAccountRepository accountRepository,
                           BankingTransactionRepository transactionRepository,
                           BeneficiaryRepository beneficiaryRepository,
                           FraudDetectionService fraudDetectionService,
                           BankingAuditService auditService,
                           NotificationRepository notificationRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.fraudDetectionService = fraudDetectionService;
        this.auditService = auditService;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BankingTransaction executeTransfer(String sourceAccNumber, String targetAccNumber, BigDecimal amount,
                                             String category, String description, String idempotencyKey) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly greater than zero");
        }

        // Idempotency Protection: If idempotencyKey is supplied and already executed, return existing transaction
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<BankingTransaction> existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existingTx.isPresent()) {
                return existingTx.get();
            }
        }

        BankAccount sourceAccount = accountRepository.findByAccountNumber(sourceAccNumber)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + sourceAccNumber));

        if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getStatus())) {
            throw new IllegalStateException("Source account is not active. Current status: " + sourceAccount.getStatus());
        }

        if (sourceAccount.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available balance. Available: " + sourceAccount.getAvailableBalance() + ", Requested: " + amount);
        }

        Optional<BankAccount> targetOpt = accountRepository.findByAccountNumber(targetAccNumber);
        BankAccount targetAccount = targetOpt.orElse(null);

        // Deduct money from source account
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(amount));
        accountRepository.save(sourceAccount);

        // If internal DB-Kavach target account exists, credit money
        if (targetAccount != null && "ACTIVE".equalsIgnoreCase(targetAccount.getStatus())) {
            targetAccount.setBalance(targetAccount.getBalance().add(amount));
            targetAccount.setAvailableBalance(targetAccount.getAvailableBalance().add(amount));
            accountRepository.save(targetAccount);
        }

        // Generate Transaction Record
        String txId = "TXN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        BankingTransaction tx = new BankingTransaction(
                txId,
                sourceAccount,
                targetAccount,
                amount,
                "TRANSFER",
                category != null ? category : "OTHER",
                description != null ? description : "Money Transfer to " + targetAccNumber
        );

        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            tx.setIdempotencyKey(idempotencyKey);
        }

        tx.setReferenceNumber("REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        // Evaluate Fraud Risk
        int riskScore = fraudDetectionService.evaluateRiskScore(sourceAccount, targetAccount, amount);
        tx.setRiskScore(riskScore);

        tx = transactionRepository.save(tx);

        // Trigger fraud alert if risk score is high
        if (riskScore >= 30) {
            fraudDetectionService.triggerFraudAlertIfNecessary(tx, riskScore, "Transfer of " + amount + " triggered risk rules.");
        }

        // Notify user
        if (sourceAccount.getCustomer() != null && sourceAccount.getCustomer().getUser() != null) {
            Notification notif = new Notification(
                    sourceAccount.getCustomer().getUser(),
                    "Transfer Successful",
                    "Transferred ₹" + amount + " from " + sourceAccount.getMaskedAccountNumber() + " to " + targetAccNumber + ". Txn ID: " + txId,
                    "TRANSFER"
            );
            notificationRepository.save(notif);
        }

        auditService.log(sourceAccount.getCustomer() != null ? sourceAccount.getCustomer().getUser().getUsername() : "SYSTEM",
                "CUSTOMER", "MONEY_TRANSFER", txId, "Transferred " + amount + " from " + sourceAccNumber + " to " + targetAccNumber, "SUCCESS");

        return tx;
    }

    @Transactional
    public Beneficiary addBeneficiary(Long customerId, String nickname, String accountNumber, String name, String bankName, String ifscCode) {
        Customer customer = accountRepository.findById(customerId)
                .map(BankAccount::getCustomer)
                .orElse(null);

        Beneficiary b = new Beneficiary(customer, nickname, accountNumber, name, bankName, ifscCode);
        // Set 30 minute cooling period
        b.setCoolingPeriodEnd(LocalDateTime.now().plusMinutes(30));
        b = beneficiaryRepository.save(b);

        auditService.log(customer != null ? customer.getUser().getUsername() : "SYSTEM", "CUSTOMER", "BENEFICIARY_ADDED", accountNumber, "Added beneficiary " + nickname, "SUCCESS");
        return b;
    }

    public List<Beneficiary> getBeneficiaries(Long customerId) {
        return beneficiaryRepository.findByCustomerId(customerId);
    }
}
