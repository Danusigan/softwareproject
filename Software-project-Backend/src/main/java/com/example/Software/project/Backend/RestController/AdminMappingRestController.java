package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.Mapping;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.MappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AdminMappingRestController {

    @Autowired
    private MappingService mappingService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Approve all PENDING LO-PO mappings for a given course/module.
     * Endpoint: PUT /api/admin/approve-course-mapping/{courseId}
     * courseId is mapped to internal moduleCode via String.valueOf(courseId).
     */
    @PutMapping("/approve-course-mapping/{courseId}")
    public ResponseEntity<?> approveCourseMappings(
            @PathVariable Long courseId,
            @RequestParam(value = "adminRemarks", required = false) String adminRemarks,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access Denied: Only Admins can approve course-level mappings");
            }

            String adminEmail = jwtUtil.extractUsername(token.substring(7));
            String moduleCode = String.valueOf(courseId);

            List<Mapping> approvedMappings = mappingService.approveCourseMappings(moduleCode, adminEmail, adminRemarks);

            return ResponseEntity.ok(Map.of(
                    "message", "Course mappings approved successfully",
                    "courseId", courseId,
                    "moduleCode", moduleCode,
                    "approvedCount", approvedMappings.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "message", "Failed to approve course mappings",
                            "error", e.getMessage(),
                            "courseId", courseId
                    ));
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
