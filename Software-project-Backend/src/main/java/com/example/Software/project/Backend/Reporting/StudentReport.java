package com.example.Software.project.Backend.Reporting;

import java.time.Instant;
import java.util.List;

public record StudentReport(String studentId, String studentName, String email,
        String academicYear, String batch, Instant generatedAt, double threshold,
        String scope, List<ModuleResult> modules, List<String> notes) {
    public record ModuleResult(String moduleId, String moduleName, List<LoResult> los) {}
    public record LoResult(String loId, String name, String description, Double percentage,
            double threshold, Double margin, String status, List<MarkRow> marks) {}
    public record MarkRow(String assessment, String type, String academicYear, String semester,
            String question, Double score, Double maximum, Double percentage, String source) {}
}
