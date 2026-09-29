package com.example.Software.project.Backend.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "student_assessment_score", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "assessment_item_id"}))
public class StudentAssessmentScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_item_id")
    private AssessmentItem assessmentItem;

    @Column(name = "score")
    private Double score;

    @PrePersist
    @PreUpdate
    private void validateMark() {
        if (score == null) return;
        Double maximum = assessmentItem == null ? null : assessmentItem.getMaxMarks();
        if (!Double.isFinite(score) || score < 0 || maximum == null || !Double.isFinite(maximum)
                || maximum <= 0 || score > maximum)
            throw new IllegalArgumentException("Obtained mark must be between zero and the assessment item's positive maximum");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public AssessmentItem getAssessmentItem() { return assessmentItem; }
    public void setAssessmentItem(AssessmentItem assessmentItem) { this.assessmentItem = assessmentItem; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getStudentId() { return student != null ? student.getStudentId() : null; }
    public Long   getItemId()    { return assessmentItem != null ? assessmentItem.getId() : null; }
}
