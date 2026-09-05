package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.Card;
import com.dbbackup.model.banking.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByCustomer(Customer customer);
    List<Card> findByCustomerId(Long customerId);
}
