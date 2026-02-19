package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lo_po_mappings")
public class OutcomeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "los_id", nullable = false) // Renamed column
    @JsonIgnore // Ignore getter to prevent recursion
    private Los learningOutcome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_outcome_id", nullable = false)
    @JsonIgnore // Ignore getter to prevent recursion
    private ProgramOutcome programOutcome;

    @Column(name = "weight", nullable = false)
    private Integer weight; // 0 = No correlation, 1 = Low, 2 = Medium, 3 = High

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "lecturer_remarks", columnDefinition = "TEXT")
    private String lecturerRemarks;

    @Column(name = "admin_remarks", columnDefinition = "TEXT")
    private String adminRemarks;

    @Column(name = "mapped_by")
    private String mappedBy; // Lecturer who created the mapping

    @Column(name = "reviewed_by")
    private String reviewedBy; // Admin who approved/rejected

    @Column(name = "mapped_at", updatable = false)
    private LocalDateTime mappedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ApprovalStatus {
        PENDING,    // Waiting for admin approval
        APPROVED,   // Approved by admin
        REJECTED    // Rejected by admin
    }

    @PrePersist
    protected void onCreate() {
        mappedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ApprovalStatus.PENDING;
        }
        // Validate weight range
        if (weight != null && (weight < 0 || weight > 3)) {
            weight = Math.max(0, Math.min(3, weight));
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Validate weight range
        if (weight != null && (weight < 0 || weight > 3)) {
            weight = Math.max(0, Math.min(3, weight));
        }
    }

    // Default constructor
    public OutcomeMapping() {}

    // Constructor
    public OutcomeMapping(Los learningOutcome, ProgramOutcome programOutcome, Integer weight, String mappedBy) {
        this.learningOutcome = learningOutcome;
        this.programOutcome = programOutcome;
        this.weight = weight;
        this.mappedBy = mappedBy;
        this.status = ApprovalStatus.PENDING;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonIgnore // Hide full object from output
    public Los getLearningOutcome() {
        return learningOutcome;
    }

    @JsonProperty("learningOutcome") // Allow setting full object from input
    public void setLearningOutcome(Los learningOutcome) {
        this.learningOutcome = learningOutcome;
    }

    @JsonIgnore // Hide full object from output
    public ProgramOutcome getProgramOutcome() {
        return programOutcome;
    }

    @JsonProperty("programOutcome") // Allow setting full object from input
    public void setProgramOutcome(ProgramOutcome programOutcome) {
        this.programOutcome = programOutcome;
    }

    // Custom getters for IDs to be included in JSON output
    @JsonProperty("learningOutcomeId")
    public String getLearningOutcomeId() {
        return learningOutcome != null ? learningOutcome.getId() : null;
    }

    @JsonProperty("programOutcomeCode")
    public String getProgramOutcomeCode() {
        return programOutcome != null ? programOutcome.getCode() : null;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        // Clamp weight between 0 and 3
        if (weight != null) {
            this.weight = Math.max(0, Math.min(3, weight));
        } else {
            this.weight = weight;
        }
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public String getLecturerRemarks() {
        return lecturerRemarks;
    }

    public void setLecturerRemarks(String lecturerRemarks) {
        this.lecturerRemarks = lecturerRemarks;
    }

    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }

    public String getMappedBy() {
        return mappedBy;
    }

    public void setMappedBy(String mappedBy) {
        this.mappedBy = mappedBy;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getMappedAt() {
        return mappedAt;
    }

    public void setMappedAt(LocalDateTime mappedAt) {
        this.mappedAt = mappedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public boolean isApproved() {
        return status == ApprovalStatus.APPROVED;
    }

    public boolean isPending() {
        return status == ApprovalStatus.PENDING;
    }

    public boolean isRejected() {
        return status == ApprovalStatus.REJECTED;
    }

    public String getWeightDescription() {
        switch (weight) {
            case 0: return "No Correlation";
            case 1: return "Low Correlation";
            case 2: return "Medium Correlation";
            case 3: return "High Correlation";
            default: return "Unknown";
        }
    }

    // Approve this mapping
    public void approve(String reviewedBy, String adminRemarks) {
        this.status = ApprovalStatus.APPROVED;
        this.reviewedBy = reviewedBy;
        this.adminRemarks = adminRemarks;
        this.reviewedAt = LocalDateTime.now();
    }

    // Reject this mapping
    public void reject(String reviewedBy, String adminRemarks) {
        this.status = ApprovalStatus.REJECTED;
        this.reviewedBy = reviewedBy;
        this.adminRemarks = adminRemarks;
        this.reviewedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "OutcomeMapping{" +
                "id=" + id +
                ", learningOutcome=" + (learningOutcome != null ? learningOutcome.getId() : "null") +
                ", programOutcome=" + (programOutcome != null ? programOutcome.getCode() : "null") +
                ", weight=" + weight +
                ", status=" + status +
                ", mappedBy='" + mappedBy + '\'' +
                '}';
    }
}
