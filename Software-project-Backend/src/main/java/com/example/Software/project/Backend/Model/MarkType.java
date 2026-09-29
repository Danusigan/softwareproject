package com.example.Software.project.Backend.Model;

public enum MarkType {
    FINAL_EXAM("Final Exam"),
    ASSIGNMENT("Assignment");

    private final String displayName;

    MarkType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
