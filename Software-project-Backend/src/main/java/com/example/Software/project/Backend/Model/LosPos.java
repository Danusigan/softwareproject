package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "los_pos")
public class LosPos {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private String id;

    @Column(name = "lo_id", nullable = false)
    private String loId;

    @Column(name = "lo_description", nullable = false, columnDefinition = "TEXT")
    private String loDescription;

    @Column(name = "module_code", nullable = false)
    private String moduleCode;

    // Relationship to Module (Many LosPos belong to one Module)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_code", referencedColumnName = "module_id", insertable = false, updatable = false)
    @JsonBackReference
    private Module module;

    // Relationship to Assignment (One LO can have multiple assignments)
    @OneToMany(mappedBy = "losPos", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Assignment> assignments;

    // Relationship to Mappings (One LO can be mapped to multiple POs)
    @OneToMany(mappedBy = "losPos", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Mapping> mappings;

    // Audit fields
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
    public LosPos() {}

    // Constructor
    public LosPos(String id, String loId, String loDescription, String moduleCode) {
        this.id = id;
        this.loId = loId;
        this.loDescription = loDescription;
        this.moduleCode = moduleCode;
    }

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLoId() { return loId; }
    public void setLoId(String loId) { this.loId = loId; }

    public String getLoDescription() { return loDescription; }
    public void setLoDescription(String loDescription) { this.loDescription = loDescription; }

    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }

    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }

    public List<Assignment> getAssignments() { return assignments; }
    public void setAssignments(List<Assignment> assignments) { this.assignments = assignments; }

    public List<Mapping> getMappings() { return mappings; }
    public void setMappings(List<Mapping> mappings) { this.mappings = mappings; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Backward compatibility methods
    public String getName() { return loDescription; }
    public void setName(String name) { this.loDescription = name; }

    @Override
    public String toString() {
        return "LosPos{" +
                "id='" + id + '\'' +
                ", loId='" + loId + '\'' +
                ", loDescription='" + loDescription + '\'' +
                ", moduleCode='" + moduleCode + '\'' +
                '}';
    }
}