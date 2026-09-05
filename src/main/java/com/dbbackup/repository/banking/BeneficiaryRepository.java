package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.Beneficiary;
import com.dbbackup.model.banking.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    List<Beneficiary> findByCustomer(Customer customer);
    List<Beneficiary> findByCustomerId(Long customerId);
}
