package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ModuleService {

    @Autowired
    private ModuleRepository moduleRepository;

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
    public void deleteModule(String id) throws Exception {
        if (!moduleRepository.existsById(id)) {
            throw new Exception("Module not found");
        }
        moduleRepository.deleteById(id);
    }
}