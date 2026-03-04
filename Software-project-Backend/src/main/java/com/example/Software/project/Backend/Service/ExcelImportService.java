package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Los;
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
    public String importMarksOBEFormat(String losId, MultipartFile file, String batch) {
        try {
            Los los = losRepository.findById(losId)
                    .orElseThrow(() -> new Exception("Learning Outcome not found"));

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
                    markRepository.save(mark);
                    count++;
                }
                return "Successfully imported " + count + " marks.";
            }
        } catch (Exception e) {
            throw new RuntimeException("Error importing marks: " + e.getMessage());
        }
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
