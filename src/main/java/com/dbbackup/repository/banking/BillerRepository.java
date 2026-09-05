package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.Biller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillerRepository extends JpaRepository<Biller, Long> {
    Optional<Biller> findByBillerCode(String billerCode);
    List<Biller> findByCategory(String category);
}
