package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.User;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class UserRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User loginUser) {
        try {
            // Authenticate using Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginUser.getUserID(), loginUser.getPassword())
            );

            // If authentication is successful, generate JWT
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Optional<User> userOptional = userService.findByUserId(userDetails.getUsername());

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                String userType = user.getUsertype();

                // Validate that user has a valid role
                if (userType == null || userType.trim().isEmpty()) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "User role is not set. Contact administrator.");
                    errorResponse.put("status", "ERROR");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                // Validate that usertype is one of the allowed roles
                String normalizedType = userType.toLowerCase().trim();
                if (!normalizedType.equals("admin") && !normalizedType.equals("lecture") && !normalizedType.equals("superadmin")) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "Invalid user role: " + userType + ". Allowed roles are: Admin, Lecture, Superadmin");
                    errorResponse.put("status", "ERROR");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                // Normalize usertype to lowercase and trim spaces for consistency
                if (userType != null) {
                    userType = userType.toLowerCase().trim();
                }

                String token = jwtUtil.generateToken(user.getUserID(), userType);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("userId", user.getUserID());
                response.put("email", user.getEmail());
                response.put("userType", userType);
                response.put("token", token); // Send token to client
                response.put("status", "SUCCESS");

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not found"));
            }

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid username or password");
            errorResponse.put("status", "ERROR");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @PostMapping("/add-admin")
    public ResponseEntity<?> addAdmin(@RequestBody User newUser, @RequestHeader("Authorization") String token) {
        try {
            // Only superadmin can add admins
            if (!isSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access Denied: Only Superadmin can add admins", "status", "ERROR"));
            }

            // Ensure new user is being created as admin
            if (newUser.getUsertype() == null || !newUser.getUsertype().toLowerCase().equals("admin")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: New user must be of type 'admin'", "status", "ERROR"));
            }

            // Get the currently authenticated superadmin user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String creatorUsername = authentication.getName();

            User createdUser = userService.addUser(newUser, creatorUsername);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Admin user added successfully");
            response.put("userId", createdUser.getUserID());
            response.put("email", createdUser.getEmail());
            response.put("userType", "admin");
            response.put("status", "SUCCESS");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to add admin: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/add-lecture")
    public ResponseEntity<?> addLecture(@RequestBody User newUser, @RequestHeader("Authorization") String token) {
        try {
            // Only admin/superadmin can add lectures
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access Denied: Only Admin can add lecturers", "status", "ERROR"));
            }

            // Ensure new user is being created as lecture
            if (newUser.getUsertype() == null || !newUser.getUsertype().toLowerCase().equals("lecture")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: New user must be of type 'lecture'", "status", "ERROR"));
            }

            // Get the currently authenticated admin user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String creatorUsername = authentication.getName();

            User createdUser = userService.addUser(newUser, creatorUsername);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Lecturer user added successfully");
            response.put("userId", createdUser.getUserID());
            response.put("email", createdUser.getEmail());
            response.put("userType", "lecture");
            response.put("status", "SUCCESS");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to add lecturer: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/add-user")
    public ResponseEntity<?> addUser(@RequestBody User newUser, @RequestHeader("Authorization") String token) {
        try {
            String requestedType = newUser.getUsertype() == null ? "" : newUser.getUsertype().toLowerCase().trim();

            if ("admin".equals(requestedType) && !isSuperAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access Denied: Only Superadmin can add admins", "status", "ERROR"));
            }

            if ("lecture".equals(requestedType) && !isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access Denied: Only Admin can add lecturers", "status", "ERROR"));
            }

            if (!"admin".equals(requestedType) && !"lecture".equals(requestedType)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: userType must be 'admin' or 'lecture'", "status", "ERROR"));
            }

            // Get the currently authenticated user from the SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String creatorUsername = authentication.getName(); // This is the username from the JWT

            User createdUser = userService.addUser(newUser, creatorUsername);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User added successfully");
            response.put("userId", createdUser.getUserID());
            response.put("email", createdUser.getEmail());
            response.put("userType", createdUser.getUsertype());
            response.put("status", "SUCCESS");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to add user: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/debug/user/{username}")
    public ResponseEntity<?> debugGetUser(@PathVariable String username) {
        try {
            Optional<User> userOptional = userService.findByUserId(username);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                Map<String, Object> response = new HashMap<>();
                response.put("userId", user.getUserID());
                response.put("email", user.getEmail());
                response.put("userType", user.getUsertype());
                response.put("userTypeRaw", user.getUsertype());
                response.put("status", "SUCCESS");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found", "status", "ERROR"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    @PostMapping("/create-test-user")
    public ResponseEntity<?> createTestUser() {
        try {
            User testUser = userService.createTestUser("admin", "password123", "admin@test.com", "admin");
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Test user created successfully");
            response.put("userId", testUser.getUserID());
            response.put("email", testUser.getEmail());
            response.put("userType", testUser.getUsertype());
            response.put("status", "SUCCESS");
            response.put("loginInfo", "You can now login with username: admin, password: password123");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to create test user: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    private boolean isSuperAdmin(String token) {
        try {
            String bearerToken = token;
            if (token != null && token.startsWith("Bearer ")) {
                bearerToken = token.substring(7);
            }
            String role = jwtUtil.extractRole(bearerToken);
            role = role == null ? null : role.trim().toLowerCase();
            return role != null && role.equals("superadmin");
        } catch (Exception e) {
            return false;
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
