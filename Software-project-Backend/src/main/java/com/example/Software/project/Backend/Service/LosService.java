package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Los;
import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Repository.LosRepository;
import com.example.Software.project.Backend.Repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LosService {

    @Autowired
    private LosRepository losRepository;

    @Autowired
    private ModuleRepository moduleRepository;

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
        return losRepository.save(los);
    }

    // Delete Los (Lecture)
    public void deleteLos(String id) throws Exception {
        if (!losRepository.existsById(id)) {
            throw new Exception("Los not found");
        }
        losRepository.deleteById(id);
    }
}
