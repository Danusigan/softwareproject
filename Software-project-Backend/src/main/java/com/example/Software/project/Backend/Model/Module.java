package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "modules")
public class Module {

    @Id
    @Column(name = "module_id", unique = true, nullable = false)
    private String moduleId; // Changed to String to allow custom IDs like "SE101"

    @Column(name = "module_name")
    private String moduleName;

    // Relationship: One Module has many LosPos
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<LosPos> losPosList;

    // --- Getters and Setters ---

    public String getModuleId() { return moduleId; }
    
    public void setModuleId(String moduleId) {
        // Validate: only capital letters and digits allowed
        if (moduleId != null && !moduleId.matches("^[A-Z0-9]+$")) {
            throw new IllegalArgumentException("Module ID must contain only capital letters and digits (A-Z, 0-9)");
        }
        this.moduleId = moduleId;
    }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public List<LosPos> getLosPosList() { return losPosList; }
    public void setLosPosList(List<LosPos> losPosList) { this.losPosList = losPosList; }

    /**
     * Derived Attribute:
     * Returns a list of names derived from the connected LosPos objects.
     * This acts "like an attribute" but is calculated from the relationship.
     */
    @Transient // This annotation tells JPA not to store this column in the DB, it's calculated on the fly
    public List<String> getLosPosNames() {
        if (losPosList == null) return null;
        return losPosList.stream()
                .map(LosPos::getName)
                .collect(Collectors.toList());
    }
}