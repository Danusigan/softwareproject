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
    @Autowired private NotificationService notificationService;   // NEW

    public List<CqiAction> checkAndTriggerCQI(String moduleId, String batch) {
        return checkAndTriggerCQI(moduleId, batch, null, null);
    }

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
            CqiAction saved = cqiActionRepository.save(action);                 // CHANGED
            triggered.add(saved);                                               // CHANGED
            notifyCqiTriggered(module, saved, "LO " + los.getId());             // NEW
        }
        return triggered;
    }

    // NEW: puts an "action triggered" message on the lecturers' profile pages
    private void notifyCqiTriggered(Module module, CqiAction action, String target) {
        try {
            java.util.Set<String> who = new java.util.LinkedHashSet<>(module.getAssignedLecturerUsernames());
            if (action.getCreatedBy() != null) who.add(action.getCreatedBy());
            notificationService.notifyUsers(who, "CQI action triggered for " + target
                + " in module " + module.getModuleId() + " (batch " + action.getBatch() + ")");
        } catch (Exception ignored) { }
    }

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
            CqiAction saved = cqiActionRepository.save(action);                 // CHANGED
            triggered.add(saved);                                               // CHANGED
            notifyCqiTriggered(module, saved, "PO " + po.getPoId());            // NEW
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

    public Map<String, Object> finalizeModuleAttainment(String moduleId, String batch) {
        return finalizeModuleAttainment(moduleId, batch, null, null);
    }

    public Map<String, Object> finalizeModuleAttainment(String moduleId, String batch,
                                                        Double studentPassThreshold, Double batchTarget) {
        moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RuntimeException("Module not found: " + moduleId));
        List<Los> losList = losRepository.findByModule_ModuleIdAndIsDeletedFalse(moduleId);

        for (Los los : losList) {
            double loThreshold = los.getAttainmentThreshold() != null ? los.getAttainmentThreshold() : 50.0;
            double passThreshold = studentPassThreshold != null ? studentPassThreshold : loThreshold;
            Double attainment = attainmentService.calculateLoAttainmentForBatch(los.getId(), batch, passThreshold);
            if (attainment == null) continue;
            linkNextSemesterResult(moduleId, los.getId(), batch, attainment, studentPassThreshold, batchTarget);
        }

        List<CqiAction> triggered = checkAndTriggerCQI(moduleId, batch, studentPassThreshold, batchTarget);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("triggered", triggered);
        result.put("triggeredCount", triggered.size());
        return result;
    }

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

        ProgramOutcome po = programOutcomeRepository.findById(poId)
            .or(() -> programOutcomeRepository.findByCode(poId))
            .orElseThrow(() -> new RuntimeException("Program outcome not found: " + poId));
        plan.setProgramOutcome(po);

        return cqiActionRepository.save(plan);
    }

    public List<CqiAction> getCqiPlansForBatch(String batch) {
        return cqiActionRepository.findByBatchOrderByCreatedAtDesc(batch);
    }

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
