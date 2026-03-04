package com.example.Software.project.Backend.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "program_outcomes")
public class ProgramOutcome {
    @Id
    @Column(name = "po_id", unique = true, nullable = false)
    private String poId; // e.g., "PO1", "PO2" - capital letters, numbers, spaces only

    @Column(name = "code", unique = true, nullable = false)
    private String code; // e.g., "PO1", "PO2" - unique code for reference

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public ProgramOutcome() {}

    public ProgramOutcome(String poId, String code, String description) {
        setPoId(poId);
        this.code = code;
        this.description = description;
    }

    // Getters and Setters
    public String getPoId() { return poId; }
    public void setPoId(String poId) {
        // Validate: only capital letters, numbers, and spaces allowed
        if (poId != null && !poId.matches("^[A-Z0-9\\s]+$")) {
            throw new IllegalArgumentException(
                "PO ID must contain only capital letters, numbers, and spaces (A-Z, 0-9, spaces)"
            );
        }
        this.poId = poId;
    }

    // Backward compatibility for existing code that uses getId/setId
    public String getId() { return poId; }
    public void setId(String id) { setPoId(id); }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}