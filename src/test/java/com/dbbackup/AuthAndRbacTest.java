package com.dbbackup;

import com.dbbackup.model.banking.Customer;
import com.dbbackup.model.banking.User;
import com.dbbackup.service.banking.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AuthAndRbacTest {

    @Autowired
    private AuthService authService;

    @Test
    public void testCustomerRegistrationAndLogin() {
        String username = "testuser_" + System.currentTimeMillis();
        Customer customer = authService.registerCustomer(
                username, username + "@example.com", "Password123!",
                "Test", "User", "+1-555-0100", LocalDate.of(1995, 1, 1), "123 Main St"
        );

        assertNotNull(customer);
        assertEquals(username, customer.getUser().getUsername());

        User loggedInUser = authService.login(username, "Password123!");
        assertNotNull(loggedInUser);
        assertEquals(username, loggedInUser.getUsername());
    }

    @Test
    public void testAccountLockoutAfterFailedAttempts() {
        String username = "lockoutuser_" + System.currentTimeMillis();
        authService.registerCustomer(
                username, username + "@example.com", "CorrectPassword1!",
                "Lock", "Out", "+1-555-0101", LocalDate.of(1990, 5, 5), "456 Oak St"
        );

        for (int i = 0; i < 4; i++) {
            assertThrows(IllegalArgumentException.class, () -> authService.login(username, "WrongPassword!"));
        }

        // 5th failed attempt triggers lockout
        assertThrows(IllegalArgumentException.class, () -> authService.login(username, "WrongPassword!"));

        // Subsequent attempt shows locked account error
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> authService.login(username, "CorrectPassword1!"));
        assertTrue(ex.getMessage().contains("locked"));
    }
}
