package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.OutcomeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutcomeMappingRepository extends JpaRepository<OutcomeMapping, Long> {
    
    // Find by Learning Outcome ID
    List<OutcomeMapping> findByLearningOutcome_Id(String loId);
    
    // Find by Module ID
    List<OutcomeMapping> findByLearningOutcome_Module_ModuleId(String moduleId);
    
    // Find by Program Outcome ID
    List<OutcomeMapping> findByProgramOutcome_PoId(String poId);
    
    // Find by status
    List<OutcomeMapping> findByStatus(OutcomeMapping.ApprovalStatus status);
    
    // Find by lecturer
    List<OutcomeMapping> findByMappedBy(String mappedBy);
    
    // Find by reviewer
    List<OutcomeMapping> findByReviewedBy(String reviewedBy);
    
    // Find pending mappings for a specific lecturer
    List<OutcomeMapping> findByMappedByAndStatus(String mappedBy, OutcomeMapping.ApprovalStatus status);
    
    // Find mappings by LO ID and status
    List<OutcomeMapping> findByLearningOutcome_IdAndStatus(String loId, OutcomeMapping.ApprovalStatus status);
    
    // Find mappings by module and status
    List<OutcomeMapping> findByLearningOutcome_Module_ModuleIdAndStatus(String moduleId, OutcomeMapping.ApprovalStatus status);
    
    // Check if mapping exists for specific LO-PO combination
    boolean existsByLearningOutcome_IdAndProgramOutcome_PoId(String loId, String poId);
    
    // Find mapping by LO-PO combination
    OutcomeMapping findByLearningOutcome_IdAndProgramOutcome_PoId(String loId, String poId);
    
    // Custom query to get mapping statistics for dashboard
    @Query("SELECT " +
           "COUNT(*) as totalMappings, " +
           "SUM(CASE WHEN m.status = 'PENDING' THEN 1 ELSE 0 END) as pendingMappings, " +
           "SUM(CASE WHEN m.status = 'APPROVED' THEN 1 ELSE 0 END) as approvedMappings, " +
           "SUM(CASE WHEN m.status = 'REJECTED' THEN 1 ELSE 0 END) as rejectedMappings " +
           "FROM OutcomeMapping m WHERE m.learningOutcome.module.moduleId = :moduleId")
    Object[] getMappingStatsByModule(@Param("moduleId") String moduleId);
    
    // Get approved mappings for attainment calculation
    @Query("SELECT m FROM OutcomeMapping m WHERE m.status = 'APPROVED' AND m.learningOutcome.module.moduleId = :moduleId")
    List<OutcomeMapping> getApprovedMappingsForModule(@Param("moduleId") String moduleId);
    
    // Get PO coverage for a module
    @Query("SELECT DISTINCT m.programOutcome.poId FROM OutcomeMapping m WHERE m.status = 'APPROVED' AND m.learningOutcome.module.moduleId = :moduleId")
    List<String> getCoveredPOsForModule(@Param("moduleId") String moduleId);
    
    // Get weight distribution for a PO across all modules
    @Query("SELECT m.learningOutcome.module.moduleId, AVG(m.weight) FROM OutcomeMapping m WHERE m.status = 'APPROVED' AND m.programOutcome.poId = :poId GROUP BY m.learningOutcome.module.moduleId")
    List<Object[]> getWeightDistributionForPO(@Param("poId") String poId);
    
    // Find mappings that need review (pending for more than X days)
    @Query("SELECT m FROM OutcomeMapping m WHERE m.status = 'PENDING' AND m.mappedAt < :cutoffDate ORDER BY m.mappedAt ASC")
    List<OutcomeMapping> findOldPendingMappings(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
    
    // Delete all mappings for a specific LO (cascade delete)
    void deleteByLearningOutcome_Id(String loId);
    
    // Count mappings by status for dashboard
    @Query("SELECT m.status, COUNT(*) FROM OutcomeMapping m GROUP BY m.status")
    List<Object[]> countMappingsByStatus();
    
    // Count mappings by specific status
    long countByStatus(OutcomeMapping.ApprovalStatus status);
}