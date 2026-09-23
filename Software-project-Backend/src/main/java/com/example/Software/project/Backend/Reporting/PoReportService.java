package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Model.OutcomeMapping;
import com.example.Software.project.Backend.Model.ProgramOutcome;
import com.example.Software.project.Backend.Model.Student;
import com.example.Software.project.Backend.Model.StudentPoCredit;
import com.example.Software.project.Backend.Repository.OutcomeMappingRepository;
import com.example.Software.project.Backend.Repository.ProgramOutcomeRepository;
import com.example.Software.project.Backend.Repository.StudentPoCreditRepository;
import com.example.Software.project.Backend.Repository.StudentRepository;
import com.example.Software.project.Backend.Service.POAttainmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

/**
 * Admin-only PO attainment reports, built from the saved {@code student_po_credit} rows that
 * POAttainmentService.calculateStudentPOCredits persists on every calculation — manual, or
 * auto-triggered by a marks upload/edit/delete (see POAttainmentService.recalculateForModule).
 *
 * PO-level only (no module breakdown) - just each PO's credits/attainment. Every Washington
 * Accord standard PO ({@code ProgramOutcome.isDefault}) always appears, even with no evidence
 * yet, so the report always has the same shape; any custom PO with actual data is appended
 * after it.
 *
 * Distinct from {@link BatchReportService}: that one is LO-evidence-based, anonymized (no
 * student names) and staff-accessible; this one names students, reports on PO credits directly
 * rather than re-deriving them from LO evidence, and is admin-only (enforced by
 * {@link PoReportController}, not here).
 */
@Service
public class PoReportService {

    @Autowired private POAttainmentService poAttainmentService;
    @Autowired private StudentRepository studentRepository;
    @Autowired private StudentPoCreditRepository studentPoCreditRepository;
    @Autowired private OutcomeMappingRepository outcomeMappingRepository;
    @Autowired private ProgramOutcomeRepository programOutcomeRepository;

