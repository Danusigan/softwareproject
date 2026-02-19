package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @Column(name = "assignment_id", unique = true, nullable = false)
    private String assignmentId; // Changed to String for custom ID

    @Column(name = "assignment_name")
    private String assignmentName;

    @Column(name = "academic_year", nullable = false)
    private String academicYear; // e.g., "2023-2024"

    // Storing the CSV file content inside the DB
    // @Lob indicates a Large Object (BLOB)
    @Lob
    @Column(name = "marks_csv_file", columnDefinition = "LONGBLOB")
    private byte[] marksCsvFile;

    // Optional: Store filename if needed
    private String fileName;

    // Bidirectional relationship for JPQL queries
    @OneToOne(mappedBy = "assignment")
    @JsonIgnore // Prevent infinite recursion
    private Los los; // Renamed from losPos

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

    // --- Getters and Setters ---

    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }

    public String getAssignmentName() { return assignmentName; }
    public void setAssignmentName(String assignmentName) { this.assignmentName = assignmentName; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public byte[] getMarksCsvFile() { return marksCsvFile; }
    public void setMarksCsvFile(byte[] marksCsvFile) { this.marksCsvFile = marksCsvFile; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Los getLos() { return los; }
    public void setLos(Los los) { this.los = los; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
