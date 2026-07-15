package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.ProgramOutcome;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.AuditLogService;
import com.example.Software.project.Backend.Service.ProgramOutcomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/program-outcomes")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ProgramOutcomeRestController {

    @Autowired
    private ProgramOutcomeService poService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuditLogService auditLogService;

    // Helper method to validate admin access
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

    // Helper method to create error response
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ERROR");
        response.put("message", message);
        return response;
    }

    // Helper method to create success response
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    // === ADMIN ONLY ENDPOINTS ===

    // Create new PO (Admin only)
    @PostMapping("/create")
    public ResponseEntity<?> createPO(@RequestBody ProgramOutcome po, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }
            
            // Set created by from token
            String jwt = token.substring(7);
            String username = jwtUtil.extractUsername(jwt);
            po.setCreatedBy(username);
            
            ProgramOutcome createdPO = poService.createPO(po);
            auditLogService.log(username, "PO_CREATE", createdPO.getPoId(), "SUCCESS", null);
            return ResponseEntity.ok(createSuccessResponse("Program Outcome created successfully", createdPO));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error creating Program Outcome: " + e.getMessage()));
        }
    }

    // Update PO (Admin only)
    @PutMapping("/{poId}")
    public ResponseEntity<?> updatePO(@PathVariable String poId, @RequestBody ProgramOutcome poDetails, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }
            
            ProgramOutcome updatedPO = poService.updatePO(poId, poDetails);
            auditLogService.log(jwtUtil.extractUsername(token.substring(7)), "PO_UPDATE", poId, "SUCCESS", null);
            return ResponseEntity.ok(createSuccessResponse("Program Outcome updated successfully", updatedPO));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error updating Program Outcome: " + e.getMessage()));
        }
    }

    // Soft delete PO (Admin only)
    @DeleteMapping("/{poId}")
    public ResponseEntity<?> deletePO(@PathVariable String poId, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }
            
            poService.softDeletePO(poId);
            auditLogService.log(jwtUtil.extractUsername(token.substring(7)), "PO_DELETE", poId, "SUCCESS", "soft delete");
            return ResponseEntity.ok(createSuccessResponse("Program Outcome deactivated successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error deleting Program Outcome: " + e.getMessage()));
        }
    }

    // Hard delete PO (SuperAdmin only — irreversible, more destructive than the other admin-level actions here)
    @DeleteMapping("/{poId}/permanent")
    @PreAuthorize("hasAuthority('superadmin')")
    public ResponseEntity<?> hardDeletePO(@PathVariable String poId, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }
            
            poService.hardDeletePO(poId);
            auditLogService.log(jwtUtil.extractUsername(token.substring(7)), "PO_DELETE", poId, "SUCCESS", "permanent delete");
            return ResponseEntity.ok(createSuccessResponse("Program Outcome permanently deleted", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error permanently deleting Program Outcome: " + e.getMessage()));
        }
    }

    // Restore PO (Admin only)
    @PutMapping("/{poId}/restore")
    public ResponseEntity<?> restorePO(@PathVariable String poId, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }
            
            ProgramOutcome restoredPO = poService.restorePO(poId);
            return ResponseEntity.ok(createSuccessResponse("Program Outcome restored successfully", restoredPO));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error restoring Program Outcome: " + e.getMessage()));
        }
    }

    // Initialize default POs (Admin only)
    @PostMapping("/initialize-defaults")
    public ResponseEntity<?> initializeDefaultPOs(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }
            
            poService.initializeDefaultPOs();
            List<ProgramOutcome> defaultPOs = poService.getDefaultPOs();
            return ResponseEntity.ok(createSuccessResponse("Default Washington Accord Program Outcomes initialized successfully", defaultPOs));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error initializing default POs: " + e.getMessage()));
        }
    }

    // Reorder POs (Admin only)
    @PutMapping("/reorder")
    public ResponseEntity<?> reorderPOs(@RequestBody List<String> poIds, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }
            
            poService.reorderPOs(poIds);
            return ResponseEntity.ok(createSuccessResponse("Program Outcomes reordered successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error reordering POs: " + e.getMessage()));
        }
    }

    // === PUBLIC/AUTHENTICATED ENDPOINTS ===

    // Get all active POs
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<?> getAllActivePOs(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            List<ProgramOutcome> pos = poService.getAllActivePOs();
            return ResponseEntity.ok(createSuccessResponse("Active Program Outcomes retrieved successfully", pos));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error fetching Program Outcomes: " + e.getMessage()));
        }
    }

    // Get all POs including inactive (Admin only)
    @GetMapping("/all-including-inactive")
    public ResponseEntity<?> getAllPOs(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access denied. Admin privileges required."));
            }
            
            List<ProgramOutcome> pos = poService.getAllPOs();
            return ResponseEntity.ok(createSuccessResponse("All Program Outcomes retrieved successfully", pos));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error fetching Program Outcomes: " + e.getMessage()));
        }
    }

    // Get PO by ID
    @GetMapping("/{poId}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<?> getPOById(@PathVariable String poId, @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Optional<ProgramOutcome> po = poService.getPOById(poId);
            if (po.isPresent()) {
                return ResponseEntity.ok(createSuccessResponse("Program Outcome found", po.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("Program Outcome not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error fetching Program Outcome: " + e.getMessage()));
        }
    }

    // Get default Washington Accord POs
    @GetMapping("/defaults")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<?> getDefaultPOs(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            List<ProgramOutcome> defaultPOs = poService.getDefaultPOs();
            return ResponseEntity.ok(createSuccessResponse("Default Washington Accord Program Outcomes retrieved", defaultPOs));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error fetching default POs: " + e.getMessage()));
        }
    }

    // Get custom POs
    @GetMapping("/custom")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<?> getCustomPOs(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            List<ProgramOutcome> customPOs = poService.getCustomPOs();
            return ResponseEntity.ok(createSuccessResponse("Custom Program Outcomes retrieved", customPOs));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error fetching custom POs: " + e.getMessage()));
        }
    }

    // Get POs by category
    @GetMapping("/by-category/{category}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<?> getPOsByCategory(@PathVariable String category, @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // This would require a new method in service
            List<ProgramOutcome> pos = poService.getAllActivePOs().stream()
                .filter(po -> category.equalsIgnoreCase(po.getCategory()))
                .toList();
            return ResponseEntity.ok(createSuccessResponse("Program Outcomes for category '" + category + "' retrieved", pos));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error fetching POs by category: " + e.getMessage()));
        }
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            long totalPOs = poService.getAllPOs().size();
            long activePOs = poService.getAllActivePOs().size();
            long defaultPOs = poService.getDefaultPOs().size();
            long customPOs = poService.getCustomPOs().size();

            Map<String, Object> healthData = new HashMap<>();
            healthData.put("totalPOs", totalPOs);
            healthData.put("activePOs", activePOs);
            healthData.put("defaultPOs", defaultPOs);
            healthData.put("customPOs", customPOs);
            healthData.put("status", "Healthy");

            return ResponseEntity.ok(createSuccessResponse("Program Outcome system is healthy", healthData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Health check failed: " + e.getMessage()));
        }
    }
}