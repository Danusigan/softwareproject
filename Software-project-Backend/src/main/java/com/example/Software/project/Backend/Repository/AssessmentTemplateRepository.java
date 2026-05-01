package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.AssessmentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentTemplateRepository extends JpaRepository<AssessmentTemplate, String> {
    List<AssessmentTemplate> findByModule_ModuleId(String moduleId);
    List<AssessmentTemplate> findByModule_ModuleIdOrderByCreatedAtDesc(String moduleId);
    List<AssessmentTemplate> findByModule_ModuleIdAndBatch(String moduleId, String batch);
}
