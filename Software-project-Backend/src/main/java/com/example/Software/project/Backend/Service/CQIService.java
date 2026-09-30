package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CQIService {

    private static final List<CqiStatus> OPEN_STATUSES = List.of(CqiStatus.PLANNED, CqiStatus.IN_PROGRESS);
    private static final double DEFAULT_PO_TARGET_PERCENT = 70.0;

    @Autowired private CqiActionRepository cqiActionRepository;
    @Autowired private LosRepository losRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private ProgramOutcomeRepository programOutcomeRepository;
    @Autowired private AttainmentService attainmentService;
    @Autowired private StudentPoCreditRepository studentPoCreditRepository;

    // Triggers a new CQI action for every LO in the module whose batch attainment fell below
    // its stored threshold, unless one is already open (PLANNED or IN_PROGRESS) for that LO.
    public List<CqiAction> checkAndTriggerCQI(String moduleId, String batch) {
        return checkAndTriggerCQI(moduleId, batch, null, null);
    }

    // Two separate values decide a CQI: studentPassThreshold is the % of an LO's marks a student
    // must reach to pass it, and batchTarget is the % of the batch that must pass. Either may be
    // null, in which case the LO's own stored threshold (default 50) stands in for it.
    public List<CqiAction> checkAndTriggerCQI(String moduleId, String batch,
                                              Double studentPassThreshold, Double batchTarget) {
        Module module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RuntimeException("Module not found: " + moduleId));
        List<Los> losList = losRepository.findByModule_ModuleIdAndIsDeletedFalse(moduleId);

        List<CqiAction> triggered = new ArrayList<>();
        for (Los los : losList) {
            double loThreshold = los.getAttainmentThreshold() != null ? los.getAttainmentThreshold() : 50.0;
            double passThreshold = studentPassThreshold != null ? studentPassThreshold : loThreshold;
            double target = batchTarget != null ? batchTarget : loThreshold;
            Double attainment = attainmentService.calculateLoAttainmentForBatch(los.getId(), batch, passThreshold);
            if (attainment == null || attainment >= target) continue;

            boolean alreadyOpen = !cqiActionRepository
                .findByModule_ModuleIdAndLos_IdAndStatusIn(moduleId, los.getId(), OPEN_STATUSES)
                .isEmpty();
            if (alreadyOpen) continue;

            CqiAction action = new CqiAction();
            action.setModule(module);
            action.setLos(los);
            action.setBatch(batch);
            action.setAttainmentScore(attainment);
            action.setTargetScore(target);
            action.setStatus(CqiStatus.PLANNED);
            action.setSubmitted(false);
            List<String> lecturers = module.getAssignedLecturerUsernames();
            if (lecturers != null && !lecturers.isEmpty()) {
                action.setCreatedBy(lecturers.get(0));
            }
            triggered.add(cqiActionRepository.save(action));
        }
        return triggered;
    }

    // PO-level counterpart to checkAndTriggerCQI: for every Program Outcome that has saved
    // StudentPoCredit rows for this module+batch (i.e. a lecturer has already run PO attainment
    // calculation - see POAttainmentService.calculateAndPersistPOAttainment), computes the % of
    // students who met their credit threshold and triggers a PO-level CqiAction (los=null,
    // programOutcome set) if that percentage falls below target and no cycle is already open for
    // that PO in this module. If no StudentPoCredit rows exist yet for this module+batch, this is
    // a no-op (returns an empty list) rather than an error.
    //
    // Target is an explicit poTargetPercent argument if given, otherwise DEFAULT_PO_TARGET_PERCENT.
    // There is deliberately no per-PO stored/admin-configurable threshold here (that was tried and
    // rolled back - no UI surface needed it).
    public List<CqiAction> checkAndTriggerCQI_PO(String moduleId, String batch) {
        return checkAndTriggerCQI_PO(moduleId, batch, null);
    }

    public List<CqiAction> checkAndTriggerCQI_PO(String moduleId, String batch, Double poTargetPercent) {
        Module module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RuntimeException("Module not found: " + moduleId));
        double target = poTargetPercent != null ? poTargetPercent : DEFAULT_PO_TARGET_PERCENT;

        List<StudentPoCredit> credits = studentPoCreditRepository.findByModule_ModuleIdAndBatch(moduleId, batch);
        Map<String, List<StudentPoCredit>> byPo = new LinkedHashMap<>();
        for (StudentPoCredit credit : credits) {
            if (credit.getProgramOutcome() == null) continue;
            byPo.computeIfAbsent(credit.getProgramOutcome().getPoId(), k -> new ArrayList<>()).add(credit);
        }

        List<CqiAction> triggered = new ArrayList<>();
        for (Map.Entry<String, List<StudentPoCredit>> entry : byPo.entrySet()) {
            List<StudentPoCredit> rows = entry.getValue();
            ProgramOutcome po = rows.get(0).getProgramOutcome();
            long achieved = rows.stream()
                .filter(r -> r.getCreditsEarned() != null && r.getThreshold() != null && r.getCreditsEarned() >= r.getThreshold())
                .count();
            double percent = rows.isEmpty() ? 0 : (achieved * 100.0) / rows.size();
            if (percent >= target) continue;

            boolean alreadyOpen = !cqiActionRepository
                .findByModule_ModuleIdAndProgramOutcome_PoIdAndStatusIn(moduleId, entry.getKey(), OPEN_STATUSES)
                .isEmpty();
            if (alreadyOpen) continue;

            CqiAction action = new CqiAction();
            action.setModule(module);
            action.setProgramOutcome(po);
            action.setBatch(batch);
            action.setAttainmentScore(percent);
            action.setTargetScore(target);
            action.setStatus(CqiStatus.PLANNED);
            action.setSubmitted(false);
            List<String> lecturers = module.getAssignedLecturerUsernames();
            if (lecturers != null && !lecturers.isEmpty()) {
                action.setCreatedBy(lecturers.get(0));
            }
            triggered.add(cqiActionRepository.save(action));
        }
        return triggered;
    }

    public CqiAction submitPlan(Long cqiActionId, String lecturerUsername, CqiPlanDTO dto) {
        CqiAction action = cqiActionRepository.findById(cqiActionId)
            .orElseThrow(() -> new RuntimeException("CQI action not found: " + cqiActionId));

        List<String> moduleLecturers = action.getModule() != null ? action.getModule().getAssignedLecturerUsernames() : null;
        boolean owns = lecturerUsername != null && (
            lecturerUsername.equals(action.getCreatedBy())
            || (moduleLecturers != null && moduleLecturers.contains(lecturerUsername))
        );
        if (!owns) {
            throw new RuntimeException("You do not have access to this CQI action");
        }

        action.setRootCause(dto.getRootCause());
        action.setActionPlan(dto.getActionPlan());
        if (dto.getActionType() != null && !dto.getActionType().isBlank()) {
            action.setActionType(CqiActionType.valueOf(dto.getActionType().trim().toUpperCase()));
        }
        action.setTargetAttainment(dto.getTargetAttainment());
        action.setDeadline(dto.getDeadline());
        action.setSubmitted(true);
        action.setAdminComment(null);
        return cqiActionRepository.save(action);
    }

    public List<CqiAction> getPendingForAdmin() {
        return cqiActionRepository.findByStatusAndSubmittedTrue(CqiStatus.PLANNED);
    }

    public List<CqiAction> getLoReviewHistory() {
        return cqiActionRepository.findByLosIsNotNullAndStatusInOrderByCreatedAtDesc(
            List.of(CqiStatus.IN_PROGRESS, CqiStatus.COMPLETED));
    }

    public CqiAction approvePlan(Long cqiActionId, String adminUsername) {
        CqiAction action = cqiActionRepository.findById(cqiActionId)
            .orElseThrow(() -> new RuntimeException("CQI action not found: " + cqiActionId));
        action.setStatus(CqiStatus.IN_PROGRESS);
        action.setApprovedBy(adminUsername);
        return cqiActionRepository.save(action);
    }

    public CqiAction returnPlan(Long cqiActionId, String adminUsername, String comment) {
        CqiAction action = cqiActionRepository.findById(cqiActionId)
            .orElseThrow(() -> new RuntimeException("CQI action not found: " + cqiActionId));
        action.setAdminComment(comment);
        action.setSubmitted(false);
        action.setStatus(CqiStatus.PLANNED);
        return cqiActionRepository.save(action);
    }

    // Links a newly calculated semester's attainment back to an open (IN_PROGRESS) CQI action for
    // the same module+LO. Closes the loop if the target was met; otherwise leaves it open and lets
    // checkAndTriggerCQI re-evaluate (it no-ops here since this LO's action is still open).
    public void linkNextSemesterResult(String moduleId, String losId, String newBatch, Double newAttainment) {
        linkNextSemesterResult(moduleId, losId, newBatch, newAttainment, null, null);
    }

    public void linkNextSemesterResult(String moduleId, String losId, String newBatch, Double newAttainment,
                                       Double studentPassThreshold, Double batchTarget) {
        List<CqiAction> open = cqiActionRepository
            .findByModule_ModuleIdAndLos_IdAndStatusIn(moduleId, losId, List.of(CqiStatus.IN_PROGRESS));
        if (open.isEmpty()) return;

        CqiAction action = open.get(0);
        action.setNextSemAttainment(newAttainment);
        if (newAttainment != null && action.getTargetScore() != null && newAttainment >= action.getTargetScore()) {
            action.setStatus(CqiStatus.COMPLETED);
        }
        cqiActionRepository.save(action);

        if (action.getStatus() != CqiStatus.COMPLETED) {
            checkAndTriggerCQI(moduleId, newBatch, studentPassThreshold, batchTarget);
        }
    }

    public List<CqiAction> getCqiHistoryForModule(String moduleId) {
        return cqiActionRepository.findByModule_ModuleIdOrderByCreatedAtDesc(moduleId);
    }

    public List<CqiAction> getMyPlans(String lecturerUsername) {
        return cqiActionRepository.findByCreatedByOrderByCreatedAtDesc(lecturerUsername);
    }

    // Entry point for POST /api/cqi/finalize/{moduleId}: for every LO in the module, links this
    // batch's result to any open CQI cycle, then checks whether a new cycle should be triggered.
    public Map<String, Object> finalizeModuleAttainment(String moduleId, String batch) {
        return finalizeModuleAttainment(moduleId, batch, null, null);
    }

    public Map<String, Object> finalizeModuleAttainment(String moduleId, String batch,
                                                        Double studentPassThreshold, Double batchTarget) {
        moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RuntimeException("Module not found: " + moduleId));
        List<Los> losList = losRepository.findByModule_ModuleIdAndIsDeletedFalse(moduleId);

        List<CqiAction> removed = new ArrayList<>();
        for (Los los : losList) {
            double loThreshold = los.getAttainmentThreshold() != null ? los.getAttainmentThreshold() : 50.0;
            double passThreshold = studentPassThreshold != null ? studentPassThreshold : loThreshold;
            double target = batchTarget != null ? batchTarget : loThreshold;
            Double attainment = attainmentService.calculateLoAttainmentForBatch(los.getId(), batch, passThreshold);
            if (attainment == null) continue;
            removed.addAll(removeStaleUntouchedCqi(moduleId, los.getId(), batch, attainment, target));
            linkNextSemesterResult(moduleId, los.getId(), batch, attainment, studentPassThreshold, batchTarget);
        }

        List<CqiAction> triggered = checkAndTriggerCQI(moduleId, batch, studentPassThreshold, batchTarget);

        // Deliberately NOT calling checkAndTriggerCQI_PO here: several existing consumers of
        // "this module's CQI actions" (getCqiHistoryForModule's most-recent-first pick,
        // POAttainmentService.getStudentPOSummary's per-LO cqiActions breakdown) assume every row
        // for a module is LO-scoped. Mixing in PO-level rows here breaks those assumptions non-
        // obviously - see MarksToPoPipelineTest. PO-level triggering is intentionally opt-in via
        // POST /api/cqi/trigger-po/{moduleId}, kept out of this LO-focused pipeline until those
        // consumers are updated to distinguish LO vs PO actions.
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("triggered", triggered);
        result.put("triggeredCount", triggered.size());
        result.put("removedCount", removed.size());
        return result;
    }

    // When Finalize is clicked and an LO now meets its target, deletes any CQI for that LO+batch that
    // was auto-triggered earlier but never touched by anyone (e.g. triggered on partial marks, then the
    // remaining marks lifted the LO). Submitted or approved cycles, and plans an admin returned with a
    // comment or that hold a lecturer's draft, are never deleted.
    private List<CqiAction> removeStaleUntouchedCqi(String moduleId, String losId, String batch,
                                                    double attainment, double target) {
        if (attainment < target) return List.of();
        List<CqiAction> stale = cqiActionRepository
            .findByModule_ModuleIdAndLos_IdAndBatchAndStatusAndSubmittedFalse(moduleId, losId, batch, CqiStatus.PLANNED)
            .stream()
            .filter(a -> isBlank(a.getRootCause()) && isBlank(a.getActionPlan()) && isBlank(a.getAdminComment()))
            .toList();
        cqiActionRepository.deleteAll(stale);
        return stale;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // ADMIN: Create a PO-level CQI plan directly from batch report (no approval workflow)
    public CqiAction createPoCqiPlan(String poId, String batch, String moduleId, Double currentAttainment,
                                      Double targetAttainment, String plannedActions, String adminUsername) {
        CqiAction plan = new CqiAction();
        plan.setBatch(batch);
        plan.setStatus(CqiStatus.IN_PROGRESS);
        plan.setSubmitted(true);
        plan.setCreatedBy(adminUsername);
        plan.setApprovedBy(adminUsername);
        plan.setAttainmentScore(currentAttainment);
        plan.setTargetAttainment(targetAttainment);
        plan.setActionPlan(plannedActions);

        if (moduleId != null && !moduleId.isBlank()) {
            Module module = moduleRepository.findById(moduleId).orElse(null);
            if (module != null) {
                plan.setModule(module);
            }
        }

        // PO report rows identify a PO by its code, not its primary key, so accept either
        ProgramOutcome po = programOutcomeRepository.findById(poId)
            .or(() -> programOutcomeRepository.findByCode(poId))
            .orElseThrow(() -> new RuntimeException("Program outcome not found: " + poId));
        plan.setProgramOutcome(po);

        return cqiActionRepository.save(plan);
    }

    // ADMIN: Get all CQI plans for a specific batch
    public List<CqiAction> getCqiPlansForBatch(String batch) {
        return cqiActionRepository.findByBatchOrderByCreatedAtDesc(batch);
    }

    // ADMIN: Update CQI plan status (Created -> In Progress -> Completed -> Closed)
    public CqiAction updateCqiPlanStatus(Long planId, String newStatus) {
        CqiAction plan = cqiActionRepository.findById(planId)
            .orElseThrow(() -> new RuntimeException("CQI plan not found: " + planId));

        try {
            CqiStatus status = CqiStatus.valueOf(newStatus.toUpperCase());
            plan.setStatus(status);
            return cqiActionRepository.save(plan);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + newStatus);
        }
    }

    // ADMIN: Update CQI plan details
    public CqiAction updateCqiPlanDetails(Long planId, String plannedActions, Double targetAttainment) {
        CqiAction plan = cqiActionRepository.findById(planId)
            .orElseThrow(() -> new RuntimeException("CQI plan not found: " + planId));

        if (plannedActions != null && !plannedActions.isBlank()) {
            plan.setActionPlan(plannedActions);
        }
        if (targetAttainment != null) {
            plan.setTargetAttainment(targetAttainment);
        }
        return cqiActionRepository.save(plan);
    }
}
