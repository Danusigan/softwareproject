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