package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.Customer;
import com.dbbackup.model.banking.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUser(User user);
    Optional<Customer> findByUserId(Long userId);
    List<Customer> findByKycStatus(String kycStatus);
    long countByKycStatus(String kycStatus);
}
