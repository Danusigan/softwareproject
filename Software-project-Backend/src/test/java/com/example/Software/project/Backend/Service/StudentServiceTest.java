package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Student;
import com.example.Software.project.Backend.Repository.StudentRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("StudentService")
class StudentServiceTest {

    @Mock private StudentRepository studentRepository;

    @InjectMocks
    private StudentService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private MultipartFile workbookOf(String[]... rows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Students");
            Row header = sheet.createRow(0);
            String[] headers = {"Student ID", "Student Name", "Email", "Academic Year", "Batch"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    if (rows[r][c] != null) row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            wb.write(out);
            return new MockMultipartFile("file", "students.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    @DisplayName("importFromExcel creates a new student with batch populated")
    void importCreatesNewStudent() throws Exception {
        when(studentRepository.findById("EN001")).thenReturn(Optional.empty());

        MultipartFile file = workbookOf(new String[]{"EN001", "Student One", "s1@example.com", "2024", "24"});
        Map<String, Object> result = service.importFromExcel(file);

        assertEquals(1, result.get("totalRows"));
        assertEquals(1, result.get("created"));
        assertEquals(0, result.get("updated"));

        ArgumentCaptor<List<Student>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(studentRepository).saveAll(captor.capture());
        Student saved = captor.getValue().get(0);
        assertEquals("EN001", saved.getStudentId());
        assertEquals("Student One", saved.getStudentName());
        assertEquals("24", saved.getBatch(), "Batch must be populated from the upload - this is the whole point of the feature");
        assertEquals("2024", saved.getAcademicYear());
    }

    @Test
    @DisplayName("importFromExcel updates an existing student but keeps its batch when the cell is blank")
    void importUpdateKeepsExistingBatchWhenBlank() throws Exception {
        Student existing = new Student();
        existing.setStudentId("EN001");
        existing.setStudentName("Old Name");
        existing.setBatch("23");
        when(studentRepository.findById("EN001")).thenReturn(Optional.of(existing));

        // Batch cell left blank on this row - existing batch "23" must not be wiped out.
        MultipartFile file = workbookOf(new String[]{"EN001", "New Name", null, null, null});
        Map<String, Object> result = service.importFromExcel(file);

        assertEquals(0, result.get("created"));
        assertEquals(1, result.get("updated"));

        ArgumentCaptor<List<Student>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(studentRepository).saveAll(captor.capture());
        Student saved = captor.getValue().get(0);
        assertEquals("New Name", saved.getStudentName());
        assertEquals("23", saved.getBatch(), "Blank batch cell on update must not overwrite the existing value");
    }

    @Test
    @DisplayName("importFromExcel rejects the whole file when a row is missing Student Name")
    void importRejectsMissingName() throws Exception {
        when(studentRepository.findById(any())).thenReturn(Optional.empty());
        MultipartFile file = workbookOf(
                new String[]{"EN001", "Valid Student", null, null, "24"},
                new String[]{"EN002", null, null, null, "24"});

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.importFromExcel(file));
        assertTrue(ex.getMessage().contains("EN002"));
        org.mockito.Mockito.verify(studentRepository, org.mockito.Mockito.never()).saveAll(any());
    }

    @Test
    @DisplayName("importFromExcel rejects a batch value longer than the student_po_credit column width")
    void importRejectsOverlongBatch() throws Exception {
        when(studentRepository.findById(any())).thenReturn(Optional.empty());
        String tooLong = "1".repeat(21); // student_po_credit.batch is varchar(20)
        MultipartFile file = workbookOf(new String[]{"EN001", "Student One", null, null, tooLong});

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.importFromExcel(file));
        assertTrue(ex.getMessage().contains("Batch exceeds"));
    }

    @Test
    @DisplayName("importFromExcel rejects duplicate Student IDs within the same file")
    void importRejectsDuplicateIdInFile() throws Exception {
        when(studentRepository.findById(any())).thenReturn(Optional.empty());
        MultipartFile file = workbookOf(
                new String[]{"EN001", "Student One", null, null, "24"},
                new String[]{"EN001", "Student One Duplicate", null, null, "24"});

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.importFromExcel(file));
        assertTrue(ex.getMessage().contains("duplicate Student ID"));
    }

    @Test
    @DisplayName("importFromExcel rejects a file with no data rows")
    void importRejectsEmptyFile() throws Exception {
        MultipartFile file = workbookOf();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.importFromExcel(file));
        assertTrue(ex.getMessage().contains("No student rows found"));
    }

    @Test
    @DisplayName("generateTemplate produces a readable workbook with the expected headers")
    void generateTemplateHasExpectedHeaders() throws Exception {
        byte[] bytes = service.generateTemplate();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (var wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("Student ID", header.getCell(0).toString());
            assertEquals("Batch", header.getCell(4).toString());
        }
    }

    @Test
    @DisplayName("list filters by batch and academic year")
    void listFiltersByBatchAndYear() {
        Student s1 = new Student(); s1.setStudentId("EN001"); s1.setStudentName("A"); s1.setBatch("24"); s1.setAcademicYear("2024");
        Student s2 = new Student(); s2.setStudentId("EN002"); s2.setStudentName("B"); s2.setBatch("23"); s2.setAcademicYear("2023");
        when(studentRepository.findAll()).thenReturn(List.of(s1, s2));

        List<Student> filtered = service.list("24", null);
        assertEquals(1, filtered.size());
        assertEquals("EN001", filtered.get(0).getStudentId());
    }
}
