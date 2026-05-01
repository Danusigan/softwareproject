package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class POAttainmentService {

    @Autowired
    private StudentMarkRepository studentMarkRepository;

    @Autowired
    private OutcomeMappingRepository outcomeMappingRepository;

    @Autowired
    private LosRepository losRepository;

    @Autowired
    private StudentAssessmentScoreRepository studentAssessmentScoreRepository;

    @Autowired
    private AssessmentItemRepository assessmentItemRepository;

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Calculate per-student PO credits based on LO pass/fail and LO-PO mappings.
     *
     * Logic: For each student, for each LO:
     *   - If student's score >= threshold → student PASSED this LO
     *   - For each approved LO→PO mapping of that LO, add 100% of the mapping weight to student's PO credit
     *   - If student failed → add 0
     *
     * @param losIds    List of LO IDs to consider
     * @param markType  FINAL_EXAM or ASSIGNMENT
     * @param batch     Batch year
     * @param threshold Pass threshold (0-100)
     * @return Map with keys: "students", "poList", "credits", "maxCredits", "loPoMappings"
     */
    @Transactional(readOnly = true)
    public Map<String, Object> calculateStudentPOCredits(List<String> losIds, String markType, String batch, int threshold) {
        MarkType type = MarkType.valueOf(markType.toUpperCase());

        // 1. Get all distinct students for these LOs, markType, and batch
        List<Student> students = studentMarkRepository
                .findDistinctStudentsByLosIdsAndMarkTypeAndBatch(losIds, type, batch);

        // 2. Get all marks for these LOs
        List<StudentMark> allMarks = studentMarkRepository
                .findByLosIdsAndMarkTypeAndBatch(losIds, type, batch);

        // 3. Get ALL mappings for these LOs (even if pending)
        List<OutcomeMapping> allMappings = new ArrayList<>();
        List<Map<String, String>> loListInfo = new ArrayList<>();

        for (String losId : losIds) {
            List<OutcomeMapping> loMappings = outcomeMappingRepository
                    .findByLearningOutcome_Id(losId);
            allMappings.addAll(loMappings);

            Los los = losRepository.findById(losId).orElse(null);
            Map<String, String> info = new LinkedHashMap<>();
            info.put("id", losId);
            info.put("name", los != null ? los.getName() : losId);
            loListInfo.add(info);
        }

        // 4. Build a lookup: LO ID -> List of (PO code, weight)
        Map<String, List<OutcomeMapping>> mappingsByLo = allMappings.stream()
                .collect(Collectors.groupingBy(m -> m.getLearningOutcome().getId()));

        // 5. Collect all unique PO codes (sorted)
        Set<String> poCodeSet = new TreeSet<>();
        for (OutcomeMapping m : allMappings) {
            poCodeSet.add(m.getProgramOutcome().getCode());
        }
        List<String> poList = new ArrayList<>(poCodeSet);

        // 6. Build marks lookup: studentId -> loId -> score
        Map<String, Map<String, Double>> marksByStudentAndLo = new HashMap<>();
        for (StudentMark mark : allMarks) {
            String studentId = mark.getStudent().getStudentId();
            String losId = mark.getLos().getId();
            marksByStudentAndLo
                    .computeIfAbsent(studentId, k -> new HashMap<>())
                    .put(losId, mark.getScore());
        }

        // 7. Calculate max possible credit per PO (sum of all LO weights mapped to that PO)
        Map<String, Integer> maxCredits = new LinkedHashMap<>();
        for (String poCode : poList) {
            int totalWeight = 0;
            for (OutcomeMapping m : allMappings) {
                if (m.getProgramOutcome().getCode().equals(poCode)) {
                    totalWeight += m.getWeight();
                }
            }
            maxCredits.put(poCode, totalWeight);
        }

        // 8. Calculate per-student PO credits
        // credits: { studentId: { poCode: creditValue } }
        Map<String, Map<String, Integer>> credits = new LinkedHashMap<>();
        // Also track per-student LO pass/fail detail
        List<Map<String, Object>> studentDetails = new ArrayList<>();

        for (Student student : students) {
            String studentId = student.getStudentId();
            Map<String, Integer> studentCredits = new LinkedHashMap<>();

            // Initialize all POs to 0
            for (String poCode : poList) {
                studentCredits.put(poCode, 0);
            }

            Map<String, Double> studentMarks = marksByStudentAndLo.getOrDefault(studentId, new HashMap<>());
            Map<String, String> loScoresMap = new LinkedHashMap<>();

            for (String losId : losIds) {
                Double score = studentMarks.get(losId);

                // Compute LO percentage: if assessment items exist with max marks, use (score / totalMaxMarks) * 100
                // Otherwise, assume score is already a percentage (legacy behavior)
                double loPercentage = 0.0;
                boolean hasAssessmentItems = false;

                if (score != null) {
                    double totalMaxMarks = getTotalMaxMarksForLO(losId, batch, markType);
                    if (totalMaxMarks > 0) {
                        // Normalize: score is aggregated from question-wise imports
                        loPercentage = (score / totalMaxMarks) * 100.0;
                        hasAssessmentItems = true;
                    } else {
                        // Legacy: assume score is already a percentage (0-100)
                        loPercentage = score;
                    }
                }

                boolean passed = score != null && loPercentage >= threshold;

                if (score != null) {
                    loScoresMap.put(losId, (passed ? "Pass" : "Fail") + " (" + String.format("%.2f", score) + (hasAssessmentItems ? "%, " : ", ") + String.format("%.1f", loPercentage) + ")");
                } else {
                    loScoresMap.put(losId, "N/A");
                }

                if (passed) {
                    // Add 100% of each mapping weight for this LO to the corresponding PO
                    List<OutcomeMapping> loMappings = mappingsByLo.getOrDefault(losId, Collections.emptyList());
                    for (OutcomeMapping mapping : loMappings) {
                        String poCode = mapping.getProgramOutcome().getCode();
                        studentCredits.merge(poCode, mapping.getWeight(), Integer::sum);
                    }
                }
            }

            credits.put(studentId, studentCredits);

            // Build student detail object
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("studentId", studentId);
            detail.put("studentName", student.getStudentName());
            detail.put("poCredits", studentCredits);
            detail.put("loScores", loScoresMap);

            // Calculate total credit for this student
            int totalCredit = studentCredits.values().stream().mapToInt(Integer::intValue).sum();
            detail.put("totalCredit", totalCredit);

            studentDetails.add(detail);
        }

        // 9. Calculate total max credit
        int totalMaxCredit = maxCredits.values().stream().mapToInt(Integer::intValue).sum();

        // 10. Build LO-PO mapping info for frontend display
        List<Map<String, Object>> loPoMappingInfo = new ArrayList<>();
        for (String losId : losIds) {
            Los los = losRepository.findById(losId).orElse(null);
            List<OutcomeMapping> loMappings = mappingsByLo.getOrDefault(losId, Collections.emptyList());
            for (OutcomeMapping m : loMappings) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("loId", losId);
                info.put("loName", los != null ? los.getName() : losId);
                info.put("poCode", m.getProgramOutcome().getCode());
                info.put("poTitle", m.getProgramOutcome().getTitle());
                info.put("weight", m.getWeight());
                loPoMappingInfo.add(info);
            }
        }

        // 11. Build result
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("poList", poList);
        result.put("loList", loListInfo);
        result.put("maxCredits", maxCredits);
        result.put("totalMaxCredit", totalMaxCredit);
        result.put("threshold", threshold);
        result.put("studentCount", students.size());
        result.put("students", studentDetails);
        result.put("loPoMappings", loPoMappingInfo);

        return result;
    }

    /**
     * Helper: Get total max marks for an LO across all assessment items in a batch/markType.
     * @param loId Learning Outcome ID
     * @param batch Batch identifier
     * @param markType Mark type (FINAL_EXAM or ASSIGNMENT)
     * @return Total max marks, or 0 if no items found
     */
    private double getTotalMaxMarksForLO(String loId, String batch, String markType) {
        List<AssessmentItem> items = assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(loId, batch, markType);
        return items.stream().mapToDouble(item -> item.getMaxMarks() != null ? item.getMaxMarks() : 0.0).sum();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> calculateOverallPOAttainment(String batch, String markType, Double poThreshold) {
        if (poThreshold == null) poThreshold = 60.0; // Default PO attainment benchmark

        // 1. Find all approved LO->PO mappings
        List<OutcomeMapping> approvedMappings = outcomeMappingRepository.findByStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        if (approvedMappings.isEmpty()) {
            return Map.of("message", "No approved LO-PO mappings found.", "poAttainment", Collections.emptyList());
        }

        // 2. Group mappings by PO
        Map<ProgramOutcome, List<OutcomeMapping>> mappingsByPo = approvedMappings.stream()
            .collect(Collectors.groupingBy(OutcomeMapping::getProgramOutcome));

        // 3. Get all relevant students
        List<Student> students = studentRepository.findByBatch(batch);
        if (students.isEmpty()) {
            return Map.of("message", "No students found for batch: " + batch, "poAttainment", Collections.emptyList());
        }
        long totalStudents = students.size();

        List<Map<String, Object>> poResults = new ArrayList<>();

        // 4. For each PO, calculate its attainment
        for (Map.Entry<ProgramOutcome, List<OutcomeMapping>> entry : mappingsByPo.entrySet()) {
            ProgramOutcome po = entry.getKey();
            List<OutcomeMapping> loMappingsForPo = entry.getValue();
            Set<String> loIdsForPo = loMappingsForPo.stream().map(m -> m.getLearningOutcome().getId()).collect(Collectors.toSet());

            long studentsAchievingPo = 0;

            // 5. For each student, check if they achieved this PO
            for (Student student : students) {
                boolean studentAchievedPo = checkStudentPoAchievement(student, loIdsForPo, batch, markType);
                if (studentAchievedPo) {
                    studentsAchievingPo++;
                }
            }

            // 6. Calculate PO attainment percentage
            double attainmentPercent = (totalStudents > 0) ? ((double) studentsAchievingPo / totalStudents) * 100.0 : 0.0;

            Map<String, Object> poResult = new LinkedHashMap<>();
            poResult.put("poId", po.getId());
            poResult.put("poCode", po.getCode());
            poResult.put("poDescription", po.getDescription());
            poResult.put("totalStudents", totalStudents);
            poResult.put("studentsAchieved", studentsAchievingPo);
            poResult.put("attainmentPercent", attainmentPercent);
            poResult.put("benchmark", poThreshold);
            poResult.put("metBenchmark", attainmentPercent >= poThreshold);
            poResult.put("formula", "(studentsAchieved / totalStudents) * 100");
            poResults.add(poResult);
        }

        // Sort results by PO code
        poResults.sort(Comparator.comparing(m -> (String) m.get("poCode")));

        Map<String, Object> finalResult = new LinkedHashMap<>();
        finalResult.put("batch", batch);
        finalResult.put("markType", markType);
        finalResult.put("poAttainment", poResults);
        finalResult.put("count", poResults.size());

        return finalResult;
    }

    private boolean checkStudentPoAchievement(Student student, Set<String> loIdsForPo, String batch, String markType) {
        return checkStudentPoAchievement(student, loIdsForPo, batch, markType, 50.0);
    }

    /**
     * Check if a student achieves a PO (passes all LOs mapped to it).
     * Uses normalized percentage thresholds: if assessment items exist, compute (score / maxMarks) * 100;
     * otherwise assume StudentMark.score is already a percentage.
     * @param student Student entity
     * @param loIdsForPo Set of LO IDs mapped to the PO
     * @param batch Batch identifier
     * @param markType Mark type
     * @param loThreshold LO pass threshold (percentage, 0-100)
     * @return true if student passed all LOs, false otherwise
     */
    private boolean checkStudentPoAchievement(Student student, Set<String> loIdsForPo, String batch, String markType, double loThreshold) {
        // A student achieves a PO if they pass ALL LOs mapped to it.
        for (String loId : loIdsForPo) {
            // Check question-based scores first
            List<AssessmentItem> items = assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(loId, batch, markType);
            if (!items.isEmpty()) {
                double totalScore = 0;
                double totalMaxMarks = 0;
                for (AssessmentItem item : items) {
                    Optional<StudentAssessmentScore> scoreOpt = studentAssessmentScoreRepository.findByStudentAndAssessmentItem(student, item);
                    if (scoreOpt.isPresent()) {
                        totalScore += scoreOpt.get().getScore();
                    }
                    totalMaxMarks += item.getMaxMarks();
                }
                // Normalize: compute percentage and compare to threshold
                if (totalMaxMarks > 0) {
                    double loPercentage = (totalScore / totalMaxMarks) * 100.0;
                    if (loPercentage < loThreshold) {
                        return false; // Failed this LO, so cannot achieve the PO
                    }
                } else {
                    // No max marks found; assume score is percentage and compare directly
                    if (totalScore < loThreshold) {
                        return false;
                    }
                }
            } else {
                // Fallback to legacy StudentMark (assume score is already percentage)
                Optional<StudentMark> markOpt = studentMarkRepository.findByStudentAndLos_IdAndBatchAndMarkType(student, loId, batch, MarkType.valueOf(markType));
                if (markOpt.isEmpty() || markOpt.get().getScore() < loThreshold) {
                    return false; // Failed this LO
                }
            }
        }
        return true; // Passed all required LOs for this PO
    }
}
