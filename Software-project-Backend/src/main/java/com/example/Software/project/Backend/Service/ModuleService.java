package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Los;
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

    @Autowired
    private NotificationService notificationService;

    // Create (Admin)
    // Create (Admin)
    public Module createModule(Module module) throws Exception {
        return createModule(module, null);
    }

    public Module createModule(Module module, String createdBy) throws Exception {
        if (moduleRepository.existsById(module.getModuleId())) {
            throw new Exception("Module ID already exists");
        }
        module.setAssignedLecturers(resolveLecturers(module.getAssignedLecturerUsernamesInput()));
        Module saved = moduleRepository.save(module);
        try {
            String text = "Created module: " + saved.getModuleId() + " - " + saved.getModuleName();
            java.util.Set<String> who = new java.util.LinkedHashSet<>(saved.getAssignedLecturerUsernames());
            if (createdBy != null) who.add(createdBy);
            notificationService.notifyUsers(who, text);
        } catch (Exception ignored) { }
        return saved;
    }

    // Read All
    public List<Module> getAllModules() {
        return moduleRepository.findByIsDeletedFalse();
    }

    // Read One
    public Optional<Module> getModuleById(String id) {
        return moduleRepository.findByModuleIdAndIsDeletedFalse(id);
    }

    // Read All visible to a lecturer: unassigned modules stay visible to everyone
    // (so modules created before this feature existed don't suddenly disappear);
    // assigning at least one lecturer scopes that module to just them.
    public List<Module> getModulesForLecturer(String username) {
        return moduleRepository.findByIsDeletedFalse().stream()
                .filter(m -> m.getAssignedLecturers() == null || m.getAssignedLecturers().isEmpty()
                        || m.getAssignedLecturers().stream().anyMatch(u -> u.getUserID().equals(username)))
                .collect(Collectors.toList());
    }

    // Module IDs this lecturer is explicitly assigned to (not the "also sees open
    // modules" superset from getModulesForLecturer - used to pre-fill the admin's
    // per-lecturer module picker with exactly what's actually assigned).
    public List<String> getModuleIdsAssignedTo(String username) {
        return moduleRepository.findByIsDeletedFalse().stream()
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
        Module module = moduleRepository.findByModuleIdAndIsDeletedFalse(id)
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

    // Delete (Admin) — soft delete: preserves the module and everything under it as
    // accreditation evidence. See LosService.deleteLos for the LO-level cascade.
    @Transactional
    public void deleteModule(String id, String deletedBy) throws Exception {
        Module module = moduleRepository.findByModuleIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new Exception("Module not found or already deleted"));

        // 1. Soft-delete CqiAction records tied to this module directly (module-level, not LO-level —
        //    LO-linked ones are handled by the per-LO cascade below).
        try {
            jdbcTemplate.update(
                "UPDATE cqi_action SET is_deleted = 1, deleted_at = NOW(), deleted_by = ? WHERE module_id = ? AND is_deleted = 0",
                deletedBy, id);
        } catch (Exception ignored) {}

        // 2. Soft-delete AssessmentTemplates under this module
        try {
            jdbcTemplate.update(
                "UPDATE assessment_template SET is_deleted = 1, deleted_at = NOW(), deleted_by = ? WHERE module_id = ? AND is_deleted = 0",
                deletedBy, id);
        } catch (Exception ignored) {}

        // 3. Soft-delete each active LO under this module
        for (Los los : losRepository.findByModule_ModuleIdAndIsDeletedFalse(id)) {
            losService.deleteLos(los.getId(), deletedBy);
        }

        // 4. Soft-delete the module itself
        module.softDelete(deletedBy);
        moduleRepository.save(module);
    }

    // Restore (Admin) — reverses deleteModule, including its LO/template/CQI cascade
    @Transactional
    public void restoreModule(String id) throws Exception {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new Exception("Module not found"));
        if (!Boolean.TRUE.equals(module.getIsDeleted())) {
            throw new Exception("Module is not deleted");
        }

        module.restore();
        moduleRepository.save(module);

        for (Los los : losRepository.findByModule_ModuleIdAndIsDeletedTrue(id)) {
            losService.restoreLos(los.getId());
        }

        try {
            jdbcTemplate.update(
                "UPDATE assessment_template SET is_deleted = 0, deleted_at = NULL, deleted_by = NULL WHERE module_id = ?",
                id);
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.update(
                "UPDATE cqi_action SET is_deleted = 0, deleted_at = NULL, deleted_by = NULL WHERE module_id = ?",
                id);
        } catch (Exception ignored) {}
    }

    public List<Module> getDeletedModules() {
        return moduleRepository.findByIsDeletedTrue();
    }
}
