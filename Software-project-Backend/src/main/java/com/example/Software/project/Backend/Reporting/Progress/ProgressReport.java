package com.example.Software.project.Backend.Reporting.Progress;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static com.example.Software.project.Backend.Reporting.Progress.AttainmentCalculator.*;

public record ProgressReport(String reference, Instant generatedAt, Map<String,Object> student,
        Map<String,Object> policy, Summary summary, List<ModuleResult> modules, List<ProgrammeResult> pos,
        List<Map<String,Object>> unassignedEvidence, List<String> warnings, String conclusion) {
    public record Summary(BigDecimal creditsAttempted, BigDecimal creditsCompleted, BigDecimal gpa,
            String academicStatus, Status poStatus, String completeness,
            long losAchieved, long losNotAchieved, long posAchieved, long posNotAchieved) {}
    public record LearningResult(String loId, String name, String description, Result result, List<Mark> marks) {}
    public record ModuleAttempt(String offering, int number, String academicYear, String semester, String startsOn,
            String status, boolean selected, BigDecimal finalMark, String grade, BigDecimal gradePoints,
            List<LearningResult> los) {}
    public record ModuleResult(String moduleId, String moduleName, BigDecimal credits, boolean compulsory,
            String selectedOffering, List<ModuleAttempt> attempts, List<LearningResult> los) {}
    public record ProgrammeResult(String poId, String code, String description, boolean required, int minimumEvidence, PoResult calculation) {}
}
