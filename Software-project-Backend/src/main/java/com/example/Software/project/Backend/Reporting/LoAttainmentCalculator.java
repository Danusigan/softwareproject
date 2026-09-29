package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Model.*;
import java.util.*;

/**
 * Shared per-LO attainment calculation used by both the batch report and,
 * formerly, the individual student report. Report-only projection: no marks,
 * thresholds or existing attainment services are modified.
 */
public final class LoAttainmentCalculator {
    private LoAttainmentCalculator() {}

    public record LoResult(String loId, String name, String description, Double percentage,
            double threshold, Double margin, String status, List<MarkRow> marks) {}
    public record MarkRow(String assessment, String type, String academicYear, String semester,
            String question, Double score, Double maximum, Double percentage, String source) {}

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
