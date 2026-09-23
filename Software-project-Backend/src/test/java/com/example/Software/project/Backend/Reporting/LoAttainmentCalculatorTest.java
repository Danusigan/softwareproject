package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LoAttainmentCalculatorTest {
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
        var report = LoAttainmentCalculator.calculate(lo(), List.of(a,b),
                List.of(score(a,10.0),score(b,40.0)), List.of(), 50);
        assertEquals(50.0, report.percentage());
        assertEquals("Achieved", report.status());
        assertEquals(0.0, report.margin());
    }
    @Test void belowThresholdAndZeroAreRealResults() {
        var a = item(1,10);
        var result = LoAttainmentCalculator.calculate(lo(), List.of(a), List.of(score(a,0.0)), List.of(),50);
        assertEquals("Below threshold",result.status());
        assertEquals(-50.0,result.margin());
    }
    @Test void missingQuestionsArePendingNotFailures() {
        var a=item(1,10); var b=item(2,10);
        var result=LoAttainmentCalculator.calculate(lo(),List.of(a,b),List.of(score(a,10.0)),List.of(),50);
        assertNull(result.percentage()); assertEquals("Pending",result.status());
        assertNull(result.marks().get(1).score());
    }
    @Test void invalidMarksAndMaximaCannotProduceAchievement() {
        var a=item(1,0);
        assertNull(LoAttainmentCalculator.calculate(lo(),List.of(a),List.of(score(a,0.0)),List.of(),50).percentage());
        a.setMaxMarks(10.0);
        assertNull(LoAttainmentCalculator.calculate(lo(),List.of(a),List.of(score(a,11.0)),List.of(),50).percentage());
        assertNull(LoAttainmentCalculator.calculate(lo(),List.of(a),List.of(score(a,Double.NaN)),List.of(),50).percentage());
    }
    @Test void legacyMirrorIsDisplayedButNotDoubleCounted() {
        var a=item(1,10); var legacy=new StudentMark();
        legacy.setScore(5.0); legacy.setMarkType(MarkType.ASSIGNMENT); legacy.setAssignmentLabel("Assignment 1");
        var result=LoAttainmentCalculator.calculate(lo(),List.of(a),List.of(score(a,5.0)),List.of(legacy),50);
        assertEquals(50.0,result.percentage()); assertEquals(2,result.marks().size());
        assertTrue(result.marks().get(1).source().contains("reference only"));
    }
    @Test void legacyWithoutMaximumIsPending() {
        var mark=new StudentMark(); mark.setScore(70.0);
        var result=LoAttainmentCalculator.calculate(lo(),List.of(),List.of(),List.of(mark),50);
        assertEquals("Pending",result.status()); assertNull(result.percentage());
    }
    @Test void emptyLoIsNotAssessed() {
        assertEquals("Not assessed",LoAttainmentCalculator.calculate(lo(),List.of(),List.of(),List.of(),50).status());
    }
}
