package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.Los;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.LosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lospos") // Kept as requested
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class LosRestController {

    @Autowired
    private LosService losService;

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

    private boolean isLecture(String token) {
        String role = jwtUtil.extractRole(token.substring(7));
        return "Lecture".equalsIgnoreCase(role) || "Admin".equalsIgnoreCase(role) || "Superadmin".equalsIgnoreCase(role);
    }
}
