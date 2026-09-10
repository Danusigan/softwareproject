package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Model.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class StudentReportRepository {
    private final EntityManager em;
    public StudentReportRepository(EntityManager em) { this.em = em; }

    public Student student(String id) { return em.find(Student.class, id); }

    public List<StudentMark> legacy(String id, String batch) {
        return em.createQuery("select s from StudentMark s join fetch s.los l join fetch l.module " +
                "where s.student.studentId = :id and s.batch = :batch", StudentMark.class)
                .setParameter("id", id).setParameter("batch", batch).getResultList();
    }

    public List<StudentAssessmentScore> scores(String id, String batch) {
        return em.createQuery("select s from StudentAssessmentScore s join fetch s.assessmentItem i " +
                "join fetch i.los l join fetch l.module join fetch i.assessmentTemplate t " +
                "where s.student.studentId = :id and t.batch = :batch", StudentAssessmentScore.class)
                .setParameter("id", id).setParameter("batch", batch).getResultList();
    }

    public List<Los> los(String moduleId) {
        return em.createQuery("select l from Los l where l.module.moduleId = :id order by l.id", Los.class)
                .setParameter("id", moduleId).getResultList();
    }

    public List<AssessmentItem> items(String moduleId, String batch) {
        return em.createQuery("select i from AssessmentItem i join fetch i.los l " +
                "join fetch i.assessmentTemplate t where l.module.moduleId = :id and t.batch = :batch " +
                "order by t.id, i.questionNumber", AssessmentItem.class)
                .setParameter("id", moduleId).setParameter("batch", batch).getResultList();
    }

    public List<Student> students(String batch, List<String> modules) {
        return em.createQuery("select s from Student s where exists " +
                "(select m.id from StudentMark m where m.student = s and m.batch = :batch " +
                "and m.los.module.moduleId in :modules) or exists " +
                "(select q.id from StudentAssessmentScore q where q.student = s " +
                "and q.assessmentItem.assessmentTemplate.batch = :batch " +
                "and q.assessmentItem.los.module.moduleId in :modules) order by s.studentId", Student.class)
                .setParameter("batch", batch).setParameter("modules", modules).getResultList();
    }
}
