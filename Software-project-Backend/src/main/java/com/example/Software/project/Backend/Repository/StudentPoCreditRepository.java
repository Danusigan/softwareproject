package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.StudentPoCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface StudentPoCreditRepository extends JpaRepository<StudentPoCredit, Long> {

    // Overwrite-in-place: called before re-saving a module/batch's credits so a recalculation
    // replaces the previous result rather than accumulating alongside it.
    //
    // Deliberately a bulk delete rather than a derived deleteBy... query. A derived delete only
    // queues em.remove() calls, which Hibernate runs at flush - after inserts. StudentPoCredit
    // uses GenerationType.IDENTITY, so saving the fresh rows immediately executes their INSERTs
    // to obtain the generated keys, meaning they hit the database while the previous rows are
    // still there and collide with uk_student_po_credit. This form executes the DELETE straight
    // away, so the re-insert always lands on an empty slot.
    @Modifying
    @Transactional
    @Query("delete from StudentPoCredit c where c.module.moduleId = :moduleId and c.batch = :batch")
    void deleteByModule_ModuleIdAndBatch(@Param("moduleId") String moduleId, @Param("batch") String batch);

    // Every saved credit row for one student, regardless of which module earned it — the raw
    // material for the cross-module cumulative PO summary.
    List<StudentPoCredit> findByStudent_StudentId(String studentId);

    // Every saved credit row for one batch, across all modules — the raw material for the admin
    // batch PO report (PoReportService.batchReport).
    List<StudentPoCredit> findByBatch(String batch);
}
