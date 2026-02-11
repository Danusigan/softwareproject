package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Mapping;
import com.example.Software.project.Backend.Model.LosPos;
import com.example.Software.project.Backend.Model.ProgramOutcome;
import com.example.Software.project.Backend.Model.Mapping.MappingStatus;
import com.example.Software.project.Backend.Repository.MappingRepository;
import com.example.Software.project.Backend.Repository.LosPosRepository;
import com.example.Software.project.Backend.Repository.ProgramOutcomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MappingService {
    
    @Autowired
    private MappingRepository mappingRepository;
    
    @Autowired
    private LosPosRepository losPosRepository;
    
    @Autowired
    private ProgramOutcomeRepository programOutcomeRepository;
    
    // Create new LO-PO mapping
    public Mapping createMapping(String losPosId, Long programOutcomeId, Integer weight, String mappedBy, String lecturerRemarks) {
        // Validate LosPos exists
        Optional<LosPos> losPos = losPosRepository.findById(losPosId);
        if (losPos.isEmpty()) {
            throw new RuntimeException("Learning Outcome not found with ID: " + losPosId);
        }
        
        // Validate ProgramOutcome exists and is active
        Optional<ProgramOutcome> programOutcome = programOutcomeRepository.findById(programOutcomeId);
        if (programOutcome.isEmpty()) {
            throw new RuntimeException("Program Outcome not found with ID: " + programOutcomeId);
        }
        if (!programOutcome.get().getIsActive()) {
            throw new RuntimeException("Cannot map to inactive Program Outcome");
        }
        
        // Check if mapping already exists
        if (mappingRepository.existsByLosPosAndProgramOutcome(losPos.get(), programOutcome.get())) {
            throw new RuntimeException("Mapping already exists for this LO-PO combination");
        }
        
        // Validate weight
        if (weight == null || weight < 0 || weight > 3) {
            throw new RuntimeException("Weight must be between 0 and 3 (0=No correlation, 1=Low, 2=Medium, 3=High)");
        }
        
        Mapping mapping = new Mapping(losPos.get(), programOutcome.get(), weight, mappedBy);
        mapping.setLecturerRemarks(lecturerRemarks);
        
        return mappingRepository.save(mapping);
    }
    
    // Get all mappings
    public List<Mapping> getAllMappings() {
        return mappingRepository.findAll();
    }
    
    // Get mappings by LosPos ID
    public List<Mapping> getMappingsByLosPosId(String losPosId) {
        return mappingRepository.findByLosPosId(losPosId);
    }
    
    // Get mappings by Program Outcome ID
    public List<Mapping> getMappingsByProgramOutcomeId(Long programOutcomeId) {
        return mappingRepository.findByProgramOutcomeId(programOutcomeId);
    }
    
    // Get mapping by ID
    public Optional<Mapping> getMappingById(Long id) {
        return mappingRepository.findById(id);
    }
    
    // Get pending mappings for admin review
    public List<Mapping> getPendingMappingsForReview() {
        return mappingRepository.findPendingMappingsForReview();
    }
    
    // Get mappings by status
    public List<Mapping> getMappingsByStatus(MappingStatus status) {
        return mappingRepository.findByStatusOrderByMappedAtAsc(status);
    }
    
    // Get mappings by lecturer
    public List<Mapping> getMappingsByLecturer(String lecturerEmail) {
        return mappingRepository.findByMappedBy(lecturerEmail);
    }
    
    // Update mapping (only for pending mappings by original lecturer)
    public Mapping updateMapping(Long id, Integer weight, String lecturerRemarks, String requestingUser) {
        Optional<Mapping> optionalMapping = mappingRepository.findById(id);
        if (optionalMapping.isEmpty()) {
            throw new RuntimeException("Mapping not found with ID: " + id);
        }
        
        Mapping mapping = optionalMapping.get();
        
        // Check if user is the original mapper and mapping is still pending
        if (!mapping.getMappedBy().equals(requestingUser)) {
            throw new RuntimeException("You can only update your own mappings");
        }
        if (mapping.getStatus() != MappingStatus.PENDING) {
            throw new RuntimeException("Can only update pending mappings");
        }
        
        // Validate weight
        if (weight != null && (weight < 0 || weight > 3)) {
            throw new RuntimeException("Weight must be between 0 and 3");
        }
        
        if (weight != null) mapping.setWeight(weight);
        if (lecturerRemarks != null) mapping.setLecturerRemarks(lecturerRemarks);
        
        return mappingRepository.save(mapping);
    }
    
    // Approve mapping (Admin only)
    public Mapping approveMapping(Long id, String reviewedBy, String adminRemarks) {
        Optional<Mapping> optionalMapping = mappingRepository.findById(id);
        if (optionalMapping.isEmpty()) {
            throw new RuntimeException("Mapping not found with ID: " + id);
        }
        
        Mapping mapping = optionalMapping.get();
        if (mapping.getStatus() != MappingStatus.PENDING) {
            throw new RuntimeException("Can only approve pending mappings");
        }
        
        mapping.approve(reviewedBy, adminRemarks);
        return mappingRepository.save(mapping);
    }
    
    // Reject mapping (Admin only)
    public Mapping rejectMapping(Long id, String reviewedBy, String adminRemarks) {
        Optional<Mapping> optionalMapping = mappingRepository.findById(id);
        if (optionalMapping.isEmpty()) {
            throw new RuntimeException("Mapping not found with ID: " + id);
        }
        
        Mapping mapping = optionalMapping.get();
        if (mapping.getStatus() != MappingStatus.PENDING) {
            throw new RuntimeException("Can only reject pending mappings");
        }
        
        if (adminRemarks == null || adminRemarks.trim().isEmpty()) {
            throw new RuntimeException("Admin remarks are required for rejection");
        }
        
        mapping.reject(reviewedBy, adminRemarks);
        return mappingRepository.save(mapping);
    }
    
    // Delete mapping (only by original lecturer and only if pending)
    public void deleteMapping(Long id, String requestingUser) {
        Optional<Mapping> optionalMapping = mappingRepository.findById(id);
        if (optionalMapping.isEmpty()) {
            throw new RuntimeException("Mapping not found with ID: " + id);
        }
        
        Mapping mapping = optionalMapping.get();
        
        // Check if user is the original mapper and mapping is still pending
        if (!mapping.getMappedBy().equals(requestingUser)) {
            throw new RuntimeException("You can only delete your own mappings");
        }
        if (mapping.getStatus() != MappingStatus.PENDING) {
            throw new RuntimeException("Can only delete pending mappings");
        }
        
        mappingRepository.deleteById(id);
    }
    
    // Get approved mappings for OBE calculation
    public List<Mapping> getApprovedMappingsForOBE(String losPosId) {
        return mappingRepository.findApprovedMappingsByLosPosId(losPosId);
    }
    
    // Get approved mappings by module
    public List<Mapping> getApprovedMappingsByModule(String moduleCode) {
        return mappingRepository.findApprovedMappingsByModuleCode(moduleCode);
    }
    
    // Get high correlation mappings (weight = 3) for a module
    public List<Mapping> getHighCorrelationMappingsByModule(String moduleCode) {
        return mappingRepository.findHighCorrelationMappingsByModuleCode(moduleCode);
    }
    
    // Get mapping statistics
    public long getPendingMappingsCount() {
        return mappingRepository.countByStatus(MappingStatus.PENDING);
    }
    
    public long getApprovedMappingsCount() {
        return mappingRepository.countByStatus(MappingStatus.APPROVED);
    }
    
    public long getRejectedMappingsCount() {
        return mappingRepository.countByStatus(MappingStatus.REJECTED);
    }
    
    public long getMappingsCountByLecturer(String lecturerEmail) {
        return mappingRepository.countByMappedBy(lecturerEmail);
    }
    
    // Get mappings requiring urgent review (pending for more than 7 days)
    public List<Mapping> getMappingsRequiringUrgentReview() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return mappingRepository.findMappingsRequiringUrgentReview(sevenDaysAgo);
    }
    
    // Get recent mappings (within last 30 days)
    public List<Mapping> getRecentMappings() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return mappingRepository.findRecentMappings(thirtyDaysAgo);
    }
    
    // Bulk approve mappings (Admin only)
    @Transactional
    public List<Mapping> bulkApproveMappings(List<Long> mappingIds, String reviewedBy, String adminRemarks) {
        return mappingIds.stream()
                .map(id -> approveMapping(id, reviewedBy, adminRemarks))
                .toList();
    }
    
    // Get duplicate mappings for cleanup
    public List<Mapping> getDuplicateMappings() {
        return mappingRepository.findDuplicateMappings();
    }
    
    // Get mapping statistics by lecturer
    public List<Object[]> getMappingStatisticsByLecturer() {
        return mappingRepository.getMappingStatisticsByLecturer();
    }
    
    // Validate mapping weight description
    public String getWeightDescription(Integer weight) {
        return switch (weight) {
            case 0 -> "No Correlation";
            case 1 -> "Low Correlation";
            case 2 -> "Medium Correlation";
            case 3 -> "High Correlation";
            default -> "Invalid Weight";
        };
    }
}