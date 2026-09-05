package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByBranchCode(String branchCode);
    Optional<Branch> findByIfscCode(String ifscCode);
}
