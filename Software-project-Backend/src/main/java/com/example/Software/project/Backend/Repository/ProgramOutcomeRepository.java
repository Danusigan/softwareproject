package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.ProgramOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramOutcomeRepository extends JpaRepository<ProgramOutcome, String> {
    
    // Find by code
    Optional<ProgramOutcome> findByCode(String code);
    
    // Check if exists by code
    boolean existsByCode(String code);
    
    // Find all ordered by display order
    List<ProgramOutcome> findAllByOrderByDisplayOrderAsc();
    
    // Find active POs ordered by display order
    List<ProgramOutcome> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    // Find inactive POs
    List<ProgramOutcome> findByIsActiveFalseOrderByDisplayOrderAsc();
    
    // Find default POs (Washington Accord standard)
    List<ProgramOutcome> findByIsDefaultTrueOrderByDisplayOrderAsc();
    
    // Find custom POs (non-default)
    List<ProgramOutcome> findByIsDefaultFalseOrderByDisplayOrderAsc();
    
    // Find by category
    List<ProgramOutcome> findByCategoryOrderByDisplayOrderAsc(String category);
    
    // Find active POs by category
    List<ProgramOutcome> findByIsActiveTrueAndCategoryOrderByDisplayOrderAsc(String category);
    
    // Custom query to find POs with specific criteria
    @Query("SELECT po FROM ProgramOutcome po WHERE " +
           "(:isActive IS NULL OR po.isActive = :isActive) AND " +
           "(:isDefault IS NULL OR po.isDefault = :isDefault) AND " +
           "(:category IS NULL OR po.category = :category) " +
           "ORDER BY po.displayOrder ASC")
    List<ProgramOutcome> findPOsWithCriteria(
        @Param("isActive") Boolean isActive,
        @Param("isDefault") Boolean isDefault,
        @Param("category") String category
    );
    
    // Search POs by title or description
    @Query("SELECT po FROM ProgramOutcome po WHERE " +
           "po.isActive = true AND " +
           "(LOWER(po.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(po.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(po.code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY po.displayOrder ASC")
    List<ProgramOutcome> searchActivePOs(@Param("searchTerm") String searchTerm);
    
    // Count by category
    long countByCategory(String category);
    
    // Count active POs
    long countByIsActiveTrue();
    
    // Count default POs
    long countByIsDefaultTrue();

    @Modifying
    @Query("DELETE FROM ProgramOutcome po WHERE po.isDefault = true")
    void deleteAllDefaultPOs();
}