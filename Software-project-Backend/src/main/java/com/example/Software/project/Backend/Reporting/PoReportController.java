package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Security.JwtUtil;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Admin-only PO reports: a named student's cross-module PO credit standing, and a batch's PO
 * success rate. See {@link PoReportService} for how each is built and why this is a separate
 * system from {@link BatchReportController} (that one is LO-based, anonymized, staff-wide).
 */
@RestController
@RequestMapping("/api/reports/po")
public class PoReportController {

    private final PoReportService service;
    private final PoReportPdf pdf;
    private final JwtUtil jwt;

    public PoReportController(PoReportService service, PoReportPdf pdf, JwtUtil jwt) {
        this.service = service; this.pdf = pdf; this.jwt = jwt;
    }

    private void requireAdmin(String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException();
            String token = authorization.substring(7);
            String username = jwt.extractUsername(token);
            if (!Boolean.TRUE.equals(jwt.validateToken(token, username))) throw new IllegalArgumentException();
            String role = jwt.extractRole(token);
            role = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
            if (!role.equals("admin") && !role.equals("superadmin")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A valid admin login is required");
        }
    }

    private void validateFormat(String format) {
        if (!"json".equals(format) && !"pdf".equals(format)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format must be json or pdf");
        }
    }

    @GetMapping("/student")
    public ResponseEntity<?> studentReport(
            @RequestParam String studentId,
            @RequestParam(defaultValue = "40") double studentThreshold,
            @RequestParam(defaultValue = "json") String format,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        validateFormat(format);
        PoStudentReport report = service.studentReport(studentId.trim(), studentThreshold);
        if ("pdf".equals(format)) {
            String filename = "po-report-" + report.studentId().replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf";
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                    .contentType(MediaType.APPLICATION_PDF).body(pdf.renderStudent(report));
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(report);
    }

    @GetMapping("/batch")
    public ResponseEntity<?> batchReport(
            @RequestParam String batch,
            @RequestParam(defaultValue = "40") double studentThreshold,
            @RequestParam(defaultValue = "60") double batchTarget,
            @RequestParam(defaultValue = "json") String format,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        validateFormat(format);
        PoBatchReport report = service.batchReport(batch.trim(), studentThreshold, batchTarget);
        if ("pdf".equals(format)) {
            String filename = "po-batch-report-" + report.batch().replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf";
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                    .contentType(MediaType.APPLICATION_PDF).body(pdf.renderBatch(report));
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(report);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> error(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(Map.of("message", Objects.toString(exception.getReason(), "Report unavailable")));
    }
}
