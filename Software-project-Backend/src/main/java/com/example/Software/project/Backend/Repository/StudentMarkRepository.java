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

    // Batch-level Trend (General Average per Batch)
    @Query("SELECT lo.batch, AVG(sm.score) " +
           "FROM StudentMark sm " +
           "JOIN sm.los lo " +
           "JOIN lo.module m " +
           "WHERE m.moduleId = :courseId " +
           "GROUP BY lo.batch " +
           "ORDER BY lo.batch ASC")
    List<Object[]> findYearlyAverageByCourse(@Param("courseId") String courseId);

    // LO-level Trend (Average per LO per Batch)
    @Query("SELECT lo.id, lo.name, lo.batch, AVG(sm.score) " +
           "FROM StudentMark sm " +
           "JOIN sm.los lo " +
           "JOIN lo.module m " +
           "WHERE m.moduleId = :courseId " +
           "GROUP BY lo.id, lo.name, lo.batch " +
           "ORDER BY lo.id, lo.batch ASC")
    List<Object[]> findLoTrendByCourse(@Param("courseId") String courseId);
}
