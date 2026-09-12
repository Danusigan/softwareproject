package com.example.Software.project.Backend.Reporting.Progress;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class AttainmentCalculator {
    public enum Status { ACHIEVED, NOT_ACHIEVED, IN_PROGRESS, INSUFFICIENT_EVIDENCE }
    public record Mark(String assessment, String question, BigDecimal obtained, BigDecimal maximum) {}
    public record Result(BigDecimal obtained, BigDecimal maximum, BigDecimal percentage,
                         BigDecimal threshold, Status status, int evidenceCount) {}
    public record Evidence(String module, String lo, BigDecimal percentage, BigDecimal mappingWeight,
                           BigDecimal creditWeight, Status status, List<Mark> marks) {}
    public record Contribution(Evidence evidence, BigDecimal effectiveWeight, BigDecimal weightedValue,
                               BigDecimal percentagePoints) {}
    public record PoResult(Result result, BigDecimal numerator, BigDecimal denominator,
                           List<Contribution> contributions) {}
    public record Attempt(String offering, int number, String status, boolean official, BigDecimal mark) {}
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final MathContext PRECISION = MathContext.DECIMAL128;

    public Result lo(List<Mark> marks, BigDecimal threshold, boolean inProgress) {
        validateThreshold(threshold);
        BigDecimal obtained = BigDecimal.ZERO, maximum = BigDecimal.ZERO;
        int count = 0;
        for (Mark mark : marks) {
            if (mark.maximum() == null || mark.maximum().signum() <= 0)
                return new Result(null, null, null, threshold, Status.INSUFFICIENT_EVIDENCE, 0);
            maximum = maximum.add(mark.maximum());
            if (mark.obtained() != null) {
                if (mark.obtained().signum() < 0 || mark.obtained().compareTo(mark.maximum()) > 0)
                    return new Result(null, maximum, null, threshold, Status.INSUFFICIENT_EVIDENCE, 0);
                obtained = obtained.add(mark.obtained());
                count++;
            }
        }
        if (marks.isEmpty() || count == 0 || count < marks.size())
            return new Result(count == 0 ? null : obtained, maximum, null, threshold,
                    inProgress && !marks.isEmpty() ? Status.IN_PROGRESS : Status.INSUFFICIENT_EVIDENCE, count);
        BigDecimal value = obtained.multiply(HUNDRED).divide(maximum, PRECISION);
        // Compare exact cross-products before display rounding, including at the threshold boundary.
        Status status = inProgress ? Status.IN_PROGRESS : obtained.multiply(HUNDRED)
                .compareTo(threshold.multiply(maximum)) >= 0 ? Status.ACHIEVED : Status.NOT_ACHIEVED;
        return new Result(obtained, maximum, value, threshold, status, count);
    }

    public PoResult po(List<Evidence> evidence, BigDecimal threshold, int minimumEvidence) {
        validateThreshold(threshold);
        if (minimumEvidence < 1) throw new IllegalArgumentException("Minimum evidence must be positive");
        BigDecimal sum = BigDecimal.ZERO, denominator = BigDecimal.ZERO;
        int count = 0;
        for (Evidence e : evidence) {
            if (e.mappingWeight() == null || e.mappingWeight().signum() <= 0 ||
                    e.creditWeight() == null || e.creditWeight().signum() <= 0)
                throw new IllegalArgumentException("Mapping and credit weights must be positive");
            if (complete(e)) {
                BigDecimal weight = e.mappingWeight().multiply(e.creditWeight());
                sum = sum.add(e.percentage().multiply(weight));
                denominator = denominator.add(weight); count++;
            }
        }
        BigDecimal value = denominator.signum() == 0 ? null : sum.divide(denominator, PRECISION);
        Status status = count < minimumEvidence || evidence.isEmpty() ? Status.INSUFFICIENT_EVIDENCE :
                count < evidence.size() ? (evidence.stream().anyMatch(e -> e.status() == Status.IN_PROGRESS)
                        ? Status.IN_PROGRESS : Status.INSUFFICIENT_EVIDENCE) :
                sum.compareTo(threshold.multiply(denominator)) >= 0 ? Status.ACHIEVED : Status.NOT_ACHIEVED;
        List<Contribution> contributions = new ArrayList<>();
        for (Evidence e : evidence) {
            BigDecimal weight = e.mappingWeight().multiply(e.creditWeight());
            BigDecimal weighted = complete(e) ? e.percentage().multiply(weight) : null;
            contributions.add(new Contribution(e, weight, weighted,
                    weighted == null || denominator.signum() == 0 ? null : weighted.divide(denominator, PRECISION)));
        }
        return new PoResult(new Result(null, null, value, threshold, status, count), sum, denominator, contributions);
    }

    public Optional<Attempt> select(List<Attempt> attempts, String policy) {
        List<Attempt> completed = attempts.stream().filter(a -> Set.of("PASS", "FAIL").contains(a.status())).toList();
        return switch (policy) {
            case "OFFICIAL" -> {
                List<Attempt> official = attempts.stream().filter(Attempt::official).toList();
                yield official.size() == 1 ? Optional.of(official.get(0)) : Optional.empty();
            }
            case "LATEST_COMPLETED" -> completed.stream().max(Comparator.comparingInt(Attempt::number));
            case "BEST" -> completed.stream().filter(a -> a.mark() != null)
                    .max(Comparator.comparing(Attempt::mark).thenComparingInt(Attempt::number));
            default -> throw new IllegalArgumentException("Unsupported attempt policy");
        };
    }
    private boolean complete(Evidence e) {
        return e.percentage() != null && (e.status() == Status.ACHIEVED || e.status() == Status.NOT_ACHIEVED);
    }
    public static void validateThreshold(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(HUNDRED) > 0)
            throw new IllegalArgumentException("A configured threshold between 0 and 100 is required");
    }
    public static BigDecimal display(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
