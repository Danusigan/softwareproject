package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.CqiAction;
import com.example.Software.project.Backend.Model.CqiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CqiActionRepository extends JpaRepository<CqiAction, Long> {
    List<CqiAction> findByModule_ModuleIdOrderByCreatedAtDesc(String moduleId);
    List<CqiAction> findByModule_ModuleIdAndBatch(String moduleId, String batch);
    List<CqiAction> findByModule_ModuleIdAndStatus(String moduleId, CqiStatus status);
    List<CqiAction> findByLos_Id(String losId);
    List<CqiAction> findByProgramOutcome_PoId(String poId);

    List<CqiAction> findByStatus(CqiStatus status);
    List<CqiAction> findByStatusAndSubmittedTrue(CqiStatus status);
    List<CqiAction> findByModule_ModuleIdAndLos_Id(String moduleId, String losId);
    List<CqiAction> findByModule_ModuleIdAndLos_IdAndStatusIn(String moduleId, String losId, List<CqiStatus> statuses);
    List<CqiAction> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}
