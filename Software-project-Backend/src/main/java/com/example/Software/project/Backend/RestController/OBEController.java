package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/obe")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class OBEController {

    @Autowired private ProgramOutcomeRepository poRepo;
    @Autowired private OutcomeMappingRepository mapRepo;
    @Autowired private LosRepository losRepo;
    @Autowired private ExcelImportService excelService;
    @Autowired private AttainmentService attainmentService;
    @Autowired private TrendService trendService;
    @Autowired private JwtUtil jwtUtil;

    // --- ADMIN ONLY: Create PO ---
    @PostMapping("/po/create")
    public ResponseEntity<?> createPO(@RequestBody ProgramOutcome po, @RequestHeader("Authorization") String token) {
        if (!isAdmin(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin only");
        return ResponseEntity.ok(poRepo.save(po));
    }

    // --- LECTURE: Bulk Save Mappings (Pending) ---
    @PostMapping("/mappings/bulk-save")
    public ResponseEntity<?> saveMappings(@RequestBody List<OutcomeMapping> mappings, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lecture only");

        try {
            // Ensure LO and PO exist before saving
            for (OutcomeMapping m : mappings) {
                m.setStatus(OutcomeMapping.ApprovalStatus.PENDING);

                // Fetch existing Learning Outcome
                if (m.getLearningOutcome() != null && m.getLearningOutcome().getId() != null) {
                    Los los = losRepo.findById(m.getLearningOutcome().getId())
                            .orElseThrow(() -> new RuntimeException("Learning Outcome not found: " + m.getLearningOutcome().getId()));
                    m.setLearningOutcome(los);
                } else {
                    throw new RuntimeException("Learning Outcome ID is required");
                }

                // Fetch existing Program Outcome
                if (m.getProgramOutcome() != null && m.getProgramOutcome().getId() != null) {
                    ProgramOutcome po = poRepo.findById(m.getProgramOutcome().getId())
                            .orElseThrow(() -> new RuntimeException("Program Outcome not found: " + m.getProgramOutcome().getId()));
                    m.setProgramOutcome(po);
                } else {
                    throw new RuntimeException("Program Outcome ID is required");
                }
            }
            return ResponseEntity.ok(mapRepo.saveAll(mappings));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error saving mappings: " + e.getMessage());
        }
    }

    // --- ADMIN: Approve Mappings ---
    @PutMapping("/admin/approve-mapping/{id}")
    public ResponseEntity<?> approveMapping(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        if (!isAdmin(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin only");

        OutcomeMapping mapping = mapRepo.findById(id).orElseThrow();
        mapping.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        return ResponseEntity.ok(mapRepo.save(mapping));
    }

    // --- LECTURE: Upload Marks ---
    @PostMapping("/marks/upload/{assignmentId}")
    public ResponseEntity<?> uploadMarks(@PathVariable String assignmentId, @RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lecture only");
        try {
            excelService.importMarks(file, assignmentId);
            return ResponseEntity.ok("Marks uploaded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // --- REPORT: Course Attainment (Flat JSON for Charts) ---
    @GetMapping("/reports/course/{moduleId}")
    public ResponseEntity<?> getCourseReport(@PathVariable String moduleId) {
        Map<String, Double> poScores = attainmentService.getPOAttainment(moduleId);
        return ResponseEntity.ok(poScores);
    }

    // --- ANALYSIS: Module Trend ---
    @GetMapping("/analysis/trend/{moduleId}")
    public ResponseEntity<?> getTrend(@PathVariable String moduleId) {
        return ResponseEntity.ok(trendService.getCourseTrend(moduleId));
    }

    // --- ANALYSIS: LO Trend (New) ---
    @GetMapping("/analysis/trend/lo/{moduleId}")
    public ResponseEntity<?> getLoTrend(@PathVariable String moduleId) {
        return ResponseEntity.ok(trendService.getLoTrend(moduleId));
    }

    // Helper RBAC
    private boolean isAdmin(String token) {
        String role = jwtUtil.extractRole(token.substring(7));
        return "Admin".equalsIgnoreCase(role) || "Superadmin".equalsIgnoreCase(role);
    }
    private boolean isLecture(String token) {
        String role = jwtUtil.extractRole(token.substring(7));
        return "Lecture".equalsIgnoreCase(role) || "Admin".equalsIgnoreCase(role) || "Superadmin".equalsIgnoreCase(role);
    }
}
