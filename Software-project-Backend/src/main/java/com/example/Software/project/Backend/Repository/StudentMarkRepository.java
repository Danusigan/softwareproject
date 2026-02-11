package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.StudentMark;
import com.example.Software.project.Backend.Model.Student;
import com.example.Software.project.Backend.Model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentMarkRepository extends JpaRepository<StudentMark, Long> {
    
    // Find all marks by assignment
    List<StudentMark> findByAssignment(Assignment assignment);
    
    // Find all marks by assignment ID
    List<StudentMark> findByAssignmentAssignmentId(String assignmentId);
    
    // Find all marks by student
    List<StudentMark> findByStudent(Student student);
    
    // Find all marks by student ID
    List<StudentMark> findByStudentStudentId(String studentId);
    
    // Find specific mark for student and assignment
    Optional<StudentMark> findByStudentAndAssignment(Student student, Assignment assignment);
    
    // Find specific mark by student ID and assignment ID
    Optional<StudentMark> findByStudentStudentIdAndAssignmentAssignmentId(String studentId, String assignmentId);
    
    // Check if mark exists for student and assignment
    boolean existsByStudentAndAssignment(Student student, Assignment assignment);
    
    // Find all valid marks (not absent, not medical) for an assignment
    @Query("SELECT sm FROM StudentMark sm WHERE sm.assignment.assignmentId = :assignmentId AND sm.isAbsent = false AND sm.isMedical = false AND sm.mark IS NOT NULL")
    List<StudentMark> findValidMarksByAssignmentId(@Param("assignmentId") String assignmentId);
    
    // Find all absent marks for an assignment
    @Query("SELECT sm FROM StudentMark sm WHERE sm.assignment.assignmentId = :assignmentId AND sm.isAbsent = true")
    List<StudentMark> findAbsentMarksByAssignmentId(@Param("assignmentId") String assignmentId);
    
    // Find all medical marks for an assignment
    @Query("SELECT sm FROM StudentMark sm WHERE sm.assignment.assignmentId = :assignmentId AND sm.isMedical = true")
    List<StudentMark> findMedicalMarksByAssignmentId(@Param("assignmentId") String assignmentId);
    
    // Calculate average mark for an assignment (excluding absent/medical)
    @Query("SELECT AVG(sm.mark) FROM StudentMark sm WHERE sm.assignment.assignmentId = :assignmentId AND sm.isAbsent = false AND sm.isMedical = false AND sm.mark IS NOT NULL")
    Double calculateAverageMarkForAssignment(@Param("assignmentId") String assignmentId);
    
    // Count total submissions for an assignment
    @Query("SELECT COUNT(sm) FROM StudentMark sm WHERE sm.assignment.assignmentId = :assignmentId")
    long countSubmissionsForAssignment(@Param("assignmentId") String assignmentId);
    
    // Count valid submissions for an assignment
    @Query("SELECT COUNT(sm) FROM StudentMark sm WHERE sm.assignment.assignmentId = :assignmentId AND sm.isAbsent = false AND sm.isMedical = false AND sm.mark IS NOT NULL")
    long countValidSubmissionsForAssignment(@Param("assignmentId") String assignmentId);
    
    // Count absent submissions for an assignment
    @Query("SELECT COUNT(sm) FROM StudentMark sm WHERE sm.assignment.assignmentId = :assignmentId AND sm.isAbsent = true")
    long countAbsentSubmissionsForAssignment(@Param("assignmentId") String assignmentId);
    
    // Count medical submissions for an assignment
    @Query("SELECT COUNT(sm) FROM StudentMark sm WHERE sm.assignment.assignmentId = :assignmentId AND sm.isMedical = true")
    long countMedicalSubmissionsForAssignment(@Param("assignmentId") String assignmentId);
    
    // Find marks above a certain threshold for OBE calculations
    @Query("SELECT sm FROM StudentMark sm WHERE sm.assignment.assignmentId = :assignmentId AND sm.mark >= :threshold AND sm.isAbsent = false AND sm.isMedical = false")
    List<StudentMark> findMarksAboveThreshold(@Param("assignmentId") String assignmentId, @Param("threshold") Double threshold);
    
    // Calculate pass rate for an assignment (marks >= 50)
    @Query("SELECT (COUNT(sm) * 100.0 / (SELECT COUNT(sm2) FROM StudentMark sm2 WHERE sm2.assignment.assignmentId = :assignmentId AND sm2.isAbsent = false AND sm2.isMedical = false)) FROM StudentMark sm WHERE sm.assignment.assignmentId = :assignmentId AND sm.mark >= 50 AND sm.isAbsent = false AND sm.isMedical = false")
    Double calculatePassRateForAssignment(@Param("assignmentId") String assignmentId);
    
    // Find all marks by LosPos (through assignment relationship)
    @Query("SELECT sm FROM StudentMark sm WHERE sm.assignment.losPos.id = :losPosId")
    List<StudentMark> findByLosPosId(@Param("losPosId") String losPosId);
    
    // Find marks for OBE attainment calculation by LosPos
    @Query("SELECT sm FROM StudentMark sm WHERE sm.assignment.losPos.id = :losPosId AND sm.isAbsent = false AND sm.isMedical = false AND sm.mark IS NOT NULL")
    List<StudentMark> findValidMarksByLosPosId(@Param("losPosId") String losPosId);
}