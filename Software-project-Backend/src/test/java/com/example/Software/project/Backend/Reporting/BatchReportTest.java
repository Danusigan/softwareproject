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
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static com.example.Software.project.Backend.Reporting.BatchReport.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BatchReportTest {
    private Los lo(String id) {
        Module m=new Module(); m.setModuleId("SE101"); m.setModuleName("Engineering");
        Los lo=new Los(); lo.setId(id); lo.setName("Analysis " + id); lo.setModule(m); return lo;
    }
    private AssessmentItem item(Los lo, long id) {
        var t=new AssessmentTemplate(); t.setId("A1"); t.setBatch("22");
        t.setName("Assignment 1"); t.setAssignmentLabel("Assignment 1"); t.setMarkType("ASSIGNMENT");
        var i=new AssessmentItem(); i.setId(id); i.setLos(lo); i.setMaxMarks(10.0);
        i.setQuestionLabel("Q"+id); i.setQuestionNumber((int)id); i.setAssessmentTemplate(t); return i;
    }
    private StudentAssessmentScore score(AssessmentItem item,String id,double mark) {
        var score=new StudentAssessmentScore(); score.setStudent(new Student(id,"Private student " + id,null));
        score.setAssessmentItem(item); score.setScore(mark); return score;
    }
    private ProgramOutcome po(String id) { return new ProgramOutcome(id,id,"Engineering knowledge","Knowledge"); }
    private OutcomeMapping mapping(String lo,String po,int weight) {
        var m=new OutcomeMapping(lo(lo),po(po),weight,"staff"); m.setStatus(OutcomeMapping.ApprovalStatus.APPROVED); return m;
    }
    private LoResult metric(String id,double pct,String status) {
        return new LoResult(id,id,4,4,2,2,0,pct,100,70,status);
    }
    private BatchReport batch(List<Los> los,List<AssessmentItem> items,List<StudentAssessmentScore> scores,List<StudentMark> legacy,double target) {
        return BatchReportService.assemble("22",List.of(new ModuleOption("SE101","Engineering")),los,items,scores,legacy,
                List.of(mapping("LO1","PO1",1)),List.of(po("PO1")),50,target,70);
    }

    @Test void loPercentCountsStudentsAndThresholdBoundaryCorrectly() {
        var lo=lo("LO1"); var i=item(lo,1);
        var report=batch(List.of(lo),List.of(i),List.of(score(i,"S1",8),score(i,"S2",5),score(i,"S3",4),score(i,"S4",0)),List.of(),50);
        var result=report.modules().get(0).los().get(0);
        assertEquals(4,result.assessed()); assertEquals(2,result.achieved());
        assertEquals(2,result.belowThreshold()); assertEquals(50.0,result.achievementPercent());
        assertEquals("Achieved",result.status()); assertEquals("Achieved",report.modules().get(0).status());
        assertEquals(4,report.studentsWithRecords());
    }
    @Test void pendingStudentIsExcludedFromRateButBlocksCohortAndPoVerdict() {
        var lo=lo("LO1"); var a=item(lo,1); var b=item(lo,2);
        var report=batch(List.of(lo),List.of(a,b),List.of(score(a,"S1",10),score(b,"S1",10),score(a,"S2",10)),List.of(),70);
        var result=report.modules().get(0).los().get(0);
        assertEquals(100.0,result.achievementPercent()); assertEquals(1,result.assessed());
        assertEquals(1,result.pending()); assertEquals(50.0,result.coveragePercent());
        assertEquals("Pending",result.status()); assertEquals("Pending",report.modules().get(0).status());
        assertEquals("Pending",report.pos().get(0).status());
    }
    @Test void moduleUsesAllDefinedLosAndKeepsUnknownOutcomesPending() {
        var a=lo("LO1"); var b=lo("LO2"); var item=item(a,1);
        var report=batch(List.of(a,b),List.of(item),List.of(score(item,"S1",10)),List.of(),70);
        assertEquals(2,report.modules().get(0).totalLos()); assertEquals(1,report.modules().get(0).achievedLos());
        assertEquals(50.0,report.modules().get(0).achievedLoPercent());
        assertEquals("Pending",report.modules().get(0).status());
        assertEquals("Not assessed",report.modules().get(0).los().get(1).status());
    }
    @Test void questionAndLegacyMirrorNeverDoubleCountStudent() {
        var lo=lo("LO1"); var item=item(lo,1); var score=score(item,"S1",5);
        var mark=new StudentMark(); mark.setStudent(score.getStudent()); mark.setLos(lo);
        mark.setScore(5.0); mark.setMarkType(MarkType.ASSIGNMENT); mark.setAssignmentLabel("Assignment 1");
        var report=batch(List.of(lo),List.of(item),List.of(score),List.of(mark),70);
        assertEquals(1,report.studentsWithRecords()); assertEquals(1,report.modules().get(0).los().get(0).assessed());
        assertEquals(100.0,report.modules().get(0).los().get(0).achievementPercent());
    }
    @Test void emptyCohortRemainsNotAssessedEvenAtZeroTarget() {
        var lo=lo("LO1"); var report=batch(List.of(lo),List.of(item(lo,1)),List.of(),List.of(),0);
        assertNull(report.modules().get(0).los().get(0).achievementPercent());
        assertEquals("Not assessed",report.modules().get(0).status());
    }
    @Test void poUsesOnlyApprovedPositiveWeightsAndKeepsUnmappedNull() {
        var rejected=mapping("LO3","PO1",3); rejected.setStatus(OutcomeMapping.ApprovalStatus.REJECTED);
        var pending=mapping("LO3","PO1",3); pending.setStatus(OutcomeMapping.ApprovalStatus.PENDING);
        var results=BatchReportService.aggregatePos(List.of(po("PO1"),po("PO2")),
                List.of(mapping("LO1","PO1",3),mapping("LO2","PO1",1),mapping("LO3","PO1",0),rejected,pending),
                Map.of("LO1",metric("LO1",100,"Achieved"),"LO2",metric("LO2",0,"Below target"),"LO3",metric("LO3",0,"Pending")),70);
        assertEquals(75.0,results.get(0).attainmentPercent()); assertEquals("Achieved",results.get(0).status());
        assertEquals(2,results.get(0).mappedLos());
        assertEquals("Not mapped",results.get(1).status()); assertNull(results.get(1).attainmentPercent());
    }
    @Test void incompleteAndDuplicateMappingsDoNotSilentlyImprovePo() {
        var results=BatchReportService.aggregatePos(List.of(po("PO1")),
                List.of(mapping("LO1","PO1",3),mapping("LO2","PO1",1)),
                Map.of("LO1",metric("LO1",100,"Achieved"),"LO2",metric("LO2",90,"Pending")),70);
        assertEquals(100.0,results.get(0).attainmentPercent());
        assertEquals("Pending",results.get(0).status()); assertEquals(1,results.get(0).completeLos());
        var duplicate=BatchReportService.aggregatePos(List.of(po("PO1")),
                List.of(mapping("LO1","PO1",1),mapping("LO1","PO1",3)),Map.of("LO1",metric("LO1",100,"Achieved")),70);
        assertEquals("Pending",duplicate.get(0).status()); assertNull(duplicate.get(0).attainmentPercent());
        assertEquals(1,duplicate.get(0).mappedLos());
    }
    @Test void rejectsNonFiniteTargetsAndUnprivilegedRoles() {
        var service=new BatchReportService(mock(BatchReportRepository.class),mock(ModuleService.class));
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.generate("22",null,50,Double.NaN,70,"admin","a")).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->service.options("22","student","s")).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.options(" ","admin","s")).getStatusCode().value());
    }
    @Test void lecturerCannotRequestAnInaccessibleModule() {
        var repository=mock(BatchReportRepository.class); var modules=mock(ModuleService.class);
        var m=lo("LO1").getModule();
        when(modules.getModulesForLecturer("staff")).thenReturn(List.of(m));
        when(repository.batchModules("22",List.of("SE101"))).thenReturn(List.of("SE101"));
        var service=new BatchReportService(repository,modules);
        assertEquals(403,assertThrows(ResponseStatusException.class,()->service.generate("22",List.of("SE102"),50,70,70,"lecture","staff")).getStatusCode().value());
        verify(repository,never()).scores(anyString(),anyList());
    }
    @Test void controllerRequiresValidTokenAndProducesPdfAttachment() throws Exception {
        var service=mock(BatchReportService.class); var jwt=mock(JwtUtil.class);
        var mvc=MockMvcBuilders.standaloneSetup(new BatchReportController(service,new BatchReportPdf(),jwt)).build();
        mvc.perform(get("/api/reports/batches/modules").param("batch","22")).andExpect(status().isUnauthorized());
        when(jwt.extractUsername("ok")).thenReturn("staff"); when(jwt.validateToken("ok","staff")).thenReturn(true); when(jwt.extractRole("ok")).thenReturn("admin");
        var lo=lo("LO1"); var item=item(lo,1);
        var report=batch(List.of(lo),List.of(item),List.of(score(item,"S1",10)),List.of(),70);
        when(service.generate("22",List.of("SE101"),50,70,70,"admin","staff")).thenReturn(report);
        mvc.perform(get("/api/reports/batches/attainment").param("batch","22").param("moduleIds","SE101")
                .param("format","pdf").header("Authorization","Bearer ok"))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(header().string("Content-Disposition","attachment; filename=\"batch-attainment-22.pdf\""));
        mvc.perform(get("/api/reports/batches/attainment").param("batch","22").param("format","xml")
                .header("Authorization","Bearer ok")).andExpect(status().isBadRequest());
    }
    @Test void pdfContainsGraphsTablesAndMultiPageNotesWithoutStudentIdentities() throws Exception {
        List<Los> los=new ArrayList<>(); List<AssessmentItem> items=new ArrayList<>(); List<StudentAssessmentScore> scores=new ArrayList<>();
        for(int i=1;i<=35;i++){var lo=lo("LO"+i); var item=item(lo,i); los.add(lo); items.add(item); scores.add(score(item,"S1",i%2==0?10:0));}
        var report=batch(los,items,scores,List.of(),70);
        byte[] bytes=new BatchReportPdf().render(report);
        assertTrue(new String(bytes,0,5,StandardCharsets.ISO_8859_1).equals("%PDF-"));
        var reader=new PdfReader(bytes); assertTrue(reader.getNumberOfPages()>1);
        var extractor=new PdfTextExtractor(reader); String text="";
        for(int i=1;i<=reader.getNumberOfPages();i++) text+=extractor.getTextFromPage(i);
        assertTrue(text.contains("BATCH ATTAINMENT REPORT")); assertTrue(text.contains("PO1")); assertTrue(text.contains("Below target"));
        assertTrue(text.contains("Calculation and scope notes")); assertFalse(text.contains("Private student S1")); reader.close();
        Files.createDirectories(Path.of("target","report-samples"));
        Files.write(Path.of("target","report-samples","batch-report-sample.pdf"),bytes);
    }
}
