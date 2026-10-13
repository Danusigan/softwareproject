package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.User;
import com.example.Software.project.Backend.Repository.UserRepository;
import com.example.Software.project.Backend.Security.JwtUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Starts a real HTTP server with an isolated database; never touches local MySQL accounts. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:password-http;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false", "logging.level.org.springframework=WARN",
        "logging.level.org.hibernate=WARN"
})
class PasswordHashingIntegrationTest {
    @Autowired private TestRestTemplate http;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtUtil jwt;

    @BeforeEach
    void configureHttpClient() {
        // HttpURLConnection retries 401 responses in streaming mode instead of exposing them.
        http.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @ParameterizedTest
    @CsvSource({"lecture,lecturers,admin", "admin,admins,superadmin"})
    void persistedPasswordUpdateSupportsRealHttpLogin(String role, String route, String actorRole) {
        String username = "hash-target-" + role;
        String actor = "hash-actor-" + actorRole;
        String oldPassword = "OriginalPass123!";
        String newPassword = "ReplacementPass456!";
        users.saveAndFlush(new User(actor, actor + "@example.test", encoder.encode(oldPassword), actorRole));
        users.saveAndFlush(new User(username, username + "@example.test", encoder.encode(oldPassword), role));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt.generateToken(actor, actorRole));
        ResponseEntity<String> update = http.exchange("/api/auth/" + route + "/" + username,
                HttpMethod.PUT, new HttpEntity<>(Map.of("password", newPassword), headers), String.class);
        assertEquals(HttpStatus.OK, update.getStatusCode());

        String savedHash = users.findByUsername(username).orElseThrow().getPassword();
        assertNotEquals(newPassword, savedHash);
        assertTrue(savedHash.startsWith("$2"));
        assertTrue(encoder.matches(newPassword, savedHash));
        assertFalse(encoder.matches(oldPassword, savedHash));

        ResponseEntity<Map> login = http.postForEntity("/api/auth/login",
                Map.of("userID", username, "password", newPassword), Map.class);
        assertEquals(HttpStatus.OK, login.getStatusCode());
        assertNotNull(login.getBody());
        String token = (String) login.getBody().get("token");
        assertNotNull(token);
        assertTrue(jwt.validateToken(token, username));

        ResponseEntity<String> rejected = http.postForEntity("/api/auth/login",
                Map.of("userID", username, "password", oldPassword), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, rejected.getStatusCode());

        HttpHeaders signedIn = new HttpHeaders();
        signedIn.setBearerAuth(token);
        assertEquals(HttpStatus.OK, http.exchange("/api/modules/all", HttpMethod.GET,
                new HttpEntity<>(signedIn), String.class).getStatusCode());
    }
}
