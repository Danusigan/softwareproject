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

    // --- ADMIN ONLY: Create PO (Program Outcome) ---
    @PostMapping("/po/create")
    public ResponseEntity<?> createPO(@RequestBody ProgramOutcome po, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin only", "status", "ERROR"));
            }
            ProgramOutcome createdPo = poRepo.save(po);
            return ResponseEntity.ok(Map.of("message", "Program Outcome created successfully", "data", createdPo, "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error creating PO: " + e.getMessage(), "status", "ERROR"));
        }
    }

    // --- ADMIN ONLY: Read All POs ---
    @GetMapping("/po/all")
    public ResponseEntity<?> getAllPOs(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin only", "status", "ERROR"));
            }
            return ResponseEntity.ok(Map.of("message", "All Program Outcomes", "data", poRepo.findAll(), "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error fetching POs: " + e.getMessage(), "status", "ERROR"));
        }
    }

    // --- ADMIN ONLY: Read One PO by ID ---
    @GetMapping("/po/{poId}")
    public ResponseEntity<?> getPOById(@PathVariable String poId, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin only", "status", "ERROR"));
            }
            return poRepo.findById(poId)
                .map(po -> ResponseEntity.ok(Map.of("message", "PO found", "data", po, "status", "SUCCESS")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "PO not found", "status", "ERROR")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error fetching PO: " + e.getMessage(), "status", "ERROR"));
        }
    }

    // --- ADMIN ONLY: Update PO ---
    @PutMapping("/po/{poId}")
    public ResponseEntity<?> updatePO(@PathVariable String poId, @RequestBody ProgramOutcome poDetails, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin only", "status", "ERROR"));
            }
            return poRepo.findById(poId)
                .map(po -> {
                    if (poDetails.getCode() != null) po.setCode(poDetails.getCode());
                    if (poDetails.getDescription() != null) po.setDescription(poDetails.getDescription());
                    ProgramOutcome updatedPo = poRepo.save(po);
                    return ResponseEntity.ok(Map.of("message", "PO updated successfully", "data", updatedPo, "status", "SUCCESS"));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "PO not found", "status", "ERROR")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error updating PO: " + e.getMessage(), "status", "ERROR"));
        }
    }

    // --- ADMIN ONLY: Delete PO ---
    @DeleteMapping("/po/{poId}")
    public ResponseEntity<?> deletePO(@PathVariable String poId, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin only", "status", "ERROR"));
            }
            if (poRepo.existsById(poId)) {
                poRepo.deleteById(poId);
                return ResponseEntity.ok(Map.of("message", "PO deleted successfully", "status", "SUCCESS"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "PO not found", "status", "ERROR"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error deleting PO: " + e.getMessage(), "status", "ERROR"));
        }
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
    @PostMapping("/marks/upload/{losId}")
    public ResponseEntity<?> uploadMarks(@PathVariable String losId, @RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lecture only");
        try {
            excelService.importMarks(file, losId);
            return ResponseEntity.ok("Marks uploaded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // --- REPORT: Course Attainment (Flat JSON for Charts) ---
    @GetMapping("/reports/course/{moduleId}")
    public ResponseEntity<?> getCourseReport(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lecture only");
        Map<String, Double> poScores = attainmentService.getPOAttainment(moduleId);
        return ResponseEntity.ok(poScores);
    }

    // --- ANALYSIS: Module Trend ---
    @GetMapping("/analysis/trend/{moduleId}")
    public ResponseEntity<?> getTrend(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lecture only");
        return ResponseEntity.ok(trendService.getCourseTrend(moduleId));
    }

    // --- ANALYSIS: LO Trend (New) ---
    @GetMapping("/analysis/trend/lo/{moduleId}")
    public ResponseEntity<?> getLoTrend(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lecture only");
        return ResponseEntity.ok(trendService.getLoTrend(moduleId));
    }

    // Helper RBAC
    private boolean isAdmin(String token) {
        try {
            String bearerToken = token;
            if (token != null && token.startsWith("Bearer ")) {
                bearerToken = token.substring(7);
            }
            String role = jwtUtil.extractRole(bearerToken);
            role = role == null ? null : role.trim();
            return role != null && ("Admin".equalsIgnoreCase(role) || "Superadmin".equalsIgnoreCase(role));
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
            role = role == null ? null : role.trim();
            return role != null && ("Lecture".equalsIgnoreCase(role) || "Admin".equalsIgnoreCase(role) || "Superadmin".equalsIgnoreCase(role));
        } catch (Exception e) {
            return false;
        }
    }
}
