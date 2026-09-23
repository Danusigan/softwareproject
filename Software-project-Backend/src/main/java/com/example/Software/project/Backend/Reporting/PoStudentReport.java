package com.example.Software.project.Backend.Reporting;

import java.time.Instant;
import java.util.List;

/**
 * One student's cross-module PO credit standing, admin-only — see PoReportService for how it's
 * built. PO-level only, no module breakdown: how many credits earned/possible per PO, and
 * whether that clears the attainment threshold. Distinct from {@link BatchReport}: that one is
 * LO-evidence-based, anonymized and staff-accessible; this one names a student and is built
 * from the saved {@code student_po_credit} rows that POAttainmentService persists on every
 * calculation.
 */
public record PoStudentReport(String studentId, String studentName, Instant generatedAt,
        double studentThreshold, int moduleCount, List<PoRow> pos) {
    public record PoRow(String code, String title, int creditsEarned, int maxCredits,
            Double percentage, String status) {}
}
