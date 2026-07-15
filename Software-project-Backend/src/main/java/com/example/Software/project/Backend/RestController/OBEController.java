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
    @Autowired private FileValidationService fileValidationService;

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
            fileValidationService.validateExcelFile(file);
            excelService.importMarks(file, losId);
            return ResponseEntity.ok("Marks uploaded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // --- LECTURE: Upload question-wise marks using a template ---
    @PostMapping("/marks/upload-question-wise")
    public ResponseEntity<?> uploadQuestionWiseMarks(
            @RequestParam("excelFile") MultipartFile file,
            @RequestParam("templateId") String templateId,
            @RequestParam("batch") String batch,
            @RequestParam(value = "markType", required = false, defaultValue = "FINAL_EXAM") String markType,
            @RequestHeader("Authorization") String token) {
        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied: Only Lecturers/Admins can upload marks", "status", "ERROR"));
        }

        try {
            fileValidationService.validateExcelFile(file);
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

            // Generate template
            byte[] templateBytes = excelExportService.generateMarkTemplate(losIds);

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

    // --- BULK UPLOAD: Upload marks for multiple LOs in one Excel file ---
    @PostMapping("/marks/upload-bulk")
    public ResponseEntity<?> uploadMarksBulk(
            @RequestParam("excelFile") MultipartFile file,
            @RequestParam("losIds") String losIdsParam,
            @RequestParam("batch") String batch,
            @RequestParam(value = "markType", required = false, defaultValue = "FINAL_EXAM") String markType,
            @RequestHeader("Authorization") String token) {

        if (!isLecture(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied: Only Lecturers/Admins can upload marks", "status", "ERROR"));
        }

        try {
            fileValidationService.validateExcelFile(file);

            // Parse losIds from comma-separated string
            String[] losIds = losIdsParam.split(",");

            if (losIds.length == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "message", "Error: losIds cannot be empty",
                        "status", "ERROR"
                    ));
            }

            if (batch == null || batch.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "message", "Error: batch is required",
                        "status", "ERROR"
                    ));
            }

            // Import marks using bulk method
            String result = excelService.importMarksBulk(file, losIds, batch, markType);

            return ResponseEntity.ok(Map.of(
                "message", result,
                "status", "SUCCESS",
                "data", Map.of(
                    "losCount", losIds.length,
                    "batch", batch,
                    "markType", markType
                )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "message", "Failed to import marks: " + e.getMessage(),
                    "error", e.getMessage(),
                    "status", "ERROR"
                ));
        }
    }

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

            Map<String, Object> result = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold);
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

            // Calculate PO credits
            Map<String, Object> attainmentData = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold);

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
            }

            byte[] templateBytes = excelExportService.generateQuestionMarkTemplate(templateId, numberOfQuestions, questionMappings);

            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"question_mark_template.xlsx\"")
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
