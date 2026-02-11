package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.TrendReportDTO;
import com.example.Software.project.Backend.Repository.LosPosRepository;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.AttainmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AnalysisRestController {

    @Autowired
    private AttainmentService attainmentService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LosPosRepository losPosRepository;

    /**
     * Comparative trend analysis endpoint.
     * GET /api/analysis/trend/{courseId}?years=2023,2024,2025
     * Accessible to SuperAdmin, Admin, and Lecturer.
     * Lecturers are restricted to courses (modules) where they have defined LOs.
     */
    @GetMapping("/trend/{courseId}")
    public ResponseEntity<?> getPerformanceTrend(
            @PathVariable Long courseId,
            @RequestParam("years") String yearsParam,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Access Denied: Authentication required");
            }

            String role = jwtUtil.extractRole(token.substring(7));
            String userEmail = jwtUtil.extractUsername(token.substring(7));

            // Restrict lecturers to their own courses (modules with LOs created by them)
            if ("lecture".equalsIgnoreCase(role)) {
                String moduleCode = String.valueOf(courseId);
                boolean hasAccess = !losPosRepository.findByModuleCodeAndCreatedBy(moduleCode, userEmail).isEmpty();
                if (!hasAccess) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Access Denied: You are not assigned to this course");
                }
            }

            List<String> years = Arrays.stream(yearsParam.split(","))
                    .map(String::trim)
                    .filter(y -> !y.isEmpty())
                    .collect(Collectors.toList());

            TrendReportDTO report = attainmentService.getPerformanceTrend(courseId, years);

            return ResponseEntity.ok(Map.of(
                    "courseId", courseId,
                    "years", years,
                    "trend", report
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "message", "Failed to generate performance trend analysis",
                            "error", e.getMessage(),
                            "courseId", courseId
                    ));
        }
    }

    private boolean isAuthenticated(String token) {
        try {
            return token != null && token.startsWith("Bearer ") &&
                    jwtUtil.extractUsername(token.substring(7)) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
