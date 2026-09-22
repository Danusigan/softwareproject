package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module; // disambiguate from java.lang.Module
import com.example.Software.project.Backend.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for POAttainmentService threshold normalization.
 *
 * Tests the fix for the bug where a student with a score of 6/10 (60%)
 * was incorrectly marked as FAIL when threshold was 50%.
 */
@DisplayName("POAttainmentService Threshold Normalization Tests")
class POAttainmentServiceTest {

    @Mock
    private StudentMarkRepository studentMarkRepository;

    @Mock
    private OutcomeMappingRepository outcomeMappingRepository;

    @Mock
    private LosRepository losRepository;

    @Mock
    private StudentAssessmentScoreRepository studentAssessmentScoreRepository;

    @Mock
    private AssessmentItemRepository assessmentItemRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentPoCreditRepository studentPoCreditRepository;

    @InjectMocks
    private POAttainmentService poAttainmentService;

    private Student student1;
    private Student student2;
    private Los los1;
    private ProgramOutcome po1;
    private OutcomeMapping mapping1;
    private AssessmentTemplate template1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test entities
        student1 = new Student();
        student1.setStudentId("EN001");
        student1.setStudentName("Student One");

        student2 = new Student();
        student2.setStudentId("EN002");
        student2.setStudentName("Student Two");

        los1 = new Los();
        los1.setId("LO001");
        los1.setName("Learning Outcome 1");

        po1 = new ProgramOutcome();
        po1.setId("PO001");
        po1.setPoId("PO1");
        po1.setCode("PO1");
        po1.setTitle("Program Outcome 1");

