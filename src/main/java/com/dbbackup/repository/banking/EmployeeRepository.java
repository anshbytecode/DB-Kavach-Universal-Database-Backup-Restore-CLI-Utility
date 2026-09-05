package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.Employee;
import com.dbbackup.model.banking.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUser(User user);
    Optional<Employee> findByEmployeeCode(String employeeCode);
    List<Employee> findByBranchId(Long branchId);
}
