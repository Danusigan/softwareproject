package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.Assignment;
import com.example.Software.project.Backend.Model.LosPos;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.AssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AssignmentRestController {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private JwtUtil jwtUtil;

    // Create (Lecture Only) - Add to LosPos
    @PostMapping("/{losPosId}/add")
    public ResponseEntity<?> addAssignment(
            @PathVariable String losPosId,
            @RequestParam("assignmentId") String assignmentId,
            @RequestParam("assignmentName") String assignmentName,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecture can add Assignments");
            }

            Assignment assignment = new Assignment();
            assignment.setAssignmentId(assignmentId);
            assignment.setAssignmentName(assignmentName);

            Assignment addedAssignment = assignmentService.addAssignmentToLosPos(losPosId, assignment, file);
            return ResponseEntity.ok(addedAssignment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Read All (Global)
    @GetMapping("/all")
    public ResponseEntity<List<Assignment>> getAllAssignments(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(assignmentService.getAllAssignments());
    }

    // Read Assignment by LosPos ID
    @GetMapping("/lospos/{losPosId}")
    public ResponseEntity<?> getAssignmentByLosPosId(@PathVariable String losPosId, @RequestHeader("Authorization") String token) {
        Optional<Assignment> assignment = assignmentService.getAssignmentByLosPosId(losPosId);
        return assignment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Read One by Assignment ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getAssignmentById(@PathVariable String id, @RequestHeader("Authorization") String token) {
        Optional<Assignment> assignment = assignmentService.getAssignmentById(id);
        return assignment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update (Lecture Only)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAssignment(
            @PathVariable String id,
            @RequestParam(value = "assignmentName", required = false) String assignmentName,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecture can update Assignments");
            }
            Assignment updatedAssignment = assignmentService.updateAssignment(id, assignmentName, file);
            return ResponseEntity.ok(updatedAssignment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Delete (Lecture Only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAssignment(@PathVariable String id, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecture can delete Assignments");
            }
            assignmentService.deleteAssignment(id);
            return ResponseEntity.ok("Assignment deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Import student marks from Excel/CSV file (Admin/Lecture Only)
    @PostMapping("/{assignmentId}/import-marks")
    public ResponseEntity<?> importStudentMarks(
            @PathVariable String assignmentId,
            @RequestParam("excelFile") MultipartFile excelFile,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access Denied: Only Lecturers/Admins can import student marks");
            }

            if (excelFile.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Excel file is required");
            }

            String result = assignmentService.importMarksFromExcel(assignmentId, excelFile);
            return ResponseEntity.ok(Map.of(
                "message", "Student marks imported successfully",
                "details", result,
                "assignmentId", assignmentId,
                "fileName", excelFile.getOriginalFilename(),
                "format", "3-column (Student ID, Name, Mark)",
                "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "message", "Failed to import student marks",
                    "error", e.getMessage(),
                    "status", "ERROR"
                ));
        }
    }

    // Import student marks from Excel using OBE format (Admin/Lecture Only)
    @PostMapping("/{assignmentId}/import-marks-obe")
    public ResponseEntity<?> importStudentMarksOBE(
            @PathVariable String assignmentId,
            @RequestParam("excelFile") MultipartFile excelFile,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access Denied: Only Lecturers/Admins can import student marks");
            }

            if (excelFile.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Excel file is required");
            }

            String result = assignmentService.importMarksFromExcelOBEFormat(assignmentId, excelFile);
            return ResponseEntity.ok(Map.of(
                "message", "Student marks imported successfully (OBE Format)",
                "details", result,
                "assignmentId", assignmentId,
                "fileName", excelFile.getOriginalFilename(),
                "format", "2-column (Student Index, Mark)",
                "dataCleaning", "Non-numeric values set to 0.0, marks clamped 0-100",
                "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "message", "Failed to import student marks (OBE Format)",
                    "error", e.getMessage(),
                    "status", "ERROR"
                ));
        }
    }

    private boolean isLecture(String token) {
        String role = jwtUtil.extractRole(token.substring(7));
        return "lecture".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role) || "superadmin".equalsIgnoreCase(role);
    }
}