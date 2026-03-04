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

    // Create (Lecture) - Add to Module
    public Los addLosToModule(String moduleId, Los los) throws Exception {
        Optional<Module> moduleOptional = moduleRepository.findById(moduleId);
        if (moduleOptional.isEmpty()) {
            throw new Exception("Module not found");
        }
        if (losRepository.existsById(los.getId())) {
            throw new Exception("Los ID already exists");
        }
        los.setModule(moduleOptional.get());
        return losRepository.save(los);
    }

    // Read All Los by Module ID
    public List<Los> getLosByModuleId(String moduleId) {
        return losRepository.findByModule_ModuleId(moduleId);
    }

    // Read One Los
    public Optional<Los> getLosById(String id) {
        return losRepository.findById(id);
    }

    // Update Los (Lecture)
    public Los updateLos(String id, Los losDetails) throws Exception {
        Los los = losRepository.findById(id)
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
        if (!losRepository.existsById(id)) {
            throw new Exception("Los not found");
        }

        studentMarkRepository.deleteByLos_Id(id);

        jdbcTemplate.update("DELETE FROM lo_po_mappings WHERE los_id = ?", id);

        try {
            jdbcTemplate.update("DELETE FROM assignments WHERE los_pos_id = ?", id);
        } catch (Exception ignored) {
        }

        losRepository.deleteById(id);
    }
}
