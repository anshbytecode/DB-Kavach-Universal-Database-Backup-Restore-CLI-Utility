package com.dbbackup.service.notification;

import com.dbbackup.model.BackupHistoryRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SlackNotificationService {
    private static final Logger log = LoggerFactory.getLogger(SlackNotificationService.class);

    @Value("${backup.slack.enabled:false}")
    private boolean slackEnabled;

    @Value("${backup.slack.webhook-url:}")
    private String defaultWebhookUrl;

    private final RestTemplate restTemplate;

    public SlackNotificationService() {
        this.restTemplate = new RestTemplate();
    }

    public void sendNotification(BackupHistoryRecord record, String customWebhookUrl) {
        String targetWebhookUrl = (customWebhookUrl != null && !customWebhookUrl.trim().isEmpty())
                ? customWebhookUrl : defaultWebhookUrl;

        if (targetWebhookUrl == null || targetWebhookUrl.trim().isEmpty()) {
            log.info("Slack notification skipped: No webhook URL provided.");
            return;
        }

        try {
            log.info("Sending Slack notification for backup job {}", record.getBackupId());

            boolean isSuccess = "SUCCESS".equalsIgnoreCase(record.getStatus());
            String color = isSuccess ? "#36a64f" : "#ff0000";
            String title = (isSuccess ? "✅ Backup Succeeded: " : "❌ Backup Failed: ") + record.getDatabaseName();

            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", color);
            attachment.put("title", title);
            attachment.put("text", "Database Backup Utility Activity Alert");

            List<Map<String, Object>> fields = List.of(
                    Map.of("title", "Backup ID", "value", String.valueOf(record.getBackupId()), "short", true),
                    Map.of("title", "DBMS Type", "value", String.valueOf(record.getDbmsType()), "short", true),
                    Map.of("title", "Backup Type", "value", String.valueOf(record.getBackupType()), "short", true),
                    Map.of("title", "Storage", "value", String.valueOf(record.getStorageType()), "short", true),
                    Map.of("title", "Size (bytes)", "value", String.valueOf(record.getSizeBytes()), "short", true),
                    Map.of("title", "Duration (ms)", "value", String.valueOf(record.getDurationMs()), "short", true),
                    Map.of("title", "Location", "value", String.valueOf(record.getStorageLocation()), "short", false)
            );

            attachment.put("fields", fields);

            if (!isSuccess && record.getErrorMessage() != null) {
                attachment.put("footer", "Error: " + record.getErrorMessage());
            }

            Map<String, Object> payload = Map.of("attachments", List.of(attachment));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(targetWebhookUrl, requestEntity, String.class);
            log.info("Slack notification sent successfully.");

        } catch (Exception e) {
            log.warn("Failed to send Slack notification: {}", e.getMessage());
        }
    }
}
