package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.BankProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankProductRepository extends JpaRepository<BankProduct, Long> {
    List<BankProduct> findByProductType(String productType);
    List<BankProduct> findByStatus(String status);
}
