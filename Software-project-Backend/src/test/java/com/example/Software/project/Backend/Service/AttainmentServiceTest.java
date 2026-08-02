package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AttainmentService: LO-level classification thresholds and the
 * weighted PO-attainment rollup consumed by the dashboards/charts.
 *
 * No AssessmentItem/StudentAssessmentScore data is stubbed here, so
 * calculateLOLevel() falls back to the legacy per-LO StudentMark path
 * (same fallback the existing POAttainmentServiceTest exercises).
 */
@DisplayName("AttainmentService Tests")
class AttainmentServiceTest {

    @Mock
    private StudentMarkRepository markRepository;
    @Mock
    private OutcomeMappingRepository mappingRepository;
    @Mock
    private LosRepository losRepository;
    @Mock
    private AssessmentItemRepository assessmentItemRepository;
    @Mock
    private StudentAssessmentScoreRepository studentAssessmentScoreRepository;

    @InjectMocks
    private AttainmentService attainmentService;

    private Los los1;
    private Los los2;
    private ProgramOutcome po1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        los1 = new Los();
        los1.setId("LO001");
        los1.setName("LO 1");

        los2 = new Los();
        los2.setId("LO002");
        los2.setName("LO 2");

        po1 = new ProgramOutcome();
        po1.setId("PO001");
        po1.setPoId("PO1");
        po1.setCode("PO1");
    }

    private List<StudentMark> marksWithPassRate(Los los, int total, int passed) {
        List<StudentMark> marks = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            Student student = new Student();
            student.setStudentId("EN" + i);
            StudentMark mark = new StudentMark();
            mark.setStudent(student);
            mark.setLos(los);
            mark.setScore(i < passed ? 100.0 : 0.0); // clearly pass (>=50) or fail
            marks.add(mark);
        }
        return marks;
    }

    // ---- calculateLOLevel threshold boundaries ----

    @Test
    @DisplayName("calculateLOLevel returns 0 when there are no marks for the LO")
    void calculateLOLevel_noMarks_returnsZero() {
        when(markRepository.findByLos_Id("LO001")).thenReturn(Collections.emptyList());

        assertEquals(0, attainmentService.calculateLOLevel("LO001"));
    }

    @Test
    @DisplayName("calculateLOLevel returns 0 when the pass rate is below 60%")
    void calculateLOLevel_belowSixty_returnsZero() {
        when(markRepository.findByLos_Id("LO001")).thenReturn(marksWithPassRate(los1, 10, 5)); // 50%

        assertEquals(0, attainmentService.calculateLOLevel("LO001"));
    }

    @Test
    @DisplayName("calculateLOLevel returns 1 exactly at the 60% boundary")
    void calculateLOLevel_atSixty_returnsOne() {
        when(markRepository.findByLos_Id("LO001")).thenReturn(marksWithPassRate(los1, 10, 6)); // 60%

        assertEquals(1, attainmentService.calculateLOLevel("LO001"));
    }

    @Test
    @DisplayName("calculateLOLevel returns 2 exactly at the 70% boundary")
    void calculateLOLevel_atSeventy_returnsTwo() {
        when(markRepository.findByLos_Id("LO001")).thenReturn(marksWithPassRate(los1, 10, 7)); // 70%

        assertEquals(2, attainmentService.calculateLOLevel("LO001"));
    }

    @Test
    @DisplayName("calculateLOLevel returns 3 exactly at the 80% boundary")
    void calculateLOLevel_atEighty_returnsThree() {
        when(markRepository.findByLos_Id("LO001")).thenReturn(marksWithPassRate(los1, 10, 8)); // 80%

        assertEquals(3, attainmentService.calculateLOLevel("LO001"));
    }

    // ---- getPOAttainment ----

    @Test
    @DisplayName("getPOAttainment returns no score for a PO with zero approved mappings")
    void getPOAttainment_noApprovedMappings_returnsEmpty() {
        OutcomeMapping rejected = new OutcomeMapping(los1, po1, 3, "lecturer1");
        rejected.setStatus(OutcomeMapping.ApprovalStatus.REJECTED);

        when(mappingRepository.findByLearningOutcome_Module_ModuleId("MOD1"))
            .thenReturn(List.of(rejected));

        Map<String, Double> result = attainmentService.getPOAttainment("MOD1");

        assertTrue(result.isEmpty(), "rejected mappings must not contribute a PO score");
    }

    @Test
    @DisplayName("getPOAttainment computes a weight-weighted average of LO levels for approved mappings only")
    void getPOAttainment_weightsApprovedLoLevels() {
        OutcomeMapping approvedHighWeight = new OutcomeMapping(los1, po1, 3, "lecturer1");
        approvedHighWeight.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);

        OutcomeMapping approvedLowWeight = new OutcomeMapping(los2, po1, 1, "lecturer1");
        approvedLowWeight.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);

        // A pending mapping to the same PO must be excluded from the calculation entirely.
        Los los3 = new Los();
        los3.setId("LO003");
        OutcomeMapping pending = new OutcomeMapping(los3, po1, 3, "lecturer1");
        pending.setStatus(OutcomeMapping.ApprovalStatus.PENDING);

        when(mappingRepository.findByLearningOutcome_Module_ModuleId("MOD1"))
            .thenReturn(List.of(approvedHighWeight, approvedLowWeight, pending));

        // LO1: 100% pass rate -> level 3. LO2: 60% pass rate -> level 1.
        when(markRepository.findByLos_Id("LO001")).thenReturn(marksWithPassRate(los1, 5, 5));
        when(markRepository.findByLos_Id("LO002")).thenReturn(marksWithPassRate(los2, 5, 3));

        Map<String, Double> result = attainmentService.getPOAttainment("MOD1");

        // weightedSum = (3*3) + (1*1) = 10, totalWeight = 3 + 1 = 4 -> 2.5
        assertEquals(2.5, result.get("PO1"), 0.0001);
        // The pending mapping's LO (LO3) must never have been looked up.
    }
}
