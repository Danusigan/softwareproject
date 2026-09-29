package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

// The table is "studentmark", one lowercase word - named explicitly because this project's
// physical naming strategy (PhysicalNamingStrategyStandardImpl, see application.properties) is a
// pass-through, so without this Hibernate derives the table name from the class and looks for
// "StudentMark". That matches on a case-insensitive MySQL (lower_case_table_names=1, the Windows
// default) but not on a case-sensitive one (Linux default), where ddl-auto=validate fails with
// "missing table [StudentMark]".
@Entity
@Table(name = "studentmark")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "mark_type")
    private MarkType markType; // FINAL_EXAM or ASSIGNMENT

    @Column(name = "assignment_label")
    private String assignmentLabel; // e.g. "Assignment 1", "Assignment 2"

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    public void softDelete(String deletedByUsername) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedByUsername;
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

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

    public MarkType getMarkType() { return markType; }
    public void setMarkType(MarkType markType) { this.markType = markType; }

    public String getAssignmentLabel() { return assignmentLabel; }
    public void setAssignmentLabel(String assignmentLabel) { this.assignmentLabel = assignmentLabel; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    // Helper to get student index for backward compatibility/display
    public String getStudentIndex() {
        return student != null ? student.getStudentId() : null;
    }
}
