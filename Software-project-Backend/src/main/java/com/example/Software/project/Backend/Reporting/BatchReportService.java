package com.example.Software.project.Backend.Reporting;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Service.ModuleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import static com.example.Software.project.Backend.Reporting.BatchReport.*;

@Service
@Transactional(readOnly = true)
public class BatchReportService {
    private final BatchReportRepository repository;
    private final ModuleService moduleService;
    public BatchReportService(BatchReportRepository repository, ModuleService moduleService) {
        this.repository = repository; this.moduleService = moduleService;
    }
    private List<Module> visible(String role, String username) {
        if ("admin".equals(role) || "superadmin".equals(role)) return moduleService.getAllModules();
        if ("lecture".equals(role)) return moduleService.getModulesForLecturer(username);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff access required");
    }
    private String batch(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid batch is required");
        return value.trim();
    }
    private void percentage(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thresholds and targets must be between 0 and 100");
    }
    public List<ModuleOption> options(String batch, String role, String username) {
        String selectedBatch = batch(batch);
        List<Module> allowed = visible(role, username);
        if (allowed.isEmpty()) return List.of();
        Set<String> associated = new HashSet<>(repository.batchModules(selectedBatch,
                allowed.stream().map(Module::getModuleId).toList()));
        return allowed.stream().filter(m -> associated.contains(m.getModuleId()))
                .sorted(Comparator.comparing(Module::getModuleId))
                .map(m -> new ModuleOption(m.getModuleId(), m.getModuleName())).toList();
    }

