package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.Los;
import com.example.Software.project.Backend.Model.OutcomeMapping;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.LosService;
import com.example.Software.project.Backend.Service.LOPOMappingService;
import com.example.Software.project.Backend.Service.ProgramOutcomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/los-with-mapping")
public class IntegratedLORestController {

    @Autowired
    private LosService losService;

    @Autowired
    private LOPOMappingService mappingService;

    @Autowired
    private ProgramOutcomeService programOutcomeService;

    @Autowired
    private JwtUtil jwtUtil;

    // Helper methods
    private boolean isLecturer(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) return false;
            String jwt = token.substring(7);
            String userRole = jwtUtil.extractRole(jwt);
            return "lecture".equalsIgnoreCase(userRole) || isAdmin(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAdmin(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) return false;
            String jwt = token.substring(7);
            String userRole = jwtUtil.extractRole(jwt);
            return "admin".equalsIgnoreCase(userRole) || "superadmin".equalsIgnoreCase(userRole);
        } catch (Exception e) {
            return false;
        }
    }

    private String extractUsername(String token) {
        try {
            String jwt = token.substring(7);
            return jwtUtil.extractUsername(jwt);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ERROR");
        response.put("message", message);
        return response;
    }

    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    /**
     * Create Learning Outcome with PO mappings in one transaction
     */
    @PostMapping("/create")
    public ResponseEntity<?> createLOWithMappings(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            String username = extractUsername(token);

            // Extract LO data
            @SuppressWarnings("unchecked")
            Map<String, Object> loData = (Map<String, Object>) request.get("learningOutcome");
            
            Los los = new Los();
            los.setId((String) loData.get("id"));
            los.setName((String) loData.get("name"));
            los.setDescription((String) loData.get("description"));
            los.setBatch((String) loData.get("batch"));
            los.setCreatedBy(username);

            String moduleId = (String) request.get("moduleId");

            // Create Learning Outcome
            Los createdLO = losService.addLosToModule(moduleId, los);

            // Extract mapping data
            @SuppressWarnings("unchecked")
            Map<String, Integer> mappings = (Map<String, Integer>) request.get("mappings");
            String mappingRemarks = (String) request.getOrDefault("mappingRemarks", "");

            List<OutcomeMapping> createdMappings = null;
            if (mappings != null && !mappings.isEmpty()) {
                // Create PO mappings
                createdMappings = mappingService.createMappings(createdLO.getId(), mappings, username, mappingRemarks);
            }

            // Prepare response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("learningOutcome", createdLO);
            responseData.put("mappings", createdMappings);
            responseData.put("mappingCount", createdMappings != null ? createdMappings.size() : 0);

            return ResponseEntity.ok(createSuccessResponse("Learning Outcome created with mappings successfully", responseData));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error creating Learning Outcome: " + e.getMessage()));
        }
    }

    /**
     * Get LO creation form data (suggestions based on module)
     */
    @GetMapping("/form-data/{moduleId}")
    public ResponseEntity<?> getCreateFormData(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            // Get mapping suggestions for this module type
            Map<String, Integer> suggestions = mappingService.getSuggestedMappings(moduleId, null);

            // Get all available Program Outcomes
            List<com.example.Software.project.Backend.Model.ProgramOutcome> allPOs = programOutcomeService.getAllActivePOs();

            Map<String, Object> formData = new HashMap<>();
            formData.put("suggestedMappings", suggestions);
            formData.put("allProgramOutcomes", allPOs);
            formData.put("moduleId", moduleId);

            return ResponseEntity.ok(createSuccessResponse("Form data retrieved", formData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error retrieving form data: " + e.getMessage()));
        }
    }

    /**
     * Get comprehensive LO details with mappings
     */
    @GetMapping("/{loId}/details")
    public ResponseEntity<?> getLOWithMappings(@PathVariable String loId, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            // Get Learning Outcome
            Los los = losService.getLosById(loId)
                .orElseThrow(() -> new RuntimeException("Learning Outcome not found: " + loId));

            // Get mappings
            List<OutcomeMapping> mappings = mappingService.getMappingsForLO(loId);

            // Get mapping statistics
            Map<String, Object> stats = mappingService.getMappingStatistics(los.getModule().getModuleId());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("learningOutcome", los);
            responseData.put("mappings", mappings);
            responseData.put("statistics", stats);

            return ResponseEntity.ok(createSuccessResponse("LO details with mappings retrieved", responseData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error retrieving LO details: " + e.getMessage()));
        }
    }

    /**
     * Update LO mappings (if not approved)
     */
    @PutMapping("/{loId}/mappings")
    public ResponseEntity<?> updateLOMappings(
            @PathVariable String loId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            String username = extractUsername(token);

            // Get current mappings
            List<OutcomeMapping> currentMappings = mappingService.getMappingsForLO(loId);

            // Check if any are approved (cannot update approved mappings)
            boolean hasApproved = currentMappings.stream()
                .anyMatch(mapping -> mapping.getStatus() == OutcomeMapping.ApprovalStatus.APPROVED);

            if (hasApproved) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse("Cannot update mappings that have been approved"));
            }

            // Delete existing pending/rejected mappings
            for (OutcomeMapping mapping : currentMappings) {
                if (mapping.getStatus() != OutcomeMapping.ApprovalStatus.APPROVED) {
                    mappingService.deleteMapping(mapping.getId());
                }
            }

            // Create new mappings
            @SuppressWarnings("unchecked")
            Map<String, Integer> newMappings = (Map<String, Integer>) request.get("mappings");
            String mappingRemarks = (String) request.getOrDefault("mappingRemarks", "Updated mappings");

            List<OutcomeMapping> createdMappings = mappingService.createMappings(loId, newMappings, username, mappingRemarks);

            return ResponseEntity.ok(createSuccessResponse("LO mappings updated successfully", createdMappings));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error updating mappings: " + e.getMessage()));
        }
    }

    /**
     * Get module overview with all LOs and their mapping status
     */
    @GetMapping("/module/{moduleId}/overview")
    public ResponseEntity<?> getModuleOverview(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            // Get all LOs for the module
            List<Los> los = losService.getLosByModuleId(moduleId);

            // Get mapping statistics
            Map<String, Object> mappingStats = mappingService.getMappingStatistics(moduleId);

            // Get comprehensive report
            Map<String, Object> report = mappingService.getModuleMappingReport(moduleId);

            Map<String, Object> overview = new HashMap<>();
            overview.put("moduleId", moduleId);
            overview.put("learningOutcomes", los);
            overview.put("totalLOs", los.size());
            overview.put("mappingStatistics", mappingStats);
            overview.put("detailedReport", report);

            return ResponseEntity.ok(createSuccessResponse("Module overview retrieved", overview));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error retrieving module overview: " + e.getMessage()));
        }
    }
}