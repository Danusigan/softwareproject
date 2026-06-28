package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Model.User;
import com.example.Software.project.Backend.Repository.AssessmentTemplateRepository;
import com.example.Software.project.Backend.Repository.LosRepository;
import com.example.Software.project.Backend.Repository.ModuleRepository;
import com.example.Software.project.Backend.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Create (Admin)
    public Module createModule(Module module) throws Exception {
        if (moduleRepository.existsById(module.getModuleId())) {
            throw new Exception("Module ID already exists");
        }
        module.setAssignedLecturers(resolveLecturers(module.getAssignedLecturerUsernamesInput()));
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

    // Read All visible to a lecturer: unassigned modules stay visible to everyone
    // (so modules created before this feature existed don't suddenly disappear);
    // assigning at least one lecturer scopes that module to just them.
    public List<Module> getModulesForLecturer(String username) {
        return moduleRepository.findAll().stream()
                .filter(m -> m.getAssignedLecturers() == null || m.getAssignedLecturers().isEmpty()
                        || m.getAssignedLecturers().stream().anyMatch(u -> u.getUserID().equals(username)))
                .collect(Collectors.toList());
    }

    // Module IDs this lecturer is explicitly assigned to (not the "also sees open
    // modules" superset from getModulesForLecturer - used to pre-fill the admin's
    // per-lecturer module picker with exactly what's actually assigned).
    public List<String> getModuleIdsAssignedTo(String username) {
        return moduleRepository.findAll().stream()
                .filter(m -> m.getAssignedLecturers() != null
                        && m.getAssignedLecturers().stream().anyMatch(u -> u.getUserID().equals(username)))
                .map(Module::getModuleId)
                .collect(Collectors.toList());
    }

    // Reverse-direction assignment: from a lecturer's record, set exactly which modules
    // they're assigned to. Diffs against current state so other lecturers already on
    // those modules are left untouched.
    public void setModulesForLecturer(String username, List<String> moduleIds) throws Exception {
        User lecturer = userRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("Lecturer not found: " + username));
        if (!"lecture".equalsIgnoreCase(lecturer.getUsertype())) {
            throw new Exception(username + " is not a lecturer");
        }
        List<String> targetIds = moduleIds == null ? new ArrayList<>() : moduleIds;
        for (Module module : moduleRepository.findAll()) {
            List<User> current = module.getAssignedLecturers() == null ? new ArrayList<>() : new ArrayList<>(module.getAssignedLecturers());
            boolean isCurrentlyAssigned = current.stream().anyMatch(u -> u.getUserID().equals(username));
            boolean shouldBeAssigned = targetIds.contains(module.getModuleId());
            if (shouldBeAssigned && !isCurrentlyAssigned) {
                current.add(lecturer);
                module.setAssignedLecturers(current);
                moduleRepository.save(module);
            } else if (!shouldBeAssigned && isCurrentlyAssigned) {
                current.removeIf(u -> u.getUserID().equals(username));
                module.setAssignedLecturers(current);
                moduleRepository.save(module);
            }
        }
    }

    // Used when deleting a lecturer: drop their join-table rows first so the FK on
    // module_lecturers.lecturer_username doesn't block the user delete.
    public void removeLecturerFromAllModules(String username) {
        for (Module module : moduleRepository.findAll()) {
            List<User> current = module.getAssignedLecturers();
            if (current != null && current.stream().anyMatch(u -> u.getUserID().equals(username))) {
                List<User> updated = new ArrayList<>(current);
                updated.removeIf(u -> u.getUserID().equals(username));
                module.setAssignedLecturers(updated);
                moduleRepository.save(module);
            }
        }
    }

    // Resolves submitted lecturer usernames into User entities for the ManyToMany relation.
    private List<User> resolveLecturers(List<String> usernames) throws Exception {
        if (usernames == null || usernames.isEmpty()) {
            return new ArrayList<>();
        }
        List<User> lecturers = new ArrayList<>();
        for (String username : usernames) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new Exception("Lecturer not found: " + username));
            if (!"lecture".equalsIgnoreCase(user.getUsertype())) {
                throw new Exception(username + " is not a lecturer");
            }
            lecturers.add(user);
        }
        return lecturers;
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
        // Only touch assignment if the request actually included it, so the "view modules"
        // edit form (which never sends this field) doesn't wipe out existing assignments.
        if (moduleDetails.getAssignedLecturerUsernamesInput() != null) {
            module.setAssignedLecturers(resolveLecturers(moduleDetails.getAssignedLecturerUsernamesInput()));
        }
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
