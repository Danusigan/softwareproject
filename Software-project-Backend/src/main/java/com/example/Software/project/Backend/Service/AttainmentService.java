package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttainmentService {

    @Autowired
    private StudentMarkRepository markRepository;
    @Autowired
    private OutcomeMappingRepository mappingRepository;
    @Autowired
    private LosRepository losRepository;
    @Autowired
    private AssessmentItemRepository assessmentItemRepository;
    @Autowired
    private StudentAssessmentScoreRepository studentAssessmentScoreRepository;

    // 1. Calculate LO Attainment Level (0, 1, 2, or 3)
    public int calculateLOLevel(String loId) {
        Double attainment = calculateQuestionBasedLoAttainmentPercent(loId, 50.0, null);
        if (attainment != null) {
            if (attainment >= 80) return 3;
            if (attainment >= 70) return 2;
            if (attainment >= 60) return 1;
            return 0;
        }

        // Fallback: legacy per-LO StudentMark entries
        List<StudentMark> marks = markRepository.findByLos_Id(loId);
        if (marks.isEmpty()) return 0;

        long totalStudents = marks.size();
        long passedStudents = marks.stream().filter(m -> m.getScore() >= 50.0).count();
        double percentage = (double) passedStudents / totalStudents * 100;

        if (percentage >= 80) return 3;
        if (percentage >= 70) return 2;
        if (percentage >= 60) return 1;
        return 0;
    }

    public Map<String, Object> getLoAttainmentMetrics(List<String> loIds,
                                                      String templateId,
                                                      Double defaultThreshold,
                                                      Map<String, Double> loThresholds,
                                                      Map<Long, Double> itemThresholds) {
        if (defaultThreshold == null) defaultThreshold = 50.0;
        if (loThresholds == null) loThresholds = new HashMap<>();
        if (itemThresholds == null) itemThresholds = new HashMap<>();

        Set<String> resolvedLoIds = new LinkedHashSet<>();
        if (loIds != null) {
            for (String loId : loIds) {
                if (loId != null && !loId.trim().isEmpty()) {
                    resolvedLoIds.add(loId.trim());
                }
            }
        }

        if (templateId != null && !templateId.trim().isEmpty()) {
            List<AssessmentItem> templateItems = assessmentItemRepository.findByAssessmentTemplate_IdOrderByQuestionNumber(templateId.trim());
            for (AssessmentItem item : templateItems) {
                if (item.getLos() != null && item.getLos().getId() != null) {
                    resolvedLoIds.add(item.getLos().getId());
                }
            }
        }

        List<Map<String, Object>> loResults = new ArrayList<>();
        for (String loId : resolvedLoIds) {
            Los lo = losRepository.findById(loId).orElse(null);
            double loThreshold = loThresholds.getOrDefault(loId, defaultThreshold);

            List<AssessmentItem> items = assessmentItemRepository.findByLos_Id(loId);
            if (templateId != null && !templateId.trim().isEmpty()) {
                String tId = templateId.trim();
                items = items.stream()
                    .filter(i -> i.getAssessmentTemplate() != null && tId.equals(i.getAssessmentTemplate().getId()))
                    .toList();
            }

            if (items.isEmpty()) {
                loResults.add(Map.of(
                    "loId", loId,
                    "loName", lo != null ? lo.getName() : loId,
                    "thresholdPercent", loThreshold,
                    "totalStudents", 0,
                    "studentsAchieved", 0,
                    "attainmentPercent", 0.0,
                    "formula", "(studentsAchieved / totalStudents) * 100"
                ));
                continue;
            }

            double totalMax = items.stream().mapToDouble(i -> i.getMaxMarks() == null ? 0.0 : i.getMaxMarks()).sum();
            boolean hasItemThreshold = false;
            for (AssessmentItem item : items) {
                if (itemThresholds.containsKey(item.getId())) {
                    hasItemThreshold = true;
                    break;
                }
            }
            double requiredMarks;
            if (hasItemThreshold) {
                requiredMarks = 0.0;
                for (AssessmentItem item : items) {
                    double max = item.getMaxMarks() == null ? 0.0 : item.getMaxMarks();
                    double itemThreshold = itemThresholds.getOrDefault(item.getId(), loThreshold);
                    requiredMarks += (itemThreshold / 100.0) * max;
                }
            } else {
                requiredMarks = (loThreshold / 100.0) * totalMax;
            }

            List<StudentAssessmentScore> scores = studentAssessmentScoreRepository.findByAssessmentItem_Los_Id(loId);
            if (templateId != null && !templateId.trim().isEmpty()) {
                String tId = templateId.trim();
                scores = scores.stream()
                    .filter(s -> s.getAssessmentItem() != null
                        && s.getAssessmentItem().getAssessmentTemplate() != null
                        && tId.equals(s.getAssessmentItem().getAssessmentTemplate().getId()))
                    .toList();
            }

            Map<String, Double> studentTotals = new HashMap<>();
            for (StudentAssessmentScore s : scores) {
                if (s.getStudent() == null || s.getStudent().getStudentId() == null) continue;
                String sid = s.getStudent().getStudentId();
                double sc = s.getScore() == null ? 0.0 : s.getScore();
                studentTotals.put(sid, studentTotals.getOrDefault(sid, 0.0) + sc);
            }

            int totalStudents = studentTotals.size();
            long achieved = 0;
            if (totalStudents > 0) {
                for (Double total : studentTotals.values()) {
                    if (total >= requiredMarks) achieved++;
                }
            }
            double attainmentPercent = totalStudents == 0 ? 0.0 : ((double) achieved / totalStudents) * 100.0;

            Map<String, Object> result = new HashMap<>();
            result.put("loId", loId);
            result.put("loName", lo != null ? lo.getName() : loId);
            result.put("thresholdPercent", loThreshold);
            result.put("totalMaxMarks", totalMax);
            result.put("requiredMarks", requiredMarks);
            result.put("totalStudents", totalStudents);
            result.put("studentsAchieved", achieved);
            result.put("attainmentPercent", attainmentPercent);
            result.put("formula", "(studentsAchieved / totalStudents) * 100");
            loResults.add(result);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("defaultThreshold", defaultThreshold);
        response.put("loAttainment", loResults);
        response.put("count", loResults.size());
        return response;
    }

    private Double calculateQuestionBasedLoAttainmentPercent(String loId, Double thresholdPercent, Map<Long, Double> itemThresholds) {
        Los los = losRepository.findById(loId).orElse(null);
        if (los == null) return null;

        List<AssessmentItem> items = assessmentItemRepository.findByLos_Id(loId);
        if (items == null || items.isEmpty()) return null;

        double effectiveThresholdPercent = thresholdPercent != null ? thresholdPercent : 50.0;
        Map<Long, Double> effectiveItemThresholds = itemThresholds != null ? itemThresholds : new HashMap<>();

        double totalMax = items.stream().mapToDouble(i -> i.getMaxMarks() == null ? 0.0 : i.getMaxMarks()).sum();
        if (totalMax <= 0) return 0.0;

        boolean hasItemThreshold = false;
        for (AssessmentItem item : items) {
            if (effectiveItemThresholds.containsKey(item.getId())) {
                hasItemThreshold = true;
                break;
            }
        }
        double requiredMarks;
        if (hasItemThreshold) {
            requiredMarks = 0.0;
            for (AssessmentItem item : items) {
                double max = item.getMaxMarks() == null ? 0.0 : item.getMaxMarks();
                double itemThreshold = effectiveItemThresholds.getOrDefault(item.getId(), effectiveThresholdPercent);
                requiredMarks += (itemThreshold / 100.0) * max;
            }
        } else {
            requiredMarks = (effectiveThresholdPercent / 100.0) * totalMax;
        }

        List<StudentAssessmentScore> scores = studentAssessmentScoreRepository.findByAssessmentItem_Los_Id(loId);
        if (scores.isEmpty()) return 0.0;

        Map<String, Double> studentTotals = new HashMap<>();
        for (StudentAssessmentScore s : scores) {
            if (s.getStudent() == null || s.getStudent().getStudentId() == null) continue;
            String sid = s.getStudent().getStudentId();
            double sc = s.getScore() == null ? 0.0 : s.getScore();
            studentTotals.put(sid, studentTotals.getOrDefault(sid, 0.0) + sc);
        }

        long totalStudents = studentTotals.size();
        if (totalStudents == 0) return 0.0;

        long achieved = 0;
        for (Double total : studentTotals.values()) {
            if (total >= requiredMarks) achieved++;
        }
        return (double) achieved / totalStudents * 100.0;
    }

    // 2. Calculate PO Attainment for a Course
    public Map<String, Double> getPOAttainment(String moduleId) {
        List<OutcomeMapping> mappings = mappingRepository.findByLearningOutcome_Module_ModuleId(moduleId);

        // Group mappings by PO Code
        Map<String, List<OutcomeMapping>> poGroups = mappings.stream()
                .filter(m -> m.getStatus() == OutcomeMapping.ApprovalStatus.APPROVED)
                .collect(Collectors.groupingBy(m -> m.getProgramOutcome().getCode()));

        Map<String, Double> poScores = new HashMap<>();

        for (Map.Entry<String, List<OutcomeMapping>> entry : poGroups.entrySet()) {
            String poCode = entry.getKey();
            List<OutcomeMapping> poMappings = entry.getValue();

            double weightedSum = 0;
            double totalWeight = 0;

            for (OutcomeMapping map : poMappings) {
                int loLevel = calculateLOLevel(map.getLearningOutcome().getId());
                weightedSum += (loLevel * map.getWeight());
                totalWeight += map.getWeight();
            }

            double finalScore = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
            poScores.put(poCode, finalScore);
        }
        return poScores;
    }
}
