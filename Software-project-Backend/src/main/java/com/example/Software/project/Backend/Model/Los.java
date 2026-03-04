package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "los") // Renamed table
public class Los {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private String id; // Changed to String for custom ID

    @Column(name = "name")
    private String name; // The "Lo Name"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Description of the Learning Outcome

    @Column(name = "batch")
    private String batch; // e.g., "24", "25" (batch year for marks)

    // Storing the marks file content inside the DB
    @Lob
    @Column(name = "marks_csv_file", columnDefinition = "LONGBLOB")
    private byte[] marksCsvFile;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Foreign Key to Module
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    @JsonIgnore // Prevent infinite recursion
    private Module module;

    // Relationship to OutcomeMapping
    @OneToMany(mappedBy = "learningOutcome", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Prevent recursion
    private List<OutcomeMapping> mappings;

    // Relationship to Student Marks
    @OneToMany(mappedBy = "los", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    private List<StudentMark> studentMarks;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) {
        // Validate: only capital letters, numbers, and spaces allowed
        if (id != null && !id.matches("^[A-Z0-9\\s]+$")) {
            throw new IllegalArgumentException(
                "Los ID must contain only capital letters, numbers, and spaces (A-Z, 0-9, spaces)"
            );
        }
        this.id = id;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public byte[] getMarksCsvFile() { return marksCsvFile; }
    public void setMarksCsvFile(byte[] marksCsvFile) { this.marksCsvFile = marksCsvFile; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }

    public List<OutcomeMapping> getMappings() { return mappings; }
    public void setMappings(List<OutcomeMapping> mappings) { this.mappings = mappings; }

    public List<StudentMark> getStudentMarks() { return studentMarks; }
    public void setStudentMarks(List<StudentMark> studentMarks) { this.studentMarks = studentMarks; }

    // Helper to expose just the module ID in the JSON
    @JsonProperty("moduleId")
    public String getModuleId() {
        return module != null ? module.getModuleId() : null;
    }
}
