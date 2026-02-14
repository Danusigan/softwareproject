package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.OutcomeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutcomeMappingRepository extends JpaRepository<OutcomeMapping, Long> {
    List<OutcomeMapping> findByLearningOutcome_Id(String loId);
    List<OutcomeMapping> findByLearningOutcome_Module_ModuleId(String moduleId);
}