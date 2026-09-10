package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Model.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class BatchReportRepository {
    private final EntityManager em;
    public BatchReportRepository(EntityManager em) { this.em = em; }

    public List<AssessmentItem> items(String batch, List<String> modules) {
        return em.createQuery("select i from AssessmentItem i join fetch i.los l " +
                "join fetch l.module m join fetch i.assessmentTemplate t " +
                "where t.batch = :batch and m.moduleId in :modules order by t.id, i.questionNumber", AssessmentItem.class)
                .setParameter("batch", batch).setParameter("modules", modules).getResultList();
    }
    public List<StudentAssessmentScore> scores(String batch, List<String> modules) {
        return em.createQuery("select s from StudentAssessmentScore s join fetch s.student " +
                "join fetch s.assessmentItem i join fetch i.los l join fetch l.module m " +
                "join fetch i.assessmentTemplate t where t.batch = :batch and m.moduleId in :modules",
                StudentAssessmentScore.class).setParameter("batch", batch).setParameter("modules", modules).getResultList();
    }
    public List<StudentMark> legacy(String batch, List<String> modules) {
        return em.createQuery("select s from StudentMark s join fetch s.student join fetch s.los l " +
                "join fetch l.module m where s.batch = :batch and m.moduleId in :modules", StudentMark.class)
                .setParameter("batch", batch).setParameter("modules", modules).getResultList();
    }
    public List<Los> los(List<String> modules) {
        return em.createQuery("select l from Los l join fetch l.module m where m.moduleId in :modules order by l.id", Los.class)
                .setParameter("modules", modules).getResultList();
    }
    public List<OutcomeMapping> mappings(List<String> modules) {
        return em.createQuery("select o from OutcomeMapping o join fetch o.learningOutcome l " +
                "join fetch l.module m join fetch o.programOutcome p where m.moduleId in :modules " +
                "and o.status = :approved and o.weight > 0 and p.isActive = true", OutcomeMapping.class)
                .setParameter("modules", modules).setParameter("approved", OutcomeMapping.ApprovalStatus.APPROVED).getResultList();
    }
    public List<ProgramOutcome> pos() {
        return em.createQuery("select p from ProgramOutcome p where p.isActive = true order by p.code", ProgramOutcome.class).getResultList();
    }
    public List<String> batchModules(String batch, List<String> modules) {
        return em.createQuery("select m.moduleId from Module m where m.moduleId in :modules and (" +
                "exists (select l.id from Los l where l.module = m and l.batch = :batch) or " +
                "exists (select s.id from StudentMark s where s.los.module = m and s.batch = :batch) or " +
                "exists (select i.id from AssessmentItem i where i.los.module = m and i.assessmentTemplate.batch = :batch))",
                String.class).setParameter("batch", batch).setParameter("modules", modules).getResultList();
    }
}
