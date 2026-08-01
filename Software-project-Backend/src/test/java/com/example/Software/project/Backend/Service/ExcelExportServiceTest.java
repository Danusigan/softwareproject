package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ExcelExportService.generateMarksExcel — the pass/fail report download
 * behind the Marks Workbench "export" action. Generates a real workbook with the mocked
 * repository data and reads it back with POI, so the actual header layout and per-cell
 * pass/fail text (not just that "some bytes" came back) are what's under test.
 */
@DisplayName("ExcelExportService.generateMarksExcel Tests")
class ExcelExportServiceTest {

    @Mock
    private StudentMarkRepository studentMarkRepository;
    @Mock
    private LosRepository losRepository;
    @Mock
    private AssessmentItemRepository assessmentItemRepository;
    @Mock
    private AssessmentTemplateRepository assessmentTemplateRepository;

    @InjectMocks
    private ExcelExportService excelExportService;

    private Los los1;
    private Student student1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        los1 = new Los();
        los1.setId("LO001");
        los1.setName("Learning Outcome 1");
        when(losRepository.findById("LO001")).thenReturn(Optional.of(los1));

        student1 = new Student();
        student1.setStudentId("EN001");
        student1.setStudentName("Student One");
    }

    private Sheet readGeneratedSheet(byte[] bytes) throws Exception {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            return wb.getSheetAt(0);
        }
    }

    @Test
    @DisplayName("header row is 'Index Number' followed by each LO's name")
    void headerRow_hasIndexNumberThenLoNames() throws Exception {
        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(
            List.of("LO001"), MarkType.FINAL_EXAM, "20")).thenReturn(Collections.emptyList());
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(
            List.of("LO001"), MarkType.FINAL_EXAM, "20")).thenReturn(Collections.emptyList());

        byte[] bytes = excelExportService.generateMarksExcel(List.of("LO001"), "FINAL_EXAM", "20", 50);
        Sheet sheet = readGeneratedSheet(bytes);
        Row header = sheet.getRow(0);

        assertEquals("Index Number", header.getCell(0).getStringCellValue());
        assertEquals("Learning Outcome 1", header.getCell(1).getStringCellValue());
    }

    @Test
    @DisplayName("marks the student card as 'Pass' when the normalized score meets the threshold")
    void marksPass_whenNormalizedScoreMeetsThreshold() throws Exception {
        StudentMark mark = new StudentMark();
        mark.setStudent(student1);
        mark.setLos(los1);
        mark.setScore(6.0); // 6 out of max 10 = 60%
        mark.setBatch("20");
        mark.setMarkType(MarkType.FINAL_EXAM);

        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(
            List.of("LO001"), MarkType.FINAL_EXAM, "20")).thenReturn(List.of(student1));
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(
            List.of("LO001"), MarkType.FINAL_EXAM, "20")).thenReturn(List.of(mark));

        AssessmentItem item = new AssessmentItem();
        item.setId(1L);
        item.setMaxMarks(10.0);
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(
            "LO001", "20", "FINAL_EXAM", MarkType.FINAL_EXAM)).thenReturn(List.of(item));

        byte[] bytes = excelExportService.generateMarksExcel(List.of("LO001"), "FINAL_EXAM", "20", 50);
        Sheet sheet = readGeneratedSheet(bytes);
        String cellText = sheet.getRow(1).getCell(1).getStringCellValue();

        assertEquals("Pass (6.00)", cellText);
    }

    @Test
    @DisplayName("marks the student card as 'Fail' when the normalized score is below threshold")
    void marksFail_whenNormalizedScoreBelowThreshold() throws Exception {
        StudentMark mark = new StudentMark();
        mark.setStudent(student1);
        mark.setLos(los1);
        mark.setScore(3.0); // 3 out of max 10 = 30%
        mark.setBatch("20");
        mark.setMarkType(MarkType.FINAL_EXAM);

        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(
            List.of("LO001"), MarkType.FINAL_EXAM, "20")).thenReturn(List.of(student1));
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(
            List.of("LO001"), MarkType.FINAL_EXAM, "20")).thenReturn(List.of(mark));

        AssessmentItem item = new AssessmentItem();
        item.setId(1L);
        item.setMaxMarks(10.0);
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(
            "LO001", "20", "FINAL_EXAM", MarkType.FINAL_EXAM)).thenReturn(List.of(item));

        byte[] bytes = excelExportService.generateMarksExcel(List.of("LO001"), "FINAL_EXAM", "20", 50);
        Sheet sheet = readGeneratedSheet(bytes);
        String cellText = sheet.getRow(1).getCell(1).getStringCellValue();

        assertEquals("Fail (3.00)", cellText);
    }

    @Test
    @DisplayName("falls back to treating the raw score as a percentage when no assessment items exist (legacy data)")
    void legacyFallback_treatsScoreAsPercentage() throws Exception {
        StudentMark mark = new StudentMark();
        mark.setStudent(student1);
        mark.setLos(los1);
        mark.setScore(60.0); // legacy: already a percentage
        mark.setBatch("20");
        mark.setMarkType(MarkType.FINAL_EXAM);

        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(
            List.of("LO001"), MarkType.FINAL_EXAM, "20")).thenReturn(List.of(student1));
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(
            List.of("LO001"), MarkType.FINAL_EXAM, "20")).thenReturn(List.of(mark));
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(
            "LO001", "20", "FINAL_EXAM", MarkType.FINAL_EXAM)).thenReturn(Collections.emptyList());

        byte[] bytes = excelExportService.generateMarksExcel(List.of("LO001"), "FINAL_EXAM", "20", 50);
        Sheet sheet = readGeneratedSheet(bytes);
        String cellText = sheet.getRow(1).getCell(1).getStringCellValue();

        assertEquals("Pass (60.00)", cellText);
    }

    @Test
    @DisplayName("writes 'N/A' for a student with no mark for a given LO")
    void writesNA_whenStudentHasNoMarkForLo() throws Exception {
        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(
            List.of("LO001"), MarkType.FINAL_EXAM, "20")).thenReturn(List.of(student1));
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(
            List.of("LO001"), MarkType.FINAL_EXAM, "20")).thenReturn(Collections.emptyList());

        byte[] bytes = excelExportService.generateMarksExcel(List.of("LO001"), "FINAL_EXAM", "20", 50);
        Sheet sheet = readGeneratedSheet(bytes);
        String cellText = sheet.getRow(1).getCell(1).getStringCellValue();

        assertEquals("N/A", cellText);
    }

    @Test
    @DisplayName("defaults the threshold to 50 when null is passed")
    void defaultsThresholdToFifty() throws Exception {
        StudentMark mark = new StudentMark();
        mark.setStudent(student1);
        mark.setLos(los1);
        mark.setScore(50.0); // exactly at the default threshold
        mark.setBatch("20");
        mark.setMarkType(MarkType.FINAL_EXAM);

        when(studentMarkRepository.findDistinctStudentsByLosIdsAndMarkTypeAndBatch(
            List.of("LO001"), MarkType.FINAL_EXAM, "20")).thenReturn(List.of(student1));
        when(studentMarkRepository.findByLosIdsAndMarkTypeAndBatch(
            List.of("LO001"), MarkType.FINAL_EXAM, "20")).thenReturn(List.of(mark));
        when(assessmentItemRepository.findByLos_IdAndAssessmentTemplate_BatchAndAssessmentTemplate_MarkType(
            "LO001", "20", "FINAL_EXAM", MarkType.FINAL_EXAM)).thenReturn(Collections.emptyList());

        byte[] bytes = excelExportService.generateMarksExcel(List.of("LO001"), "FINAL_EXAM", "20", null);
        Sheet sheet = readGeneratedSheet(bytes);
        String cellText = sheet.getRow(1).getCell(1).getStringCellValue();

        assertEquals("Pass (50.00)", cellText);
    }
}
