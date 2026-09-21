package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Los;
import com.example.Software.project.Backend.Model.MarkType;
import com.example.Software.project.Backend.Model.AssessmentItem;
import com.example.Software.project.Backend.Model.AssessmentTemplate;
import com.example.Software.project.Backend.Model.StudentAssessmentScore;
import com.example.Software.project.Backend.Model.Student;
import com.example.Software.project.Backend.Model.StudentMark;
import com.example.Software.project.Backend.Repository.AssessmentTemplateRepository;
import com.example.Software.project.Backend.Repository.LosRepository;
import com.example.Software.project.Backend.Repository.AssessmentItemRepository;
import com.example.Software.project.Backend.Repository.StudentAssessmentScoreRepository;
import com.example.Software.project.Backend.Repository.StudentMarkRepository;
import com.example.Software.project.Backend.Repository.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ExcelImportService {

    @Autowired
    private StudentMarkRepository markRepository;
    @Autowired
    private LosRepository losRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private AssessmentItemRepository assessmentItemRepository;
    @Autowired
    private AssessmentTemplateRepository assessmentTemplateRepository;
    @Autowired
    private StudentAssessmentScoreRepository studentAssessmentScoreRepository;
    @Autowired
    private ObjectMapper objectMapper;

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
    /** Read the METADATA sheet from an uploaded Excel and return key→value map. */
    public Map<String, String> readMetadata(MultipartFile file) {
        Map<String, String> meta = new HashMap<>();
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheet("METADATA");
            if (sheet == null) return meta;
            for (Row row : sheet) {
                Cell key = row.getCell(0);
                Cell val = row.getCell(1);
                if (key != null && val != null) {
                    meta.put(key.toString().trim(), val.toString().trim());
                }
            }
        } catch (Exception ignored) {}
        return meta;
    }

    @Transactional
    public String importMarksBulk(MultipartFile file, String[] losIds, String batch, String markType) {
        return importMarksBulk(file, losIds, batch, markType, null, null);
    }

    @Transactional
    public String importMarksBulk(MultipartFile file, String[] losIds, String batch, String markType, String assignmentLabel) {
        return importMarksBulk(file, losIds, batch, markType, assignmentLabel, null);
    }

    @Transactional
    public String importMarksBulk(MultipartFile file, String[] losIds, String batch, String markType,
                                  String assignmentLabel, Map<String, Double> perLoMaxMarks) {
        try {
            for (String losId : losIds) {
                if (!losRepository.existsById(losId)) throw new Exception("Learning Outcome not found: " + losId);
            }

            MarkType tempType = MarkType.FINAL_EXAM;
            if (markType != null && !markType.isEmpty()) {
                try { tempType = MarkType.valueOf(markType.toUpperCase().replace(" ","_")); } catch (IllegalArgumentException ignored) {}
            }
            final MarkType finalType = tempType;

            try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
                // Find the data sheet (skip METADATA, Instructions)
                Sheet sheet = null;
                for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
                    String sname = workbook.getSheetName(si).toUpperCase();
                    if (!sname.equals("METADATA") && !sname.equals("INSTRUCTIONS")) { sheet = workbook.getSheetAt(si); break; }
                }
                if (sheet == null) sheet = workbook.getSheetAt(0);

                // Find header row — may be row 0 (old) or row 1 (new with title)
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) throw new Exception("Excel file must have a header row");
                // If row 0 col 0 looks like a title (not "Student Index"), shift to row 1
                int headerRowIdx = 0;
                String firstCell = headerRow.getCell(0) != null ? headerRow.getCell(0).toString().trim() : "";
                if (!firstCell.toLowerCase().contains("student") && !firstCell.toLowerCase().contains("index")) {
                    headerRowIdx = 1;
                    headerRow = sheet.getRow(1);
                }
                if (headerRow == null) throw new Exception("Could not find header row in Excel file");

                // Build per-LO max marks from header if not provided
                Map<String, Double> effectiveLoMax = new java.util.LinkedHashMap<>();
                if (perLoMaxMarks != null && !perLoMaxMarks.isEmpty()) {
                    effectiveLoMax.putAll(perLoMaxMarks);
                } else {
                    // Try to parse max from header: "LO1 (max=50)"
                    for (int i = 0; i < losIds.length; i++) {
                        Cell hCell = headerRow.getCell(i + 1);
                        double mx = 100.0;
                        if (hCell != null) {
                            String hVal = hCell.toString();
                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("max=([0-9.]+)").matcher(hVal);
                            if (m.find()) { try { mx = Double.parseDouble(m.group(1)); } catch (Exception ignored) {} }
                        }
                        effectiveLoMax.put(losIds[i], mx);
                    }
                }

                // Validate all marks first (collect errors, reject whole file if any)
                List<String> errors = new java.util.ArrayList<>();
                for (Row row : sheet) {
                    if (row.getRowNum() <= headerRowIdx) continue;
                    Cell idxCell = row.getCell(0);
                    if (idxCell == null || idxCell.toString().trim().isEmpty()) continue;
                    String studentIndex = idxCell.toString().trim();
                    for (int i = 0; i < losIds.length; i++) {
                        Cell markCell = row.getCell(i + 1);
                        if (markCell == null || markCell.toString().trim().isEmpty()) continue;
                        String raw = markCell.toString().trim().toUpperCase();
                        if (raw.equals("AB") || raw.equals("MC") || raw.equals("N/A")) continue;
                        double score;
                        try { score = Double.parseDouble(raw); } catch (NumberFormatException e) { continue; }
                        double maxMark = effectiveLoMax.getOrDefault(losIds[i], 100.0);
                        if (score < 0 || score > maxMark) {
                            errors.add("Row " + (row.getRowNum() + 1) + ", Student " + studentIndex +
                                       ", " + losIds[i] + ": score " + score + " out of range [0, " + (int)maxMark + "]");
                        }
                    }
                }
                if (!errors.isEmpty()) {
                    throw new Exception("Validation failed — " + errors.size() + " error(s):\n" + String.join("\n", errors));
                }

                // Delete existing marks for this exact assignment
                for (String losId : losIds) {
                    if (assignmentLabel != null && !assignmentLabel.isBlank()) {
                        markRepository.deleteByLos_IdAndBatchAndMarkTypeAndAssignmentLabel(losId, batch, finalType, assignmentLabel);
                    } else {
                        markRepository.deleteByLos_IdAndBatch(losId, batch);
                    }
                }

                // Create/update AssessmentTemplate + AssessmentItems for per-LO max marks
                if (!effectiveLoMax.isEmpty()) {
                    String tmplBatch = batch != null ? batch : "batch";
                    String tmplType = markType != null ? markType.toUpperCase() : "FINAL_EXAM";
                    String tmplLabel = assignmentLabel != null ? assignmentLabel : "";
                    String tmplId = "lo_" + tmplBatch + "_" + tmplType + (tmplLabel.isEmpty() ? "" : "_" + tmplLabel.replaceAll("[^a-zA-Z0-9]", "_"));
                    // Delete old template+items for this id if re-uploading
                    if (assessmentTemplateRepository.existsById(tmplId)) {
                        assessmentTemplateRepository.deleteById(tmplId);
                    }
                    AssessmentTemplate tmpl = new AssessmentTemplate();
                    tmpl.setId(tmplId);
                    tmpl.setBatch(tmplBatch);
                    tmpl.setMarkType(tmplType);
                    tmpl.setAssignmentLabel(tmplLabel.isEmpty() ? null : tmplLabel);
                    tmpl.setName(tmplBatch + "_" + tmplType + (tmplLabel.isEmpty() ? "" : "_" + tmplLabel) + "_lo_wise");
                    tmpl = assessmentTemplateRepository.save(tmpl);
                    for (int i = 0; i < losIds.length; i++) {
                        String losId = losIds[i];
                        Los los = losRepository.findById(losId).orElse(null);
                        if (los == null) continue;
                        double mx = effectiveLoMax.getOrDefault(losId, 100.0);
                        AssessmentItem item = new AssessmentItem();
                        item.setAssessmentTemplate(tmpl);
                        item.setLos(los);
                        item.setQuestionNumber(1);
                        item.setQuestionLabel("LO" + (i + 1));
                        item.setMaxMarks(mx);
                        assessmentItemRepository.save(item);
                    }
                }

                // Import marks
                int totalImported = 0;
                for (Row row : sheet) {
                    if (row.getRowNum() <= headerRowIdx) continue;
                    Cell idxCell = row.getCell(0);
                    if (idxCell == null || idxCell.toString().trim().isEmpty()) continue;
                    String studentIndex = idxCell.toString().trim();
                    final Student student = studentRepository.findById(studentIndex).orElseGet(() -> {
                        Student s = new Student(); s.setStudentId(studentIndex); s.setStudentName("Unknown");
                        return studentRepository.save(s);
                    });
                    for (int i = 0; i < losIds.length; i++) {
                        final String losId = losIds[i];
                        Cell markCell = row.getCell(i + 1);
                        if (markCell == null || markCell.toString().trim().isEmpty()) continue;
                        String raw = markCell.toString().trim().toUpperCase();
                        if (raw.equals("AB") || raw.equals("MC") || raw.equals("N/A")) continue;
                        double score;
                        try { score = Double.parseDouble(raw); } catch (NumberFormatException e) { continue; }
                        Los los = losRepository.findById(losId).orElseThrow(() -> new Exception("LO not found: " + losId));
                        StudentMark mark = new StudentMark();
                        mark.setStudent(student); mark.setScore(score); mark.setLos(los);
                        mark.setBatch(batch); mark.setMarkType(finalType); mark.setAssignmentLabel(assignmentLabel);
                        markRepository.save(mark);
                        totalImported++;
                    }
                }
                return "Successfully imported " + totalImported + " LO marks from " + losIds.length + " LOs"
                    + (assignmentLabel != null ? " for " + assignmentLabel : "");
            }
        } catch (RuntimeException e) { throw e; }
          catch (Exception e) { throw new RuntimeException(e.getMessage(), e); }
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

    /**
     * Import question-wise marks using an assessment template.
     * Expected Excel layout: Student ID | Student Name | Q1 | Q2 | ...
     */
    @Transactional
    public String importQuestionWiseMarks(MultipartFile file, String templateId, String batch, String markType) {
        return importQuestionWiseMarks(file, templateId, batch, markType, null);
    }

    @Transactional
    public String importQuestionWiseMarks(MultipartFile file, String templateId, String batch, String markType, String assignmentLabel) {
        try {
            if (templateId == null || templateId.trim().isEmpty()) {
                throw new Exception("templateId is required for question-wise import");
            }

            MarkType type = MarkType.FINAL_EXAM;
            if (markType != null && !markType.trim().isEmpty()) {
                try { type = MarkType.valueOf(markType.trim().toUpperCase()); } catch (IllegalArgumentException e) { type = MarkType.FINAL_EXAM; }
            }

            List<AssessmentItem> items = assessmentItemRepository.findByAssessmentTemplate_IdOrderByQuestionNumber(templateId.trim());
            if (items.isEmpty()) {
                throw new Exception("No assessment items found for template: " + templateId);
            }

            // Detect column layout from header row: new templates have Student ID at col 0, Q1 at col 1.
            // Old templates (with Student Name) have Q1 at col 2. Detect by reading header.
            final int[] qColOffset = {1}; // default: Q1 at col 1

            studentAssessmentScoreRepository.deleteByAssessmentItem_AssessmentTemplate_Id(templateId.trim());

            Map<String, Double> totalsByStudentAndLo = new HashMap<>();
            Map<String, Student> studentsById = new HashMap<>();
            List<String> validationErrors = new ArrayList<>();
            int questionScoresSaved = 0;

            try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
                Sheet sheet = workbook.getSheetAt(0);

                // Detect layout from row 2 (header row) col 1
                Row headerRow = sheet.getRow(2);
                if (headerRow != null) {
                    Cell col1Header = headerRow.getCell(1);
                    if (col1Header != null) {
                        String col1Val = col1Header.toString().trim().toLowerCase();
                        if (col1Val.startsWith("student name") || col1Val.equals("name")) {
                            qColOffset[0] = 2; // old format: Student ID | Student Name | Q1...
                        }
                    }
                }

                int dataStartRow = 3;
                for (Row row : sheet) {
                    if (row.getRowNum() < dataStartRow) continue;

                    Cell studentIdCell = row.getCell(0);
                    if (studentIdCell == null || studentIdCell.toString().trim().isEmpty()) continue;

                    String studentId = studentIdCell.toString().trim();

                    // If old format, read student name from col 1
                    String studentName = "Unknown";
                    if (qColOffset[0] == 2) {
                        Cell nameCell = row.getCell(1);
                        if (nameCell != null && !nameCell.toString().trim().isEmpty()) {
                            studentName = nameCell.toString().trim();
                        }
                    }

                    final String finalStudentName = studentName;
                    Student student = studentRepository.findById(studentId).orElseGet(() -> {
                        Student s = new Student();
                        s.setStudentId(studentId);
                        s.setStudentName(finalStudentName.isEmpty() ? "Unknown" : finalStudentName);
                        return studentRepository.save(s);
                    });
                    if ((student.getStudentName() == null || student.getStudentName().equals("Unknown")) && !finalStudentName.isEmpty()) {
                        student.setStudentName(finalStudentName);
                        studentRepository.save(student);
                    }
                    studentsById.put(studentId, student);

                    for (int i = 0; i < items.size(); i++) {
                        AssessmentItem item = items.get(i);
                        Cell markCell = row.getCell(i + qColOffset[0]);
                        if (markCell == null || markCell.toString().trim().isEmpty()) continue;

                        Double score = parseScore(markCell);
                        if (score == null) continue;

                        double maxForItem = item.getMaxMarks() != null ? item.getMaxMarks() : 100.0;
                        // Validate range — collect all errors before rejecting
                        if (score < 0 || score > maxForItem) {
                            validationErrors.add("Row " + (row.getRowNum() + 1) + ", Student " + studentId +
                                ", Q" + (i + 1) + ": score " + score + " is out of range [0, " + maxForItem + "]");
                            continue;
                        }

                        StudentAssessmentScore assessmentScore = new StudentAssessmentScore();
                        assessmentScore.setStudent(student);
                        assessmentScore.setAssessmentItem(item);
                        assessmentScore.setScore(score);
                        studentAssessmentScoreRepository.save(assessmentScore);
                        questionScoresSaved++;

                        String loId = item.getLos() != null ? item.getLos().getId() : null;
                        if (loId != null) {
                            String key = studentId + "::" + loId;
                            totalsByStudentAndLo.put(key, totalsByStudentAndLo.getOrDefault(key, 0.0) + score);
                        }
                    }
                }
            }

            // Reject entire upload if any score was out of range
            if (!validationErrors.isEmpty()) {
                throw new Exception("Upload rejected — invalid marks found:\n" + String.join("\n", validationErrors));
            }

            // Delete only this assignment's LO marks (preserve other assignments)
            List<String> affectedLoIds = new ArrayList<>();
            for (AssessmentItem item : items) {
                if (item.getLos() != null && item.getLos().getId() != null && !affectedLoIds.contains(item.getLos().getId())) {
                    affectedLoIds.add(item.getLos().getId());
                }
            }
            for (String loId : affectedLoIds) {
                if (batch != null && !batch.trim().isEmpty() && assignmentLabel != null && !assignmentLabel.trim().isEmpty()) {
                    markRepository.deleteByLos_IdAndBatchAndMarkTypeAndAssignmentLabel(loId, batch.trim(), type, assignmentLabel.trim());
                } else if (batch != null && !batch.trim().isEmpty()) {
                    markRepository.deleteByLos_IdAndBatch(loId, batch.trim());
                } else {
                    markRepository.deleteByLos_Id(loId);
                }
            }

            int aggregatedMarksSaved = 0;
            for (Map.Entry<String, Double> entry : totalsByStudentAndLo.entrySet()) {
                String[] parts = entry.getKey().split("::", 2);
                if (parts.length != 2) continue;
                String studentId = parts[0];
                String loId = parts[1];

                Student student = studentsById.get(studentId);
                if (student == null) student = studentRepository.findById(studentId).orElse(null);
                Los los = losRepository.findById(loId).orElse(null);
                if (student == null || los == null) continue;

                StudentMark mark = new StudentMark();
                mark.setStudent(student);
                mark.setLos(los);
                mark.setScore(entry.getValue());
                mark.setBatch(batch);
                mark.setMarkType(type);
                mark.setAssignmentLabel(assignmentLabel);
                markRepository.save(mark);
                aggregatedMarksSaved++;
            }

            return "Successfully imported " + questionScoresSaved + " question scores and " + aggregatedMarksSaved + " LO marks for " + (assignmentLabel != null ? assignmentLabel : templateId);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Double parseScore(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        String val = cell.toString().trim().toUpperCase();
        if (val.isEmpty() || val.equals("N/A") || val.equals("AB") || val.equals("MC")) {
            return null;
        }
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
