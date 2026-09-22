package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        return calculateStudentPOCredits(losIds, markType, batch, threshold, 0.0);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> calculateStudentPOCredits(List<String> losIds, String markType, String batch, int threshold, double maxMarksPerLo) {
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

        // 6. Build marks lookup: studentId -> loId -> assignment label -> score.
        // Marks are kept split by assignment rather than pre-summed because each assignment
        // carries its own max marks; a student's percentage for an LO can only be worked out
        // against the assignments that student actually has marks for.
        Map<String, Map<String, Map<String, Double>>> marksByStudentLoAndLabel = new HashMap<>();
        for (StudentMark mark : allMarks) {
            String studentId = mark.getStudent().getStudentId();
            String losId = mark.getLos().getId();
            if (mark.getScore() == null) continue;
            marksByStudentLoAndLabel
                    .computeIfAbsent(studentId, k -> new HashMap<>())
                    .computeIfAbsent(losId, k -> new LinkedHashMap<>())
                    .merge(normalizeLabel(mark.getAssignmentLabel()), mark.getScore(), Double::sum);
        }

        // 6b. Max marks per LO per assignment label, resolved once instead of per student.
        Map<String, Map<String, Double>> maxMarksByLoAndLabel = new HashMap<>();
        for (String losId : losIds) {
            maxMarksByLoAndLabel.put(losId, getMaxMarksByAssignmentLabel(losId, batch, markType));
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

            Map<String, Map<String, Double>> studentMarks =
                    marksByStudentLoAndLabel.getOrDefault(studentId, Collections.emptyMap());
            Map<String, String> loScoresMap = new LinkedHashMap<>();

            for (String losId : losIds) {
                Map<String, Double> scoresByLabel = studentMarks.get(losId);
                Double score = totalScore(scoresByLabel);

                // Compute LO percentage: if assessment items exist with max marks, use (score / maxMarks) * 100
                // Otherwise, assume score is already a percentage (legacy behavior)
                double loPercentage = 0.0;
                boolean hasAssessmentItems = false;

                if (score != null) {
                    // Denominator covers only the assignments this student has marks for. Charging
                    // a student for an assignment they have no marks under (a second assignment they
                    // missed, or one that only some LOs appear in) would sink an LO they passed.
                    Map<String, Double> maxByLabel = maxMarksByLoAndLabel.getOrDefault(losId, Collections.emptyMap());
                    double scored = 0.0;
                    double possible = 0.0;
                    for (Map.Entry<String, Double> entry : scoresByLabel.entrySet()) {
                        Double labelMax = maxByLabel.get(entry.getKey());
                        if (labelMax != null && labelMax > 0) {
                            scored += entry.getValue();
                            possible += labelMax;
                        }
                    }

                    if (possible > 0) {
                        // Normalize: scores are aggregated from question-wise / LO-wise imports
                        loPercentage = (scored / possible) * 100.0;
                        hasAssessmentItems = true;
                    } else if (maxMarksPerLo > 0) {
                        // Bulk upload with known max marks per LO, applied once per assignment
                        loPercentage = (score / (maxMarksPerLo * scoresByLabel.size())) * 100.0;
                    } else {
                        // Legacy: assume each score is already a percentage (0-100)
                        loPercentage = score / scoresByLabel.size();
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
     * Helper: max marks available for an LO in a batch/markType, broken down by assignment label.
     *
     * A markType can hold several assessments — "Assignment 01", "Assignment 02" and so on —
     * and a student's StudentMark is recorded per assignment. Returning one summed total would
     * mean measuring a student against every assignment recorded for the module, including ones
     * they have no marks under, so the breakdown is kept and the caller picks the assignments
     * that apply to each student.
     *
     * @param loId Learning Outcome ID
     * @param batch Batch identifier
     * @param markType Mark type (FINAL_EXAM or ASSIGNMENT)
     * @return assignment label (normalized, "" when unlabelled) → max marks; empty if no items found
     */
    private Map<String, Double> getMaxMarksByAssignmentLabel(String loId, String batch, String markType) {
        MarkType type = MarkType.valueOf(markType.toUpperCase());
        List<AssessmentItem> items = assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(loId, batch, markType, type);

        Map<String, List<AssessmentItem>> itemsByLabel = items.stream()
                .collect(Collectors.groupingBy(this::assignmentLabelOf, LinkedHashMap::new, Collectors.toList()));

        Map<String, Double> maxByLabel = new LinkedHashMap<>();
        for (Map.Entry<String, List<AssessmentItem>> entry : itemsByLabel.entrySet()) {
            maxByLabel.put(entry.getKey(), maxMarksForSingleAssignment(entry.getValue()));
        }
        return maxByLabel;
    }

    /** Total of a student's scores for one LO, or null when they have no marks for it. */
    private Double totalScore(Map<String, Double> scoresByLabel) {
        if (scoresByLabel == null || scoresByLabel.isEmpty()) return null;
        return scoresByLabel.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private String assignmentLabelOf(AssessmentItem item) {
        AssessmentTemplate template = item.getAssessmentTemplate();
        return normalizeLabel(template == null ? null : template.getAssignmentLabel());
    }

    /** Assignment labels are matched between marks and templates, so null and "" have to agree. */
    private String normalizeLabel(String label) {
        return label == null ? "" : label.trim();
    }

    /**
     * Max marks for one assignment label. Re-uploading an assignment overwrites the student's
     * aggregated mark but leaves the earlier template behind, so the denominator must come from
     * the same (latest) upload the mark came from - not the sum of every template sharing the label.
     */
    private double maxMarksForSingleAssignment(List<AssessmentItem> labelItems) {
        return sumMaxMarks(latestTemplateItems(labelItems));
    }

    /**
     * The items of the most recent template among those sharing an assignment label. Downloading a
     * template again for the same assignment mints a new template without retiring the old one, so
     * several can share a label; only the newest one describes the marks that are on file.
     */
    private List<AssessmentItem> latestTemplateItems(List<AssessmentItem> labelItems) {
        Map<String, List<AssessmentItem>> itemsByTemplate = labelItems.stream()
                .collect(Collectors.groupingBy(item -> {
                    AssessmentTemplate template = item.getAssessmentTemplate();
                    return template == null || template.getId() == null ? "" : template.getId();
                }, LinkedHashMap::new, Collectors.toList()));

        if (itemsByTemplate.size() <= 1) {
            return labelItems;
        }

        return itemsByTemplate.values().stream()
                .max(Comparator.comparing(this::templateCreatedAt))
                .orElse(Collections.emptyList());
    }

    private LocalDateTime templateCreatedAt(List<AssessmentItem> templateItems) {
        return templateItems.stream()
                .map(AssessmentItem::getAssessmentTemplate)
                .filter(Objects::nonNull)
                .map(AssessmentTemplate::getCreatedAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(LocalDateTime.MIN);
    }

    private double sumMaxMarks(List<AssessmentItem> items) {
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
            List<AssessmentItem> items = assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(loId, batch, markType, MarkType.valueOf(markType.toUpperCase()));
            if (!items.isEmpty()) {
                double totalScore = 0;
                double totalMaxMarks = 0;
                // Grouped by assignment so a student is only measured against the assignments they
                // have recorded scores under — a markType can hold several, and the ones a student
                // has no scores for must not count toward their denominator.
                Map<String, List<AssessmentItem>> itemsByLabel = items.stream()
                        .collect(Collectors.groupingBy(this::assignmentLabelOf, LinkedHashMap::new, Collectors.toList()));

                for (List<AssessmentItem> labelItems : itemsByLabel.values()) {
                    double labelScore = 0;
                    double labelMax = 0;
                    boolean sat = false;
                    for (AssessmentItem item : latestTemplateItems(labelItems)) {
                        Optional<StudentAssessmentScore> scoreOpt = studentAssessmentScoreRepository.findByStudentAndAssessmentItem(student, item);
                        if (scoreOpt.isPresent()) {
                            labelScore += scoreOpt.get().getScore();
                            sat = true;
                        }
                        labelMax += item.getMaxMarks() != null ? item.getMaxMarks() : 0.0;
                    }
                    if (sat) {
                        totalScore += labelScore;
                        totalMaxMarks += labelMax;
                    }
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
