package com.example.Software.project.Backend.Reporting;

import java.time.Instant;
import java.util.List;

public record BatchReport(String batch, Instant generatedAt, double studentThreshold,
        double loTarget, double poTarget, String scope, int studentsWithRecords,
        List<ModuleResult> modules, List<PoResult> pos, List<String> notes) {
    public record ModuleResult(String moduleId, String moduleName, int studentsWithRecords,
            int achievedLos, int totalLos, Double achievedLoPercent, String status, List<LoResult> los) {}
    public record LoResult(String loId, String name, int studentsWithRecords, int assessed,
            int achieved, int belowThreshold, int pending, Double achievementPercent,
            double coveragePercent, double target, String status) {}
    public record Contribution(String moduleId, String loId, int weight,
            Double achievementPercent, String status) {}
    public record PoResult(String poId, String code, String title, Double attainmentPercent,
            double target, int mappedLos, int completeLos, String status, List<Contribution> contributions) {}
    public record ModuleOption(String moduleId, String moduleName) {}
}
