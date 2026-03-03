package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.Los;
import com.example.Software.project.Backend.Model.Assignment;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.AssignmentService;
import com.example.Software.project.Backend.Service.LosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private AssignmentService assignmentService;

    @Autowired
    private JwtUtil jwtUtil;

    // Create (Lecture Only) - Add to Module
    @PostMapping("/{moduleId}/add")
    public ResponseEntity<?> addLos(@PathVariable String moduleId, @RequestBody Los los, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecture can add Los");
            }
            Los createdLos = losService.addLosToModule(moduleId, los);
            return ResponseEntity.ok(createdLos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Read All by Module ID (The main way to get Los)
    @GetMapping("/module/{moduleId}")
    public List<Los> getLosByModuleId(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        return losService.getLosByModuleId(moduleId);
    }

    // Read One
    @GetMapping("/{id}")
    public ResponseEntity<?> getLosById(@PathVariable String id, @RequestHeader("Authorization") String token) {
        Optional<Los> los = losService.getLosById(id);
        return los.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update (Lecture Only)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLos(@PathVariable String id, @RequestBody Los losDetails, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecture can update Los");
            }
            Los updatedLos = losService.updateLos(id, losDetails);
            return ResponseEntity.ok(updatedLos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Delete (Lecture Only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLos(@PathVariable String id, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecture can delete Los");
            }
            losService.deleteLos(id);
            return ResponseEntity.ok("Los deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Import student marks directly for a specific LO (Lecture/Admin Only)
    @PostMapping("/{loId}/marks/import-obe")
    public ResponseEntity<?> importMarksForLo(
            @PathVariable String loId,
            @RequestParam("excelFile") MultipartFile excelFile,
            @RequestParam(value = "batch", required = false) String batch,
            @RequestParam(value = "academicYear", required = false) String academicYear,
            @RequestParam(value = "loNumber", required = false) String loNumber,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access Denied: Only Lecturers/Admins can import student marks");
            }

            if (excelFile == null || excelFile.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Error: Excel file is required");
            }

            Assignment assignment = assignmentService.resolveOrCreateAssignmentForLos(loId, batch, academicYear);
            String result = assignmentService.importMarksFromExcelOBEFormat(assignment.getAssignmentId(), excelFile);
            String assignmentId = assignment.getAssignmentId();

            return ResponseEntity.ok(Map.of(
                    "message", "Student marks imported successfully (LO OBE Format)",
                    "details", result,
                    "loId", loId,
                    "loNumber", loNumber == null ? "" : loNumber,
                    "assignmentId", assignmentId,
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
