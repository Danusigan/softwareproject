package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Los;
import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Repository.LosRepository;
import com.example.Software.project.Backend.Repository.ModuleRepository;
import com.example.Software.project.Backend.Repository.StudentMarkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class LosService {

    @Autowired
    private LosRepository losRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private StudentMarkRepository studentMarkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String normalizeLoId(String id) {
        return id == null ? null : id.trim().toUpperCase();
    }

    private String buildModuleScopedLosId(String moduleId, String loId) {
        String normalizedModuleId = normalizeLoId(moduleId);
        String normalizedLoId = normalizeLoId(loId);
        return normalizedModuleId + " " + normalizedLoId;
    }

    private String resolveStoredLosId(String id) throws Exception {
        String normalized = normalizeLoId(id);
        if (normalized == null || normalized.isEmpty()) {
            throw new Exception("Los ID is required");
        }

        if (losRepository.existsById(normalized)) {
            return normalized;
        }

        List<Los> suffixMatches = losRepository.findByIdEndingWith(" " + normalized);
        if (suffixMatches.size() == 1) {
            return suffixMatches.get(0).getId();
        }
        if (suffixMatches.size() > 1) {
            throw new Exception("Multiple modules contain Los ID '" + normalized + "'. Use module-specific Los ID.");
        }

        throw new Exception("Los not found");
    }

    // Create (Lecture) - Add to Module
    public Los addLosToModule(String moduleId, Los los) throws Exception {
        Optional<Module> moduleOptional = moduleRepository.findById(moduleId);
        if (moduleOptional.isEmpty()) {
            throw new Exception("Module not found");
        }
        String requestedLoId = normalizeLoId(los.getId());
        if (requestedLoId == null || requestedLoId.isEmpty()) {
            throw new Exception("Los ID is required");
        }

        String storedLosId = buildModuleScopedLosId(moduleId, requestedLoId);
        if (losRepository.existsById(storedLosId)) {
            throw new Exception("Los ID already exists for this module");
        }

        los.setId(storedLosId);
        los.setModule(moduleOptional.get());
        return losRepository.save(los);
    }

    // Read All Los by Module ID
    public List<Los> getLosByModuleId(String moduleId) {
        return losRepository.findByModule_ModuleId(moduleId);
    }

    // Read One Los
    public Optional<Los> getLosById(String id) {
        try {
            return losRepository.findById(resolveStoredLosId(id));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Update Los (Lecture)
    public Los updateLos(String id, Los losDetails) throws Exception {
        String storedLosId = resolveStoredLosId(id);
        Los los = losRepository.findById(storedLosId)
            .orElseThrow(() -> new Exception("Los not found"));

        los.setName(losDetails.getName());
        if (losDetails.getDescription() != null) {
            los.setDescription(losDetails.getDescription());
        }
        if (losDetails.getBatch() != null) {
            los.setBatch(losDetails.getBatch());
        }
        if (losDetails.getMarksCsvFile() != null) {
            los.setMarksCsvFile(losDetails.getMarksCsvFile());
        }
        if (losDetails.getFileName() != null) {
            los.setFileName(losDetails.getFileName());
        }
        return losRepository.save(los);
    }

    // Delete Los (Lecture)
    @Transactional
    public void deleteLos(String id) throws Exception {
        String storedLosId = resolveStoredLosId(id);

        studentMarkRepository.deleteByLos_Id(storedLosId);

        jdbcTemplate.update("DELETE FROM lo_po_mappings WHERE los_id = ? OR lospos_id = ?", storedLosId, storedLosId);

        try {
            jdbcTemplate.update("DELETE FROM assignments WHERE los_pos_id = ?", storedLosId);
        } catch (Exception ignored) {
        }

        losRepository.deleteById(storedLosId);
    }
}
