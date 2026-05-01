package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.StudentAssessmentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAssessmentScoreRepository extends JpaRepository<StudentAssessmentScore, Long> {

    List<StudentAssessmentScore> findByAssessmentItem_Los_Id(String loId);

    List<StudentAssessmentScore> findByAssessmentItem_AssessmentTemplate_Id(String templateId);

    List<StudentAssessmentScore> findByAssessmentItem_Id(Long itemId);

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

    void deleteByAssessmentItem_AssessmentTemplate_Id(String templateId);
}
