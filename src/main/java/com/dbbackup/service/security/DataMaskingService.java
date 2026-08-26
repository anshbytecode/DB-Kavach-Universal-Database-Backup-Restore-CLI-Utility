package com.dbbackup.service.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataMaskingService {
    private static final Logger log = LoggerFactory.getLogger(DataMaskingService.class);

    // Regex patterns for PII detection
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\s]?\\d{4}\\b");
    
    // Pattern for sensitive keys in SQL INSERT or JSON (password, secret, token, api_key)
    private static final Pattern SENSITIVE_KEY_VALUE = Pattern.compile(
        "(?i)('(?:password|passwd|pwd|secret|api_key|token|access_token|ssn|credit_card)'\\s*[,:]\\s*')([^']+)(')",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Masks sensitive PII data in a database dump file line by line and writes the sanitized output.
     */
    public void maskDumpFile(File inputFile, File outputFile) throws IOException {
        log.info("Sanitizing/Masking sensitive PII data in dump file [{}] -> [{}]...", inputFile.getName(), outputFile.getName());

        long count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String maskedLine = maskContent(line);
                writer.write(maskedLine);
                writer.newLine();
                count++;
            }
            writer.flush();
        }

        log.info("Data masking complete! Processed {} lines. Output file size: {} bytes", count, outputFile.length());
    }

    /**
     * Applies masking rules to a given string content.
     */
    public String maskContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String result = content;

        // 1. Mask Email Addresses (e.g. john.doe@example.com -> j***e@example.com)
        Matcher emailMatcher = EMAIL_PATTERN.matcher(result);
        StringBuffer emailBuffer = new StringBuffer();
        while (emailMatcher.find()) {
            String user = emailMatcher.group(1);
            String domain = emailMatcher.group(2);
            String maskedUser;
            if (user.length() <= 2) {
                maskedUser = "***";
            } else {
                maskedUser = user.charAt(0) + "***" + user.charAt(user.length() - 1);
            }
            emailMatcher.appendReplacement(emailBuffer, Matcher.quoteReplacement(maskedUser + "@" + domain));
        }
        emailMatcher.appendTail(emailBuffer);
        result = emailBuffer.toString();

        // 2. Mask Credit Cards (e.g. 4532-1234-5678-9010 -> XXXX-XXXX-XXXX-9010)
        Matcher ccMatcher = CREDIT_CARD_PATTERN.matcher(result);
        StringBuffer ccBuffer = new StringBuffer();
        while (ccMatcher.find()) {
            String rawCC = ccMatcher.group().replaceAll("[^0-9]", "");
            if (rawCC.length() >= 13 && rawCC.length() <= 16) {
                String last4 = rawCC.substring(rawCC.length() - 4);
                ccMatcher.appendReplacement(ccBuffer, "XXXX-XXXX-XXXX-" + last4);
            } else {
                ccMatcher.appendReplacement(ccBuffer, Matcher.quoteReplacement(ccMatcher.group()));
            }
        }
        ccMatcher.appendTail(ccBuffer);
        result = ccBuffer.toString();

        // 3. Mask SSN (e.g. 123-45-6789 -> XXX-XX-6789)
        Matcher ssnMatcher = SSN_PATTERN.matcher(result);
        StringBuffer ssnBuffer = new StringBuffer();
        while (ssnMatcher.find()) {
            String ssn = ssnMatcher.group();
            String last4 = ssn.substring(ssn.length() - 4);
            ssnMatcher.appendReplacement(ssnBuffer, "XXX-XX-" + last4);
        }
        ssnMatcher.appendTail(ssnBuffer);
        result = ssnBuffer.toString();

        // 4. Mask Phone Numbers (e.g. 555-123-4567 -> XXX-XXX-4567)
        Matcher phoneMatcher = PHONE_PATTERN.matcher(result);
        StringBuffer phoneBuffer = new StringBuffer();
        while (phoneMatcher.find()) {
            String phone = phoneMatcher.group();
            String digitsOnly = phone.replaceAll("[^0-9]", "");
            if (digitsOnly.length() >= 10) {
                String last4 = digitsOnly.substring(digitsOnly.length() - 4);
                phoneMatcher.appendReplacement(phoneBuffer, "XXX-XXX-" + last4);
            } else {
                phoneMatcher.appendReplacement(phoneBuffer, Matcher.quoteReplacement(phone));
            }
        }
        phoneMatcher.appendTail(phoneBuffer);
        result = phoneBuffer.toString();

        // 5. Mask Sensitive Key-Values
        Matcher keyValMatcher = SENSITIVE_KEY_VALUE.matcher(result);
        StringBuffer keyValBuffer = new StringBuffer();
        while (keyValMatcher.find()) {
            String keyPrefix = keyValMatcher.group(1);
            String suffix = keyValMatcher.group(3);
            keyValMatcher.appendReplacement(keyValBuffer, Matcher.quoteReplacement(keyPrefix + "[REDACTED]" + suffix));
        }
        keyValMatcher.appendTail(keyValBuffer);
        result = keyValBuffer.toString();

        return result;
    }
}
