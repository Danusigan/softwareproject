package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Assignment;
import com.example.Software.project.Backend.Model.Student;
import com.example.Software.project.Backend.Model.StudentMark;
import com.example.Software.project.Backend.Repository.AssignmentRepository;
import com.example.Software.project.Backend.Repository.StudentRepository;
import com.example.Software.project.Backend.Repository.StudentMarkRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ExcelImportService {
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private StudentMarkRepository studentMarkRepository;
    
    /**
     * Import marks from Excel file with OBE specification
     * @param file MultipartFile Excel file
     * @param assessmentId Long assessment ID (converted to String internally)
     * @return Import result message
     */
    public String importMarks(MultipartFile file, Long assessmentId) {
        return importMarksOBEFormat(String.valueOf(assessmentId), file);
    }
    
    // Import student marks from Excel file (OBE Format: 2 columns)
    public String importMarksOBEFormat(String assignmentId, MultipartFile excelFile) {
        if (excelFile.isEmpty()) {
            throw new RuntimeException("Excel file is empty or not provided");
        }
        
        // Validate assignment exists
        Optional<Assignment> assignmentOpt = assignmentRepository.findById(assignmentId);
        if (assignmentOpt.isEmpty()) {
            throw new RuntimeException("Assignment not found: " + assignmentId);
        }
        Assignment assignment = assignmentOpt.get();
        
        try {
            List<StudentMark> importedMarks = processExcelFileOBEFormat(excelFile, assignment);
            
            // Save all student marks
            studentMarkRepository.saveAll(importedMarks);
            
            return String.format("Successfully imported %d student marks for assignment: %s", 
                    importedMarks.size(), assignment.getAssignmentName());
            
        } catch (Exception e) {
            throw new RuntimeException("Error importing Excel file: " + e.getMessage(), e);
        }
    }

    // Process Excel file with OBE format (2 columns: Student Index, Mark)
    private List<StudentMark> processExcelFileOBEFormat(MultipartFile excelFile, Assignment assignment) throws IOException {
        List<StudentMark> studentMarks = new ArrayList<>();
        
        try (InputStream inputStream = excelFile.getInputStream()) {
            Workbook workbook = createWorkbook(inputStream, excelFile.getOriginalFilename());
            Sheet sheet = workbook.getSheetAt(0); // Use first sheet
            
            boolean isFirstRow = true;
            int processedRows = 0;
            int skippedRows = 0;
            
            for (Row row : sheet) {
                // Skip header row
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }
                
                // Skip empty rows
                if (isRowEmpty(row)) {
                    continue;
                }
                
                try {
                    StudentMark studentMark = processStudentMarkRowOBEFormat(row, assignment);
                    if (studentMark != null) {
                        studentMarks.add(studentMark);
                        processedRows++;
                    } else {
                        skippedRows++;
                    }
                } catch (Exception e) {
                    skippedRows++;
                    System.err.println("Error processing row " + row.getRowNum() + ": " + e.getMessage());
                }
            }
            
            workbook.close();
            
            System.out.println(String.format("OBE Import summary: %d processed, %d skipped", processedRows, skippedRows));
        }
        
        return studentMarks;
    }

    // Process individual row with OBE format (Student Index, Mark)
    private StudentMark processStudentMarkRowOBEFormat(Row row, Assignment assignment) {
        try {
            // Column 0: Student Index (String)
            // Column 1: Mark (Numeric)
            Cell studentIndexCell = row.getCell(0);
            Cell markCell = row.getCell(1);
            
            if (studentIndexCell == null) {
                throw new RuntimeException("Student Index is required");
            }
            
            String studentIndex = getCellStringValue(studentIndexCell).trim();
            if (studentIndex.isEmpty()) {
                throw new RuntimeException("Student Index cannot be empty");
            }
            
            // Get or create student using index as both ID and name initially
            Student student = getOrCreateStudentByIndex(studentIndex);
            
            // Check if mark already exists for this student and assignment
            if (studentMarkRepository.existsByStudentAndAssignment(student, assignment)) {
                System.out.println("Mark already exists for student " + studentIndex + " in assignment " + assignment.getAssignmentId() + ", skipping...");
                return null;
            }
            
            // Process mark with OBE rules
            StudentMark studentMark = parseStudentMarkOBEFormat(markCell, student, assignment);
            
            return studentMark;
            
        } catch (Exception e) {
            throw new RuntimeException("Error processing student mark: " + e.getMessage());
        }
    }

    // Get or create student by index only
    private Student getOrCreateStudentByIndex(String studentIndex) {
        Optional<Student> existingStudent = studentRepository.findById(studentIndex);
        
        if (existingStudent.isPresent()) {
            return existingStudent.get();
        } else {
            // Create new student with index as both ID and name
            Student newStudent = new Student(studentIndex, "Student " + studentIndex, null);
            return studentRepository.save(newStudent);
        }
    }

    // Parse mark with OBE format and data cleaning rules
    private StudentMark parseStudentMarkOBEFormat(Cell markCell, Student student, Assignment assignment) {
        if (markCell == null) {
            // No mark provided - set to 0.0
            return new StudentMark(student, assignment, 0.0);
        }
        
        String markValue = getCellStringValue(markCell).trim().toUpperCase();
        
        // Handle non-numeric values (AB, MC, etc.) - set to 0.0 as per OBE spec
        if ("AB".equals(markValue) || "ABSENT".equals(markValue) || 
            "MC".equals(markValue) || "MEDICAL".equals(markValue) || 
            "N/A".equals(markValue) || "NA".equals(markValue)) {
            return new StudentMark(student, assignment, 0.0);
        }
        
        // Parse numeric mark
        try {
            double mark = Double.parseDouble(markValue);
            
            // Data cleaning: Clamp marks between 0.0 and 100.0 as per OBE spec
            mark = Math.max(0.0, Math.min(100.0, mark));
            
            return new StudentMark(student, assignment, mark);
            
        } catch (NumberFormatException e) {
            // Non-numeric value, set to 0.0 as per OBE spec
            System.out.println("Non-numeric mark for student " + student.getStudentId() + ": " + markValue + " - setting to 0.0");
            return new StudentMark(student, assignment, 0.0);
        }
    }

    // Import student marks from Excel file
    public String importStudentMarksFromExcel(String assignmentId, MultipartFile excelFile) {
        if (excelFile.isEmpty()) {
            throw new RuntimeException("Excel file is empty or not provided");
        }
        
        // Validate assignment exists
        Optional<Assignment> assignmentOpt = assignmentRepository.findById(assignmentId);
        if (assignmentOpt.isEmpty()) {
            throw new RuntimeException("Assignment not found: " + assignmentId);
        }
        Assignment assignment = assignmentOpt.get();
        
        try {
            List<StudentMark> importedMarks = processExcelFile(excelFile, assignment);
            
            // Save all student marks
            studentMarkRepository.saveAll(importedMarks);
            
            return String.format("Successfully imported %d student marks for assignment: %s", 
                    importedMarks.size(), assignment.getAssignmentName());
            
        } catch (Exception e) {
            throw new RuntimeException("Error importing Excel file: " + e.getMessage(), e);
        }
    }
    
    // Process Excel file and extract student marks
    private List<StudentMark> processExcelFile(MultipartFile excelFile, Assignment assignment) throws IOException {
        List<StudentMark> studentMarks = new ArrayList<>();
        
        try (InputStream inputStream = excelFile.getInputStream()) {
            Workbook workbook = createWorkbook(inputStream, excelFile.getOriginalFilename());
            Sheet sheet = workbook.getSheetAt(0); // Use first sheet
            
            boolean isFirstRow = true;
            int processedRows = 0;
            int skippedRows = 0;
            
            for (Row row : sheet) {
                // Skip header row
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }
                
                // Skip empty rows
                if (isRowEmpty(row)) {
                    continue;
                }
                
                try {
                    StudentMark studentMark = processStudentMarkRow(row, assignment);
                    if (studentMark != null) {
                        studentMarks.add(studentMark);
                        processedRows++;
                    } else {
                        skippedRows++;
                    }
                } catch (Exception e) {
                    skippedRows++;
                    System.err.println("Error processing row " + row.getRowNum() + ": " + e.getMessage());
                }
            }
            
            workbook.close();
            
            System.out.println(String.format("Import summary: %d processed, %d skipped", processedRows, skippedRows));
        }
        
        return studentMarks;
    }
    
    // Create appropriate workbook based on file extension
    private Workbook createWorkbook(InputStream inputStream, String fileName) throws IOException {
        if (fileName.endsWith(".xlsx")) {
            return new XSSFWorkbook(inputStream);
        } else if (fileName.endsWith(".xls")) {
            return new HSSFWorkbook(inputStream);
        } else {
            throw new RuntimeException("Unsupported file format. Please use .xlsx or .xls files.");
        }
    }
    
    // Process individual row to create StudentMark
    private StudentMark processStudentMarkRow(Row row, Assignment assignment) {
        try {
            // Expected columns: Student ID | Student Name | Mark
            Cell studentIdCell = row.getCell(0);
            Cell studentNameCell = row.getCell(1);
            Cell markCell = row.getCell(2);
            
            if (studentIdCell == null) {
                throw new RuntimeException("Student ID is required");
            }
            
            String studentId = getCellStringValue(studentIdCell).trim();
            if (studentId.isEmpty()) {
                throw new RuntimeException("Student ID cannot be empty");
            }
            
            String studentName = studentNameCell != null ? getCellStringValue(studentNameCell).trim() : "";
            if (studentName.isEmpty()) {
                throw new RuntimeException("Student name is required");
            }
            
            // Get or create student
            Student student = getOrCreateStudent(studentId, studentName);
            
            // Check if mark already exists for this student and assignment
            if (studentMarkRepository.existsByStudentAndAssignment(student, assignment)) {
                System.out.println("Mark already exists for student " + studentId + " in assignment " + assignment.getAssignmentId() + ", skipping...");
                return null;
            }
            
            // Process mark
            StudentMark studentMark = parseStudentMark(markCell, student, assignment);
            
            return studentMark;
            
        } catch (Exception e) {
            throw new RuntimeException("Error processing student mark: " + e.getMessage());
        }
    }
    
    // Get or create student
    private Student getOrCreateStudent(String studentId, String studentName) {
        Optional<Student> existingStudent = studentRepository.findById(studentId);
        
        if (existingStudent.isPresent()) {
            Student student = existingStudent.get();
            // Update name if different
            if (!student.getStudentName().equals(studentName)) {
                student.setStudentName(studentName);
                studentRepository.save(student);
            }
            return student;
        } else {
            // Create new student
            Student newStudent = new Student(studentId, studentName, null);
            return studentRepository.save(newStudent);
        }
    }
    
    // Parse mark cell and create StudentMark object
    private StudentMark parseStudentMark(Cell markCell, Student student, Assignment assignment) {
        if (markCell == null) {
            throw new RuntimeException("Mark cell is empty for student: " + student.getStudentId());
        }
        
        String markValue = getCellStringValue(markCell).trim().toUpperCase();
        
        // Handle special cases: AB (Absent), MC (Medical)
        if ("AB".equals(markValue) || "ABSENT".equals(markValue)) {
            return new StudentMark(student, assignment, true, false);
        }
        
        if ("MC".equals(markValue) || "MEDICAL".equals(markValue)) {
            return new StudentMark(student, assignment, false, true);
        }
        
        // Parse numeric mark
        try {
            double mark = Double.parseDouble(markValue);
            
            // Validate mark range
            if (mark < 0.0 || mark > 100.0) {
                throw new RuntimeException("Mark must be between 0 and 100 for student: " + student.getStudentId());
            }
            
            return new StudentMark(student, assignment, mark);
            
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid mark format for student " + student.getStudentId() + ": " + markValue);
        }
    }
    
    // Extract string value from any cell type
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    // Check if it's a whole number (for student IDs)
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }
    
    // Check if row is empty
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String cellValue = getCellStringValue(cell).trim();
                if (!cellValue.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    // Import multiple assignments from a workbook with multiple sheets
    public String importMultipleAssignmentsFromExcel(MultipartFile excelFile) {
        if (excelFile.isEmpty()) {
            throw new RuntimeException("Excel file is empty or not provided");
        }
        
        try (InputStream inputStream = excelFile.getInputStream()) {
            Workbook workbook = createWorkbook(inputStream, excelFile.getOriginalFilename());
            
            int totalImported = 0;
            List<String> results = new ArrayList<>();
            
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();
                
                // Assume first row has assignment information or use sheet name as assignment ID
                try {
                    Optional<Assignment> assignmentOpt = assignmentRepository.findById(sheetName);
                    if (assignmentOpt.isPresent()) {
                        List<StudentMark> sheetMarks = processExcelSheet(sheet, assignmentOpt.get());
                        studentMarkRepository.saveAll(sheetMarks);
                        totalImported += sheetMarks.size();
                        results.add(String.format("Sheet '%s': %d marks imported", sheetName, sheetMarks.size()));
                    } else {
                        results.add(String.format("Sheet '%s': Assignment not found, skipped", sheetName));
                    }
                } catch (Exception e) {
                    results.add(String.format("Sheet '%s': Error - %s", sheetName, e.getMessage()));
                }
            }
            
            workbook.close();
            
            return String.format("Multi-sheet import completed. Total marks imported: %d\\nDetails:\\n%s", 
                    totalImported, String.join("\\n", results));
            
        } catch (Exception e) {
            throw new RuntimeException("Error importing multi-sheet Excel file: " + e.getMessage(), e);
        }
    }
    
    // Process individual sheet
    private List<StudentMark> processExcelSheet(Sheet sheet, Assignment assignment) {
        List<StudentMark> studentMarks = new ArrayList<>();
        
        boolean isFirstRow = true;
        for (Row row : sheet) {
            if (isFirstRow) {
                isFirstRow = false;
                continue;
            }
            
            if (isRowEmpty(row)) {
                continue;
            }
            
            try {
                StudentMark studentMark = processStudentMarkRow(row, assignment);
                if (studentMark != null) {
                    studentMarks.add(studentMark);
                }
            } catch (Exception e) {
                System.err.println("Error processing row in sheet " + sheet.getSheetName() + ": " + e.getMessage());
            }
        }
        
        return studentMarks;
    }
    
    // Validate Excel file format before processing
    public String validateExcelFormat(MultipartFile excelFile) {
        try (InputStream inputStream = excelFile.getInputStream()) {
            Workbook workbook = createWorkbook(inputStream, excelFile.getOriginalFilename());
            Sheet sheet = workbook.getSheetAt(0);
            
            if (sheet.getPhysicalNumberOfRows() < 2) {
                return "Excel file must have at least 2 rows (header + data)";
            }
            
            Row headerRow = sheet.getRow(0);
            if (headerRow == null || headerRow.getPhysicalNumberOfCells() < 3) {
                return "Header row must have at least 3 columns: Student ID, Student Name, Mark";
            }
            
            workbook.close();
            return "Excel file format is valid";
            
        } catch (Exception e) {
            return "Invalid Excel file format: " + e.getMessage();
        }
    }
}