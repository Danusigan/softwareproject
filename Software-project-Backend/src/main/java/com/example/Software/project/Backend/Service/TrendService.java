package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Repository.StudentMarkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TrendService {

    @Autowired
    private StudentMarkRepository markRepository;

    // Original Module-level trend
    public List<Map<String, Object>> getCourseTrend(String courseId) {
        List<Object[]> rawData = markRepository.findYearlyAverageByCourse(courseId);
        return processTrendData(rawData, false);
    }

    // New LO-level trend
    public Map<String, List<Map<String, Object>>> getLoTrend(String courseId) {
        List<Object[]> rawData = markRepository.findLoTrendByCourse(courseId);
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        // Group raw data by LO ID
        Map<String, List<Object[]>> groupedData = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            if (row == null || row.length < 4) {
                continue;
            }
            String loId = toSafeString(row[0]);
            String loName = toSafeString(row[1]);
            String key = loId + " - " + loName;

            groupedData.computeIfAbsent(key, k -> new ArrayList<>()).add(new Object[]{row[2], row[3]}); // year, avg
        }

        // Process trend for each LO
        for (Map.Entry<String, List<Object[]>> entry : groupedData.entrySet()) {
            result.put(entry.getKey(), processTrendData(entry.getValue(), false));
        }

        return result;
    }

    private List<Map<String, Object>> processTrendData(List<Object[]> rawData, boolean hasBatch) {
        List<Map<String, Object>> result = new ArrayList<>();
        Double previousAvg = null;

        for (Object[] row : rawData) {
            if (row == null || row.length < 2) {
                continue;
            }

            String year = toSafeString(row[0]);
            String batch = null;
            Double currentAvg;

            if (hasBatch && row.length >= 3) {
                batch = toSafeString(row[1]);
                currentAvg = toSafeDouble(row[2]);
            } else {
                currentAvg = toSafeDouble(row[1]);
            }

            if (currentAvg == null) {
                continue;
            }

            Map<String, Object> entry = new HashMap<>();
            // Format year to include batch if present
            String displayYear = (batch != null && !batch.isEmpty()) ? year + " (" + batch + ")" : year;
            entry.put("year", displayYear);
            entry.put("average", currentAvg);

            if (previousAvg != null && previousAvg != 0) {
                double delta = ((currentAvg - previousAvg) / previousAvg) * 100;
                entry.put("delta", delta);
                if (delta > 5) entry.put("status", "IMPROVED");
                else if (delta < -5) entry.put("status", "DECLINED");
                else entry.put("status", "STABLE");
            } else {
                entry.put("status", "BASELINE");
            }

            result.add(entry);
            previousAvg = currentAvg;
        }
        return result;
    }

    private String toSafeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Double toSafeDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
