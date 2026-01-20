package com.example.Software.project.Backend.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @Column(name = "assignment_id", unique = true, nullable = false)
    private String assignmentId; // Changed to String for custom ID

    @Column(name = "assignment_name")
    private String assignmentName;

    // Storing the CSV file content inside the DB
    // @Lob indicates a Large Object (BLOB)
    @Lob
    @Column(name = "marks_csv_file", columnDefinition = "LONGBLOB")
    private byte[] marksCsvFile;

    // Optional: Store filename if needed
    private String fileName;

    // --- Getters and Setters ---

    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }

    public String getAssignmentName() { return assignmentName; }
    public void setAssignmentName(String assignmentName) { this.assignmentName = assignmentName; }

    public byte[] getMarksCsvFile() { return marksCsvFile; }
    public void setMarksCsvFile(byte[] marksCsvFile) { this.marksCsvFile = marksCsvFile; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}