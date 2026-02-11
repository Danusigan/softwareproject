package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.LosPos;
import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Repository.LosPosRepository;
import com.example.Software.project.Backend.Repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LosPosService {

    @Autowired
    private LosPosRepository losPosRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    // Create new LosPos for a module
    public LosPos createLosPos(String loId, String loDescription, String moduleCode, String createdBy) {
        // Validate that module exists
        Optional<Module> moduleOpt = moduleRepository.findById(moduleCode);
        if (moduleOpt.isEmpty()) {
            throw new RuntimeException("Module not found: " + moduleCode);
        }

        // Generate unique ID for LosPos
        String losPosId = moduleCode + "_" + loId;
        
        // Check if LO ID already exists for this module
        if (losPosRepository.existsById(losPosId)) {
            throw new RuntimeException("Learning Outcome ID already exists for this module: " + loId);
        }

        LosPos losPos = new LosPos();
        losPos.setId(losPosId);
        losPos.setLoId(loId);
        losPos.setLoDescription(loDescription);
        losPos.setModuleCode(moduleCode);
        losPos.setCreatedBy(createdBy);

        return losPosRepository.save(losPos);
    }

    // Get all LosPos
    public List<LosPos> getAllLosPos() {
        return losPosRepository.findAll();
    }

    // Get LosPos by module code
    public List<LosPos> getLosPosByModuleCode(String moduleCode) {
        return losPosRepository.findByModuleCode(moduleCode);
    }

    // Get LosPos by ID
    public Optional<LosPos> getLosPosById(String id) {
        return losPosRepository.findById(id);
    }

    // Update LosPos
    public LosPos updateLosPos(String id, String loDescription, String requestingUser) {
        Optional<LosPos> optionalLosPos = losPosRepository.findById(id);
        if (optionalLosPos.isEmpty()) {
            throw new RuntimeException("Learning Outcome not found: " + id);
        }

        LosPos losPos = optionalLosPos.get();
        
        // Check if user has permission (could be enhanced with proper authorization)
        if (losPos.getCreatedBy() != null && !losPos.getCreatedBy().equals(requestingUser)) {
            // For now, allow Admin/SuperAdmin to edit all, Lecturer to edit their own
            // This would be enhanced with proper role checking
        }

        losPos.setLoDescription(loDescription);
        return losPosRepository.save(losPos);
    }

    // Delete LosPos
    public void deleteLosPos(String id, String requestingUser) {
        Optional<LosPos> optionalLosPos = losPosRepository.findById(id);
        if (optionalLosPos.isEmpty()) {
            throw new RuntimeException("Learning Outcome not found: " + id);
        }

        LosPos losPos = optionalLosPos.get();
        
        // Check if user has permission
        if (losPos.getCreatedBy() != null && !losPos.getCreatedBy().equals(requestingUser)) {
            // Enhanced authorization would go here
        }

        // Check if LosPos has any assignments or mappings
        if (losPos.getAssignments() != null && !losPos.getAssignments().isEmpty()) {
            throw new RuntimeException("Cannot delete Learning Outcome with existing assignments");
        }
        if (losPos.getMappings() != null && !losPos.getMappings().isEmpty()) {
            throw new RuntimeException("Cannot delete Learning Outcome with existing mappings");
        }

        losPosRepository.deleteById(id);
    }

    // Get LosPos by LO ID and module code
    public Optional<LosPos> getLosPosByLoIdAndModuleCode(String loId, String moduleCode) {
        return losPosRepository.findByLoIdAndModuleCode(loId, moduleCode);
    }

    // Check if LO ID exists in module
    public boolean existsLoIdInModule(String loId, String moduleCode) {
        return losPosRepository.existsByLoIdAndModuleCode(loId, moduleCode);
    }

    // Count LosPos by module
    public long countLosPosByModuleCode(String moduleCode) {
        return losPosRepository.countByModuleCode(moduleCode);
    }
}