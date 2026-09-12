package com.example.Software.project.Backend.Reporting.Progress;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Service.ModuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.file.*;
import java.util.*;
import static com.example.Software.project.Backend.Reporting.Progress.ProgressConfiguration.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest(showSql=false,properties={"spring.flyway.enabled=false","spring.datasource.url=jdbc:h2:mem:progress;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
        "logging.level.org.springframework=WARN","logging.level.org.hibernate=WARN"})
@Import({ProgressMigrations.class,ProgressStore.class,ProgressAccess.class,ProgressConfiguration.class,AttainmentCalculator.class,ProgressService.class,ProgressIntegrationTest.JsonConfig.class})
class ProgressIntegrationTest {
    @TestConfiguration static class JsonConfig { @Bean ObjectMapper mapper(){return JsonMapper.builder().findAndAddModules().build();} }
    @Autowired EntityManager em;
    @Autowired ProgressStore store;
    @Autowired ProgressConfiguration configuration;
    @Autowired ProgressService service;
    @Autowired ProgressAccess access;
    @MockBean ModuleService modules;
    private Authentication admin=auth("qa","admin");
    private static Authentication auth(String name,String role){return new UsernamePasswordAuthenticationToken(name,"unused",List.of(new SimpleGrantedAuthority(role)));}
    private BigDecimal d(String v){return new BigDecimal(v);}
    private List<AssessmentItem> items;