    /**
     * One student's cross-module PO credit standing — "end of their academics" cumulative view,
     * not scoped to any one batch or module. A PO is "Attained" when the student's credit
     * percentage for it is >= studentThreshold, "Not attained" when it's below, and
     * "No evidence" when no module has ever calculated a credit for that PO for this student.
     */
    @Transactional(readOnly = true)
    public PoStudentReport studentReport(String studentId, double studentThreshold) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));

        Map<String, Object> summary = poAttainmentService.getStudentPOSummary(studentId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> poSummaries = (List<Map<String, Object>>) summary.get("poSummaries");
        Map<String, Map<String, Object>> summaryByCode = new LinkedHashMap<>();
        for (Map<String, Object> po : poSummaries) {
            summaryByCode.put((String) po.get("poCode"), po);
        }

        List<PoStudentReport.PoRow> rows = new ArrayList<>();
        Set<String> covered = new LinkedHashSet<>();

        // Washington Accord standard POs always appear first, in their display order, whether
        // or not this student has any evidence for them yet.
        for (ProgramOutcome po : programOutcomeRepository.findByIsDefaultTrueOrderByDisplayOrderAsc()) {
            rows.add(studentRow(po.getCode(), po.getTitle(), summaryByCode.get(po.getCode()), studentThreshold));
            covered.add(po.getCode());
        }
        // Any custom PO the student actually has credit data for, appended after.
        for (Map<String, Object> po : poSummaries) {
            String code = (String) po.get("poCode");
            if (covered.contains(code)) continue;
            rows.add(studentRow(code, poTitle(code), po, studentThreshold));
        }

        int moduleCount = ((Number) summary.get("moduleCount")).intValue();
        return new PoStudentReport(studentId, student.getStudentName(), Instant.now(), studentThreshold, moduleCount, rows);
    }

    /**
     * Every student in a batch's individual PO report, for bundling into one download (see
     * {@link PoReportController}) — each report here is exactly what {@link #studentReport}
     * would produce for that student on its own; this just runs it once per student in the
     * batch so the admin can download every individual report in one action instead of
     * searching each student up one at a time.
     */
    @Transactional(readOnly = true)
    public List<PoStudentReport> studentReportsForBatch(String batch, double studentThreshold) {
        List<Student> students = studentRepository.findByBatch(batch);
        if (students.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No students found for batch: " + batch);
        }
        List<PoStudentReport> reports = new ArrayList<>();
        for (Student student : students) {
            reports.add(studentReport(student.getStudentId(), studentThreshold));
        }
        return reports;
    }

    private PoStudentReport.PoRow studentRow(String code, String title, Map<String, Object> po, double studentThreshold) {
        if (po == null) {
            return new PoStudentReport.PoRow(code, title, 0, 0, null, "No evidence");
        }
        int earned = ((Number) po.get("creditsEarned")).intValue();
        int max = ((Number) po.get("maxCredits")).intValue();
        Double percentage = (Double) po.get("percentage");
        String status = percentage == null ? "No evidence" : (percentage >= studentThreshold ? "Attained" : "Not attained");
        return new PoStudentReport.PoRow(code, title, earned, max, percentage, status);
    }

    /**
     * Batch-level PO success: for each PO, what share of the batch's students attained it
     * (their saved credit % in this batch >= studentThreshold), and whether that share clears
     * batchTarget. A student with no saved credit for a PO counts as not having attained it —
     * same "missing evidence fails" convention POAttainmentService.calculateOverallPOAttainment
     * already uses for the live LO-based check, kept consistent here for the credit-based one.
     *
     * Every Washington Accord standard PO always appears, whether or not it has an approved
     * mapping or any saved credit yet; any custom PO with an approved mapping is appended after.
     */
    @Transactional(readOnly = true)
    public PoBatchReport batchReport(String batch, double studentThreshold, double batchTarget) {
        List<Student> students = studentRepository.findByBatch(batch);
        if (students.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No students found for batch: " + batch);
        }

        // studentId -> poCode -> [earned, max], summed across every module saved for this batch.
        Map<String, Map<String, int[]>> byStudentAndPo = new HashMap<>();
        for (StudentPoCredit row : studentPoCreditRepository.findByBatch(batch)) {
            byStudentAndPo
                    .computeIfAbsent(row.getStudent().getStudentId(), k -> new HashMap<>())
                    .merge(row.getProgramOutcome().getCode(), new int[]{row.getCreditsEarned(), row.getMaxCredits()},
                            (a, b) -> new int[]{a[0] + b[0], a[1] + b[1]});
        }

        List<PoBatchReport.PoRow> rows = new ArrayList<>();
        Set<String> covered = new LinkedHashSet<>();

        for (ProgramOutcome po : programOutcomeRepository.findByIsDefaultTrueOrderByDisplayOrderAsc()) {
            rows.add(batchRow(po, students, byStudentAndPo, studentThreshold, batchTarget));
            covered.add(po.getCode());
        }

        List<OutcomeMapping> approvedMappings = outcomeMappingRepository.findByStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        Map<String, ProgramOutcome> customPos = new LinkedHashMap<>();
        for (OutcomeMapping m : approvedMappings) {
            String code = m.getProgramOutcome().getCode();
            if (!covered.contains(code)) customPos.putIfAbsent(code, m.getProgramOutcome());
        }
        for (ProgramOutcome po : customPos.values()) {
            rows.add(batchRow(po, students, byStudentAndPo, studentThreshold, batchTarget));
        }

        return new PoBatchReport(batch, Instant.now(), studentThreshold, batchTarget, students.size(), rows);
    }

    private PoBatchReport.PoRow batchRow(ProgramOutcome po, List<Student> students,
            Map<String, Map<String, int[]>> byStudentAndPo, double studentThreshold, double batchTarget) {
        int attained = 0;
        for (Student student : students) {
            int[] credit = byStudentAndPo.getOrDefault(student.getStudentId(), Collections.emptyMap()).get(po.getCode());
            if (credit == null || credit[1] <= 0) continue;
            double pct = credit[0] * 100.0 / credit[1];
            if (pct >= studentThreshold) attained++;
        }
        double attainmentPercent = attained * 100.0 / students.size();
        String status = attainmentPercent >= batchTarget ? "Success" : "Not successful";
        return new PoBatchReport.PoRow(po.getCode(), po.getTitle(), attained, students.size(), attainmentPercent, status);
    }

    private String poTitle(String code) {
        return programOutcomeRepository.findByCode(code).map(ProgramOutcome::getTitle).orElse(code);
    }
}
