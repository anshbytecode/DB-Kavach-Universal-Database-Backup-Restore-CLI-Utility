package com.dbbackup.controller;

import com.dbbackup.model.*;
import com.dbbackup.service.security.CredentialVaultService;
import com.dbbackup.service.security.DataMaskingService;
import com.dbbackup.service.security.EncryptionService;
import com.dbbackup.service.security.SecurityAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/security")
@CrossOrigin(origins = "*")
public class SecurityApiController {
    private static final Logger log = LoggerFactory.getLogger(SecurityApiController.class);

    private final SecurityAuditService auditService;
    private final EncryptionService encryptionService;
    private final DataMaskingService maskingService;
    private final CredentialVaultService vaultService;

    @Autowired
    public SecurityApiController(SecurityAuditService auditService,
                                 EncryptionService encryptionService,
                                 DataMaskingService maskingService,
                                 CredentialVaultService vaultService) {
        this.auditService = auditService;
        this.encryptionService = encryptionService;
        this.maskingService = maskingService;
        this.vaultService = vaultService;
    }

    @PostMapping("/audit")
    public ResponseEntity<?> runAudit(@RequestBody DatabaseCredentials credentials,
                                      @RequestParam(required = false, defaultValue = "./temp-backups") String backupDir) {
        try {
            SecurityAuditReport report = auditService.performSecurityAudit(credentials, backupDir);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/encrypt")
    public ResponseEntity<?> encryptFile(@RequestBody Map<String, String> body) {
        try {
            String filePath = body.get("filePath");
            String passphrase = body.get("passphrase");
            String outputPath = body.get("outputPath");

            File inputFile = new File(filePath);
            File outputFile = (outputPath != null && !outputPath.trim().isEmpty()) ? new File(outputPath) : new File(filePath + ".enc");

            encryptionService.encryptFile(inputFile, outputFile, passphrase);

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("outputFile", outputFile.getAbsolutePath());
            res.put("sizeBytes", outputFile.length());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/decrypt")
    public ResponseEntity<?> decryptFile(@RequestBody Map<String, String> body) {
        try {
            String filePath = body.get("filePath");
            String passphrase = body.get("passphrase");
            String outputPath = body.get("outputPath");

            File inputFile = new File(filePath);
            File outputFile = (outputPath != null && !outputPath.trim().isEmpty()) ? new File(outputPath) : new File(filePath.replace(".enc", ""));

            encryptionService.decryptFile(inputFile, outputFile, passphrase);

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("outputFile", outputFile.getAbsolutePath());
            res.put("sizeBytes", outputFile.length());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/mask")
    public ResponseEntity<?> maskFile(@RequestBody Map<String, String> body) {
        try {
            String filePath = body.get("filePath");
            String outputPath = body.get("outputPath");

            File inputFile = new File(filePath);
            File outputFile = (outputPath != null && !outputPath.trim().isEmpty()) ? new File(outputPath) : new File(filePath + ".masked");

            maskingService.maskDumpFile(inputFile, outputFile);

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("outputFile", outputFile.getAbsolutePath());
            res.put("sizeBytes", outputFile.length());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/vault/save")
    public ResponseEntity<?> saveVaultProfile(@RequestParam String masterPassword, @RequestBody CredentialProfile profile) {
        try {
            vaultService.saveProfile(masterPassword, profile);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Profile [" + profile.getProfileName() + "] saved to vault.");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/vault/list")
    public ResponseEntity<?> listVaultProfiles(@RequestParam String masterPassword) {
        try {
            List<String> profiles = vaultService.listProfiles(masterPassword);
            Map<String, Object> res = new HashMap<>();
            res.put("profiles", profiles);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/vault/get")
    public ResponseEntity<?> getVaultProfile(@RequestParam String masterPassword, @RequestParam String name) {
        try {
            CredentialProfile profile = vaultService.getProfile(masterPassword, name);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/vault/remove")
    public ResponseEntity<?> removeVaultProfile(@RequestParam String masterPassword, @RequestParam String name) {
        try {
            boolean removed = vaultService.removeProfile(masterPassword, name);
            Map<String, Object> res = new HashMap<>();
            res.put("success", removed);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
