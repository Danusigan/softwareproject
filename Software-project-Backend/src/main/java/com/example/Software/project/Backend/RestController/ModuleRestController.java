package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.ModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admin can create modules");
            }
            com.example.Software.project.Backend.Model.Module createdModule = moduleService.createModule(module);
            return ResponseEntity.ok(createdModule);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Read All (Public/Auth)
    @GetMapping("/all")
    public List<com.example.Software.project.Backend.Model.Module> getAllModules() {
        return moduleService.getAllModules();
    }

    // Read One
    @GetMapping("/{id}")
    public ResponseEntity<?> getModuleById(@PathVariable String id) {
        Optional<com.example.Software.project.Backend.Model.Module> module = moduleService.getModuleById(id);
        return module.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update (Admin Only)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateModule(@PathVariable String id, @RequestBody com.example.Software.project.Backend.Model.Module moduleDetails, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admin can update modules");
            }
            com.example.Software.project.Backend.Model.Module updatedModule = moduleService.updateModule(id, moduleDetails);
            return ResponseEntity.ok(updatedModule);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Delete (Admin Only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteModule(@PathVariable String id, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admin can delete modules");
            }
            moduleService.deleteModule(id);
            return ResponseEntity.ok("Module deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    private boolean isAdmin(String token) {
        String role = jwtUtil.extractRole(token.substring(7));
        return "Admin".equalsIgnoreCase(role) || "Superadmin".equalsIgnoreCase(role);
    }
}