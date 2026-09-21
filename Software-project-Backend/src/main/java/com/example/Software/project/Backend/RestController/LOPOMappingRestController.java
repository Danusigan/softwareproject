package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.OutcomeMapping;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.AuditLogService;
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
@RequestMapping("/api/lo-po-mapping")
public class LOPOMappingRestController {

    @Autowired
    private LOPOMappingService mappingService;

    @Autowired
    private ProgramOutcomeService poService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuditLogService auditLogService;

    private String normalizeRole(String role) {
        if (role == null) return "";
        return role.trim().toLowerCase().replace("-", "").replace(" ", "");
    }

    // Helper methods for authentication
    private boolean isLecturer(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) return false;
            String jwt = token.substring(7);
            String userRole = normalizeRole(jwtUtil.extractRole(jwt));
            return "lecture".equals(userRole)
                || "lecturer".equals(userRole)
                || isAdmin(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAdmin(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) return false;
            String jwt = token.substring(7);
            String userRole = normalizeRole(jwtUtil.extractRole(jwt));
            return "admin".equals(userRole) || "superadmin".equals(userRole);
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

    // === PUBLIC ENDPOINTS ===

    // Get all LO-PO mappings
    @GetMapping("/all")
    public ResponseEntity<?> getAllMappings(
            @RequestParam(required = false) String moduleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) String search,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            List<OutcomeMapping> mappings = mappingService.getAllMappings(moduleId, status, batch, search);
            return ResponseEntity.ok(createSuccessResponse("Mappings retrieved successfully", mappings));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error retrieving mappings: " + e.getMessage()));
        }
    }

    // Get LO-PO mapping statistics
    @GetMapping("/statistics")
    public ResponseEntity<?> getMappingStatistics(@RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalMappings", mappingService.getTotalMappingsCount());
            statistics.put("pendingMappings", mappingService.getPendingMappingsCount());
            statistics.put("approvedMappings", mappingService.getApprovedMappingsCount());
            statistics.put("coveragePercentage", mappingService.calculateCoveragePercentage());

            return ResponseEntity.ok(createSuccessResponse("Mapping statistics retrieved", statistics));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error retrieving statistics: " + e.getMessage()));
        }
    }

    // === LECTURER ENDPOINTS ===

    // Get mapping suggestions for an LO
    @GetMapping("/suggestions")
    public ResponseEntity<?> getMappingSuggestions(
            @RequestParam String moduleId,
            @RequestParam(required = false) String loDescription,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            Map<String, Integer> suggestions = mappingService.getSuggestedMappings(moduleId, loDescription);
            
            // Enrich with PO details
            Map<String, Object> enrichedSuggestions = new HashMap<>();
            for (Map.Entry<String, Integer> entry : suggestions.entrySet()) {
                String poId = entry.getKey();
                Integer weight = entry.getValue();
                
                Map<String, Object> poInfo = new HashMap<>();
                poService.getPOById(poId).ifPresent(po -> {
                    poInfo.put("poId", po.getPoId());
                    poInfo.put("code", po.getCode());
                    poInfo.put("title", po.getTitle());
                    poInfo.put("category", po.getCategory());
                    poInfo.put("suggestedWeight", weight);
                });
                
                if (!poInfo.isEmpty()) {
                    enrichedSuggestions.put(poId, poInfo);
                }
            }

            return ResponseEntity.ok(createSuccessResponse("Mapping suggestions generated", enrichedSuggestions));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error generating suggestions: " + e.getMessage()));
        }
    }

    // Create mappings for an LO
    @PostMapping("/create")
    public ResponseEntity<?> createMappings(
            @RequestParam String loId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            String username = extractUsername(token);
            String remarks = (String) request.getOrDefault("remarks", "");
            
            @SuppressWarnings("unchecked")
            Map<String, Integer> mappings = (Map<String, Integer>) request.get("mappings");

            List<OutcomeMapping> createdMappings = mappingService.createMappings(loId, mappings, username, remarks);
            return ResponseEntity.ok(createSuccessResponse("Mappings created successfully", createdMappings));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error creating mappings: " + e.getMessage()));
        }
    }

    // Get mappings for an LO
    @GetMapping("/lo/{loId}")
    public ResponseEntity<?> getMappingsForLO(@PathVariable String loId, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            List<OutcomeMapping> mappings = mappingService.getMappingsForLO(loId);
            return ResponseEntity.ok(createSuccessResponse("Mappings retrieved", mappings));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error retrieving mappings: " + e.getMessage()));
        }
    }

    // Get mappings for a module  
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<?> getMappingsForModule(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            List<OutcomeMapping> mappings = mappingService.getMappingsForModule(moduleId);
            return ResponseEntity.ok(createSuccessResponse("Module mappings retrieved", mappings));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error retrieving mappings: " + e.getMessage()));
        }
    }

    // Update mapping (if not approved)
    @PutMapping("/{mappingId}")
    public ResponseEntity<?> updateMapping(
            @PathVariable Long mappingId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            Integer newWeight = (Integer) request.get("weight");
            String lecturerRemarks = (String) request.getOrDefault("lecturerRemarks", "");

            OutcomeMapping updatedMapping = mappingService.updateMapping(mappingId, newWeight, lecturerRemarks);
            return ResponseEntity.ok(createSuccessResponse("Mapping updated successfully", updatedMapping));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error updating mapping: " + e.getMessage()));
        }
    }

    // Delete mapping (if not approved)
    @DeleteMapping("/{mappingId}")
    public ResponseEntity<?> deleteMapping(@PathVariable Long mappingId, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            mappingService.deleteMapping(mappingId);
            return ResponseEntity.ok(createSuccessResponse("Mapping deleted successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error deleting mapping: " + e.getMessage()));
        }
    }

    // Get mapping statistics for a module
    @GetMapping("/statistics/{moduleId}")
    public ResponseEntity<?> getMappingStatistics(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecturer(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Lecturer privileges required."));
            }

            Map<String, Object> stats = mappingService.getMappingStatistics(moduleId);
            return ResponseEntity.ok(createSuccessResponse("Statistics retrieved", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error retrieving statistics: " + e.getMessage()));
        }
    }

    // === ADMIN ENDPOINTS ===

    // Get all pending mappings for review
    @GetMapping("/admin/pending")
    public ResponseEntity<?> getPendingMappings(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }

            List<OutcomeMapping> pendingMappings = mappingService.getPendingMappings();
            return ResponseEntity.ok(createSuccessResponse("Pending mappings retrieved", pendingMappings));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error retrieving pending mappings: " + e.getMessage()));
        }
    }

    // Approve mapping
    @PutMapping("/admin/{mappingId}/approve")
    public ResponseEntity<?> approveMapping(
            @PathVariable Long mappingId,
            @RequestBody(required = false) Map<String, String> request,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }

            String username = extractUsername(token);
            String adminRemarks = request != null ? request.getOrDefault("adminRemarks", "") : "";

            OutcomeMapping approvedMapping = mappingService.approveMapping(mappingId, username, adminRemarks);
            auditLogService.log(username, "MAPPING_APPROVE", String.valueOf(mappingId), "SUCCESS", adminRemarks);
            return ResponseEntity.ok(createSuccessResponse("Mapping approved successfully", approvedMapping));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error approving mapping: " + e.getMessage()));
        }
    }

    // Reject mapping
    @PutMapping("/admin/{mappingId}/reject")
    public ResponseEntity<?> rejectMapping(
            @PathVariable Long mappingId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }

            String username = extractUsername(token);
            String adminRemarks = request.getOrDefault("adminRemarks", "").trim();
            if (adminRemarks.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Rejection remarks are required."));
            }

            OutcomeMapping rejectedMapping = mappingService.rejectMapping(mappingId, username, adminRemarks);
            auditLogService.log(username, "MAPPING_REJECT", String.valueOf(mappingId), "SUCCESS", adminRemarks);
            return ResponseEntity.ok(createSuccessResponse("Mapping rejected", rejectedMapping));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error rejecting mapping: " + e.getMessage()));
        }
    }

    // Bulk approve mappings for an LO
    @PutMapping("/admin/lo/{loId}/approve-all")
    public ResponseEntity<?> bulkApproveMappingsForLO(
            @PathVariable String loId,
            @RequestBody(required = false) Map<String, String> request,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }

            String username = extractUsername(token);
            String adminRemarks = request != null ? request.getOrDefault("adminRemarks", "Bulk approved") : "Bulk approved";

            List<OutcomeMapping> approvedMappings = mappingService.bulkApproveMappingsForLO(loId, username, adminRemarks);
            return ResponseEntity.ok(createSuccessResponse("Mappings approved successfully", Map.of(
                "approvedCount", approvedMappings.size(),
                "mappings", approvedMappings
            )));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error bulk approving mappings: " + e.getMessage()));
        }
    }

    // Get comprehensive mapping report for a module
    @GetMapping("/admin/report/{moduleId}")
    public ResponseEntity<?> getModuleMappingReport(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }

            Map<String, Object> report = mappingService.getModuleMappingReport(moduleId);
            return ResponseEntity.ok(createSuccessResponse("Module mapping report generated", report));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error generating report: " + e.getMessage()));
        }
    }

    // === PUBLIC ENDPOINTS ===

    // Get all available Program Outcomes for mapping interface
    @GetMapping("/program-outcomes")
    public ResponseEntity<?> getProgramOutcomes(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            List<com.example.Software.project.Backend.Model.ProgramOutcome> pos = poService.getAllActivePOs();
            return ResponseEntity.ok(createSuccessResponse("Program Outcomes retrieved", pos));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error retrieving Program Outcomes: " + e.getMessage()));
        }
    }

    // Health check for mapping system
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            List<OutcomeMapping> pendingMappings = mappingService.getPendingMappings();

            Map<String, Object> healthData = new HashMap<>();
            healthData.put("pendingMappings", pendingMappings.size());
            healthData.put("systemStatus", "Healthy");

            return ResponseEntity.ok(createSuccessResponse("LO-PO Mapping system is healthy", healthData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Health check failed: " + e.getMessage()));
        }
    }
}