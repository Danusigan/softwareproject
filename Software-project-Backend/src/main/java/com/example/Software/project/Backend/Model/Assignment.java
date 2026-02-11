package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @Column(name = "assignment_id", unique = true, nullable = false)
    private String assignmentId;

    @Column(name = "assignment_name", nullable = false)
    private String assignmentName;

    @Column(name = "assignment_description", columnDefinition = "TEXT")
    private String assignmentDescription;

    // Relationship to LosPos (Many assignments can belong to one LO)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lospos_id")
    private LosPos losPos;

    // Storing the CSV file content inside the DB
    @Lob
    @Column(name = "marks_csv_file", columnDefinition = "LONGBLOB")
    private byte[] marksCsvFile;

    // Optional: Store filename if needed
    @Column(name = "file_name")
    private String fileName;

    // Maximum marks for this assignment
    @Column(name = "max_marks")
    private Double maxMarks = 100.0;

    // Weight of assignment in final calculation
    @Column(name = "assignment_weight")
    private Double assignmentWeight = 1.0;

    // Relationship to student marks
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<StudentMark> studentMarks;

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
    public Assignment() {}

    // Constructor
    public Assignment(String assignmentId, String assignmentName) {
        this.assignmentId = assignmentId;
        this.assignmentName = assignmentName;
    }

    // --- Getters and Setters ---

    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }

    public String getAssignmentName() { return assignmentName; }
    public void setAssignmentName(String assignmentName) { this.assignmentName = assignmentName; }

    public String getAssignmentDescription() { return assignmentDescription; }
    public void setAssignmentDescription(String assignmentDescription) { this.assignmentDescription = assignmentDescription; }

    public LosPos getLosPos() { return losPos; }
    public void setLosPos(LosPos losPos) { this.losPos = losPos; }

    public byte[] getMarksCsvFile() { return marksCsvFile; }
    public void setMarksCsvFile(byte[] marksCsvFile) { this.marksCsvFile = marksCsvFile; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Double getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Double maxMarks) { this.maxMarks = maxMarks; }

    public Double getAssignmentWeight() { return assignmentWeight; }
    public void setAssignmentWeight(Double assignmentWeight) { this.assignmentWeight = assignmentWeight; }

    public List<StudentMark> getStudentMarks() { return studentMarks; }
    public void setStudentMarks(List<StudentMark> studentMarks) { this.studentMarks = studentMarks; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Assignment{" +
                "assignmentId='" + assignmentId + '\'' +
                ", assignmentName='" + assignmentName + '\'' +
                ", maxMarks=" + maxMarks +
                ", assignmentWeight=" + assignmentWeight +
                '}';
    }
}