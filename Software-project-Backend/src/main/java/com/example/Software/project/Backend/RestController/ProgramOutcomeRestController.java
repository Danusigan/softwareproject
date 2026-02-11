package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.ProgramOutcome;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.ProgramOutcomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/program-outcomes")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ProgramOutcomeRestController {

    @Autowired
    private ProgramOutcomeService programOutcomeService;

    @Autowired
    private JwtUtil jwtUtil;

    // Get all active Program Outcomes (All authenticated users)
    @GetMapping
    public ResponseEntity<List<ProgramOutcome>> getAllActiveProgramOutcomes(@RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<ProgramOutcome> programOutcomes = programOutcomeService.getAllActiveProgramOutcomes();
            return ResponseEntity.ok(programOutcomes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get all Program Outcomes including inactive (Admin/SuperAdmin only)
    @GetMapping("/all")
    public ResponseEntity<List<ProgramOutcome>> getAllProgramOutcomes(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
            
            List<ProgramOutcome> programOutcomes = programOutcomeService.getAllProgramOutcomes();
            return ResponseEntity.ok(programOutcomes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get Program Outcome by ID (All authenticated users)
    @GetMapping("/{id}")
    public ResponseEntity<ProgramOutcome> getProgramOutcomeById(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Optional<ProgramOutcome> programOutcome = programOutcomeService.getProgramOutcomeById(id);
            return programOutcome.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get Program Outcome by code (All authenticated users)
    @GetMapping("/code/{poCode}")
    public ResponseEntity<ProgramOutcome> getProgramOutcomeByCode(@PathVariable String poCode, @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Optional<ProgramOutcome> programOutcome = programOutcomeService.getProgramOutcomeByCode(poCode);
            return programOutcome.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Create Program Outcome (Admin/SuperAdmin only)
    @PostMapping
    public ResponseEntity<?> createProgramOutcome(
            @RequestParam("poCode") String poCode,
            @RequestParam("poDescription") String poDescription,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admin or SuperAdmin can create Program Outcomes");
            }

            String userEmail = jwtUtil.extractUsername(token.substring(7));
            ProgramOutcome programOutcome = programOutcomeService.createProgramOutcome(poCode, poDescription, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(programOutcome);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Update Program Outcome (Admin/SuperAdmin only)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProgramOutcome(
            @PathVariable Long id,
            @RequestParam("poDescription") String poDescription,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admin or SuperAdmin can update Program Outcomes");
            }

            ProgramOutcome updatedProgramOutcome = programOutcomeService.updateProgramOutcome(id, poDescription);
            return ResponseEntity.ok(updatedProgramOutcome);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Deactivate Program Outcome (Admin/SuperAdmin only)
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateProgramOutcome(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admin or SuperAdmin can deactivate Program Outcomes");
            }

            programOutcomeService.deactivateProgramOutcome(id);
            return ResponseEntity.ok("Program Outcome deactivated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Activate Program Outcome (Admin/SuperAdmin only)
    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateProgramOutcome(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdminOrSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Admin or SuperAdmin can activate Program Outcomes");
            }

            programOutcomeService.activateProgramOutcome(id);
            return ResponseEntity.ok("Program Outcome activated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Delete Program Outcome (SuperAdmin only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProgramOutcome(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            if (!isSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only SuperAdmin can delete Program Outcomes");
            }

            programOutcomeService.deleteProgramOutcome(id);
            return ResponseEntity.ok("Program Outcome deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Initialize default Washington Accord Program Outcomes (SuperAdmin only)
    @PostMapping("/initialize-defaults")
    public ResponseEntity<?> initializeDefaultProgramOutcomes(@RequestHeader("Authorization") String token) {
        try {
            if (!isSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only SuperAdmin can initialize default Program Outcomes");
            }

            String userEmail = jwtUtil.extractUsername(token.substring(7));
            List<ProgramOutcome> defaultPOs = programOutcomeService.initializeDefaultProgramOutcomes(userEmail);
            
            if (defaultPOs.isEmpty()) {
                return ResponseEntity.ok("Default Program Outcomes already exist. No new POs were created.");
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(
                String.format("Successfully initialized %d default Program Outcomes based on Washington Accord Graduate Attributes", 
                    defaultPOs.size())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Get default Program Outcomes (All authenticated users)
    @GetMapping("/defaults")
    public ResponseEntity<List<ProgramOutcome>> getDefaultProgramOutcomes(@RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<ProgramOutcome> defaultPOs = programOutcomeService.getDefaultProgramOutcomes();
            return ResponseEntity.ok(defaultPOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get custom Program Outcomes (All authenticated users)
    @GetMapping("/custom")
    public ResponseEntity<List<ProgramOutcome>> getCustomProgramOutcomes(@RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<ProgramOutcome> customPOs = programOutcomeService.getCustomProgramOutcomes();
            return ResponseEntity.ok(customPOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Search Program Outcomes (All authenticated users)
    @GetMapping("/search")
    public ResponseEntity<List<ProgramOutcome>> searchProgramOutcomes(
            @RequestParam("query") String searchTerm,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<ProgramOutcome> results = programOutcomeService.searchProgramOutcomes(searchTerm);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get Program Outcomes by module (All authenticated users)
    @GetMapping("/module/{moduleCode}")
    public ResponseEntity<List<ProgramOutcome>> getProgramOutcomesByModule(
            @PathVariable String moduleCode,
            @RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<ProgramOutcome> modulePOs = programOutcomeService.getProgramOutcomesByModule(moduleCode);
            return ResponseEntity.ok(modulePOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get Program Outcomes with approved mappings (All authenticated users)
    @GetMapping("/with-mappings")
    public ResponseEntity<List<ProgramOutcome>> getProgramOutcomesWithApprovedMappings(@RequestHeader("Authorization") String token) {
        try {
            if (!isAuthenticated(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<ProgramOutcome> activePOs = programOutcomeService.getProgramOutcomesWithApprovedMappings();
            return ResponseEntity.ok(activePOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Authorization helper methods
    private boolean isAuthenticated(String token) {
        try {
            String role = jwtUtil.extractRole(token.substring(7));
            return role != null && !role.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAdminOrSuperAdmin(String token) {
        try {
            String role = jwtUtil.extractRole(token.substring(7));
            return "Admin".equalsIgnoreCase(role) || "Superadmin".equalsIgnoreCase(role);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSuperAdmin(String token) {
        try {
            String role = jwtUtil.extractRole(token.substring(7));
            return "Superadmin".equalsIgnoreCase(role);
        } catch (Exception e) {
            return false;
        }
    }
}