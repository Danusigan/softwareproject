package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.StudentMark;
import com.example.Software.project.Backend.Model.MarkType;
import com.example.Software.project.Backend.Model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentMarkRepository extends JpaRepository<StudentMark, Long> {
    List<StudentMark> findByLos_Id(String losId);
    List<StudentMark> findByLos_IdOrderByIdDesc(String losId);

    Optional<StudentMark> findByStudentAndLos_IdAndBatchAndMarkType(Student student, String losId, String batch, MarkType markType);

    Optional<StudentMark> findByStudentAndLos_IdAndBatchAndMarkTypeAndAssignmentLabel(Student student, String losId, String batch, MarkType markType, String assignmentLabel);

    // Query by mark type and batch
    @Query("SELECT sm FROM StudentMark sm WHERE sm.los.id IN :losIds AND sm.markType = :markType AND sm.batch = :batch ORDER BY sm.student.studentId ASC")
       List<StudentMark> findByLosIdsAndMarkTypeAndBatch(@Param("losIds") List<String> losIds, @Param("markType") MarkType markType, @Param("batch") String batch);

    // Get all unique students for given LOs
   @Query("SELECT DISTINCT s FROM StudentMark sm JOIN sm.student s WHERE sm.los.id IN :losIds AND sm.markType = :markType AND sm.batch = :batch ORDER BY s.studentId ASC")
       List<Student> findDistinctStudentsByLosIdsAndMarkTypeAndBatch(@Param("losIds") List<String> losIds, @Param("markType") MarkType markType, @Param("batch") String batch);

    // Get all batches for given LOs and mark type
    @Query("SELECT DISTINCT sm.batch FROM StudentMark sm WHERE sm.los.id IN :losIds AND sm.markType = :markType AND sm.batch IS NOT NULL ORDER BY sm.batch")
       List<String> findDistinctBatchesByLosIdsAndMarkType(@Param("losIds") List<String> losIds, @Param("markType") MarkType markType);

    @Modifying
    @Transactional
    void deleteByLos_Id(String losId);

    // Batch-specific queries
    @Query("SELECT DISTINCT sm.batch FROM StudentMark sm WHERE sm.los.id = :losId AND sm.batch IS NOT NULL ORDER BY sm.batch")
    List<String> findDistinctBatchesByLosId(String losId);

    List<StudentMark> findByLos_IdAndBatch(String losId, String batch);

    long countByLos_IdAndBatch(String losId, String batch);

    @Modifying
    @Transactional
    void deleteByLos_IdAndBatch(String losId, String batch);

    @Modifying
    @Transactional
    @Query("DELETE FROM StudentMark sm WHERE sm.los.id = :losId AND sm.batch = :batch AND sm.markType = :markType AND sm.assignmentLabel = :assignmentLabel")
    void deleteByLos_IdAndBatchAndMarkTypeAndAssignmentLabel(@Param("losId") String losId, @Param("batch") String batch, @Param("markType") MarkType markType, @Param("assignmentLabel") String assignmentLabel);

    @Modifying
    @Transactional
    @Query("DELETE FROM StudentMark sm WHERE sm.los.id IN " +
           "(SELECT lo.id FROM Los lo JOIN lo.module m WHERE m.moduleId = :moduleId) " +
           "AND sm.batch = :batch AND sm.markType = :markType AND sm.assignmentLabel = :assignmentLabel")
    void deleteByModuleIdAndBatchAndMarkTypeAndAssignmentLabel(@Param("moduleId") String moduleId, @Param("batch") String batch, @Param("markType") MarkType markType, @Param("assignmentLabel") String assignmentLabel);

    // Batch-level Trend (General Average per Batch)
    @Query("SELECT lo.batch, AVG(sm.score) " +
           "FROM StudentMark sm " +
           "JOIN sm.los lo " +
           "JOIN lo.module m " +
           "WHERE m.moduleId = :courseId " +
           "GROUP BY lo.batch " +
           "ORDER BY lo.batch ASC")
    List<Object[]> findYearlyAverageByCourse(@Param("courseId") String courseId);

    // LO-level Trend (Average per LO per Batch) — uses sm.batch (mark upload batch)
    @Query("SELECT lo.id, lo.name, sm.batch, AVG(sm.score) " +
           "FROM StudentMark sm " +
           "JOIN sm.los lo " +
           "JOIN lo.module m " +
           "WHERE m.moduleId = :courseId AND sm.batch IS NOT NULL " +
           "GROUP BY lo.id, lo.name, sm.batch " +
           "ORDER BY lo.id, sm.batch ASC")
    List<Object[]> findLoTrendByCourse(@Param("courseId") String courseId);

    // Available marks summary: distinct batch + markType + assignmentLabel for a module
    @Query("SELECT DISTINCT sm.batch, sm.markType, sm.assignmentLabel, COUNT(sm.id), COUNT(DISTINCT sm.los.id) " +
           "FROM StudentMark sm " +
           "JOIN sm.los lo " +
           "JOIN lo.module m " +
           "WHERE m.moduleId = :moduleId AND sm.batch IS NOT NULL " +
           "GROUP BY sm.batch, sm.markType, sm.assignmentLabel " +
           "ORDER BY sm.batch, sm.markType, sm.assignmentLabel")
    List<Object[]> findMarksSummaryByModuleId(@Param("moduleId") String moduleId);

    // Get LO IDs for a module+batch+markType
    @Query("SELECT DISTINCT sm.los.id FROM StudentMark sm " +
           "JOIN sm.los lo JOIN lo.module m " +
           "WHERE m.moduleId = :moduleId AND sm.batch = :batch AND sm.markType = :markType")
    List<String> findLoIdsByModuleIdAndBatchAndMarkType(@Param("moduleId") String moduleId, @Param("batch") String batch, @Param("markType") MarkType markType);

    // Delete all marks for a module+batch+markType
    @Modifying
    @Transactional
    @Query("DELETE FROM StudentMark sm WHERE sm.los.id IN " +
           "(SELECT lo.id FROM Los lo JOIN lo.module m WHERE m.moduleId = :moduleId) " +
           "AND sm.batch = :batch AND sm.markType = :markType")
    void deleteByModuleIdAndBatchAndMarkType(@Param("moduleId") String moduleId, @Param("batch") String batch, @Param("markType") MarkType markType);

    // Available marks summary for a single LO: distinct batch + markType
    @Query("SELECT DISTINCT sm.batch, sm.markType, COUNT(sm.id) " +
           "FROM StudentMark sm WHERE sm.los.id = :losId AND sm.batch IS NOT NULL " +
           "GROUP BY sm.batch, sm.markType ORDER BY sm.batch, sm.markType")
    List<Object[]> findMarksSummaryByLosId(@Param("losId") String losId);

    // LO-level Pass Rate (% students scoring >= threshold per LO per Batch)
    @Query("SELECT lo.id, lo.name, sm.batch, " +
           "COUNT(sm.id), " +
           "SUM(CASE WHEN sm.score >= :threshold THEN 1 ELSE 0 END) " +
           "FROM StudentMark sm " +
           "JOIN sm.los lo " +
           "JOIN lo.module m " +
           "WHERE m.moduleId = :courseId " +
           "GROUP BY lo.id, lo.name, sm.batch " +
           "ORDER BY lo.id, sm.batch ASC")
    List<Object[]> findLoPassRateByCourse(@Param("courseId") String courseId, @Param("threshold") double threshold);
}
