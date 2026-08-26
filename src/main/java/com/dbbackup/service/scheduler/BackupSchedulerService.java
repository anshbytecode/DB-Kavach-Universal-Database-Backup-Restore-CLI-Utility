package com.dbbackup.service.scheduler;

import com.dbbackup.model.BackupRequest;
import com.dbbackup.service.backup.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class BackupSchedulerService {
    private static final Logger log = LoggerFactory.getLogger(BackupSchedulerService.class);

    private final ThreadPoolTaskScheduler scheduler;
    private final BackupService backupService;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Autowired
    public BackupSchedulerService(BackupService backupService) {
        this.backupService = backupService;
        this.scheduler = new ThreadPoolTaskScheduler();
        this.scheduler.setPoolSize(5);
        this.scheduler.setThreadNamePrefix("backup-scheduler-");
        this.scheduler.initialize();
    }

    public String scheduleBackup(BackupRequest request, String cronExpression) {
        String jobName = "backup_job_" + request.getCredentials().getDbmsType() + "_" + System.currentTimeMillis();
        log.info("Scheduling automated backup job [{}] with cron: {}", jobName, cronExpression);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                log.info("Executing scheduled backup job [{}]...", jobName);
                backupService.executeBackup(request);
            } catch (Exception e) {
                log.error("Scheduled backup job [{}] failed: {}", jobName, e.getMessage());
            }
        }, new CronTrigger(cronExpression));

        scheduledTasks.put(jobName, future);
        return jobName;
    }

    public boolean cancelSchedule(String jobName) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobName);
        if (future != null) {
            return future.cancel(true);
        }
        return false;
    }

    public Map<String, ScheduledFuture<?>> getActiveSchedules() {
        return scheduledTasks;
    }
}
