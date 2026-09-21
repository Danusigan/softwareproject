package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.AssessmentItem;
import com.example.Software.project.Backend.Model.MarkType;
import com.example.Software.project.Backend.Model.OutcomeMapping;
import com.example.Software.project.Backend.Model.StudentMark;
import com.example.Software.project.Backend.Repository.AssessmentItemRepository;
import com.example.Software.project.Backend.Repository.OutcomeMappingRepository;
import com.example.Software.project.Backend.Repository.StudentMarkRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TrendService {

    private static final double DEFAULT_PASS_THRESHOLD = 50.0;
    private static final double DEFAULT_TARGET = 60.0;

    private final StudentMarkRepository markRepository;
    private final AssessmentItemRepository assessmentItemRepository;
    private final OutcomeMappingRepository outcomeMappingRepository;

    public TrendService(StudentMarkRepository markRepository,
                        AssessmentItemRepository assessmentItemRepository,
                        OutcomeMappingRepository outcomeMappingRepository) {
        this.markRepository = markRepository;
        this.assessmentItemRepository = assessmentItemRepository;
        this.outcomeMappingRepository = outcomeMappingRepository;
    }

    /** Backward-compatible module trend endpoint. */
    public List<Map<String, Object>> getCourseTrend(String moduleId) {
        return castList(getDashboardGraphs(moduleId, null, null,
            DEFAULT_PASS_THRESHOLD, DEFAULT_TARGET, null).get("moduleTrend"));
    }

    /** Backward-compatible LO trend endpoint, now normalized consistently. */
    public Map<String, List<Map<String, Object>>> getLoTrend(String moduleId) {
        return castTrendMap(getDashboardGraphs(moduleId, null, null,
            DEFAULT_PASS_THRESHOLD, DEFAULT_TARGET, null).get("loTrend"));
    }

    /** Backward-compatible LO pass-rate endpoint. */
    public Map<String, List<Map<String, Object>>> getLoPassRate(String moduleId, double threshold) {
        validatePercentage("threshold", threshold);
        Map<String, List<Map<String, Object>>> trends = castTrendMap(
            getDashboardGraphs(moduleId, null, null, threshold, DEFAULT_TARGET, null).get("loTrend"));
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        trends.forEach((key, points) -> result.put(key, points.stream().map(point -> {
            Map<String, Object> rate = new LinkedHashMap<>();
            rate.put("batch", point.get("batch"));
            rate.put("year", point.get("year"));
            rate.put("totalStudents", point.get("studentCount"));
            rate.put("passedStudents", point.get("passedStudents"));
            rate.put("passRate", point.get("passRate"));
            return rate;
        }).toList()));
        return result;
    }

    public Map<String, Object> getDashboardGraphs(String moduleId) {
        return getDashboardGraphs(moduleId, null, null,
            DEFAULT_PASS_THRESHOLD, DEFAULT_TARGET, null);
    }

    /**
     * Builds one consistent, filterable analytics payload for the QA dashboard.
     * Percentages are calculated per student/LO/batch before higher-level aggregation.
     */
    public Map<String, Object> getDashboardGraphs(String moduleId,
                                                   String batch,
                                                   MarkType markType,
                                                   double passThreshold,
                                                   double target,
                                                   String focusLoId) {
        if (moduleId == null || moduleId.isBlank()) throw new IllegalArgumentException("moduleId is required");
        validatePercentage("threshold", passThreshold);
        validatePercentage("target", target);

        String normalizedBatch = normalizeOptional(batch);
        String normalizedLoId = normalizeOptional(focusLoId);
        List<StudentMark> allMarks = safeList(markRepository.findByLos_Module_ModuleId(moduleId.trim()));

        List<String> availableBatches = allMarks.stream().map(StudentMark::getBatch)
            .filter(value -> value != null && !value.isBlank()).distinct()
            .sorted(TrendService::compareLabels).toList();
        List<String> availableMarkTypes = allMarks.stream().map(StudentMark::getMarkType)
            .filter(Objects::nonNull).map(Enum::name).distinct().sorted().toList();

        List<StudentMark> filteredMarks = allMarks.stream()
            .filter(mark -> normalizedBatch == null || normalizedBatch.equals(mark.getBatch()))
            .filter(mark -> markType == null || markType == mark.getMarkType()).toList();

        List<StudentMark> scopedMarks = normalizedLoId == null ? filteredMarks
            : filteredMarks.stream().filter(mark -> mark.getLos() != null
                && normalizedLoId.equalsIgnoreCase(mark.getLos().getId())).toList();
        List<String> warnings = new ArrayList<>();
        NormalizationContext normalization = new NormalizationContext(markType, scopedMarks.size());
        List<Observation> scopedObservations = buildObservations(scopedMarks, normalization);

        if (normalizedLoId != null && scopedObservations.isEmpty()) {
            warnings.add("No marks matched the selected learning outcome and filters.");
        } else if (scopedObservations.isEmpty()) {
            warnings.add("No marks matched the selected module and filters.");
        }

        List<Map<String, Object>> loPerformance = buildLoPerformance(scopedObservations, target);
        Map<String, List<Map<String, Object>>> loTrend = buildLoTrend(scopedObservations, passThreshold, target);
        List<Map<String, Object>> moduleTrend = buildModuleTrend(scopedObservations, passThreshold, target);
        List<StudentResult> studentResults = buildStudentResults(scopedObservations);
        List<Map<String, Object>> poPerformance = buildPoPerformance(
            moduleId.trim(), normalizedLoId, loPerformance, target, warnings);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("meta", buildMeta(moduleId.trim(), normalizedLoId, normalizedBatch, markType,
            passThreshold, target, availableBatches, availableMarkTypes));
        response.put("summary", buildSummary(studentResults, loPerformance, target, scopedObservations, passThreshold));
        response.put("moduleTrend", moduleTrend);
        response.put("loTrend", loTrend);
        response.put("focusTrend", resolveFocusTrend(loTrend, normalizedLoId, moduleTrend));
        response.put("loPerformance", loPerformance);
        response.put("poPerformance", poPerformance);
        response.put("poTrend", poPerformance);
        response.put("passFail", buildPassFail(studentResults, passThreshold));
        response.put("scoreBands", buildScoreBands(studentResults));
        response.put("benchmark", loPerformance);
        response.put("weakAreas", loPerformance.stream()
            .filter(item -> number(item.get("actual")) < target)
            .sorted(Comparator.comparingDouble(item -> number(item.get("actual")))).toList());
        response.put("dataQuality", buildDataQuality(normalization, scopedObservations, studentResults, warnings));
        return response;
    }

    private List<Observation> buildObservations(List<StudentMark> marks, NormalizationContext context) {
        Map<ObservationKey, List<StudentMark>> groups = marks.stream().filter(this::isUsableMark)
            .collect(Collectors.groupingBy(mark -> new ObservationKey(
                    mark.getStudent().getStudentId(), mark.getLos().getId(),
                    safeName(mark.getLos().getName(), mark.getLos().getId()),
                    safeName(mark.getBatch(), "Unspecified")),
                LinkedHashMap::new, Collectors.toList()));

        List<Observation> observations = new ArrayList<>();
        for (Map.Entry<ObservationKey, List<StudentMark>> entry : groups.entrySet()) {
            ObservationKey key = entry.getKey();
            List<StudentMark> groupMarks = entry.getValue();
            double maxMarks = context.maxMarks(key.loId(), key.batch(), groupMarks);
            double percentage;
            String method;
            if (maxMarks > 0) {
                // One StudentMark per type is expected. Averaging duplicates prevents an accidental
                // repeated import from inflating attainment before different assessment types are summed.
                double rawTotal = groupMarks.stream().collect(Collectors.groupingBy(
                        mark -> mark.getMarkType() == null ? "UNSPECIFIED" : mark.getMarkType().name()))
                    .values().stream().mapToDouble(typeMarks -> typeMarks.stream()
                        .map(StudentMark::getScore).filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue).average().orElse(0.0)).sum();
                percentage = percentage(rawTotal, maxMarks);
                method = "ASSESSMENT_MAX";
                context.normalizedCount++;
            } else {
                percentage = groupMarks.stream().map(StudentMark::getScore).filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue).average().orElse(0.0);
                percentage = clamp(percentage);
                method = "ASSUMED_PERCENTAGE";
                context.assumedPercentageCount++;
                context.missingMaxGroups.add(key.loId() + " / batch " + key.batch());
            }
            observations.add(new Observation(key.studentId(), key.loId(), key.loName(), key.batch(),
                round1(percentage), method));
        }
        observations.sort(Comparator.comparing(Observation::batch, TrendService::compareLabels)
            .thenComparing(Observation::loId).thenComparing(Observation::studentId));
        return observations;
    }

    private double getMaxMarks(String loId, String batch, MarkType markType) {
        if ("Unspecified".equals(batch)) return 0.0;
        return sumMaxMarks(assessmentItemRepository
            .findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(
                loId, batch, markType.name()));
    }

    private double sumMaxMarks(Collection<AssessmentItem> items) {
        if (items == null) return 0.0;
        return items.stream().map(AssessmentItem::getMaxMarks).filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue).sum();
    }

    private List<Map<String, Object>> buildLoPerformance(List<Observation> observations, double target) {
        Map<String, List<Observation>> byLo = observations.stream()
            .collect(Collectors.groupingBy(Observation::loId, TreeMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        byLo.forEach((loId, values) -> {
            double actual = average(values, Observation::percentage);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("loId", loId);
            item.put("label", loId);
            item.put("name", values.get(0).loName());
            item.put("actual", round1(actual));
            item.put("value", round1(actual));
            item.put("target", round1(target));
            item.put("gap", round1(actual - target));
            item.put("studentCount", distinctCount(values, Observation::studentId));
            item.put("observationCount", values.size());
            item.put("status", attainmentStatus(actual, target));
            result.add(item);
        });
        return result;
    }

    private Map<String, List<Map<String, Object>>> buildLoTrend(List<Observation> observations,
                                                                 double passThreshold, double target) {
        Map<String, List<Observation>> byLo = observations.stream().collect(Collectors.groupingBy(
            observation -> observation.loId() + " - " + observation.loName(),
            LinkedHashMap::new, Collectors.toList()));
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        byLo.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Map<String, List<Observation>> byBatch = entry.getValue().stream().collect(Collectors.groupingBy(
                Observation::batch, () -> new TreeMap<>(TrendService::compareLabels), Collectors.toList()));
            List<Map<String, Object>> points = new ArrayList<>();
            Double previous = null;
            for (Map.Entry<String, List<Observation>> batchEntry : byBatch.entrySet()) {
                List<Observation> values = batchEntry.getValue();
                double actual = average(values, Observation::percentage);
                long passed = values.stream().filter(o -> o.percentage() >= passThreshold).count();
                Map<String, Object> point = trendPoint(batchEntry.getKey(), actual, target,
                    values.size(), passed, passThreshold, previous);
                points.add(point);
                previous = actual;
            }
            result.put(entry.getKey(), points);
        });
        return result;
    }

    private List<Map<String, Object>> buildModuleTrend(List<Observation> observations,
                                                        double passThreshold, double target) {
        Map<String, List<Observation>> byBatch = observations.stream().collect(Collectors.groupingBy(
            Observation::batch, () -> new TreeMap<>(TrendService::compareLabels), Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        Double previous = null;
        for (Map.Entry<String, List<Observation>> entry : byBatch.entrySet()) {
            List<StudentResult> students = buildStudentResults(entry.getValue());
            double actual = average(students, StudentResult::percentage);
            long passed = students.stream().filter(student -> student.percentage() >= passThreshold).count();
            result.add(trendPoint(entry.getKey(), actual, target, students.size(), passed, passThreshold, previous));
            previous = actual;
        }
        return result;
    }

    private Map<String, Object> trendPoint(String batch, double actual, double target,
                                            long total, long passed, double threshold, Double previous) {
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("batch", batch);
        point.put("year", batchLabel(batch));
        point.put("average", round1(actual));
        point.put("target", round1(target));
        point.put("gap", round1(actual - target));
        point.put("studentCount", total);
        point.put("passedStudents", passed);
        point.put("passRate", round1(percent(passed, total)));
        point.put("passThreshold", round1(threshold));
        point.put("delta", previous == null ? null : round1(actual - previous));
        point.put("status", previous == null ? "BASELINE" : trendStatus(actual - previous));
        return point;
    }

    private List<StudentResult> buildStudentResults(List<Observation> observations) {
        Map<StudentResultKey, List<Observation>> grouped = observations.stream().collect(Collectors.groupingBy(
            observation -> new StudentResultKey(observation.studentId(), observation.batch()),
            LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream().map(entry -> new StudentResult(
                entry.getKey().studentId(), entry.getKey().batch(),
                round1(average(entry.getValue(), Observation::percentage))))
            .sorted(Comparator.comparing(StudentResult::batch, TrendService::compareLabels)
                .thenComparing(StudentResult::studentId)).toList();
    }

    private List<Map<String, Object>> buildPoPerformance(String moduleId, String focusLoId,
                                                          List<Map<String, Object>> loPerformance,
                                                          double target, List<String> warnings) {
        Map<String, Double> loScores = loPerformance.stream().collect(Collectors.toMap(
            item -> String.valueOf(item.get("loId")), item -> number(item.get("actual")), (a, b) -> a));
        List<OutcomeMapping> mappings = safeList(outcomeMappingRepository
            .findByLearningOutcome_Module_ModuleIdAndStatus(moduleId, OutcomeMapping.ApprovalStatus.APPROVED))
            .stream().filter(mapping -> focusLoId == null || (mapping.getLearningOutcome() != null
                && focusLoId.equalsIgnoreCase(mapping.getLearningOutcome().getId())))
            .filter(mapping -> mapping.getWeight() != null && mapping.getWeight() > 0)
            .filter(mapping -> mapping.getProgramOutcome() != null && mapping.getLearningOutcome() != null).toList();

        if (mappings.isEmpty() && !loPerformance.isEmpty()) {
            warnings.add("No approved LO-to-PO mappings matched this scope; PO analysis is unavailable.");
            return Collections.emptyList();
        }
        Map<String, List<OutcomeMapping>> byPo = mappings.stream().collect(Collectors.groupingBy(
            mapping -> safeName(mapping.getProgramOutcome().getCode(), mapping.getProgramOutcome().getPoId()),
            TreeMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        byPo.forEach((poCode, poMappings) -> {
            double weightedTotal = 0.0;
            double totalWeight = 0.0;
            Set<String> mappedLos = new LinkedHashSet<>();
            String title = "";
            for (OutcomeMapping mapping : poMappings) {
                String loId = mapping.getLearningOutcome().getId();
                Double loScore = loScores.get(loId);
                if (loScore == null) continue;
                weightedTotal += loScore * mapping.getWeight();
                totalWeight += mapping.getWeight();
                mappedLos.add(loId);
                title = safeName(mapping.getProgramOutcome().getTitle(), poCode);
            }
            if (totalWeight == 0) return;
            double actual = weightedTotal / totalWeight;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("poCode", poCode);
            item.put("label", poCode);
            item.put("name", title);
            item.put("actual", round1(actual));
            item.put("value", round1(actual));
            item.put("target", round1(target));
            item.put("gap", round1(actual - target));
            item.put("mappedLoCount", mappedLos.size());
            item.put("status", attainmentStatus(actual, target));
            result.add(item);
        });
        return result;
    }

    private Map<String, Object> buildPassFail(List<StudentResult> students, double threshold) {
        long pass = students.stream().filter(student -> student.percentage() >= threshold).count();
        long fail = students.size() - pass;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", List.of("Pass", "Fail"));
        result.put("data", List.of(pass, fail));
        result.put("total", students.size());
        result.put("passRate", round1(percent(pass, students.size())));
        result.put("threshold", round1(threshold));
        return result;
    }

    private Map<String, Object> buildScoreBands(List<StudentResult> students) {
        int[] bands = new int[6];
        for (StudentResult student : students) {
            double score = student.percentage();
            if (score < 40) bands[0]++;
            else if (score < 50) bands[1]++;
            else if (score < 60) bands[2]++;
            else if (score < 70) bands[3]++;
            else if (score < 80) bands[4]++;
            else bands[5]++;
        }
        return Map.of("labels", List.of("0–39", "40–49", "50–59", "60–69", "70–79", "80–100"),
            "data", List.of(bands[0], bands[1], bands[2], bands[3], bands[4], bands[5]),
            "total", students.size());
    }

    private Map<String, Object> buildSummary(List<StudentResult> students,
                                              List<Map<String, Object>> loPerformance,
                                              double target, List<Observation> observations,
                                              double passThreshold) {
        double average = average(students, StudentResult::percentage);
        long passed = students.stream().filter(student -> student.percentage() >= passThreshold).count();
        long targetMet = loPerformance.stream().filter(item -> number(item.get("actual")) >= target).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("average", round1(average));
        result.put("passRate", round1(percent(passed, students.size())));
        result.put("totalStudents", distinctCount(observations, Observation::studentId));
        result.put("studentResults", students.size());
        result.put("observationCount", observations.size());
        result.put("loCount", loPerformance.size());
        result.put("weakAreaCount", loPerformance.size() - targetMet);
        result.put("targetMet", targetMet);
        return result;
    }

    private Map<String, Object> buildMeta(String moduleId, String focusLoId, String batch,
                                          MarkType markType, double threshold, double target,
                                          List<String> batches, List<String> markTypes) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("batch", batch);
        filters.put("markType", markType == null ? null : markType.name());
        filters.put("loId", focusLoId);
        filters.put("passThreshold", round1(threshold));
        filters.put("target", round1(target));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("moduleId", moduleId);
        meta.put("scope", focusLoId == null ? "MODULE" : "LEARNING_OUTCOME");
        meta.put("filters", filters);
        meta.put("availableFilters", Map.of("batches", batches, "markTypes", markTypes));
        meta.put("unit", "PERCENT");
        meta.put("formulaVersion", "QA-GRAPH-1.0");
        meta.put("generatedAt", Instant.now().toString());
        return meta;
    }

    private Map<String, Object> buildDataQuality(NormalizationContext context,
                                                  List<Observation> observations,
                                                  List<StudentResult> students,
                                                  List<String> warnings) {
        if (!context.missingMaxGroups.isEmpty()) {
            warnings.add("Some LO/batch groups have no assessment maximum and were treated as percentages: "
                + String.join(", ", context.missingMaxGroups));
        }
        Map<String, Object> quality = new LinkedHashMap<>();
        quality.put("hasData", !observations.isEmpty());
        quality.put("normalizedObservationCount", context.normalizedCount);
        quality.put("assumedPercentageObservationCount", context.assumedPercentageCount);
        quality.put("sourceMarkCount", context.sourceMarkCount);
        quality.put("studentResultCount", students.size());
        quality.put("warnings", warnings.stream().distinct().toList());
        quality.put("normalizationRule", "Assessment maximum where available; otherwise stored score is treated as a percentage.");
        return quality;
    }

    private List<Map<String, Object>> resolveFocusTrend(Map<String, List<Map<String, Object>>> loTrend,
                                                         String focusLoId,
                                                         List<Map<String, Object>> moduleTrend) {
        if (focusLoId == null) return moduleTrend;
        return loTrend.entrySet().stream()
            .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT)
                .startsWith(focusLoId.toLowerCase(Locale.ROOT) + " -"))
            .map(Map.Entry::getValue).findFirst().orElse(Collections.emptyList());
    }

    private boolean isUsableMark(StudentMark mark) {
        return mark != null && mark.getScore() != null && mark.getStudent() != null
            && mark.getStudent().getStudentId() != null && mark.getLos() != null
            && mark.getLos().getId() != null;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String safeName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String batchLabel(String batch) {
        return "Unspecified".equals(batch) ? batch : "Batch " + batch;
    }

    private static String trendStatus(double delta) {
        if (delta > 5) return "IMPROVED";
        if (delta < -5) return "DECLINED";
        return "STABLE";
    }

    private static String attainmentStatus(double actual, double target) {
        if (actual >= target) return "TARGET_MET";
        if (actual >= target - 10) return "WATCH";
        return "ACTION_REQUIRED";
    }

    private static void validatePercentage(String name, double value) {
        if (!Double.isFinite(value) || value < 0 || value > 100) {
            throw new IllegalArgumentException(name + " must be between 0 and 100");
        }
    }

    private static double percentage(double score, double maximum) {
        return maximum <= 0 ? clamp(score) : clamp((score / maximum) * 100.0);
    }

    private static double percent(long part, long total) {
        return total == 0 ? 0.0 : (part * 100.0) / total;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double number(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static int compareLabels(String left, String right) {
        try {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        } catch (NumberFormatException ignored) {
            return left.compareToIgnoreCase(right);
        }
    }

    private static <T> double average(List<T> values, Function<T, Double> extractor) {
        return values.stream().map(extractor).filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private static <T> long distinctCount(List<T> values, Function<T, String> extractor) {
        return values.stream().map(extractor).filter(Objects::nonNull).distinct().count();
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return value instanceof List<?> ? (List<Map<String, Object>>) value : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<Map<String, Object>>> castTrendMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, List<Map<String, Object>>>) value : Collections.emptyMap();
    }

    private record ObservationKey(String studentId, String loId, String loName, String batch) {}
    private record StudentResultKey(String studentId, String batch) {}
    private record Observation(String studentId, String loId, String loName, String batch,
                               Double percentage, String normalizationMethod) {}
    private record StudentResult(String studentId, String batch, Double percentage) {}

    private final class NormalizationContext {
        private final MarkType selectedMarkType;
        private final int sourceMarkCount;
        private final Map<String, Double> maxMarksCache = new LinkedHashMap<>();
        private final Set<String> missingMaxGroups = new LinkedHashSet<>();
        private int normalizedCount;
        private int assumedPercentageCount;

        private NormalizationContext(MarkType selectedMarkType, int sourceMarkCount) {
            this.selectedMarkType = selectedMarkType;
            this.sourceMarkCount = sourceMarkCount;
        }

        private double maxMarks(String loId, String batch, List<StudentMark> groupMarks) {
            String typeKey = selectedMarkType == null ? "ALL" : selectedMarkType.name();
            String key = loId + "::" + batch + "::" + typeKey;
            return maxMarksCache.computeIfAbsent(key, ignored -> {
                if (selectedMarkType != null) return getMaxMarks(loId, batch, selectedMarkType);
                Set<MarkType> presentTypes = groupMarks.stream().map(StudentMark::getMarkType)
                    .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
                if (presentTypes.isEmpty()) return 0.0;
                return presentTypes.stream().mapToDouble(type -> getMaxMarks(loId, batch, type)).sum();
            });
        }
    }
}
