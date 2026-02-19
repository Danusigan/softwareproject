package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Assignment;
import com.example.Software.project.Backend.Model.Los;
import com.example.Software.project.Backend.Repository.AssignmentRepository;
import com.example.Software.project.Backend.Repository.LosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private LosRepository losRepository;

    @Autowired
    private ExcelImportService excelImportService;

    // Create (Lecture) - Add to Los
    @Transactional
    public Assignment addAssignmentToLos(String losId, Assignment assignment, MultipartFile file) throws Exception {
        Optional<Los> losOptional = losRepository.findById(losId);
        if (losOptional.isEmpty()) {
            throw new Exception("Los not found");
        }
        if (assignmentRepository.existsById(assignment.getAssignmentId())) {
            throw new Exception("Assignment ID already exists");
        }

        Los los = losOptional.get();

        // Process file if present (Optional)
        if (file != null && !file.isEmpty()) {
            assignment.setMarksCsvFile(file.getBytes());
            assignment.setFileName(file.getOriginalFilename());
        }

        // Link assignment to LosPos (Many assignments pointing to one LosPos)
        assignment.setLosPos(losPos);

        // Save assignment
        Assignment savedAssignment = assignmentRepository.save(assignment);

        // Link assignment to Los (Los is the owner)
        los.setAssignment(savedAssignment);
        losRepository.save(los);

        return savedAssignment;
    }

    // Read All Assignments (Global)
    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    // Read Assignment by Los ID
    public Optional<Assignment> getAssignmentByLosId(String losId) {
        Optional<Los> los = losRepository.findById(losId);
        if (los.isPresent() && los.get().getAssignment() != null) {
            return Optional.of(los.get().getAssignment());
        }
        return List.of();
    }

    // Read One Assignment
    public Optional<Assignment> getAssignmentById(String id) {
        return assignmentRepository.findById(id);
    }

    // Update Assignment (Lecture)
    @Transactional
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
    @Transactional
    public void deleteAssignment(String id) throws Exception {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new Exception("Assignment not found"));

        // Unlink from Los before deleting
        if (assignment.getLos() != null) {
            Los los = assignment.getLos();
            los.setAssignment(null);
            losRepository.save(los);
        } else {
            // Fallback search if bidirectional link isn't set
            List<Los> allLos = losRepository.findAll();
            for (Los lp : allLos) {
                if (lp.getAssignment() != null && lp.getAssignment().getAssignmentId().equals(id)) {
                    lp.setAssignment(null);
                    losRepository.save(lp);
                    break;
                }
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
