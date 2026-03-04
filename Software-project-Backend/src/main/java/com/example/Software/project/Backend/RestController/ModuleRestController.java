package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.ModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/modules")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ModuleRestController {

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private JwtUtil jwtUtil;

    // Create (Admin Only)
    @PostMapping("/create")
    public ResponseEntity<?> createModule(@RequestBody com.example.Software.project.Backend.Model.Module module, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Access Denied: Only Admin can create modules",
                    "status", "ERROR"
                ));
            }
            com.example.Software.project.Backend.Model.Module createdModule = moduleService.createModule(module);
            return ResponseEntity.ok(Map.of(
                "message", "Module created successfully",
                "data", createdModule,
                "moduleId", createdModule.getModuleId(),
                "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Error: " + e.getMessage(),
                "status", "ERROR"
            ));
        }
    }

    // Read All (Public/Auth)
    @GetMapping("/all")
    public ResponseEntity<?> getAllModules(@RequestHeader("Authorization") String token) {
        try {
            return ResponseEntity.ok(Map.of(
                "message", "All modules retrieved successfully",
                "data", moduleService.getAllModules(),
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
    public ResponseEntity<?> getModuleById(@PathVariable String id, @RequestHeader("Authorization") String token) {
        try {
            java.util.Optional<com.example.Software.project.Backend.Model.Module> module = moduleService.getModuleById(id);
            return module.map(m -> ResponseEntity.ok(Map.of(
                "message", "Module found",
                "data", m,
                "moduleId", m.getModuleId(),
                "status", "SUCCESS"
            ))).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "message", "Module not found",
                "status", "ERROR"
            )));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Error: " + e.getMessage(),
                "status", "ERROR"
            ));
        }
    }

    // Update (Admin Only)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateModule(@PathVariable String id, @RequestBody com.example.Software.project.Backend.Model.Module moduleDetails, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Access Denied: Only Admin can update modules",
                    "status", "ERROR"
                ));
            }
            com.example.Software.project.Backend.Model.Module updatedModule = moduleService.updateModule(id, moduleDetails);
            return ResponseEntity.ok(Map.of(
                "message", "Module updated successfully",
                "data", updatedModule,
                "moduleId", updatedModule.getModuleId(),
                "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Error: " + e.getMessage(),
                "status", "ERROR"
            ));
        }
    }

    // Delete (Admin Only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteModule(@PathVariable String id, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Access Denied: Only Admin can delete modules",
                    "status", "ERROR"
                ));
            }
            moduleService.deleteModule(id);
            return ResponseEntity.ok(Map.of(
                "message", "Module deleted successfully",
                "moduleId", id,
                "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Error: " + e.getMessage(),
                "status", "ERROR"
            ));
        }
    }

    private boolean isAdmin(String token) {
        try {
            String bearerToken = token;
            if (token != null && token.startsWith("Bearer ")) {
                bearerToken = token.substring(7);
            }
            String role = jwtUtil.extractRole(bearerToken);
            role = role == null ? null : role.trim().toLowerCase();
            return role != null && ("admin".equals(role) || "superadmin".equals(role));
        } catch (Exception e) {
            return false;
        }
    }
}