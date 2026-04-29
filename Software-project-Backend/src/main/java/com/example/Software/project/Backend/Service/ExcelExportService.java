package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Los;
import com.example.Software.project.Backend.Model.MarkType;
import com.example.Software.project.Backend.Model.Student;
import com.example.Software.project.Backend.Model.StudentMark;
import com.example.Software.project.Backend.Repository.LosRepository;
import com.example.Software.project.Backend.Repository.StudentMarkRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    @Autowired
    private StudentMarkRepository studentMarkRepository;

    @Autowired
    private LosRepository losRepository;

    /**
     * Generate Excel file with student marks for selected LOs
     * @param losIds List of LO IDs to include
     * @param markType Type of marks (FINAL_EXAM or ASSIGNMENT)
     * @param batch Batch year/identifier
     * @param threshold Pass threshold score (default 50)
     * @return byte array of Excel file
     */
    @Transactional(readOnly = true)
    public byte[] generateMarksExcel(List<String> losIds, String markType, String batch, Integer threshold) throws IOException {
        if (threshold == null) {
            threshold = 50;
        }

        MarkType type = MarkType.valueOf(markType.toUpperCase());

        // Fetch all LOs
        Map<String, Los> losMap = new HashMap<>();
        for (String losId : losIds) {
            Optional<Los> los = losRepository.findById(losId);
            los.ifPresent(l -> losMap.put(losId, l));
        }

        // Get distinct students for these LOs
        List<Student> students = studentMarkRepository
            .findDistinctStudentsByLosIdsAndMarkTypeAndBatch(losIds, type, batch);

        // Get all marks for these LOs
        List<StudentMark> allMarks = studentMarkRepository
            .findByLosIdsAndMarkTypeAndBatch(losIds, type, batch);

        // Group marks by student and LO for quick lookup
        Map<String, Map<String, StudentMark>> marksByStudentAndLo = new HashMap<>();
        for (StudentMark mark : allMarks) {
            String studentId = mark.getStudent().getStudentId();
            String losId = mark.getLos().getId();

            marksByStudentAndLo.computeIfAbsent(studentId, k -> new HashMap<>())
                .put(losId, mark);
        }

        // Create workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Marks Report");

            // Define styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle passStyle = createPassStyle(workbook);
            CellStyle failStyle = createFailStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // Create header row
            Row headerRow = sheet.createRow(0);

            // First column - Index Number
            Cell indexHeader = headerRow.createCell(0);
            indexHeader.setCellValue("Index Number");
            indexHeader.setCellStyle(headerStyle);
            sheet.setColumnWidth(0, 5000);

            // LO columns
            int colIndex = 1;
            for (String losId : losIds) {
                Los los = losMap.get(losId);
                Cell loHeader = headerRow.createCell(colIndex);
                loHeader.setCellValue(los != null ? los.getName() : losId);
                loHeader.setCellStyle(headerStyle);
                sheet.setColumnWidth(colIndex, 4000);
                colIndex++;
            }

            // Add student data rows
            int rowIndex = 1;
            for (Student student : students) {
                Row dataRow = sheet.createRow(rowIndex);

                // Student Index
                Cell studentIdCell = dataRow.createCell(0);
                studentIdCell.setCellValue(student.getStudentId());
                studentIdCell.setCellStyle(dataStyle);

                // Marks for each LO
                int colIdx = 1;
                for (String losId : losIds) {
                    Cell markCell = dataRow.createCell(colIdx);

                    StudentMark mark = marksByStudentAndLo
                        .getOrDefault(student.getStudentId(), new HashMap<>())
                        .get(losId);

                    if (mark != null && mark.getScore() != null) {
                        double score = mark.getScore();
                        String passFailStatus = score >= threshold ? "Pass" : "Fail";

                        markCell.setCellValue(passFailStatus + " (" + String.format("%.2f", score) + ")");
                        markCell.setCellStyle(score >= threshold ? passStyle : failStyle);
                    } else {
                        markCell.setCellValue("N/A");
                        markCell.setCellStyle(dataStyle);
                    }

                    colIdx++;
                }

                rowIndex++;
            }

            // Convert to byte array
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        }
    }

    /**
     * Create header cell style (bold, colored background)
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Create Pass status cell style (green background)
     */
    private CellStyle createPassStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Create Fail status cell style (red background)
     */
    private CellStyle createFailStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Create data cell style (standard formatting)
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Generate empty Excel template for mark upload
     * Users can download this, fill in marks, and re-upload
     * @param losIds List of LO IDs to include in template
     * @return byte array of Excel template file
     */
    public byte[] generateMarkTemplate(List<String> losIds) throws IOException {
        // Fetch all LOs
        Map<String, Los> losMap = new HashMap<>();
        for (String losId : losIds) {
            Optional<Los> los = losRepository.findById(losId);
            los.ifPresent(l -> losMap.put(losId, l));
        }

        // Create workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Mark Template");

            // Define styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle inputStyle = createInputStyle(workbook);

            // Create header row
            Row headerRow = sheet.createRow(0);

            // First column - Student Index
            Cell indexHeader = headerRow.createCell(0);
            indexHeader.setCellValue("Student Index");
            indexHeader.setCellStyle(headerStyle);
            sheet.setColumnWidth(0, 5000);

            // LO columns
            int colIndex = 1;
            for (String losId : losIds) {
                Los los = losMap.get(losId);
                Cell loHeader = headerRow.createCell(colIndex);
                loHeader.setCellValue(los != null ? los.getName() : losId);
                loHeader.setCellStyle(headerStyle);
                sheet.setColumnWidth(colIndex, 4000);
                colIndex++;
            }

            // Add sample rows (10 empty rows for user to fill in)
            for (int rowNum = 1; rowNum <= 10; rowNum++) {
                Row row = sheet.createRow(rowNum);

                // Student index column (user fills this)
                Cell studentCell = row.createCell(0);
                studentCell.setCellStyle(inputStyle);

                // LO mark columns (user fills these with scores 0-100)
                for (int col = 1; col <= losIds.size(); col++) {
                    Cell markCell = row.createCell(col);
                    markCell.setCellStyle(inputStyle);
                    // Set cell format as number with 2 decimal places
                    markCell.setCellValue(""); // Leave empty for user to fill
                }
            }

            // Add instructions sheet
            Sheet instructionsSheet = workbook.createSheet("Instructions");
            instructionsSheet.setColumnWidth(0, 15000);

            int instrRow = 0;

            // Add title
            Row titleRow = instructionsSheet.createRow(instrRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Mark Entry Template - Instructions");
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            instrRow++; // blank row

            // Instructions
            String[] instructions = {
                "1. Fill in the 'Student Index' column with student IDs (e.g., EN001, EN002)",
                "2. Fill in the mark columns with scores between 0 and 100",
                "3. Use decimal values (e.g., 85.50, 92.00)",
                "4. Do NOT modify the header row",
                "5. Do NOT change the column order",
                "6. Do NOT change sheet names",
                "7. Save the file as Excel format (.xlsx)",
                "8. Upload the completed file back to the system"
            };

            for (String instruction : instructions) {
                Row instrRowObj = instructionsSheet.createRow(instrRow++);
                Cell instrCell = instrRowObj.createCell(0);
                instrCell.setCellValue(instruction);
                instrRowObj.setHeightInPoints(20);
            }

            // Convert to byte array
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        }
    }

    /**
     * Generate Excel file with per-student PO attainment credits
     * @param attainmentData Data from POAttainmentService.calculateStudentPOCredits()
     * @return byte array of Excel file
     */
    @SuppressWarnings("unchecked")
    public byte[] generatePOAttainmentExcel(Map<String, Object> attainmentData) throws IOException {
        List<String> poList = (List<String>) attainmentData.get("poList");
        List<Map<String, String>> loList = (List<Map<String, String>>) attainmentData.get("loList");
        if (loList == null) loList = new ArrayList<>();
        
        Map<String, Integer> maxCredits = (Map<String, Integer>) attainmentData.get("maxCredits");
        int totalMaxCredit = (int) attainmentData.get("totalMaxCredit");
        int threshold = (int) attainmentData.get("threshold");
        List<Map<String, Object>> students = (List<Map<String, Object>>) attainmentData.get("students");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PO Attainment");

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle passStyle = createPassStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle failStyle = createFailStyle(workbook);

            // Title row with threshold info
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("PO Attainment & LO Scores Report (Threshold: " + threshold + "%)");
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Max credits row
            Row maxRow = sheet.createRow(1);
            Cell maxLabel = maxRow.createCell(0);
            maxLabel.setCellValue("Max Credit");
            maxLabel.setCellStyle(headerStyle);
            
            int colIdx = 1;
            // Empty cells for LO columns in max credit row
            for (int i = 0; i < loList.size(); i++) {
                Cell cell = maxRow.createCell(colIdx);
                cell.setCellValue("");
                cell.setCellStyle(headerStyle);
                colIdx++;
            }
            
            // PO max credits
            for (String poCode : poList) {
                Cell cell = maxRow.createCell(colIdx);
                cell.setCellValue(maxCredits.getOrDefault(poCode, 0));
                cell.setCellStyle(headerStyle);
                colIdx++;
            }
            Cell maxTotalCell = maxRow.createCell(colIdx);
            maxTotalCell.setCellValue(totalMaxCredit);
            maxTotalCell.setCellStyle(headerStyle);

            // Header row
            Row headerRow = sheet.createRow(2);
            Cell indexHeader = headerRow.createCell(0);
            indexHeader.setCellValue("Student Index");
            indexHeader.setCellStyle(headerStyle);
            sheet.setColumnWidth(0, 5000);

            colIdx = 1;
            // LO Headers
            for (Map<String, String> lo : loList) {
                Cell loHeader = headerRow.createCell(colIdx);
                loHeader.setCellValue(lo.get("name"));
                loHeader.setCellStyle(headerStyle);
                sheet.setColumnWidth(colIdx, 4000);
                colIdx++;
            }
            
            // PO Headers
            for (String poCode : poList) {
                Cell poHeader = headerRow.createCell(colIdx);
                poHeader.setCellValue(poCode);
                poHeader.setCellStyle(headerStyle);
                sheet.setColumnWidth(colIdx, 3500);
                colIdx++;
            }
            Cell totalHeader = headerRow.createCell(colIdx);
            totalHeader.setCellValue("Total Credit");
            totalHeader.setCellStyle(headerStyle);
            sheet.setColumnWidth(colIdx, 4000);

            // Data rows
            int rowIndex = 3;
            for (Map<String, Object> student : students) {
                Row dataRow = sheet.createRow(rowIndex);

                Cell studentIdCell = dataRow.createCell(0);
                studentIdCell.setCellValue((String) student.get("studentId"));
                studentIdCell.setCellStyle(dataStyle);

                int col = 1;
                
                // LO Scores
                Map<String, String> loScoresMap = (Map<String, String>) student.get("loScores");
                for (Map<String, String> lo : loList) {
                    Cell cell = dataRow.createCell(col);
                    String scoreLabel = loScoresMap.getOrDefault(lo.get("id"), "N/A");
                    cell.setCellValue(scoreLabel);
                    
                    if (scoreLabel.startsWith("Pass")) cell.setCellStyle(passStyle);
                    else if (scoreLabel.startsWith("Fail")) cell.setCellStyle(failStyle);
                    else cell.setCellStyle(dataStyle);
                    
                    col++;
                }
                
                // PO Credits
                Map<String, Integer> poCredits = (Map<String, Integer>) student.get("poCredits");
                for (String poCode : poList) {
                    Cell cell = dataRow.createCell(col);
                    int credit = poCredits.getOrDefault(poCode, 0);
                    cell.setCellValue(credit);
                    cell.setCellStyle(credit > 0 ? passStyle : dataStyle);
                    col++;
                }

                Cell totalCell = dataRow.createCell(col);
                totalCell.setCellValue((int) student.get("totalCredit"));
                int totalCredit = (int) student.get("totalCredit");
                totalCell.setCellStyle(totalCredit > 0 ? passStyle : dataStyle);

                rowIndex++;
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        }
    }

    /**
     * Create input cell style for template (light gray background for user input area)
     */
    private CellStyle createInputStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
