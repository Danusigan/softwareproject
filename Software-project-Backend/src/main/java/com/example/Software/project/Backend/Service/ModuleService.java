package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Repository.AssessmentTemplateRepository;
import com.example.Software.project.Backend.Repository.LosRepository;
import com.example.Software.project.Backend.Repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ModuleService {

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private LosRepository losRepository;

    @Autowired
    private LosService losService;

    @Autowired
    private AssessmentTemplateRepository assessmentTemplateRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Create (Admin)
    public Module createModule(Module module) throws Exception {
        if (moduleRepository.existsById(module.getModuleId())) {
            throw new Exception("Module ID already exists");
        }
        return moduleRepository.save(module);
    }

    // Read All
    public List<Module> getAllModules() {
        return moduleRepository.findAll();
    }

    // Read One
    public Optional<Module> getModuleById(String id) {
        return moduleRepository.findById(id);
    }

    // Update (Admin)
    public Module updateModule(String id, Module moduleDetails) throws Exception {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new Exception("Module not found"));

        String newModuleId = moduleDetails.getModuleId();

        // If moduleId is being changed, check if new ID already exists
        if (newModuleId != null && !newModuleId.equals(id)) {
            if (moduleRepository.existsById(newModuleId)) {
                throw new Exception("Module ID '" + newModuleId + "' already exists");
            }
            // Delete old module and create new one with updated ID
            moduleRepository.deleteById(id);
            module.setModuleId(newModuleId);
        }

        module.setModuleName(moduleDetails.getModuleName());
        return moduleRepository.save(module);
    }

    // Delete (Admin)
    @Transactional
    public void deleteModule(String id) throws Exception {
        if (!moduleRepository.existsById(id)) {
            throw new Exception("Module not found");
        }

        // 1. Delete CqiAction records FIRST — they reference BOTH module_id (NOT NULL) and los_id (nullable).
        //    Must run before LO deletions, otherwise los_id FK blocks each LO delete.
        try { jdbcTemplate.update("DELETE FROM cqi_action WHERE module_id = ?", id); } catch (Exception ignored) {}

        // 2. Nullify AssessmentTemplate.module_id — nullable FK still enforced by MySQL
        try {
            jdbcTemplate.update("UPDATE assessment_template SET module_id = NULL WHERE module_id = ?", id);
        } catch (Exception e) {
            try { assessmentTemplateRepository.deleteAll(assessmentTemplateRepository.findByModule_ModuleId(id)); } catch (Exception ignored) {}
        }

        // 3. Delete each LO — handles StudentMark, StudentAssessmentScore, AssessmentItem, LO-PO mappings
        List<String> losIds = losRepository.findIdsByModuleId(id);
        for (String losId : losIds) {
            losService.deleteLos(losId);
        }

        // 4. Delete the module
        moduleRepository.deleteById(id);
    }
}
