package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.ModuleRepository;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.CQIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cqi")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class CqiActionController {

    @Autowired private CQIService cqiService;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private JwtUtil jwtUtil;

    // --- LECTURE: List own CQI actions (any status) ---
    @GetMapping("/my-plans")
    public ResponseEntity<?> getMyPlans(@RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Lecture only", "status", "ERROR"));
        try {
            List<CqiAction> plans = cqiService.getMyPlans(username(token));
            return ResponseEntity.ok(Map.of("message", "My CQI plans", "data", plans, "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    // --- LECTURE: Submit/fill in a CQI plan ---
    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitPlan(@PathVariable Long id, @RequestBody CqiPlanDTO dto, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Lecture only", "status", "ERROR"));
        try {
            CqiAction action = cqiService.submitPlan(id, username(token), dto);
            return ResponseEntity.ok(Map.of("message", "CQI plan submitted", "data", action, "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    // --- ADMIN: Review queue ---
    @GetMapping("/pending")
    public ResponseEntity<?> getPending(@RequestHeader("Authorization") String token) {
        if (!isAdmin(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin only", "status", "ERROR"));
        try {
            return ResponseEntity.ok(Map.of("message", "Pending CQI plans", "data", cqiService.getPendingForAdmin(), "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    // --- ADMIN: Approve a submitted plan ---
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approvePlan(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        if (!isAdmin(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin only", "status", "ERROR"));
        try {
            CqiAction action = cqiService.approvePlan(id, username(token));
            return ResponseEntity.ok(Map.of("message", "CQI plan approved", "data", action, "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    // --- ADMIN: Return a plan for revision ---
    @PutMapping("/{id}/return")
    public ResponseEntity<?> returnPlan(@PathVariable Long id, @RequestBody Map<String, String> body, @RequestHeader("Authorization") String token) {
        if (!isAdmin(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin only", "status", "ERROR"));
        try {
            String comment = body != null ? body.get("comment") : null;
            CqiAction action = cqiService.returnPlan(id, username(token), comment);
            return ResponseEntity.ok(Map.of("message", "CQI plan returned for revision", "data", action, "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    // --- ADMIN or owning LECTURE: Full CQI history for a module (accreditation evidence) ---
    @GetMapping("/module/{moduleId}/history")
    public ResponseEntity<?> getModuleHistory(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        if (!isAdmin(token) && !ownsModule(token, moduleId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied", "status", "ERROR"));
        }
        try {
            return ResponseEntity.ok(Map.of("message", "CQI history", "data", cqiService.getCqiHistoryForModule(moduleId), "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    // --- LECTURE/ADMIN: Finalize a batch's LO attainment for a module — links/triggers CQI ---
    @PostMapping("/finalize/{moduleId}")
    public ResponseEntity<?> finalize(@PathVariable String moduleId, @RequestParam String batch, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Lecture only", "status", "ERROR"));
        if (batch == null || batch.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "batch is required", "status", "ERROR"));
        }
        try {
            Map<String, Object> result = cqiService.finalizeModuleAttainment(moduleId, batch.trim());
            return ResponseEntity.ok(Map.of("message", "Attainment finalized", "data", result, "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    // --- Helpers ---

    private boolean ownsModule(String token, String moduleId) {
        try {
            if (!isLecture(token)) return false;
            String username = username(token);
            return moduleRepository.findById(moduleId)
                .map(m -> {
                    List<String> lecturers = m.getAssignedLecturerUsernames();
                    return lecturers == null || lecturers.isEmpty() || lecturers.contains(username);
                })
                .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    private String username(String token) {
        String bearerToken = token;
        if (token != null && token.startsWith("Bearer ")) {
            bearerToken = token.substring(7);
        }
        return jwtUtil.extractUsername(bearerToken);
    }

    private boolean isAdmin(String token) {
        try {
            String bearerToken = token;
            if (token != null && token.startsWith("Bearer ")) {
                bearerToken = token.substring(7);
            }
            String role = jwtUtil.extractRole(bearerToken);
            role = role == null ? null : role.trim().toLowerCase();
            return role != null && (role.equals("admin") || role.equals("superadmin"));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLecture(String token) {
        try {
            String bearerToken = token;
            if (token != null && token.startsWith("Bearer ")) {
                bearerToken = token.substring(7);
            }
            String role = jwtUtil.extractRole(bearerToken);
            role = role == null ? null : role.trim().toLowerCase();
            return role != null && (role.equals("lecture") || role.equals("admin") || role.equals("superadmin"));
        } catch (Exception e) {
            return false;
        }
    }
}
