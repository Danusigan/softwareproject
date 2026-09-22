package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.MarkType;
import com.example.Software.project.Backend.Model.StudentPoCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface StudentPoCreditRepository extends JpaRepository<StudentPoCredit, Long> {

    // Overwrite-in-place: called before re-saving a module/batch/markType's credits so a
    // recalculation replaces the previous result rather than accumulating alongside it.
    @Modifying
    @Transactional
    void deleteByModule_ModuleIdAndBatchAndMarkType(String moduleId, String batch, MarkType markType);

    // Every saved credit row for one student, regardless of which module or mark type earned
    // it — the raw material for the cross-module cumulative PO summary.
    List<StudentPoCredit> findByStudent_StudentId(String studentId);

    List<StudentPoCredit> findByStudent_StudentIdAndMarkType(String studentId, MarkType markType);
}
