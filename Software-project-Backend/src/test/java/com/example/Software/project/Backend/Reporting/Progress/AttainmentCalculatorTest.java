package com.example.Software.project.Backend.Reporting.Progress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.Software.project.Backend.Reporting.Progress.AttainmentCalculator.*;

class AttainmentCalculatorTest {
    private final AttainmentCalculator calculator=new AttainmentCalculator();
    private BigDecimal d(String value) {return new BigDecimal(value);}
    private Mark mark(String score,String max) {return new Mark("Assessment","Question",score==null?null:d(score),d(max));}
    private Evidence evidence(String lo,String score,String weight,Status status) {return new Evidence("MODULE",lo,score==null?null:d(score),d(weight),BigDecimal.ONE,status,List.of());}
    @Test void fixedExampleUsesUnequalQuestionMaximaAndMappingWeights() {
        var lo=calculator.lo(List.of(mark("8","10"),mark("16","20")),d("60"),false);
        assertEquals(0,d("80").compareTo(lo.percentage()));assertEquals(Status.ACHIEVED,lo.status());
        assertEquals(d("24"),lo.obtained());assertEquals(d("30"),lo.maximum());
        var po=calculator.po(List.of(evidence("LO1","80","3",Status.ACHIEVED),evidence("LO2","60","1",Status.NOT_ACHIEVED)),d("65"),2);
        assertEquals(0,d("75").compareTo(po.result().percentage()));assertEquals(Status.ACHIEVED,po.result().status());
        assertEquals(0,d("300").compareTo(po.numerator()));assertEquals(0,d("4").compareTo(po.denominator()));
    }
    @ParameterizedTest @CsvSource({"5.9,NOT_ACHIEVED","6,ACHIEVED","6.1,ACHIEVED","0,NOT_ACHIEVED"})
    void thresholdsAreInclusive(String score,Status status) {assertEquals(status,calculator.lo(List.of(mark(score,"10")),d("60"),false).status());}
    @Test void poThresholdIsInclusive() {assertEquals(Status.ACHIEVED,calculator.po(List.of(evidence("LO","65","1",Status.ACHIEVED)),d("65"),1).result().status());}
    @Test void missingIsNeverZero() {
        var marks=List.of(mark("8","10"),mark(null,"20"));
        assertNull(calculator.lo(marks,d("60"),true).percentage());
        assertEquals(Status.IN_PROGRESS,calculator.lo(marks,d("60"),true).status());
        assertEquals(Status.INSUFFICIENT_EVIDENCE,calculator.lo(marks,d("60"),false).status());
        assertEquals(Status.INSUFFICIENT_EVIDENCE,calculator.lo(List.of(),d("60"),false).status());
    }
    @ParameterizedTest @CsvSource({"0,0","-1,10","11,10","5,-10"})
    void invalidMarksCannotAchieve(String score,String max) {assertEquals(Status.INSUFFICIENT_EVIDENCE,calculator.lo(List.of(mark(score,max)),d("60"),false).status());}
    @Test void incompletePoCannotAchieveEvenWhenAvailableScoreIsHigh() {
        var result=calculator.po(List.of(evidence("LO1","100","1",Status.ACHIEVED),evidence("LO2",null,"1",Status.INSUFFICIENT_EVIDENCE)),d("60"),1);
        assertEquals(Status.INSUFFICIENT_EVIDENCE,result.result().status());assertEquals(1,result.result().evidenceCount());
        assertNull(result.contributions().get(1).weightedValue());
    }
    @Test void minimumEvidenceIsEnforced() {assertEquals(Status.INSUFFICIENT_EVIDENCE,calculator.po(List.of(evidence("LO1","100","1",Status.ACHIEVED)),d("60"),2).result().status());}
    @Test void roundingDoesNotTurnFailureIntoAchievement() {
        var lo=calculator.lo(List.of(mark("59.9999","100")),d("60"),false);
        assertEquals(d("60.00"),display(lo.percentage()));assertEquals(Status.NOT_ACHIEVED,lo.status());
        var decimal=calculator.lo(List.of(mark("0.1","0.2"),mark("0.2","0.3")),d("60"),false);
        assertEquals(0,d("60").compareTo(decimal.percentage()));assertEquals(Status.ACHIEVED,decimal.status());
    }
    @Test void creditsParticipateInPoWeighting() {
        var a=new Evidence("A","LO1",d("80"),d("3"),d("2"),Status.ACHIEVED,List.of());
        var b=evidence("LO2","60","1",Status.NOT_ACHIEVED);
        assertEquals(d("77.14"),display(calculator.po(List.of(a,b),d("65"),1).result().percentage()));
    }
    @Test void retakesAreSelectedNotCombined() {
        var first=new Attempt("A",1,"PASS",true,d("80"));var second=new Attempt("B",2,"FAIL",false,d("40"));var pending=new Attempt("C",3,"IN_PROGRESS",false,null);
        assertEquals(first,calculator.select(List.of(first,second,pending),"OFFICIAL").orElseThrow());
        assertEquals(second,calculator.select(List.of(first,second,pending),"LATEST_COMPLETED").orElseThrow());
        assertEquals(first,calculator.select(List.of(first,second,pending),"BEST").orElseThrow());
        assertTrue(calculator.select(List.of(first,new Attempt("B",2,"PASS",true,d("90"))),"OFFICIAL").isEmpty());
        assertTrue(calculator.select(List.of(pending),"LATEST_COMPLETED").isEmpty());
    }
    @Test void invalidPoliciesAndWeightsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->calculator.lo(List.of(),null,false));
        assertThrows(IllegalArgumentException.class,()->calculator.lo(List.of(),d("101"),false));
        assertThrows(IllegalArgumentException.class,()->calculator.po(List.of(evidence("LO","80","-1",Status.ACHIEVED)),d("60"),1));
        assertThrows(IllegalArgumentException.class,()->calculator.select(List.of(),"UNSUPPORTED"));
    }
}
