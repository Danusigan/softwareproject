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
    private StudentMarkRepository studentMarkRepository;
    
    @Autowired
    private MappingRepository mappingRepository;
    
    @Autowired
    private LosPosRepository losPosRepository;
    
    @Autowired
    private ModuleRepository moduleRepository;
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private ProgramOutcomeRepository programOutcomeRepository;
    
    private static final double PASS_THRESHOLD = 50.0; // Minimum 50% for pass
    
    // OBE-specific thresholds for CO level assignment
    private static final double LEVEL_1_THRESHOLD = 60.0; // Level 1: >60% students pass
    private static final double LEVEL_2_THRESHOLD = 70.0; // Level 2: >70% students pass  
    private static final double LEVEL_3_THRESHOLD = 80.0; // Level 3: >80% students pass
    
    // Calculate attainment for a specific assignment
    public Map<String, Object> calculateAssignmentAttainment(String assignmentId) {
        Optional<Assignment> assignmentOpt = assignmentRepository.findById(assignmentId);
        if (assignmentOpt.isEmpty()) {
            throw new RuntimeException("Assignment not found: " + assignmentId);
        }
        
        Assignment assignment = assignmentOpt.get();
        List<StudentMark> validMarks = studentMarkRepository.findValidMarksByAssignmentId(assignmentId);
        
        if (validMarks.isEmpty()) {
            return createEmptyAttainmentResult("assignment", assignmentId, assignment.getAssignmentName());
        }
        
        double totalStudents = validMarks.size();
        double passedStudents = validMarks.stream()
                .mapToDouble(StudentMark::getMark)
                .filter(mark -> mark >= PASS_THRESHOLD)
                .count();
        
        double attainmentPercentage = (passedStudents / totalStudents) * 100.0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("assignmentId", assignmentId);
        result.put("assignmentName", assignment.getAssignmentName());
        result.put("attainmentPercentage", Math.round(attainmentPercentage * 100.0) / 100.0);
        result.put("totalStudents", (int) totalStudents);
        result.put("passedStudents", (int) passedStudents);
        result.put("averageMark", calculateAverageMark(validMarks));
        result.put("loId", assignment.getLosPos().getLoId());
        result.put("moduleCode", assignment.getLosPos().getModuleCode());
        
        return result;
    }
    
    // Calculate attainment for a specific Learning Outcome
    public Map<String, Object> calculateLoAttainment(String losPosId) {
        Optional<LosPos> losPosOpt = losPosRepository.findById(losPosId);
        if (losPosOpt.isEmpty()) {
            throw new RuntimeException("Learning Outcome not found: " + losPosId);
        }
        
        LosPos losPos = losPosOpt.get();
        List<StudentMark> allMarks = studentMarkRepository.findValidMarksByLosPosId(losPosId);
        
        if (allMarks.isEmpty()) {
            return createEmptyAttainmentResult("lo", losPosId, losPos.getLoDescription());
        }
        
        // Group marks by student to get individual student performance
        Map<String, List<StudentMark>> marksByStudent = allMarks.stream()
                .collect(Collectors.groupingBy(sm -> sm.getStudent().getStudentId()));
        
        double totalStudents = marksByStudent.size();
        long passedStudents = marksByStudent.values().stream()
                .mapToLong(studentMarks -> {
                    double avgMark = studentMarks.stream()
                            .mapToDouble(StudentMark::getMark)
                            .average()
                            .orElse(0.0);
                    return avgMark >= PASS_THRESHOLD ? 1 : 0;
                })
                .sum();
        
        double attainmentPercentage = (passedStudents / totalStudents) * 100.0;
        
        // Calculate PO attainments for this LO
        List<Map<String, Object>> poAttainments = calculatePOAttainmentsForLO(losPosId, allMarks);
        
        Map<String, Object> result = new HashMap<>();
        result.put("losPosId", losPosId);
        result.put("loId", losPos.getLoId());
        result.put("loDescription", losPos.getLoDescription());
        result.put("attainmentPercentage", Math.round(attainmentPercentage * 100.0) / 100.0);
        result.put("totalStudents", (int) totalStudents);
        result.put("passedStudents", (int) passedStudents);
        result.put("moduleCode", losPos.getModuleCode());
        result.put("poAttainments", poAttainments);
        
        return result;
    }
    
    // Calculate attainment for an entire module
    public Map<String, Object> calculateModuleAttainment(String moduleCode) {
        Optional<com.example.Software.project.Backend.Model.Module> moduleOpt = moduleRepository.findById(moduleCode);
        if (moduleOpt.isEmpty()) {
            throw new RuntimeException("Module not found: " + moduleCode);
        }
        
        com.example.Software.project.Backend.Model.Module module = moduleOpt.get();
        List<LosPos> moduleLosList = losPosRepository.findByModuleCode(moduleCode);
        
        if (moduleLosList.isEmpty()) {
            return createEmptyAttainmentResult("module", moduleCode, module.getModuleName());
        }
        
        List<Map<String, Object>> loAttainments = new ArrayList<>();
        double totalModuleAttainment = 0.0;
        int validLoCount = 0;
        
        Set<String> allStudents = new HashSet<>();
        Set<String> passedStudents = new HashSet<>();
        
        for (LosPos losPos : moduleLosList) {
            Map<String, Object> loAttainment = calculateLoAttainment(losPos.getId());
            if ((int) loAttainment.get("totalStudents") > 0) {
                loAttainments.add(loAttainment);
                totalModuleAttainment += (double) loAttainment.get("attainmentPercentage");
                validLoCount++;
                
                // Track students for module-level statistics
                List<StudentMark> loMarks = studentMarkRepository.findValidMarksByLosPosId(losPos.getId());
                Map<String, List<StudentMark>> marksByStudent = loMarks.stream()
                        .collect(Collectors.groupingBy(sm -> sm.getStudent().getStudentId()));
                
                allStudents.addAll(marksByStudent.keySet());
                
                marksByStudent.forEach((studentId, marks) -> {
                    double avgMark = marks.stream()
                            .mapToDouble(StudentMark::getMark)
                            .average()
                            .orElse(0.0);
                    if (avgMark >= PASS_THRESHOLD) {
                        passedStudents.add(studentId);
                    }
                });
            }
        }
        
        double moduleAttainmentPercentage = validLoCount > 0 ? totalModuleAttainment / validLoCount : 0.0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("moduleCode", moduleCode);
        result.put("moduleName", module.getModuleName());
        result.put("attainmentPercentage", Math.round(moduleAttainmentPercentage * 100.0) / 100.0);
        result.put("totalStudents", allStudents.size());
        result.put("passedStudents", passedStudents.size());
        result.put("totalLearningOutcomes", validLoCount);
        result.put("loAttainments", loAttainments);
        
        return result;
    }
    
    // Calculate overall program attainment across all modules
    public Map<String, Object> calculateOverallProgramAttainment() {
        List<com.example.Software.project.Backend.Model.Module> allModules = moduleRepository.findAll();
        List<Map<String, Object>> moduleAttainments = new ArrayList<>();
        
        double totalProgramAttainment = 0.0;
        int validModuleCount = 0;
        
        Set<String> allStudents = new HashSet<>();
        Set<String> passedStudents = new HashSet<>();
        
        for (com.example.Software.project.Backend.Model.Module module : allModules) {
            Map<String, Object> moduleAttainment = calculateModuleAttainment(module.getModuleId());
            if ((int) moduleAttainment.get("totalStudents") > 0) {
                moduleAttainments.add(moduleAttainment);
                totalProgramAttainment += (double) moduleAttainment.get("attainmentPercentage");
                validModuleCount++;
                
                // Aggregate student data
                allStudents.addAll(getStudentsFromModuleAttainment(moduleAttainment));
                passedStudents.addAll(getPassedStudentsFromModuleAttainment(moduleAttainment));
            }
        }
        
        double programAttainmentPercentage = validModuleCount > 0 ? totalProgramAttainment / validModuleCount : 0.0;
        
        // Calculate PO-wise attainment across the program
        List<Map<String, Object>> programPOAttainments = calculateProgramPOAttainments();
        
        Map<String, Object> result = new HashMap<>();
        result.put("programAttainmentPercentage", Math.round(programAttainmentPercentage * 100.0) / 100.0);
        result.put("totalStudents", allStudents.size());
        result.put("totalModules", validModuleCount);
        result.put("moduleAttainments", moduleAttainments);
        result.put("programOutcomeAttainments", programPOAttainments);
        result.put("calculatedAt", new Date());
        
        return result;
    }
    
    // Calculate PO attainments for a specific Learning Outcome
    private List<Map<String, Object>> calculatePOAttainmentsForLO(String losPosId, List<StudentMark> allMarks) {
        List<Mapping> approvedMappings = mappingRepository.findApprovedMappingsByLosPosId(losPosId);
        List<Map<String, Object>> poAttainments = new ArrayList<>();
        
        for (Mapping mapping : approvedMappings) {
            if (mapping.getWeight() > 0) { // Only consider non-zero correlations
                // Calculate weighted attainment for this PO based on the mapping weight
                double weight = mapping.getWeight();
                double correlation = Math.min(1.0, weight / 3.0); // Normalize to 0-1
                
                // Calculate student performance for this PO mapping
                Map<String, List<StudentMark>> marksByStudent = allMarks.stream()
                        .collect(Collectors.groupingBy(sm -> sm.getStudent().getStudentId()));
                
                double totalStudents = marksByStudent.size();
                long passedForPO = marksByStudent.values().stream()
                        .mapToLong(studentMarks -> {
                            double avgMark = studentMarks.stream()
                                    .mapToDouble(StudentMark::getMark)
                                    .average()
                                    .orElse(0.0);
                            // Apply correlation factor to the threshold
                            double adjustedThreshold = PASS_THRESHOLD * correlation;
                            return avgMark >= adjustedThreshold ? 1 : 0;
                        })
                        .sum();
                
                double poAttainmentPercentage = totalStudents > 0 ? (passedForPO / totalStudents) * 100.0 : 0.0;
                
                Map<String, Object> poAttainment = new HashMap<>();
                poAttainment.put("poCode", mapping.getProgramOutcome().getPoCode());
                poAttainment.put("poDescription", mapping.getProgramOutcome().getPoDescription());
                poAttainment.put("attainmentPercentage", Math.round(poAttainmentPercentage * 100.0) / 100.0);
                poAttainment.put("weight", mapping.getWeight());
                poAttainment.put("correlation", Math.round(correlation * 100.0) / 100.0);
                poAttainment.put("totalStudents", (int) totalStudents);
                poAttainment.put("passedStudents", (int) passedForPO);
                
                poAttainments.add(poAttainment);
            }
        }
        
        return poAttainments;
    }
    
    // Calculate program-wide PO attainments
    private List<Map<String, Object>> calculateProgramPOAttainments() {
        List<ProgramOutcome> activePOs = programOutcomeRepository.findWithApprovedMappings();
        List<Map<String, Object>> programPOAttainments = new ArrayList<>();
        
        for (ProgramOutcome po : activePOs) {
            List<Mapping> poMappings = mappingRepository.findByProgramOutcome(po);
            List<Mapping> approvedMappings = poMappings.stream()
                    .filter(m -> m.getStatus() == Mapping.MappingStatus.APPROVED && m.getWeight() > 0)
                    .toList();
            
            if (!approvedMappings.isEmpty()) {
                double totalAttainment = 0.0;
                int validMappingCount = 0;
                int totalStudents = 0;
                int totalPassedStudents = 0;
                
                for (Mapping mapping : approvedMappings) {
                    List<StudentMark> loMarks = studentMarkRepository.findValidMarksByLosPosId(mapping.getLosPos().getId());
                    if (!loMarks.isEmpty()) {
                        List<Map<String, Object>> poAttainmentData = calculatePOAttainmentsForLO(mapping.getLosPos().getId(), loMarks);
                        
                        // Find this PO's attainment in the results
                        for (Map<String, Object> data : poAttainmentData) {
                            if (data.get("poCode").equals(po.getPoCode())) {
                                totalAttainment += (double) data.get("attainmentPercentage");
                                totalStudents += (int) data.get("totalStudents");
                                totalPassedStudents += (int) data.get("passedStudents");
                                validMappingCount++;
                                break;
                            }
                        }
                    }
                }
                
                if (validMappingCount > 0) {
                    double avgAttainment = totalAttainment / validMappingCount;
                    
                    Map<String, Object> programPOAttainment = new HashMap<>();
                    programPOAttainment.put("poCode", po.getPoCode());
                    programPOAttainment.put("poDescription", po.getPoDescription());
                    programPOAttainment.put("attainmentPercentage", Math.round(avgAttainment * 100.0) / 100.0);
                    programPOAttainment.put("totalMappings", validMappingCount);
                    programPOAttainment.put("totalStudents", totalStudents);
                    programPOAttainment.put("totalPassedStudents", totalPassedStudents);
                    
                    programPOAttainments.add(programPOAttainment);
                }
            }
        }
        
        return programPOAttainments;
    }
    
    // Helper methods
    private Map<String, Object> createEmptyAttainmentResult(String type, String id, String name) {
        Map<String, Object> result = new HashMap<>();
        result.put(type + "Id", id);
        result.put(type + "Name", name);
        result.put("attainmentPercentage", 0.0);
        result.put("totalStudents", 0);
        result.put("passedStudents", 0);
        result.put("message", "No valid student marks found for calculation");
        return result;
    }
    
    private double calculateAverageMark(List<StudentMark> marks) {
        return marks.stream()
                .mapToDouble(StudentMark::getMark)
                .average()
                .orElse(0.0);
    }
    
    private Set<String> getStudentsFromModuleAttainment(Map<String, Object> moduleAttainment) {
        // This would need to be extracted from the detailed module data
        // For now, return empty set - this would be implemented based on detailed requirements
        return new HashSet<>();
    }
    
    private Set<String> getPassedStudentsFromModuleAttainment(Map<String, Object> moduleAttainment) {
        // This would need to be extracted from the detailed module data
        // For now, return empty set - this would be implemented based on detailed requirements
        return new HashSet<>();
    }
    
    // ======================== OBE-SPECIFIC METHODS ========================
    
    /**
     * Calculate Course Attainment following OBE specifications
     * @param courseId The course ID (maps to Module in current system)
     * @return JSON structure with COs and POs attainment data
     */
    public Map<String, Object> calculateCourseAttainment(Long courseId) {
        // Convert Long to String for current Module system
        String moduleCode = String.valueOf(courseId);
        
        Optional<com.example.Software.project.Backend.Model.Module> moduleOpt = moduleRepository.findById(moduleCode);
        if (moduleOpt.isEmpty()) {
            throw new RuntimeException("Course/Module not found: " + courseId);
        }
        
        com.example.Software.project.Backend.Model.Module course = moduleOpt.get();
        
        // Step A: Calculate CO (Course Outcome) Attainments
        List<Map<String, Object>> coAttainments = calculateCOAttainments(moduleCode);
        
        // Step B: Calculate PO (Program Outcome) Attainments using CO-PO mappings
        List<Map<String, Object>> poAttainments = calculatePOAttainmentsFromCOs(coAttainments, moduleCode);
        
        Map<String, Object> result = new HashMap<>();
        result.put("courseId", courseId);
        result.put("courseName", course.getModuleName());
        result.put("learningOutcomes", coAttainments);
        result.put("programOutcomes", poAttainments);
        
        return result;
    }
    
    /**
     * Calculate Learning Outcome (LO) attainments for a course
     */
    private List<Map<String, Object>> calculateCOAttainments(String moduleCode) {
        // Get all Learning Outcomes (LOs) for this course/module
        List<LosPos> learningOutcomes = losPosRepository.findByModuleCode(moduleCode);
        List<Map<String, Object>> loAttainments = new ArrayList<>();
        
        for (LosPos lo : learningOutcomes) {
            // Get all assessments linked to this LO
            List<Assignment> assessments = assignmentRepository.findByLosPosId(lo.getId());
            
            if (assessments.isEmpty()) {
                continue; // Skip LOs with no assessments
            }
            
            // Calculate overall LO attainment across all assessments
            Map<String, Object> loAttainment = calculateSingleCOAttainment(lo, assessments);
            loAttainments.add(loAttainment);
        }
        
        return loAttainments;
    }
    
    /**
     * Calculate attainment for a single Learning Outcome (LO)
     */
    private Map<String, Object> calculateSingleCOAttainment(LosPos lo, List<Assignment> assessments) {
        Set<String> allStudents = new HashSet<>();
        Map<String, List<Double>> studentMarks = new HashMap<>();
        
        // Collect all student marks across all assessments for this LO
        for (Assignment assessment : assessments) {
            List<StudentMark> marks = studentMarkRepository.findByAssignmentAssignmentId(assessment.getAssignmentId());
            
            for (StudentMark mark : marks) {
                String studentId = mark.getStudent().getStudentId();
                allStudents.add(studentId);
                
                studentMarks.computeIfAbsent(studentId, k -> new ArrayList<>()).add(mark.getMark());
            }
        }
        
        if (allStudents.isEmpty()) {
            return createEmptyCOAttainment(lo);
        }
        
        // Calculate percentage of students who scored above target threshold (50%)
        long passedStudents = studentMarks.entrySet().stream()
                .mapToLong(entry -> {
                    double avgMark = entry.getValue().stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.0);
                    return avgMark >= PASS_THRESHOLD ? 1 : 0;
                })
                .sum();
        
        double passPercentage = (double) passedStudents / allStudents.size() * 100.0;
        
        // Assign Level based on OBE criteria
        int level = assignCOLevel(passPercentage);
        
        // Calculate average mark for this LO
        double avgMark = studentMarks.values().stream()
                .flatMap(List::stream)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        
        Map<String, Object> result = new HashMap<>();
        result.put("loId", lo.getLoId());
        result.put("loDescription", lo.getLoDescription());
        result.put("attainmentPercentage", Math.round(passPercentage * 100.0) / 100.0);
        result.put("level", level);
        result.put("totalStudents", allStudents.size());
        result.put("passedStudents", (int) passedStudents);
        result.put("averageMark", Math.round(avgMark * 100.0) / 100.0);
        result.put("assessmentCount", assessments.size());
        
        return result;
    }
    
    /**
     * Assign CO Level based on percentage of students who passed
     * Level 1: >60% students pass
     * Level 2: >70% students pass  
     * Level 3: >80% students pass
     */
    private int assignCOLevel(double passPercentage) {
        if (passPercentage >= LEVEL_3_THRESHOLD) {
            return 3;
        } else if (passPercentage >= LEVEL_2_THRESHOLD) {
            return 2;
        } else if (passPercentage >= LEVEL_1_THRESHOLD) {
            return 1;
        }
        return 0; // Below threshold
    }
    
    /**
     * Calculate PO attainments using CO-PO mapping weights
     * Formula: PO_Attainment = Σ(CO_Level × Mapping_Weight) / ΣMapping_Weight
     */
    private List<Map<String, Object>> calculatePOAttainmentsFromCOs(List<Map<String, Object>> coAttainments, String moduleCode) {
        // Get all Program Outcomes
        List<ProgramOutcome> allPOs = programOutcomeRepository.findAll();
        List<Map<String, Object>> poAttainments = new ArrayList<>();
        
        for (ProgramOutcome po : allPOs) {
            Map<String, Object> poAttainment = calculateSinglePOAttainment(po, coAttainments, moduleCode);
            if (poAttainment != null) {
                poAttainments.add(poAttainment);
            }
        }
        
        return poAttainments;
    }
    
    /**
     * Calculate attainment for a single Program Outcome (PO)
     */
    private Map<String, Object> calculateSinglePOAttainment(ProgramOutcome po, List<Map<String, Object>> coAttainments, String moduleCode) {
        double weightedSum = 0.0;
        double totalWeight = 0.0;
        int mappedCOCount = 0;
        
        for (Map<String, Object> coAttainment : coAttainments) {
            String coId = (String) coAttainment.get("coId");
            
            // Find CO-PO mapping for this CO and PO
            Optional<LosPos> losPosOpt = losPosRepository.findByLoIdAndModuleCode(coId, moduleCode);
            if (losPosOpt.isPresent()) {
                List<Mapping> mappings = mappingRepository.findByLosPosIdAndProgramOutcomeId(
                    losPosOpt.get().getId(), po.getId());
                
                for (Mapping mapping : mappings) {
                    int weight = mapping.getWeight();
                    int level = (Integer) coAttainment.get("level");
                    
                    weightedSum += level * weight;
                    totalWeight += weight;
                    mappedCOCount++;
                }
            }
        }
        
        if (totalWeight == 0.0) {
            return null; // No mappings found for this PO
        }
        
        double poAttainment = weightedSum / totalWeight;
        
        Map<String, Object> result = new HashMap<>();
        result.put("poId", po.getPoCode());
        result.put("poDescription", po.getPoDescription());
        result.put("attainmentScore", Math.round(poAttainment * 100.0) / 100.0);
        result.put("mappedCOCount", mappedCOCount);
        result.put("totalWeight", (int) totalWeight);
        
        return result;
    }
    
    /**
     * Create empty LO attainment result
     */
    private Map<String, Object> createEmptyCOAttainment(LosPos lo) {
        Map<String, Object> result = new HashMap<>();
        result.put("loId", lo.getLoId());
        result.put("loDescription", lo.getLoDescription());
        result.put("attainmentPercentage", 0.0);
        result.put("level", 0);
        result.put("totalStudents", 0);
        result.put("passedStudents", 0);
        result.put("averageMark", 0.0);
        result.put("assessmentCount", 0);
        return result;
    }
}