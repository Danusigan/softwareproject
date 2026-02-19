package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttainmentService {

    @Autowired
    private StudentMarkRepository markRepository;
    @Autowired
    private OutcomeMappingRepository mappingRepository;
    @Autowired
    private LosRepository losRepository;

    // 1. Calculate LO Attainment Level (1, 2, or 3)
    public int calculateLOLevel(String loId) {
        LosPos lo = losPosRepository.findById(loId).orElse(null);
        if (lo == null || lo.getAssignments() == null || lo.getAssignments().isEmpty()) return 0;

        List<StudentMark> allMarks = new ArrayList<>();
        for (Assignment assignment : lo.getAssignments()) {
            allMarks.addAll(markRepository.findByAssessment_AssignmentId(assignment.getAssignmentId()));
        }

        if (allMarks.isEmpty()) return 0;

        long totalStudents = allMarks.size();
        long passedStudents = allMarks.stream().filter(m -> m.getScore() >= 50.0).count();

        double percentage = (double) passedStudents / totalStudents * 100;

        if (percentage >= 80) return 3;
        if (percentage >= 70) return 2;
        if (percentage >= 60) return 1;
        return 0;
    }

    // 2. Calculate PO Attainment for a Course
    public Map<String, Double> getPOAttainment(String moduleId) {
        List<OutcomeMapping> mappings = mappingRepository.findByLearningOutcome_Module_ModuleId(moduleId);

        // Group mappings by PO Code
        Map<String, List<OutcomeMapping>> poGroups = mappings.stream()
                .filter(m -> m.getStatus() == OutcomeMapping.ApprovalStatus.APPROVED)
                .collect(Collectors.groupingBy(m -> m.getProgramOutcome().getCode()));

        Map<String, Double> poScores = new HashMap<>();

        for (Map.Entry<String, List<OutcomeMapping>> entry : poGroups.entrySet()) {
            String poCode = entry.getKey();
            List<OutcomeMapping> poMappings = entry.getValue();

            double weightedSum = 0;
            double totalWeight = 0;

            for (OutcomeMapping map : poMappings) {
                int loLevel = calculateLOLevel(map.getLearningOutcome().getId());
                weightedSum += (loLevel * map.getWeight());
                totalWeight += map.getWeight();
            }

            double finalScore = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
            poScores.put(poCode, finalScore);
        }
        return poScores;
    }
}
