package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.LosPos;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.LosPosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lospos")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class LosPosRestController {

    @Autowired
    private LosPosService losPosService;

    @Autowired
    private JwtUtil jwtUtil;

    // Create (Lecture Only) - Add to Module
    @PostMapping("/{moduleId}/add")
    public ResponseEntity<?> addLosPos(@PathVariable String moduleId, @RequestBody LosPos losPos, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecture can add LosPos");
            }
            LosPos createdLosPos = losPosService.addLosPosToModule(moduleId, losPos);
            return ResponseEntity.ok(createdLosPos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Read All by Module ID (The main way to get LosPos)
    @GetMapping("/module/{moduleId}")
    public List<LosPos> getLosPosByModuleId(@PathVariable String moduleId, @RequestHeader("Authorization") String token) {
        return losPosService.getLosPosByModuleId(moduleId);
    }

    // Read One
    @GetMapping("/{id}")
    public ResponseEntity<?> getLosPosById(@PathVariable String id, @RequestHeader("Authorization") String token) {
        Optional<LosPos> losPos = losPosService.getLosPosById(id);
        return losPos.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update (Lecture Only)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLosPos(@PathVariable String id, @RequestBody LosPos losPosDetails, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecture can update LosPos");
            }
            LosPos updatedLosPos = losPosService.updateLosPos(id, losPosDetails);
            return ResponseEntity.ok(updatedLosPos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Delete (Lecture Only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLosPos(@PathVariable String id, @RequestHeader("Authorization") String token) {
        try {
            if (!isLecture(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Lecture can delete LosPos");
            }
            losPosService.deleteLosPos(id);
            return ResponseEntity.ok("LosPos deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    private boolean isLecture(String token) {
        String role = jwtUtil.extractRole(token.substring(7));
        return "Lecture".equalsIgnoreCase(role) || "Admin".equalsIgnoreCase(role) || "Superadmin".equalsIgnoreCase(role);
    }
}