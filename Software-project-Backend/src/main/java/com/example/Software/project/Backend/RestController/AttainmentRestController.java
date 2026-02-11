package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.AttainmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/attainment")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AttainmentRestController {

    @Autowired
    private AttainmentService attainmentService;

    @Autowired
    private JwtUtil jwtUtil;

    // Calculate attainment for a specific assignment (All authenticated users)
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<?> calculateAssignmentAttainment(@PathVariable String assignmentId, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied: Authentication required");
            }

            Map<String, Object> attainmentResult = attainmentService.calculateAssignmentAttainment(assignmentId);
            return ResponseEntity.ok(attainmentResult);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Calculate attainment for a specific Learning Outcome (All authenticated users)
    @GetMapping("/learning-outcome/{losPosId}")
    public ResponseEntity<?> calculateLearningOutcomeAttainment(@PathVariable String losPosId, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied: Authentication required");
            }

            Map<String, Object> attainmentResult = attainmentService.calculateLoAttainment(losPosId);
            return ResponseEntity.ok(attainmentResult);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Calculate attainment for an entire module (All authenticated users)
    @GetMapping("/module/{moduleCode}")
    public ResponseEntity<?> calculateModuleAttainment(@PathVariable String moduleCode, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied: Authentication required");
            }

            Map<String, Object> attainmentResult = attainmentService.calculateModuleAttainment(moduleCode);
            return ResponseEntity.ok(attainmentResult);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Calculate overall program attainment across all modules (Admin/SuperAdmin only)
    @GetMapping("/program")
    public ResponseEntity<?> calculateOverallProgramAttainment(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admins can view overall program attainment");
            }

            Map<String, Object> attainmentResult = attainmentService.calculateOverallProgramAttainment();
            return ResponseEntity.ok(attainmentResult);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Calculate attainment summary for lecturer's modules (Lecturer can see their own modules)
    @GetMapping("/lecturer-summary")
    public ResponseEntity<?> calculateLecturerAttainmentSummary(@RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecturers can view their attainment summary");
            }

            // This would typically filter modules by lecturer
            // For now, we'll return a message indicating functionality
            return ResponseEntity.ok(Map.of(
                "message", "Lecturer-specific attainment summary",
                "note", "This endpoint would show attainment for modules assigned to the requesting lecturer",
                "lecturerEmail", jwtUtil.extractUsername(token.substring(7))
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Get attainment trends (Admin/SuperAdmin only) 
    @GetMapping("/trends")
    public ResponseEntity<?> getAttainmentTrends(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admins can view attainment trends");
            }

            // This would typically show historical trends
            // For now, we'll return a placeholder response
            return ResponseEntity.ok(Map.of(
                "message", "Attainment trends analysis",
                "note", "This endpoint would show historical attainment trends across semesters",
                "requestedBy", jwtUtil.extractUsername(token.substring(7))
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Get attainment comparison between modules (Admin/SuperAdmin only)
    @GetMapping("/comparison")
    public ResponseEntity<?> getModuleAttainmentComparison(
            @RequestParam(value = "modules", required = false) String[] moduleCodes,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admins can view module comparisons");
            }

            if (moduleCodes == null || moduleCodes.length == 0) {
                return ResponseEntity.badRequest().body("Error: At least one module code must be provided for comparison");
            }

            // This would typically compare attainment across specified modules
            // For now, we'll return a placeholder response
            return ResponseEntity.ok(Map.of(
                "message", "Module attainment comparison",
                "modules", moduleCodes,
                "note", "This endpoint would compare attainment levels across the specified modules",
                "requestedBy", jwtUtil.extractUsername(token.substring(7))
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Get Program Outcome attainment across the program (Admin/SuperAdmin only)
    @GetMapping("/program-outcomes")
    public ResponseEntity<?> getProgramOutcomeAttainment(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admins can view program outcome attainment");
            }

            // This would extract PO attainment from the program calculation
            Map<String, Object> programAttainment = attainmentService.calculateOverallProgramAttainment();
            
            return ResponseEntity.ok(Map.of(
                "programOutcomeAttainments", programAttainment.get("programOutcomeAttainments"),
                "calculatedAt", programAttainment.get("calculatedAt"),
                "summary", "Program Outcome attainment across all modules"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Get attainment status for dashboard (Role-based access)
    @GetMapping("/dashboard")
    public ResponseEntity<?> getAttainmentDashboard(@RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied: Authentication required");
            }

            String userRole = jwtUtil.extractRole(token.substring(7));
            String userEmail = jwtUtil.extractUsername(token.substring(7));

            Map<String, Object> dashboardData;

            if ("Superadmin".equalsIgnoreCase(userRole) || "Admin".equalsIgnoreCase(userRole)) {
                // Admin dashboard: Overall program view
                dashboardData = attainmentService.calculateOverallProgramAttainment();
                dashboardData.put("userRole", userRole);
                dashboardData.put("dashboardType", "admin");
            } else if ("lecture".equalsIgnoreCase(userRole)) {
                // Lecturer dashboard: Their modules only (placeholder for now)
                dashboardData = Map.of(
                    "message", "Lecturer dashboard - showing attainment for your modules",
                    "userEmail", userEmail,
                    "userRole", userRole,
                    "dashboardType", "lecturer",
                    "note", "This would show attainment for modules where you are the lecturer"
                );
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Invalid role");
            }

            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Get detailed attainment report for specific entity (Admin/SuperAdmin only)
    @GetMapping("/report/{entityType}/{entityId}")
    public ResponseEntity<?> getDetailedAttainmentReport(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admins can generate detailed reports");
            }

            Map<String, Object> reportData;

            switch (entityType.toLowerCase()) {
                case "assignment":
                    reportData = attainmentService.calculateAssignmentAttainment(entityId);
                    break;
                case "learning-outcome":
                case "lospos":
                    reportData = attainmentService.calculateLoAttainment(entityId);
                    break;
                case "module":
                    reportData = attainmentService.calculateModuleAttainment(entityId);
                    break;
                default:
                    return ResponseEntity.badRequest().body("Error: Invalid entity type. Use 'assignment', 'learning-outcome', or 'module'");
            }

            reportData.put("reportType", "detailed");
            reportData.put("generatedBy", jwtUtil.extractUsername(token.substring(7)));
            reportData.put("entityType", entityType);

            return ResponseEntity.ok(reportData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
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