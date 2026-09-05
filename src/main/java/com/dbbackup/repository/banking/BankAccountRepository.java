package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.BankAccount;
import com.dbbackup.model.banking.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    List<BankAccount> findByCustomer(Customer customer);
    List<BankAccount> findByCustomerId(Long customerId);

    @Query("SELECT SUM(b.balance) FROM BankAccount b")
    BigDecimal sumTotalDeposits();

    long countByStatus(String status);
}
