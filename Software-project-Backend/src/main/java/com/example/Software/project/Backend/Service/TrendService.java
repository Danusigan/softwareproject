package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Repository.StudentMarkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TrendService {

    @Autowired
    private StudentMarkRepository markRepository;

    public List<Map<String, Object>> getCourseTrend(String courseId) {
        List<Object[]> rawData = markRepository.findYearlyAverageByCourse(courseId);
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