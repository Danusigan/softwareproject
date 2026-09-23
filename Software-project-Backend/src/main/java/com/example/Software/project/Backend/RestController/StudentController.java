package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.Student;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.FileValidationService;
import com.example.Software.project.Backend.Service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Admin-only student roster management: bulk import from Excel (so marks upload, PO credit
 * calculation and the batch PO report all have real batch data to work with - see
 * StudentService), a blank template to fill in, and a listing to confirm what's in the system.
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private FileValidationService fileValidationService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                     @RequestHeader("Authorization") String token) {
        if (!isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin only", "status", "ERROR"));
        }
        try {
            fileValidationService.validateExcelFile(file);
            Map<String, Object> result = studentService.importFromExcel(file);
            return ResponseEntity.ok(Map.of(
                    "message", "Imported " + result.get("totalRows") + " student(s) - " +
                            result.get("created") + " created, " + result.get("updated") + " updated.",
                    "status", "SUCCESS", "data", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "status", "ERROR"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Could not process the uploaded file: " + e.getMessage(), "status", "ERROR"));
        }
    }

    @GetMapping("/template")
    public ResponseEntity<?> template(@RequestHeader("Authorization") String token) {
        if (!isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin only", "status", "ERROR"));
        }
        try {
            byte[] bytes = studentService.generateTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"student_upload_template.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Could not generate the template: " + e.getMessage(), "status", "ERROR"));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String batch,
                                   @RequestParam(required = false) String academicYear,
                                   @RequestHeader("Authorization") String token) {
        if (!isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin only", "status", "ERROR"));
        }
        List<Student> students = studentService.list(batch, academicYear);
        return ResponseEntity.ok(Map.of("message", "OK", "status", "SUCCESS", "data", students));
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
}
