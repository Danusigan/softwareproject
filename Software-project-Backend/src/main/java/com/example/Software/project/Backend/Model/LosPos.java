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

    // Foreign Key to Module
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    @JsonIgnore // Prevent infinite recursion
    private Module module;

    // Relationship to Assignment
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;
    
    // Relationship to OutcomeMapping
    @OneToMany(mappedBy = "learningOutcome", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Prevent recursion
    private List<OutcomeMapping> mappings;

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }

    public Assignment getAssignment() { return assignment; }
    public void setAssignment(Assignment assignment) { this.assignment = assignment; }
    
    public List<OutcomeMapping> getMappings() { return mappings; }
    public void setMappings(List<OutcomeMapping> mappings) { this.mappings = mappings; }

    // Helper to expose just the module ID in the JSON
    @JsonProperty("moduleId")
    public String getModuleId() {
        return module != null ? module.getModuleId() : null;
    }
}