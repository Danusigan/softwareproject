package com.example.Software.project.Backend.Security;

/** Distinguishes a missing domain record from a database or programming failure. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
