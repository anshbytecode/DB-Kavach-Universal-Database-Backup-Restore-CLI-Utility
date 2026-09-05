package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.Customer;
import com.dbbackup.model.banking.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findByLoanNumber(String loanNumber);
    List<Loan> findByCustomer(Customer customer);
    List<Loan> findByCustomerId(Long customerId);
    List<Loan> findByStatus(String status);

    @Query("SELECT SUM(l.principalAmount) FROM Loan l WHERE l.status = 'ACTIVE' OR l.status = 'APPROVED'")
    BigDecimal sumTotalActiveLoans();

    long countByStatus(String status);
}
