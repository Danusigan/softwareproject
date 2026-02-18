package com.example.Software.project.Backend.Model;

import java.util.Map;

/**
 * DTO for Comparative Trend Analysis across academic years for a single course/module.
 */
public class TrendReportDTO {

    // Average CO (LO) attainment percentage per academic year
    private Map<String, Double> averageCoAttainmentByYear;

    // Average PO attainment percentage per academic year
    private Map<String, Double> averagePoAttainmentByYear;

    // Performance status per year relative to baseline (IMPROVED, STABLE, DECLINED)
    private Map<String, String> performanceStatusByYear;

    // PO-wise attainment per year: year -> (poCode -> attainmentPercentage)
    private Map<String, Map<String, Double>> poAttainmentByYear;

    // PO that improved the most between baseline and latest year
    private String highestImprovedPO;
    private Double highestImprovedDelta;

    // PO that declined the most between baseline and latest year
    private String mostDeclinedPO;
    private Double mostDeclinedDelta;

    // Human-readable summary (e.g., "Overall performance improved by 12% compared to 2024")
    private String summary;

    public Map<String, Double> getAverageCoAttainmentByYear() {
        return averageCoAttainmentByYear;
    }

    public void setAverageCoAttainmentByYear(Map<String, Double> averageCoAttainmentByYear) {
        this.averageCoAttainmentByYear = averageCoAttainmentByYear;
    }

    public Map<String, Double> getAveragePoAttainmentByYear() {
        return averagePoAttainmentByYear;
    }

    public void setAveragePoAttainmentByYear(Map<String, Double> averagePoAttainmentByYear) {
        this.averagePoAttainmentByYear = averagePoAttainmentByYear;
    }

    public Map<String, String> getPerformanceStatusByYear() {
        return performanceStatusByYear;
    }

    public void setPerformanceStatusByYear(Map<String, String> performanceStatusByYear) {
        this.performanceStatusByYear = performanceStatusByYear;
    }

    public Map<String, Map<String, Double>> getPoAttainmentByYear() {
        return poAttainmentByYear;
    }

    public void setPoAttainmentByYear(Map<String, Map<String, Double>> poAttainmentByYear) {
        this.poAttainmentByYear = poAttainmentByYear;
    }

    public String getHighestImprovedPO() {
        return highestImprovedPO;
    }

    public void setHighestImprovedPO(String highestImprovedPO) {
        this.highestImprovedPO = highestImprovedPO;
    }

    public Double getHighestImprovedDelta() {
        return highestImprovedDelta;
    }

    public void setHighestImprovedDelta(Double highestImprovedDelta) {
        this.highestImprovedDelta = highestImprovedDelta;
    }

    public String getMostDeclinedPO() {
        return mostDeclinedPO;
    }

    public void setMostDeclinedPO(String mostDeclinedPO) {
        this.mostDeclinedPO = mostDeclinedPO;
    }

    public Double getMostDeclinedDelta() {
        return mostDeclinedDelta;
    }

    public void setMostDeclinedDelta(Double mostDeclinedDelta) {
        this.mostDeclinedDelta = mostDeclinedDelta;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
