package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * The saved result of one PO-attainment calculation for one student, in one module, batch and
 * mark type: how many of that PO's possible credits (from this module's mapped LOs) the student
 * earned. Written by {@code POAttainmentService.calculateStudentPOCredits} every time a lecturer
 * runs the calculation for a module — see V3__student_po_credit.sql for why this row exists and
 * how it feeds the cross-module summary.
 */
@Entity
@Table(name = "student_po_credit",
       uniqueConstraints = @UniqueConstraint(name = "uk_student_po_credit",
               columnNames = {"student_id", "po_id", "module_id", "batch", "mark_type"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentPoCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Narrower than the varchar(255) primary keys these reference (students.student_id,
    // program_outcomes.po_id, modules.module_id) - see V3__student_po_credit.sql for why:
    // at 255 each, the composite unique key below exceeds InnoDB's 3072-byte index limit.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, columnDefinition = "varchar(100)")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false, columnDefinition = "varchar(50)")
    private ProgramOutcome programOutcome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false, columnDefinition = "varchar(50)")
    private Module module;

    @Column(name = "batch", nullable = false, length = 20)
    private String batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "mark_type", nullable = false)
    private MarkType markType;

    @Column(name = "credits_earned", nullable = false)
    private Integer creditsEarned;

    @Column(name = "max_credits", nullable = false)
    private Integer maxCredits;

    @Column(name = "threshold", nullable = false)
    private Integer threshold;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public ProgramOutcome getProgramOutcome() { return programOutcome; }
    public void setProgramOutcome(ProgramOutcome programOutcome) { this.programOutcome = programOutcome; }

    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public MarkType getMarkType() { return markType; }
    public void setMarkType(MarkType markType) { this.markType = markType; }

    public Integer getCreditsEarned() { return creditsEarned; }
    public void setCreditsEarned(Integer creditsEarned) { this.creditsEarned = creditsEarned; }

    public Integer getMaxCredits() { return maxCredits; }
    public void setMaxCredits(Integer maxCredits) { this.maxCredits = maxCredits; }

    public Integer getThreshold() { return threshold; }
    public void setThreshold(Integer threshold) { this.threshold = threshold; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
