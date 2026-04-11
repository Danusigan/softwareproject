package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Los;
import com.example.Software.project.Backend.Model.MarkType;
import com.example.Software.project.Backend.Model.Student;
import com.example.Software.project.Backend.Model.StudentMark;
import com.example.Software.project.Backend.Repository.LosRepository;
import com.example.Software.project.Backend.Repository.StudentMarkRepository;
import com.example.Software.project.Backend.Repository.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;

@Service
public class ExcelImportService {

    @Autowired
    private StudentMarkRepository markRepository;
    @Autowired
    private LosRepository losRepository;
    @Autowired
    private StudentRepository studentRepository;

    @Transactional
    public String importMarksOBEFormat(String losId, MultipartFile file, String batch, String markType) {
        try {
            Los los = losRepository.findById(losId)
                    .orElseThrow(() -> new Exception("Learning Outcome not found"));

            // Default to FINAL_EXAM if not specified
            MarkType type = MarkType.FINAL_EXAM;
            if (markType != null && !markType.isEmpty()) {
                try {
                    type = MarkType.valueOf(markType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    type = MarkType.FINAL_EXAM;
                }
            }

            try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
                Sheet sheet = workbook.getSheetAt(0);
                int count = 0;
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // Skip header

                    Cell indexCell = row.getCell(0);
                    Cell markCell = row.getCell(1);

                    if (indexCell == null || markCell == null) continue;

                    String studentIndex = indexCell.toString();
                    double score = 0.0;

                    if (markCell.getCellType() == CellType.NUMERIC) {
                        score = markCell.getNumericCellValue();
                    } else {
                        String val = markCell.toString().trim().toUpperCase();
                        if (val.equals("AB") || val.equals("MC")) {
                            score = 0.0;
                        } else {
                            try {
                                score = Double.parseDouble(val);
                            } catch (NumberFormatException e) {
                                score = 0.0;
                            }
                        }
                    }

                    // Clamp 0-100
                    score = Math.max(0.0, Math.min(100.0, score));

                    // Find or Create Student
                    Student student = studentRepository.findById(studentIndex)
                            .orElseGet(() -> {
                                Student newStudent = new Student();
                                newStudent.setStudentId(studentIndex);
                                newStudent.setStudentName("Unknown"); // Placeholder
                                return studentRepository.save(newStudent);
                            });

                    StudentMark mark = new StudentMark();
                    mark.setStudent(student);
                    mark.setScore(score);
                    mark.setLos(los);
                    mark.setBatch(batch); // Store batch with each mark
                    mark.setMarkType(type); // Store mark type
                    markRepository.save(mark);
                    count++;
                }
                return "Successfully imported " + count + " marks.";
            }
        } catch (Exception e) {
            throw new RuntimeException("Error importing marks: " + e.getMessage());
        }
    }

    /**
     * Import marks for multiple LOs from a single Excel file
     * Excel format: Student Index | LO1 | LO2 | LO3 | ...
     * @param file Excel file with multiple LO columns
     * @param losIds List of LO IDs matching the column order
     * @param batch Batch identifier
     * @param markType Type of marks (FINAL_EXAM or ASSIGNMENT)
     * @return Success message with count
     */
    @Transactional
    public String importMarksBulk(MultipartFile file, String[] losIds, String batch, String markType) {
        try {
            // Validate all LOs exist first
            for (String losId : losIds) {
                if (!losRepository.existsById(losId)) {
                    throw new Exception("Learning Outcome not found: " + losId);
                }
            }

            // Determine mark type (must be final for use in lambda expressions)
            MarkType tempType = MarkType.FINAL_EXAM;
            if (markType != null && !markType.isEmpty()) {
                try {
                    tempType = MarkType.valueOf(markType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    tempType = MarkType.FINAL_EXAM;
                }
            }
            final MarkType finalType = tempType;

            try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
                Sheet sheet = workbook.getSheetAt(0);

                // Validate header row
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    throw new Exception("Excel file must have a header row");
                }

                int totalMarksImported = 0;

                // Process each data row
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // Skip header

                    Cell indexCell = row.getCell(0);
                    if (indexCell == null || indexCell.toString().trim().isEmpty()) {
                        continue; // Skip empty rows
                    }

                    String studentIndex = indexCell.toString().trim();

                    // Find or Create Student
                    Student student = studentRepository.findById(studentIndex)
                            .orElseGet(() -> {
                                Student newStudent = new Student();
                                newStudent.setStudentId(studentIndex);
                                newStudent.setStudentName("Unknown");
                                return studentRepository.save(newStudent);
                            });

                    // Process each LO column
                    for (int i = 0; i < losIds.length; i++) {
                        final String currentLosId = losIds[i]; // Make final for lambda
                        Cell markCell = row.getCell(i + 1); // +1 because column 0 is Student Index

                        if (markCell == null || markCell.toString().trim().isEmpty()) {
                            continue; // Skip empty marks
                        }

                        double score = 0.0;

                        // Parse score
                        if (markCell.getCellType() == CellType.NUMERIC) {
                            score = markCell.getNumericCellValue();
                        } else {
                            String val = markCell.toString().trim().toUpperCase();
                            if (val.equals("AB") || val.equals("MC") || val.equals("N/A")) {
                                continue; // Skip absent/missing marks
                            } else {
                                try {
                                    score = Double.parseDouble(val);
                                } catch (NumberFormatException e) {
                                    continue; // Skip invalid values
                                }
                            }
                        }

                        // Clamp score to 0-100
                        score = Math.max(0.0, Math.min(100.0, score));

                        // Get LO
                        Los los = losRepository.findById(currentLosId)
                                .orElseThrow(() -> new Exception("LO not found: " + currentLosId));

                        // Create and save mark
                        StudentMark mark = new StudentMark();
                        mark.setStudent(student);
                        mark.setScore(score);
                        mark.setLos(los);
                        mark.setBatch(batch);
                        mark.setMarkType(finalType);
                        markRepository.save(mark);

                        totalMarksImported++;
                    }
                }

                return "Successfully imported " + totalMarksImported + " marks for " + losIds.length + " LOs";
            }
        } catch (Exception e) {
            throw new RuntimeException("Error importing marks: " + e.getMessage());
        }
    }

    @Transactional
    public String importMarksOBEFormat(String losId, MultipartFile file, String batch) {
        return importMarksOBEFormat(losId, file, batch, "FINAL_EXAM");
    }

    // Backward compatibility - defaults to null batch
    public String importMarksOBEFormat(String losId, MultipartFile file) {
        return importMarksOBEFormat(losId, file, null);
    }

    // Alias for standard import if needed, or different logic
    public String importStudentMarksFromExcel(String losId, MultipartFile file) {
        return importMarksOBEFormat(losId, file, null);
    }

    // Backward compatibility
    public void importMarks(MultipartFile file, String losId) throws Exception {
        importMarksOBEFormat(losId, file, null);
    }
}
