package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.BankingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankingAuditLogRepository extends JpaRepository<BankingAuditLog, Long> {
    List<BankingAuditLog> findTop100ByOrderByTimestampDesc();
    List<BankingAuditLog> findByUsername(String username);
    List<BankingAuditLog> findByAction(String action);
}
