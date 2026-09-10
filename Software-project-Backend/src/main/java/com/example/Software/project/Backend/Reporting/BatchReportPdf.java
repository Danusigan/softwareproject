package com.example.Software.project.Backend.Reporting;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import static com.example.Software.project.Backend.Reporting.BatchReport.*;

@Component
public class BatchReportPdf {
    private static final Color NAVY = new Color(30, 64, 110);
    private Font font(float size, boolean bold) { return FontFactory.getFont(FontFactory.HELVETICA, size, bold ? Font.BOLD : Font.NORMAL); }
    public byte[] render(BatchReport report) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 42);
        PdfWriter writer = PdfWriter.getInstance(document, bytes);
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override public void onEndPage(PdfWriter w, Document d) {
                ColumnText.showTextAligned(w.getDirectContent(), Element.ALIGN_CENTER,
                        new Phrase("Batch " + report.batch() + " | Analysis copy | Page " + w.getPageNumber(), font(8, false)),
                        297, 22, 0);
            }
        });
        document.addTitle("Batch LO and PO attainment report");
        document.addAuthor("LO-PO Analytics");
        document.open();
        document.add(new Paragraph("BATCH ATTAINMENT REPORT", font(20, true)));
        document.add(new Paragraph("Batch " + report.batch() + " | Generated: " + report.generatedAt(), font(10, false)));
        document.add(new Paragraph(report.scope(), font(10, false)));
        document.add(new Paragraph("Report settings: student LO threshold " + num(report.studentThreshold()) +
                "% | batch LO target " + num(report.loTarget()) + "% | PO target " + num(report.poTarget()) + "%", font(11, true)));
        document.add(new Paragraph("These editable settings are not approved academic policy. Missing evidence keeps a verdict pending.", font(9, false)));
        document.add(new Paragraph(report.studentsWithRecords() + " distinct students with records | " +
                report.modules().size() + " modules | " + report.pos().size() + " active POs", font(11, true)));

        document.add(new Paragraph("Module LO achievement coverage", font(15, true)));
        document.add(new Paragraph("Report rule: all defined LOs meet the batch target with complete evidence. This is not a module pass rate.", font(9, false)));
        PdfPTable modules = table("Module", "Students with records", "LOs achieved / total", "Coverage %", "Status");
        for (ModuleResult m : report.modules()) row(modules, m.moduleId() + " - " + text(m.moduleName()),
                String.valueOf(m.studentsWithRecords()), m.achievedLos() + " / " + m.totalLos(), num(m.achievedLoPercent()), m.status());
        document.add(modules);

        for (ModuleResult m : report.modules()) {
            document.add(new Paragraph(m.moduleId() + " - " + text(m.moduleName()), font(14, true)));
            document.add(new Paragraph("LO achievement among fully assessed students. Any pending student makes the percentage provisional.", font(9, false)));
            PdfPTable los = table("LO", "Assessed / known", "Achieved", "Below", "Pending", "LO %", "Status");
            los.setWidths(new float[]{1.4f,1.5f,1,1,1,1,1.7f});
            for (LoResult lo : m.los()) row(los, lo.loId() + "\n" + text(lo.name()), lo.assessed() + " / " + lo.studentsWithRecords(),
                    String.valueOf(lo.achieved()), String.valueOf(lo.belowThreshold()), String.valueOf(lo.pending()),
                    num(lo.achievementPercent()), lo.status());
            document.add(los);
            for (LoResult lo : m.los()) {
                chart(document, lo.loId(), lo.achievementPercent(), lo.target(), lo.status());
            }
        }
        document.add(new Paragraph("PO attainment in the selected scope", font(15, true)));
        document.add(new Paragraph("Weighted LO achievement rates, not student PO pass rates. Pending scores use only complete mapped LOs and are provisional.", font(9, false)));
        PdfPTable pos = table("PO", "Score %", "Target %", "Complete / mapped LOs", "Status");
        for (PoResult po : report.pos()) row(pos, po.code() + " - " + text(po.title()), num(po.attainmentPercent()),
                num(po.target()), po.completeLos() + " / " + po.mappedLos(), po.status());
        if (report.pos().isEmpty()) row(pos, "No active programme outcomes", "-", "-", "-", "Not mapped");
        document.add(pos);
        for (PoResult po : report.pos()) {
            chart(document, po.code(), po.attainmentPercent(), po.target(), po.status());
            if (!po.contributions().isEmpty()) {
                document.add(new Paragraph(po.code() + " - approved LO contributions", font(11, true)));
                PdfPTable mappings = table("Module", "LO", "Weight", "LO %", "Evidence status");
                for (Contribution c : po.contributions()) row(mappings, c.moduleId(), c.loId(), String.valueOf(c.weight()),
                        num(c.achievementPercent()), c.status());
                document.add(mappings);
            }
        }
        document.add(new Paragraph("Areas requiring review", font(14, true)));
        boolean finding = false;
        for (ModuleResult m : report.modules()) for (LoResult lo : m.los()) {
            if (!"Achieved".equals(lo.status())) {
                finding = true;
                document.add(new Paragraph(m.moduleId() + " / " + lo.loId() + ": " + lo.status() +
                        ("Below target".equals(lo.status()) ? " - review teaching and assessment evidence." : " - complete or verify assessment records."), font(9, false)));
            }
        }
        if (!finding) document.add(new Paragraph("All listed LOs meet the selected target.", font(9, false)));
        document.add(new Paragraph("Calculation and scope notes", font(14, true)));
        for (String note : report.notes()) document.add(new Paragraph("- " + note, font(9, false)));
        document.close();
        return bytes.toByteArray();
    }

    private void chart(Document document, String label, Double value, double target, String status) {
        PdfPTable chart = new PdfPTable(new float[]{1,3,2});
        chart.setWidthPercentage(100); chart.setSpacingBefore(5); chart.setSpacingAfter(5);
        chart.addCell(new Phrase(label, font(9,false)));
        PdfPCell bar = new PdfPCell(); bar.setFixedHeight(22);
        bar.setCellEvent((cell, rect, canvases) -> {
            PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
            canvas.saveState();
            float width = rect.getWidth() - 8;
            canvas.setColorFill(new Color(235,239,245));
            canvas.rectangle(rect.getLeft()+4,rect.getBottom()+6,width,10); canvas.fill();
            if (value != null) {
                canvas.setColorFill(color(status));
                canvas.rectangle(rect.getLeft()+4,rect.getBottom()+6,width*value.floatValue()/100,10); canvas.fill();
            }
            canvas.setColorStroke(NAVY);
            float x=rect.getLeft()+4+width*(float)target/100;
            canvas.moveTo(x,rect.getBottom()+2); canvas.lineTo(x,rect.getTop()-2); canvas.stroke();
            canvas.restoreState();
        });
        chart.addCell(bar);
        chart.addCell(new Phrase(num(value) + "% / target " + num(target) + "%\n" + status, font(8,false)));
        document.add(chart);
    }
    private Color color(String status) {
        return "Achieved".equals(status) ? new Color(21,128,61) : "Below target".equals(status) ?
                new Color(185,28,28) : new Color(180,110,15);
    }
    private PdfPTable table(String... headers) {
        PdfPTable table=new PdfPTable(headers.length); table.setWidthPercentage(100);
        table.setSpacingBefore(8); table.setSpacingAfter(8); table.setHeaderRows(1); table.setSplitLate(false);
        for (String header:headers) {
            PdfPCell cell=new PdfPCell(new Phrase(header,new Font(Font.HELVETICA,9,Font.BOLD,Color.WHITE)));
            cell.setBackgroundColor(NAVY); cell.setPadding(5); table.addCell(cell);
        }
        return table;
    }
    private void row(PdfPTable table,String... values) {
        for(String value:values) {
            PdfPCell cell=new PdfPCell(new Phrase(text(value),font(9,false)));
            cell.setPadding(5); cell.setBorderColor(new Color(210,218,230)); table.addCell(cell);
        }
    }
    private static String text(String value) { return value==null || value.isBlank() ? "Not recorded" : value; }
    private static String num(Double value) { return value==null ? "-" : String.format(Locale.ROOT,"%.2f",value); }
}
