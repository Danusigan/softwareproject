package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    
    // Find student by email
    Optional<Student> findByEmail(String email);
    
    // Find students by academic year
    List<Student> findByAcademicYear(String academicYear);
    
    // Check if student exists by student ID
    boolean existsByStudentId(String studentId);
    
    // Check if student exists by email
    boolean existsByEmail(String email);
    
    // Find students by name (case-insensitive partial match)
    @Query("SELECT s FROM Student s WHERE LOWER(s.studentName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Student> findByStudentNameContainingIgnoreCase(@Param("name") String name);
    
    // Find students by academic year and order by student name
    List<Student> findByAcademicYearOrderByStudentName(String academicYear);
    
    // Count total students
    @Query("SELECT COUNT(s) FROM Student s")
    long countAllStudents();
    
    // Count students by academic year
    long countByAcademicYear(String academicYear);
    
    // Find students who have submitted at least one assignment
    @Query("SELECT DISTINCT s FROM Student s JOIN s.studentMarks sm WHERE sm.mark IS NOT NULL")
    List<Student> findStudentsWithMarks();
    
    // Find students by part of student ID (useful for batch operations)
    @Query("SELECT s FROM Student s WHERE s.studentId LIKE :pattern")
    List<Student> findByStudentIdPattern(@Param("pattern") String pattern);
}