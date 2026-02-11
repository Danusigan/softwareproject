package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lo_po_mappings")
public class Mapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lospos_id", nullable = false)
    @JsonBackReference
    private LosPos losPos;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_outcome_id", nullable = false)
    @JsonBackReference
    private ProgramOutcome programOutcome;
    
    @Column(name = "weight", nullable = false)
    private Integer weight; // 0 = No correlation, 1 = Low, 2 = Medium, 3 = High
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MappingStatus status = MappingStatus.PENDING;
    
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
    
    public enum MappingStatus {
        PENDING,    // Waiting for admin approval
        APPROVED,   // Approved by admin
        REJECTED    // Rejected by admin
    }
    
    @PrePersist
    protected void onCreate() {
        mappedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = MappingStatus.PENDING;
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
    public Mapping() {}
    
    // Constructor
    public Mapping(LosPos losPos, ProgramOutcome programOutcome, Integer weight, String mappedBy) {
        this.losPos = losPos;
        this.programOutcome = programOutcome;
        this.weight = weight;
        this.mappedBy = mappedBy;
        this.status = MappingStatus.PENDING;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LosPos getLosPos() {
        return losPos;
    }
    
    public void setLosPos(LosPos losPos) {
        this.losPos = losPos;
    }
    
    public ProgramOutcome getProgramOutcome() {
        return programOutcome;
    }
    
    public void setProgramOutcome(ProgramOutcome programOutcome) {
        this.programOutcome = programOutcome;
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
    
    public MappingStatus getStatus() {
        return status;
    }
    
    public void setStatus(MappingStatus status) {
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
        return status == MappingStatus.APPROVED;
    }
    
    public boolean isPending() {
        return status == MappingStatus.PENDING;
    }
    
    public boolean isRejected() {
        return status == MappingStatus.REJECTED;
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
        this.status = MappingStatus.APPROVED;
        this.reviewedBy = reviewedBy;
        this.adminRemarks = adminRemarks;
        this.reviewedAt = LocalDateTime.now();
    }
    
    // Reject this mapping
    public void reject(String reviewedBy, String adminRemarks) {
        this.status = MappingStatus.REJECTED;
        this.reviewedBy = reviewedBy;
        this.adminRemarks = adminRemarks;
        this.reviewedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "Mapping{" +
                "id=" + id +
                ", losPos=" + (losPos != null ? losPos.getLoId() : "null") +
                ", programOutcome=" + (programOutcome != null ? programOutcome.getPoCode() : "null") +
                ", weight=" + weight +
                ", status=" + status +
                ", mappedBy='" + mappedBy + '\'' +
                '}';
    }
}