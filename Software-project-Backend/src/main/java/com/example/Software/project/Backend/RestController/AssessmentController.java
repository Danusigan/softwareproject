package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.AssessmentTemplate;
import com.example.Software.project.Backend.Service.AssessmentService;
import com.example.Software.project.Backend.Security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/obe/assessment")
public class AssessmentController {

    @Autowired
    private AssessmentService assessmentService;
    @Autowired
    private JwtUtil jwtUtil;

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

    @PostMapping("/template")
    public ResponseEntity<?> createTemplate(@RequestBody Map<String, Object> payload, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Lecture only"));
        try {
            String bearer = token != null && token.startsWith("Bearer ") ? token.substring(7) : token;
            String createdBy = jwtUtil.extractUsername(bearer);
            AssessmentTemplate t = assessmentService.createTemplate(payload, createdBy);
            return ResponseEntity.ok(Map.of("message", "Template created", "data", t, "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Error: " + e.getMessage(), "status", "ERROR"));
        }
    }

    @GetMapping("/templates/{moduleId}")
    public ResponseEntity<?> listTemplatesByModule(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Lecture only"));
        try {
            java.util.List<AssessmentTemplate> templates = assessmentService.listTemplatesByModule(moduleId);
            return ResponseEntity.ok(Map.of(
                "message", "Templates retrieved successfully",
                "data", templates,
                "status", "SUCCESS",
                "count", templates.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Error: " + e.getMessage(), "status", "ERROR"));
        }
    }

    @GetMapping("/template/{templateId}")
    public ResponseEntity<?> getTemplate(@PathVariable String templateId, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Lecture only"));
        try {
            AssessmentTemplate template = assessmentService.getTemplate(templateId);
            if (template == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Template not found", "status", "ERROR"));
            }
            return ResponseEntity.ok(Map.of(
                "message", "Template retrieved successfully",
                "data", template,
                "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Error: " + e.getMessage(), "status", "ERROR"));
        }
    }

}
