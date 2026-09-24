package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module; // disambiguate from java.lang.Module
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The whole chain, wired together for real on H2: an Excel marks upload, the per-LO batch
 * attainment CQI is judged on, the CQI action that attainment triggers, and each student's PO
 * credits. Every other test in this project covers one of those stages behind mocks; this one
 * exists to catch the stages disagreeing with each other.
 *
 * The fixture is small enough to work out by hand. One module, batch "24", two LOs, four
 * students; the sheet header carries each LO's max marks ("LO1 (max=50)"):
 *
 *            LO1 /50        LO2 /20
 *   S1       40  (80%)      15  (75%)
 *   S2       30  (60%)       5  (25%)
 *   S3       20  (40%)      18  (90%)
 *   S4       10  (20%)       4  (20%)
 *
 * LO1 (threshold 50): S1, S2 pass -> 2 of 4 = 50% of the batch attained it.
 * LO2 (threshold 60): S1, S3 pass -> 2 of 4 = 50% of the batch attained it.
 *
 * Mappings (all APPROVED): LO1 -> PO1 weight 3, LO2 -> PO1 weight 2, LO2 -> PO2 weight 1.
 * Flyway migrations are MySQL-flavored, so Hibernate builds the schema for H2 here, as in
 * DeleteOrderingRepositoryTest.
 */
@DataJpaTest(properties = {"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.org.springframework=WARN", "logging.level.org.hibernate=WARN"}, showSql = false)
@Import({ExcelImportService.class, POAttainmentService.class, CQIService.class,
        AttainmentService.class, JacksonAutoConfiguration.class})
class MarksToPoPipelineTest {

    private static final String MODULE = "EC5001";
    private static final String BATCH = "24";

    @Autowired TestEntityManager em;
    @Autowired ExcelImportService excelImportService;
    @Autowired POAttainmentService poAttainmentService;
    @Autowired CQIService cqiService;

    @BeforeEach
    void seedModuleLosStudentsAndMappings() {
        Module module = new Module();
        module.setModuleId(MODULE);
        module.setModuleName("Pipeline Module");
        em.persist(module);

        Los lo1 = lo("LO1", module, 50.0);
        Los lo2 = lo("LO2", module, 60.0);

        ProgramOutcome po1 = new ProgramOutcome("PO1", "PO1", "Knowledge", "Knowledge");
        ProgramOutcome po2 = new ProgramOutcome("PO2", "PO2", "Analysis", "Analysis");
        em.persist(po1);
        em.persist(po2);

        for (String id : List.of("S1", "S2", "S3", "S4")) {
            em.persist(new Student(id, "Student " + id, null));
        }

        mapping(lo1, po1, 3);
        mapping(lo2, po1, 2);
        mapping(lo2, po2, 1);
        em.flush();
    }

    @Test
    @DisplayName("uploaded marks become LO batch attainment, and only the LO below its own threshold raises a CQI action")
    void uploadedMarksTriggerCqiOnlyForTheLoBelowItsThreshold() throws Exception {
        excelImportService.importMarksBulk(marksSheet(), new String[]{"LO1", "LO2"}, BATCH, "FINAL_EXAM");

        Map<String, Object> result = cqiService.finalizeModuleAttainment(MODULE, BATCH);

        // LO1 sits exactly on its 50% threshold (50% >= 50%): no action.
        // LO2 is 50% against a 60% target: one action.
        assertEquals(1, result.get("triggeredCount"));
        List<CqiAction> history = cqiService.getCqiHistoryForModule(MODULE);
        assertEquals(1, history.size());
        CqiAction action = history.get(0);
        assertEquals("LO2", action.getLosId());
        assertEquals(50.0, action.getAttainmentScore(), 1e-9);
        assertEquals(60.0, action.getTargetScore(), 1e-9);
        assertEquals(CqiStatus.PLANNED, action.getStatus());
    }

    @Test
    @DisplayName("CQI judges each student against the student pass threshold and the batch against a separate batch target")
    void cqiUsesSeparateStudentPassThresholdAndBatchTarget() throws Exception {
        excelImportService.importMarksBulk(marksSheet(), new String[]{"LO1", "LO2"}, BATCH, "FINAL_EXAM");

        // Students must reach 70%: only S1 passes LO1 (25% of the batch), S1 and S3 pass LO2 (50%).
        // The batch target is a low 20%, so both LOs are attained and no action is raised -
        // even though LO2's own stored threshold (60) would have flagged it.
        Map<String, Object> lenient = cqiService.finalizeModuleAttainment(MODULE, BATCH, 70.0, 20.0);
        assertEquals(0, lenient.get("triggeredCount"));

        // Students must reach 60%: LO1 and LO2 are each passed by 2 of 4 (50% of the batch).
        // Against a 60% batch target both fall short; each action records the batch target.
        Map<String, Object> strict = cqiService.finalizeModuleAttainment(MODULE, BATCH, 60.0, 60.0);
        assertEquals(2, strict.get("triggeredCount"));
        for (CqiAction a : cqiService.getCqiHistoryForModule(MODULE)) {
            assertEquals(50.0, a.getAttainmentScore(), 1e-9, a.getLosId());
            assertEquals(60.0, a.getTargetScore(), 1e-9, a.getLosId());
        }
    }

