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

    // Create (Lecture) - Add to LosPos
    public LosPos addAssignmentToLosPos(String losPosId, Assignment assignment, MultipartFile file) throws Exception {
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

        // Save assignment first
        Assignment savedAssignment = assignmentRepository.save(assignment);

        // Link assignment to LosPos
        losPos.setAssignment(savedAssignment);
        return losPosRepository.save(losPos);
    }

    // Read All Assignments (Global)
    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    // Read Assignment by LosPos ID
    public Optional<Assignment> getAssignmentByLosPosId(String losPosId) {
        Optional<LosPos> losPos = losPosRepository.findById(losPosId);
        if (losPos.isPresent() && losPos.get().getAssignment() != null) {
            return Optional.of(losPos.get().getAssignment());
        }
        return Optional.empty();
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
        List<LosPos> allLosPos = losPosRepository.findAll();
        for (LosPos lp : allLosPos) {
            if (lp.getAssignment() != null && lp.getAssignment().getAssignmentId().equals(id)) {
                lp.setAssignment(null);
                losPosRepository.save(lp);
            }
        }
        
        assignmentRepository.deleteById(id);
    }
}