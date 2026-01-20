package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

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
    @JsonIgnore // Prevent infinite recursion and fetching the whole module object
    private Module module;

    // Relationship to Assignment
    // Changed to OneToOne with CascadeType.ALL so deleting LosPos deletes Assignment
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }

    public Assignment getAssignment() { return assignment; }
    public void setAssignment(Assignment assignment) { this.assignment = assignment; }

    // Helper to expose just the module ID in the JSON
    @JsonProperty("moduleId")
    public String getModuleId() {
        return module != null ? module.getModuleId() : null;
    }
}