package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Security.JwtUtil;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/reports/batches")
public class BatchReportController {
    private final BatchReportService service;
    private final BatchReportPdf pdf;
    private final JwtUtil jwt;
    public BatchReportController(BatchReportService service, BatchReportPdf pdf, JwtUtil jwt) {
        this.service = service; this.pdf = pdf; this.jwt = jwt;
    }
    private record Staff(String role, String username) {}
    private Staff staff(String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException();
            String token = authorization.substring(7);
            String username = jwt.extractUsername(token);
            if (!Boolean.TRUE.equals(jwt.validateToken(token, username))) throw new IllegalArgumentException();
            String role = jwt.extractRole(token);
            return new Staff(role == null ? "" : role.trim().toLowerCase(Locale.ROOT), username);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A valid staff login is required");
        }
    }
    @GetMapping("/modules")
    public ResponseEntity<?> modules(@RequestParam String batch,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Staff staff = staff(authorization);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.options(batch, staff.role(), staff.username()));
    }
    @GetMapping("/attainment")
    public ResponseEntity<?> report(@RequestParam String batch,
            @RequestParam(required = false) List<String> moduleIds,
            @RequestParam(defaultValue = "50") double studentThreshold,
            @RequestParam(defaultValue = "70") double loTarget,
            @RequestParam(defaultValue = "70") double poTarget,
            @RequestParam(defaultValue = "json") String format,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Staff staff = staff(authorization);
        if (!"json".equals(format) && !"pdf".equals(format))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format must be json or pdf");
        BatchReport report = service.generate(batch, moduleIds, studentThreshold, loTarget, poTarget, staff.role(), staff.username());
        if ("pdf".equals(format)) {
            String filename = "batch-attainment-" + report.batch().replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf";
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                    .contentType(MediaType.APPLICATION_PDF).body(pdf.render(report));
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(report);
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> error(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(Map.of("message", Objects.toString(exception.getReason(), "Report unavailable")));
    }
}
