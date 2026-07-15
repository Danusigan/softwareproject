package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    void deleteByTimestampBefore(LocalDateTime cutoff);
}
