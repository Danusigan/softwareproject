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

import java.util.*;

@RestController
@RequestMapping("/api/obe")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class OBEController {

    @Autowired private ProgramOutcomeRepository poRepo;
    @Autowired private OutcomeMappingRepository mapRepo;
    @Autowired private LosRepository losRepo;
    @Autowired private ExcelImportService excelService;
    @Autowired private ExcelExportService excelExportService;
    @Autowired private AttainmentService attainmentService;
    @Autowired private POAttainmentService poAttainmentService;
    @Autowired private TrendService trendService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private AssessmentTemplateRepository assessmentTemplateRepo;
    @Autowired private AssessmentItemRepository assessmentItemRepo;
    @Autowired private ModuleRepository moduleRepo;

    // --- ADMIN ONLY: Create PO (Program Outcome) ---
    @PostMapping("/po/create")
    public ResponseEntity<?> createPO(@RequestBody ProgramOutcome po, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin only", "status", "ERROR"));
            }
            if (po.getPoId() != null) {
                po.setPoId(po.getPoId());
                po.setCode(po.getPoId());
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
                    if (poDetails.getDescription() != null) po.setDescription(poDetails.getDescription());
                    po.setCode(po.getPoId());
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

    // --- MARKS: Unified upload — auto-detects LO-wise vs question-wise from METADATA sheet ---
    @PostMapping("/marks/upload")
    public ResponseEntity<?> uploadMarksUnified(
            @RequestParam("excelFile") MultipartFile file,
            @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied", "status", "ERROR"));
        }
        try {
            Map<String, String> meta = excelService.readMetadata(file);
            String templateType = meta.getOrDefault("TEMPLATE_TYPE", "LO_WISE");
            String batch = meta.getOrDefault("BATCH", "");
            String markType = meta.getOrDefault("MARK_TYPE", "FINAL_EXAM");
            String assignmentLabel = meta.getOrDefault("ASSIGNMENT_LABEL", "");

            if (batch.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Could not determine batch from file. Please use a template downloaded from this system.", "status", "ERROR"));
            }

            String result;
            if ("QUESTION_WISE".equalsIgnoreCase(templateType)) {
                String templateId = meta.getOrDefault("TEMPLATE_ID", "");
                if (templateId.isBlank()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "TEMPLATE_ID not found in file. Please re-download the template.", "status", "ERROR"));
                }
                result = excelService.importQuestionWiseMarks(file, templateId, batch, markType,
                    assignmentLabel.isBlank() ? null : assignmentLabel);
            } else {
                String losIdsStr = meta.getOrDefault("LO_IDS", "");
                if (losIdsStr.isBlank()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "LO_IDS not found in file. Please re-download the template.", "status", "ERROR"));
                }
                String[] losIds = losIdsStr.split(",");
                // Parse per-LO max marks: "LO1:50,LO2:30" → Map
                Map<String, Double> perLoMaxMarks = new java.util.LinkedHashMap<>();
                String loMaxMeta = meta.getOrDefault("LO_MAX_MARKS", "");
                if (!loMaxMeta.isBlank()) {
                    for (String pair : loMaxMeta.split(",")) {
                        String[] parts = pair.trim().split(":");
                        if (parts.length == 2) {
                            try { perLoMaxMarks.put(parts[0].trim(), Double.parseDouble(parts[1].trim())); } catch (Exception ignored) {}
                        }
                    }
                }
                result = excelService.importMarksBulk(file, losIds, batch, markType,
                    assignmentLabel.isBlank() ? null : assignmentLabel, perLoMaxMarks);
            }

            return ResponseEntity.ok(Map.of(
                "message", result,
                "status", "SUCCESS",
                "data", Map.of("batch", batch, "markType", markType, "assignmentLabel", assignmentLabel, "templateType", templateType)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    // --- LECTURE: Upload question-wise marks using a template ---
    @PostMapping("/marks/upload-question-wise")
    public ResponseEntity<?> uploadQuestionWiseMarks(
            @RequestParam("excelFile") MultipartFile file,
            @RequestParam(value = "templateId", required = false) String templateId,
            @RequestParam(value = "batch", required = false) String batch,
            @RequestParam(value = "markType", required = false, defaultValue = "FINAL_EXAM") String markType,
            @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied: Only Lecturers/Admins can upload marks", "status", "ERROR"));
        }

        try {
            // Read embedded metadata from the Excel file (batch, markType, templateId)
            Map<String, String> meta = excelService.readMetadata(file);
            if (meta.containsKey("TEMPLATE_ID") && !meta.get("TEMPLATE_ID").isEmpty()
                    && (templateId == null || templateId.isBlank())) {
                templateId = meta.get("TEMPLATE_ID");
            }
            if (meta.containsKey("BATCH") && !meta.get("BATCH").isEmpty()
                    && (batch == null || batch.isBlank())) {
                batch = meta.get("BATCH");
            }
            if (meta.containsKey("MARK_TYPE") && !meta.get("MARK_TYPE").isEmpty()) {
                markType = meta.get("MARK_TYPE");
            }
            if (templateId == null || templateId.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "templateId is required (or embed it in the template METADATA sheet)", "status", "ERROR"));
            }
            String result = excelService.importQuestionWiseMarks(file, templateId, batch, markType);
            return ResponseEntity.ok(Map.of(
                "message", result,
                "status", "SUCCESS",
                "data", Map.of(
                    "templateId", templateId,
                    "batch", batch,
                    "markType", markType
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "message", "Failed to import question-wise marks: " + e.getMessage(),
                    "error", e.getMessage(),
                    "status", "ERROR"
                ));
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

    // --- ANALYSIS: LO Pass Rate by Batch ---
    @GetMapping("/analysis/pass-rate/lo/{moduleId}")
    public ResponseEntity<?> getLoPassRate(
            @PathVariable String moduleId,
            @RequestParam(defaultValue = "50") double threshold,
            @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lecture only");
        return ResponseEntity.ok(trendService.getLoPassRate(moduleId, threshold));
    }

    // --- EXPORT: Generate Excel with selected LOs and mark type ---
    @PostMapping("/export/marks")
    public ResponseEntity<?> exportMarks(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied: Only Lecturers/Admins can export marks", "status", "ERROR"));
        }

        try {
            // Extract parameters from request body
            @SuppressWarnings("unchecked")
            List<String> losIds = (List<String>) request.get("losIds");
            String markType = request.get("markType") != null ? request.get("markType").toString().trim() : null;
            String batch = request.get("batch") != null ? request.get("batch").toString().trim() : null;

            int threshold = 50;
            Object thresholdObj = request.get("threshold");
            if (thresholdObj != null) {
                String thresholdRaw = thresholdObj.toString().trim();
                if (!thresholdRaw.isEmpty()) {
                    try {
                        threshold = Integer.parseInt(thresholdRaw);
                    } catch (NumberFormatException ex) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Error: threshold must be an integer between 0 and 100", "status", "ERROR"));
                    }
                }
            }

            if (threshold < 0 || threshold > 100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: threshold must be between 0 and 100", "status", "ERROR"));
            }

            if (losIds == null || losIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: losIds list cannot be empty", "status", "ERROR"));
            }

            losIds = losIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();

            if (losIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: losIds list cannot be empty", "status", "ERROR"));
            }

            if (markType == null || markType.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: markType is required (FINAL_EXAM or ASSIGNMENT)", "status", "ERROR"));
            }

            if (batch == null || batch.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: batch is required", "status", "ERROR"));
            }

            // Validate mark type
            try {
                MarkType.valueOf(markType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: Invalid markType. Must be FINAL_EXAM or ASSIGNMENT", "status", "ERROR"));
            }

            // Generate Excel
            byte[] excelBytes = excelExportService.generateMarksExcel(losIds, markType, batch, threshold);

            // Return as file download
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"marks_report_" + batch + "_" + markType.toLowerCase() + ".xlsx\"")
                .body(excelBytes);

        } catch (Exception e) {
            String errorDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "message", "Failed to generate marks report: " + errorDetail,
                    "error", errorDetail,
                    "status", "ERROR"
                ));
        }
    }

    // --- TEMPLATE: Generate empty Excel template for mark entry ---
    @PostMapping("/template/marks")
    public ResponseEntity<?> generateMarkTemplate(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied: Only Lecturers/Admins can generate templates", "status", "ERROR"));
        }

        try {
            // Extract parameters
            @SuppressWarnings("unchecked")
            List<String> losIds = (List<String>) request.get("losIds");
            String markType = (String) request.get("markType");
            String batch = (String) request.get("batch");

            if (losIds == null || losIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: losIds list cannot be empty", "status", "ERROR"));
            }

            if (markType == null || markType.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: markType is required", "status", "ERROR"));
            }

            if (batch == null || batch.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: batch is required", "status", "ERROR"));
            }

            // Validate mark type
            try {
                MarkType.valueOf(markType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: Invalid markType", "status", "ERROR"));
            }

            int threshold = 50;
            Object threshObj = request.get("threshold");
            if (threshObj != null) { try { threshold = Integer.parseInt(threshObj.toString().trim()); } catch (Exception ignored) {} }
            double maxMarks = 100;
            Object mmObj = request.get("maxMarksPerLo");
            if (mmObj != null) { try { maxMarks = Double.parseDouble(mmObj.toString().trim()); } catch (Exception ignored) {} }
            String moduleId = request.get("moduleId") != null ? request.get("moduleId").toString().trim() : null;
            String assignmentLabelLo = request.get("assignmentLabel") != null ? request.get("assignmentLabel").toString().trim() : null;
            if (assignmentLabelLo != null && assignmentLabelLo.isEmpty()) assignmentLabelLo = null;

            // Per-LO max marks (map of loId → maxMarks)
            java.util.Map<String, Double> perLoMaxMarks = new java.util.LinkedHashMap<>();
            Object perLoRaw = request.get("perLoMaxMarks");
            if (perLoRaw instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> perLoMap = (java.util.Map<String, Object>) perLoRaw;
                for (java.util.Map.Entry<String, Object> e : perLoMap.entrySet()) {
                    try { perLoMaxMarks.put(e.getKey(), Double.parseDouble(e.getValue().toString())); } catch (Exception ignored) {}
                }
            }
            // Fall back to global maxMarks if no per-LO map provided
            if (perLoMaxMarks.isEmpty()) {
                final double globalMax = maxMarks;
                losIds.forEach(id -> perLoMaxMarks.put(id, globalMax));
            }

            // Generate template
            byte[] templateBytes = excelExportService.generateMarkTemplate(losIds, batch, markType, threshold, perLoMaxMarks, moduleId, assignmentLabelLo);

            // Return as file download
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"mark_template_" + batch + "_" + markType.toLowerCase() + ".xlsx\"")
                .body(templateBytes);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "message", "Failed to generate template",
                    "error", e.getMessage(),
                    "status", "ERROR"
                ));
        }
    }

    // --- BULK UPLOAD: Upload marks — reads batch/markType from METADATA sheet if present ---
    @PostMapping("/marks/upload-bulk")
    public ResponseEntity<?> uploadMarksBulk(
            @RequestParam("excelFile") MultipartFile file,
            @RequestParam(value = "losIds", required = false) String losIdsParam,
            @RequestParam(value = "batch", required = false) String batch,
            @RequestParam(value = "markType", required = false, defaultValue = "FINAL_EXAM") String markType,
            @RequestHeader("Authorization") String token) {

        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied", "status", "ERROR"));
        }

        try {
            // Read metadata from Excel first — overrides form params if present
            Map<String, String> meta = excelService.readMetadata(file);
            if (meta.containsKey("BATCH") && !meta.get("BATCH").isEmpty()) batch = meta.get("BATCH");
            if (meta.containsKey("MARK_TYPE") && !meta.get("MARK_TYPE").isEmpty()) markType = meta.get("MARK_TYPE");
            if (meta.containsKey("LO_IDS") && !meta.get("LO_IDS").isEmpty() && (losIdsParam == null || losIdsParam.isBlank()))
                losIdsParam = meta.get("LO_IDS");

            if (losIdsParam == null || losIdsParam.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: losIds cannot be determined. Use a template downloaded from this system.", "status", "ERROR"));
            }
            if (batch == null || batch.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: batch is required (or use a template that contains METADATA).", "status", "ERROR"));
            }

            String[] losIds = losIdsParam.split(",");
            String result = excelService.importMarksBulk(file, losIds, batch.trim(), markType);

            return ResponseEntity.ok(Map.of(
                "message", result, "status", "SUCCESS",
                "data", Map.of("losCount", losIds.length, "batch", batch.trim(), "markType", markType)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Failed to import marks: " + e.getMessage(), "status", "ERROR"));
        }
    }

    // --- MARKS: List available marks (batch+markType+assignmentLabel groups) for a module ---
    @GetMapping("/marks/available/module/{moduleId}")
    public ResponseEntity<?> getAvailableMarks(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lecture only");
        try {
            List<Object[]> raw = markRepo().findMarksSummaryByModuleId(moduleId);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : raw) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("batch", row[0]);
                entry.put("markType", row[1] != null ? row[1].toString() : null);
                entry.put("assignmentLabel", row[2] != null ? row[2].toString() : null);
                entry.put("markCount", row[3]);
                entry.put("loCount", row[4]);
                result.add(entry);
            }
            return ResponseEntity.ok(Map.of("data", result, "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    // --- MARKS: Delete one assignment's marks for a module ---
    @DeleteMapping("/marks/assignment/module/{moduleId}")
    public ResponseEntity<?> deleteAssignmentMarks(
            @PathVariable String moduleId,
            @RequestParam String batch,
            @RequestParam String markType,
            @RequestParam(required = false) String assignmentLabel,
            @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lecture only");
        try {
            MarkType type = MarkType.valueOf(markType.toUpperCase().replace(" ", "_").replace("-", "_"));
            if (assignmentLabel == null || assignmentLabel.isBlank()) {
                // Delete ALL marks for this module+batch+markType regardless of assignment
                markRepo().deleteByModuleIdAndBatchAndMarkType(moduleId, batch, type);
                assessmentTemplateRepo.findByModule_ModuleIdAndBatchAndMarkType(moduleId, batch, markType.toUpperCase())
                    .forEach(t -> assessmentTemplateRepo.delete(t));
            } else {
                markRepo().deleteByModuleIdAndBatchAndMarkTypeAndAssignmentLabel(
                    moduleId, batch, type, assignmentLabel);
                assessmentTemplateRepo.findByModule_ModuleIdAndBatchAndMarkType(moduleId, batch, markType.toUpperCase())
                    .stream()
                    .filter(t -> assignmentLabel.equals(t.getAssignmentLabel()))
                    .forEach(t -> assessmentTemplateRepo.delete(t));
            }
            return ResponseEntity.ok(Map.of("message", "Assignment marks deleted", "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    // --- MARKS: Delete all marks for a module+batch+markType ---
    @DeleteMapping("/marks/module/{moduleId}")
    public ResponseEntity<?> deleteMarksBatch(
            @PathVariable String moduleId,
            @RequestParam String batch,
            @RequestParam String markType,
            @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lecture only");
        try {
            markRepo().deleteByModuleIdAndBatchAndMarkType(moduleId, batch, MarkType.valueOf(markType.toUpperCase().replace(" ", "_").replace("-", "_")));
            return ResponseEntity.ok(Map.of("message", "Marks deleted", "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    // --- MARKS: Download existing marks for a module+batch+markType as Excel ---
    @GetMapping("/marks/export/module/{moduleId}")
    public ResponseEntity<?> exportBatchMarks(
            @PathVariable String moduleId,
            @RequestParam String batch,
            @RequestParam String markType,
            @RequestParam(defaultValue = "50") int threshold,
            @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lecture only");
        try {
            List<String> losIds = markRepo().findLoIdsByModuleIdAndBatchAndMarkType(moduleId, batch, MarkType.valueOf(markType.toUpperCase()));
            if (losIds.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "No marks found for this batch/markType", "status", "ERROR"));
            byte[] bytes = excelExportService.generateMarksExcel(losIds, markType, batch, threshold);
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"marks_" + moduleId + "_batch" + batch + "_" + markType.toLowerCase() + ".xlsx\"")
                .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    // --- MARKS: List available marks for a single LO ---
    @GetMapping("/marks/available/lo/{loId}")
    public ResponseEntity<?> getAvailableMarksForLo(@PathVariable String loId, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lecture only");
        try {
            List<Object[]> raw = markRepo().findMarksSummaryByLosId(loId);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : raw) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("batch", row[0]);
                entry.put("markType", row[1] != null ? row[1].toString() : null);
                entry.put("markCount", row[2]);
                result.add(entry);
            }
            return ResponseEntity.ok(Map.of("data", result, "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    private StudentMarkRepository markRepo() {
        return studentMarkRepository;
    }
    @Autowired private StudentMarkRepository studentMarkRepository;

    // --- PO ATTAINMENT: Calculate per-student PO credits based on LO pass/fail ---
    @PostMapping("/po-attainment")
    public ResponseEntity<?> getStudentPOAttainment(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied: Only Lecturers/Admins can view PO attainment", "status", "ERROR"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> losIds = (List<String>) request.get("losIds");
            String markType = request.get("markType") != null ? request.get("markType").toString().trim() : null;
            String batch = request.get("batch") != null ? request.get("batch").toString().trim() : null;

            int threshold = 50;
            Object thresholdObj = request.get("threshold");
            if (thresholdObj != null) {
                String thresholdRaw = thresholdObj.toString().trim();
                if (!thresholdRaw.isEmpty()) {
                    threshold = Integer.parseInt(thresholdRaw);
                }
            }

            if (threshold < 0 || threshold > 100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: threshold must be between 0 and 100", "status", "ERROR"));
            }

            if (losIds == null || losIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: losIds list cannot be empty", "status", "ERROR"));
            }

            losIds = losIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();

            if (markType == null || markType.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: markType is required", "status", "ERROR"));
            }

            if (batch == null || batch.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: batch is required", "status", "ERROR"));
            }

            // Validate mark type
            try {
                MarkType.valueOf(markType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: Invalid markType. Must be FINAL_EXAM or ASSIGNMENT", "status", "ERROR"));
            }

            double maxMarksPerLo = 0.0;
            Object maxMarksObj = request.get("maxMarksPerLo");
            if (maxMarksObj != null) {
                try { maxMarksPerLo = Double.parseDouble(maxMarksObj.toString().trim()); } catch (Exception ignored) {}
            }

            Map<String, Object> result = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold, maxMarksPerLo);
            return ResponseEntity.ok(Map.of("message", "PO attainment calculated successfully", "data", result, "status", "SUCCESS"));

        } catch (Exception e) {
            String errorDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Failed to calculate PO attainment: " + errorDetail, "status", "ERROR"));
        }
    }

    // --- EXPORT: Generate Excel with per-student PO attainment credits ---
    @PostMapping("/export/po-attainment")
    public ResponseEntity<?> exportPOAttainment(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied: Only Lecturers/Admins can export PO attainment", "status", "ERROR"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> losIds = (List<String>) request.get("losIds");
            String markType = request.get("markType") != null ? request.get("markType").toString().trim() : null;
            String batch = request.get("batch") != null ? request.get("batch").toString().trim() : null;

            int threshold = 50;
            Object thresholdObj = request.get("threshold");
            if (thresholdObj != null) {
                String thresholdRaw = thresholdObj.toString().trim();
                if (!thresholdRaw.isEmpty()) {
                    threshold = Integer.parseInt(thresholdRaw);
                }
            }

            if (losIds == null || losIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: losIds list cannot be empty", "status", "ERROR"));
            }

            losIds = losIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();

            if (markType == null || markType.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: markType is required", "status", "ERROR"));
            }

            if (batch == null || batch.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: batch is required", "status", "ERROR"));
            }

            try {
                MarkType.valueOf(markType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: Invalid markType", "status", "ERROR"));
            }

            double maxMarksPerLoExport = 0.0;
            Object maxMarksObjExport = request.get("maxMarksPerLo");
            if (maxMarksObjExport != null) {
                try { maxMarksPerLoExport = Double.parseDouble(maxMarksObjExport.toString().trim()); } catch (Exception ignored) {}
            }

            // Calculate PO credits
            Map<String, Object> attainmentData = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold, maxMarksPerLoExport);

            // Generate Excel
            byte[] excelBytes = excelExportService.generatePOAttainmentExcel(attainmentData);

            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"po_attainment_" + batch + "_" + markType.toLowerCase() + ".xlsx\"")
                .body(excelBytes);

        } catch (Exception e) {
            String errorDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Failed to export PO attainment: " + errorDetail, "status", "ERROR"));
        }
    }

    // --- TEMPLATE: Generate question-wise Excel template for mark entry ---
    @PostMapping("/template/marks-question-wise")
    public ResponseEntity<?> generateQuestionMarkTemplate(@RequestParam(value = "templateId", required = false) String templateId,
                                                          @RequestBody(required = false) Map<String, Object> request,
                                                          @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied: Only Lecturers/Admins can generate templates", "status", "ERROR"));
        }

        try {
            Integer numberOfQuestions = null;
            List<Map<String, Object>> questionMappings = new ArrayList<>();
            String batch = null;
            String markType = "FINAL_EXAM";
            String moduleId = null;

            if (request != null) {
                Object countObj = request.get("numberOfQuestions");
                if (countObj != null) {
                    try {
                        numberOfQuestions = Integer.parseInt(countObj.toString().trim());
                    } catch (NumberFormatException ex) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "numberOfQuestions must be a positive integer", "status", "ERROR"));
                    }
                    if (numberOfQuestions <= 0) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "numberOfQuestions must be greater than 0", "status", "ERROR"));
                    }
                }

                Object mappingsObj = request.get("questionMappings");
                if (mappingsObj instanceof List<?> mappingsList) {
                    for (Object item : mappingsList) {
                        if (item instanceof Map<?, ?> mapItem) {
                            Map<String, Object> normalized = new HashMap<>();
                            for (Map.Entry<?, ?> e : mapItem.entrySet()) {
                                if (e.getKey() != null) {
                                    normalized.put(e.getKey().toString(), e.getValue());
                                }
                            }
                            questionMappings.add(normalized);
                        }
                    }
                }

                if (request.get("batch") != null) batch = request.get("batch").toString().trim();
                if (request.get("markType") != null) markType = request.get("markType").toString().trim();
                if (request.get("moduleId") != null) moduleId = request.get("moduleId").toString().trim();
            }

            // Read assignmentLabel from request
            String assignmentLabel = null;
            if (request != null && request.get("assignmentLabel") != null) {
                assignmentLabel = request.get("assignmentLabel").toString().trim();
                if (assignmentLabel.isEmpty()) assignmentLabel = null;
            }

            // Generate a unique template ID and persist AssessmentTemplate + AssessmentItems to DB
            String generatedTemplateId = (templateId != null && !templateId.isBlank())
                ? templateId
                : java.util.UUID.randomUUID().toString();

            AssessmentTemplate tmpl = new AssessmentTemplate();
            tmpl.setId(generatedTemplateId);
            tmpl.setName((moduleId != null ? moduleId : "module") + "_" + (batch != null ? batch : "batch") + "_" + markType + (assignmentLabel != null ? "_" + assignmentLabel : ""));
            tmpl.setBatch(batch);
            tmpl.setMarkType(markType);
            tmpl.setAssignmentLabel(assignmentLabel);
            if (moduleId != null) {
                moduleRepo.findById(moduleId).ifPresent(tmpl::setModule);
            }
            assessmentTemplateRepo.save(tmpl);

            if (!questionMappings.isEmpty()) {
                int qIdx = 0;
                for (Map<String, Object> mapping : questionMappings) {
                    qIdx++;
                    Double maxMarks = null;
                    Object maxMarksObj = mapping.get("maxMarks");
                    if (maxMarksObj != null) {
                        try { maxMarks = Double.parseDouble(maxMarksObj.toString()); } catch (Exception ignored) {}
                    }
                    Los lo = null;
                    Object loIdObj = mapping.get("loId");
                    if (loIdObj != null && !loIdObj.toString().isBlank()) {
                        lo = losRepo.findById(loIdObj.toString().trim()).orElse(null);
                    }
                    if (lo == null || maxMarks == null) continue; // los and maxMarks are non-nullable

                    AssessmentItem item = new AssessmentItem();
                    item.setQuestionNumber(qIdx);
                    Object qLabel = mapping.get("questionNumber");
                    item.setQuestionLabel("Q" + (qLabel != null ? qLabel.toString() : qIdx));
                    item.setMaxMarks(maxMarks);
                    item.setLos(lo);
                    item.setAssessmentTemplate(tmpl);
                    assessmentItemRepo.save(item);
                }
            }

            byte[] templateBytes = excelExportService.generateQuestionMarkTemplate(generatedTemplateId, numberOfQuestions, questionMappings, batch, markType, assignmentLabel);

            String fname = "question_mark_template_" + (batch != null ? batch : "batch")
                + (assignmentLabel != null ? "_" + assignmentLabel.replaceAll("[^a-zA-Z0-9]", "_") : "") + ".xlsx";
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"" + fname + "\"")
                .body(templateBytes);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "message", "Failed to generate question-wise template",
                    "error", e.getMessage(),
                    "status", "ERROR"
                ));
        }
    }

    // --- REPORT: LO attainment with configurable thresholds (per-LO or per-item) ---
    @PostMapping("/attainment/lo")
    public ResponseEntity<?> getLoAttainment(@RequestBody Map<String, Object> request,
                                             @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied: Only Lecturers/Admins can view LO attainment", "status", "ERROR"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> loIds = (List<String>) request.get("loIds");
            String templateId = request.get("templateId") != null ? request.get("templateId").toString().trim() : null;
            Double defaultThreshold = request.get("defaultThreshold") != null
                ? Double.parseDouble(request.get("defaultThreshold").toString())
                : 50.0;

            if (defaultThreshold < 0 || defaultThreshold > 100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "defaultThreshold must be between 0 and 100", "status", "ERROR"));
            }

            if ((loIds == null || loIds.isEmpty()) && (templateId == null || templateId.isEmpty())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Provide either loIds or templateId", "status", "ERROR"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> loThresholdRaw = (Map<String, Object>) request.get("loThresholds");
            Map<String, Double> loThresholds = new HashMap<>();
            if (loThresholdRaw != null) {
                for (Map.Entry<String, Object> e : loThresholdRaw.entrySet()) {
                    if (e.getValue() == null) continue;
                    Double value = Double.parseDouble(e.getValue().toString());
                    if (value < 0 || value > 100) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Each loThreshold must be between 0 and 100", "status", "ERROR"));
                    }
                    loThresholds.put(e.getKey(), value);
                }
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> itemThresholdRaw = (Map<String, Object>) request.get("itemThresholds");
            Map<Long, Double> itemThresholds = new HashMap<>();
            if (itemThresholdRaw != null) {
                for (Map.Entry<String, Object> e : itemThresholdRaw.entrySet()) {
                    if (e.getValue() == null) continue;
                    Long itemId = Long.parseLong(e.getKey());
                    Double value = Double.parseDouble(e.getValue().toString());
                    if (value < 0 || value > 100) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Each itemThreshold must be between 0 and 100", "status", "ERROR"));
                    }
                    itemThresholds.put(itemId, value);
                }
            }

            Map<String, Object> result = attainmentService.getLoAttainmentMetrics(loIds, templateId, defaultThreshold, loThresholds, itemThresholds);
            return ResponseEntity.ok(Map.of("message", "LO attainment calculated", "data", result, "status", "SUCCESS"));

        } catch (NumberFormatException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Invalid numeric input in thresholds", "status", "ERROR"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Failed to calculate LO attainment: " + e.getMessage(), "status", "ERROR"));
        }
    }

    // --- EXPORT: Generate marks report with per-LO thresholds ---
    @PostMapping("/export/marks-per-lo-threshold")
    public ResponseEntity<?> exportMarksWithPerLoThreshold(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied: Only Lecturers/Admins can export marks", "status", "ERROR"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> losIds = (List<String>) request.get("losIds");
            String markType = request.get("markType") != null ? request.get("markType").toString().trim() : null;
            String batch = request.get("batch") != null ? request.get("batch").toString().trim() : null;
            @SuppressWarnings("unchecked")
            Map<String, Integer> loThresholds = (Map<String, Integer>) request.get("loThresholds");

            if (losIds == null || losIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: losIds list cannot be empty", "status", "ERROR"));
            }

            losIds = losIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();

            if (losIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: losIds list cannot be empty", "status", "ERROR"));
            }

            if (markType == null || markType.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: markType is required (FINAL_EXAM or ASSIGNMENT)", "status", "ERROR"));
            }

            if (batch == null || batch.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: batch is required", "status", "ERROR"));
            }

            // Validate mark type
            try {
                MarkType.valueOf(markType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: Invalid markType. Must be FINAL_EXAM or ASSIGNMENT", "status", "ERROR"));
            }

            // Validate loThresholds if provided
            if (loThresholds != null) {
                for (Integer threshold : loThresholds.values()) {
                    if (threshold < 0 || threshold > 100) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Error: each threshold must be between 0 and 100", "status", "ERROR"));
                    }
                }
            }

            // Generate Excel with per-LO thresholds
            byte[] excelBytes = excelExportService.generateMarksExcelWithPerLoThreshold(losIds, markType, batch, loThresholds);

            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"marks_report_per_lo_" + batch + "_" + markType.toLowerCase() + ".xlsx\"")
                .body(excelBytes);

        } catch (Exception e) {
            String errorDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "message", "Failed to generate marks report: " + errorDetail,
                    "error", errorDetail,
                    "status", "ERROR"
                ));
        }
    }

    // --- REPORT: Overall PO Attainment with Benchmark ---
    @PostMapping("/po-attainment/overall")
    public ResponseEntity<?> getOverallPOAttainment(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied: Only Lecturers/Admins can view PO attainment", "status", "ERROR"));
        }

        try {
            String batch = request.get("batch") != null ? request.get("batch").toString().trim() : null;
            String markType = request.get("markType") != null ? request.get("markType").toString().trim() : "FINAL_EXAM";
            Double poThreshold = request.get("poThreshold") != null ? Double.parseDouble(request.get("poThreshold").toString()) : null;

            if (batch == null || batch.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: batch is required", "status", "ERROR"));
            }

            Map<String, Object> result = poAttainmentService.calculateOverallPOAttainment(batch, markType, poThreshold);
            return ResponseEntity.ok(Map.of("message", "Overall PO attainment calculated successfully", "data", result, "status", "SUCCESS"));

        } catch (Exception e) {
            String errorDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Failed to calculate overall PO attainment: " + errorDetail, "status", "ERROR"));
        }
    }

    // Helper RBAC
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
