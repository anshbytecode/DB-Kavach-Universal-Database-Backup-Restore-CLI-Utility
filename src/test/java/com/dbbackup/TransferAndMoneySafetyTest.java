package com.dbbackup;

import com.dbbackup.model.banking.*;
import com.dbbackup.repository.banking.*;
import com.dbbackup.service.banking.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class TransferAndMoneySafetyTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    private BankAccount sourceAcc;
    private BankAccount targetAcc;

    @BeforeEach
    public void setUp() {
        User user1 = userRepository.save(new User("user_t1", "t1@example.com", "hash", Role.CUSTOMER));
        Customer c1 = customerRepository.save(new Customer(user1, "Sender", "Test", "+1-555", LocalDate.of(1990,1,1), "Addr"));
        sourceAcc = accountRepository.save(new BankAccount("SAV_TEST_001", c1, "SAVINGS", new BigDecimal("10000.00"), new BigDecimal("4.00")));

        User user2 = userRepository.save(new User("user_t2", "t2@example.com", "hash", Role.CUSTOMER));
        Customer c2 = customerRepository.save(new Customer(user2, "Receiver", "Test", "+1-555", LocalDate.of(1992,1,1), "Addr"));
        targetAcc = accountRepository.save(new BankAccount("SAV_TEST_002", c2, "SAVINGS", new BigDecimal("2000.00"), new BigDecimal("4.00")));
    }

    @Test
    public void testSuccessfulTransfer() {
        BigDecimal amount = new BigDecimal("1500.00");
        BankingTransaction tx = transferService.executeTransfer(
                sourceAcc.getAccountNumber(), targetAcc.getAccountNumber(), amount, "OTHER", "Test Transfer", "KEY_001"
        );

        assertNotNull(tx);
        assertEquals("SUCCESS", tx.getStatus());

        BankAccount updatedSource = accountRepository.findByAccountNumber(sourceAcc.getAccountNumber()).orElseThrow();
        BankAccount updatedTarget = accountRepository.findByAccountNumber(targetAcc.getAccountNumber()).orElseThrow();

        assertEquals(0, new BigDecimal("8500.00").compareTo(updatedSource.getBalance()));
        assertEquals(0, new BigDecimal("3500.00").compareTo(updatedTarget.getBalance()));
    }

    @Test
    public void testInsufficientBalanceTransferFailure() {
        BigDecimal amount = new BigDecimal("50000.00");
        assertThrows(IllegalStateException.class, () -> transferService.executeTransfer(
                sourceAcc.getAccountNumber(), targetAcc.getAccountNumber(), amount, "OTHER", "Too Much Money", "KEY_002"
        ));
    }

    @Test
    public void testIdempotencyProtectionPreventsDoubleDebiting() {
        String idempotencyKey = "IDEMP_KEY_SAFETY_100";
        BigDecimal amount = new BigDecimal("2000.00");

        // First transfer
        BankingTransaction tx1 = transferService.executeTransfer(
                sourceAcc.getAccountNumber(), targetAcc.getAccountNumber(), amount, "OTHER", "Transfer 1", idempotencyKey
        );

        // Second transfer with SAME idempotency key
        BankingTransaction tx2 = transferService.executeTransfer(
                sourceAcc.getAccountNumber(), targetAcc.getAccountNumber(), amount, "OTHER", "Transfer 2", idempotencyKey
        );

        assertEquals(tx1.getTransactionId(), tx2.getTransactionId());

        BankAccount updatedSource = accountRepository.findByAccountNumber(sourceAcc.getAccountNumber()).orElseThrow();
        // Balance should be debited ONLY ONCE ($10,000 - $2,000 = $8,000)
        assertEquals(0, new BigDecimal("8000.00").compareTo(updatedSource.getBalance()));
    }
}
