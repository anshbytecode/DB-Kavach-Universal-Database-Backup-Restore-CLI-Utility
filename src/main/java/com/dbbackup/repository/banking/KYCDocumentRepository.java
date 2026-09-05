package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.Customer;
import com.dbbackup.model.banking.KYCDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KYCDocumentRepository extends JpaRepository<KYCDocument, Long> {
    List<KYCDocument> findByCustomer(Customer customer);
    List<KYCDocument> findByCustomerId(Long customerId);
    List<KYCDocument> findByStatus(String status);
    long countByStatus(String status);
}
