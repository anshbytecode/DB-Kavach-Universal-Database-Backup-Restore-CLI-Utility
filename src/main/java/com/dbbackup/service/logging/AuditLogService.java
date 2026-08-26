package com.dbbackup.service.logging;

import com.dbbackup.model.BackupHistoryRecord;
import com.dbbackup.model.DbmsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final BackupHistoryRepository repository;

    @Autowired
    public AuditLogService(BackupHistoryRepository repository) {
        this.repository = repository;
    }

    public BackupHistoryRecord recordActivity(BackupHistoryRecord record) {
        log.info("Recording Audit Log: [{}] Operation: {}, DBMS: {}, Status: {}, Duration: {}ms",
                record.getBackupId(), record.getOperation(), record.getDbmsType(), record.getStatus(), record.getDurationMs());
        return repository.save(record);
    }

    public List<BackupHistoryRecord> getRecentHistory() {
        return repository.findTop20ByOrderByStartTimeDesc();
    }

    public List<BackupHistoryRecord> getAllHistory() {
        return repository.findAll();
    }

    public List<BackupHistoryRecord> getHistoryByStatus(String status) {
        return repository.findByStatus(status);
    }

    public List<BackupHistoryRecord> getHistoryByDbms(DbmsType dbmsType) {
        return repository.findByDbmsType(dbmsType);
    }
}
