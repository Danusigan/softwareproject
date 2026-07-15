package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.AuditLog;
import com.example.Software.project.Backend.Repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Records login attempts and privileged actions (mapping approval, marks edits, PO changes,
 * admin/lecturer creation) for accountability, per the Phase 4 secure design decision.
 * Records older than 90 days are purged daily.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final int RETENTION_DAYS = 90;

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String actor, String action, String target, String outcome, String details) {
        try {
            auditLogRepository.save(new AuditLog(actor, action, target, outcome, details));
        } catch (Exception e) {
            // Audit logging must never break the calling request
            log.error("Failed to write audit log entry: action={}, actor={}", action, actor, e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *") // daily at 03:00
    public void purgeOldEntries() {
        auditLogRepository.deleteByTimestampBefore(LocalDateTime.now().minusDays(RETENTION_DAYS));
    }
}
