package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.Customer;
import com.dbbackup.model.banking.FDAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FDAccountRepository extends JpaRepository<FDAccount, Long> {
    Optional<FDAccount> findByFdNumber(String fdNumber);
    List<FDAccount> findByCustomer(Customer customer);
    List<FDAccount> findByCustomerId(Long customerId);

    @Query("SELECT SUM(f.principalAmount) FROM FDAccount f WHERE f.status = 'ACTIVE'")
    BigDecimal sumTotalActiveFdAmount();
}
