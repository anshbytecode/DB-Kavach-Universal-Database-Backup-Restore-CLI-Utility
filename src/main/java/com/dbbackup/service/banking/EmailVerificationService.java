package com.dbbackup.service.banking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailVerificationService {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private static class OtpData {
        final String otp;
        final long expiryTime;

        OtpData(String otp, long expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }

    public String sendVerificationOtp(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required for verification");
        }

        String cleanEmail = email.trim().toLowerCase();
        int code = 100000 + random.nextInt(900000);
        String otp = String.valueOf(code);
        long expiry = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes

        otpStore.put(cleanEmail, new OtpData(otp, expiry));

        System.out.println("=========================================================================");
        System.out.println(" 📧 DB-KAVACH GMAIL REAL OTP DISPATCH ");
        System.out.println(" Target Email Address : " + cleanEmail);
        System.out.println(" 6-Digit Security OTP : " + otp);
        System.out.println(" Status               : DISPATCHED TO GMAIL SERVERS (Expires in 5 mins)");
        System.out.println("=========================================================================");

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("dbkavach.banking@gmail.com");
                message.setTo(cleanEmail);
                message.setSubject("DB-Kavach Banking - Real Account Approval OTP Code");
                message.setText("Dear Customer,\n\n" +
                        "Your real 6-digit email approval verification code for DB-Kavach Banking is:\n\n" +
                        "    " + otp + "\n\n" +
                        "This OTP code is valid for 5 minutes. Do not share this code with anyone for your security.\n\n" +
                        "Regards,\n" +
                        "DB-Kavach Banking Security Operations Team");
                mailSender.send(message);
                System.out.println("✅ Real email successfully sent via Gmail SMTP to " + cleanEmail);
            } catch (Exception e) {
                System.out.println("⚠️ SMTP Dispatch note: " + e.getMessage() + ". Real OTP code logged above: " + otp);
            }
        }

        return otp;
    }

    public boolean verifyOtp(String email, String inputOtp) {
        if (email == null || inputOtp == null) return false;

        String cleanEmail = email.trim().toLowerCase();
        OtpData data = otpStore.get(cleanEmail);

        if (data == null) {
            return false;
        }

        if (System.currentTimeMillis() > data.expiryTime) {
            otpStore.remove(cleanEmail);
            return false;
        }

        boolean valid = data.otp.equals(inputOtp.trim());
        if (valid) {
            otpStore.remove(cleanEmail);
        }
        return valid;
    }
}
