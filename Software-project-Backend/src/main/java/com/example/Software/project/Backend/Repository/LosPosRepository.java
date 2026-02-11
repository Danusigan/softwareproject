package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.LosPos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LosPosRepository extends JpaRepository<LosPos, String> {
    
    // Find by module code
    List<LosPos> findByModuleCode(String moduleCode);
    
    // Find by LO ID and module code
    Optional<LosPos> findByLoIdAndModuleCode(String loId, String moduleCode);
    
    // Check if LO ID exists in specific module
    boolean existsByLoIdAndModuleCode(String loId, String moduleCode);
    
    // Find by LO ID (across all modules)
    List<LosPos> findByLoId(String loId);
    
    // Find by creator
    List<LosPos> findByCreatedBy(String createdBy);
    
    // Count by module code
    long countByModuleCode(String moduleCode);
    
    // Find by module code and order by LO ID
    List<LosPos> findByModuleCodeOrderByLoId(String moduleCode);
    
    // Search by description
    @Query("SELECT lp FROM LosPos lp WHERE LOWER(lp.loDescription) LIKE LOWER(CONCAT('%', :description, '%'))")
    List<LosPos> findByLoDescriptionContainingIgnoreCase(@Param("description") String description);
    
    // Find LosPos with assignments
    @Query("SELECT DISTINCT lp FROM LosPos lp WHERE SIZE(lp.assignments) > 0")
    List<LosPos> findLosPosWithAssignments();
    
    // Find LosPos with mappings
    @Query("SELECT DISTINCT lp FROM LosPos lp WHERE SIZE(lp.mappings) > 0")
    List<LosPos> findLosPosWithMappings();
    
    // Find LosPos without assignments
    @Query("SELECT lp FROM LosPos lp WHERE SIZE(lp.assignments) = 0")
    List<LosPos> findLosPosWithoutAssignments();
    
    // Find LosPos without mappings
    @Query("SELECT lp FROM LosPos lp WHERE SIZE(lp.mappings) = 0")
    List<LosPos> findLosPosWithoutMappings();
}