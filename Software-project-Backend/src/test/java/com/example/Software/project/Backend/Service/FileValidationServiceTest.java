package com.example.Software.project.Backend.Service;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import java.io.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class FileValidationServiceTest {
    FileValidationService validator = new FileValidationService();
    MockMultipartFile file(String name, byte[] bytes) { return new MockMultipartFile("file", name, "application/octet-stream", bytes); }
    byte[] bytes(Workbook wb) throws Exception { ByteArrayOutputStream out=new ByteArrayOutputStream();wb.write(out);return out.toByteArray(); }
    @Test void acceptsBothRealWorkbookFormatsAndUppercaseExtensions() throws Exception {
        try(Workbook x=new XSSFWorkbook();Workbook h=new HSSFWorkbook()) {
            x.createSheet("Data");h.createSheet("Data");
            try(Workbook accepted=validator.openWorkbook(file("DATA.XLSX",bytes(x)))) {assertEquals(1,accepted.getNumberOfSheets());}
            try(Workbook accepted=validator.openWorkbook(file("DATA.XLS",bytes(h)))) {assertEquals(1,accepted.getNumberOfSheets());}
            assertThrows(FileValidationService.InvalidUpload.class,()->validator.openWorkbook(file("wrong.xls",bytes(x))));
            assertThrows(FileValidationService.InvalidUpload.class,()->validator.openWorkbook(file("wrong.xlsx",bytes(h))));
        }
    }
    @ParameterizedTest @ValueSource(strings={"file.xlsx","file.xls","file.csv","file.xlsx.exe"})
    void rejectsSpoofedOrUnsupportedFiles(String name) { assertThrows(FileValidationService.InvalidUpload.class,()->validator.openWorkbook(file(name,"not Excel".getBytes()))); }
    @Test void rejectsEmptyOversizeAndBadMime() {
        assertThrows(FileValidationService.InvalidUpload.class,()->validator.openWorkbook(file("file.xlsx",new byte[0])));
        assertThrows(FileValidationService.InvalidUpload.class,()->validator.openWorkbook(file("file.xlsx",new byte[5*1024*1024+1])));
        assertThrows(FileValidationService.InvalidUpload.class,()->validator.openWorkbook(new MockMultipartFile("file","x.xlsx","text/html",new byte[]{1})));
    }
    @Test void rejectsTruncatedZipWithSafeMessage() {
        var error=assertThrows(FileValidationService.InvalidUpload.class,()->validator.openWorkbook(file("file.xlsx",new byte[]{80,75,3,4,0,0,0,0})));
        assertFalse(error.getMessage().contains("org.apache"));
    }
    @Test void rejectsArchiveExpansionBeforeWorkbookParsing() throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(out)) {zip.putNextEntry(new ZipEntry("xl/padding.xml"));byte[] block=new byte[1024*1024];for(int i=0;i<33;i++)zip.write(block);zip.closeEntry();}
        var error=assertThrows(FileValidationService.InvalidUpload.class,()->validator.openWorkbook(file("file.xlsx",out.toByteArray())));
        assertTrue(error.getMessage().contains("32 MB"));
    }
    @ParameterizedTest @ValueSource(strings={"formula","error","row","column","sheets","text"})
    void rejectsUnsafeCellsAndDimensions(String kind) throws Exception {
        try(Workbook wb=new XSSFWorkbook()) {
            Sheet sheet=wb.createSheet("Data");Cell cell=sheet.createRow(0).createCell(0);
            switch(kind) {
                case "formula" -> cell.setCellFormula("1+1");
                case "error" -> cell.setCellErrorValue(FormulaError.DIV0.getCode());
                case "row" -> sheet.createRow(10000).createCell(0).setCellValue("x");
                case "column" -> sheet.getRow(0).createCell(256).setCellValue("x");
                case "sheets" -> {for(int i=0;i<16;i++) wb.createSheet("extra"+i);}
                case "text" -> cell.setCellValue("x".repeat(4097));
            }
            assertThrows(FileValidationService.InvalidUpload.class,()->validator.openWorkbook(file("file.xlsx",bytes(wb))));
        }
    }
}
