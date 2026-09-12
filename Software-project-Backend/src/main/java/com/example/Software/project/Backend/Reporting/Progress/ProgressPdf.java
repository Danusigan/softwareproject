package com.example.Software.project.Backend.Reporting.Progress;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Comparator;
import static com.example.Software.project.Backend.Reporting.Progress.ProgressReport.*;

@Component
public class ProgressPdf {
    private Font font(int size,boolean bold) { return FontFactory.getFont(FontFactory.HELVETICA,size,bold?Font.BOLD:Font.NORMAL); }
    public byte[] render(ProgressReport r) {
        var bytes=new ByteArrayOutputStream();
        Document d=new Document(PageSize.A4.rotate(),32,32,36,40);
        PdfWriter writer=PdfWriter.getInstance(d,bytes);
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override public void onEndPage(PdfWriter w,Document doc) {
                ColumnText.showTextAligned(w.getDirectContent(),Element.ALIGN_CENTER,new Phrase("Confidential | "+r.reference()+" | Page "+w.getPageNumber(),font(8,false)),421,20,0);
            }
        });
        d.addTitle("Student Academic Progress and Programme Outcome Attainment"); d.open();
        heading(d,writer,"Student Academic Progress and Programme Outcome Attainment");
        paragraph(d,value(r.student().get("university"))+" | "+value(r.student().get("programme")));
        heading(d,writer,"Student profile and report period");
        paragraph(d,value(r.student().get("studentId"))+" | "+value(r.student().get("studentName"))+" | "+value(r.student().get("email")));
        paragraph(d,"Cohort: "+value(r.student().get("cohort"))+" | Curriculum: "+value(r.student().get("curriculum"))+" | Entire available academic history");
        paragraph(d,"Generated (UTC): "+r.generatedAt()+" | Calculation policy: "+value(r.policy().get("policy_version"))+" | Retake policy: "+value(r.policy().get("retake_policy")));
        heading(d,writer,"Academic progress summary");
        paragraph(d,"Credits attempted: "+value(r.summary().creditsAttempted())+" | Credits completed: "+value(r.summary().creditsCompleted())+" | GPA: "+value(r.summary().gpa()));
        paragraph(d,"Academic completion: "+r.summary().academicStatus()+" | PO attainment: "+r.summary().poStatus()+" | Data completeness: "+r.summary().completeness());
        paragraph(d,"LOs achieved / not achieved: "+r.summary().losAchieved()+" / "+r.summary().losNotAchieved()+" | POs achieved / not achieved: "+r.summary().posAchieved()+" / "+r.summary().posNotAchieved());
        heading(d,writer,"Semester-by-semester module results");
        PdfPTable history=table("Year / semester","Module","Attempt / selected","Mark / grade","Credits","Academic result");
        record Entry(ModuleResult module,ModuleAttempt attempt) {}
        r.modules().stream().flatMap(m->m.attempts().stream().map(a->new Entry(m,a)))
                .sorted(Comparator.comparing(e->e.attempt().startsOn())).forEach(e->{var a=e.attempt();row(history,a.academicYear()+" / "+a.semester(),e.module().moduleId()+" - "+e.module().moduleName(),a.number()+" / "+(a.selected()?"Yes":"No"),value(a.finalMark())+" / "+value(a.grade()),value(e.module().credits()),a.status());});
        d.add(history);
        heading(d,writer,"Module LO attainment and threshold comparisons");
        for(var m:r.modules()) {
            paragraph(d,m.moduleId()+" - "+m.moduleName()+" | Selected offering: "+value(m.selectedOffering()));
            PdfPTable los=table("LO / description","Obtained","Maximum","Attainment %","Threshold %","Status");
            for(var l:m.los()) row(los,l.loId()+" - "+value(l.description()),value(l.result().obtained()),value(l.result().maximum()),value(l.result().percentage()),value(l.result().threshold()),l.result().status().name());
            d.add(los);
            for(var a:m.attempts()) for(var l:a.los()) {
                paragraph(d,m.moduleId()+" / "+l.loId()+" | Attempt "+a.number()+" | "+a.academicYear()+" / "+a.semester()+" | "+(a.selected()?"Selected":"Not selected"));
                PdfPTable marks=table("Assessment","Question","Obtained","Maximum");
                for(var mark:l.marks()) row(marks,mark.assessment(),mark.question(),value(mark.obtained()),value(mark.maximum()));
                if(l.marks().isEmpty()) row(marks,"No question evidence","-","-","-");
                d.add(marks);
            }
        }
        heading(d,writer,"PO attainment summary");
        PdfPTable outcomes=table("PO / description","Attainment %","Threshold %","Evidence / minimum","Required","Status");
        for(var p:r.pos()) { var result=p.calculation().result(); row(outcomes,p.code()+" - "+value(p.description()),value(result.percentage()),value(result.threshold()),result.evidenceCount()+" / "+p.minimumEvidence(),p.required()?"Yes":"No",result.status().name()); }
        d.add(outcomes);
        heading(d,writer,"PO evidence and calculation details");
        paragraph(d,"LO % = sum(obtained mapped question marks) / sum(maximum mapped question marks) x 100. Missing marks are not zero.");
        paragraph(d,"PO % = sum(LO % x mapping weight x credit weight) / sum(mapping weight x credit weight). Both achieved and not-achieved complete LOs contribute. Incomplete evidence prevents achievement. Status comparisons occur before display rounding.");
        for(var p:r.pos()) {
            paragraph(d,p.code()+": numerator "+value(p.calculation().numerator())+" / denominator "+value(p.calculation().denominator())+" = "+value(p.calculation().result().percentage())+"%");
            PdfPTable contributions=table("Module / LO","LO % / status","Mapping weight","Credit weight","Weighted value","PO percentage points");
            for(var c:p.calculation().contributions()) row(contributions,c.evidence().module()+" / "+c.evidence().lo(),value(c.evidence().percentage())+" / "+c.evidence().status(),value(c.evidence().mappingWeight()),value(c.evidence().creditWeight()),value(c.weightedValue()),value(c.percentagePoints()));
            d.add(contributions);
        }
        heading(d,writer,"Strongest and weakest outcome areas");
        var ranked=r.pos().stream().filter(p->p.calculation().result().percentage()!=null).sorted(Comparator.comparing(p->p.calculation().result().percentage())).toList();
        if(ranked.isEmpty()) paragraph(d,"Insufficient evidence to rank outcomes.");
        else paragraph(d,"Lowest available PO: "+ranked.get(0).code()+" | Highest available PO: "+ranked.get(ranked.size()-1).code()+". Provisional outcomes remain subject to the completeness checks above.");
        heading(d,writer,"Missing-data and insufficient-evidence warnings");
        if(r.warnings().isEmpty()) paragraph(d,"No data-completeness warnings.");
        for(String warning:r.warnings()) paragraph(d,warning);
        if(!r.unassignedEvidence().isEmpty()) {
            PdfPTable evidence=table("Period / module / LO","Assessment / question","Obtained","Maximum");
            for(var e:r.unassignedEvidence()) row(evidence,value(e.get("academic_year"))+" / "+value(e.get("semester"))+" / "+value(e.get("module_id"))+" / "+value(e.get("lo_id")),value(e.get("assessment"))+" / "+value(e.get("question")),value(e.get("obtained")),value(e.get("maximum")));
            d.add(evidence);
        }
        heading(d,writer,"Final conclusion"); paragraph(d,r.conclusion());
        paragraph(d,"The university or authorized awarding body makes the final qualification decision.");
        paragraph(d,"Authorized review: ____________________    Signature: ____________________    Date: ____________________");
        d.close(); return bytes.toByteArray();
    }
    private void heading(Document d,PdfWriter writer,String text) { if(writer.getVerticalPosition(true)<d.bottom()+100) d.newPage(); Paragraph p=new Paragraph(text,font(14,true));p.setSpacingBefore(14);p.setSpacingAfter(6);d.add(p); }
    private void paragraph(Document d,String text) { d.add(new Paragraph(text,font(10,false))); }
    private PdfPTable table(String... headers) {
        PdfPTable t=new PdfPTable(headers.length);t.setWidthPercentage(100);t.setHeaderRows(1);t.setSplitLate(false);t.setSpacingBefore(8);t.setSpacingAfter(8);
        for(String h:headers) {PdfPCell c=new PdfPCell(new Phrase(h,font(9,true)));c.setBackgroundColor(new Color(222,232,245));c.setPadding(6);t.addCell(c);}return t;
    }
    private void row(PdfPTable t,String... values) {for(String v:values){PdfPCell c=new PdfPCell(new Phrase(value(v),font(9,false)));c.setPadding(5);t.addCell(c);}}
    private String value(Object v) {return v==null?"Not recorded":v instanceof BigDecimal b?AttainmentCalculator.display(b).toPlainString():v.toString();}
}
