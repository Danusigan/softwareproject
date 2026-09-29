package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module; // disambiguate from java.lang.Module
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Both repositories here delete rows that something else is about to write over or point at, so
 * the delete has to reach the database before the statements that follow it - not sit in
 * Hibernate's action queue until flush, which is all a derived {@code deleteBy...} would do.
 * These are integration tests against a real schema on purpose: a mocked repository happily
 * reports the delete was "called" while the database still rejects what comes next.
 *
 * Flyway migrations are MySQL-flavored and not meant for plain embedded H2 - disabled here so
 * Hibernate builds the schema (including the unique constraint and foreign keys these tests
 * depend on) straight from the entity mappings.
 */
@DataJpaTest(properties = {"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.org.springframework=WARN", "logging.level.org.hibernate=WARN"}, showSql = false)
class DeleteOrderingRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired StudentPoCreditRepository studentPoCreditRepository;
    @Autowired StudentAssessmentScoreRepository studentAssessmentScoreRepository;
    @Autowired AssessmentTemplateRepository assessmentTemplateRepository;
    @Autowired AssessmentItemRepository assessmentItemRepository;

    @Test
    @DisplayName("Recalculating a module/batch/markType overwrites its saved PO credits instead of colliding with them")
    void recalculationOverwritesSavedCredits() {
        Module module = new Module();
        module.setModuleId("EC4356");
        module.setModuleName("Networking");
        em.persist(module);
        Student student = new Student("EG/2025/7888", "A Student", null);
        em.persist(student);
        ProgramOutcome po = new ProgramOutcome("PO1", "PO1", "Knowledge", "Knowledge");
        em.persist(po);
        em.flush();

        studentPoCreditRepository.save(credit(student, po, module, 3, 5));
        em.flush();
        em.clear();

        // What a recalculation does: clear this module/batch's previous result, then save the
        // new one. The new row has the same (student, po, module, batch) as the old, so if the
        // delete hasn't actually run yet, uk_student_po_credit rejects it.
        studentPoCreditRepository.deleteByModule_ModuleIdAndBatch("EC4356", "24");
        studentPoCreditRepository.save(credit(em.find(Student.class, "EG/2025/7888"),
                em.find(ProgramOutcome.class, "PO1"), em.find(Module.class, "EC4356"), 4, 5));
        em.flush();
        em.clear();

        List<StudentPoCredit> rows = studentPoCreditRepository.findByStudent_StudentId("EG/2025/7888");
        assertEquals(1, rows.size(), "The recalculation must replace the previous save, not stack on top of it");
        assertEquals(4, rows.get(0).getCreditsEarned(), "The surviving row must be the recalculated one");
    }

    @Test
    @DisplayName("Deleting an assessment template clears its students' scores first, so the item foreign key doesn't block it")
    void deletingTemplateClearsScoresFirst() {
        Module module = new Module();
        module.setModuleId("EC4356");
        module.setModuleName("Networking");
        em.persist(module);
        Los lo = new Los();
        lo.setId("LO1");
        lo.setModule(module);
        em.persist(lo);
        Student student = new Student("EG/2025/7888", "A Student", null);
        em.persist(student);
        AssessmentTemplate template = new AssessmentTemplate();
        template.setId("A1");
        template.setBatch("26");
        template.setModule(module);
        template.setMarkType("ASSIGNMENT");
        template.setAssignmentLabel("Assignment 1");
        em.persist(template);
        AssessmentItem item = new AssessmentItem();
        item.setLos(lo);
        item.setQuestionNumber(1);
        item.setQuestionLabel("Q1");
        item.setMaxMarks(10.0);
        item.setAssessmentTemplate(template);
        em.persist(item);
        StudentAssessmentScore score = new StudentAssessmentScore();
        score.setStudent(student);
        score.setAssessmentItem(item);
        score.setScore(8.0);
        em.persist(score);
        em.flush();
        em.clear();

        // The order OBEController.deleteTemplateWithScores uses: scores, then the template
        // (whose cascade takes the items with it).
        studentAssessmentScoreRepository.deleteByAssessmentItem_AssessmentTemplate_Id("A1");
        assessmentTemplateRepository.delete(assessmentTemplateRepository.findById("A1").orElseThrow());
        em.flush();
        em.clear();

        assertTrue(assessmentTemplateRepository.findById("A1").isEmpty(), "Template should be gone");
        assertTrue(assessmentItemRepository.findByAssessmentTemplate_Id("A1").isEmpty(), "Its items should be gone with it");
        assertEquals(0, studentAssessmentScoreRepository.count(), "Its students' scores should be gone too");
    }

    private StudentPoCredit credit(Student student, ProgramOutcome po, Module module, int earned, int max) {
        StudentPoCredit c = new StudentPoCredit();
        c.setStudent(student);
        c.setProgramOutcome(po);
        c.setModule(module);
        c.setBatch("24");
        c.setCreditsEarned(earned);
        c.setMaxCredits(max);
        c.setThreshold(50);
        return c;
    }
}
