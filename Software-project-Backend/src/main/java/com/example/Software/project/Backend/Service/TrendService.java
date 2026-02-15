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
        return processTrendData(rawData);
    }

    // New LO-level trend
    public Map<String, List<Map<String, Object>>> getLoTrend(String courseId) {
        List<Object[]> rawData = markRepository.findLoTrendByCourse(courseId);
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        // Group raw data by LO ID
        Map<String, List<Object[]>> groupedData = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            String loId = (String) row[0];
            String loName = (String) row[1];
            String key = loId + " - " + loName;
            
            groupedData.computeIfAbsent(key, k -> new ArrayList<>()).add(new Object[]{row[2], row[3]}); // year, avg
        }

        // Process trend for each LO
        for (Map.Entry<String, List<Object[]>> entry : groupedData.entrySet()) {
            result.put(entry.getKey(), processTrendData(entry.getValue()));
        }

        return result;
    }

    private List<Map<String, Object>> processTrendData(List<Object[]> rawData) {
        List<Map<String, Object>> result = new ArrayList<>();
        Double previousAvg = null;

        for (Object[] row : rawData) {
            String year = (String) row[0];
            Double currentAvg = (Double) row[1];
            
            Map<String, Object> entry = new HashMap<>();
            entry.put("year", year);
            entry.put("average", currentAvg);

            if (previousAvg != null) {
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
}