package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.ArrayList;
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

    // Lecturers allowed to manage this module. An empty list means "unrestricted" -
    // every lecturer can still see it, preserving behavior for modules created before
    // this feature existed. Assigning at least one lecturer scopes visibility to them.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "module_lecturers",
        joinColumns = @JoinColumn(name = "module_id"),
        inverseJoinColumns = @JoinColumn(name = "lecturer_username")
    )
    @JsonIgnore
    private List<User> assignedLecturers;

    // Transient holder for incoming create/update requests, which send usernames rather
    // than full User objects. The service layer resolves these into assignedLecturers.
    @Transient
    @JsonIgnore
    private List<String> assignedLecturerUsernamesInput;

    // --- Getters and Setters ---

    public String getModuleId() { return moduleId; }

    public void setModuleId(String moduleId) {
        // Validate: only capital letters, numbers, and spaces allowed
        if (moduleId != null && !moduleId.matches("^[A-Z0-9\\s]+$")) {
            throw new IllegalArgumentException("Module ID must contain only capital letters, numbers, and spaces (A-Z, 0-9, spaces)");
        }
        this.moduleId = moduleId;
    }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public List<Los> getLosList() { return losList; }
    public void setLosList(List<Los> losList) { this.losList = losList; }

    public List<User> getAssignedLecturers() { return assignedLecturers; }
    public void setAssignedLecturers(List<User> assignedLecturers) { this.assignedLecturers = assignedLecturers; }

    @JsonProperty("assignedLecturerUsernames")
    public List<String> getAssignedLecturerUsernames() {
        if (assignedLecturers == null) return new ArrayList<>();
        return assignedLecturers.stream().map(User::getUserID).collect(Collectors.toList());
    }

    public void setAssignedLecturerUsernames(List<String> usernames) {
        this.assignedLecturerUsernamesInput = usernames;
    }

    public List<String> getAssignedLecturerUsernamesInput() { return assignedLecturerUsernamesInput; }

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
