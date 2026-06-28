package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.AssessmentItem;
import com.example.Software.project.Backend.Model.StudentMark;
import com.example.Software.project.Backend.Repository.AssessmentItemRepository;
import com.example.Software.project.Backend.Repository.StudentMarkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TrendService {

    @Autowired
    private StudentMarkRepository markRepository;

    @Autowired
    private AssessmentItemRepository assessmentItemRepository;

    // Original Module-level trend (kept for backward compat)
    public List<Map<String, Object>> getCourseTrend(String courseId) {
        List<Object[]> rawData = markRepository.findYearlyAverageByCourse(courseId);
        return processTrendData(rawData, false);
    }

    /**
     * LO-level trend normalized to percentage.
     * For each LO+batch: average = (sum of all student scores / number of students) then
     * normalized to % using total max marks from AssessmentItem.
     * If no AssessmentItems exist (legacy data), raw score used as-is (assumed 0-100).
     */
    public Map<String, List<Map<String, Object>>> getLoTrend(String courseId) {
        List<Object[]> rawData = markRepository.findLoTrendByCourse(courseId);

        // Group by LO key; carry loId for normalization lookup
        // rawData row: [loId, loName, batch, rawAvg]
        Map<String, List<Object[]>> grouped = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            if (row == null || row.length < 4) continue;
            String loId   = toSafeString(row[0]);
            String loName = toSafeString(row[1]);
            String key = loId + " - " + loName;
            grouped.computeIfAbsent(key, k -> new ArrayList<>())
                   .add(new Object[]{loId, row[2], row[3]}); // loId, batch, rawAvg
        }

        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        for (Map.Entry<String, List<Object[]>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), buildNormalizedTrend(entry.getValue()));
        }
        return result;
    }

    private List<Map<String, Object>> buildNormalizedTrend(List<Object[]> rows) {
        // rows: [loId, batch, rawAvg]
        List<Map<String, Object>> result = new ArrayList<>();
        Double prevPct = null;

        for (Object[] row : rows) {
            String loId   = toSafeString(row[0]);
            String batch  = toSafeString(row[1]);
            Double rawAvg = toSafeDouble(row[2]);
            if (rawAvg == null) continue;

            double pct = normalizeToPercent(rawAvg, loId, batch);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("year",    batch + "nd Batch");
            entry.put("batch",   batch);
            entry.put("average", round1(pct));

            if (prevPct != null) {
                double delta = pct - prevPct; // absolute change in percentage points
                entry.put("delta", round1(delta));
                if (delta > 5)       entry.put("status", "IMPROVED");
                else if (delta < -5) entry.put("status", "DECLINED");
                else                 entry.put("status", "STABLE");
            } else {
                entry.put("status", "BASELINE");
            }

            result.add(entry);
            prevPct = pct;
        }
        return result;
    }

    /**
     * LO-level pass rate — properly normalized.
     * For each student, sums raw marks across all assignments for the LO+batch,
     * divides by total max marks, and compares to threshold (as %).
     * Falls back to treating raw score as % when no AssessmentItems exist.
     */
    public Map<String, List<Map<String, Object>>> getLoPassRate(String courseId, double threshold) {
        // Get unique LO+batch combinations from trend data
        List<Object[]> trendData = markRepository.findLoTrendByCourse(courseId);

        // Build: loKey -> loId, loName; and loId -> Set<batch>
        Map<String, String[]> loMeta = new LinkedHashMap<>(); // loId -> [loId, loName]
        Map<String, Set<String>> loBatches = new LinkedHashMap<>(); // loId -> batches
        for (Object[] row : trendData) {
            if (row == null || row.length < 4) continue;
            String loId   = toSafeString(row[0]);
            String loName = toSafeString(row[1]);
            String batch  = toSafeString(row[2]);
            loMeta.put(loId, new String[]{loId, loName});
            loBatches.computeIfAbsent(loId, k -> new LinkedHashSet<>()).add(batch);
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        for (Map.Entry<String, String[]> metaEntry : loMeta.entrySet()) {
            String loId   = metaEntry.getValue()[0];
            String loName = metaEntry.getValue()[1];
            String key    = loId + " - " + loName;
            Set<String> batches = loBatches.getOrDefault(loId, Collections.emptySet());

            List<Map<String, Object>> loRates = new ArrayList<>();

            for (String batch : batches) {
                // Get all marks for this LO+batch (across all assignments & markTypes)
                List<StudentMark> marks = markRepository.findByLos_IdAndBatch(loId, batch);
                if (marks.isEmpty()) continue;

                // Sum scores per student (cross-assignment)
                Map<String, Double> studentScores = new LinkedHashMap<>();
                for (StudentMark m : marks) {
                    if (m.getScore() == null) continue;
                    String sid = m.getStudent().getStudentId();
                    studentScores.merge(sid, m.getScore(), Double::sum);
                }
                if (studentScores.isEmpty()) continue;

                double maxMarks = getTotalMaxMarksBatch(loId, batch);

                long total  = studentScores.size();
                long passed = studentScores.values().stream().filter(score -> {
                    double pct = maxMarks > 0 ? (score / maxMarks) * 100.0 : score;
                    return pct >= threshold;
                }).count();

                double passRate = (double) passed / total * 100.0;

                Map<String, Object> point = new LinkedHashMap<>();
                point.put("batch",          batch);
                point.put("year",           batch + "nd Batch");
                point.put("totalStudents",  total);
                point.put("passedStudents", passed);
                point.put("passRate",       round1(passRate));
                loRates.add(point);
            }

            if (!loRates.isEmpty()) result.put(key, loRates);
        }

        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Total max marks for a LO+batch across all mark types and assignments. */
    private double getTotalMaxMarksBatch(String loId, String batch) {
        List<AssessmentItem> items = assessmentItemRepository.findByLos_IdAndAssessmentTemplate_Batch(loId, batch);
        return items.stream().mapToDouble(ai -> ai.getMaxMarks() != null ? ai.getMaxMarks() : 0.0).sum();
    }

    /** Normalize a raw average score to a percentage (0–100). */
    private double normalizeToPercent(double rawAvg, String loId, String batch) {
        double maxMarks = getTotalMaxMarksBatch(loId, batch);
        if (maxMarks > 0) {
            return Math.min(100.0, (rawAvg / maxMarks) * 100.0);
        }
        // No AssessmentItems — assume score is already a percentage
        return Math.min(100.0, Math.max(0.0, rawAvg));
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private List<Map<String, Object>> processTrendData(List<Object[]> rawData, boolean hasBatch) {
        List<Map<String, Object>> result = new ArrayList<>();
        Double previousAvg = null;

        for (Object[] row : rawData) {
            if (row == null || row.length < 2) continue;

            String year = toSafeString(row[0]);
            String batch = null;
            Double currentAvg;

            if (hasBatch && row.length >= 3) {
                batch = toSafeString(row[1]);
                currentAvg = toSafeDouble(row[2]);
            } else {
                currentAvg = toSafeDouble(row[1]);
            }

            if (currentAvg == null) continue;

            Map<String, Object> entry = new HashMap<>();
            String displayYear = (batch != null && !batch.isEmpty()) ? year + " (" + batch + ")" : year;
            entry.put("year", displayYear);
            entry.put("average", currentAvg);

            if (previousAvg != null && previousAvg != 0) {
                double delta = ((currentAvg - previousAvg) / previousAvg) * 100;
                entry.put("delta", delta);
                if (delta > 5)       entry.put("status", "IMPROVED");
                else if (delta < -5) entry.put("status", "DECLINED");
                else                 entry.put("status", "STABLE");
            } else {
                entry.put("status", "BASELINE");
            }

            result.add(entry);
            previousAvg = currentAvg;
        }
        return result;
    }

    private String toSafeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Double toSafeDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }
}
