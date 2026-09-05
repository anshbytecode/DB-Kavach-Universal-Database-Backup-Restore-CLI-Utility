package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.BankAccount;
import com.dbbackup.model.banking.BankingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankingTransactionRepository extends JpaRepository<BankingTransaction, Long> {
    Optional<BankingTransaction> findByTransactionId(String transactionId);
    Optional<BankingTransaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM BankingTransaction t WHERE t.sourceAccount.id = :accId OR t.targetAccount.id = :accId ORDER BY t.timestamp DESC")
    List<BankingTransaction> findByAccountId(@Param("accId") Long accId);

    @Query("SELECT t FROM BankingTransaction t WHERE t.sourceAccount.customer.id = :custId OR t.targetAccount.customer.id = :custId ORDER BY t.timestamp DESC")
    List<BankingTransaction> findByCustomerId(@Param("custId") Long custId);

    List<BankingTransaction> findTop20ByOrderByTimestampDesc();

    @Query("SELECT SUM(t.amount) FROM BankingTransaction t WHERE t.type = :type AND t.status = 'SUCCESS'")
    BigDecimal sumAmountByType(@Param("type") String type);

    long countByStatus(String status);
    long countByTimestampAfter(LocalDateTime timestamp);
}
