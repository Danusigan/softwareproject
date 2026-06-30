package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cqi_action")
public class CqiAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    @JsonIgnore
    private Module module;

    @Column(name = "module_id", insertable = false, updatable = false)
    private String moduleId;

    // Nullable — action may target an LO, a PO, or both
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "los_id")
    private Los los;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id")
    private ProgramOutcome programOutcome;

    @Column(name = "batch", nullable = false)
    private String batch;

    @Column(name = "semester")
    private String semester;

    @Column(name = "academic_year")
    private String academicYear;

    // Lecturer-filled fields, populated via /api/cqi/{id}/submit — null until then
    @Column(name = "reason", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "action_description", columnDefinition = "TEXT")
    private String actionPlan;

    @Column(name = "responsible_staff")
    private String responsibleStaff;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private CqiActionType actionType;

    // % attainment that triggered this action, and the threshold it missed
    @Column(name = "attainment_score")
    private Double attainmentScore;

    @Column(name = "target_score")
    private Double targetScore;

    // What the lecturer commits to achieving next semester
    @Column(name = "target_attainment")
    private Double targetAttainment;

    @Column(name = "deadline")
    private LocalDate deadline;

    // True once the lecturer has filled in the plan and it's awaiting admin review
    @Column(name = "submitted", nullable = false)
    private boolean submitted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CqiStatus status = CqiStatus.PLANNED;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    // Filled automatically once next semester's attainment is calculated for this LO
    @Column(name = "next_sem_attainment")
    private Double nextSemAttainment;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }

    public String getModuleId() { return moduleId; }

    public Los getLos() { return los; }
    public void setLos(Los los) { this.los = los; }

    public ProgramOutcome getProgramOutcome() { return programOutcome; }
    public void setProgramOutcome(ProgramOutcome po) { this.programOutcome = po; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public String getActionPlan() { return actionPlan; }
    public void setActionPlan(String actionPlan) { this.actionPlan = actionPlan; }

    public String getResponsibleStaff() { return responsibleStaff; }
    public void setResponsibleStaff(String s) { this.responsibleStaff = s; }

    public CqiActionType getActionType() { return actionType; }
    public void setActionType(CqiActionType actionType) { this.actionType = actionType; }

    public Double getAttainmentScore() { return attainmentScore; }
    public void setAttainmentScore(Double attainmentScore) { this.attainmentScore = attainmentScore; }

    public Double getTargetScore() { return targetScore; }
    public void setTargetScore(Double targetScore) { this.targetScore = targetScore; }

    public Double getTargetAttainment() { return targetAttainment; }
    public void setTargetAttainment(Double targetAttainment) { this.targetAttainment = targetAttainment; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public boolean isSubmitted() { return submitted; }
    public void setSubmitted(boolean submitted) { this.submitted = submitted; }

    public CqiStatus getStatus() { return status; }
    public void setStatus(CqiStatus status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }

    public Double getNextSemAttainment() { return nextSemAttainment; }
    public void setNextSemAttainment(Double nextSemAttainment) { this.nextSemAttainment = nextSemAttainment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Convenience for JSON serialisation
    public String getLosId()     { return los != null ? los.getId() : null; }
    public String getLosName()   { return los != null ? los.getName() : null; }
    public String getPoId()      { return programOutcome != null ? programOutcome.getPoId() : null; }
    public String getPoCode()    { return programOutcome != null ? programOutcome.getCode() : null; }
    public String getModuleName() { return module != null ? module.getModuleName() : null; }
}
