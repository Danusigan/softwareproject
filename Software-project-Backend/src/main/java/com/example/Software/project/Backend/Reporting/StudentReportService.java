package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Service.ModuleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static com.example.Software.project.Backend.Reporting.StudentReport.*;

@Service
@Transactional(readOnly = true)
public class StudentReportService {
    private final StudentReportRepository repository;
    private final ModuleService modules;
    public StudentReportService(StudentReportRepository repository, ModuleService modules) {
        this.repository = repository;
        this.modules = modules;
    }

    public record StudentOption(String studentId, String studentName) {}

    private List<Module> visible(String role, String username) {
        if ("admin".equals(role) || "superadmin".equals(role)) return modules.getAllModules();
        if ("lecture".equals(role)) return modules.getModulesForLecturer(username);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff access required");
    }

    public List<StudentOption> students(String batch, String role, String username) {
        requireBatch(batch);
        List<String> ids = visible(role, username).stream().map(Module::getModuleId).toList();
        if (ids.isEmpty()) return List.of();
        return repository.students(batch.trim(), ids).stream()
                .map(s -> new StudentOption(s.getStudentId(), s.getStudentName())).toList();
    }

    public StudentReport generate(String id, String batch, double threshold, String role, String username) {
        requireBatch(batch);
        if (!Double.isFinite(threshold) || threshold < 0 || threshold > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Threshold must be between 0 and 100");
        if (id == null || id.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student ID is required");
        batch = batch.trim();
        List<Module> allowed = visible(role, username);
        List<StudentMark> legacy = repository.legacy(id, batch);
        List<StudentAssessmentScore> scores = repository.scores(id, batch);
        Set<String> evidenced = new HashSet<>();
        legacy.forEach(m -> evidenced.add(m.getLos().getModule().getModuleId()));
        scores.forEach(s -> evidenced.add(s.getAssessmentItem().getLos().getModule().getModuleId()));
        List<Module> selected = allowed.stream().filter(m -> evidenced.contains(m.getModuleId()))
                .sorted(Comparator.comparing(Module::getModuleId)).toList();
        if (selected.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No accessible student marks found for this batch");
        Student student = repository.student(id);
        if (student == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        List<ModuleResult> results = new ArrayList<>();
        for (Module module : selected) {
            List<AssessmentItem> items = repository.items(module.getModuleId(), batch);
            List<LoResult> loResults = new ArrayList<>();
            for (Los lo : repository.los(module.getModuleId())) {
                loResults.add(calculate(lo,
                        items.stream().filter(i -> lo.getId().equals(i.getLos().getId())).toList(),
                        scores.stream().filter(s -> lo.getId().equals(s.getAssessmentItem().getLos().getId())).toList(),
                        legacy.stream().filter(m -> lo.getId().equals(m.getLos().getId())).toList(), threshold));
            }
            results.add(new ModuleResult(module.getModuleId(), module.getModuleName(), loResults));
        }
        return new StudentReport(student.getStudentId(), student.getStudentName(), student.getEmail(),
                student.getAcademicYear(), batch, Instant.now(), threshold,
                "Modules with recorded marks visible to the requesting staff member; selected batch only.",
                results, List.of(
                "Analysis report, not an approved transcript. Threshold is selected for this report and does not change saved settings.",
                "LO score = sum of mapped question marks / sum of their maximum marks x 100. Each question contributes once.",
                "All stored assessments for the selected batch are included. Academic year and semester are shown per assessment. Repeat attempts are not resolved by this report.",
                "Missing or invalid question marks remain Pending. Legacy LO totals are displayed separately; matching question records take precedence and are not counted twice.",
                "Legacy totals without question-level evidence cannot establish a complete LO percentage. No maximum marks are assumed.",
                "Achieved means LO percentage is at least the selected threshold. These are individual LO results, not module pass/fail or cohort attainment levels.",
                "Programme, enrolment completeness, official module pass rules and individual level bands are not available to this report."));
    }

    private void requireBatch(String batch) {
        if (batch == null || batch.isBlank() || batch.length() > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid batch is required");
    }

    // Report-only projection: no marks, thresholds or existing attainment services are modified.
    public static LoResult calculate(Los lo, List<AssessmentItem> items,
            List<StudentAssessmentScore> scores, List<StudentMark> legacy, double threshold) {
        Map<Long, StudentAssessmentScore> byItem = new HashMap<>();
        boolean duplicate = false;
        for (StudentAssessmentScore score : scores) {
            if (byItem.put(score.getItemId(), score) != null) duplicate = true;
        }
        List<MarkRow> rows = new ArrayList<>();
        double total = 0, maximum = 0;
        boolean complete = !items.isEmpty() && !duplicate;
        Set<String> detailedAssessments = new HashSet<>();
        for (AssessmentItem item : items) {
            AssessmentTemplate template = item.getAssessmentTemplate();
            StudentAssessmentScore record = byItem.get(item.getId());
            Double score = record == null ? null : record.getScore();
            Double max = item.getMaxMarks();
            boolean valid = score != null && max != null && Double.isFinite(score) &&
                    Double.isFinite(max) && max > 0 && score >= 0 && score <= max;
            complete &= valid;
            if (valid) { total += score; maximum += max; }
            if (record != null) detailedAssessments.add(key(template.getMarkType(), template.getAssignmentLabel()));
            rows.add(new MarkRow(label(template), template.getMarkType(), template.getAcademicYear(),
                    template.getSemester(), item.getQuestionLabel(), score, max,
                    valid ? score / max * 100 : null, valid ? "Question mark" :
                    score == null ? "Pending: missing mark" : "Pending: invalid mark or maximum"));
        }
        for (StudentMark mark : legacy) {
            String type = mark.getMarkType() == null ? "" : mark.getMarkType().name();
            boolean mirrored = detailedAssessments.contains(key(type, mark.getAssignmentLabel()));
            if (!mirrored) complete = false;
            rows.add(new MarkRow(mark.getAssignmentLabel(), type, null, null,
                    "LO total", mark.getScore(), null, null, mirrored ?
                    "Legacy reference only; question marks used" : "Legacy total; question evidence unavailable"));
        }
        Double percentage = complete && maximum > 0 ? total / maximum * 100 : null;
        String status = percentage == null ? (rows.isEmpty() ? "Not assessed" : "Pending") :
                percentage >= threshold ? "Achieved" : "Below threshold";
        return new LoResult(lo.getId(), lo.getName(), lo.getDescription(), percentage, threshold,
                percentage == null ? null : percentage - threshold, status, rows);
    }

    private static String key(String type, String label) {
        return Objects.toString(type, "").trim().toUpperCase(Locale.ROOT) + "|" +
                Objects.toString(label, "").trim();
    }
    private static String label(AssessmentTemplate t) {
        return t.getAssignmentLabel() != null && !t.getAssignmentLabel().isBlank() ?
                t.getAssignmentLabel() : t.getName() != null ? t.getName() : t.getId();
    }
}
