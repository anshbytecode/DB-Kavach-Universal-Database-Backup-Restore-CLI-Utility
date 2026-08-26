package com.dbbackup.controller;

import com.dbbackup.model.*;
import com.dbbackup.service.backup.BackupService;
import com.dbbackup.service.logging.AuditLogService;
import com.dbbackup.service.restore.RestoreService;
import com.dbbackup.service.security.CredentialVaultService;
import com.dbbackup.dbms.DbmsAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BackupApiController {
    private static final Logger log = LoggerFactory.getLogger(BackupApiController.class);

    private final BackupService backupService;
    private final RestoreService restoreService;
    private final AuditLogService auditLogService;
    private final DbmsAdapterFactory dbmsAdapterFactory;
    private final CredentialVaultService vaultService;

    @Autowired
    public BackupApiController(BackupService backupService,
                               RestoreService restoreService,
                               AuditLogService auditLogService,
                               DbmsAdapterFactory dbmsAdapterFactory,
                               CredentialVaultService vaultService) {
        this.backupService = backupService;
        this.restoreService = restoreService;
        this.auditLogService = auditLogService;
        this.dbmsAdapterFactory = dbmsAdapterFactory;
        this.vaultService = vaultService;
    }

    @PostMapping("/backup")
    public ResponseEntity<?> executeBackup(@RequestBody BackupRequest request) {
        try {
            log.info("API Request: Execute Backup for DBMS: {}, DB: {}", 
                     request.getCredentials() != null ? request.getCredentials().getDbmsType() : "NULL",
                     request.getCredentials() != null ? request.getCredentials().getDatabaseName() : "NULL");

            BackupMetadata metadata = backupService.executeBackup(request);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            log.error("API Backup Execution Failed: {}", e.getMessage(), e);
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            err.put("status", "FAILED");
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/restore")
    public ResponseEntity<?> executeRestore(@RequestBody RestoreRequest request) {
        try {
            log.info("API Request: Execute Restore for DBMS: {}, DB: {}",
                     request.getTargetCredentials() != null ? request.getTargetCredentials().getDbmsType() : "NULL",
                     request.getTargetCredentials() != null ? request.getTargetCredentials().getDatabaseName() : "NULL");

            boolean success = restoreService.executeRestore(request);
            Map<String, Object> res = new HashMap<>();
            res.put("success", success);
            res.put("message", "Database restore completed successfully!");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("API Restore Execution Failed: {}", e.getMessage(), e);
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            err.put("status", "FAILED");
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/test-connection")
    public ResponseEntity<?> testConnection(@RequestBody DatabaseCredentials credentials) {
        try {
            boolean valid = dbmsAdapterFactory.getAdapter(credentials.getDbmsType()).testConnection(credentials);
            List<String> tables = dbmsAdapterFactory.getAdapter(credentials.getDbmsType()).getTables(credentials);
            Map<String, Object> res = new HashMap<>();
            res.put("valid", valid);
            res.put("tables", tables);
            res.put("message", "Database connection successful!");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("valid", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<BackupHistoryRecord>> getHistory() {
        List<BackupHistoryRecord> history = auditLogService.getAllHistory();
        return ResponseEntity.ok(history);
    }
}
