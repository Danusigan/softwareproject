package com.example.Software.project.Backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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

    @Column(name = "action_description", columnDefinition = "TEXT", nullable = false)
    private String actionDescription;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "responsible_staff")
    private String responsibleStaff;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CqiStatus status = CqiStatus.PLANNED;

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

    public String getActionDescription() { return actionDescription; }
    public void setActionDescription(String d) { this.actionDescription = d; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getResponsibleStaff() { return responsibleStaff; }
    public void setResponsibleStaff(String s) { this.responsibleStaff = s; }

    public CqiStatus getStatus() { return status; }
    public void setStatus(CqiStatus status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Convenience for JSON serialisation
    public String getLosId()   { return los != null ? los.getId() : null; }
    public String getLosName() { return los != null ? los.getName() : null; }
    public String getPoId()    { return programOutcome != null ? programOutcome.getPoId() : null; }
    public String getPoCode()  { return programOutcome != null ? programOutcome.getCode() : null; }
}
