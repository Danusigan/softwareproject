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
    private ProgramOutcome customPo;
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

        // Washington Accord standard POs (ProgramOutcome.isDefault=true).
        po1 = new ProgramOutcome();
        po1.setPoId("PO001"); po1.setCode("PO1"); po1.setTitle("Engineering Knowledge");
        po1.setIsDefault(true); po1.setDisplayOrder(1);

        po2 = new ProgramOutcome();
        po2.setPoId("PO002"); po2.setCode("PO2"); po2.setTitle("Problem Analysis");
        po2.setIsDefault(true); po2.setDisplayOrder(2);

        // A non-standard PO an admin added themselves.
        customPo = new ProgramOutcome();
        customPo.setPoId("PO099"); customPo.setCode("PO99"); customPo.setTitle("Custom Outcome");
        customPo.setIsDefault(false);

        module = new Module();
        module.setModuleId("EC4356");

        // Default for every test unless overridden: no Washington Accord POs configured.
        when(programOutcomeRepository.findByIsDefaultTrueOrderByDisplayOrderAsc()).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("studentReport marks a PO Attained/Not attained against the threshold, using saved credit data")
    void studentReportAppliesThreshold() {
        when(programOutcomeRepository.findByIsDefaultTrueOrderByDisplayOrderAsc()).thenReturn(List.of(po1, po2));

        Map<String, Object> po1Row = new LinkedHashMap<>();
        po1Row.put("poCode", "PO1");
        po1Row.put("creditsEarned", 3);
        po1Row.put("maxCredits", 5);
        po1Row.put("percentage", 60.0);

        Map<String, Object> po2Row = new LinkedHashMap<>();
        po2Row.put("poCode", "PO2");
        po2Row.put("creditsEarned", 1);
        po2Row.put("maxCredits", 5);
        po2Row.put("percentage", 20.0);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("moduleCount", 1L);
        summary.put("poSummaries", List.of(po1Row, po2Row));

        when(studentRepository.findById("EN001")).thenReturn(Optional.of(student1));
        when(poAttainmentService.getStudentPOSummary("EN001")).thenReturn(summary);

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
        when(programOutcomeRepository.findByIsDefaultTrueOrderByDisplayOrderAsc()).thenReturn(List.of(po1));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("moduleCount", 0L);
        summary.put("poSummaries", List.of());

        when(studentRepository.findById("EN001")).thenReturn(Optional.of(student1));
        when(poAttainmentService.getStudentPOSummary("EN001")).thenReturn(summary);

        PoStudentReport report = service.studentReport("EN001", 40.0);

        assertEquals(1, report.pos().size());
        assertEquals("No evidence", report.pos().get(0).status());
        assertEquals(0, report.pos().get(0).creditsEarned());
    }

    @Test
    @DisplayName("studentReport always lists every Washington Accord standard PO, in display order, even with zero evidence")
    void studentReportAlwaysListsWashingtonAccordDefaults() {
        // po2 has displayOrder 2, po1 has displayOrder 1 - repository is mocked to already
        // return them in that order, same as findByIsDefaultTrueOrderByDisplayOrderAsc would.
        when(programOutcomeRepository.findByIsDefaultTrueOrderByDisplayOrderAsc()).thenReturn(List.of(po1, po2));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("moduleCount", 0L);
        summary.put("poSummaries", List.of()); // no saved credits for this student at all

        when(studentRepository.findById("EN001")).thenReturn(Optional.of(student1));
        when(poAttainmentService.getStudentPOSummary("EN001")).thenReturn(summary);

        PoStudentReport report = service.studentReport("EN001", 40.0);

        assertEquals(2, report.pos().size(), "Both standard POs must appear even though neither has data");
        assertEquals("PO1", report.pos().get(0).code(), "Must follow displayOrder, not alphabetical/insertion order of credit data");
        assertEquals("PO2", report.pos().get(1).code());
        assertTrue(report.pos().stream().allMatch(r -> r.status().equals("No evidence")));
    }

    @Test
    @DisplayName("studentReport appends a custom (non-Washington-Accord) PO after the standard ones when it has data")
    void studentReportAppendsCustomPoWithData() {
        when(programOutcomeRepository.findByIsDefaultTrueOrderByDisplayOrderAsc()).thenReturn(List.of(po1));
        when(programOutcomeRepository.findByCode("PO99")).thenReturn(Optional.of(customPo));

        Map<String, Object> customRow = new LinkedHashMap<>();
        customRow.put("poCode", "PO99");
        customRow.put("creditsEarned", 2);
        customRow.put("maxCredits", 4);
        customRow.put("percentage", 50.0);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("moduleCount", 1L);
        summary.put("poSummaries", List.of(customRow));

        when(studentRepository.findById("EN001")).thenReturn(Optional.of(student1));
        when(poAttainmentService.getStudentPOSummary("EN001")).thenReturn(summary);

        PoStudentReport report = service.studentReport("EN001", 40.0);

        assertEquals(2, report.pos().size(), "PO1 (standard, no evidence) + PO99 (custom, with data)");
        assertEquals("PO1", report.pos().get(0).code(), "Standard PO comes first even with no evidence");
        assertEquals("PO99", report.pos().get(1).code());
        assertEquals("Attained", report.pos().get(1).status(), "50% >= 40% threshold");
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
        when(programOutcomeRepository.findByIsDefaultTrueOrderByDisplayOrderAsc()).thenReturn(List.of(po1));
        when(outcomeMappingRepository.findByStatus(OutcomeMapping.ApprovalStatus.APPROVED)).thenReturn(List.of());

        when(studentRepository.findByBatch("24")).thenReturn(Arrays.asList(student1, student2));

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
        when(programOutcomeRepository.findByIsDefaultTrueOrderByDisplayOrderAsc()).thenReturn(List.of(po1));
        when(outcomeMappingRepository.findByStatus(OutcomeMapping.ApprovalStatus.APPROVED)).thenReturn(List.of());

        when(studentRepository.findByBatch("24")).thenReturn(Arrays.asList(student1, student2));

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
    @DisplayName("batchReport always lists every Washington Accord standard PO, even with no mapping or data at all")
    void batchReportAlwaysListsWashingtonAccordDefaults() {
        when(programOutcomeRepository.findByIsDefaultTrueOrderByDisplayOrderAsc()).thenReturn(List.of(po1, po2));
        when(outcomeMappingRepository.findByStatus(OutcomeMapping.ApprovalStatus.APPROVED)).thenReturn(List.of());

        when(studentRepository.findByBatch("24")).thenReturn(Arrays.asList(student1, student2));
        when(studentPoCreditRepository.findByBatch("24")).thenReturn(Collections.emptyList());

        PoBatchReport report = service.batchReport("24", 40.0, 60.0);

        assertEquals(2, report.pos().size(), "Both standard POs must appear even with zero mapped or credited data");
        assertEquals("PO1", report.pos().get(0).code());
        assertEquals("PO2", report.pos().get(1).code());
        assertTrue(report.pos().stream().allMatch(r -> r.studentsAttained() == 0 && r.status().equals("Not successful")));
    }

    @Test
    @DisplayName("batchReport appends a custom PO with an approved mapping after the standard ones")
    void batchReportAppendsCustomMappedPo() {
        when(programOutcomeRepository.findByIsDefaultTrueOrderByDisplayOrderAsc()).thenReturn(List.of(po1));

        OutcomeMapping customMapping = new OutcomeMapping();
        customMapping.setId(2L);
        customMapping.setProgramOutcome(customPo);
        customMapping.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        Los los = new Los(); los.setId("LO099");
        customMapping.setLearningOutcome(los);
        when(outcomeMappingRepository.findByStatus(OutcomeMapping.ApprovalStatus.APPROVED)).thenReturn(List.of(customMapping));

        when(studentRepository.findByBatch("24")).thenReturn(Arrays.asList(student1, student2));
        StudentPoCredit customCredit = credit(student1, customPo, module, "24", 5, 5);
        when(studentPoCreditRepository.findByBatch("24")).thenReturn(List.of(customCredit));

        PoBatchReport report = service.batchReport("24", 40.0, 60.0);

        assertEquals(2, report.pos().size(), "PO1 (standard, no data) + PO99 (custom, mapped and credited)");
        assertEquals("PO1", report.pos().get(0).code());
        assertEquals("PO99", report.pos().get(1).code());
        assertEquals(1, report.pos().get(1).studentsAttained());
    }

    @Test
    @DisplayName("batchReport 404s when the batch has no students")
    void batchReportMissingBatch() {
        when(studentRepository.findByBatch("99")).thenReturn(Collections.emptyList());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.batchReport("99", 40.0, 60.0));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("studentReportsForBatch builds one individual report per student in the batch")
    void studentReportsForBatchBuildsOneReportPerStudent() {
        when(programOutcomeRepository.findByIsDefaultTrueOrderByDisplayOrderAsc()).thenReturn(List.of(po1));
        when(studentRepository.findByBatch("24")).thenReturn(Arrays.asList(student1, student2));
        when(studentRepository.findById("EN001")).thenReturn(Optional.of(student1));
        when(studentRepository.findById("EN002")).thenReturn(Optional.of(student2));

        Map<String, Object> po1RowForStudent1 = new LinkedHashMap<>();
        po1RowForStudent1.put("poCode", "PO1");
        po1RowForStudent1.put("creditsEarned", 4);
        po1RowForStudent1.put("maxCredits", 5);
        po1RowForStudent1.put("percentage", 80.0);
        Map<String, Object> summary1 = new LinkedHashMap<>();
        summary1.put("moduleCount", 1L);
        summary1.put("poSummaries", List.of(po1RowForStudent1));
        when(poAttainmentService.getStudentPOSummary("EN001")).thenReturn(summary1);

        Map<String, Object> summary2 = new LinkedHashMap<>();
        summary2.put("moduleCount", 0L);
        summary2.put("poSummaries", List.of());
        when(poAttainmentService.getStudentPOSummary("EN002")).thenReturn(summary2);

        List<PoStudentReport> reports = service.studentReportsForBatch("24", 40.0);

        assertEquals(2, reports.size(), "One report per student in the batch");
        PoStudentReport report1 = reports.stream().filter(r -> r.studentId().equals("EN001")).findFirst().orElseThrow();
        assertEquals("Attained", report1.pos().get(0).status(), "80% >= 40% threshold");
        PoStudentReport report2 = reports.stream().filter(r -> r.studentId().equals("EN002")).findFirst().orElseThrow();
        assertEquals("No evidence", report2.pos().get(0).status(), "No saved credits for this student");
    }

    @Test
    @DisplayName("studentReportsForBatch 404s when the batch has no students")
    void studentReportsForBatchMissingBatch() {
        when(studentRepository.findByBatch("99")).thenReturn(Collections.emptyList());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.studentReportsForBatch("99", 40.0));
        assertEquals(404, ex.getStatusCode().value());
    }

    private StudentPoCredit credit(Student student, ProgramOutcome po, Module module, String batch, int earned, int max) {
        StudentPoCredit c = new StudentPoCredit();
        c.setStudent(student); c.setProgramOutcome(po); c.setModule(module); c.setBatch(batch);
        c.setCreditsEarned(earned); c.setMaxCredits(max); c.setThreshold(50);
        return c;
    }
}
