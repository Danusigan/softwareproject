package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_marks")
public class StudentMark {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonBackReference
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    @JsonBackReference
    private Assignment assignment;
    
    @Column(name = "mark")
    private Double mark;
    
    @Column(name = "is_absent", nullable = false)
    private Boolean isAbsent = false;
    
    @Column(name = "is_medical", nullable = false)
    private Boolean isMedical = false;
    
    @Column(name = "remarks")
    private String remarks;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Ensure mark is within valid range
        if (mark != null && (mark < 0.0 || mark > 100.0)) {
            mark = Math.max(0.0, Math.min(100.0, mark));
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Ensure mark is within valid range
        if (mark != null && (mark < 0.0 || mark > 100.0)) {
            mark = Math.max(0.0, Math.min(100.0, mark));
        }
    }
    
    // Default constructor
    public StudentMark() {}
    
    // Constructor
    public StudentMark(Student student, Assignment assignment, Double mark) {
        this.student = student;
        this.assignment = assignment;
        this.mark = mark;
        this.isAbsent = false;
        this.isMedical = false;
    }
    
    // Constructor for absent/medical
    public StudentMark(Student student, Assignment assignment, boolean isAbsent, boolean isMedical) {
        this.student = student;
        this.assignment = assignment;
        this.isAbsent = isAbsent;
        this.isMedical = isMedical;
        this.mark = null;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Student getStudent() {
        return student;
    }
    
    public void setStudent(Student student) {
        this.student = student;
    }
    
    public Assignment getAssignment() {
        return assignment;
    }
    
    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }
    
    public Double getMark() {
        return mark;
    }
    
    public void setMark(Double mark) {
        // Clamp mark between 0 and 100
        if (mark != null) {
            this.mark = Math.max(0.0, Math.min(100.0, mark));
        } else {
            this.mark = mark;
        }
    }
    
    public Boolean getIsAbsent() {
        return isAbsent;
    }
    
    public void setIsAbsent(Boolean isAbsent) {
        this.isAbsent = isAbsent;
    }
    
    public Boolean getIsMedical() {
        return isMedical;
    }
    
    public void setIsMedical(Boolean isMedical) {
        this.isMedical = isMedical;
    }
    
    public String getRemarks() {
        return remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Helper methods
    public boolean isValidMark() {
        return mark != null && !isAbsent && !isMedical;
    }
    
    public double getEffectiveMark() {
        if (isValidMark()) {
            return mark;
        }
        return 0.0; // Return 0 for absent or medical cases in calculations
    }
    
    @Override
    public String toString() {
        return "StudentMark{" +
                "id=" + id +
                ", student=" + (student != null ? student.getStudentId() : "null") +
                ", assignment=" + (assignment != null ? assignment.getAssignmentId() : "null") +
                ", mark=" + mark +
                ", isAbsent=" + isAbsent +
                ", isMedical=" + isMedical +
                '}';
    }
}