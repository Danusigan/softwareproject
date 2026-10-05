package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.PasswordResetToken;
import com.example.Software.project.Backend.Model.User;
import com.example.Software.project.Backend.Repository.PasswordResetTokenRepository;
import com.example.Software.project.Backend.Repository.UserAccessRepository;
import com.example.Software.project.Backend.Repository.UserRepository;
import com.example.Software.project.Backend.Security.JwtRequestFilter;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Security.SecurityConfig;
import com.example.Software.project.Backend.Service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real controllers, services, BCrypt and security filters; only storage and side effects are mocked. */
@WebMvcTest(UserRestController.class)
@Import({SecurityConfig.class, JwtRequestFilter.class, JwtUtil.class,
        CustomUserDetailsService.class, UserService.class, PasswordResetService.class})
class PasswordHashingTest {
    private static final String OLD_PASSWORD = "OriginalPass123!";
    private static final String NEW_PASSWORD = "ReplacementPass456!";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtUtil jwt;
    @Autowired private UserService users;
    @MockBean private UserRepository repository;
    @MockBean private PasswordResetTokenRepository resetTokens;
    @MockBean private ModuleService modules;
    @MockBean private AuditLogService audit;
    @MockBean private EmailService email;
    @MockBean private UserAccessRepository access;
    private Map<String, User> stored;

    @BeforeEach
    void setUp() {
        stored = new HashMap<>();
        when(repository.findByUsername(anyString())).thenAnswer(call ->
                Optional.ofNullable(stored.get(call.getArgument(0))));
        when(repository.save(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            stored.put(user.getUserID(), user);
            return user;
        });
        addStored("super", "superadmin");
        addStored("admin", "admin");
        addStored("lecturer", "lecture");
    }

    private void addStored(String username, String role) {
        stored.put(username, new User(username, username + "@example.test", encoder.encode(OLD_PASSWORD), role));
    }

    private String bearer(String username) {
        return "Bearer " + jwt.generateToken(username, stored.get(username).getUsertype());
    }

    private void assertHash(String username, String password) {
        String hash = stored.get(username).getPassword();
        assertNotEquals(password, hash);
        assertTrue(hash.startsWith("$2"), "Password must be stored as BCrypt");
        assertTrue(encoder.matches(password, hash));
    }

    private void assertLogin(String username, String password, boolean success) throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("userID", username, "password", password))));
        if (success) result.andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        else result.andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"lecturer,lecturers,admin", "admin,admins,super"})
    void updatedPasswordIsHashedAndWorksThroughLogin(String username, String path, String actor) throws Exception {
        mvc.perform(put("/api/auth/" + path + "/" + username)
                .header("Authorization", bearer(actor)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("password", NEW_PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
        assertHash(username, NEW_PASSWORD);
        assertLogin(username, NEW_PASSWORD, true);
        assertLogin(username, OLD_PASSWORD, false);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void emailOnlyUpdatesPreserveExistingHash(String password) throws Exception {
        for (String username : new String[]{"lecturer", "admin"}) {
            String originalHash = stored.get(username).getPassword();
            if (username.equals("lecturer")) users.updateLecturer(username, "updated@example.test", password);
            else users.updateAdmin(username, "updated@example.test", password);
            assertEquals(originalHash, stored.get(username).getPassword());
            assertLogin(username, OLD_PASSWORD, true);
        }
    }

    @ParameterizedTest
    @CsvSource({"admin,add-admin,super", "lecture,add-lecture,admin"})
    void accountCreationHashesPasswordAndAllowsLogin(String role, String path, String actor) throws Exception {
        mvc.perform(post("/api/auth/" + path).header("Authorization", bearer(actor))
                .contentType("application/json").content(json.writeValueAsString(Map.of(
                        "userID", "newuser", "email", "new@example.test", "usertype", role, "password", NEW_PASSWORD))))
                .andExpect(status().isOk());
        assertHash("newuser", NEW_PASSWORD);
        assertLogin("newuser", NEW_PASSWORD, true);
    }

    @Test
    void selfServiceChangeHashesPasswordAndRejectsOldPassword() throws Exception {
        users.changePassword("lecturer", OLD_PASSWORD, NEW_PASSWORD);
        assertHash("lecturer", NEW_PASSWORD);
        assertLogin("lecturer", NEW_PASSWORD, true);
        assertLogin("lecturer", OLD_PASSWORD, false);
    }

    @Test
    void incorrectCurrentPasswordDoesNotChangeHash() {
        String originalHash = stored.get("lecturer").getPassword();
        assertThrows(IllegalArgumentException.class,
                () -> users.changePassword("lecturer", "incorrect", NEW_PASSWORD));
        assertEquals(originalHash, stored.get("lecturer").getPassword());
    }

    @Test
    void recoveryHashesPasswordAndAllowsLogin() throws Exception {
        PasswordResetToken reset = new PasswordResetToken("synthetic-reset-token", "lecturer", LocalDateTime.now().plusMinutes(30));
        when(resetTokens.findByToken(reset.getToken())).thenReturn(Optional.of(reset));
        mvc.perform(post("/api/auth/reset-password").contentType("application/json")
                .content(json.writeValueAsString(Map.of("token", reset.getToken(), "newPassword", NEW_PASSWORD))))
                .andExpect(status().isOk());
        assertHash("lecturer", NEW_PASSWORD);
        assertTrue(reset.isUsed());
        assertLogin("lecturer", NEW_PASSWORD, true);
        assertLogin("lecturer", OLD_PASSWORD, false);
    }

    @Test
    void testAccountCreationUsesIndependentSalt() throws Exception {
        users.createTestUser("first", NEW_PASSWORD, "first@example.test", "lecture");
        users.createTestUser("second", NEW_PASSWORD, "second@example.test", "lecture");
        assertHash("first", NEW_PASSWORD);
        assertHash("second", NEW_PASSWORD);
        assertNotEquals(stored.get("first").getPassword(), stored.get("second").getPassword());
        assertLogin("first", NEW_PASSWORD, true);
    }
}
