package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.AssessmentItem;
import com.example.Software.project.Backend.Model.Los;
import com.example.Software.project.Backend.Model.MarkType;
import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Model.OutcomeMapping;
import com.example.Software.project.Backend.Model.ProgramOutcome;
import com.example.Software.project.Backend.Model.Student;
import com.example.Software.project.Backend.Model.StudentMark;
import com.example.Software.project.Backend.Repository.AssessmentItemRepository;
import com.example.Software.project.Backend.Repository.OutcomeMappingRepository;
import com.example.Software.project.Backend.Repository.StudentMarkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TrendServiceTest {

    @Mock private StudentMarkRepository markRepository;
    @Mock private AssessmentItemRepository assessmentItemRepository;
    @Mock private OutcomeMappingRepository outcomeMappingRepository;

    private TrendService trendService;
    private Los lo1;
    private Los lo2;
    private List<StudentMark> marks;

    @BeforeEach
    void setUp() {
        trendService = new TrendService(markRepository, assessmentItemRepository, outcomeMappingRepository);

        Module module = new Module();
        module.setModuleId("SE101");
        module.setModuleName("Software Engineering");
        lo1 = learningOutcome("LO1", "Requirements analysis", module);
        lo2 = learningOutcome("LO2", "Software design", module);

        Student student1 = student("S001");
        Student student2 = student("S002");
        marks = List.of(
            mark(student1, lo1, "24", MarkType.FINAL_EXAM, 40.0),
            mark(student2, lo1, "24", MarkType.FINAL_EXAM, 30.0),
            mark(student1, lo2, "24", MarkType.FINAL_EXAM, 70.0),
            mark(student2, lo2, "24", MarkType.FINAL_EXAM, 50.0)
        );

        AssessmentItem lo1Item = new AssessmentItem();
        lo1Item.setLos(lo1);
        lo1Item.setMaxMarks(50.0);
        lenient().when(assessmentItemRepository
            .findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(
                "LO1", "24", "FINAL_EXAM"))
            .thenReturn(List.of(lo1Item));

        ProgramOutcome po = new ProgramOutcome();
        po.setPoId("PO1");
        po.setCode("PO1");
        po.setTitle("Engineering knowledge");
        OutcomeMapping map1 = mapping(lo1, po, 3);
        OutcomeMapping map2 = mapping(lo2, po, 1);
        lenient().when(outcomeMappingRepository.findByLearningOutcome_Module_ModuleIdAndStatus(
            "SE101", OutcomeMapping.ApprovalStatus.APPROVED)).thenReturn(List.of(map1, map2));
        lenient().when(markRepository.findByLos_Module_ModuleId("SE101")).thenReturn(marks);
    }

    @Test
    void dashboardUsesNormalizedDistinctStudentResultsAndApprovedPoWeights() {
        Map<String, Object> dashboard = trendService.getDashboardGraphs(
            "SE101", "24", MarkType.FINAL_EXAM, 50.0, 60.0, null);

        Map<String, Object> summary = map(dashboard.get("summary"));
        assertThat(summary.get("average")).isEqualTo(65.0);
        assertThat(summary.get("passRate")).isEqualTo(100.0);
        assertThat(summary.get("totalStudents")).isEqualTo(2L);
        assertThat(summary.get("studentResults")).isEqualTo(2);
        assertThat(summary.get("loCount")).isEqualTo(2);

        List<Map<String, Object>> loPerformance = list(dashboard.get("loPerformance"));
        assertThat(loPerformance).extracting(item -> item.get("actual"))
            .containsExactly(70.0, 60.0);

        List<Map<String, Object>> poPerformance = list(dashboard.get("poPerformance"));
        assertThat(poPerformance).hasSize(1);
        assertThat(poPerformance.get(0).get("label")).isEqualTo("PO1");
        assertThat(poPerformance.get(0).get("actual")).isEqualTo(67.5);

        Map<String, Object> quality = map(dashboard.get("dataQuality"));
        assertThat(quality.get("normalizedObservationCount")).isEqualTo(2);
        assertThat(quality.get("assumedPercentageObservationCount")).isEqualTo(2);
    }

    @Test
    void dashboardCanFocusOnOneLearningOutcome() {
        Map<String, Object> dashboard = trendService.getDashboardGraphs(
            "SE101", "24", MarkType.FINAL_EXAM, 65.0, 70.0, "LO1");

        Map<String, Object> meta = map(dashboard.get("meta"));
        Map<String, Object> summary = map(dashboard.get("summary"));
        Map<String, Object> passFail = map(dashboard.get("passFail"));

        assertThat(meta.get("scope")).isEqualTo("LEARNING_OUTCOME");
        assertThat(summary.get("average")).isEqualTo(70.0);
        assertThat(summary.get("passRate")).isEqualTo(50.0);
        assertThat(list(dashboard.get("loPerformance"))).hasSize(1);
        assertThat(passFail.get("data")).isEqualTo(List.of(1L, 1L));
        assertThat(list(dashboard.get("focusTrend"))).hasSize(1);
    }

    @Test
    void invalidThresholdIsRejected() {
        assertThatThrownBy(() -> trendService.getDashboardGraphs(
            "SE101", null, null, 101.0, 60.0, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("threshold");
    }

    private static Los learningOutcome(String id, String name, Module module) {
        Los lo = new Los();
        lo.setId(id);
        lo.setName(name);
        lo.setModule(module);
        return lo;
    }

    private static Student student(String id) {
        Student student = new Student();
        student.setStudentId(id);
        student.setStudentName(id);
        return student;
    }

    private static StudentMark mark(Student student, Los lo, String batch,
                                    MarkType markType, double score) {
        StudentMark mark = new StudentMark();
        mark.setStudent(student);
        mark.setLos(lo);
        mark.setBatch(batch);
        mark.setMarkType(markType);
        mark.setScore(score);
        return mark;
    }

    private static OutcomeMapping mapping(Los lo, ProgramOutcome po, int weight) {
        OutcomeMapping mapping = new OutcomeMapping(lo, po, weight, "lecturer");
        mapping.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        return mapping;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
