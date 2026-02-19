package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Assignment;
import com.example.Software.project.Backend.Model.LosPos;
import com.example.Software.project.Backend.Repository.AssignmentRepository;
import com.example.Software.project.Backend.Repository.LosPosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private LosPosRepository losPosRepository;

    @Autowired
    private ExcelImportService excelImportService;

    // Create (Lecture) - Add to LosPos
    public Assignment addAssignmentToLosPos(String losPosId, Assignment assignment, MultipartFile file) throws Exception {
        Optional<LosPos> losPosOptional = losPosRepository.findById(losPosId);
        if (losPosOptional.isEmpty()) {
            throw new Exception("LosPos not found");
        }
        if (assignmentRepository.existsById(assignment.getAssignmentId())) {
            throw new Exception("Assignment ID already exists");
        }

        LosPos losPos = losPosOptional.get();

        // Process file if present (Optional)
        if (file != null && !file.isEmpty()) {
            assignment.setMarksCsvFile(file.getBytes());
            assignment.setFileName(file.getOriginalFilename());
        }

        // Link assignment to LosPos (Many assignments pointing to one LosPos)
        assignment.setLosPos(losPos);
        
        // Save assignment
        Assignment savedAssignment = assignmentRepository.save(assignment);
        
        // We don't need to save losPos explicitly if there's no changes to it,
        // but if we want to ensure the relationship is refreshed:
        if (losPos.getAssignments() != null) {
            losPos.getAssignments().add(savedAssignment);
        }
        
        return savedAssignment;
    }

    // Read All Assignments (Global)
    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    public List<Assignment> getAssignmentsByLosPosId(String losPosId) {
        Optional<LosPos> losPos = losPosRepository.findById(losPosId);
        if (losPos.isPresent()) {
            return losPos.get().getAssignments();
        }
        return List.of();
    }

    // Read One Assignment
    public Optional<Assignment> getAssignmentById(String id) {
        return assignmentRepository.findById(id);
    }

    // Update Assignment (Lecture)
    public Assignment updateAssignment(String id, String newName, MultipartFile file) throws Exception {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new Exception("Assignment not found"));

        if (newName != null && !newName.isEmpty()) {
            assignment.setAssignmentName(newName);
        }
        
        // File is optional during update
        if (file != null && !file.isEmpty()) {
            assignment.setMarksCsvFile(file.getBytes());
            assignment.setFileName(file.getOriginalFilename());
        }
        
        return assignmentRepository.save(assignment);
    }

    // Delete Assignment (Lecture)
    public void deleteAssignment(String id) throws Exception {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new Exception("Assignment not found"));
        
        // Unlink from LosPos before deleting
        // Since LosPos owns the relationship with CascadeType.ALL, we need to be careful.
        // If we delete Assignment directly, JPA might complain if LosPos still references it.
        // We should set LosPos assignment to null first.
        
        if (assignment.getLosPos() != null) {
            LosPos losPos = assignment.getLosPos();
            if (losPos.getAssignments() != null) {
                losPos.getAssignments().remove(assignment);
            }
        }
        
        assignmentRepository.deleteById(id);
    }

    // Import marks from Excel (delegates to ExcelImportService)
    public String importMarksFromExcel(String assignmentId, MultipartFile excelFile) {
        return excelImportService.importStudentMarksFromExcel(assignmentId, excelFile);
    }

    // Import marks from Excel using OBE format (2 columns: Student Index, Mark)
    public String importMarksFromExcelOBEFormat(String assignmentId, MultipartFile excelFile, String academicYear, String batch) {
        // Ensure academicYear and batch are updated if provided
        assignmentRepository.findById(assignmentId).ifPresent(a -> {
            boolean changed = false;
            if (academicYear != null && !academicYear.trim().isEmpty()) {
                a.setAcademicYear(academicYear);
                changed = true;
            }
            if (batch != null && !batch.trim().isEmpty()) {
                a.setBatch(batch);
                changed = true;
            }
            if (changed) {
                assignmentRepository.save(a);
            }
        });
        return excelImportService.importMarksOBEFormat(assignmentId, excelFile);
    }
}