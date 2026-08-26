package com.dbbackup.service.logging;

import com.dbbackup.model.BackupHistoryRecord;
import com.dbbackup.model.DbmsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupHistoryRepository extends JpaRepository<BackupHistoryRecord, Long> {
    List<BackupHistoryRecord> findByStatus(String status);
    List<BackupHistoryRecord> findByDbmsType(DbmsType dbmsType);
    List<BackupHistoryRecord> findTop20ByOrderByStartTimeDesc();
}