    public BatchReport generate(String batch, List<String> moduleIds, double studentThreshold,
            double loTarget, double poTarget, String role, String username) {
        String selectedBatch = batch(batch);
        percentage(studentThreshold); percentage(loTarget); percentage(poTarget);
        List<ModuleOption> available = options(selectedBatch, role, username);
        Set<String> requested = moduleIds == null ? new LinkedHashSet<>() :
                moduleIds.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> availableIds = available.stream().map(ModuleOption::moduleId).collect(Collectors.toSet());
        if (!availableIds.containsAll(requested))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A selected module is not available for this batch or account");
        List<ModuleOption> selected = available.stream().filter(m -> requested.isEmpty() || requested.contains(m.moduleId())).toList();
        if (selected.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No accessible modules found for this batch");
        List<String> ids = selected.stream().map(ModuleOption::moduleId).toList();
        return assemble(selectedBatch, selected, repository.los(ids), repository.items(selectedBatch, ids),
                repository.scores(selectedBatch, ids), repository.legacy(selectedBatch, ids),
                repository.mappings(ids), repository.pos(), studentThreshold, loTarget, poTarget);
    }

    // Shares the individual report's evidence rules; never calls mutating academic workflows.
    static BatchReport assemble(String batch, List<ModuleOption> modules, List<Los> los,
            List<AssessmentItem> items, List<StudentAssessmentScore> scores, List<StudentMark> legacy,
            List<OutcomeMapping> mappings, List<ProgramOutcome> pos, double threshold, double loTarget, double poTarget) {
        Map<String, Set<String>> populations = new HashMap<>();
        scores.forEach(s -> populations.computeIfAbsent(s.getAssessmentItem().getLos().getModuleId(), k -> new TreeSet<>())
                .add(s.getStudentId()));
        legacy.forEach(m -> populations.computeIfAbsent(m.getLos().getModuleId(), k -> new TreeSet<>()).add(m.getStudentIndex()));
        Set<String> uniqueStudents = populations.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        Map<String, List<AssessmentItem>> itemsByLo = items.stream().collect(Collectors.groupingBy(i -> i.getLos().getId()));
        Map<String, Map<String, List<StudentAssessmentScore>>> scoresByLo = scores.stream().collect(Collectors.groupingBy(
                s -> s.getAssessmentItem().getLos().getId(), Collectors.groupingBy(StudentAssessmentScore::getStudentId)));
        Map<String, Map<String, List<StudentMark>>> legacyByLo = legacy.stream().collect(Collectors.groupingBy(
                m -> m.getLos().getId(), Collectors.groupingBy(StudentMark::getStudentIndex)));
        List<ModuleResult> moduleResults = new ArrayList<>();
        Map<String, LoResult> loLookup = new HashMap<>();
        for (ModuleOption module : modules) {
            Set<String> population = populations.getOrDefault(module.moduleId(), Set.of());
            List<LoResult> results = new ArrayList<>();
            for (Los lo : los) {
                if (!module.moduleId().equals(lo.getModuleId())) continue;
                int assessed = 0, achieved = 0;
                for (String student : population) {
                    var result = StudentReportService.calculate(lo, itemsByLo.getOrDefault(lo.getId(), List.of()),
                            scoresByLo.getOrDefault(lo.getId(), Map.of()).getOrDefault(student, List.of()),
                            legacyByLo.getOrDefault(lo.getId(), Map.of()).getOrDefault(student, List.of()), threshold);
                    if (result.percentage() != null) {
                        assessed++;
                        if ("Achieved".equals(result.status())) achieved++;
                    }
                }
                Double percent = assessed == 0 ? null : 100.0 * achieved / assessed;
                int pending = population.size() - assessed;
                String status = assessed == 0 ? "Not assessed" : pending > 0 ? "Pending" :
                        percent >= loTarget ? "Achieved" : "Below target";
                LoResult result = new LoResult(lo.getId(), lo.getName(), population.size(), assessed,
                        achieved, assessed - achieved, pending, percent,
                        population.isEmpty() ? 0 : 100.0 * assessed / population.size(), loTarget, status);
                results.add(result); loLookup.put(lo.getId(), result);
            }
            int reached = (int) results.stream().filter(l -> "Achieved".equals(l.status())).count();
            String status = results.isEmpty() || population.isEmpty() ? "Not assessed" :
                    results.stream().anyMatch(l -> !complete(l.status())) ? "Pending" :
                    reached == results.size() ? "Achieved" : "Below target";
            moduleResults.add(new ModuleResult(module.moduleId(), module.moduleName(), population.size(),
                    reached, results.size(), results.isEmpty() ? null : 100.0 * reached / results.size(), status, results));
        }
        return new BatchReport(batch, Instant.now(), threshold, loTarget, poTarget,
                "Selected batch and listed accessible modules only; PO results describe this scope, not full programme certification.",
                uniqueStudents.size(), moduleResults, aggregatePos(pos, mappings, loLookup, poTarget), List.of(
                "Analysis copy. Student threshold, batch LO target and PO target are report settings, not saved academic policies.",
                "Module population = distinct students with any question or legacy mark record in that module and batch. This is not an enrolment count; students without any records cannot be counted.",
                "Individual LO achievement uses the same question-score/max-marks calculation and evidence checks as the individual report. Legacy mirrors are not counted twice.",
                "LO achievement % = achieved students / fully assessed students x 100. Pending students are excluded from that denominator and shown separately; any pending evidence keeps the cohort verdict Pending.",
                "Module LO coverage = LOs achieving the batch target / all defined module LOs x 100. A module achieves the report rule only when all LOs meet the target with complete evidence. This is not a module pass rate.",
                "PO attainment % = sum(approved positive mapping weight x LO achievement %) / sum(approved positive mapping weights). These are weighted LO rates, not the percentage of students passing the PO.",
                "A PO is classified only when every contributing mapped LO in the selected scope has complete evidence. The available-data score may still be displayed as provisional with Pending status. No mapping means Not mapped, not zero.",
                "Only active POs and approved positive-weight mappings contribute. Repeated mappings for the same LO/PO are marked Pending rather than counted twice.",
                "All stored assessment templates for the selected batch are included, matching the individual report. Repeat attempts and academic periods are not separated.",
                "No approved enrolment list, module pass rules or programme-to-batch membership are available. All active POs are listed; unmapped POs may be outside this programme."));
    }

    static List<PoResult> aggregatePos(List<ProgramOutcome> pos, List<OutcomeMapping> mappings,
            Map<String, LoResult> los, double target) {
        List<PoResult> results = new ArrayList<>();
        for (ProgramOutcome po : pos) {
            if (!Boolean.TRUE.equals(po.getIsActive())) continue;
            Map<String, List<OutcomeMapping>> byLo = mappings.stream()
                    .filter(m -> m.getStatus() == OutcomeMapping.ApprovalStatus.APPROVED &&
                            m.getWeight() != null && m.getWeight() > 0 &&
                            po.getPoId().equals(m.getProgramOutcome().getPoId()) && los.containsKey(m.getLearningOutcomeId()))
                    .collect(Collectors.groupingBy(OutcomeMapping::getLearningOutcomeId, TreeMap::new, Collectors.toList()));
            List<Contribution> contributions = new ArrayList<>();
            int completeLos = 0;
            double sum = 0, denominator = 0;
            for (var entry : byLo.entrySet()) {
                OutcomeMapping mapping = entry.getValue().get(0);
                LoResult lo = los.get(entry.getKey());
                boolean duplicate = entry.getValue().size() > 1;
                boolean valid = !duplicate && complete(lo.status()) && lo.achievementPercent() != null;
                if (valid) { sum += mapping.getWeight() * lo.achievementPercent(); denominator += mapping.getWeight(); completeLos++; }
                contributions.add(new Contribution(mapping.getModuleId(), lo.loId(), mapping.getWeight(),
                        lo.achievementPercent(), duplicate ? "Pending: duplicate mapping" : lo.status()));
            }
            Double value = denominator == 0 ? null : sum / denominator;
            String status = byLo.isEmpty() ? "Not mapped" : completeLos < byLo.size() ? "Pending" :
                    value >= target ? "Achieved" : "Below target";
            results.add(new PoResult(po.getPoId(), po.getCode(), po.getTitle(), value, target,
                    byLo.size(), completeLos, status, contributions));
        }
        return results;
    }
    private static boolean complete(String status) {
        return "Achieved".equals(status) || "Below target".equals(status);
    }
}
