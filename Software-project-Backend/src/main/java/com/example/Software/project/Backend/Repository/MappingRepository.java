package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.Mapping;
import com.example.Software.project.Backend.Model.LosPos;
import com.example.Software.project.Backend.Model.ProgramOutcome;
import com.example.Software.project.Backend.Model.Mapping.MappingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MappingRepository extends JpaRepository<Mapping, Long> {
    
    // Find mappings by LosPos
    List<Mapping> findByLosPos(LosPos losPos);
    
    // Find mappings by LosPos ID
    List<Mapping> findByLosPosId(String losPosId);
    
    // Find mappings by Program Outcome
    List<Mapping> findByProgramOutcome(ProgramOutcome programOutcome);
    
    // Find mappings by Program Outcome ID
    List<Mapping> findByProgramOutcomeId(Long programOutcomeId);
    
    // Find mappings by LosPos ID and Program Outcome ID  
    List<Mapping> findByLosPosIdAndProgramOutcomeId(String losPosId, Long programOutcomeId);
    
    // Find specific mapping by LosPos and ProgramOutcome
    Optional<Mapping> findByLosPosAndProgramOutcome(LosPos losPos, ProgramOutcome programOutcome);
    
    // Find mappings by status
    List<Mapping> findByStatus(MappingStatus status);
    
    // Find pending mappings
    List<Mapping> findByStatusOrderByMappedAtAsc(MappingStatus status);
    
    // Find approved mappings
    List<Mapping> findByStatusAndLosPosId(MappingStatus status, String losPosId);
    
    // Find mappings by lecturer (creator)
    List<Mapping> findByMappedBy(String mappedBy);
    
    // Find mappings by reviewer (admin)
    List<Mapping> findByReviewedBy(String reviewedBy);
    
    // Check if mapping exists for specific LO-PO combination
    boolean existsByLosPosAndProgramOutcome(LosPos losPos, ProgramOutcome programOutcome);
    
    // Find mappings by weight
    List<Mapping> findByWeight(Integer weight);
    
    // Find mappings above certain weight threshold
    @Query("SELECT m FROM Mapping m WHERE m.weight >= :minWeight AND m.status = :status")
    List<Mapping> findByWeightGreaterThanEqualAndStatus(@Param("minWeight") Integer minWeight, @Param("status") MappingStatus status);
    
    // Find all approved mappings for a module
    @Query("SELECT m FROM Mapping m WHERE m.losPos.moduleCode = :moduleCode AND m.status = 'APPROVED'")
    List<Mapping> findApprovedMappingsByModuleCode(@Param("moduleCode") String moduleCode);
    
    // Find all pending mappings for admin review
    @Query("SELECT m FROM Mapping m WHERE m.status = 'PENDING' ORDER BY m.mappedAt ASC")
    List<Mapping> findPendingMappingsForReview();
    
    // Count mappings by status
    long countByStatus(MappingStatus status);
    
    // Count mappings for a specific lecturer
    long countByMappedBy(String mappedBy);
    
    // Count approved mappings for a module
    @Query("SELECT COUNT(m) FROM Mapping m WHERE m.losPos.moduleCode = :moduleCode AND m.status = 'APPROVED'")
    long countApprovedMappingsByModuleCode(@Param("moduleCode") String moduleCode);
    
    // Find recent mappings (within specified days)
    @Query("SELECT m FROM Mapping m WHERE m.mappedAt >= :since ORDER BY m.mappedAt DESC")
    List<Mapping> findRecentMappings(@Param("since") LocalDateTime since);
    
    // Find mappings for OBE calculation (approved mappings for active LOs)
    @Query("SELECT m FROM Mapping m WHERE m.status = 'APPROVED' AND m.losPos.id = :losPosId")
    List<Mapping> findApprovedMappingsByLosPosId(@Param("losPosId") String losPosId);
    
    // Find all approved mappings with weights for attainment calculation
    @Query("SELECT m FROM Mapping m WHERE m.status = 'APPROVED' AND m.weight > 0")
    List<Mapping> findApprovedMappingsWithWeights();
    
    // Find mappings by module and program outcome
    @Query("SELECT m FROM Mapping m WHERE m.losPos.moduleCode = :moduleCode AND m.programOutcome.poCode = :poCode AND m.status = :status")
    List<Mapping> findByModuleCodeAndPoCodeAndStatus(@Param("moduleCode") String moduleCode, @Param("poCode") String poCode, @Param("status") MappingStatus status);
    
    // Find high correlation mappings (weight = 3) for a module
    @Query("SELECT m FROM Mapping m WHERE m.losPos.moduleCode = :moduleCode AND m.weight = 3 AND m.status = 'APPROVED'")
    List<Mapping> findHighCorrelationMappingsByModuleCode(@Param("moduleCode") String moduleCode);
    
    // Find mappings requiring review (pending for more than specified days)
    @Query("SELECT m FROM Mapping m WHERE m.status = 'PENDING' AND m.mappedAt <= :cutoffDate")
    List<Mapping> findMappingsRequiringUrgentReview(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Find duplicate mappings (same LO-PO combination with different weights)
    @Query("SELECT m1 FROM Mapping m1, Mapping m2 WHERE m1.id < m2.id AND m1.losPos = m2.losPos AND m1.programOutcome = m2.programOutcome")
    List<Mapping> findDuplicateMappings();
    
    // Get mapping statistics by lecturer
    @Query("SELECT NEW map(m.mappedBy as lecturer, COUNT(m) as total, SUM(CASE WHEN m.status = 'APPROVED' THEN 1 ELSE 0 END) as approved) FROM Mapping m GROUP BY m.mappedBy")
    List<Object[]> getMappingStatisticsByLecturer();
}