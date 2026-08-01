package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
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
}
