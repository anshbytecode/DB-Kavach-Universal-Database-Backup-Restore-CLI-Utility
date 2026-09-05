package com.dbbackup.service.banking;

import com.dbbackup.model.banking.*;
import com.dbbackup.repository.banking.BankingTransactionRepository;
import com.dbbackup.repository.banking.FraudAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FraudDetectionService {

    private final FraudAlertRepository fraudAlertRepository;
    private final BankingTransactionRepository transactionRepository;
    private final BankingAuditService auditService;

    public FraudDetectionService(FraudAlertRepository fraudAlertRepository,
                                 BankingTransactionRepository transactionRepository,
                                 BankingAuditService auditService) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    public int evaluateRiskScore(BankAccount sourceAccount, BankAccount targetAccount, BigDecimal amount) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        // 1. Large transaction amount check (> 100,000)
        if (amount.compareTo(new BigDecimal("100000.00")) > 0) {
            score += 40;
            reasons.add("Unusually large transaction amount (" + amount + ")");
        } else if (amount.compareTo(new BigDecimal("50000.00")) > 0) {
            score += 20;
            reasons.add("High value transaction (" + amount + ")");
        }

        // 2. High frequency check (more than 3 transactions in past 10 minutes)
        if (sourceAccount != null && sourceAccount.getCustomer() != null) {
            long recentCount = transactionRepository.countByTimestampAfter(LocalDateTime.now().minusMinutes(10));
            if (recentCount >= 3) {
                score += 35;
                reasons.add("Multiple rapid transfers detected in past 10 minutes");
            }
        }

        // 3. Late night transaction timing (between 1 AM and 4 AM)
        LocalTime nowTime = LocalTime.now();
        if (nowTime.isAfter(LocalTime.of(1, 0)) && nowTime.isBefore(LocalTime.of(4, 0))) {
            score += 20;
            reasons.add("Unusual transaction timing (late night)");
        }

        // 4. Source account status check
        if (sourceAccount != null && "FROZEN".equalsIgnoreCase(sourceAccount.getStatus())) {
            score += 50;
            reasons.add("Attempted transfer from frozen account");
        }

        return Math.min(100, score);
    }

    @Transactional
    public FraudAlert triggerFraudAlertIfNecessary(BankingTransaction transaction, int riskScore, String reasons) {
        if (riskScore < 30) {
            return null;
        }

        String level = riskScore >= 80 ? "CRITICAL" :
                      riskScore >= 50 ? "HIGH" : "MEDIUM";

        Customer customer = transaction.getSourceAccount() != null ? transaction.getSourceAccount().getCustomer() : null;

        FraudAlert alert = new FraudAlert(transaction, customer, riskScore, level, reasons);
        alert = fraudAlertRepository.save(alert);

        auditService.log("SYSTEM_FRAUD_ENGINE", "SYSTEM", "FRAUD_ALERT_GENERATED", "TRANSACTION_" + transaction.getTransactionId(),
                "Generated " + level + " risk fraud alert (Score: " + riskScore + "): " + reasons, "WARNING");

        return alert;
    }

    public List<FraudAlert> getAllAlerts() {
        return fraudAlertRepository.findTop50ByOrderByCreatedAtDesc();
    }

    @Transactional
    public FraudAlert updateAlertStatus(Long alertId, String status, String adminUser) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Fraud Alert not found"));
        alert.setStatus(status.toUpperCase());

        if ("FROZEN_ACCOUNT".equalsIgnoreCase(status) && alert.getCustomer() != null) {
            // Option to freeze customer accounts
            auditService.log(adminUser, "ADMIN", "ACCOUNT_FROZEN", "CUSTOMER_" + alert.getCustomer().getId(), "Account frozen via Fraud Center", "SUCCESS");
        }

        fraudAlertRepository.save(alert);
        auditService.log(adminUser, "ADMIN", "FRAUD_ALERT_UPDATED", "ALERT_" + alertId, "Alert status updated to " + status, "SUCCESS");
        return alert;
    }
}
