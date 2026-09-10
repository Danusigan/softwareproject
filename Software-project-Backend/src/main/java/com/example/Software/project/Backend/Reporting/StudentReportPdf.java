package com.example.Software.project.Backend.Reporting;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import static com.example.Software.project.Backend.Reporting.StudentReport.*;

@Component
public class StudentReportPdf {
    private static final Color NAVY = new Color(30, 64, 110);
    private static final Color GREEN = new Color(21, 128, 61);
    private static final Color RED = new Color(185, 28, 28);
    private Font font(float size, boolean bold) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, bold ? Font.BOLD : Font.NORMAL);
    }

    public byte[] render(StudentReport report) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 42);
        PdfWriter writer = PdfWriter.getInstance(document, bytes);
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override public void onEndPage(PdfWriter w, Document d) {
                ColumnText.showTextAligned(w.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase("Confidential student report | " + report.studentId() + " | Page " +
                            w.getPageNumber(), font(8, false)), 297, 22, 0);
            }
        });
        document.addTitle("Individual student achievement report");
        document.addAuthor("LO-PO Analytics");
        document.open();
        document.add(new Paragraph("STUDENT ACHIEVEMENT REPORT", font(20, true)));
        document.add(new Paragraph("LO-PO Analytics | Individual assessment summary", font(11, false)));
        document.add(new Paragraph("Generated: " + report.generatedAt() + " | Analysis copy", font(9, false)));
        document.add(Chunk.NEWLINE);
        PdfPTable identity = table("Student ID", "Name", "Batch", "Academic year");
        row(identity, report.studentId(), report.studentName(), report.batch(), report.academicYear());
        document.add(identity);
        document.add(new Paragraph("Email: " + text(report.email()), font(10, false)));
        document.add(new Paragraph("LO threshold: " + number(report.threshold()) + "%", font(11, true)));
        document.add(new Paragraph(report.scope(), font(9, false)));
        long achieved = report.modules().stream().flatMap(m -> m.los().stream())
                .filter(l -> "Achieved".equals(l.status())).count();
        long below = report.modules().stream().flatMap(m -> m.los().stream())
                .filter(l -> "Below threshold".equals(l.status())).count();
        long count = report.modules().stream().mapToLong(m -> m.los().size()).sum();
        document.add(new Paragraph(report.modules().size() + " modules | " + achieved + " LOs achieved | " +
                below + " below threshold | " + (count - achieved - below) + " pending / not assessed", font(11, true)));
        for (ModuleResult module : report.modules()) {
            document.add(new Paragraph(module.moduleId() + " - " + text(module.moduleName()), font(15, true)));
            document.add(new Paragraph("LO score versus selected threshold (0-100%)", font(10, false)));
            PdfPTable summary = table("LO", "Score / threshold", "Status", "Gap (pp)");
            for (LoResult lo : module.los()) {
                row(summary, lo.loId(), number(lo.percentage()) + " / " + number(lo.threshold()) + "%",
                        lo.status(), number(lo.margin()));
            }
            document.add(summary);
            for (LoResult lo : module.los()) {
                if (lo.percentage() != null) {
                    PdfPTable chart = new PdfPTable(new float[]{1, 4});
                    chart.setWidthPercentage(100);
                    chart.setSpacingBefore(4);
                    chart.addCell(new Phrase(lo.loId(), font(9, false)));
                    PdfPCell bar = new PdfPCell();
                    bar.setFixedHeight(20);
                    bar.setCellEvent((cell, rect, canvases) -> {
                        PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
                        float width = rect.getWidth() - 8;
                        canvas.setColorFill(new Color(235, 239, 245));
                        canvas.rectangle(rect.getLeft() + 4, rect.getBottom() + 5, width, 10);
                        canvas.fill();
                        canvas.setColorFill("Achieved".equals(lo.status()) ? GREEN : RED);
                        canvas.rectangle(rect.getLeft() + 4, rect.getBottom() + 5,
                                width * lo.percentage().floatValue() / 100, 10);
                        canvas.fill();
                        canvas.setColorStroke(NAVY);
                        float x = rect.getLeft() + 4 + width * (float) lo.threshold() / 100;
                        canvas.moveTo(x, rect.getBottom() + 2);
                        canvas.lineTo(x, rect.getTop() - 2);
                        canvas.stroke();
                    });
                    chart.addCell(bar);
                    document.add(chart);
                }
                document.add(new Paragraph(lo.loId() + " - " + text(lo.name()), font(12, true)));
                if (lo.description() != null) document.add(new Paragraph(lo.description(), font(9, false)));
                PdfPTable marks = table("Assessment / period", "Question", "Mark / max", "%", "Evidence");
                marks.setWidths(new float[]{3, 1, 1.3f, 0.8f, 2.5f});
                for (MarkRow mark : lo.marks()) {
                    row(marks, text(mark.assessment()) + " (" + text(mark.type()) + ")\n" +
                            text(mark.academicYear()) + " / " + text(mark.semester()),
                            mark.question(), number(mark.score()) + " / " + number(mark.maximum()),
                            number(mark.percentage()), mark.source());
                }
                if (lo.marks().isEmpty()) row(marks, "No recorded assessments", "-", "-", "-", "Not assessed");
                document.add(marks);
            }
        }
        document.add(new Paragraph("Calculation and interpretation", font(14, true)));
        for (String note : report.notes()) document.add(new Paragraph("- " + note, font(9, false)));
        document.close();
        return bytes.toByteArray();
    }

    private PdfPTable table(String... headers) {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setSpacingAfter(8);
        table.setHeaderRows(1);
        table.setSplitLate(false);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            cell.setBackgroundColor(NAVY);
            cell.setPadding(6);
            table.addCell(cell);
        }
        return table;
    }
    private void row(PdfPTable table, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(text(value), font(9, false)));
            cell.setPadding(5);
            cell.setBorderColor(new Color(210, 218, 230));
            table.addCell(cell);
        }
    }
    private static String text(String value) { return value == null || value.isBlank() ? "Not recorded" : value; }
    private static String number(Double value) {
        return value == null || !Double.isFinite(value) ? "-" : String.format(Locale.ROOT, "%.2f", value);
    }
}
