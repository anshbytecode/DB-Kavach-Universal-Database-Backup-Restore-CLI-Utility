package com.dbbackup.service.banking;

import com.dbbackup.model.banking.BankingAuditLog;
import com.dbbackup.repository.banking.BankingAuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BankingAuditService {

    private final BankingAuditLogRepository auditLogRepository;

    public BankingAuditService(BankingAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public BankingAuditLog log(String username, String role, String action, String targetResource, String details, String status) {
        BankingAuditLog log = new BankingAuditLog(username, role, action, targetResource, details, status);
        return auditLogRepository.save(log);
    }

    public List<BankingAuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc();
    }
}