    @Test
    @DisplayName("an assessment a student did not attend is left out for that student, by both CQI and PO credits - not counted as zero")
    @SuppressWarnings("unchecked")
    void missedAssessmentIsNotAttendedRatherThanZeroForBothCqiAndPo() throws Exception {
        // LO1 = Assignment 1 (max 20) + Final Exam (max 80). Final exam goes first: an unlabelled
        // upload clears every earlier mark for the LO and batch.
        excelImportService.importMarksBulk(
                sheet("Student Index|LO1 (max=80)", "S1|60", "S2|60"),
                new String[]{"LO1"}, BATCH, "FINAL_EXAM");
        excelImportService.importMarksBulk(
                sheet("Student Index|LO1 (max=20)", "S1|15", "S2|AB"),
                new String[]{"LO1"}, BATCH, "ASSIGNMENT", "Assignment 1");

        // S1: 15 + 60 = 75/100 = 75%.  S2 has no assignment mark (absent, a makeup may follow), so
        // S2 is judged on what they sat: 60/80 = 75%. Both pass a 70% pass mark, so 100% of the
        // batch attained LO1 and the 60% batch target is met: no CQI action.
        Map<String, Object> cqi = cqiService.finalizeModuleAttainment(MODULE, BATCH, 70.0, 60.0);
        assertEquals(0, cqi.get("triggeredCount"));

        // PO credits reach the same verdict on S2: LO1 passed, so both students earn PO1's weight (3).
        Map<String, Object> po = poAttainmentService.calculateStudentPOCredits(List.of("LO1"), BATCH, 70);
        Map<String, Integer> creditsByStudent = new java.util.HashMap<>();
        for (Map<String, Object> st : (List<Map<String, Object>>) po.get("students")) {
            creditsByStudent.put((String) st.get("studentId"), ((Map<String, Integer>) st.get("poCredits")).get("PO1"));
        }
        assertEquals(3, creditsByStudent.get("S1"));
        assertEquals(3, creditsByStudent.get("S2"));

        // The makeup assignment arrives later (a separate request, so a fresh persistence context): S2's result now includes it (12/20), so S2 = 72/100.
        // Re-read the template with its items, as a fresh request would, so the re-upload can replace it.
        em.flush();
        em.refresh(em.find(AssessmentTemplate.class, "lo_EC5001_24_ASSIGNMENT_Assignment_1"));
        excelImportService.importMarksBulk(
                sheet("Student Index|LO1 (max=20)", "S1|15", "S2|12"),
                new String[]{"LO1"}, BATCH, "ASSIGNMENT", "Assignment 1");
        assertEquals(1, cqiService.finalizeModuleAttainment(MODULE, BATCH, 73.0, 60.0).get("triggeredCount"),
                "S1 (75%) passes a 73% mark, S2 (72%) does not: 50% of the batch, under the 60% target");
    }

    @Test
    @DisplayName("uploaded marks accumulate into each student's PO credits: passed LOs add their mapped weights, failed ones add nothing")
    void uploadedMarksAccumulateIntoEachStudentsPoCredits() throws Exception {
        excelImportService.importMarksBulk(marksSheet(), new String[]{"LO1", "LO2"}, BATCH, "FINAL_EXAM");

        // What OBEController does right after an upload.
        poAttainmentService.recalculateForModule(MODULE, BATCH);

        // Available: PO1 = 3 + 2 = 5, PO2 = 1. Threshold for the auto-recalculation is 50%.
        assertPoStanding("S1", 5, 1);   // passed LO1 and LO2
        assertPoStanding("S2", 3, 0);   // passed LO1 only
        assertPoStanding("S3", 2, 1);   // passed LO2 only
        assertPoStanding("S4", 0, 0);   // passed neither
    }

    @Test
    @DisplayName("closing a CQI cycle records the next batch's result on the action but never changes any student's PO credits")
    void completingACqiCycleLeavesPoCreditsUntouched() throws Exception {
        excelImportService.importMarksBulk(marksSheet(), new String[]{"LO1", "LO2"}, BATCH, "FINAL_EXAM");
        poAttainmentService.recalculateForModule(MODULE, BATCH);
        cqiService.finalizeModuleAttainment(MODULE, BATCH);
        CqiAction action = cqiService.getCqiHistoryForModule(MODULE).get(0);
        cqiService.approvePlan(action.getId(), "admin1");

        // Next batch: everyone clears both LOs (LO1 40/50, LO2 15/20 = 100% of the batch attained).
        excelImportService.importMarksBulk(
                marksSheet("S1|40|15", "S2|40|15", "S3|40|15", "S4|40|15"),
                new String[]{"LO1", "LO2"}, "25", "FINAL_EXAM");
        cqiService.finalizeModuleAttainment(MODULE, "25");

        CqiAction closed = cqiService.getCqiHistoryForModule(MODULE).get(0);
        assertEquals(CqiStatus.COMPLETED, closed.getStatus());
        assertEquals(100.0, closed.getNextSemAttainment(), 1e-9);

        // Same standing as before the cycle closed: CQI does not write or adjust PO credits.
        assertPoStanding("S1", 5, 1);
        assertPoStanding("S2", 3, 0);
        assertPoStanding("S3", 2, 1);
        assertPoStanding("S4", 0, 0);
    }

