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

        Map<String, Object> summary = poAttainmentService.getStudentPOSummary(studentId, null);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> poSummaries = (List<Map<String, Object>>) summary.get("poSummaries");

        List<PoStudentReport.PoRow> rows = new ArrayList<>();
        for (Map<String, Object> po : poSummaries) {
            String code = (String) po.get("poCode");
            int earned = ((Number) po.get("creditsEarned")).intValue();
            int max = ((Number) po.get("maxCredits")).intValue();
            Double percentage = (Double) po.get("percentage");
            String status = percentage == null ? "No evidence" : (percentage >= studentThreshold ? "Attained" : "Not attained");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> breakdown = (List<Map<String, Object>>) po.get("moduleBreakdown");
            rows.add(new PoStudentReport.PoRow(code, poTitle(code), earned, max, percentage, status, mergeByModule(breakdown)));
        }
        rows.sort(Comparator.comparing(PoStudentReport.PoRow::code));

        int moduleCount = ((Number) summary.get("moduleCount")).intValue();
        return new PoStudentReport(studentId, student.getStudentName(), Instant.now(), studentThreshold, moduleCount, rows);
    }

    // A module can save two rows for the same PO (one from Final Exam marks, one from
    // Assignment marks) - merged into one contribution per module+batch here, same rule the
    // frontend's StudentPOSummaryPage applies, so the report doesn't show a module twice
    // unexplained.
    private List<PoStudentReport.ModuleContribution> mergeByModule(List<Map<String, Object>> breakdown) {
        record Key(String moduleId, String batch) {}
        Map<Key, int[]> merged = new LinkedHashMap<>();
        for (Map<String, Object> m : breakdown) {
            Key key = new Key((String) m.get("moduleId"), (String) m.get("batch"));
            int earned = ((Number) m.get("creditsEarned")).intValue();
            int max = ((Number) m.get("maxCredits")).intValue();
            merged.merge(key, new int[]{earned, max}, (a, b) -> new int[]{a[0] + b[0], a[1] + b[1]});
        }
        List<PoStudentReport.ModuleContribution> result = new ArrayList<>();
        for (Map.Entry<Key, int[]> e : merged.entrySet()) {
            result.add(new PoStudentReport.ModuleContribution(e.getKey().moduleId(), e.getKey().batch(), e.getValue()[0], e.getValue()[1]));
        }
        return result;
    }

    /**
     * Batch-level PO success: for each PO with an approved mapping, what share of the batch's
     * students attained it (their saved credit % in this batch >= studentThreshold), and
     * whether that share clears batchTarget. A student with no saved credit for a PO counts as
     * not having attained it — same "missing evidence fails" convention
     * POAttainmentService.calculateOverallPOAttainment already uses for the live LO-based
     * check, kept consistent here for the credit-based one.
     */
    @Transactional(readOnly = true)
    public PoBatchReport batchReport(String batch, double studentThreshold, double batchTarget) {
        List<Student> students = studentRepository.findByBatch(batch);
        if (students.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No students found for batch: " + batch);
        }

        List<OutcomeMapping> approvedMappings = outcomeMappingRepository.findByStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        Map<String, ProgramOutcome> posByCode = new LinkedHashMap<>();
        for (OutcomeMapping m : approvedMappings) {
            posByCode.putIfAbsent(m.getProgramOutcome().getCode(), m.getProgramOutcome());
        }

        // studentId -> poCode -> [earned, max], summed across every module/markType saved for
        // this batch (a module can contribute via both Final Exam and Assignment rows).
        Map<String, Map<String, int[]>> byStudentAndPo = new HashMap<>();
        for (StudentPoCredit row : studentPoCreditRepository.findByBatch(batch)) {
            byStudentAndPo
                    .computeIfAbsent(row.getStudent().getStudentId(), k -> new HashMap<>())
                    .merge(row.getProgramOutcome().getCode(), new int[]{row.getCreditsEarned(), row.getMaxCredits()},
                            (a, b) -> new int[]{a[0] + b[0], a[1] + b[1]});
        }

        List<PoBatchReport.PoRow> rows = new ArrayList<>();
        for (ProgramOutcome po : posByCode.values()) {
            int attained = 0;
            for (Student student : students) {
                int[] credit = byStudentAndPo.getOrDefault(student.getStudentId(), Collections.emptyMap()).get(po.getCode());
                if (credit == null || credit[1] <= 0) continue;
                double pct = credit[0] * 100.0 / credit[1];
                if (pct >= studentThreshold) attained++;
            }
            double attainmentPercent = attained * 100.0 / students.size();
            String status = attainmentPercent >= batchTarget ? "Success" : "Not successful";
            rows.add(new PoBatchReport.PoRow(po.getCode(), po.getTitle(), attained, students.size(), attainmentPercent, status));
        }
        rows.sort(Comparator.comparing(PoBatchReport.PoRow::code));

        return new PoBatchReport(batch, Instant.now(), studentThreshold, batchTarget, students.size(), rows);
    }

    private String poTitle(String code) {
        return programOutcomeRepository.findByCode(code).map(ProgramOutcome::getTitle).orElse(code);
    }
}
