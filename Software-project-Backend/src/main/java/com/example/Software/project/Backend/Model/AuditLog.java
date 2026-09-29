package com.example.Software.project.Backend.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Records login attempts and privileged actions (mapping approval, marks edits, PO changes,
 * admin/lecturer creation) for accountability. Retained 90 days — see AuditLogService cleanup.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actor;

    private String action;

    private String target;

    private String outcome;

    private String details;

    private LocalDateTime timestamp;

    public AuditLog() {
    }

    public AuditLog(String actor, String action, String target, String outcome, String details) {
        this.actor = actor;
        this.action = action;
        this.target = target;
        this.outcome = outcome;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getTarget() {
        return target;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