    @Test
    @DisplayName("a student's PO summary flags the CQI cycle behind each module/batch contribution, without changing its credits")
    @SuppressWarnings("unchecked")
    void poSummaryFlagsTheCqiCycleForTheContributingBatch() throws Exception {
        excelImportService.importMarksBulk(marksSheet(), new String[]{"LO1", "LO2"}, BATCH, "FINAL_EXAM");
        poAttainmentService.recalculateForModule(MODULE, BATCH);
        cqiService.finalizeModuleAttainment(MODULE, BATCH);

        // Open (PLANNED) cycle: flagged as such, no next-batch result yet.
        Map<String, Object> open = cqiActionsOf("S1");
        assertEquals("LO2", open.get("losId"));
        assertEquals("PLANNED", open.get("status"));
        assertNull(open.get("nextSemAttainment"));

        cqiService.approvePlan(cqiService.getCqiHistoryForModule(MODULE).get(0).getId(), "admin1");
        excelImportService.importMarksBulk(
                marksSheet("S1|40|15", "S2|40|15", "S3|40|15", "S4|40|15"),
                new String[]{"LO1", "LO2"}, "25", "FINAL_EXAM");
        cqiService.finalizeModuleAttainment(MODULE, "25");

        Map<String, Object> closed = cqiActionsOf("S1");
        assertEquals("COMPLETED", closed.get("status"));
        assertEquals(100.0, (Double) closed.get("nextSemAttainment"), 1e-9);
        assertPoStanding("S1", 5, 1);
    }

    // --- fixture helpers -------------------------------------------------------------------

    /** The single CQI action flagged on S1's PO1 contribution from batch 24. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> cqiActionsOf(String studentId) {
        List<Map<String, Object>> poSummaries =
                (List<Map<String, Object>>) poAttainmentService.getStudentPOSummary(studentId).get("poSummaries");
        List<Map<String, Object>> breakdown = (List<Map<String, Object>>) poSummaries.get(0).get("moduleBreakdown");
        List<Map<String, Object>> actions = (List<Map<String, Object>>) breakdown.get(0).get("cqiActions");
        assertNotNull(actions, "moduleBreakdown should carry a cqiActions list");
        assertEquals(1, actions.size());
        return actions.get(0);
    }

    @SuppressWarnings("unchecked")
    private void assertPoStanding(String studentId, int expectedPo1, int expectedPo2) {
        Map<String, Object> summary = poAttainmentService.getStudentPOSummary(studentId);
        List<Map<String, Object>> poSummaries = (List<Map<String, Object>>) summary.get("poSummaries");
        assertEquals(2, poSummaries.size(), studentId + " should have a row for both POs");
        Map<String, Object> po1 = poSummaries.get(0);
        Map<String, Object> po2 = poSummaries.get(1);
        assertEquals("PO1", po1.get("poCode"));
        assertEquals("PO2", po2.get("poCode"));
        assertEquals(expectedPo1, po1.get("creditsEarned"), studentId + " PO1 credits earned");
        assertEquals(5, po1.get("maxCredits"), studentId + " PO1 credits available");
        assertEquals(expectedPo2, po2.get("creditsEarned"), studentId + " PO2 credits earned");
        assertEquals(1, po2.get("maxCredits"), studentId + " PO2 credits available");
    }

    private Los lo(String id, Module module, double threshold) {
        Los lo = new Los();
        lo.setId(id);
        lo.setName(id);
        lo.setModule(module);
        lo.setAttainmentThreshold(threshold);
        em.persist(lo);
        return lo;
    }

    private void mapping(Los lo, ProgramOutcome po, int weight) {
        OutcomeMapping m = new OutcomeMapping();
        m.setLearningOutcome(lo);
        m.setProgramOutcome(po);
        m.setWeight(weight);
        m.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        em.persist(m);
    }

    private MockMultipartFile marksSheet() throws Exception {
        return marksSheet("S1|40|15", "S2|30|5", "S3|20|18", "S4|10|4");
    }

    private MockMultipartFile marksSheet(String... studentRows) throws Exception {
        List<String> all = new java.util.ArrayList<>();
        all.add("Student Index|LO1 (max=50)|LO2 (max=20)");
        all.addAll(List.of(studentRows));
        return sheet(all.toArray(new String[0]));
    }

    private MockMultipartFile sheet(String... rows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Marks");
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                String[] cells = rows[r].split("\\|", -1);
                for (int c = 0; c < cells.length; c++) {
                    row.createCell(c).setCellValue(cells[c]);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("excelFile", "marks.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }
}
