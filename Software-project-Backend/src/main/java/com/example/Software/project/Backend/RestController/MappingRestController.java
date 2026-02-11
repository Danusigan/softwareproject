package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.Mapping;
import com.example.Software.project.Backend.Model.Mapping.MappingStatus;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.MappingService;
import com.example.Software.project.Backend.Service.MappingService.BulkMappingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/mappings")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class MappingRestController {

    @Autowired
    private MappingService mappingService;

    @Autowired
    private JwtUtil jwtUtil;

    // Create new LO-PO mapping (Lecture only)
    @PostMapping
    public ResponseEntity<?> createMapping(
            @RequestParam("losPosId") String losPosId,
            @RequestParam("programOutcomeId") Long programOutcomeId,
            @RequestParam("weight") Integer weight,
            @RequestParam(value = "lecturerRemarks", required = false) String lecturerRemarks,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecturers can create LO-PO mappings");
            }

            String lecturerEmail = jwtUtil.extractUsername(token.substring(7));
            Mapping mapping = mappingService.createMapping(losPosId, programOutcomeId, weight, lecturerEmail, lecturerRemarks);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapping);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    /**
     * Bulk save LO-PO mappings in PENDING status.
     * Endpoint: POST /api/mappings/bulk-save
     * Request body: [ { "loId": "<losPosId>", "poId": 1, "weight": 3 }, ... ]
     */
    @PostMapping("/bulk-save")
    public ResponseEntity<?> bulkSaveMappings(
            @RequestBody List<BulkMappingRequest> requests,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access Denied: Only Lecturers can create LO-PO mappings");
            }

            if (requests == null || requests.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Request body must contain at least one mapping entry");
            }

            String lecturerEmail = jwtUtil.extractUsername(token.substring(7));
            var createdMappings = mappingService.bulkCreateMappings(requests, lecturerEmail);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Bulk mappings created successfully",
                    "count", createdMappings.size(),
                    "status", "PENDING",
                    "mappings", createdMappings
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "message", "Failed to bulk-save mappings",
                            "error", e.getMessage()
                    ));
        }
    }

    // Get all mappings (All authenticated users can view)
    @GetMapping
    public ResponseEntity<List<Mapping>> getAllMappings(@RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<Mapping> mappings = mappingService.getAllMappings();
            return ResponseEntity.ok(mappings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get mapping by ID (All authenticated users)
    @GetMapping("/{id}")
    public ResponseEntity<Mapping> getMappingById(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Optional<Mapping> mapping = mappingService.getMappingById(id);
            return mapping.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get mappings by LosPos ID (All authenticated users)
    @GetMapping("/lospos/{losPosId}")
    public ResponseEntity<List<Mapping>> getMappingsByLosPosId(@PathVariable String losPosId, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<Mapping> mappings = mappingService.getMappingsByLosPosId(losPosId);
            return ResponseEntity.ok(mappings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get mappings by Program Outcome ID (All authenticated users)
    @GetMapping("/program-outcome/{programOutcomeId}")
    public ResponseEntity<List<Mapping>> getMappingsByProgramOutcomeId(@PathVariable Long programOutcomeId, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<Mapping> mappings = mappingService.getMappingsByProgramOutcomeId(programOutcomeId);
            return ResponseEntity.ok(mappings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get pending mappings for admin review (Admin/SuperAdmin only)
    @GetMapping("/pending")
    public ResponseEntity<List<Mapping>> getPendingMappingsForReview(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<Mapping> pendingMappings = mappingService.getPendingMappingsForReview();
            return ResponseEntity.ok(pendingMappings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get mappings by status (All authenticated users)
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Mapping>> getMappingsByStatus(@PathVariable String status, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            MappingStatus mappingStatus = MappingStatus.valueOf(status.toUpperCase());
            List<Mapping> mappings = mappingService.getMappingsByStatus(mappingStatus);
            return ResponseEntity.ok(mappings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get mappings by lecturer (Lecturers can see their own, Admins can see all)
    @GetMapping("/lecturer/{lecturerEmail}")
    public ResponseEntity<List<Mapping>> getMappingsByLecturer(@PathVariable String lecturerEmail, @RequestHeader("Authorization") String token) {
        try {
            String currentUserEmail = jwtUtil.extractUsername(token.substring(7));
            String currentUserRole = jwtUtil.extractRole(token.substring(7));
            
            // Lecturers can only see their own mappings, Admins can see any lecturer's mappings
            if ("lecture".equalsIgnoreCase(currentUserRole) && !currentUserEmail.equals(lecturerEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<Mapping> mappings = mappingService.getMappingsByLecturer(lecturerEmail);
            return ResponseEntity.ok(mappings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Update mapping (Only original lecturer can update pending mappings)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMapping(
            @PathVariable Long id,
            @RequestParam(value = "weight", required = false) Integer weight,
            @RequestParam(value = "lecturerRemarks", required = false) String lecturerRemarks,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecturers can update mappings");
            }

            String lecturerEmail = jwtUtil.extractUsername(token.substring(7));
            Mapping updatedMapping = mappingService.updateMapping(id, weight, lecturerRemarks, lecturerEmail);
            return ResponseEntity.ok(updatedMapping);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Approve mapping (Admin/SuperAdmin only)
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveMapping(
            @PathVariable Long id,
            @RequestParam(value = "adminRemarks", required = false) String adminRemarks,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admins can approve mappings");
            }

            String adminEmail = jwtUtil.extractUsername(token.substring(7));
            Mapping approvedMapping = mappingService.approveMapping(id, adminEmail, adminRemarks);
            return ResponseEntity.ok(approvedMapping);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Reject mapping (Admin/SuperAdmin only)
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectMapping(
            @PathVariable Long id,
            @RequestParam("adminRemarks") String adminRemarks,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admins can reject mappings");
            }

            String adminEmail = jwtUtil.extractUsername(token.substring(7));
            Mapping rejectedMapping = mappingService.rejectMapping(id, adminEmail, adminRemarks);
            return ResponseEntity.ok(rejectedMapping);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Delete mapping (Only original lecturer can delete pending mappings)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMapping(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecturers can delete their own mappings");
            }

            String lecturerEmail = jwtUtil.extractUsername(token.substring(7));
            mappingService.deleteMapping(id, lecturerEmail);
            return ResponseEntity.ok("Mapping deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Get approved mappings by module (All authenticated users)
    @GetMapping("/module/{moduleCode}/approved")
    public ResponseEntity<List<Mapping>> getApprovedMappingsByModule(@PathVariable String moduleCode, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<Mapping> approvedMappings = mappingService.getApprovedMappingsByModule(moduleCode);
            return ResponseEntity.ok(approvedMappings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get high correlation mappings by module (All authenticated users)
    @GetMapping("/module/{moduleCode}/high-correlation")
    public ResponseEntity<List<Mapping>> getHighCorrelationMappingsByModule(@PathVariable String moduleCode, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<Mapping> highCorrelationMappings = mappingService.getHighCorrelationMappingsByModule(moduleCode);
            return ResponseEntity.ok(highCorrelationMappings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Bulk approve mappings (Admin/SuperAdmin only)
    @PostMapping("/bulk-approve")
    public ResponseEntity<?> bulkApproveMappings(
            @RequestParam("mappingIds") List<Long> mappingIds,
            @RequestParam(value = "adminRemarks", required = false) String adminRemarks,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admins can bulk approve mappings");
            }

            String adminEmail = jwtUtil.extractUsername(token.substring(7));
            List<Mapping> approvedMappings = mappingService.bulkApproveMappings(mappingIds, adminEmail, adminRemarks);
            return ResponseEntity.ok(String.format("Successfully approved %d mappings", approvedMappings.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Get mapping statistics (Admin/SuperAdmin only)
    @GetMapping("/statistics")
    public ResponseEntity<?> getMappingStatistics(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admins can view mapping statistics");
            }
            
            var statistics = Map.of(
                "pendingCount", mappingService.getPendingMappingsCount(),
                "approvedCount", mappingService.getApprovedMappingsCount(),
                "rejectedCount", mappingService.getRejectedMappingsCount()
            );
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get mappings requiring urgent review (Admin/SuperAdmin only)
    @GetMapping("/urgent-review")
    public ResponseEntity<List<Mapping>> getMappingsRequiringUrgentReview(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<Mapping> urgentMappings = mappingService.getMappingsRequiringUrgentReview();
            return ResponseEntity.ok(urgentMappings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get recent mappings (All authenticated users)
    @GetMapping("/recent")
    public ResponseEntity<List<Mapping>> getRecentMappings(@RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<Mapping> recentMappings = mappingService.getRecentMappings();
            return ResponseEntity.ok(recentMappings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get weight description helper endpoint (All authenticated users)
    @GetMapping("/weight-description/{weight}")
    public ResponseEntity<String> getWeightDescription(@PathVariable Integer weight, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String description = mappingService.getWeightDescription(weight);
            return ResponseEntity.ok(description);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Frontend-friendly LO-PO matrix for a course/module.
     * GET /api/mappings/matrix/{courseId}
     * courseId is mapped to the internal moduleCode via String.valueOf(courseId).
     */
    @GetMapping("/matrix/{courseId}")
    public ResponseEntity<?> getCourseMappingMatrix(
            @PathVariable Long courseId,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String moduleCode = String.valueOf(courseId);
            Map<String, Map<String, Integer>> matrix = mappingService.getCourseMappingMatrix(moduleCode);

            return ResponseEntity.ok(Map.of(
                    "courseId", courseId,
                    "moduleCode", moduleCode,
                    "matrix", matrix
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "message", "Failed to build course mapping matrix",
                            "error", e.getMessage(),
                            "courseId", courseId
                    ));
        }
    }

    // Authorization helper methods
    private boolean isAuthenticated(String token) {
        try {
            String role = jwtUtil.extractRole(token.substring(7));
            return role != null && !role.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLecture(String token) {
        try {
            String role = jwtUtil.extractRole(token.substring(7));
            return "lecture".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role) || "superadmin".equalsIgnoreCase(role);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAdminOrSuperAdmin(String token) {
        try {
            String role = jwtUtil.extractRole(token.substring(7));
            return "Admin".equalsIgnoreCase(role) || "Superadmin".equalsIgnoreCase(role);
        } catch (Exception e) {
            return false;
        }
    }
}