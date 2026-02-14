package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, String> {
    
    // Find assignments by LosPos ID
    List<Assignment> findByLosPosId(String losPosId);
    
    // Find assignments by assignment name
    List<Assignment> findByAssignmentNameContainingIgnoreCase(String assignmentName);
    
    // Find assignments by LosPos Module Code
    // Fixed: a.losPos.module.moduleId instead of a.losPos.moduleCode
    @Query("SELECT a FROM Assignment a WHERE a.losPos.module.moduleId = :moduleCode")
    List<Assignment> findByModuleCode(@Param("moduleCode") String moduleCode);
    
    // Find assignments created by a specific user
    List<Assignment> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    
    // Find assignments within a date range
    @Query("SELECT a FROM Assignment a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<Assignment> findByCreatedAtBetween(@Param("startDate") java.time.LocalDateTime startDate, 
                                          @Param("endDate") java.time.LocalDateTime endDate);
}