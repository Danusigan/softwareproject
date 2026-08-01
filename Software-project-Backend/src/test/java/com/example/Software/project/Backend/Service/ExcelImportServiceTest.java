package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExcelImportService.importMarksBulk — the parser behind the main
 * Marks Workbench bulk-upload workflow. Builds real in-memory .xlsx workbooks
 * with Apache POI so the row/column parsing and validation rules are exercised
 * for real, not just mocked around.
 */
@DisplayName("ExcelImportService.importMarksBulk Tests")
class ExcelImportServiceTest {

    @Mock
    private StudentMarkRepository markRepository;
    @Mock
    private LosRepository losRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AssessmentItemRepository assessmentItemRepository;
    @Mock
    private AssessmentTemplateRepository assessmentTemplateRepository;
    @Mock
    private StudentAssessmentScoreRepository studentAssessmentScoreRepository;

    @InjectMocks
    private ExcelImportService excelImportService;

    private static final String[] LOS_IDS = {"LO001"};

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(losRepository.existsById("LO001")).thenReturn(true);
        when(losRepository.findById("LO001")).thenReturn(Optional.of(los("LO001")));
        when(studentRepository.findById(anyString())).thenAnswer(invocation -> {
            Student s = new Student();
            s.setStudentId(invocation.getArgument(0));
            s.setStudentName("Unknown");
            return Optional.of(s);
        });
    }

    private static Los los(String id) {
        Los l = new Los();
        l.setId(id);
        return l;
    }

    private MockMultipartFile workbookOf(String... rows) throws Exception {
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

    @Test
    @DisplayName("rejects the whole file when an unknown LO id is passed")
    void rejectsUnknownLo() throws Exception {
        when(losRepository.existsById("LO999")).thenReturn(false);
        MockMultipartFile file = workbookOf("Student Index|LO1", "EN001|55");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> excelImportService.importMarksBulk(file, new String[]{"LO999"}, "20", "FINAL_EXAM"));

        assertTrue(ex.getMessage().contains("LO999"));
        verify(markRepository, never()).save(any());
    }

    @Test
    @DisplayName("imports a valid row and reports the correct count")
    void importsValidRow() throws Exception {
        MockMultipartFile file = workbookOf("Student Index|LO1", "EN001|55");

        String result = excelImportService.importMarksBulk(file, LOS_IDS, "20", "FINAL_EXAM");

        assertTrue(result.contains("1"), "expected 1 mark imported: " + result);
        ArgumentCaptor<StudentMark> captor = ArgumentCaptor.forClass(StudentMark.class);
        verify(markRepository, times(1)).save(captor.capture());
        assertEquals(55.0, captor.getValue().getScore());
        assertEquals("EN001", captor.getValue().getStudent().getStudentId());
    }

    @Test
    @DisplayName("skips rows with a blank student index instead of importing garbage")
    void skipsBlankStudentIndexRows() throws Exception {
        MockMultipartFile file = workbookOf("Student Index|LO1", "EN001|55", "|60");

        String result = excelImportService.importMarksBulk(file, LOS_IDS, "20", "FINAL_EXAM");

        assertTrue(result.contains("1"), "blank-index row must not be imported: " + result);
        verify(markRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("treats AB/MC/N-A cells as absent rather than a zero score")
    void treatsAbsentMarkersAsSkipped() throws Exception {
        MockMultipartFile file = workbookOf("Student Index|LO1", "EN001|AB", "EN002|MC", "EN003|45");

        String result = excelImportService.importMarksBulk(file, LOS_IDS, "20", "FINAL_EXAM");

        assertTrue(result.contains("1"), "only the numeric row should be imported: " + result);
        verify(markRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("rejects the whole file when any score exceeds the per-LO max marks")
    void rejectsOutOfRangeScore() throws Exception {
        // Default max marks is 100 when no per-LO max is supplied and header has no "max=" hint.
        MockMultipartFile file = workbookOf("Student Index|LO1", "EN001|150");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> excelImportService.importMarksBulk(file, LOS_IDS, "20", "FINAL_EXAM"));

        assertTrue(ex.getMessage().contains("out of range"), ex.getMessage());
        verify(markRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a negative score")
    void rejectsNegativeScore() throws Exception {
        MockMultipartFile file = workbookOf("Student Index|LO1", "EN001|-5");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> excelImportService.importMarksBulk(file, LOS_IDS, "20", "FINAL_EXAM"));

        assertTrue(ex.getMessage().contains("out of range"), ex.getMessage());
    }

    @Test
    @DisplayName("shifts to row 1 as the header when row 0 is a title row, not 'Student Index'")
    void shiftsHeaderRowWhenRow0IsATitle() throws Exception {
        MockMultipartFile file = workbookOf("Marks Upload Template", "Student Index|LO1", "EN001|70");

        String result = excelImportService.importMarksBulk(file, LOS_IDS, "20", "FINAL_EXAM");

        assertTrue(result.contains("1"), "expected the single data row below the shifted header to import: " + result);
        verify(markRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("respects an explicit per-LO max marks override for range validation")
    void respectsExplicitPerLoMaxMarks() throws Exception {
        MockMultipartFile file = workbookOf("Student Index|LO1", "EN001|15");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> excelImportService.importMarksBulk(file, LOS_IDS, "20", "FINAL_EXAM", null,
                java.util.Map.of("LO001", 10.0)));

        assertTrue(ex.getMessage().contains("out of range"), ex.getMessage());
    }
}
