package com.dbbackup.service.security;

import com.dbbackup.model.CredentialProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Service
public class CredentialVaultService {
    private static final Logger log = LoggerFactory.getLogger(CredentialVaultService.class);

    @Value("${vault.file-path:./vault.enc}")
    private String vaultFilePath = "./vault.enc";

    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public CredentialVaultService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public void setVaultFilePath(String path) {
        this.vaultFilePath = path;
    }

    public String getVaultFilePath() {
        return this.vaultFilePath;
    }

    /**
     * Saves a credential profile into the encrypted vault store.
     */
    public void saveProfile(String masterPassword, CredentialProfile profile) throws Exception {
        if (profile == null || profile.getProfileName() == null || profile.getProfileName().trim().isEmpty()) {
            throw new IllegalArgumentException("Profile name must be specified.");
        }

        Map<String, CredentialProfile> profiles = loadVaultProfiles(masterPassword);
        profiles.put(profile.getProfileName().trim().toLowerCase(), profile);

        saveVaultProfiles(masterPassword, profiles);
        log.info("Saved profile [{}] into vault: {}", profile.getProfileName(), vaultFilePath);
    }

    /**
     * Retrieves a credential profile from the encrypted vault.
     */
    public CredentialProfile getProfile(String masterPassword, String profileName) throws Exception {
        if (profileName == null || profileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Profile name cannot be empty.");
        }

        Map<String, CredentialProfile> profiles = loadVaultProfiles(masterPassword);
        CredentialProfile profile = profiles.get(profileName.trim().toLowerCase());
        if (profile == null) {
            throw new IllegalArgumentException("Profile [" + profileName + "] not found in vault.");
        }
        return profile;
    }

    /**
     * Lists all profile names stored in the vault.
     */
    public List<String> listProfiles(String masterPassword) throws Exception {
        Map<String, CredentialProfile> profiles = loadVaultProfiles(masterPassword);
        return new ArrayList<>(profiles.keySet());
    }

    /**
     * Removes a profile from the vault.
     */
    public boolean removeProfile(String masterPassword, String profileName) throws Exception {
        if (profileName == null || profileName.trim().isEmpty()) {
            return false;
        }

        Map<String, CredentialProfile> profiles = loadVaultProfiles(masterPassword);
        CredentialProfile removed = profiles.remove(profileName.trim().toLowerCase());
        if (removed != null) {
            saveVaultProfiles(masterPassword, profiles);
            log.info("Removed profile [{}] from vault.", profileName);
            return true;
        }
        return false;
    }

    private Map<String, CredentialProfile> loadVaultProfiles(String masterPassword) throws Exception {
        File vaultFile = new File(vaultFilePath);
        if (!vaultFile.exists() || vaultFile.length() == 0) {
            return new HashMap<>();
        }

        byte[] fileBytes = Files.readAllBytes(vaultFile.toPath());
        String base64Encrypted = new String(fileBytes, StandardCharsets.UTF_8).trim();

        String decryptedJson = encryptionService.decryptText(base64Encrypted, masterPassword);
        return objectMapper.readValue(decryptedJson, new TypeReference<Map<String, CredentialProfile>>() {});
    }

    private void saveVaultProfiles(String masterPassword, Map<String, CredentialProfile> profiles) throws Exception {
        String json = objectMapper.writeValueAsString(profiles);
        String encryptedBase64 = encryptionService.encryptText(json, masterPassword);

        File vaultFile = new File(vaultFilePath);
        if (vaultFile.getParentFile() != null && !vaultFile.getParentFile().exists()) {
            vaultFile.getParentFile().mkdirs();
        }

        Files.write(vaultFile.toPath(), encryptedBase64.getBytes(StandardCharsets.UTF_8));
    }
}
