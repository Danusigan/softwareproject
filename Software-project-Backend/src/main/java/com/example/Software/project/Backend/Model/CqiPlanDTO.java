package com.example.Software.project.Backend.Model;

import java.time.LocalDate;

public class CqiPlanDTO {

    private String rootCause;
    private String actionPlan;
    private String actionType;
    private Double targetAttainment;
    private LocalDate deadline;

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public String getActionPlan() { return actionPlan; }
    public void setActionPlan(String actionPlan) { this.actionPlan = actionPlan; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Double getTargetAttainment() { return targetAttainment; }
    public void setTargetAttainment(Double targetAttainment) { this.targetAttainment = targetAttainment; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
}
