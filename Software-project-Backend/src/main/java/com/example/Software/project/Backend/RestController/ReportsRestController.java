package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.AttainmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ReportsRestController {

    @Autowired
    private AttainmentService attainmentService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * OBE Course Attainment Report
     * GET /api/reports/course/{courseId}
     * 
     * Returns a JSON object containing:
     * - A list of COs with their attainment percentages and levels
     * - A list of POs with their final calculated achievement scores
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getCourseAttainmentReport(@PathVariable Long courseId, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied: Authentication required");
            }

            Map<String, Object> attainmentReport = attainmentService.calculateCourseAttainment(courseId);
            return ResponseEntity.ok(attainmentReport);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Failed to generate course attainment report",
                "message", e.getMessage(),
                "courseId", courseId
            ));
        }
    }

    /**
     * Get all course reports (Admin/SuperAdmin only)
     */
    @GetMapping("/courses")
    public ResponseEntity<?> getAllCourseReports(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admins can view all course reports");
            }

            // This would return reports for all courses
            return ResponseEntity.ok(Map.of(
                "message", "All course reports endpoint",
                "note", "This would aggregate reports from all courses"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get attainment summary for lecturer's courses (Lecturer can see their own courses)
     */
    @GetMapping("/lecturer/courses")
    public ResponseEntity<?> getLecturerCourseReports(@RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecturers can view their course reports");
            }

            String lecturerEmail = jwtUtil.extractUsername(token.substring(7));
            
            // This would filter courses by lecturer
            return ResponseEntity.ok(Map.of(
                "message", "Lecturer-specific course reports",
                "lecturerEmail", lecturerEmail,
                "note", "This would show attainment reports for courses assigned to the lecturer"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    /**
     * Export course attainment data to Excel/CSV format
     */
    @GetMapping("/course/{courseId}/export")
    public ResponseEntity<?> exportCourseReportData(@PathVariable Long courseId, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied: Authentication required");
            }

            // Get the attainment data
            Map<String, Object> attainmentReport = attainmentService.calculateCourseAttainment(courseId);
            
            // For now, return the data in JSON format
            // This could be enhanced to generate actual Excel/CSV files
            return ResponseEntity.ok(Map.of(
                "exportData", attainmentReport,
                "format", "json",
                "note", "This endpoint can be enhanced to generate Excel/CSV exports"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Failed to export course report data",
                "message", e.getMessage()
            ));
        }
    }

    // Authentication and authorization helper methods
    private boolean isAuthenticated(String token) {
        try {
            return token != null && token.startsWith("Bearer ") && 
                   jwtUtil.extractUsername(token.substring(7)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLecture(String token) {
        String role = jwtUtil.extractRole(token.substring(7));
        return "lecture".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role) || "superadmin".equalsIgnoreCase(role);
    }

    private boolean isAdminOrSuperAdmin(String token) {
        String role = jwtUtil.extractRole(token.substring(7));
        return "Admin".equalsIgnoreCase(role) || "Superadmin".equalsIgnoreCase(role);
    }
}