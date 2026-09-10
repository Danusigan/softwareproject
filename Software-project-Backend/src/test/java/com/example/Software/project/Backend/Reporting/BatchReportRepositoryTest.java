package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Service.ModuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties={"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "logging.level.org.springframework=WARN","logging.level.org.hibernate=WARN"},showSql=false)
@Import(BatchReportRepository.class)
class BatchReportRepositoryTest {
    @Autowired TestEntityManager em;
    @Autowired BatchReportRepository repository;
    @Test void queriesAndAssemblyRespectBatchModuleAndApprovedMappingScope() {
        Module m=new Module(); m.setModuleId("SE101"); m.setModuleName("Engineering"); em.persist(m);
        Los lo=new Los(); lo.setId("LO1"); lo.setModule(m); em.persist(lo);
        Student s=new Student("S1","Private Student",null); em.persist(s);
        AssessmentTemplate t=new AssessmentTemplate(); t.setId("A1"); t.setBatch("22");
        t.setModule(m); t.setMarkType("ASSIGNMENT"); t.setAssignmentLabel("Assignment 1"); em.persist(t);
        AssessmentItem i=new AssessmentItem(); i.setLos(lo); i.setQuestionNumber(1); i.setQuestionLabel("Q1");
        i.setMaxMarks(10.0); i.setAssessmentTemplate(t); em.persist(i);
        StudentAssessmentScore score=new StudentAssessmentScore(); score.setStudent(s); score.setAssessmentItem(i); score.setScore(8.0); em.persist(score);
        StudentMark legacy=new StudentMark(); legacy.setStudent(s); legacy.setLos(lo); legacy.setBatch("22");
        legacy.setScore(8.0); legacy.setMarkType(MarkType.ASSIGNMENT); legacy.setAssignmentLabel("Assignment 1"); em.persist(legacy);
        var po=new ProgramOutcome("PO1","PO1","Knowledge","Knowledge"); em.persist(po);
        var approved=new OutcomeMapping(lo,po,3,"staff"); approved.setStatus(OutcomeMapping.ApprovalStatus.APPROVED); em.persist(approved);
        var pending=new OutcomeMapping(lo,po,1,"staff"); em.persist(pending);
        em.flush(); em.clear();
        assertEquals(List.of("SE101"),repository.batchModules("22",List.of("SE101")));
        assertTrue(repository.batchModules("23",List.of("SE101")).isEmpty());
        assertTrue(repository.scores("23",List.of("SE101")).isEmpty());
        assertTrue(repository.scores("22",List.of("SE102")).isEmpty());
        assertEquals(1,repository.mappings(List.of("SE101")).size());
        assertEquals(1,repository.pos().size());
        var modules=mock(ModuleService.class); when(modules.getAllModules()).thenReturn(List.of(m));
        var report=new BatchReportService(repository,modules).generate("22",List.of("SE101"),50,70,70,"admin","staff");
        assertEquals(1,report.studentsWithRecords()); assertEquals(100.0,report.modules().get(0).los().get(0).achievementPercent());
        assertEquals(100.0,report.pos().get(0).attainmentPercent()); assertEquals("Achieved",report.pos().get(0).status());
    }
}
