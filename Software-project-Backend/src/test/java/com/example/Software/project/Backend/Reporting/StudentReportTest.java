package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Service.ModuleService;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudentReportTest {
    private Los lo() {
        Los lo = new Los(); lo.setId("LO1"); lo.setName("Apply analysis"); return lo;
    }
    private AssessmentItem item(long id, double max) {
        AssessmentTemplate template = new AssessmentTemplate();
        template.setId("A1"); template.setName("Assignment 1");
        template.setAssignmentLabel("Assignment 1"); template.setMarkType("ASSIGNMENT");
        template.setBatch("22"); template.setAcademicYear("2025/26"); template.setSemester("1");
        AssessmentItem item = new AssessmentItem(); item.setId(id);
        item.setQuestionLabel("Q" + id); item.setQuestionNumber((int)id);
        item.setMaxMarks(max); item.setLos(lo()); item.setAssessmentTemplate(template);
        return item;
    }
    private StudentAssessmentScore score(AssessmentItem item, Double value) {
        StudentAssessmentScore score = new StudentAssessmentScore();
        score.setAssessmentItem(item); score.setScore(value); return score;
    }
    @Test void sumsMarksRatherThanAveragingUnequalQuestions() {
        var a = item(1, 10); var b = item(2, 90);
        var report = StudentReportService.calculate(lo(), List.of(a,b),
                List.of(score(a,10.0),score(b,40.0)), List.of(), 50);
        assertEquals(50.0, report.percentage());
        assertEquals("Achieved", report.status());
        assertEquals(0.0, report.margin());
    }
    @Test void belowThresholdAndZeroAreRealResults() {
        var a = item(1,10);
        var result = StudentReportService.calculate(lo(), List.of(a), List.of(score(a,0.0)), List.of(),50);
        assertEquals("Below threshold",result.status());
        assertEquals(-50.0,result.margin());
    }
    @Test void missingQuestionsArePendingNotFailures() {
        var a=item(1,10); var b=item(2,10);
        var result=StudentReportService.calculate(lo(),List.of(a,b),List.of(score(a,10.0)),List.of(),50);
        assertNull(result.percentage()); assertEquals("Pending",result.status());
        assertNull(result.marks().get(1).score());
    }
    @Test void invalidMarksAndMaximaCannotProduceAchievement() {
        var a=item(1,0);
        assertNull(StudentReportService.calculate(lo(),List.of(a),List.of(score(a,0.0)),List.of(),50).percentage());
        a.setMaxMarks(10.0);
        assertNull(StudentReportService.calculate(lo(),List.of(a),List.of(score(a,11.0)),List.of(),50).percentage());
        assertNull(StudentReportService.calculate(lo(),List.of(a),List.of(score(a,Double.NaN)),List.of(),50).percentage());
    }
    @Test void legacyMirrorIsDisplayedButNotDoubleCounted() {
        var a=item(1,10); var legacy=new StudentMark();
        legacy.setScore(5.0); legacy.setMarkType(MarkType.ASSIGNMENT); legacy.setAssignmentLabel("Assignment 1");
        var result=StudentReportService.calculate(lo(),List.of(a),List.of(score(a,5.0)),List.of(legacy),50);
        assertEquals(50.0,result.percentage()); assertEquals(2,result.marks().size());
        assertTrue(result.marks().get(1).source().contains("reference only"));
    }
    @Test void legacyWithoutMaximumIsPending() {
        var mark=new StudentMark(); mark.setScore(70.0);
        var result=StudentReportService.calculate(lo(),List.of(),List.of(),List.of(mark),50);
        assertEquals("Pending",result.status()); assertNull(result.percentage());
    }
    @Test void emptyLoIsNotAssessed() {
        assertEquals("Not assessed",StudentReportService.calculate(lo(),List.of(),List.of(),List.of(),50).status());
    }
    @Test void studentReportRespectsLecturerModuleVisibility() {
        var repo=mock(StudentReportRepository.class); var modules=mock(ModuleService.class);
        var module=new Module(); module.setModuleId("SE101");
        var lo=lo(); lo.setModule(module);
        var mark=new StudentMark(); mark.setLos(lo);
        when(repo.legacy("S1","22")).thenReturn(List.of(mark));
        when(repo.scores("S1","22")).thenReturn(List.of());
        when(modules.getModulesForLecturer("staff")).thenReturn(List.of());
        var service=new StudentReportService(repo,modules);
        var error=assertThrows(ResponseStatusException.class,()->service.generate("S1","22",50,"lecture","staff"));
        assertEquals(404,error.getStatusCode().value());
        verify(repo,never()).student(anyString());
    }
    @Test void validatesThresholdAndRole() {
        var service=new StudentReportService(mock(StudentReportRepository.class),mock(ModuleService.class));
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.generate("S1","22",Double.NaN,"admin","a")).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->service.students("22","student","s")).getStatusCode().value());
    }
    @Test void controllerRejectsMissingAndExpiredTokens() throws Exception {
        var service=mock(StudentReportService.class); var jwt=mock(JwtUtil.class);
        var mvc=MockMvcBuilders.standaloneSetup(new StudentReportController(service,new StudentReportPdf(),jwt)).build();
        mvc.perform(get("/api/reports/students").param("batch","22")).andExpect(status().isUnauthorized());
        when(jwt.extractUsername("expired")).thenThrow(new IllegalArgumentException("expired"));
        mvc.perform(get("/api/reports/students").param("batch","22").header("Authorization","Bearer expired"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void pdfEndpointReturnsAttachmentAndNoStore() throws Exception {
        var service=mock(StudentReportService.class); var jwt=mock(JwtUtil.class);
        when(jwt.extractUsername("valid")).thenReturn("staff");
        when(jwt.validateToken("valid","staff")).thenReturn(true);
        when(jwt.extractRole("valid")).thenReturn("admin");
        var report=new StudentReport("S1","Sample",null,null,"22",Instant.now(),50,
                "Visible modules only",List.of(),List.of());
        when(service.generate("S1","22",50,"admin","staff")).thenReturn(report);
        var mvc=MockMvcBuilders.standaloneSetup(new StudentReportController(service,new StudentReportPdf(),jwt)).build();
        mvc.perform(get("/api/reports/students/individual").param("studentId","S1").param("batch","22")
                .param("format","pdf").header("Authorization","Bearer valid"))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(header().string("Content-Disposition","attachment; filename=\"student-report-S1.pdf\""))
                .andExpect(result -> assertTrue(new String(result.getResponse().getContentAsByteArray(),
                        java.nio.charset.StandardCharsets.ISO_8859_1).startsWith("%PDF-")));
    }
    @Test void pdfContainsStudentDetailsAndPaginatedMarks() throws Exception {
        var a=item(1,10);
        var result=StudentReportService.calculate(lo(),List.of(a),List.of(score(a,7.0)),List.of(),50);
        var los=new ArrayList<StudentReport.LoResult>();
        for(int i=0;i<30;i++) los.add(result);
        var report=new StudentReport("SAMPLE001","Sample Student","sample@example.test","2025/26","22",
                Instant.parse("2026-09-10T00:00:00Z"),50,"Sample data only.",
                List.of(new StudentReport.ModuleResult("SE101","Software Engineering",los)),
                List.of("LO score = mapped marks / maximum marks x 100."));
        byte[] bytes=new StudentReportPdf().render(report);
        var reader=new PdfReader(bytes);
        assertTrue(reader.getNumberOfPages()>1);
        var extractor=new PdfTextExtractor(reader);
        String first=extractor.getTextFromPage(1);
        assertTrue(first.contains("SAMPLE001"));
        assertTrue(first.contains("Sample Student"));
        assertTrue(first.contains("Achieved"));
        assertTrue(first.contains("70.00"));
        assertTrue(extractor.getTextFromPage(reader.getNumberOfPages()).contains("Calculation and interpretation"));
        reader.close();
        Files.createDirectories(Path.of("target","report-samples"));
        Files.write(Path.of("target","report-samples","student-report-sample.pdf"),bytes);
    }
}
