package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.AuditLog;
import com.example.Software.project.Backend.Repository.AuditLogRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Best-effort, append-only application audit writes, isolated from business transactions. */
@Service
public class AuditLogService {
    public static final String REQUEST_ID = AuditLogService.class.getName() + ".requestId";
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository repository;
    private final TransactionTemplate transaction;
    private final int retentionDays;
    private final Counter writeFailures;
    private final Counter cleanupFailures;

    public AuditLogService(AuditLogRepository repository, PlatformTransactionManager manager,
                           MeterRegistry meters, @Value("${app.audit.retention-days:90}") int retentionDays) {
        if (retentionDays < 1 || retentionDays > 3650) throw new IllegalArgumentException("Audit retention must be between 1 and 3650 days.");
        this.repository = repository;
        this.retentionDays = retentionDays;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.writeFailures = meters.counter("app.audit.write.failures");
        this.cleanupFailures = meters.counter("app.audit.cleanup.failures");
    }

    public void log(String actor, String action, String target, String outcome, String details) {
        String requestId = UUID.randomUUID().toString();
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            Object value = attributes.getRequest().getAttribute(REQUEST_ID);
            if (value instanceof String id) requestId = id;
        }
        record(actor, action, target, outcome, details, requestId);
    }

    public void record(String actor, String action, String target, String outcome, String details, String requestId) {
        AuditLog event = new AuditLog(clean(actor), clean(action), clean(target), clean(outcome),
                clean("requestId=" + requestId + "; time=UTC" + (details == null ? "" : "; " + details)));
        if ("SUCCESS".equals(outcome) && TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { write(event); }
            });
        } else {
            write(event);
        }
    }

    private void write(AuditLog event) {
        try {
            // Catch flush AND commit failures outside the independent transaction boundary.
            transaction.executeWithoutResult(status -> repository.saveAndFlush(event));
        } catch (RuntimeException error) {
            writeFailures.increment();
            // Do not log the request, event payload, SQL exception message or stack trace.
            logger.error("Audit write failed; action={}, exception={}", event.getAction(), error.getClass().getSimpleName());
        }
    }

    @Scheduled(cron = "${app.audit.cleanup-cron:0 0 3 * * *}", zone = "UTC")
    public void purgeOldEntries() {
        try {
            transaction.executeWithoutResult(status -> repository.deleteByTimestampBefore(LocalDateTime.now(ZoneOffset.UTC).minusDays(retentionDays)));
        } catch (RuntimeException error) {
            cleanupFailures.increment();
            logger.error("Audit retention cleanup failed; exception={}", error.getClass().getSimpleName());
        }
    }

    public static String clean(String value) {
        if (value == null) return null;
        // Never store raw request bodies/headers. This is an additional guard for bounded identifiers.
        String safe = value.replaceAll("[\\p{Cntrl}\\p{Cf}]", " ")
                .replaceAll("(?i)(password|token|secret|authorization)\\s*[=:]\\s*[^;\\s]+", "$1=[REDACTED]")
                .replaceAll("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+", "[REDACTED]");
        return safe.length() > 255 ? safe.substring(0, 255) : safe;
    }
}
