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
                
                // Normalize usertype to lowercase for consistency
                if (userType != null) {
                    userType = userType.toLowerCase();
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

    @PostMapping("/add-user")
    public ResponseEntity<?> addUser(@RequestBody User newUser) {
        try {
            // Get the currently authenticated user from the SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String creatorUsername = authentication.getName(); // This is the username from the JWT

            User createdUser = userService.addUser(newUser, creatorUsername);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User added successfully");
            response.put("userId", createdUser.getUserID());
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
}