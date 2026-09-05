package com.dbbackup.controller.banking;

import com.dbbackup.model.banking.Customer;
import com.dbbackup.model.banking.User;
import com.dbbackup.repository.banking.UserRepository;
import com.dbbackup.service.banking.AuthService;
import com.dbbackup.service.banking.EmailVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService, UserRepository userRepository) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.userRepository = userRepository;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String username = request.get("username");

            if (username != null && !username.trim().isEmpty() && userRepository.existsByUsername(username.trim())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Username '" + username.trim() + "' is already taken. Please choose another username.");
                return ResponseEntity.badRequest().body(error);
            }

            if (email != null && !email.trim().isEmpty() && userRepository.existsByEmail(email.trim())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Email '" + email.trim() + "' is already registered. Please sign in or use another email.");
                return ResponseEntity.badRequest().body(error);
            }

            String otp = emailVerificationService.sendVerificationOtp(email);

            Map<String, Object> res = new HashMap<>();
            res.put("status", "success");
            res.put("message", "Real approval security OTP code dispatched to " + email);
            res.put("otpCode", otp);

            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        boolean verified = emailVerificationService.verifyOtp(email, otp);
        Map<String, Object> res = new HashMap<>();
        if (verified) {
            res.put("status", "success");
            res.put("message", "Gmail OTP verified successfully");
            return ResponseEntity.ok(res);
        } else {
            res.put("status", "error");
            res.put("error", "Invalid or expired OTP verification code");
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            User user = authService.login(username, password);

            Map<String, Object> res = new HashMap<>();
            res.put("status", "success");
            res.put("userId", user.getId());
            res.put("username", user.getUsername());
            res.put("email", user.getEmail());
            res.put("role", user.getRole().name());
            res.put("token", "DEMO_JWT_TOKEN_" + System.currentTimeMillis());

            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            String firstName = request.get("firstName");
            String lastName = request.get("lastName");
            String phone = request.get("phone");
            String address = request.get("address");

            Customer customer = authService.registerCustomer(username, email, password, firstName, lastName, phone, LocalDate.now().minusYears(25), address);

            Map<String, Object> res = new HashMap<>();
            res.put("status", "success");
            res.put("customerId", customer.getId());
            res.put("username", username);
            res.put("message", "Customer account registered successfully");

            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            authService.changePassword(username, oldPassword, newPassword);

            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            res.put("message", "Password changed successfully");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
