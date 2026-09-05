package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findByStatus(String status);
    List<FraudAlert> findByCustomerId(Long customerId);
    List<FraudAlert> findTop50ByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
