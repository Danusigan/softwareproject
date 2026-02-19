package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.StudentMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentMarkRepository extends JpaRepository<StudentMark, Long> {
    List<StudentMark> findByAssessment_AssignmentId(String assessmentId);

    // Module-level Trend (General Average)
    @Query("SELECT a.academicYear, a.batch, AVG(sm.score) " +
           "FROM StudentMark sm " +
           "JOIN sm.assessment a " +
           "JOIN a.losPos lo " + 
           "JOIN lo.module m " +
           "WHERE m.moduleId = :courseId " +
           "GROUP BY a.academicYear, a.batch " +
           "ORDER BY a.academicYear ASC, a.batch ASC")
    List<Object[]> findYearlyAverageByCourse(String courseId);

    // LO-level Trend (Average per LO per Year)
    @Query("SELECT lo.id, lo.name, a.academicYear, a.batch, AVG(sm.score) " +
           "FROM StudentMark sm " +
           "JOIN sm.assessment a " +
           "JOIN a.losPos lo " + 
           "JOIN lo.module m " +
           "WHERE m.moduleId = :courseId " +
           "GROUP BY lo.id, lo.name, a.academicYear, a.batch " +
           "ORDER BY lo.id, a.academicYear ASC, a.batch ASC")
    List<Object[]> findLoTrendByCourse(String courseId);
}