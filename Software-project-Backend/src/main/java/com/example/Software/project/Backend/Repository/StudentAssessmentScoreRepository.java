package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.Student;
import com.example.Software.project.Backend.Model.StudentAssessmentScore;
import com.example.Software.project.Backend.Model.AssessmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAssessmentScoreRepository extends JpaRepository<StudentAssessmentScore, Long> {

    List<StudentAssessmentScore> findByAssessmentItem_Los_Id(String loId);

    List<StudentAssessmentScore> findByAssessmentItem_AssessmentTemplate_Id(String templateId);

    List<StudentAssessmentScore> findByAssessmentItem_Id(Long itemId);

    Optional<StudentAssessmentScore> findByStudentAndAssessmentItem(Student student, AssessmentItem item);

    @Query("SELECT s FROM StudentAssessmentScore s " +
        "WHERE s.assessmentItem.assessmentTemplate.id = :templateId " +
        "AND s.student.studentId = :studentId")
    List<StudentAssessmentScore> findByTemplateIdAndStudentId(
         @Param("templateId") String templateId,
         @Param("studentId") String studentId);

    @Query("SELECT DISTINCT s.student.studentId " +
        "FROM StudentAssessmentScore s " +
        "WHERE s.assessmentItem.assessmentTemplate.id = :templateId")
    List<String> findDistinctStudentIdsByTemplateId(@Param("templateId") String templateId);

    // Scores must go before the assessment_item rows they point at, otherwise the FK
    // student_assessment_score.assessment_item_id blocks the item delete. A bulk delete rather
    // than a derived deleteBy... so it executes immediately: a derived delete only queues
    // em.remove() calls that Hibernate runs at flush time, by which point the cascaded item
    // deletes (or, on a re-upload, the IDENTITY-generated score inserts) have already hit the
    // database.
    @Modifying
    @Transactional
    @Query("delete from StudentAssessmentScore s where s.assessmentItem.id in "
            + "(select i.id from AssessmentItem i where i.assessmentTemplate.id = :templateId)")
    void deleteByAssessmentItem_AssessmentTemplate_Id(@Param("templateId") String templateId);
}