        mapping1 = new OutcomeMapping();
        mapping1.setId(1L);
        mapping1.setLearningOutcome(los1);
        mapping1.setProgramOutcome(po1);
        mapping1.setWeight(1);
        mapping1.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);

        template1 = new AssessmentTemplate();
        template1.setId("TEMPLATE001");
        template1.setBatch("20");
        template1.setMarkType("FINAL_EXAM");

    }

    @Test
    @DisplayName("Test 1: Score 6 out of max 10 (60%) with threshold 50% should PASS")
    void testScoreSixOutOfMaxTenWithThresholdFiftyPercentShouldPass() {
        // SCENARIO: LO has one assessment item with max marks = 10
        // Student scores 6/10 = 60% which is >= 50% threshold → PASS

        String losId = "LO001";
        String batch = "20";
        String markType = "FINAL_EXAM";
        int threshold = 50;

        List<String> losIds = Arrays.asList(losId);

        // Setup: Single assessment item with max marks 10
        AssessmentItem singleItem = new AssessmentItem();
        singleItem.setId(1L);
        singleItem.setQuestionLabel("Q1");
        singleItem.setMaxMarks(10.0); // Total max = 10
        singleItem.setLos(los1);
        singleItem.setAssessmentTemplate(template1);

        // Student score = 6 (aggregated), which represents 6/10 = 60%
        StudentMark mark = new StudentMark();
        mark.setStudent(student1);
        mark.setLos(los1);
        mark.setScore(6.0); // Aggregated score from question-wise import
        mark.setBatch(batch);
        mark.setMarkType(MarkType.FINAL_EXAM);

        // Mock repositories
        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(losIds, MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(student1));

        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(losIds, MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(mark));

        when(outcomeMappingRepository.findByLearningOutcome_Id(losId))
            .thenReturn(Arrays.asList(mapping1));

        when(losRepository.findById(losId))
            .thenReturn(Optional.of(los1));

        // Mock assessment items: total max = 10
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(losId, batch, markType, MarkType.FINAL_EXAM))
            .thenReturn(Arrays.asList(singleItem));

        // EXECUTE
        Map<String, Object> result = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold);

        // VERIFY
        assertNotNull(result);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("students");
        assertNotNull(students);
        assertEquals(1, students.size());

        Map<String, Object> studentDetail = students.get(0);
        assertEquals("EN001", studentDetail.get("studentId"));

        // Critical check: student should have passed because 6/10 = 60% >= 50%
        @SuppressWarnings("unchecked")
        Map<String, Integer> poCredits = (Map<String, Integer>) studentDetail.get("poCredits");
        assertNotNull(poCredits);

        // Since student passed the LO, they should receive the mapping weight (1) for PO1
        assertEquals(1, poCredits.get("PO1"), "Student should have received 1 credit for PO1 because 60% >= 50% threshold");
    }

    @Test
    @DisplayName("Test 2: Score 4/10 (40%) with threshold 50% should FAIL")
    void testScoreFourOutOfMaxTenWithThresholdFiftyPercentShouldFail() {
        // SCENARIO: LO has one assessment item with max marks = 10
        // Student scores 4/10 = 40% which is < 50% threshold → FAIL

        String losId = "LO001";
        String batch = "20";
        String markType = "FINAL_EXAM";
        int threshold = 50;

        List<String> losIds = Arrays.asList(losId);

        // Setup: Single assessment item with max marks 10
        AssessmentItem singleItem = new AssessmentItem();
        singleItem.setId(1L);
        singleItem.setQuestionLabel("Q1");
        singleItem.setMaxMarks(10.0);
        singleItem.setLos(los1);
        singleItem.setAssessmentTemplate(template1);

        // Student score = 4/10 = 40%
        StudentMark mark = new StudentMark();
        mark.setStudent(student1);
        mark.setLos(los1);
        mark.setScore(4.0);
        mark.setBatch(batch);
        mark.setMarkType(MarkType.FINAL_EXAM);

        // Mock repositories
        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(losIds, MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(student1));

        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(losIds, MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(mark));

        when(outcomeMappingRepository.findByLearningOutcome_Id(losId))
            .thenReturn(Arrays.asList(mapping1));

        when(losRepository.findById(losId))
            .thenReturn(Optional.of(los1));

        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(losId, batch, markType, MarkType.FINAL_EXAM))
            .thenReturn(Arrays.asList(singleItem));

        // EXECUTE
        Map<String, Object> result = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold);

        // VERIFY
        assertNotNull(result);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("students");
        assertNotNull(students);
        assertEquals(1, students.size());

        Map<String, Object> studentDetail = students.get(0);

        @SuppressWarnings("unchecked")
        Map<String, Integer> poCredits = (Map<String, Integer>) studentDetail.get("poCredits");

        // Student should NOT receive credit because 40% < 50% threshold
        assertEquals(0, poCredits.get("PO1"), "Student should have 0 credit for PO1 because 40% < 50% threshold");
    }

    @Test
    @DisplayName("Test 3: Multiple students with varying scores")
    void testMultipleStudentsWithVaryingScores() {
        // SCENARIO: Two students with different scores
        // Student 1: 6/10 = 60% → PASS
        // Student 2: 3/10 = 30% → FAIL

        String losId = "LO001";
        String batch = "20";
        String markType = "FINAL_EXAM";
        int threshold = 50;

        List<String> losIds = Arrays.asList(losId);

        AssessmentItem singleItem = new AssessmentItem();
        singleItem.setId(1L);
        singleItem.setMaxMarks(10.0);
        singleItem.setLos(los1);
        singleItem.setAssessmentTemplate(template1);

        // Student 1: score 6/10
        StudentMark mark1 = new StudentMark();
        mark1.setStudent(student1);
        mark1.setLos(los1);
        mark1.setScore(6.0);
        mark1.setBatch(batch);
        mark1.setMarkType(MarkType.FINAL_EXAM);

        // Student 2: score 3/10
        StudentMark mark2 = new StudentMark();
        mark2.setStudent(student2);
        mark2.setLos(los1);
        mark2.setScore(3.0);
        mark2.setBatch(batch);
        mark2.setMarkType(MarkType.FINAL_EXAM);

        // Mock repositories
        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(losIds, MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(student1, student2));

        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(losIds, MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(mark1, mark2));

        when(outcomeMappingRepository.findByLearningOutcome_Id(losId))
            .thenReturn(Arrays.asList(mapping1));

        when(losRepository.findById(losId))
            .thenReturn(Optional.of(los1));

        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(losId, batch, markType, MarkType.FINAL_EXAM))
            .thenReturn(Arrays.asList(singleItem));

        // EXECUTE
        Map<String, Object> result = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold);

        // VERIFY
        assertNotNull(result);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("students");
        assertEquals(2, students.size());

        // Student 1 should pass (60% >= 50%)
        Map<String, Object> student1Detail = students.stream()
            .filter(s -> "EN001".equals(s.get("studentId")))
            .findFirst()
            .orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Integer> student1Credits = (Map<String, Integer>) student1Detail.get("poCredits");
        assertEquals(1, student1Credits.get("PO1"), "Student 1 should pass with 60%");

        // Student 2 should fail (30% < 50%)
        Map<String, Object> student2Detail = students.stream()
            .filter(s -> "EN002".equals(s.get("studentId")))
            .findFirst()
            .orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Integer> student2Credits = (Map<String, Integer>) student2Detail.get("poCredits");
        assertEquals(0, student2Credits.get("PO1"), "Student 2 should fail with 30%");
    }

    @Test
    @DisplayName("Test 4: Legacy behavior - score already as percentage")
    void testLegacyBehaviorScoreAlreadyAsPercentage() {
        // SCENARIO: No assessment items found (legacy path)
        // StudentMark.score is treated as percentage directly (60% = 60)
        // With threshold 50%, student should PASS

        String losId = "LO001";
        String batch = "20";
        String markType = "FINAL_EXAM";
        int threshold = 50;

        List<String> losIds = Arrays.asList(losId);

        // Student score = 60 (treated as 60%)
        StudentMark mark = new StudentMark();
        mark.setStudent(student1);
        mark.setLos(los1);
        mark.setScore(60.0); // Legacy: score is percentage
        mark.setBatch(batch);
        mark.setMarkType(MarkType.FINAL_EXAM);

        // Mock repositories - NO assessment items found (legacy path)
        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(losIds, MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(student1));

        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(losIds, MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(mark));

        when(outcomeMappingRepository.findByLearningOutcome_Id(losId))
            .thenReturn(Arrays.asList(mapping1));

        when(losRepository.findById(losId))
            .thenReturn(Optional.of(los1));

        // NO assessment items found → uses legacy behavior
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(losId, batch, markType, MarkType.FINAL_EXAM))
            .thenReturn(Collections.emptyList());

        // EXECUTE
        Map<String, Object> result = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold);

        // VERIFY
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("students");
        Map<String, Object> studentDetail = students.get(0);

        @SuppressWarnings("unchecked")
        Map<String, Integer> poCredits = (Map<String, Integer>) studentDetail.get("poCredits");

        // Legacy: 60 >= 50, so should PASS
        assertEquals(1, poCredits.get("PO1"), "Legacy behavior: 60% >= 50% threshold should pass");
    }

    @Test
    @DisplayName("Test 4: Duplicate templates sharing an assignment label must not inflate the denominator")
    void testDuplicateTemplatesForSameAssignmentLabelDoNotInflateMaxMarks() {
        // SCENARIO (from real data, module EC4356 batch 24):
        // The same assignment was uploaded twice, producing two templates sharing the label
        // "Assignment 01" - an earlier one with max 5 and a later re-upload with max 2. Both
        // carry recorded question scores, but the re-upload overwrote the student's aggregated
        // StudentMark (2), so the denominator must come from that later template.
        // Summing both gives 7, so a student scoring 2 lands at 28.6% and FAILS, when the real
        // result is 2/2 = 100% and a PASS.
        String losId = "LO001";
        String batch = "24";
        String markType = "ASSIGNMENT";
        int threshold = 50;
        List<String> losIds = Arrays.asList(losId);

        AssessmentTemplate firstUpload = new AssessmentTemplate();
        firstUpload.setId("TPL-FIRST");
        firstUpload.setBatch(batch);
        firstUpload.setMarkType(markType);
        firstUpload.setAssignmentLabel("Assignment 01");
        firstUpload.setCreatedAt(LocalDateTime.of(2026, 9, 21, 16, 15));

        AssessmentTemplate reUpload = new AssessmentTemplate();
        reUpload.setId("TPL-REUPLOAD");
        reUpload.setBatch(batch);
        reUpload.setMarkType(markType);
        reUpload.setAssignmentLabel("Assignment 01");
        reUpload.setCreatedAt(LocalDateTime.of(2026, 9, 21, 16, 29));

        AssessmentItem firstUploadItem = new AssessmentItem();
        firstUploadItem.setId(10L);
        firstUploadItem.setQuestionLabel("Q1");
        firstUploadItem.setMaxMarks(5.0);
        firstUploadItem.setLos(los1);
        firstUploadItem.setAssessmentTemplate(firstUpload);

        AssessmentItem reUploadItem = new AssessmentItem();
        reUploadItem.setId(11L);
        reUploadItem.setQuestionLabel("Q1");
        reUploadItem.setMaxMarks(2.0);
        reUploadItem.setLos(los1);
        reUploadItem.setAssessmentTemplate(reUpload);

        StudentMark mark = new StudentMark();
        mark.setStudent(student1);
        mark.setLos(los1);
        mark.setScore(2.0);
        mark.setBatch(batch);
        mark.setMarkType(MarkType.ASSIGNMENT);
        mark.setAssignmentLabel("Assignment 01");

        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(losIds, MarkType.ASSIGNMENT, batch))
            .thenReturn(Arrays.asList(student1));
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(losIds, MarkType.ASSIGNMENT, batch))
            .thenReturn(Arrays.asList(mark));
        when(outcomeMappingRepository.findByLearningOutcome_Id(losId))
            .thenReturn(Arrays.asList(mapping1));
        when(losRepository.findById(losId))
            .thenReturn(Optional.of(los1));
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(losId, batch, markType, MarkType.ASSIGNMENT))
            .thenReturn(Arrays.asList(firstUploadItem, reUploadItem));

        Map<String, Object> result = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("students");
        Map<String, Object> studentDetail = students.get(0);

        @SuppressWarnings("unchecked")
        Map<String, String> loScores = (Map<String, String>) studentDetail.get("loScores");
        assertTrue(loScores.get(losId).startsWith("Pass"),
            "2 marks against the re-upload's max of 2 is 100%, so the LO must pass; the earlier "
                + "duplicate template's max must not be added to the denominator. Got: "
                + loScores.get(losId));

        @SuppressWarnings("unchecked")
        Map<String, Integer> poCredits = (Map<String, Integer>) studentDetail.get("poCredits");
        assertEquals(1, poCredits.get("PO1"),
            "Student should earn the mapping weight for PO1 once the denominator ignores the duplicate template");
    }

    @Test
    @DisplayName("Test 5: A student is measured only against the assignments they have marks for")
    void testStudentIsNotChargedForAnAssignmentTheyHaveNoMarksIn() {
        // SCENARIO: the ASSIGNMENT markType holds two assessments for the same LO -
        // "Assignment 01" (max 10) and "Assignment 02" (max 40). A student scored 8/10 on the
        // first and has no marks at all under the second.
        // Measuring 8 against 10 + 40 gives 16% and a FAIL. The student's real result on the
        // work they submitted is 8/10 = 80%, a PASS. A FINAL_EXAM normally carries a single
        // assessment, which is why this only shows up on assignments.
        String losId = "LO001";
        String batch = "24";
        String markType = "ASSIGNMENT";
        int threshold = 50;
        List<String> losIds = Arrays.asList(losId);

        AssessmentTemplate assignment01 = new AssessmentTemplate();
        assignment01.setId("TPL-A01");
        assignment01.setBatch(batch);
        assignment01.setMarkType(markType);
        assignment01.setAssignmentLabel("Assignment 01");
        assignment01.setCreatedAt(LocalDateTime.of(2026, 9, 20, 9, 0));

        AssessmentTemplate assignment02 = new AssessmentTemplate();
        assignment02.setId("TPL-A02");
        assignment02.setBatch(batch);
        assignment02.setMarkType(markType);
        assignment02.setAssignmentLabel("Assignment 02");
        assignment02.setCreatedAt(LocalDateTime.of(2026, 9, 21, 9, 0));

        AssessmentItem itemA01 = new AssessmentItem();
        itemA01.setId(20L);
        itemA01.setQuestionLabel("Q1");
        itemA01.setMaxMarks(10.0);
        itemA01.setLos(los1);
        itemA01.setAssessmentTemplate(assignment01);

        AssessmentItem itemA02 = new AssessmentItem();
        itemA02.setId(21L);
        itemA02.setQuestionLabel("Q1");
        itemA02.setMaxMarks(40.0);
        itemA02.setLos(los1);
        itemA02.setAssessmentTemplate(assignment02);

        StudentMark assignment01Mark = new StudentMark();
        assignment01Mark.setStudent(student1);
        assignment01Mark.setLos(los1);
        assignment01Mark.setScore(8.0);
        assignment01Mark.setBatch(batch);
        assignment01Mark.setMarkType(MarkType.ASSIGNMENT);
        assignment01Mark.setAssignmentLabel("Assignment 01");

        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(losIds, MarkType.ASSIGNMENT, batch))
            .thenReturn(Arrays.asList(student1));
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(losIds, MarkType.ASSIGNMENT, batch))
            .thenReturn(Arrays.asList(assignment01Mark));
        when(outcomeMappingRepository.findByLearningOutcome_Id(losId))
            .thenReturn(Arrays.asList(mapping1));
        when(losRepository.findById(losId))
            .thenReturn(Optional.of(los1));
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(losId, batch, markType, MarkType.ASSIGNMENT))
            .thenReturn(Arrays.asList(itemA01, itemA02));

        Map<String, Object> result = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("students");
        Map<String, Object> studentDetail = students.get(0);

        @SuppressWarnings("unchecked")
        Map<String, String> loScores = (Map<String, String>) studentDetail.get("loScores");
        assertTrue(loScores.get(losId).startsWith("Pass"),
            "8/10 on the only assignment the student sat is 80%, so the LO must pass; "
                + "Assignment 02's max must not be added to their denominator. Got: " + loScores.get(losId));

        @SuppressWarnings("unchecked")
        Map<String, Integer> poCredits = (Map<String, Integer>) studentDetail.get("poCredits");
        assertEquals(1, poCredits.get("PO1"),
            "Student should earn the mapping weight for PO1 from the assignment they actually sat");
    }

    @Test
    @DisplayName("Test 6: Marks across two assignments are combined against both maxima")
    void testMarksAcrossTwoAssignmentsAreCombined() {
        // SCENARIO: the student has marks under both assignments for the same LO.
        // 4/10 on Assignment 01 and 26/40 on Assignment 02 is 30/50 = 60%, a PASS at 50%.
        String losId = "LO001";
        String batch = "24";
        String markType = "ASSIGNMENT";
        int threshold = 50;
        List<String> losIds = Arrays.asList(losId);

        AssessmentTemplate assignment01 = new AssessmentTemplate();
        assignment01.setId("TPL-A01");
        assignment01.setBatch(batch);
        assignment01.setMarkType(markType);
        assignment01.setAssignmentLabel("Assignment 01");

        AssessmentTemplate assignment02 = new AssessmentTemplate();
        assignment02.setId("TPL-A02");
        assignment02.setBatch(batch);
        assignment02.setMarkType(markType);
        assignment02.setAssignmentLabel("Assignment 02");

        AssessmentItem itemA01 = new AssessmentItem();
        itemA01.setId(30L);
        itemA01.setQuestionLabel("Q1");
        itemA01.setMaxMarks(10.0);
        itemA01.setLos(los1);
        itemA01.setAssessmentTemplate(assignment01);

        AssessmentItem itemA02 = new AssessmentItem();
        itemA02.setId(31L);
        itemA02.setQuestionLabel("Q1");
        itemA02.setMaxMarks(40.0);
        itemA02.setLos(los1);
        itemA02.setAssessmentTemplate(assignment02);

        StudentMark markA01 = new StudentMark();
        markA01.setStudent(student1);
        markA01.setLos(los1);
        markA01.setScore(4.0);
        markA01.setBatch(batch);
        markA01.setMarkType(MarkType.ASSIGNMENT);
        markA01.setAssignmentLabel("Assignment 01");

        StudentMark markA02 = new StudentMark();
        markA02.setStudent(student1);
        markA02.setLos(los1);
        markA02.setScore(26.0);
        markA02.setBatch(batch);
        markA02.setMarkType(MarkType.ASSIGNMENT);
        markA02.setAssignmentLabel("Assignment 02");

        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(losIds, MarkType.ASSIGNMENT, batch))
            .thenReturn(Arrays.asList(student1));
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(losIds, MarkType.ASSIGNMENT, batch))
            .thenReturn(Arrays.asList(markA01, markA02));
        when(outcomeMappingRepository.findByLearningOutcome_Id(losId))
            .thenReturn(Arrays.asList(mapping1));
        when(losRepository.findById(losId))
            .thenReturn(Optional.of(los1));
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(losId, batch, markType, MarkType.ASSIGNMENT))
            .thenReturn(Arrays.asList(itemA01, itemA02));

        Map<String, Object> result = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("students");
        @SuppressWarnings("unchecked")
        Map<String, String> loScores = (Map<String, String>) students.get(0).get("loScores");

        assertTrue(loScores.get(losId).startsWith("Pass"),
            "30 marks across both assignments against a combined max of 50 is 60%, a pass. Got: " + loScores.get(losId));
        assertTrue(loScores.get(losId).contains("60.0"),
            "The reported percentage should be 60.0. Got: " + loScores.get(losId));
    }

    @Test
    @DisplayName("Test 7: A calculation whose LOs resolve to no module is not persisted")
    void testCalculationWithoutModuleIsNotPersisted() {
        // SCENARIO: los1 in this suite's fixture has no module set (unit-test shortcut - real
        // LOs always have one, since los.module_id is NOT NULL). Persistence must be skipped
        // rather than throwing, and the response must say so via "persisted": false.
        String losId = "LO001";
        String batch = "20";
        String markType = "FINAL_EXAM";
        int threshold = 50;
        List<String> losIds = Arrays.asList(losId);

        AssessmentItem singleItem = new AssessmentItem();
        singleItem.setId(1L);
        singleItem.setQuestionLabel("Q1");
        singleItem.setMaxMarks(10.0);
        singleItem.setLos(los1);
        singleItem.setAssessmentTemplate(template1);

        StudentMark mark = new StudentMark();
        mark.setStudent(student1);
        mark.setLos(los1);
        mark.setScore(6.0);
        mark.setBatch(batch);
        mark.setMarkType(MarkType.FINAL_EXAM);

        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(losIds, MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(student1));
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(losIds, MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(mark));
        when(outcomeMappingRepository.findByLearningOutcome_Id(losId))
            .thenReturn(Arrays.asList(mapping1));
        when(losRepository.findById(losId))
            .thenReturn(Optional.of(los1));
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(losId, batch, markType, MarkType.FINAL_EXAM))
            .thenReturn(Arrays.asList(singleItem));

        Map<String, Object> result = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold);

        assertEquals(false, result.get("persisted"), "No module on the LO means nothing to attribute the saved row to");
        verifyNoInteractions(studentPoCreditRepository);
    }

    @Test
    @DisplayName("Test 8: A calculation whose LOs belong to a module is saved, overwriting any previous save")
    void testCalculationWithModuleIsPersistedAndOverwritesPreviousSave() {
        // SCENARIO: same 6/10 pass as Test 1, but los1 now belongs to module EC4356. The saved
        // row must carry the right student/PO/module/batch/markType/credits, and saving must
        // first clear out whatever was saved for this module/batch/markType before.
        String losId = "LO001";
        String batch = "20";
        String markType = "FINAL_EXAM";
        int threshold = 50;
        List<String> losIds = Arrays.asList(losId);

        Module module = new Module();
        module.setModuleId("EC4356");
        los1.setModule(module);

        AssessmentItem singleItem = new AssessmentItem();
        singleItem.setId(1L);
        singleItem.setQuestionLabel("Q1");
        singleItem.setMaxMarks(10.0);
        singleItem.setLos(los1);
        singleItem.setAssessmentTemplate(template1);

        StudentMark mark = new StudentMark();
        mark.setStudent(student1);
        mark.setLos(los1);
        mark.setScore(6.0);
        mark.setBatch(batch);
        mark.setMarkType(MarkType.FINAL_EXAM);

        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(losIds, MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(student1));
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(losIds, MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(mark));
        when(outcomeMappingRepository.findByLearningOutcome_Id(losId))
            .thenReturn(Arrays.asList(mapping1));
        when(losRepository.findById(losId))
            .thenReturn(Optional.of(los1));
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(losId, batch, markType, MarkType.FINAL_EXAM))
            .thenReturn(Arrays.asList(singleItem));

        Map<String, Object> result = poAttainmentService.calculateStudentPOCredits(losIds, markType, batch, threshold);

        assertEquals(true, result.get("persisted"));
        verify(studentPoCreditRepository).deleteByModule_ModuleIdAndBatchAndMarkType("EC4356", batch, MarkType.FINAL_EXAM);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentPoCredit>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(studentPoCreditRepository).saveAll(captor.capture());
        List<StudentPoCredit> saved = captor.getValue();
        assertEquals(1, saved.size(), "One student x one mapped PO = one saved row");

        StudentPoCredit row = saved.get(0);
        assertEquals("EN001", row.getStudent().getStudentId());
        assertEquals("PO1", row.getProgramOutcome().getCode());
        assertEquals("EC4356", row.getModule().getModuleId());
        assertEquals(batch, row.getBatch());
        assertEquals(MarkType.FINAL_EXAM, row.getMarkType());
        assertEquals(1, row.getCreditsEarned(), "Passed the LO, so earns the mapping's full weight (1)");
        assertEquals(1, row.getMaxCredits());
        assertEquals(threshold, row.getThreshold());
    }

    @Test
    @DisplayName("Test 9: Cross-module summary sums a student's credits across every module")
    void testStudentPOSummarySumsAcrossModules() {
        // SCENARIO: EC4356 credited PO1 with 2/4 and PO2 with 0/3; EC4357 separately credited
        // PO1 with 3/4. The cumulative summary must add PO1 across both modules (5/8) and keep
        // PO2 as-is (0/3, only ever calculated in EC4356) - not merge or drop it.
        Module ec4356 = new Module();
        ec4356.setModuleId("EC4356");
        Module ec4357 = new Module();
        ec4357.setModuleId("EC4357");

        ProgramOutcome po2 = new ProgramOutcome();
        po2.setPoId("PO2");
        po2.setCode("PO2");
        po2.setTitle("Program Outcome 2");

        StudentPoCredit ec4356Po1 = new StudentPoCredit();
        ec4356Po1.setStudent(student1);
        ec4356Po1.setProgramOutcome(po1);
        ec4356Po1.setModule(ec4356);
        ec4356Po1.setBatch("24");
        ec4356Po1.setMarkType(MarkType.ASSIGNMENT);
        ec4356Po1.setCreditsEarned(2);
        ec4356Po1.setMaxCredits(4);
        ec4356Po1.setThreshold(50);
        ec4356Po1.setUpdatedAt(LocalDateTime.now());

        StudentPoCredit ec4356Po2 = new StudentPoCredit();
        ec4356Po2.setStudent(student1);
        ec4356Po2.setProgramOutcome(po2);
        ec4356Po2.setModule(ec4356);
        ec4356Po2.setBatch("24");
        ec4356Po2.setMarkType(MarkType.ASSIGNMENT);
        ec4356Po2.setCreditsEarned(0);
        ec4356Po2.setMaxCredits(3);
        ec4356Po2.setThreshold(50);
        ec4356Po2.setUpdatedAt(LocalDateTime.now());

        StudentPoCredit ec4357Po1 = new StudentPoCredit();
        ec4357Po1.setStudent(student1);
        ec4357Po1.setProgramOutcome(po1);
        ec4357Po1.setModule(ec4357);
        ec4357Po1.setBatch("24");
        ec4357Po1.setMarkType(MarkType.ASSIGNMENT);
        ec4357Po1.setCreditsEarned(3);
        ec4357Po1.setMaxCredits(4);
        ec4357Po1.setThreshold(50);
        ec4357Po1.setUpdatedAt(LocalDateTime.now());

        when(studentPoCreditRepository.findByStudent_StudentId("EN001"))
            .thenReturn(Arrays.asList(ec4356Po1, ec4356Po2, ec4357Po1));

        Map<String, Object> result = poAttainmentService.getStudentPOSummary("EN001", null);

        assertEquals("EN001", result.get("studentId"));
        assertEquals("ALL", result.get("markType"));
        assertEquals(2L, result.get("moduleCount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> summaries = (List<Map<String, Object>>) result.get("poSummaries");
        assertEquals(2, summaries.size());

        Map<String, Object> po1Summary = summaries.stream().filter(m -> "PO1".equals(m.get("poCode"))).findFirst().orElseThrow();
        assertEquals(5, po1Summary.get("creditsEarned"), "2 (EC4356) + 3 (EC4357) = 5");
        assertEquals(8, po1Summary.get("maxCredits"), "4 (EC4356) + 4 (EC4357) = 8");
        assertEquals(62.5, (double) po1Summary.get("percentage"), 0.001);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> po1Breakdown = (List<Map<String, Object>>) po1Summary.get("moduleBreakdown");
        assertEquals(2, po1Breakdown.size(), "PO1 was credited from two separate modules");

        Map<String, Object> po2Summary = summaries.stream().filter(m -> "PO2".equals(m.get("poCode"))).findFirst().orElseThrow();
        assertEquals(0, po2Summary.get("creditsEarned"));
        assertEquals(3, po2Summary.get("maxCredits"), "PO2 was only ever calculated in EC4356, so its max is not doubled");
    }

    @Test
    @DisplayName("Test 10: Cross-module summary can be scoped to a single mark type")
    void testStudentPOSummaryScopedToMarkType() {
        when(studentPoCreditRepository.findByStudent_StudentIdAndMarkType("EN001", MarkType.ASSIGNMENT))
            .thenReturn(Collections.emptyList());

        Map<String, Object> result = poAttainmentService.getStudentPOSummary("EN001", "ASSIGNMENT");

        assertEquals("ASSIGNMENT", result.get("markType"));
        verify(studentPoCreditRepository).findByStudent_StudentIdAndMarkType("EN001", MarkType.ASSIGNMENT);
        verify(studentPoCreditRepository, never()).findByStudent_StudentId(anyString());
    }

    @Test
    @DisplayName("Test 11: recalculateForModule resolves the module's LOs and persists the recalculation")
    void testRecalculateForModuleRecalculatesAndPersists() {
        // SCENARIO: marks were just uploaded/edited/deleted for module EC4356 - the controller
        // calls recalculateForModule instead of waiting for a lecturer to click "Calculate PO
        // Attainment". It must look up every LO belonging to the module (not just whichever LO
        // the triggering change touched - see the method's own doc comment on why) and run the
        // same calculate-and-save path the manual button uses.
        String moduleId = "EC4356";
        String batch = "20";
        String markType = "FINAL_EXAM";

        Module module = new Module();
        module.setModuleId(moduleId);
        los1.setModule(module);

        AssessmentItem singleItem = new AssessmentItem();
        singleItem.setId(1L);
        singleItem.setQuestionLabel("Q1");
        singleItem.setMaxMarks(10.0);
        singleItem.setLos(los1);
        singleItem.setAssessmentTemplate(template1);

        StudentMark mark = new StudentMark();
        mark.setStudent(student1);
        mark.setLos(los1);
        mark.setScore(6.0);
        mark.setBatch(batch);
        mark.setMarkType(MarkType.FINAL_EXAM);

        when(losRepository.findByModule_ModuleId(moduleId)).thenReturn(Arrays.asList(los1));
        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(Arrays.asList("LO001"), MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(student1));
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(Arrays.asList("LO001"), MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(mark));
        when(outcomeMappingRepository.findByLearningOutcome_Id("LO001"))
            .thenReturn(Arrays.asList(mapping1));
        when(losRepository.findById("LO001"))
            .thenReturn(Optional.of(los1));
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType("LO001", batch, markType, MarkType.FINAL_EXAM))
            .thenReturn(Arrays.asList(singleItem));

        poAttainmentService.recalculateForModule(moduleId, batch, markType);

        verify(studentPoCreditRepository).deleteByModule_ModuleIdAndBatchAndMarkType(moduleId, batch, MarkType.FINAL_EXAM);
        verify(studentPoCreditRepository).saveAll(any());
    }

    @Test
    @DisplayName("Test 12: recalculateForModule with no LOs in the module does nothing")
    void testRecalculateForModuleNoLosIsNoOp() {
        when(losRepository.findByModule_ModuleId("EMPTY")).thenReturn(Collections.emptyList());

        poAttainmentService.recalculateForModule("EMPTY", "20", "FINAL_EXAM");

        verifyNoInteractions(studentPoCreditRepository);
    }

    @Test
    @DisplayName("Test 13: recalculateForModule with a blank moduleId, batch or markType is a no-op")
    void testRecalculateForModuleBlankParamsIsNoOp() {
        poAttainmentService.recalculateForModule(null, "20", "FINAL_EXAM");
        poAttainmentService.recalculateForModule("EC4356", "", "FINAL_EXAM");
        poAttainmentService.recalculateForModule("EC4356", "20", "");

        verifyNoInteractions(losRepository, studentPoCreditRepository);
    }

    @Test
    @DisplayName("Test 14: recalculateForModule swallows errors instead of failing the upload/delete that triggered it")
    void testRecalculateForModuleSwallowsErrors() {
        when(losRepository.findByModule_ModuleId("EC4356")).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> poAttainmentService.recalculateForModule("EC4356", "20", "FINAL_EXAM"));
    }

    @Test
    @DisplayName("Test 15: a duplicate LO id in the request neither double-counts credits nor duplicates the saved row")
    void testDuplicateLosIdsDoNotDoubleCountOrDuplicateRows() {
        // Reproduces the production crash: "Calculate PO Attainment" with a losIds list
        // containing the same LO twice (e.g. the frontend's "All" selector) made the per-student
        // loop process that LO's mapping weight twice, and made saveStudentPoCredits build two
        // rows with the identical (student, po, module, batch, markType) key - which MySQL's
        // uk_student_po_credit constraint rejects as a duplicate entry within the same saveAll
        // batch, independent of whether the prior delete worked. Both the weight double-count
        // and the row duplication must be fixed, not just the crash suppressed.
        String losId = "LO001";
        String batch = "20";
        String markType = "FINAL_EXAM";
        int threshold = 50;
        List<String> losIdsWithDuplicate = Arrays.asList(losId, losId);

        Module module = new Module();
        module.setModuleId("EC4356");
        los1.setModule(module);

        AssessmentItem singleItem = new AssessmentItem();
        singleItem.setId(1L);
        singleItem.setQuestionLabel("Q1");
        singleItem.setMaxMarks(10.0);
        singleItem.setLos(los1);
        singleItem.setAssessmentTemplate(template1);

        StudentMark mark = new StudentMark();
        mark.setStudent(student1);
        mark.setLos(los1);
        mark.setScore(6.0);
        mark.setBatch(batch);
        mark.setMarkType(MarkType.FINAL_EXAM);

        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(Arrays.asList(losId), MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(student1));
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(Arrays.asList(losId), MarkType.FINAL_EXAM, batch))
            .thenReturn(Arrays.asList(mark));
        when(outcomeMappingRepository.findByLearningOutcome_Id(losId))
            .thenReturn(Arrays.asList(mapping1));
        when(losRepository.findById(losId))
            .thenReturn(Optional.of(los1));
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(losId, batch, markType, MarkType.FINAL_EXAM))
            .thenReturn(Arrays.asList(singleItem));

        Map<String, Object> result = poAttainmentService.calculateStudentPOCredits(losIdsWithDuplicate, markType, batch, threshold);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("students");
        @SuppressWarnings("unchecked")
        Map<String, Integer> poCredits = (Map<String, Integer>) students.get(0).get("poCredits");
        assertEquals(1, poCredits.get("PO1"), "A duplicate LO id must not double the mapping weight (1, not 2)");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentPoCredit>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(studentPoCreditRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size(), "Only one saved row for this student+PO, not two colliding on the unique key");
    }
}
