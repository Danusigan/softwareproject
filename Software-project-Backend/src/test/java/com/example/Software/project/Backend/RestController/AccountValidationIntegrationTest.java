package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:account-validation;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "logging.level.org.springframework=WARN"})
@AutoConfigureMockMvc @Transactional
class AccountValidationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository resets;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtil jwt;
    @Autowired ObjectMapper json;
    static final String PASSWORD="OriginalPass123!";
    @BeforeEach void setup() {
        for(String role:List.of("superadmin","admin","lecture"))
            users.saveAndFlush(new User(role,role+"@example.test",encoder.encode(PASSWORD),role));
    }
    String token(String role) {return "Bearer "+jwt.generateToken(role,role);}
    Map<String,Object> account() {
        return new HashMap<>(Map.of("userID","newuser","email","new@example.test","password",PASSWORD,"usertype","lecture"));
    }
    void create(Map<String,Object> body,int expected) throws Exception {
        mvc.perform(post("/api/auth/add-user").header("Authorization",token("superadmin"))
            .contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().is(expected));
    }
    @ParameterizedTest @ValueSource(strings={"ab"," leading","trailing ","bad/name","<script>","bad name","_starts",""})
    void rejectsInvalidNewUsernames(String name) throws Exception {
        var body=account();body.put("userID",name);create(body,400);assertEquals(3,users.count());
    }
    @Test void usernameLengthAndCaseInsensitiveUniqueness() throws Exception {
        var body=account();body.put("userID","a".repeat(65));create(body,400);
        body.put("userID","ADMIN");create(body,400);
        body.put("userID","a".repeat(64));create(body,200);
    }
    @ParameterizedTest @ValueSource(strings={"notemail","a@@example.com","a b@example.com","", "   "})
    void invalidEmailsCannotCreateOrEditAccounts(String email) throws Exception {
        var body=account();body.put("email",email);create(body,400);
        for(String path:List.of("admins/admin","lecturers/lecture"))
            mvc.perform(put("/api/auth/"+path).header("Authorization",token("superadmin"))
                .contentType("application/json").content(json.writeValueAsString(Map.of("email",email))))
                .andExpect(status().isBadRequest());
        assertEquals("admin@example.test",users.findByUsername("admin").orElseThrow().getEmail());
    }
    @Test void duplicateEmailsAndOverlongEmailsAreRejected() throws Exception {
        var body=account();body.put("email","ADMIN@example.test");create(body,400);
        mvc.perform(put("/api/auth/lecturers/lecture").header("Authorization",token("admin"))
            .contentType("application/json").content("{\"email\":\"ADMIN@example.test\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Email already exists"));
        body.put("email","a".repeat(245)+"@example.test");create(body,400);
    }
    @ParameterizedTest @ValueSource(strings={"short","lowercase123","UPPERCASE123","NoDigitsHere","Abc123\nX"})
    void allPasswordWritePathsRejectWeakPasswordsWithoutPartialChanges(String weak) throws Exception {
        String oldHash=users.findByUsername("lecture").orElseThrow().getPassword();
        var body=account();body.put("password",weak);create(body,400);
        for(String path:List.of("admins/admin","lecturers/lecture"))
            mvc.perform(put("/api/auth/"+path).header("Authorization",token("superadmin"))
                .contentType("application/json").content(json.writeValueAsString(Map.of("email","changed@example.test","password",weak))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/profile/change-password").header("Authorization",token("lecture"))
            .contentType("application/json").content(json.writeValueAsString(Map.of("currentPassword",PASSWORD,"newPassword",weak))))
            .andExpect(status().isBadRequest());
        resets.saveAndFlush(new PasswordResetToken("validation-token","lecture",LocalDateTime.now().plusMinutes(30)));
        mvc.perform(post("/api/auth/reset-password").contentType("application/json")
            .content(json.writeValueAsString(Map.of("token","validation-token","newPassword",weak))))
            .andExpect(status().isBadRequest());
        users.flush();
        assertEquals(oldHash,users.findByUsername("lecture").orElseThrow().getPassword());
        assertEquals("lecture@example.test",users.findByUsername("lecture").orElseThrow().getEmail());
        assertFalse(resets.findByToken("validation-token").orElseThrow().isUsed());
    }
    @Test void passwordUtf8LimitIsEnforcedWithoutTruncation() throws Exception {
        var body=account();body.put("password","Aa1"+"x".repeat(70));create(body,400);
        body.put("password","Aa1"+"é".repeat(35));create(body,400);
        body.put("password","Aa1"+"x".repeat(69));create(body,200);
        assertTrue(encoder.matches(body.get("password").toString(),users.findByUsername("newuser").orElseThrow().getPassword()));
    }
    @Test void optionalPasswordRemainsUnchangedAndValidUpdatesWork() throws Exception {
        String hash=users.findByUsername("lecture").orElseThrow().getPassword();
        mvc.perform(put("/api/auth/lecturers/lecture").header("Authorization",token("admin"))
            .contentType("application/json").content("{\"email\":\"updated+tag@example.test\",\"password\":\"   \"}"))
            .andExpect(status().isOk());
        assertEquals(hash,users.findByUsername("lecture").orElseThrow().getPassword());
        String fresh=" ValidPass123! ";
        resets.saveAndFlush(new PasswordResetToken("valid-token","lecture",LocalDateTime.now().plusMinutes(30)));
        mvc.perform(post("/api/auth/reset-password").contentType("application/json")
            .content(json.writeValueAsString(Map.of("token","valid-token","newPassword",fresh)))).andExpect(status().isOk());
        String changed=users.findByUsername("lecture").orElseThrow().getPassword();
        assertTrue(encoder.matches(fresh,changed));assertFalse(encoder.matches(fresh.trim(),changed));
    }
    @Test void unsupportedUpdateFieldsAndNullEmailAreRejected() throws Exception {
        for(String body:List.of("{\"usertype\":\"superadmin\"}","{\"userID\":\"renamed\"}","{\"lockedUntil\":null}","{\"email\":null}"))
            mvc.perform(put("/api/auth/lecturers/lecture").header("Authorization",token("admin"))
                .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        assertEquals("lecture",users.findByUsername("lecture").orElseThrow().getUsertype());
    }
    @Test void malformedRecoveryEmailIs400ButUnknownValidEmailRemainsGeneric() throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType("application/json").content("{\"email\":\"not-an-email\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/forgot-password").contentType("application/json").content("{\"email\":\"unknown@example.test\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("If an account with that email exists, a password reset link has been sent."));
    }
    @Test void loginStillAcceptsLegacyPasswordAndUsername() throws Exception {
        users.saveAndFlush(new User("x","legacy@example.test",encoder.encode("old"),"lecture"));
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"userID\":\"x\",\"password\":\"old\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }
}
