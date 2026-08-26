package com.dbbackup.service.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

@Service
public class EncryptionService {
    private static final Logger log = LoggerFactory.getLogger(EncryptionService.class);

    private static final byte[] MAGIC_HEADER = new byte[]{'E', 'N', 'C', '1'};
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int BUFFER_SIZE = 8192;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Checks whether a given file starts with the AES-GCM magic header (ENC1).
     */
    public boolean isEncryptedFile(File file) {
        if (file == null || !file.exists() || file.length() < MAGIC_HEADER.length + SALT_LENGTH_BYTES + IV_LENGTH_BYTES) {
            return false;
        }
        try (InputStream is = new FileInputStream(file)) {
            byte[] header = new byte[MAGIC_HEADER.length];
            int read = is.read(header);
            return read == MAGIC_HEADER.length && Arrays.equals(header, MAGIC_HEADER);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Encrypts a file using AES-256-GCM and a passphrase.
     */
    public void encryptFile(File inputFile, File outputFile, String passphrase) throws Exception {
        if (passphrase == null || passphrase.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption passphrase cannot be empty.");
        }

        log.info("Encrypting file [{}] -> [{}] using AES-256-GCM...", inputFile.getName(), outputFile.getName());

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        secureRandom.nextBytes(iv);

        SecretKey secretKey = deriveKey(passphrase, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        try (InputStream fis = new BufferedInputStream(new FileInputStream(inputFile));
             OutputStream fos = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            
            // Write Magic Header + Salt + IV
            fos.write(MAGIC_HEADER);
            fos.write(salt);
            fos.write(iv);

            byte[] inBuffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(inBuffer)) != -1) {
                byte[] outBuffer = cipher.update(inBuffer, 0, bytesRead);
                if (outBuffer != null) {
                    fos.write(outBuffer);
                }
            }
            byte[] finalBuffer = cipher.doFinal();
            if (finalBuffer != null) {
                fos.write(finalBuffer);
            }
            fos.flush();
        }

        log.info("File encryption successful! Encrypted size: {} bytes", outputFile.length());
    }

    /**
     * Decrypts an AES-256-GCM encrypted file using a passphrase.
     */
    public void decryptFile(File inputFile, File outputFile, String passphrase) throws Exception {
        if (passphrase == null || passphrase.trim().isEmpty()) {
            throw new IllegalArgumentException("Decryption passphrase cannot be empty.");
        }

        log.info("Decrypting file [{}] -> [{}] using AES-256-GCM...", inputFile.getName(), outputFile.getName());

        try (InputStream fis = new BufferedInputStream(new FileInputStream(inputFile));
             OutputStream fos = new BufferedOutputStream(new FileOutputStream(outputFile))) {

            byte[] header = new byte[MAGIC_HEADER.length];
            int bytesRead = fis.read(header);
            if (bytesRead != MAGIC_HEADER.length || !Arrays.equals(header, MAGIC_HEADER)) {
                throw new IllegalArgumentException("Invalid encrypted file format: Magic header 'ENC1' missing.");
            }

            byte[] salt = new byte[SALT_LENGTH_BYTES];
            if (fis.read(salt) != SALT_LENGTH_BYTES) {
                throw new IllegalArgumentException("Corrupted encrypted file: missing salt.");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            if (fis.read(iv) != IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Corrupted encrypted file: missing IV.");
            }

            SecretKey secretKey = deriveKey(passphrase, salt);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] inBuffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = fis.read(inBuffer)) != -1) {
                byte[] outBuffer = cipher.update(inBuffer, 0, read);
                if (outBuffer != null) {
                    fos.write(outBuffer);
                }
            }
            byte[] finalBuffer = cipher.doFinal();
            if (finalBuffer != null) {
                fos.write(finalBuffer);
            }
            fos.flush();
        } catch (Exception e) {
            log.error("Decryption failed for [{}]: {}", inputFile.getName(), e.getMessage());
            throw new IllegalArgumentException("Decryption failed: Incorrect passphrase or corrupted file.", e);
        }

        log.info("File decryption successful! Decrypted size: {} bytes", outputFile.length());
    }

    /**
     * Encrypts plain text bytes into Base64 encoded payload.
     */
    public String encryptText(String plainText, String passphrase) throws Exception {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        secureRandom.nextBytes(iv);

        SecretKey secretKey = deriveKey(passphrase, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        ByteBuffer byteBuffer = ByteBuffer.allocate(MAGIC_HEADER.length + salt.length + iv.length + cipherText.length);
        byteBuffer.put(MAGIC_HEADER);
        byteBuffer.put(salt);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);

        return java.util.Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    /**
     * Decrypts Base64 encoded payload into plain text.
     */
    public String decryptText(String base64Payload, String passphrase) throws Exception {
        byte[] decoded = java.util.Base64.getDecoder().decode(base64Payload);
        ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

        byte[] header = new byte[MAGIC_HEADER.length];
        byteBuffer.get(header);
        if (!Arrays.equals(header, MAGIC_HEADER)) {
            throw new IllegalArgumentException("Invalid encrypted payload.");
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        byteBuffer.get(salt);

        byte[] iv = new byte[IV_LENGTH_BYTES];
        byteBuffer.get(iv);

        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        SecretKey secretKey = deriveKey(passphrase, salt);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Decryption failed: Incorrect passphrase or corrupted payload.", e);
        }
    }

    private SecretKey deriveKey(String passphrase, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}
