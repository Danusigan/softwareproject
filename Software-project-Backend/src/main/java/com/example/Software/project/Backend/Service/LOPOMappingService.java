package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.OutcomeMapping;
import com.example.Software.project.Backend.Model.ProgramOutcome;
import com.example.Software.project.Backend.Model.Los;
import com.example.Software.project.Backend.Repository.OutcomeMappingRepository;
import com.example.Software.project.Backend.Repository.ProgramOutcomeRepository;
import com.example.Software.project.Backend.Repository.LosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LOPOMappingService {

    @Autowired
    private OutcomeMappingRepository mappingRepository;

    @Autowired
    private ProgramOutcomeRepository poRepository;

    @Autowired
    private LosRepository losRepository;

    // Module-based mapping templates for smart suggestions
    private final Map<String, Map<String, Integer>> MODULE_TEMPLATES = new HashMap<String, Map<String, Integer>>() {{
        // Software Engineering Modules
        put("SOFTWARE_ENGINEERING", Map.of(
            "PO1", 2, "PO3", 3, "PO5", 3, "PO8", 1, "PO12", 2
        ));
        put("PROGRAMMING", Map.of(
            "PO1", 3, "PO3", 2, "PO5", 3, "PO12", 2
        ));
        put("DATABASE", Map.of(
            "PO1", 2, "PO2", 2, "PO3", 3, "PO5", 2
        ));
        
        // Mathematics Modules
        put("MATHEMATICS", Map.of(
            "PO1", 3, "PO2", 3, "PO4", 1, "PO12", 2
        ));
        put("STATISTICS", Map.of(
            "PO1", 3, "PO2", 3, "PO4", 2, "PO12", 1
        ));
        
        // Engineering Core
        put("ENGINEERING_MECHANICS", Map.of(
            "PO1", 3, "PO2", 2, "PO4", 2
        ));
        put("ELECTRONICS", Map.of(
            "PO1", 3, "PO3", 2, "PO5", 3
        ));
        
        // Project/Management
        put("PROJECT_MANAGEMENT", Map.of(
            "PO9", 3, "PO10", 3, "PO11", 3, "PO6", 2, "PO12", 2
        ));
        put("CAPSTONE_PROJECT", Map.of(
            "PO3", 3, "PO6", 2, "PO7", 2, "PO8", 2, "PO9", 3, "PO10", 3, "PO11", 3, "PO12", 2
        ));
        
        // Communication/Liberal Arts
        put("COMMUNICATION", Map.of(
            "PO10", 3, "PO6", 2, "PO8", 1, "PO12", 2
        ));
        put("ETHICS", Map.of(
            "PO8", 3, "PO6", 2, "PO7", 2, "PO12", 1
        ));
        
        // Research/Investigation
        put("RESEARCH_METHODS", Map.of(
            "PO4", 3, "PO2", 2, "PO10", 2, "PO12", 3
        ));
    }};

    /**
     * Get suggested PO mappings based on module type and LO description
     */
    public Map<String, Integer> getSuggestedMappings(String moduleId, String loDescription) {
        // Get module type based on module name/ID
        String moduleType = determineModuleType(moduleId);
        
        // Get base template
        Map<String, Integer> suggestions = new HashMap<>(MODULE_TEMPLATES.getOrDefault(moduleType, new HashMap<>()));
        
        // Enhance suggestions based on LO description keywords
        enhanceSuggestionsBasedOnDescription(suggestions, loDescription);
        
        // Ensure we have 2-5 meaningful mappings
        return validateAndAdjustSuggestions(suggestions);
    }

    /**
     * Determine module type from module ID/name
     */
    private String determineModuleType(String moduleId) {
        String id = moduleId.toUpperCase();
        
        if (id.contains("SE") || id.contains("SOFT")) return "SOFTWARE_ENGINEERING";
        if (id.contains("PROG") || id.contains("CS")) return "PROGRAMMING";
        if (id.contains("DB") || id.contains("DATA")) return "DATABASE";
        if (id.contains("MATH") || id.contains("MA")) return "MATHEMATICS";
        if (id.contains("STAT") || id.contains("ST")) return "STATISTICS";
        if (id.contains("MECH") || id.contains("ME")) return "ENGINEERING_MECHANICS";
        if (id.contains("EE") || id.contains("ELEC")) return "ELECTRONICS";
        if (id.contains("PM") || id.contains("MGT")) return "PROJECT_MANAGEMENT";
        if (id.contains("FYP") || id.contains("CAP")) return "CAPSTONE_PROJECT";
        if (id.contains("COM") || id.contains("ENG")) return "COMMUNICATION";
        if (id.contains("ETH") || id.contains("PHI")) return "ETHICS";
        if (id.contains("RES") || id.contains("THE")) return "RESEARCH_METHODS";
        
        return "SOFTWARE_ENGINEERING"; // Default
    }

    /**
     * Enhance suggestions based on LO description keywords
     */
    private void enhanceSuggestionsBasedOnDescription(Map<String, Integer> suggestions, String description) {
        if (description == null) return;
        
        String desc = description.toLowerCase();
        
        // Design-related keywords
        if (desc.contains("design") || desc.contains("create") || desc.contains("develop")) {
            suggestions.put("PO3", Math.max(suggestions.getOrDefault("PO3", 0), 3));
        }
        
        // Analysis keywords
        if (desc.contains("analyz") || desc.contains("evaluat") || desc.contains("assess")) {
            suggestions.put("PO2", Math.max(suggestions.getOrDefault("PO2", 0), 3));
        }
        
        // Investigation keywords
        if (desc.contains("research") || desc.contains("investigat") || desc.contains("experiment")) {
            suggestions.put("PO4", Math.max(suggestions.getOrDefault("PO4", 0), 3));
        }
        
        // Tools keywords
        if (desc.contains("tool") || desc.contains("software") || desc.contains("technology")) {
            suggestions.put("PO5", Math.max(suggestions.getOrDefault("PO5", 0), 2));
        }
        
        // Communication keywords
        if (desc.contains("present") || desc.contains("report") || desc.contains("communicat")) {
            suggestions.put("PO10", Math.max(suggestions.getOrDefault("PO10", 0), 2));
        }
        
        // Teamwork keywords
        if (desc.contains("team") || desc.contains("group") || desc.contains("collaborat")) {
            suggestions.put("PO9", Math.max(suggestions.getOrDefault("PO9", 0), 2));
        }
        
        // Ethics keywords
        if (desc.contains("ethic") || desc.contains("responsible") || desc.contains("professional")) {
            suggestions.put("PO8", Math.max(suggestions.getOrDefault("PO8", 0), 2));
        }
        
        // Society keywords
        if (desc.contains("society") || desc.contains("social") || desc.contains("impact")) {
            suggestions.put("PO6", Math.max(suggestions.getOrDefault("PO6", 0), 2));
        }
    }

    /**
     * Validate and adjust suggestions to ensure 2-5 meaningful mappings
     */
    private Map<String, Integer> validateAndAdjustSuggestions(Map<String, Integer> suggestions) {
        // Remove zero weights
        suggestions.entrySet().removeIf(entry -> entry.getValue() <= 0);
        
        // If too few suggestions, add some defaults
        if (suggestions.size() < 2) {
            suggestions.put("PO1", 2); // Engineering Knowledge
            suggestions.put("PO12", 1); // Lifelong Learning
        }
        
        // If too many suggestions, keep only top 5
        if (suggestions.size() > 5) {
            return suggestions.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
        }
        
        return suggestions;
    }

    /**
     * Create LO-PO mappings with validation
     */
    public List<OutcomeMapping> createMappings(String loId, Map<String, Integer> mappings, String mappedBy, String remarks) {
        Los los = losRepository.findById(loId)
            .orElseThrow(() -> new RuntimeException("Learning Outcome not found: " + loId));
        
        // Validate mappings
        validateMappings(mappings);
        
        List<OutcomeMapping> savedMappings = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : mappings.entrySet()) {
            String poId = entry.getKey();
            Integer weight = entry.getValue();
            
            if (weight > 0) { // Only create mappings with positive weight
                ProgramOutcome po = poRepository.findById(poId)
                    .orElseThrow(() -> new RuntimeException("Program Outcome not found: " + poId));
                
                OutcomeMapping mapping = new OutcomeMapping();
                mapping.setLearningOutcome(los);
                mapping.setProgramOutcome(po);
                mapping.setWeight(weight);
                mapping.setMappedBy(mappedBy);
                mapping.setLecturerRemarks(remarks);
                mapping.setStatus(OutcomeMapping.ApprovalStatus.PENDING);
                
                savedMappings.add(mappingRepository.save(mapping));
            }
        }
        
        return savedMappings;
    }

    /**
     * Validate mapping rules
     */
    private void validateMappings(Map<String, Integer> mappings) {
        // Remove zero/negative weights
        mappings.entrySet().removeIf(entry -> entry.getValue() <= 0);
        
        // Must have at least 2 mappings
        if (mappings.size() < 2) {
            throw new IllegalArgumentException("Learning Outcome must map to at least 2 Program Outcomes");
        }
        
        // Must not exceed 5 mappings
        if (mappings.size() > 5) {
            throw new IllegalArgumentException("Learning Outcome cannot map to more than 5 Program Outcomes");
        }
        
        // Must have at least one mapping with weight 3 (primary focus)
        boolean hasPrimaryFocus = mappings.values().stream().anyMatch(weight -> weight >= 3);
        if (!hasPrimaryFocus) {
            throw new IllegalArgumentException("Learning Outcome must have at least one primary focus (weight 3) mapping");
        }
        
        // Validate weight range
        mappings.values().forEach(weight -> {
            if (weight < 1 || weight > 3) {
                throw new IllegalArgumentException("Mapping weight must be between 1 and 3");
            }
        });
    }

    /**
     * Get mappings for a specific LO
     */
    public List<OutcomeMapping> getMappingsForLO(String loId) {
        return mappingRepository.findByLearningOutcome_Id(loId);
    }

    /**
     * Get all mappings for a module
     */
    public List<OutcomeMapping> getMappingsForModule(String moduleId) {
        return mappingRepository.findByLearningOutcome_Module_ModuleId(moduleId);
    }

    /**
     * Get pending mappings for admin review
     */
    public List<OutcomeMapping> getPendingMappings() {
        return mappingRepository.findByStatus(OutcomeMapping.ApprovalStatus.PENDING);
    }

    /**
     * Approve mapping (Admin only)
     */
    public OutcomeMapping approveMapping(Long mappingId, String reviewedBy, String adminRemarks) {
        OutcomeMapping mapping = mappingRepository.findById(mappingId)
            .orElseThrow(() -> new RuntimeException("Mapping not found: " + mappingId));
        
        mapping.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        mapping.setReviewedBy(reviewedBy);
        mapping.setAdminRemarks(adminRemarks);
        mapping.setReviewedAt(LocalDateTime.now());
        
        return mappingRepository.save(mapping);
    }

    /**
     * Reject mapping (Admin only)
     */
    public OutcomeMapping rejectMapping(Long mappingId, String reviewedBy, String adminRemarks) {
        OutcomeMapping mapping = mappingRepository.findById(mappingId)
            .orElseThrow(() -> new RuntimeException("Mapping not found: " + mappingId));
        
        mapping.setStatus(OutcomeMapping.ApprovalStatus.REJECTED);
        mapping.setReviewedBy(reviewedBy);
        mapping.setAdminRemarks(adminRemarks);
        mapping.setReviewedAt(LocalDateTime.now());
        
        return mappingRepository.save(mapping);
    }

    /**
     * Bulk approve mappings for an LO (Admin only)
     */
    public List<OutcomeMapping> bulkApproveMappingsForLO(String loId, String reviewedBy, String adminRemarks) {
        List<OutcomeMapping> mappings = getMappingsForLO(loId);
        List<OutcomeMapping> pendingMappings = mappings.stream()
            .filter(mapping -> mapping.getStatus() == OutcomeMapping.ApprovalStatus.PENDING)
            .toList();
        
        for (OutcomeMapping mapping : pendingMappings) {
            mapping.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);
            mapping.setReviewedBy(reviewedBy);
            mapping.setAdminRemarks(adminRemarks);
            mapping.setReviewedAt(LocalDateTime.now());
            mappingRepository.save(mapping);
        }
        
        return pendingMappings;
    }

    /**
     * Update mapping (Lecturer only, if not yet approved)
     */
    public OutcomeMapping updateMapping(Long mappingId, Integer newWeight, String lecturerRemarks) {
        OutcomeMapping mapping = mappingRepository.findById(mappingId)
            .orElseThrow(() -> new RuntimeException("Mapping not found: " + mappingId));
        
        if (mapping.getStatus() == OutcomeMapping.ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Cannot update approved mappings");
        }
        
        mapping.setWeight(newWeight);
        mapping.setLecturerRemarks(lecturerRemarks);
        mapping.setUpdatedAt(LocalDateTime.now());
        
        return mappingRepository.save(mapping);
    }

    /**
     * Delete mapping (Lecturer only, if not yet approved)
     */
    public void deleteMapping(Long mappingId) {
        OutcomeMapping mapping = mappingRepository.findById(mappingId)
            .orElseThrow(() -> new RuntimeException("Mapping not found: " + mappingId));
        
        if (mapping.getStatus() == OutcomeMapping.ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Cannot delete approved mappings");
        }
        
        mappingRepository.delete(mapping);
    }

    /**
     * Get mapping statistics for a module
     */
    public Map<String, Object> getMappingStatistics(String moduleId) {
        List<OutcomeMapping> mappings = getMappingsForModule(moduleId);
        
        long totalMappings = mappings.size();
        long pendingMappings = mappings.stream().filter(m -> m.getStatus() == OutcomeMapping.ApprovalStatus.PENDING).count();
        long approvedMappings = mappings.stream().filter(m -> m.getStatus() == OutcomeMapping.ApprovalStatus.APPROVED).count();
        long rejectedMappings = mappings.stream().filter(m -> m.getStatus() == OutcomeMapping.ApprovalStatus.REJECTED).count();
        
        // PO coverage analysis
        Set<String> coveredPOs = mappings.stream()
            .filter(m -> m.getStatus() == OutcomeMapping.ApprovalStatus.APPROVED)
            .map(m -> m.getProgramOutcome().getPoId())
            .collect(Collectors.toSet());
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMappings", totalMappings);
        stats.put("pendingMappings", pendingMappings);
        stats.put("approvedMappings", approvedMappings);
        stats.put("rejectedMappings", rejectedMappings);
        stats.put("coveredPOs", coveredPOs.size());
        stats.put("coveredPOsList", coveredPOs);
        stats.put("mappingCompleteness", coveredPOs.size() * 100.0 / 12); // Percentage of 12 POs covered
        
        return stats;
    }

    /**
     * Get comprehensive mapping report for a module
     */
    public Map<String, Object> getModuleMappingReport(String moduleId) {
        Map<String, Object> report = new HashMap<>();
        
        // Basic statistics
        report.put("statistics", getMappingStatistics(moduleId));
        
        // All mappings grouped by LO
        List<OutcomeMapping> allMappings = getMappingsForModule(moduleId);
        Map<String, List<OutcomeMapping>> mappingsByLO = allMappings.stream()
            .collect(Collectors.groupingBy(mapping -> mapping.getLearningOutcome().getId()));
        
        report.put("mappingsByLO", mappingsByLO);
        
        // PO coverage matrix
        Map<String, Map<String, Integer>> poMatrix = new HashMap<>();
        for (OutcomeMapping mapping : allMappings) {
            if (mapping.getStatus() == OutcomeMapping.ApprovalStatus.APPROVED) {
                String loId = mapping.getLearningOutcome().getId();
                String poId = mapping.getProgramOutcome().getPoId();
                
                poMatrix.computeIfAbsent(loId, k -> new HashMap<>()).put(poId, mapping.getWeight());
            }
        }
        
        report.put("poMatrix", poMatrix);
        
        return report;
    }

    /**
     * Get all mappings with optional filtering
     */
    public List<OutcomeMapping> getAllMappings(String moduleId, String status, String batch, String search) {
        List<OutcomeMapping> mappings = mappingRepository.findAll();
        
        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            mappings = mappings.stream()
                .filter(m -> m.getStatus().toString().equalsIgnoreCase(status))
                .collect(Collectors.toList());
        }
        
        // Filter by module if provided
        if (moduleId != null && !moduleId.isEmpty()) {
            mappings = mappings.stream()
                .filter(m -> m.getLearningOutcome().getModule().getModuleId().equals(moduleId))
                .collect(Collectors.toList());
        }
        
        // Filter by batch if provided
        if (batch != null && !batch.isEmpty()) {
            mappings = mappings.stream()
                .filter(m -> m.getLearningOutcome().getBatch() != null && 
                            m.getLearningOutcome().getBatch().equals(batch))
                .collect(Collectors.toList());
        }
        
        // Search by LO name if provided
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            mappings = mappings.stream()
                .filter(m -> m.getLearningOutcome().getName().toLowerCase().contains(searchLower) ||
                            m.getLearningOutcome().getId().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
        }
        
        return mappings;
    }

    /**
     * Get total mappings count
     */
    public long getTotalMappingsCount() {
        return mappingRepository.count();
    }

    /**
     * Get pending mappings count
     */
    public long getPendingMappingsCount() {
        return mappingRepository.countByStatus(OutcomeMapping.ApprovalStatus.PENDING);
    }

    /**
     * Get approved mappings count
     */
    public long getApprovedMappingsCount() {
        return mappingRepository.countByStatus(OutcomeMapping.ApprovalStatus.APPROVED);
    }

    /**
     * Calculate coverage percentage (approved POs / total POs)
     */
    public double calculateCoveragePercentage() {
        long totalPOs = poRepository.count();
        if (totalPOs == 0) return 0.0;
        
        List<OutcomeMapping> approvedMappings = mappingRepository.findByStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        Set<String> uniquePOs = approvedMappings.stream()
            .map(m -> m.getProgramOutcome().getPoId())
            .collect(Collectors.toSet());
        
        return (uniquePOs.size() * 100.0) / totalPOs;
    }
}