package com.example.Software.project.Backend.Security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class JwtSecretConfigurationTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short-private-value", "                                  ",
            "${JWT_SECRET:unresolved-long-placeholder}",
            "change-me-this-is-only-an-example-not-a-key",
            "your-secret-key-that-is-not-a-real-generated-key"})
    void rejectsMissingWeakOrExampleKeysWithoutDisclosingValues(String value) {
        var error = assertThrows(IllegalArgumentException.class, () -> new JwtUtil(value));
        assertEquals("JWT_SECRET must be a generated signing key of at least 32 UTF-8 bytes.", error.getMessage());
    }

    @Test void validKeySignsAndVerifiesAndDifferentKeyRejects() {
        var original = new JwtUtil("0123456789abcdef0123456789abcdef");
        var other = new JwtUtil("abcdef0123456789abcdef0123456789");
        var token = original.generateToken("local-user", "lecture");
        assertTrue(original.validateToken(token, "local-user"));
        assertEquals("lecture", original.extractRole(token));
        assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> other.extractUsername(token));
    }
}
