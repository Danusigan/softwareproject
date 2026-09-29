package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    void deleteByTimestampBefore(LocalDateTime cutoff);


    Optional<AuditLog> findFirstByActorAndActionAndOutcomeOrderByTimestampAsc(String actor, String action, String outcome);

    Optional<AuditLog> findFirstByActorAndActionAndOutcomeOrderByTimestampDesc(String actor, String action, String outcome);
}
