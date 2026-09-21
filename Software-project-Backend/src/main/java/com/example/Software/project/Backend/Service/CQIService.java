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

    @Autowired private CqiActionRepository cqiActionRepository;
    @Autowired private LosRepository losRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private AttainmentService attainmentService;

    // Triggers a new CQI action for every LO in the module whose batch attainment fell below
    // its stored threshold, unless one is already open (PLANNED or IN_PROGRESS) for that LO.
    public List<CqiAction> checkAndTriggerCQI(String moduleId, String batch) {
        Module module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RuntimeException("Module not found: " + moduleId));
        List<Los> losList = losRepository.findByModule_ModuleId(moduleId);

        List<CqiAction> triggered = new ArrayList<>();
        for (Los los : losList) {
            double threshold = los.getAttainmentThreshold() != null ? los.getAttainmentThreshold() : 50.0;
            Double attainment = attainmentService.calculateLoAttainmentForBatch(los.getId(), batch, threshold);
            if (attainment == null || attainment >= threshold) continue;

            boolean alreadyOpen = !cqiActionRepository
                .findByModule_ModuleIdAndLos_IdAndStatusIn(moduleId, los.getId(), OPEN_STATUSES)
                .isEmpty();
            if (alreadyOpen) continue;

            CqiAction action = new CqiAction();
            action.setModule(module);
            action.setLos(los);
            action.setBatch(batch);
            action.setAttainmentScore(attainment);
            action.setTargetScore(threshold);
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
            checkAndTriggerCQI(moduleId, newBatch);
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
        moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RuntimeException("Module not found: " + moduleId));
        List<Los> losList = losRepository.findByModule_ModuleId(moduleId);

        for (Los los : losList) {
            double threshold = los.getAttainmentThreshold() != null ? los.getAttainmentThreshold() : 50.0;
            Double attainment = attainmentService.calculateLoAttainmentForBatch(los.getId(), batch, threshold);
            if (attainment == null) continue;
            linkNextSemesterResult(moduleId, los.getId(), batch, attainment);
        }

        List<CqiAction> triggered = checkAndTriggerCQI(moduleId, batch);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("triggered", triggered);
        result.put("triggeredCount", triggered.size());
        return result;
    }
}
