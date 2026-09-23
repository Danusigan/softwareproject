package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Student;
import com.example.Software.project.Backend.Repository.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Admin-only bulk student roster management: importing a class list (with batch) from Excel so
 * marks upload, PO credit calculation and the batch PO report (which all key off
 * {@code students.batch}) have real student/batch data to work with, instead of relying on
 * students being created implicitly - with no batch - the first time marks are uploaded for them
 * (see ExcelImportService's upsert-on-import behaviour).
 */
@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    // Matches student_po_credit's narrowed column widths (V3__student_po_credit.sql) - a
    // student/batch that doesn't fit there would otherwise only fail later, silently, the first
    // time PO credits are calculated for them.
    private static final int MAX_STUDENT_ID_LENGTH = 100;
    private static final int MAX_BATCH_LENGTH = 20;

    private static final List<String> HEADERS = List.of("Student ID", "Student Name", "Email", "Academic Year", "Batch");

    /**
     * Imports/updates students from an uploaded Excel sheet (row 0 = headers, matching
     * {@link #HEADERS}). Existing students (matched by Student ID) are updated; new ones are
     * created. Blank optional cells (email/academic year/batch) leave an existing student's
     * value untouched rather than blanking it out. Rejects the whole file if any row is invalid -
     * same all-or-nothing convention ExcelImportService uses for marks uploads.
     */
    @Transactional
    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        List<String> errors = new ArrayList<>();
        List<Student> toSave = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        int created = 0, updated = 0, rowCount = 0;

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // header row
                if (isRowBlank(row)) continue;
                rowCount++;
                int excelRow = row.getRowNum() + 1;

                String studentId = cellString(row, 0);
                String studentName = cellString(row, 1);
                String email = cellString(row, 2);
                String academicYear = cellString(row, 3);
                String batch = cellString(row, 4);

                if (studentId == null) {
                    errors.add("Row " + excelRow + ": Student ID is required");
                    continue;
                }
                if (studentId.length() > MAX_STUDENT_ID_LENGTH) {
                    errors.add("Row " + excelRow + ", Student " + studentId + ": Student ID exceeds " + MAX_STUDENT_ID_LENGTH + " characters");
                    continue;
                }
                if (studentName == null) {
                    errors.add("Row " + excelRow + ", Student " + studentId + ": Student Name is required");
                    continue;
                }
                if (batch != null && batch.length() > MAX_BATCH_LENGTH) {
                    errors.add("Row " + excelRow + ", Student " + studentId + ": Batch exceeds " + MAX_BATCH_LENGTH + " characters");
                    continue;
                }
                if (!seenIds.add(studentId)) {
                    errors.add("Row " + excelRow + ", Student " + studentId + ": duplicate Student ID within this file");
                    continue;
                }

                Student student = studentRepository.findById(studentId).orElse(null);
                if (student == null) {
                    student = new Student();
                    student.setStudentId(studentId);
                    created++;
                } else {
                    updated++;
                }
                student.setStudentName(studentName);
                if (email != null) student.setEmail(email);
                if (academicYear != null) student.setAcademicYear(academicYear);
                if (batch != null) student.setBatch(batch);
                toSave.add(student);
            }
        }

        if (rowCount == 0) {
            throw new IllegalArgumentException("No student rows found. Use the template and fill in at least one row below the header.");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Upload rejected - invalid rows found:\n" + String.join("\n", errors));
        }

        studentRepository.saveAll(toSave);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRows", toSave.size());
        result.put("created", created);
        result.put("updated", updated);
        return result;
    }

    /** A blank .xlsx with the expected headers and one example row, for the admin to fill in. */
    public byte[] generateTemplate() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Students");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS.get(i));
                cell.setCellStyle(headerStyle);
            }

            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("EG/2024/6555");
            example.createCell(1).setCellValue("A. B. Perera");
            example.createCell(2).setCellValue("perera@example.com");
            example.createCell(3).setCellValue("2024");
            example.createCell(4).setCellValue("24");

            for (int i = 0; i < HEADERS.size(); i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /** All students, optionally filtered by batch and/or academic year, sorted by Student ID. */
    public List<Student> list(String batch, String academicYear) {
        List<Student> students = studentRepository.findAll();
        return students.stream()
                .filter(s -> batch == null || batch.isBlank() || batch.equals(s.getBatch()))
                .filter(s -> academicYear == null || academicYear.isBlank() || academicYear.equals(s.getAcademicYear()))
                .sorted(Comparator.comparing(Student::getStudentId))
                .toList();
    }

    private boolean isRowBlank(Row row) {
        for (int i = 0; i < HEADERS.size(); i++) {
            if (cellString(row, i) != null) return false;
        }
        return true;
    }

    private String cellString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        String text;
        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            text = (value == Math.floor(value) && !Double.isInfinite(value))
                    ? String.valueOf((long) value)
                    : String.valueOf(value);
        } else {
            text = cell.toString();
        }
        text = text.trim();
        return text.isEmpty() ? null : text;
    }
}
