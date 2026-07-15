package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.Los;
import com.example.Software.project.Backend.Model.StudentMark;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Repository.StudentMarkRepository;
import com.example.Software.project.Backend.Service.ExcelImportService;
import com.example.Software.project.Backend.Service.FileValidationService;
import com.example.Software.project.Backend.Service.LosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/lospos") // Kept as requested
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class LosRestController {

    @Autowired
    private LosService losService;

    @Autowired
    private ExcelImportService excelImportService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StudentMarkRepository studentMarkRepository;

    @Autowired
    private FileValidationService fileValidationService;

    // Create (Lecture Only) - Add to Module
    @PostMapping("/{moduleId}/add")
    public ResponseEntity<?> addLos(@PathVariable String moduleId, @RequestBody Los los, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Access Denied: Only Lecture can add Los",
                    "status", "ERROR"
                ));
            }
            Los createdLos = losService.addLosToModule(moduleId, los);
            return ResponseEntity.ok(Map.of(
                "message", "Learning Outcome created successfully",
                "data", createdLos,
                "losId", createdLos.getId(),
                "name", createdLos.getName(),
                "description", createdLos.getDescription(),
                "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Error: " + e.getMessage(),
                "status", "ERROR"
            ));
        }
    }

    // Read All by Module ID (The main way to get Los)
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<?> getLosByModuleId(@PathVariable String moduleId, @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            java.util.List<Los> losList = losService.getLosByModuleId(moduleId);
            return ResponseEntity.ok(Map.of(
                "message", "Learning Outcomes retrieved successfully",
                "data", losList,
                "count", losList.size(),
                "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Error: " + e.getMessage(),
                "status", "ERROR"
            ));
        }
    }

    // Read One
    @GetMapping("/{id}")
    public ResponseEntity<?> getLosById(@PathVariable String id, @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            java.util.Optional<Los> los = losService.getLosById(id);
            return los.map(l -> {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Learning Outcome found");
                response.put("data", l);
                response.put("losId", l.getId());
                response.put("name", l.getName() != null ? l.getName() : "");
                response.put("description", l.getDescription() != null ? l.getDescription() : "");
                response.put("status", "SUCCESS");
                return ResponseEntity.ok(response);
            }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "message", "Learning Outcome not found",
                "status", "ERROR"
            )));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Error: " + e.getMessage(),
                "status", "ERROR"
            ));
        }
    }

    // Update (Lecture Only)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLos(@PathVariable String id, @RequestBody Los losDetails, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Access Denied: Only Lecture can update Los",
                    "status", "ERROR"
                ));
            }
            Los updatedLos = losService.updateLos(id, losDetails);
            return ResponseEntity.ok(Map.of(
                "message", "Learning Outcome updated successfully",
                "data", updatedLos,
                "losId", updatedLos.getId(),
                "name", updatedLos.getName(),
                "description", updatedLos.getDescription(),
                "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Error: " + e.getMessage(),
                "status", "ERROR"
            ));
        }
    }

    // Delete (Lecture Only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLos(@PathVariable String id, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Access Denied: Only Lecture can delete Los",
                    "status", "ERROR"
                ));
            }
            losService.deleteLos(id);
            return ResponseEntity.ok(Map.of(
                "message", "Learning Outcome deleted successfully",
                "losId", id,
                "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Error: " + e.getMessage(),
                "status", "ERROR"
            ));
        }
    }

    // Import student marks directly for a specific LO (Lecture/Admin Only)
    // Only requires Excel file and batch (batch year like 24, 25)
    @PostMapping("/{loId}/marks/import-obe")
    public ResponseEntity<?> importMarksForLo(
            @PathVariable String loId,
            @RequestParam("excelFile") MultipartFile excelFile,
            @RequestParam(value = "batch", required = true) String batch,
            @RequestParam(value = "loNumber", required = false) String loNumber,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "message", "Access Denied: Only Lecturers/Admins can import student marks",
                            "status", "ERROR"
                        ));
            }

            fileValidationService.validateExcelFile(excelFile);

            // Only batch is required now (no academicYear)
            // Verify Los exists
            Los los = losService.getLosById(loId)
                    .orElseThrow(() -> new Exception("Learning Outcome not found"));

            // Check if batch already exists for this LO
            long existingBatchCount = studentMarkRepository.countByLos_IdAndBatch(loId, batch);
            if (existingBatchCount > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "message", "Batch " + batch + " already exists for this Learning Outcome. Please use edit to update existing batch.",
                                "status", "ERROR"
                        ));
            }

            // Store file info in Los entity (batch is stored per StudentMark record, not on LO)
            los.setFileName(excelFile.getOriginalFilename());
            los.setMarksCsvFile(excelFile.getBytes());
            losService.updateLos(loId, los);

            // Import marks directly to StudentMark (pass batch to service)
            String result = excelImportService.importMarksOBEFormat(loId, excelFile, batch);

            return ResponseEntity.ok(Map.of(
                    "message", "Student marks imported successfully (LO OBE Format)",
                    "details", result,
                    "loId", loId,
                    "loNumber", loNumber == null ? "" : loNumber,
                    "batch", batch,
                    "fileName", excelFile.getOriginalFilename(),
                    "format", "2-column (Student Index, Mark)",
                    "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "message", "Failed to import student marks for LO",
                            "error", e.getMessage(),
                            "status", "ERROR"
                    ));
        }
    }

    // Update batch number for existing LO marks (Lecture/Admin Only)
    @PutMapping("/{loId}/batch/update")
    public ResponseEntity<?> updateBatch(
            @PathVariable String loId,
            @RequestBody Map<String, String> batchData,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "message", "Access Denied: Only Lecturers/Admins can update batches",
                            "status", "ERROR"
                        ));
            }

            String oldBatch = batchData.get("oldBatch");
            String newBatch = batchData.get("newBatch");

            if (oldBatch == null || newBatch == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "message", "Error: oldBatch and newBatch are required",
                            "status", "ERROR"
                        ));
            }

            // Check if new batch already exists
            long existingCount = studentMarkRepository.countByLos_IdAndBatch(loId, newBatch);
            if (existingCount > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "message", "Batch " + newBatch + " already exists for this Learning Outcome.",
                            "status", "ERROR"
                        ));
            }

            // Update all StudentMarks with oldBatch to newBatch
            List<StudentMark> marksToUpdate = studentMarkRepository.findByLos_IdAndBatch(loId, oldBatch);
            for (StudentMark mark : marksToUpdate) {
                mark.setBatch(newBatch);
                studentMarkRepository.save(mark);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Batch updated successfully from " + oldBatch + " to " + newBatch,
                    "loId", loId,
                    "oldBatch", oldBatch,
                    "newBatch", newBatch,
                    "recordsUpdated", marksToUpdate.size(),
                    "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "message", "Failed to update batch",
                            "error", e.getMessage(),
                            "status", "ERROR"
                    ));
        }
    }

    // Delete batch marks for a specific LO (Lecture/Admin Only)
    @DeleteMapping("/{loId}/batch/{batch}")
    public ResponseEntity<?> deleteBatch(
            @PathVariable String loId,
            @PathVariable String batch,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "message", "Access Denied: Only Lecturers/Admins can delete batches",
                            "status", "ERROR"
                        ));
            }

            // Get and delete all StudentMarks with this batch
            List<StudentMark> marksToDelete = studentMarkRepository.findByLos_IdAndBatch(loId, batch);
            studentMarkRepository.deleteAll(marksToDelete);

            return ResponseEntity.ok(Map.of(
                    "message", "Batch deleted successfully",
                    "loId", loId,
                    "batch", batch,
                    "recordsDeleted", marksToDelete.size(),
                    "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "message", "Failed to delete batch",
                            "error", e.getMessage(),
                            "status", "ERROR"
                    ));
        }
    }

    // Get batches with mark counts for a specific LO (batch-grouped view)
    @GetMapping("/{loId}/batches")
    public ResponseEntity<?> getBatchesByLo(
            @PathVariable String loId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (!losService.getLosById(loId).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Learning Outcome not found",
                        "status", "ERROR"
                ));
            }

            List<String> batches = studentMarkRepository.findDistinctBatchesByLosId(loId);
            List<Map<String, Object>> batchInfo = batches.stream()
                    .map(batch -> {
                        long count = studentMarkRepository.countByLos_IdAndBatch(loId, batch);
                        Map<String, Object> info = new HashMap<>();
                        info.put("batch", batch);
                        info.put("batchLabel", batch + "nd Batch"); // Format: "22nd Batch"
                        info.put("recordCount", count);
                        return info;
                    })
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "message", "Batches retrieved successfully",
                    "data", batchInfo,
                    "count", batchInfo.size(),
                    "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Error: " + e.getMessage(),
                    "status", "ERROR"
            ));
        }
    }

    // Get all uploaded marks for a specific LO (legacy - returns all marks)
    @GetMapping("/{loId}/marks")
    public ResponseEntity<?> getMarksByLo(
            @PathVariable String loId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (!losService.getLosById(loId).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Learning Outcome not found",
                        "status", "ERROR"
                ));
            }

            List<Map<String, Object>> marks = studentMarkRepository.findByLos_IdOrderByIdDesc(loId)
                    .stream()
                    .map(mark -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("id", mark.getId());
                        row.put("studentId", mark.getStudent() != null ? mark.getStudent().getStudentId() : "");
                        row.put("studentName", mark.getStudent() != null ? mark.getStudent().getStudentName() : "");
                        row.put("score", mark.getScore() != null ? mark.getScore() : 0.0);
                        return row;
                    })
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "message", "Student marks retrieved successfully",
                    "data", marks,
                    "count", marks.size(),
                    "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Error: " + e.getMessage(),
                    "status", "ERROR"
            ));
        }
    }

    // Update one student mark under an LO
    @PutMapping("/{loId}/marks/{markId}")
    public ResponseEntity<?> updateMarkByLo(
            @PathVariable String loId,
            @PathVariable Long markId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "message", "Access Denied: Only Lecturers/Admins can update marks",
                        "status", "ERROR"
                ));
            }

            StudentMark mark = studentMarkRepository.findById(markId)
                    .orElseThrow(() -> new Exception("Mark not found"));

            if (mark.getLos() == null || !loId.equals(mark.getLos().getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "message", "Mark does not belong to this LO",
                        "status", "ERROR"
                ));
            }

            Object scoreObj = body.get("score");
            if (scoreObj == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "message", "Score is required",
                        "status", "ERROR"
                ));
            }

            double score = Double.parseDouble(scoreObj.toString());
            score = Math.max(0.0, Math.min(100.0, score));
            mark.setScore(score);
            studentMarkRepository.save(mark);

            return ResponseEntity.ok(Map.of(
                    "message", "Mark updated successfully",
                    "markId", markId,
                    "score", score,
                    "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Error: " + e.getMessage(),
                    "status", "ERROR"
            ));
        }
    }

    // Delete one student mark under an LO
    @DeleteMapping("/{loId}/marks/{markId}")
    public ResponseEntity<?> deleteMarkByLo(
            @PathVariable String loId,
            @PathVariable Long markId,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "message", "Access Denied: Only Lecturers/Admins can delete marks",
                        "status", "ERROR"
                ));
            }

            StudentMark mark = studentMarkRepository.findById(markId)
                    .orElseThrow(() -> new Exception("Mark not found"));

            if (mark.getLos() == null || !loId.equals(mark.getLos().getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "message", "Mark does not belong to this LO",
                        "status", "ERROR"
                ));
            }

            studentMarkRepository.delete(mark);
            return ResponseEntity.ok(Map.of(
                    "message", "Mark deleted successfully",
                    "markId", markId,
                    "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Error: " + e.getMessage(),
                    "status", "ERROR"
            ));
        }
    }

    // Get marks for a specific batch within an LO
    @GetMapping("/{loId}/batches/{batch}/marks")
    public ResponseEntity<?> getMarksByLoBatch(
            @PathVariable String loId,
            @PathVariable String batch,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (!losService.getLosById(loId).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Learning Outcome not found",
                        "status", "ERROR"
                ));
            }

            List<Map<String, Object>> marks = studentMarkRepository.findByLos_IdAndBatch(loId, batch)
                    .stream()
                    .map(mark -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("id", mark.getId());
                        row.put("studentId", mark.getStudent() != null ? mark.getStudent().getStudentId() : "");
                        row.put("studentName", mark.getStudent() != null ? mark.getStudent().getStudentName() : "");
                        row.put("score", mark.getScore() != null ? mark.getScore() : 0.0);
                        row.put("batch", mark.getBatch());
                        return row;
                    })
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "message", "Batch marks retrieved successfully",
                    "data", marks,
                    "batch", batch,
                    "count", marks.size(),
                    "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Error: " + e.getMessage(),
                    "status", "ERROR"
            ));
        }
    }

    // Delete all marks for a specific batch within an LO
    @DeleteMapping("/{loId}/batches/{batch}")
    public ResponseEntity<?> deleteBatchByLo(
            @PathVariable String loId,
            @PathVariable String batch,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "message", "Access Denied: Only Lecturers/Admins can delete batch marks",
                        "status", "ERROR"
                ));
            }

            if (!losService.getLosById(loId).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Learning Outcome not found",
                        "status", "ERROR"
                ));
            }

            long count = studentMarkRepository.countByLos_IdAndBatch(loId, batch);
            studentMarkRepository.deleteByLos_IdAndBatch(loId, batch);

            return ResponseEntity.ok(Map.of(
                    "message", "Batch deleted successfully",
                    "batch", batch,
                    "deletedCount", count,
                    "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Error: " + e.getMessage(),
                    "status", "ERROR"
            ));
        }
    }

    /**
     * Export an Excel sheet containing student marks per LO and pass/fail info.
     * Request JSON shape explained in ExcelExportRequest class below.
     */
    @PostMapping(value = "/export-excel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> exportExcel(@RequestBody ExcelExportRequest request) throws Exception {
        // validate
        if (request.getLos() == null || request.getLos().isEmpty()) {
            throw new IllegalArgumentException("'los' (list of LO titles) must be provided and not empty");
        }
        int loCount = request.getLos().size();
        if (request.getStudents() == null) request.setStudents(new ArrayList<>());

        double threshold = request.getPassThreshold() != null ? request.getPassThreshold() : 50.0;

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("LO Results");

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        int col = 0;
        header.createCell(col++).setCellValue("Index No");
        // LO columns
        for (String lo : request.getLos()) {
            header.createCell(col++).setCellValue(lo);
        }
        // pass/fail columns for each LO
        for (String lo : request.getLos()) {
            header.createCell(col++).setCellValue(lo + " Pass");
        }
        header.createCell(col++).setCellValue("Passed LO Count");
        header.createCell(col++).setCellValue("All Passed");

        for (ExcelStudent s : request.getStudents()) {
            Row r = sheet.createRow(rowIdx++);
            int c = 0;
            r.createCell(c++).setCellValue(s.getIndexNo() == null ? "" : s.getIndexNo());
            List<Double> marks = s.getMarks() != null ? s.getMarks() : new ArrayList<>();
            // marks per LO
            for (int i = 0; i < loCount; i++) {
                double m = i < marks.size() && marks.get(i) != null ? marks.get(i) : 0d;
                r.createCell(c++).setCellValue(m);
            }
            // pass/fail per LO
            int passedCount = 0;
            for (int i = 0; i < loCount; i++) {
                double m = i < marks.size() && marks.get(i) != null ? marks.get(i) : 0d;
                boolean pass = m >= threshold;
                if (pass) passedCount++;
                r.createCell(c++).setCellValue(pass ? "Pass" : "Fail");
            }
            r.createCell(c++).setCellValue(passedCount);
            r.createCell(c++).setCellValue(passedCount == loCount ? "Yes" : "No");
        }

        // autosize
        for (int i = 0; i < col; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();

        byte[] bytes = out.toByteArray();
        String fileName = "lo-results.xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    // DTOs for export endpoint
    public static class ExcelExportRequest {
        private String markType; // e.g. "final" or "assignment"
        private List<String> los;
        private List<ExcelStudent> students;
        private Double passThreshold; // default 50

        public ExcelExportRequest() {}
        public String getMarkType() { return markType; }
        public void setMarkType(String markType) { this.markType = markType; }
        public List<String> getLos() { return los; }
        public void setLos(List<String> los) { this.los = los; }
        public List<ExcelStudent> getStudents() { return students; }
        public void setStudents(List<ExcelStudent> students) { this.students = students; }
        public Double getPassThreshold() { return passThreshold; }
        public void setPassThreshold(Double passThreshold) { this.passThreshold = passThreshold; }
    }

    public static class ExcelStudent {
        private String indexNo;
        private List<Double> marks;
        public ExcelStudent() {}
        public String getIndexNo() { return indexNo; }
        public void setIndexNo(String indexNo) { this.indexNo = indexNo; }
        public List<Double> getMarks() { return marks; }
        public void setMarks(List<Double> marks) { this.marks = marks; }
    }

    private boolean isLecture(String token) {
        try {
            String bearerToken = token;
            if (token != null && token.startsWith("Bearer ")) {
                bearerToken = token.substring(7);
            }
            String role = jwtUtil.extractRole(bearerToken);
            role = role == null ? null : role.trim().toLowerCase();
            return role != null && ("lecture".equals(role) || "lecturer".equals(role) || "admin".equals(role) || "superadmin".equals(role));
        } catch (Exception e) {
            return false;
        }
    }
}
