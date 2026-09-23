package com.example.Software.project.Backend.Reporting;

import java.time.Instant;
import java.util.List;

/**
 * Batch-level PO success report, admin-only. Two-tier threshold, same shape as the existing LO
 * batch report ({@link BatchReport}): {@code studentThreshold} decides whether one student
 * "attained" a PO (their saved credit % in this batch >= threshold); {@code batchTarget}
 * decides whether the PO counts as successful for the batch (% of the batch's students who
 * attained it >= target). Built from saved {@code student_po_credit} rows scoped to this batch
 * — see PoReportService.
 */
public record PoBatchReport(String batch, Instant generatedAt, double studentThreshold,
        double batchTarget, int totalStudents, List<PoRow> pos) {
    public record PoRow(String code, String title, int studentsAttained, int totalStudents,
            double attainmentPercent, String status) {}
}
