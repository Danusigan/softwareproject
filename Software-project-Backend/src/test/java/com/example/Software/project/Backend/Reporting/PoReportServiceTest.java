package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module; // disambiguate from java.lang.Module
import com.example.Software.project.Backend.Repository.*;
import com.example.Software.project.Backend.Service.POAttainmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("PoReportService")
class PoReportServiceTest {

    @Mock private POAttainmentService poAttainmentService;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentPoCreditRepository studentPoCreditRepository;
    @Mock private OutcomeMappingRepository outcomeMappingRepository;
    @Mock private ProgramOutcomeRepository programOutcomeRepository;

    @InjectMocks
    private PoReportService service;

    private Student student1;
    private Student student2;
    private ProgramOutcome po1;
    private ProgramOutcome po2;
    private Module module;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        student1 = new Student();
        student1.setStudentId("EN001");
        student1.setStudentName("Student One");
        student1.setBatch("24");

        student2 = new Student();
        student2.setStudentId("EN002");
        student2.setStudentName("Student Two");
        student2.setBatch("24");

        po1 = new ProgramOutcome();
        po1.setPoId("PO001"); po1.setCode("PO1"); po1.setTitle("Engineering Knowledge");

        po2 = new ProgramOutcome();
        po2.setPoId("PO002"); po2.setCode("PO2"); po2.setTitle("Problem Analysis");

