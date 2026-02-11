package com.example.Software.project.Backend.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "program_outcomes")
public class ProgramOutcome {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "po_code", unique = true, nullable = false)
    private String poCode;
    
    @Column(name = "po_description", nullable = false, columnDefinition = "TEXT")
    private String poDescription;
    
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false; // Washington Accord defaults vs custom
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "programOutcome", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Mapping> mappings;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Default constructor
    public ProgramOutcome() {}
    
    // Constructor
    public ProgramOutcome(String poCode, String poDescription) {
        this.poCode = poCode;
        this.poDescription = poDescription;
        this.isDefault = false;
        this.isActive = true;
    }
    
    // Constructor for default POs
    public ProgramOutcome(String poCode, String poDescription, boolean isDefault, String createdBy) {
        this.poCode = poCode;
        this.poDescription = poDescription;
        this.isDefault = isDefault;
        this.isActive = true;
        this.createdBy = createdBy;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPoCode() {
        return poCode;
    }
    
    public void setPoCode(String poCode) {
        this.poCode = poCode;
    }
    
    public String getPoDescription() {
        return poDescription;
    }
    
    public void setPoDescription(String poDescription) {
        this.poDescription = poDescription;
    }
    
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<Mapping> getMappings() {
        return mappings;
    }
    
    public void setMappings(List<Mapping> mappings) {
        this.mappings = mappings;
    }
    
    @Override
    public String toString() {
        return "ProgramOutcome{" +
                "id=" + id +
                ", poCode='" + poCode + '\'' +
                ", poDescription='" + poDescription + '\'' +
                ", isDefault=" + isDefault +
                ", isActive=" + isActive +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }
    
    // Static method to create default Washington Accord POs
    public static ProgramOutcome createDefaultPO(String poCode, String description, String createdBy) {
        return new ProgramOutcome(poCode, description, true, createdBy);
    }
}