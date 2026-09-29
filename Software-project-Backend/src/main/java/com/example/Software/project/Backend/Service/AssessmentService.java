package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AssessmentService {

    @Autowired
    private AssessmentTemplateRepository templateRepository;
    @Autowired
    private AssessmentItemRepository itemRepository;
    @Autowired
    private ModuleRepository moduleRepository;
    @Autowired
    private LosRepository losRepository;

    @Transactional
    public AssessmentTemplate createTemplate(Map<String, Object> payload, String createdBy) {
        String id = payload.get("id") != null ? payload.get("id").toString() : null;
        String name = payload.get("name") != null ? payload.get("name").toString() : null;
        String moduleId = payload.get("moduleId") != null ? payload.get("moduleId").toString() : null;
        String batch = payload.get("batch") != null ? payload.get("batch").toString() : null;
        String markType = payload.get("markType") != null ? payload.get("markType").toString() : null;
        String semester = payload.get("semester") != null ? payload.get("semester").toString() : null;
        String academicYear = payload.get("academicYear") != null ? payload.get("academicYear").toString() : null;

        AssessmentTemplate template = new AssessmentTemplate();
        template.setId(id != null ? id : java.util.UUID.randomUUID().toString());
        template.setName(name != null ? name : "Template");
        template.setBatch(batch);
        template.setMarkType(markType);
        template.setSemester(semester);
        template.setAcademicYear(academicYear);
        template.setCreatedBy(createdBy);
        template.setCreatedAt(LocalDateTime.now());

        if (moduleId != null) {
            moduleRepository.findById(moduleId).ifPresent(template::setModule);
        }

        // Persist template first so items can reference it
        AssessmentTemplate saved = templateRepository.save(template);

        // Parse items array
        Object itemsObj = payload.get("items");
        if (itemsObj instanceof List) {
            List<?> itemsList = (List<?>) itemsObj;
            List<AssessmentItem> toSave = new ArrayList<>();
            for (Object o : itemsList) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> itemMap = (Map<?, ?>) o;
                AssessmentItem ai = new AssessmentItem();
                Object qLabelObj = itemMap.get("questionLabel");
                ai.setQuestionLabel(qLabelObj != null ? qLabelObj.toString() : "");
                try {
                    Object qNumObj = itemMap.get("questionNumber");
                    ai.setQuestionNumber(Integer.parseInt(qNumObj != null ? qNumObj.toString() : "0"));
                } catch (Exception ex) { ai.setQuestionNumber(0); }
                try {
                    Object maxObj = itemMap.get("maxMarks");
                    ai.setMaxMarks(Double.parseDouble(maxObj != null ? maxObj.toString() : "0"));
                } catch (Exception ex) { ai.setMaxMarks(0.0); }

                Object loObj = itemMap.get("loId");
                String loId = loObj != null ? loObj.toString() : null;
                if (loId != null) {
                    losRepository.findById(loId).ifPresent(ai::setLos);
                }

                ai.setAssessmentTemplate(saved);
                toSave.add(ai);
            }
            if (!toSave.isEmpty()) {
                itemRepository.saveAll(toSave);
            }
        }

        return saved;
    }

    public List<AssessmentTemplate> listTemplatesByModule(String moduleId) {
        return templateRepository.findByModule_ModuleIdOrderByCreatedAtDesc(moduleId);
    }

    public AssessmentTemplate getTemplate(String templateId) {
        return templateRepository.findById(templateId).orElse(null);
    }

}
