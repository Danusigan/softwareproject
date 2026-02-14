package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.StudentMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentMarkRepository extends JpaRepository<StudentMark, Long> {
    List<StudentMark> findByAssessment_AssignmentId(String assessmentId);

    // Trend Analysis Query
    // Updated to use a.academicYear instead of m.academicYear
    @Query("SELECT a.academicYear, AVG(sm.score) " +
           "FROM StudentMark sm " +
           "JOIN sm.assessment a " +
           "JOIN a.losPos lo " + 
           "JOIN lo.module m " +
           "WHERE m.moduleId = :courseId " +
           "GROUP BY a.academicYear " +
           "ORDER BY a.academicYear ASC")
    List<Object[]> findYearlyAverageByCourse(String courseId);
}