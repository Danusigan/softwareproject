package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.User;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.ModuleService;
import com.example.Software.project.Backend.Service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for UserRestController: login's role validation (a user with no/invalid
 * usertype must not get a token), and the role gate on the admin/superadmin-only user
 * management endpoints. Like the other controllers here, roles are checked manually via
 * JwtUtil rather than @PreAuthorize, so the Spring Security filter chain is disabled
 * (addFilters = false) and the endpoints are hit directly.
 *
 * add-admin/add-lecture's *success* paths are intentionally not covered here: they read
 * the creator's username from SecurityContextHolder, which nothing populates once the
 * filter chain is disabled. Their 403/400 rejection paths run before that line, so those
 * are covered instead.
 */
@WebMvcTest(controllers = UserRestController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserRestController login and access-control tests")
class UserRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private ModuleService moduleService;
    @MockBean private AuthenticationManager authenticationManager;
    @MockBean private JwtUtil jwtUtil;

    private org.springframework.security.core.userdetails.User principalFor(String username) {
        return new org.springframework.security.core.userdetails.User(
            username, "irrelevant", List.of(new SimpleGrantedAuthority("lecture")));
    }

    // ---- login ----

    @Test
    @DisplayName("login succeeds and returns a token for a user with a valid role")
    void login_succeedsForValidRole() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principalFor("lecturer1"));
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        User user = new User("lecturer1", "lecturer1@example.com", "hashed", "lecture");
        when(userService.findByUserId("lecturer1")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("lecturer1", "lecture")).thenReturn("signed.jwt.token");

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"userID\": \"lecturer1\", \"password\": \"pw\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.token").value("signed.jwt.token"))
            .andExpect(jsonPath("$.userType").value("lecture"));
    }

    @Test
    @DisplayName("login rejects bad credentials with 401 without leaking whether the user exists")
    void login_rejectsBadCredentials() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad creds"));

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"userID\": \"ghost\", \"password\": \"wrong\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("login rejects a user whose usertype is blank")
    void login_rejectsBlankUserType() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principalFor("lecturer1"));
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        User user = new User("lecturer1", "lecturer1@example.com", "hashed", "  ");
        when(userService.findByUserId("lecturer1")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"userID\": \"lecturer1\", \"password\": \"pw\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("User role is not set. Contact administrator."));
    }

    @Test
    @DisplayName("login rejects a user whose usertype is not one of admin/lecture/superadmin")
    void login_rejectsInvalidUserType() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principalFor("weird1"));
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        User user = new User("weird1", "weird1@example.com", "hashed", "hacker");
        when(userService.findByUserId("weird1")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"userID\": \"weird1\", \"password\": \"pw\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid user role: hacker. Allowed roles are: Admin, Lecture, Superadmin"));
    }

    // ---- role gate on management endpoints ----

    @Test
    @DisplayName("GET /lecturers is rejected with 403 for a lecturer token")
    void getLecturers_rejectsLecturerToken() throws Exception {
        when(jwtUtil.extractRole("lecturer.jwt")).thenReturn("lecture");

        mockMvc.perform(get("/api/auth/lecturers").header("Authorization", "Bearer lecturer.jwt"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /lecturers succeeds for an admin token")
    void getLecturers_allowsAdminToken() throws Exception {
        when(jwtUtil.extractRole("admin.jwt")).thenReturn("admin");
        User lecturer = new User("lect1", "lect1@example.com", "hashed", "lecture");
        when(userService.findAllLecturers()).thenReturn(List.of(lecturer));
        when(moduleService.getModuleIdsAssignedTo("lect1")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/auth/lecturers").header("Authorization", "Bearer admin.jwt"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data[0].username").value("lect1"));
    }

    @Test
    @DisplayName("GET /admins is rejected with 403 for a plain admin token (superadmin-only)")
    void getAdmins_rejectsAdminToken() throws Exception {
        when(jwtUtil.extractRole("admin.jwt")).thenReturn("admin");

        mockMvc.perform(get("/api/auth/admins").header("Authorization", "Bearer admin.jwt"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admins succeeds for a superadmin token")
    void getAdmins_allowsSuperAdminToken() throws Exception {
        when(jwtUtil.extractRole("superadmin.jwt")).thenReturn("superadmin");
        when(userService.findAllAdmins()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/auth/admins").header("Authorization", "Bearer superadmin.jwt"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /add-admin is rejected with 403 for a plain admin token (superadmin-only)")
    void addAdmin_rejectsAdminToken() throws Exception {
        when(jwtUtil.extractRole("admin.jwt")).thenReturn("admin");

        mockMvc.perform(post("/api/auth/add-admin")
                .header("Authorization", "Bearer admin.jwt")
                .contentType("application/json")
                .content("{\"userID\": \"newadmin\", \"usertype\": \"admin\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /add-admin rejects with 400 when the payload's usertype isn't 'admin'")
    void addAdmin_rejectsWrongUserType() throws Exception {
        when(jwtUtil.extractRole("superadmin.jwt")).thenReturn("superadmin");

        mockMvc.perform(post("/api/auth/add-admin")
                .header("Authorization", "Bearer superadmin.jwt")
                .contentType("application/json")
                .content("{\"userID\": \"newlecture\", \"usertype\": \"lecture\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Error: New user must be of type 'admin'"));
    }
}
