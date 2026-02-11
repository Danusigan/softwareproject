package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.ProgramOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramOutcomeRepository extends JpaRepository<ProgramOutcome, Long> {
    
    // Find by PO code (unique identifier)
    Optional<ProgramOutcome> findByPoCode(String poCode);
    
    // Check if PO exists by code
    boolean existsByPoCode(String poCode);
    
    // Find all active program outcomes
    List<ProgramOutcome> findByIsActiveTrue();
    
    // Find all inactive program outcomes
    List<ProgramOutcome> findByIsActiveFalse();
    
    // Find all default program outcomes (Washington Accord)
    List<ProgramOutcome> findByIsDefaultTrue();
    
    // Find all custom program outcomes (university-specific)
    List<ProgramOutcome> findByIsDefaultFalse();
    
    // Find active default program outcomes
    List<ProgramOutcome> findByIsDefaultTrueAndIsActiveTrue();
    
    // Find active custom program outcomes
    List<ProgramOutcome> findByIsDefaultFalseAndIsActiveTrue();
    
    // Find program outcomes by creator
    List<ProgramOutcome> findByCreatedBy(String createdBy);
    
    // Find program outcomes by PO code pattern (for searching)
    @Query("SELECT po FROM ProgramOutcome po WHERE LOWER(po.poCode) LIKE LOWER(CONCAT('%', :code, '%')) AND po.isActive = true")
    List<ProgramOutcome> findByPoCodeContainingIgnoreCaseAndIsActiveTrue(@Param("code") String code);
    
    // Search program outcomes by description (case-insensitive)
    @Query("SELECT po FROM ProgramOutcome po WHERE LOWER(po.poDescription) LIKE LOWER(CONCAT('%', :description, '%')) AND po.isActive = true")
    List<ProgramOutcome> findByDescriptionContainingIgnoreCaseAndIsActiveTrue(@Param("description") String description);
    
    // Count total active program outcomes
    long countByIsActiveTrue();
    
    // Count total default program outcomes
    long countByIsDefaultTrue();
    
    // Count total custom program outcomes
    long countByIsDefaultFalse();
    
    // Find all program outcomes ordered by PO code
    List<ProgramOutcome> findByIsActiveTrueOrderByPoCode();
    
    // Find program outcomes with mappings (POs that are being used)
    @Query("SELECT DISTINCT po FROM ProgramOutcome po JOIN po.mappings m WHERE po.isActive = true")
    List<ProgramOutcome> findProgramOutcomesWithMappings();
    
    // Find program outcomes without mappings (unused POs)
    @Query("SELECT po FROM ProgramOutcome po WHERE po.isActive = true AND po.id NOT IN (SELECT DISTINCT m.programOutcome.id FROM Mapping m)")
    List<ProgramOutcome> findProgramOutcomesWithoutMappings();
    
    // Find program outcomes by module (through LosPos mappings)
    @Query("SELECT DISTINCT po FROM ProgramOutcome po JOIN po.mappings m JOIN m.losPos lp WHERE lp.moduleCode = :moduleCode AND po.isActive = true")
    List<ProgramOutcome> findByModuleCode(@Param("moduleCode") String moduleCode);
    
    // Find program outcomes with approved mappings only
    @Query("SELECT DISTINCT po FROM ProgramOutcome po JOIN po.mappings m WHERE m.status = 'APPROVED' AND po.isActive = true")
    List<ProgramOutcome> findWithApprovedMappings();
}