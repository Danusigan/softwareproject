package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.User;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/** Input rules for new account details, never for persisted password hashes or existing login credentials. */
@Component
public class AccountValidation {
    public static final String PASSWORD_RULE = "Password must contain at least 8 characters, an uppercase letter, a lowercase letter and a number; maximum 72 UTF-8 bytes, with no control characters.";
    private final Validator validator;
    public AccountValidation(Validator validator) { this.validator = validator; }

    public static void username(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{2,63}"))
            throw new IllegalArgumentException("Username must be 3-64 characters, start with a letter or number, and contain only letters, numbers, dots, underscores or hyphens.");
    }

    public String email(String value) {
        if (value == null || value.length() > 254 || !validator.validateValue(User.class, "email", value).isEmpty())
            throw new IllegalArgumentException("Enter a valid email address of at most 254 characters.");
        return value;
    }

    public static void password(String value) {
        if (value == null || value.length() < 8 || value.getBytes(StandardCharsets.UTF_8).length > 72
                || value.codePoints().anyMatch(Character::isISOControl)
                || !value.matches("(?s).*[A-Z].*") || !value.matches("(?s).*[a-z].*") || !value.matches("(?s).*[0-9].*"))
            throw new IllegalArgumentException(PASSWORD_RULE);
    }

    public static void updateFields(Map<String, String> body) {
        if (!Set.of("email", "password").containsAll(body.keySet()))
            throw new IllegalArgumentException("Only email and password can be updated. Username and role cannot be changed here.");
        if (body.containsKey("email") && (body.get("email") == null || body.get("email").isBlank()))
            throw new IllegalArgumentException("Email cannot be blank.");
        // A missing or blank optional password continues to mean 'leave password unchanged'.
    }
}
