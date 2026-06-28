package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.AssessmentItem;
import com.example.Software.project.Backend.Model.MarkType;
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

    // Only items belonging to a template that actually has submitted student marks for the
    // same LO/batch/markType/assignment — excludes templates created via "Download Template"
    // that were never followed up with an upload (or whose marks were later deleted without
    // deleting the template), which would otherwise inflate the max-marks denominator used
    // for attainment calculations whenever more than one assessment exists for a markType.
    @Query("SELECT ai FROM AssessmentItem ai WHERE ai.los.id = :losId AND ai.assessmentTemplate.batch = :batch AND ai.assessmentTemplate.markType = :markType " +
           "AND EXISTS (SELECT 1 FROM StudentMark sm WHERE sm.los.id = ai.los.id AND sm.batch = :batch AND sm.markType = :markTypeEnum " +
           "AND (sm.assignmentLabel = ai.assessmentTemplate.assignmentLabel OR (sm.assignmentLabel IS NULL AND ai.assessmentTemplate.assignmentLabel IS NULL)))")
    List<AssessmentItem> findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(
        @Param("losId") String losId,
        @Param("batch") String batch,
        @Param("markType") String markType,
        @Param("markTypeEnum") MarkType markTypeEnum
    );

    // All items for a LO+batch regardless of markType (used for cross-assignment normalization in trends)
    @Query("SELECT ai FROM AssessmentItem ai WHERE ai.los.id = :losId AND ai.assessmentTemplate.batch = :batch")
    List<AssessmentItem> findByLos_IdAndAssessmentTemplate_Batch(
        @Param("losId") String losId,
        @Param("batch") String batch
    );
}