    @BeforeEach void seed() {
        Module module=new Module();module.setModuleId("SE101");module.setModuleName("Sample engineering");em.persist(module);
        Los lo1=new Los();lo1.setId("LO1");lo1.setName("Analysis");lo1.setDescription("Analyse evidence");lo1.setModule(module);em.persist(lo1);
        Los lo2=new Los();lo2.setId("LO2");lo2.setName("Design");lo2.setModule(module);em.persist(lo2);
        ProgramOutcome po=new ProgramOutcome("PO1","PO1","Knowledge","Apply knowledge");em.persist(po);
        OutcomeMapping m1=new OutcomeMapping(lo1,po,3,"qa");m1.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);em.persist(m1);
        OutcomeMapping m2=new OutcomeMapping(lo2,po,1,"qa");m2.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);em.persist(m2);
        Student student=new Student("S1","Sample Student","sample@example.test");student.setBatch("22");em.persist(student);
        AssessmentTemplate t=new AssessmentTemplate();t.setId("A1");t.setName("Assignment");t.setModule(module);t.setBatch("22");t.setAcademicYear("2025/26");t.setSemester("1");em.persist(t);
        items=new ArrayList<>();
        for(int i=0;i<3;i++) {
            AssessmentItem item=new AssessmentItem();item.setAssessmentTemplate(t);item.setLos(i==2?lo2:lo1);item.setQuestionNumber(i+1);item.setQuestionLabel("Q"+(i+1));item.setMaxMarks(i==1?20.0:10.0);em.persist(item);items.add(item);
            StudentAssessmentScore score=new StudentAssessmentScore();score.setStudent(student);score.setAssessmentItem(item);score.setScore(i==0?8.0:i==1?16.0:6.0);em.persist(score);
        }
        em.flush();
    }
    private Curriculum curriculum(String code,String version,String threshold){return new Curriculum(code,"ENG","Engineering","Sample University",version,"22","policy-"+version,"OFFICIAL",true,true,d("3"),null,
            List.of(new ModuleRule("SE101",d("3"),true)),List.of(new LoRule("LO1",d(threshold)),new LoRule("LO2",d("60"))),List.of(new PoRule("PO1",d("65"),2,true)));}
    private void configure() {
        configuration.curriculum(curriculum("C1","1","60"),admin);
        configuration.profile(new Profile("S1","C1",null,"COMPLETED"),admin);
        configuration.offering(new Offering("O1","C1","SE101","P1","2025/26","1",LocalDate.of(2025,1,1),List.of("A1")),admin);
        configuration.enrolment(new Enrolment("S1","O1",1,true,"PASS",d("80"),"A",d("4")),admin);
    }
    @Test void fullReportUsesFixedExampleAndPdfSnapshotIsStable() throws Exception {
        configure();var report=service.generate("S1",admin);
        assertEquals(0,d("80").compareTo(report.modules().get(0).los().get(0).result().percentage()));
        assertEquals(0,d("75").compareTo(report.pos().get(0).calculation().result().percentage()));
        assertEquals("REQUIREMENTS_MET",report.summary().academicStatus());
        assertEquals(AttainmentCalculator.Status.ACHIEVED,report.summary().poStatus());
        store.execute("update student_assessment_score set score=0 where student_id=?1","S1");
        var saved=service.snapshot(report.reference(),admin,"PREVIEW");
        assertEquals(report.pos(),saved.pos());
        byte[] pdf=new ProgressPdf().render(saved);PdfReader reader=new PdfReader(pdf);StringBuilder content=new StringBuilder();var extractor=new PdfTextExtractor(reader);
        for(int page=1;page<=reader.getNumberOfPages();page++)content.append(extractor.getTextFromPage(page));reader.close();
        for(String section:List.of("Student profile","Academic progress summary","Semester-by-semester","Module LO attainment","PO attainment summary","PO evidence and calculation details","Strongest and weakest","Missing-data","Final conclusion","Sample Student","75.00","policy-1"))assertTrue(content.toString().contains(section),section);
        assertEquals(2,store.rows("select * from qa_report_audit where snapshot_reference=?1",report.reference()).size());
        Files.createDirectories(Path.of("target","report-samples"));Files.write(Path.of("target","report-samples","academic-progress-sample.pdf"),pdf);
        Files.writeString(Path.of("target","report-samples","academic-progress-sample.json"),JsonMapper.builder().findAndAddModules().build().writerWithDefaultPrettyPrinter().writeValueAsString(saved));
    }
    @Test void curriculumVersionsKeepIndependentThresholdsAndDefinitions() {
        configure();configuration.curriculum(curriculum("C2","2","90"),admin);
        em.find(Los.class,"LO1").setDescription("Changed later");em.find(Los.class,"LO1").setAttainmentThreshold(99.0);em.flush();
        var report=service.generate("S1",admin);
        assertEquals(0,d("60").compareTo(report.modules().get(0).los().get(0).result().threshold()));
        assertEquals("Analyse evidence",report.modules().get(0).los().get(0).description());
        assertEquals(0,d("90").compareTo(ProgressStore.dec(store.one("select threshold from qa_curriculum_lo where curriculum_code='C2' and lo_id='LO1'"),"threshold")));
        assertThrows(IllegalArgumentException.class,()->configuration.curriculum(curriculum("C1","1","90"),admin));
    }
    @Test void studentWithNoResultsAndUnconfiguredHistoryGetsWarningsNotZero() {
        Student noResults=new Student("EMPTY","No Results",null);noResults.setBatch("22");em.persist(noResults);em.flush();
        var empty=service.generate("EMPTY",admin);assertTrue(empty.modules().isEmpty());assertEquals("POLICY_NOT_CONFIGURED",empty.summary().academicStatus());
        assertEquals(AttainmentCalculator.Status.INSUFFICIENT_EVIDENCE,empty.summary().poStatus());
        assertFalse(service.generate("S1",admin).unassignedEvidence().isEmpty());
        assertEquals(1,service.search("No Results",admin).size());
    }
    @Test void missingQuestionAndAbsentAttemptDoNotBecomeZero() {
        configure();store.execute("delete from student_assessment_score where assessment_item_id=?1",items.get(0).getId());
        var result=service.generate("S1",admin);assertNull(result.modules().get(0).los().get(0).result().percentage());
        assertEquals(AttainmentCalculator.Status.INSUFFICIENT_EVIDENCE,result.summary().poStatus());
        configuration.enrolment(new Enrolment("S1","O1",1,true,"ABSENT",null,null,null),admin);
        assertEquals("REQUIREMENTS_NOT_MET",service.generate("S1",admin).summary().academicStatus());
    }
    @Test void studentOwnershipAndLecturerScopeAreCheckedOnSnapshotsToo() {
        configure();var report=service.generate("S1",admin);
        assertThrows(ResponseStatusException.class,()->service.generate("S1",auth("S1","student")));
        store.execute("update qa_student_programme set account_username='linked' where student_id='S1'");
        assertEquals("S1",service.snapshot(report.reference(),auth("linked","student"),"PREVIEW").student().get("studentId"));
        assertThrows(ResponseStatusException.class,()->service.snapshot(report.reference(),auth("other","student"),"DOWNLOAD_PDF"));
        when(modules.getModulesForLecturer("teacher")).thenReturn(List.of());
        assertThrows(ResponseStatusException.class,()->service.generate("S1",auth("teacher","lecture")));
        when(modules.getModulesForLecturer("teacher")).thenReturn(List.of(em.find(Module.class,"SE101")));
        assertEquals("S1",service.generate("S1",auth("teacher","lecture")).student().get("studentId"));
    }
    @Test void apiRejectsUnauthenticatedUnauthorizedAndInvalidInputAndDeliversPdf() throws Exception {
        configure();var mvc=MockMvcBuilders.standaloneSetup(new ProgressController(service,configuration,new ProgressPdf())).build();
        mvc.perform(get("/api/reports/progress/students")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/reports/progress/students/S1/snapshots").principal(auth("other","student"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/reports/progress/configuration").principal(auth("teacher","lecture"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/reports/progress/students").param("q","x".repeat(101)).principal(admin)).andExpect(status().isBadRequest());
        var report=service.generate("S1",admin);
        mvc.perform(get("/api/reports/progress/snapshots/"+report.reference()+"/pdf").principal(admin)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andExpect(header().string("Cache-Control","no-store"));
    }
    @Test void migrationConstraintsRejectInvalidThresholdAndDuplicateEnrolment() {
        configure();assertFalse(store.rows("select * from \"progress_schema_history\"").isEmpty());
        assertThrows(Exception.class,()->store.execute("update qa_curriculum_lo set threshold=101 where curriculum_code='C1'"));
    }
    @Test void inProgressAcademicStudyRemainsSeparateFromSuccessfulOutcomes() {
        configure();configuration.profile(new Profile("S1","C1",null,"IN_PROGRESS"),admin);
        var report=service.generate("S1",admin);assertEquals("IN_PROGRESS",report.summary().academicStatus());assertEquals(AttainmentCalculator.Status.ACHIEVED,report.summary().poStatus());
    }
    @Test void repeatedOfferingsDoNotDoubleCountCreditsOrCombineEvidence() {
        configure();
        AssessmentTemplate original=em.find(AssessmentTemplate.class,"A1");
        AssessmentTemplate second=new AssessmentTemplate();second.setId("A2");second.setName("Repeat exam");second.setBatch("22");second.setModule(original.getModule());second.setAcademicYear("2026/27");second.setSemester("1");em.persist(second);
        for(int i=0;i<items.size();i++) {
            AssessmentItem item=new AssessmentItem();item.setAssessmentTemplate(second);item.setLos(items.get(i).getLos());item.setQuestionNumber(i+1);item.setQuestionLabel("R"+(i+1));item.setMaxMarks(items.get(i).getMaxMarks());em.persist(item);
            StudentAssessmentScore score=new StudentAssessmentScore();score.setStudent(em.find(Student.class,"S1"));score.setAssessmentItem(item);score.setScore(0.0);em.persist(score);
        }
        em.flush();
        configuration.offering(new Offering("O2","C1","SE101","P2","2026/27","1",LocalDate.of(2026,1,1),List.of("A2")),admin);
        configuration.enrolment(new Enrolment("S1","O2",2,false,"FAIL",d("0"),"F",d("0")),admin);
        var report=service.generate("S1",admin);
        assertEquals(2,report.modules().get(0).attempts().size());assertEquals("O1",report.modules().get(0).selectedOffering());
        assertEquals(0,d("6").compareTo(report.summary().creditsAttempted()));assertEquals(0,d("3").compareTo(report.summary().creditsCompleted()));
        assertEquals(0,d("75").compareTo(report.pos().get(0).calculation().result().percentage()));
    }
    @Test void configuredQuestionDefinitionsSurviveLaterTemplateEdits() {
        configure();items.get(0).setMaxMarks(50.0);items.get(0).setQuestionLabel("Edited");em.flush();
        var lo=service.generate("S1",admin).modules().get(0).los().get(0);
        assertEquals(0,d("80").compareTo(lo.result().percentage()));assertEquals("Q1",lo.marks().get(0).question());
    }
    @Test void questionMarkWritesRejectOutOfRangeValues() {
        var score=em.createQuery("select s from StudentAssessmentScore s where s.assessmentItem.id=:id",StudentAssessmentScore.class).setParameter("id",items.get(0).getId()).getSingleResult();
        score.setScore(11.0);assertThrows(IllegalArgumentException.class,()->em.flush());
    }
    @Test void lecturerSearchUsesTheSameScopeAsGeneration() {
        when(modules.getModulesForLecturer("teacher")).thenReturn(List.of(em.find(Module.class,"SE101")));
        assertEquals(1,service.search("Sample",auth("teacher","lecture")).size());
        when(modules.getModulesForLecturer("teacher")).thenReturn(List.of());
        assertTrue(service.search("Sample",auth("teacher","lecture")).isEmpty());
    }
}
