package com.dbbackup.service.banking;

import com.dbbackup.model.banking.*;
import com.dbbackup.repository.banking.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final BankingAuditService auditService;

    public AuthService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       BankAccountRepository bankAccountRepository,
                       PasswordEncoder passwordEncoder,
                       BankingAuditService auditService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public User login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            auditService.log(username, "UNKNOWN", "LOGIN", "SYSTEM", "Invalid username", "FAILED");
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOpt.get();

        if (!user.isEnabled()) {
            auditService.log(username, user.getRole().name(), "LOGIN", "SYSTEM", "Disabled account login attempt", "FAILED");
            throw new IllegalStateException("Account is disabled. Please contact bank support.");
        }

        if (!user.isAccountNonLocked()) {
            if (user.getLockedUntil() != null && LocalDateTime.now().isAfter(user.getLockedUntil())) {
                user.setAccountNonLocked(true);
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            } else {
                auditService.log(username, user.getRole().name(), "LOGIN", "SYSTEM", "Locked account login attempt", "FAILED");
                throw new IllegalStateException("Account is locked due to multiple failed login attempts. Try again later.");
            }
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int failed = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failed);
            if (failed >= 5) {
                user.setAccountNonLocked(false);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                auditService.log(username, user.getRole().name(), "ACCOUNT_LOCKED", "USER", "Account locked after 5 failed attempts", "WARNING");
            }
            userRepository.save(user);
            auditService.log(username, user.getRole().name(), "LOGIN", "SYSTEM", "Incorrect password", "FAILED");
            throw new IllegalArgumentException("Invalid username or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        auditService.log(username, user.getRole().name(), "LOGIN", "SYSTEM", "Successful login", "SUCCESS");
        return user;
    }

    @Transactional
    public Customer registerCustomer(String username, String email, String password, String firstName, String lastName, String phone, LocalDate dob, String address) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(username, email, hashedPassword, Role.CUSTOMER);
        user = userRepository.save(user);

        Customer customer = new Customer(user, firstName, lastName, phone, dob, address);
        customer = customerRepository.save(customer);

        // Auto-create initial Savings Account with starting demo balance
        String accNo = "SAV" + (1000000000L + (long)(Math.random() * 9000000000L));
        BankAccount bankAccount = new BankAccount(accNo, customer, "SAVINGS", new BigDecimal("25000.00"), new BigDecimal("4.00"));
        bankAccountRepository.save(bankAccount);

        auditService.log(username, Role.CUSTOMER.name(), "CUSTOMER_CREATED", "CUSTOMER", "Registered new customer account: " + accNo, "SUCCESS");
        return customer;
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            auditService.log(username, user.getRole().name(), "PASSWORD_CHANGED", "USER", "Old password mismatch", "FAILED");
            throw new IllegalArgumentException("Incorrect current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditService.log(username, user.getRole().name(), "PASSWORD_CHANGED", "USER", "Password updated successfully", "SUCCESS");
    }
}
