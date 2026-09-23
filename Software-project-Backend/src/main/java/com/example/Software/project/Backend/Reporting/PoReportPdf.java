package com.example.Software.project.Backend.Reporting;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Renders {@link PoStudentReport} and {@link PoBatchReport} as PDFs, admin-only downloads. */
@Component
public class PoReportPdf {
    private static final Color NAVY = new Color(30, 64, 110);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(java.time.ZoneId.systemDefault());

    private Font font(float size, boolean bold) { return FontFactory.getFont(FontFactory.HELVETICA, size, bold ? Font.BOLD : Font.NORMAL); }

    public byte[] renderStudent(PoStudentReport report) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 42);
        PdfWriter writer = PdfWriter.getInstance(document, bytes);
        writer.setPageEvent(footer("Student " + report.studentId() + " | PO credit report | Page "));
        document.addTitle("Student PO credit report");
        document.addAuthor("LO-PO Analytics");
        document.open();

        document.add(new Paragraph("STUDENT PO CREDIT REPORT", font(20, true)));
        document.add(new Paragraph(report.studentName() + " (" + report.studentId() + ")", font(13, true)));
        document.add(new Paragraph("Generated: " + TIMESTAMP.format(report.generatedAt()), font(10, false)));
        document.add(new Paragraph("Student attainment threshold: " + num(report.studentThreshold()) + "%", font(11, true)));
        document.add(new Paragraph("Cumulative credits earned across every module whose PO attainment has been calculated for this student " +
                "(" + report.moduleCount() + " module(s)). Not scoped to one batch — this is the student's whole recorded academic standing.", font(9, false)));

        PdfPTable table = table("PO", "Credits Earned", "Max Credits", "%", "Status");
        for (PoStudentReport.PoRow po : report.pos()) {
            row(table, po.code() + " - " + text(po.title()), String.valueOf(po.creditsEarned()),
                    String.valueOf(po.maxCredits()), num(po.percentage()), po.status());
        }
        if (report.pos().isEmpty()) row(table, "No PO credits saved for this student yet", "-", "-", "-", "No evidence");
        document.add(table);

        for (PoStudentReport.PoRow po : report.pos()) {
            if (po.moduleBreakdown().isEmpty()) continue;
            document.add(new Paragraph(po.code() + " - contributing modules", font(11, true)));
            PdfPTable modules = table("Module", "Batch", "Credits Earned", "Max Credits");
            for (PoStudentReport.ModuleContribution c : po.moduleBreakdown()) {
                row(modules, c.moduleId(), c.batch(), String.valueOf(c.creditsEarned()), String.valueOf(c.maxCredits()));
            }
            document.add(modules);
        }

        document.add(new Paragraph("Notes", font(12, true)));
        document.add(new Paragraph("- \"Attained\" means this student's credit percentage for the PO meets or exceeds the threshold above.", font(9, false)));
        document.add(new Paragraph("- \"No evidence\" means no module has calculated a PO credit for this student yet.", font(9, false)));
        document.add(new Paragraph("- Figures reflect each module's most recent saved calculation, not necessarily today's marks.", font(9, false)));
        document.close();
        return bytes.toByteArray();
    }

    public byte[] renderBatch(PoBatchReport report) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 42);
        PdfWriter writer = PdfWriter.getInstance(document, bytes);
        writer.setPageEvent(footer("Batch " + report.batch() + " | PO success report | Page "));
        document.addTitle("Batch PO success report");
        document.addAuthor("LO-PO Analytics");
        document.open();

        document.add(new Paragraph("BATCH PO SUCCESS REPORT", font(20, true)));
        document.add(new Paragraph("Batch " + report.batch(), font(13, true)));
        document.add(new Paragraph("Generated: " + TIMESTAMP.format(report.generatedAt()), font(10, false)));
        document.add(new Paragraph("Student attainment threshold: " + num(report.studentThreshold()) +
                "% | Batch success target: " + num(report.batchTarget()) + "%", font(11, true)));
        document.add(new Paragraph("A student attains a PO when their saved credit percentage in this batch meets the student threshold. " +
                "A PO is a batch success when the share of the batch's " + report.totalStudents() +
                " students attaining it meets the batch target.", font(9, false)));

        PdfPTable table = table("PO", "Students Attained", "Total Students", "Attainment %", "Status");
        for (PoBatchReport.PoRow po : report.pos()) {
            row(table, po.code() + " - " + text(po.title()), String.valueOf(po.studentsAttained()),
                    String.valueOf(po.totalStudents()), num(po.attainmentPercent()), po.status());
        }
        if (report.pos().isEmpty()) row(table, "No active programme outcomes", "-", "-", "-", "Not mapped");
        document.add(table);

        for (PoBatchReport.PoRow po : report.pos()) {
            chart(document, po.code(), po.attainmentPercent(), report.batchTarget(), po.status());
        }

        document.add(new Paragraph("Areas requiring review", font(13, true)));
        boolean finding = false;
        for (PoBatchReport.PoRow po : report.pos()) {
            if ("Not successful".equals(po.status())) {
                finding = true;
                document.add(new Paragraph(po.code() + ": only " + num(po.attainmentPercent()) +
                        "% of the batch attained this PO, below the " + num(report.batchTarget()) + "% target.", font(9, false)));
            }
        }
        if (!finding) document.add(new Paragraph("All active POs meet the selected batch target.", font(9, false)));
        document.close();
        return bytes.toByteArray();
    }

    private void chart(Document document, String label, double value, double target, String status) {
        PdfPTable chart = new PdfPTable(new float[]{1, 3, 2});
        chart.setWidthPercentage(100); chart.setSpacingBefore(5); chart.setSpacingAfter(5);
        chart.addCell(new Phrase(label, font(9, false)));
        PdfPCell bar = new PdfPCell(); bar.setFixedHeight(22);
        bar.setCellEvent((cell, rect, canvases) -> {
            PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
            canvas.saveState();
            float width = rect.getWidth() - 8;
            canvas.setColorFill(new Color(235, 239, 245));
            canvas.rectangle(rect.getLeft() + 4, rect.getBottom() + 6, width, 10); canvas.fill();
            canvas.setColorFill(color(status));
            canvas.rectangle(rect.getLeft() + 4, rect.getBottom() + 6, width * (float) value / 100, 10); canvas.fill();
            canvas.setColorStroke(NAVY);
            float x = rect.getLeft() + 4 + width * (float) target / 100;
            canvas.moveTo(x, rect.getBottom() + 2); canvas.lineTo(x, rect.getTop() - 2); canvas.stroke();
            canvas.restoreState();
        });
        chart.addCell(bar);
        chart.addCell(new Phrase(num(value) + "% / target " + num(target) + "%\n" + status, font(8, false)));
        document.add(chart);
    }

    private Color color(String status) {
        return "Success".equals(status) || "Attained".equals(status) ? new Color(21, 128, 61)
                : "No evidence".equals(status) ? new Color(180, 110, 15) : new Color(185, 28, 28);
    }

    private PdfPageEventHelper footer(String prefix) {
        return new PdfPageEventHelper() {
            @Override public void onEndPage(PdfWriter w, Document d) {
                ColumnText.showTextAligned(w.getDirectContent(), Element.ALIGN_CENTER,
                        new Phrase(prefix + w.getPageNumber(), font(8, false)), 297, 22, 0);
            }
        };
    }

    private PdfPTable table(String... headers) {
        PdfPTable table = new PdfPTable(headers.length); table.setWidthPercentage(100);
        table.setSpacingBefore(8); table.setSpacingAfter(8); table.setHeaderRows(1); table.setSplitLate(false);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            cell.setBackgroundColor(NAVY); cell.setPadding(5); table.addCell(cell);
        }
        return table;
    }

    private void row(PdfPTable table, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(text(value), font(9, false)));
            cell.setPadding(5); cell.setBorderColor(new Color(210, 218, 230)); table.addCell(cell);
        }
    }

    private static String text(String value) { return value == null || value.isBlank() ? "Not recorded" : value; }
    private static String num(Double value) { return value == null ? "-" : String.format(Locale.ROOT, "%.2f", value); }
    private static String num(double value) { return String.format(Locale.ROOT, "%.2f", value); }
}
