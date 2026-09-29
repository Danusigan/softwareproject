package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "assessment_item")
public class AssessmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "Q1", "Q2"
    @Column(name = "question_label", nullable = false)
    private String questionLabel;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Column(name = "max_marks", nullable = false)
    private Double maxMarks;

    // Each question maps to exactly one LO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "los_id", nullable = false)
    private Los los;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @JsonIgnore
    private AssessmentTemplate assessmentTemplate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestionLabel() { return questionLabel; }
    public void setQuestionLabel(String questionLabel) { this.questionLabel = questionLabel; }

    public Integer getQuestionNumber() { return questionNumber; }
    public void setQuestionNumber(Integer questionNumber) { this.questionNumber = questionNumber; }

    public Double getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Double maxMarks) { this.maxMarks = maxMarks; }

    public Los getLos() { return los; }
    public void setLos(Los los) { this.los = los; }

    public AssessmentTemplate getAssessmentTemplate() { return assessmentTemplate; }
    public void setAssessmentTemplate(AssessmentTemplate t) { this.assessmentTemplate = t; }

    public String getLosId()   { return los != null ? los.getId() : null; }
    public String getLosName() { return los != null ? los.getName() : null; }
    public String getTemplateId() { return assessmentTemplate != null ? assessmentTemplate.getId() : null; }
}
