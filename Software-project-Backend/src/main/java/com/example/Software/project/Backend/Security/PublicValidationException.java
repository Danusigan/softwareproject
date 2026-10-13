package com.example.Software.project.Backend.Security;

/** Only use with application-authored messages that contain no secrets or internal details. */
public class PublicValidationException extends IllegalArgumentException {
    public PublicValidationException(String message) { super(message); }
}
