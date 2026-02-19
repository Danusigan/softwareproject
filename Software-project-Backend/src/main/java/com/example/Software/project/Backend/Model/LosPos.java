package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "los_pos")
public class LosPos {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private String id; // Changed to String for custom ID

    @Column(name = "name")
    private String name; // The "Lo&Po Name"

    @Column(name = "lo_id")
    private String loId;

    @Column(name = "lo_description")
    private String loDescription;

    @Column(name = "module_code")
    private String moduleCode;

    // Foreign Key to Module
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    @JsonIgnore // Prevent infinite recursion
    private Module module;

    // Relationship to Assignments (One LO has many result batches)
    @OneToMany(mappedBy = "losPos", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Assignment> assignments;
    
    // Relationship to OutcomeMapping
    @OneToMany(mappedBy = "learningOutcome", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Prevent recursion
    private List<OutcomeMapping> mappings;

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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
    
    public List<OutcomeMapping> getMappings() { return mappings; }
    public void setMappings(List<OutcomeMapping> mappings) { this.mappings = mappings; }

    // Helper to expose just the module ID in the JSON
    @JsonProperty("moduleId")
    public String getModuleId() {
        return module != null ? module.getModuleId() : null;
    }
}