package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "logging.level.org.springframework=WARN", "logging.level.org.hibernate=WARN"}, showSql = false)
@Import(StudentReportRepository.class)
class StudentReportRepositoryTest {
    @Autowired TestEntityManager em;
    @Autowired StudentReportRepository repository;
    @Test void filtersActualAssessmentBatchAndAccessibleModules() {
        Module module=new Module(); module.setModuleId("SE101"); module.setModuleName("Engineering"); em.persist(module);
        Los lo=new Los(); lo.setId("LO1"); lo.setModule(module); em.persist(lo);
        Student student=new Student("S1","Sample",null); em.persist(student);
        AssessmentTemplate template=new AssessmentTemplate(); template.setId("A1");
        template.setBatch("22"); template.setModule(module); em.persist(template);
        AssessmentItem item=new AssessmentItem(); item.setQuestionLabel("Q1"); item.setQuestionNumber(1);
        item.setLos(lo); item.setMaxMarks(10.0); item.setAssessmentTemplate(template); em.persist(item);
        StudentAssessmentScore score=new StudentAssessmentScore(); score.setStudent(student);
        score.setAssessmentItem(item); score.setScore(8.0); em.persist(score);
        StudentMark mark=new StudentMark(); mark.setStudent(student); mark.setLos(lo);
        mark.setBatch("22"); mark.setScore(8.0); em.persist(mark);
        em.flush(); em.clear();
        assertEquals(1,repository.students("22",List.of("SE101")).size());
        assertTrue(repository.students("23",List.of("SE101")).isEmpty());
        assertTrue(repository.students("22",List.of("SE102")).isEmpty());
        assertEquals(1,repository.scores("S1","22").size());
        assertTrue(repository.scores("S1","23").isEmpty());
        assertEquals(1,repository.legacy("S1","22").size());
        assertEquals(1,repository.items("SE101","22").size());
        assertEquals(1,repository.los("SE101").size());
        var modules = org.mockito.Mockito.mock(com.example.Software.project.Backend.Service.ModuleService.class);
        org.mockito.Mockito.when(modules.getAllModules()).thenReturn(List.of(module));
        // Match the legacy import mirror to the question template.
        var savedMark = repository.legacy("S1","22").get(0);
        savedMark.setMarkType(MarkType.ASSIGNMENT); savedMark.setAssignmentLabel("Assignment 1");
        var savedTemplate = repository.items("SE101","22").get(0).getAssessmentTemplate();
        savedTemplate.setMarkType("ASSIGNMENT"); savedTemplate.setAssignmentLabel("Assignment 1");
        var report = new StudentReportService(repository, modules).generate("S1","22",50,"admin","staff");
        assertEquals("Sample", report.studentName());
        assertEquals(1, report.modules().size());
        assertEquals(80.0, report.modules().get(0).los().get(0).percentage());
        assertEquals("Achieved", report.modules().get(0).los().get(0).status());
    }
}
