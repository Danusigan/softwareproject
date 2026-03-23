package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
public class StudentMark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationship to Student
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonBackReference
    private Student student;

    private Double score; // Clamped 0.0 - 100.0

    @ManyToOne
    @JoinColumn(name = "los_id", nullable = false)
    private Los los;

    @Column(name = "batch")
    private String batch; // e.g., "22", "23" - batch identifier for grouping

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Los getLos() { return los; }
    public void setLos(Los los) { this.los = los; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    // Helper to get student index for backward compatibility/display
    public String getStudentIndex() {
        return student != null ? student.getStudentId() : null;
    }
}
