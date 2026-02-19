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

    // Create (Lecture) - Add to Module
    public LosPos addLosPosToModule(String moduleId, LosPos losPos) throws Exception {
        Optional<Module> moduleOptional = moduleRepository.findById(moduleId);
        if (moduleOptional.isEmpty()) {
            throw new Exception("Module not found");
        }
        
        // Ensure ID uniqueness by prefixing it with moduleId
        if (losPos.getId() == null || losPos.getId().trim().isEmpty()) {
            throw new Exception("Learning Outcome ID cannot be empty");
        }
        
        String rawLoId = losPos.getId().trim();
        String uniqueId = moduleId + "_" + rawLoId;
        if (losPosRepository.existsById(uniqueId)) {
            throw new Exception("Learning Outcome with ID " + rawLoId + " already exists for this module.");
        }
        
        losPos.setId(uniqueId);
        losPos.setLoId(rawLoId);
        losPos.setModuleCode(moduleId);
        losPos.setModule(moduleOptional.get());
        return losPosRepository.save(losPos);
    }

    // Read All LosPos by Module ID
    public List<LosPos> getLosPosByModuleId(String moduleId) {
        return losPosRepository.findByModule_ModuleId(moduleId);
    }

    // Read One LosPos
    public Optional<LosPos> getLosPosById(String id) {
        return losPosRepository.findById(id);
    }

    // Update LosPos (Lecture)
    public LosPos updateLosPos(String id, LosPos losPosDetails) throws Exception {
        LosPos losPos = losPosRepository.findById(id)
                .orElseThrow(() -> new Exception("LosPos not found"));
        
        losPos.setName(losPosDetails.getName());
        losPos.setLoId(losPosDetails.getLoId());
        losPos.setLoDescription(losPosDetails.getLoDescription());
        losPos.setModuleCode(losPosDetails.getModuleCode());
        return losPosRepository.save(losPos);
    }

    // Delete LosPos (Lecture)
    public void deleteLosPos(String id) throws Exception {
        if (!losPosRepository.existsById(id)) {
            throw new Exception("LosPos not found");
        }
        losPosRepository.deleteById(id);
    }
}