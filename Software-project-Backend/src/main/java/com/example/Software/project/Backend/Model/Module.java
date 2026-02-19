package com.example.Software.project.Backend.Model;

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

    // Removed academicYear as requested

    // Relationship: One Module has many Los
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Los> losList; // Renamed from losPosList

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

    public List<Los> getLosList() { return losList; }
    public void setLosList(List<Los> losList) { this.losList = losList; }

    /**
     * Derived Attribute:
     * Returns a list of names derived from the connected Los objects.
     * This acts "like an attribute" but is calculated from the relationship.
     */
    @Transient // This annotation tells JPA not to store this column in the DB, it's calculated on the fly
    public List<String> getLosNames() {
        if (losList == null) return null;
        return losList.stream()
                .map(Los::getName)
                .collect(Collectors.toList());
    }
}