        module = new Module();
        module.setModuleId("EC4356");
    }

    @Test
    @DisplayName("studentReport marks a PO Attained/Not attained/No evidence against the threshold")
    void studentReportAppliesThreshold() {
        Map<String, Object> po1Row = new LinkedHashMap<>();
        po1Row.put("poCode", "PO1");
        po1Row.put("creditsEarned", 3);
        po1Row.put("maxCredits", 5);
        po1Row.put("percentage", 60.0);
        po1Row.put("moduleBreakdown", List.of(breakdown("EC4356", "24", 3, 5)));

        Map<String, Object> po2Row = new LinkedHashMap<>();
        po2Row.put("poCode", "PO2");
        po2Row.put("creditsEarned", 1);
        po2Row.put("maxCredits", 5);
        po2Row.put("percentage", 20.0);
        po2Row.put("moduleBreakdown", List.of(breakdown("EC4356", "24", 1, 5)));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("moduleCount", 1L);
        summary.put("poSummaries", List.of(po1Row, po2Row));

        when(studentRepository.findById("EN001")).thenReturn(Optional.of(student1));
        when(poAttainmentService.getStudentPOSummary("EN001", null)).thenReturn(summary);
        when(programOutcomeRepository.findByCode("PO1")).thenReturn(Optional.of(po1));
        when(programOutcomeRepository.findByCode("PO2")).thenReturn(Optional.of(po2));

        PoStudentReport report = service.studentReport("EN001", 40.0);

        assertEquals("EN001", report.studentId());
        assertEquals("Student One", report.studentName());
        assertEquals(1, report.moduleCount());
        assertEquals(2, report.pos().size());

        PoStudentReport.PoRow row1 = report.pos().stream().filter(r -> r.code().equals("PO1")).findFirst().orElseThrow();
        assertEquals("Attained", row1.status(), "60% >= 40% threshold");
        assertEquals("Engineering Knowledge", row1.title());

        PoStudentReport.PoRow row2 = report.pos().stream().filter(r -> r.code().equals("PO2")).findFirst().orElseThrow();
        assertEquals("Not attained", row2.status(), "20% < 40% threshold");
    }

    @Test
    @DisplayName("studentReport marks a PO 'No evidence' when no module has a saved credit for it")
    void studentReportNoEvidence() {
        Map<String, Object> po1Row = new LinkedHashMap<>();
        po1Row.put("poCode", "PO1");
        po1Row.put("creditsEarned", 0);
        po1Row.put("maxCredits", 0);
        po1Row.put("percentage", null);
        po1Row.put("moduleBreakdown", List.of());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("moduleCount", 0L);
        summary.put("poSummaries", List.of(po1Row));

        when(studentRepository.findById("EN001")).thenReturn(Optional.of(student1));
        when(poAttainmentService.getStudentPOSummary("EN001", null)).thenReturn(summary);
        when(programOutcomeRepository.findByCode("PO1")).thenReturn(Optional.of(po1));

        PoStudentReport report = service.studentReport("EN001", 40.0);

        assertEquals("No evidence", report.pos().get(0).status());
    }

    @Test
    @DisplayName("studentReport merges Final Exam and Assignment contributions from the same module")
    void studentReportMergesModuleContributions() {
        Map<String, Object> po1Row = new LinkedHashMap<>();
        po1Row.put("poCode", "PO1");
        po1Row.put("creditsEarned", 5);
        po1Row.put("maxCredits", 8);
        po1Row.put("percentage", 62.5);
        // Same module+batch, two rows (Final Exam + Assignment) - must merge into one contribution.
        po1Row.put("moduleBreakdown", List.of(breakdown("EC4356", "24", 2, 4), breakdown("EC4356", "24", 3, 4)));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("moduleCount", 1L);
        summary.put("poSummaries", List.of(po1Row));

        when(studentRepository.findById("EN001")).thenReturn(Optional.of(student1));
        when(poAttainmentService.getStudentPOSummary("EN001", null)).thenReturn(summary);
        when(programOutcomeRepository.findByCode("PO1")).thenReturn(Optional.of(po1));

        PoStudentReport report = service.studentReport("EN001", 40.0);

        List<PoStudentReport.ModuleContribution> breakdown = report.pos().get(0).moduleBreakdown();
        assertEquals(1, breakdown.size(), "Same module+batch rows must merge into one contribution");
        assertEquals(5, breakdown.get(0).creditsEarned());
        assertEquals(8, breakdown.get(0).maxCredits());
    }

    @Test
    @DisplayName("studentReport 404s when the student doesn't exist")
    void studentReportMissingStudent() {
        when(studentRepository.findById("GHOST")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.studentReport("GHOST", 40.0));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("batchReport computes attainment share against studentThreshold and success against batchTarget")
    void batchReportComputesAttainmentAndSuccess() {
        OutcomeMapping mapping = new OutcomeMapping();
        mapping.setId(1L);
        mapping.setProgramOutcome(po1);
        mapping.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        Los los = new Los(); los.setId("LO001");
        mapping.setLearningOutcome(los);

        when(studentRepository.findByBatch("24")).thenReturn(Arrays.asList(student1, student2));
        when(outcomeMappingRepository.findByStatus(OutcomeMapping.ApprovalStatus.APPROVED)).thenReturn(List.of(mapping));

        StudentPoCredit c1 = credit(student1, po1, module, "24", 4, 5); // 80% -> attains at 40
        StudentPoCredit c2 = credit(student2, po1, module, "24", 1, 5); // 20% -> does not attain
        when(studentPoCreditRepository.findByBatch("24")).thenReturn(Arrays.asList(c1, c2));

        PoBatchReport report = service.batchReport("24", 40.0, 60.0);

        assertEquals(2, report.totalStudents());
        assertEquals(1, report.pos().size());
        PoBatchReport.PoRow row = report.pos().get(0);
        assertEquals(1, row.studentsAttained(), "Only student1 (80%) clears the 40% student threshold");
        assertEquals(2, row.totalStudents());
        assertEquals(50.0, row.attainmentPercent(), 0.001, "1 of 2 students = 50%");
        assertEquals("Not successful", row.status(), "50% attainment is below the 60% batch target");
    }

    @Test
    @DisplayName("batchReport counts a student with no saved credit as not attaining the PO")
    void batchReportMissingCreditCountsAsNotAttained() {
        OutcomeMapping mapping = new OutcomeMapping();
        mapping.setId(1L);
        mapping.setProgramOutcome(po1);
        mapping.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        Los los = new Los(); los.setId("LO001");
        mapping.setLearningOutcome(los);

        when(studentRepository.findByBatch("24")).thenReturn(Arrays.asList(student1, student2));
        when(outcomeMappingRepository.findByStatus(OutcomeMapping.ApprovalStatus.APPROVED)).thenReturn(List.of(mapping));

        // Only student1 has a saved credit; student2 has none at all for this PO.
        StudentPoCredit c1 = credit(student1, po1, module, "24", 5, 5);
        when(studentPoCreditRepository.findByBatch("24")).thenReturn(List.of(c1));

        PoBatchReport report = service.batchReport("24", 40.0, 60.0);

        PoBatchReport.PoRow row = report.pos().get(0);
        assertEquals(2, row.totalStudents(), "Denominator is every student in the batch, not just ones with a saved credit");
        assertEquals(1, row.studentsAttained());
        assertEquals(50.0, row.attainmentPercent(), 0.001);
    }

    @Test
    @DisplayName("batchReport 404s when the batch has no students")
    void batchReportMissingBatch() {
        when(studentRepository.findByBatch("99")).thenReturn(Collections.emptyList());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.batchReport("99", 40.0, 60.0));
        assertEquals(404, ex.getStatusCode().value());
    }

    private Map<String, Object> breakdown(String moduleId, String batch, int earned, int max) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("moduleId", moduleId); m.put("batch", batch);
        m.put("creditsEarned", earned); m.put("maxCredits", max);
        m.put("markType", "ASSIGNMENT"); m.put("updatedAt", null);
        return m;
    }

    private StudentPoCredit credit(Student student, ProgramOutcome po, Module module, String batch, int earned, int max) {
        StudentPoCredit c = new StudentPoCredit();
        c.setStudent(student); c.setProgramOutcome(po); c.setModule(module); c.setBatch(batch);
        c.setMarkType(MarkType.ASSIGNMENT); c.setCreditsEarned(earned); c.setMaxCredits(max); c.setThreshold(50);
        return c;
    }
}
