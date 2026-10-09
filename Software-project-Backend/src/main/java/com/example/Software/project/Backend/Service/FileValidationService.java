package com.example.Software.project.Backend.Service;

import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Bounded workbook opening shared by imports and authorization metadata reads. */
@Service
public class FileValidationService {
    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_EXPANDED_BYTES = 32L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel", "application/octet-stream");

    public static class InvalidUpload extends IllegalArgumentException {
        public InvalidUpload(String message) { super(message); }
    }

    public void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new InvalidUpload("Choose a non-empty Excel file.");
        if (file.getSize() > MAX_FILE_SIZE_BYTES) throw new InvalidUpload("Excel files must be 5 MB or smaller.");
        String name = file.getOriginalFilename();
        if (name == null || !(name.toLowerCase(Locale.ROOT).endsWith(".xlsx") || name.toLowerCase(Locale.ROOT).endsWith(".xls")))
            throw new InvalidUpload("Only .xlsx or .xls files are allowed.");
        String type = file.getContentType();
        if (type != null && !type.isBlank() && !TYPES.contains(type.toLowerCase(Locale.ROOT)))
            throw new InvalidUpload("Unsupported Excel file content type.");
    }

    public Workbook openWorkbook(MultipartFile file) {
        validateExcelFile(file);
        Workbook workbook = null;
        try (InputStream input = FileMagic.prepareToCheckMagic(file.getInputStream())) {
            FileMagic magic = FileMagic.valueOf(input);
            boolean xlsx = file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx");
            if ((xlsx && magic != FileMagic.OOXML) || (!xlsx && magic != FileMagic.OLE2))
                throw new InvalidUpload("File contents do not match the Excel extension.");
            if (xlsx) validateArchive(file);
            workbook = WorkbookFactory.create(input);
            if (workbook.getNumberOfSheets() < 1 || workbook.getNumberOfSheets() > 16)
                throw new InvalidUpload("Excel files must contain between 1 and 16 sheets.");
            int cells = 0;
            for (Sheet sheet : workbook) {
                if (sheet.getLastRowNum() >= 10000) throw new InvalidUpload("Each sheet is limited to 10,000 rows including headers.");
                for (Row row : sheet) {
                    if (row.getLastCellNum() > 256) throw new InvalidUpload("Each sheet is limited to 256 columns.");
                    for (Cell cell : row) {
                        if (++cells > 200000) throw new InvalidUpload("Excel files are limited to 200,000 cells.");
                        if (cell.getCellType() == CellType.FORMULA || cell.getCellType() == CellType.ERROR)
                            throw new InvalidUpload("Remove formula and error cells before uploading; paste their values instead.");
                        if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().length() > 4096)
                            throw new InvalidUpload("Excel cell text is limited to 4,096 characters.");
                    }
                }
            }
            return workbook;
        } catch (Exception e) {
            if (workbook != null) try { workbook.close(); } catch (IOException ignored) { }
            if (e instanceof InvalidUpload invalid) throw invalid;
            throw new InvalidUpload("Cannot read this Excel file. Upload a valid, unencrypted .xls or .xlsx workbook.");
        }
    }

    private void validateArchive(MultipartFile file) throws IOException {
        // Count actual expanded bytes, not attacker-supplied ZIP size declarations. POI's own ZIP protections stay enabled.
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            long expanded = 0; int entries = 0; byte[] buffer = new byte[8192];
            Set<String> names = new HashSet<>();
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                String name = entry.getName().toLowerCase(Locale.ROOT);
                if (++entries > 1000 || !names.add(name)) throw new InvalidUpload("Excel archive has too many or duplicate entries.");
                if (name.contains("vbaproject") || name.startsWith("xl/embeddings/"))
                    throw new InvalidUpload("Excel files with macros or embedded objects are not supported.");
                for (int n; (n = zip.read(buffer)) != -1;) {
                    expanded += n;
                    if (expanded > MAX_EXPANDED_BYTES) throw new InvalidUpload("Expanded Excel content exceeds the 32 MB limit.");
                }
            }
        }
    }

    public static String text(Row row, int column) {
        Cell cell = row == null ? null : row.getCell(column);
        return cell == null ? "" : new DataFormatter(Locale.ROOT).formatCellValue(cell).trim();
    }
    public static void studentHeader(Row row) {
        if (!Set.of("student id", "student index", "index number", "index no").contains(text(row, 0).toLowerCase(Locale.ROOT)))
            throw new InvalidUpload("Missing Student ID/Student Index header. Use the downloaded template.");
    }
    public static void columns(Row row, int count) {
        if (row == null) throw new InvalidUpload("Missing header row. Use the downloaded template.");
        for (int c=0; c<count; c++) if (text(row,c).isBlank()) throw new InvalidUpload("Missing required column " + (c+1) + ". Use the downloaded template.");
        for (int c=count; c<row.getLastCellNum(); c++) if (!text(row,c).isBlank()) throw new InvalidUpload("Unexpected extra column " + (c+1) + ". Use the downloaded template.");
    }
    public static void requireMarkValues(Sheet sheet, int header, int firstMarkColumn, int markColumns) {
        for (Row row : sheet) {
            if (row.getRowNum() <= header) continue;
            for (int c=firstMarkColumn;c<firstMarkColumn+markColumns;c++) {
                if (!text(row,c).isBlank()) return;
            }
        }
        throw new InvalidUpload("Enter at least one mark or attendance marker before uploading. Existing marks were not changed.");
    }

    public static void dataRows(Sheet sheet, int header, int columns) {
        Set<String> ids = new HashSet<>(); int rows=0;
        for (Row row : sheet) {
            if (row.getRowNum()<=header) continue;
            boolean blank=true;
            for (Cell cell:row) if (!text(row,cell.getColumnIndex()).isBlank()) { blank=false; break; }
            if (blank) continue;
            String id=text(row,0);
            if (id.isBlank() || id.length()>100) throw new InvalidUpload("Row " +(row.getRowNum()+1)+ ": a Student ID of at most 100 characters is required.");
            if (!ids.add(id.toLowerCase(Locale.ROOT))) throw new InvalidUpload("Row " +(row.getRowNum()+1)+ ": duplicate Student ID.");
            for (int c=columns;c<row.getLastCellNum();c++) if (!text(row,c).isBlank()) throw new InvalidUpload("Row " +(row.getRowNum()+1)+ ": unexpected extra column.");
            rows++;
        }
        if (rows==0) throw new InvalidUpload("No student rows found. Fill in the downloaded template before uploading.");
    }
}
