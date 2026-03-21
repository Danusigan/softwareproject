package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "program_outcomes")
public class ProgramOutcome {
    @Id
    @Column(name = "po_id", unique = true, nullable = false)
    private String poId; // e.g., "PO1", "PO2" - capital letters, numbers, spaces only

    @Column(name = "po_code", unique = true, nullable = false)
    private String code; // e.g., "PO1", "PO2" - unique code for reference

    // Legacy schema compatibility: some DBs still require a non-null `code` column.
    @JsonIgnore
    @Column(name = "code", nullable = false)
    private String legacyCode;

    @Column(name = "title", nullable = false)
    private String title; // Short title (e.g., "Engineering Knowledge")

    @Column(name = "po_description", columnDefinition = "TEXT")
    private String description; // Detailed description

    @Column(name = "category")
    private String category; // e.g., "Knowledge", "Skills", "Attitude"

    @Column(name = "performance_indicators", columnDefinition = "TEXT")
    private String performanceIndicators; // Measurable indicators

    @Column(name = "is_default", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDefault = false; // True for Washington Accord standard POs

    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive = true; // For soft delete functionality

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "display_order")
    private Integer displayOrder; // For ordering in lists

    public ProgramOutcome() {}

    public ProgramOutcome(String poId, String code, String title, String description) {
        setPoId(poId);
        this.code = code;
        this.title = title;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ProgramOutcome(String poId, String code, String title, String description, String category, String performanceIndicators, Boolean isDefault, Integer displayOrder) {
        setPoId(poId);
        this.code = code;
        this.title = title;
        this.description = description;
        this.category = category;
        this.performanceIndicators = performanceIndicators;
        this.isDefault = isDefault;
        this.displayOrder = displayOrder;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.poId = this.poId == null ? null : this.poId.trim().toUpperCase();
        this.code = (this.code == null || this.code.trim().isEmpty()) ? this.poId : this.code.trim().toUpperCase();
        this.legacyCode = this.code;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isDefault == null) this.isDefault = false;
        if (this.isActive == null) this.isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.poId = this.poId == null ? null : this.poId.trim().toUpperCase();
        this.code = (this.code == null || this.code.trim().isEmpty()) ? this.poId : this.code.trim().toUpperCase();
        this.legacyCode = this.code;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getPoId() { return poId; }
    public void setPoId(String poId) {
        // Always normalize - never validate here (validation in service layer)
        this.poId = poId == null ? null : poId.trim().toUpperCase();
    }

    // Backward compatibility for existing code that uses getId/setId
    public String getId() { return poId; }
    public void setId(String id) { setPoId(id); }

    public String getCode() { return code; }
    public void setCode(String code) {
        // Never allow null or empty code - always normalize
        if (code == null || code.trim().isEmpty()) {
            this.code = this.poId; // Default to poId if no code provided
        } else {
            this.code = code.trim().toUpperCase();
        }
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPerformanceIndicators() { return performanceIndicators; }
    public void setPerformanceIndicators(String performanceIndicators) { this.performanceIndicators = performanceIndicators; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    @Override
    public String toString() {
        return "ProgramOutcome{" +
                "poId='" + poId + '\'' +
                ", code='" + code + '\'' +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", isDefault=" + isDefault +
                ", isActive=" + isActive +
                '}';
    }
}