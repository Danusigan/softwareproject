package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.AssessmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentItemRepository extends JpaRepository<AssessmentItem, Long> {
    List<AssessmentItem> findByLos_Id(String loId);
    List<AssessmentItem> findByAssessmentTemplate_Id(String templateId);
    List<AssessmentItem> findByAssessmentTemplate_IdOrderByQuestionNumber(String templateId);
    void deleteByAssessmentTemplate_Id(String templateId);

    @Query("SELECT ai FROM AssessmentItem ai WHERE ai.los.id = :losId AND ai.assessmentTemplate.batch = :batch AND ai.assessmentTemplate.markType = :markType")
    List<AssessmentItem> findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(
        @Param("losId") String losId,
        @Param("batch") String batch,
        @Param("markType") String markType
    );
}
