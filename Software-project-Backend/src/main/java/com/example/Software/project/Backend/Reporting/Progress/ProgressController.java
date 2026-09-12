package com.example.Software.project.Backend.Reporting.Progress;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Map;

@RestController
@RequestMapping("/api/reports/progress")
public class ProgressController {
    private final ProgressService service;
    private final ProgressConfiguration configuration;
    private final ProgressPdf pdf;
    public ProgressController(ProgressService service,ProgressConfiguration configuration,ProgressPdf pdf) {this.service=service;this.configuration=configuration;this.pdf=pdf;}
    @GetMapping("/students") public ResponseEntity<?> search(@RequestParam(defaultValue="") String q,Authentication auth) {return ok(service.search(q,auth));}
    @PostMapping("/students/{studentId}/snapshots") public ResponseEntity<?> generate(@PathVariable String studentId,Authentication auth) {return ok(service.generate(studentId,auth));}
    @GetMapping("/snapshots/{reference}") public ResponseEntity<?> snapshot(@PathVariable String reference,Authentication auth) {return ok(service.snapshot(reference,auth,"PREVIEW"));}
    @GetMapping("/snapshots/{reference}/pdf") public ResponseEntity<?> pdf(@PathVariable String reference,Authentication auth) {
        var report=service.snapshot(reference,auth,"DOWNLOAD_PDF");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename("student-progress-"+reference+".pdf").build().toString()).body(pdf.render(report));
    }
    @GetMapping("/configuration") public ResponseEntity<?> catalog(Authentication auth) {return ok(configuration.catalog(auth));}
    @PostMapping("/configuration/curricula") public ResponseEntity<?> curriculum(@RequestBody ProgressConfiguration.Curriculum body,Authentication auth) {configuration.curriculum(body,auth);return saved();}
    @PutMapping("/configuration/student-programmes") public ResponseEntity<?> profile(@RequestBody ProgressConfiguration.Profile body,Authentication auth) {configuration.profile(body,auth);return saved();}
    @PostMapping("/configuration/offerings") public ResponseEntity<?> offering(@RequestBody ProgressConfiguration.Offering body,Authentication auth) {configuration.offering(body,auth);return saved();}
    @PutMapping("/configuration/enrolments") public ResponseEntity<?> enrolment(@RequestBody ProgressConfiguration.Enrolment body,Authentication auth) {configuration.enrolment(body,auth);return saved();}
    private ResponseEntity<?> saved() {return ok(Map.of("message","Configuration saved"));}
    private ResponseEntity<?> ok(Object body) {return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> invalid(IllegalArgumentException e) {return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("message",e.getMessage()==null?"Invalid report input":e.getMessage()));}
    @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<?> conflict() {return ResponseEntity.status(409).cacheControl(CacheControl.noStore()).body(Map.of("message","Configuration conflicts with a saved record or constraint. Check codes, account links and duplicate records."));}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> error(ResponseStatusException e) {return ResponseEntity.status(e.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("message",e.getReason()==null?"Report unavailable":e.getReason()));}
}
