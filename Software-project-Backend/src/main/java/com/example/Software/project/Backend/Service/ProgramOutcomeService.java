package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.ProgramOutcome;
import com.example.Software.project.Backend.Repository.ProgramOutcomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProgramOutcomeService {
    
    @Autowired
    private ProgramOutcomeRepository programOutcomeRepository;
    
    // Create new Program Outcome
    public ProgramOutcome createProgramOutcome(String poCode, String poDescription, String createdBy) {
        if (programOutcomeRepository.existsByPoCode(poCode)) {
            throw new RuntimeException("Program Outcome with code " + poCode + " already exists");
        }
        
        ProgramOutcome programOutcome = new ProgramOutcome(poCode, poDescription);
        programOutcome.setCreatedBy(createdBy);
        return programOutcomeRepository.save(programOutcome);
    }
    
    // Get all active Program Outcomes
    public List<ProgramOutcome> getAllActiveProgramOutcomes() {
        return programOutcomeRepository.findByIsActiveTrueOrderByPoCode();
    }
    
    // Get all Program Outcomes (including inactive)
    public List<ProgramOutcome> getAllProgramOutcomes() {
        return programOutcomeRepository.findAll();
    }
    
    // Get Program Outcome by ID
    public Optional<ProgramOutcome> getProgramOutcomeById(Long id) {
        return programOutcomeRepository.findById(id);
    }
    
    // Get Program Outcome by PO Code
    public Optional<ProgramOutcome> getProgramOutcomeByCode(String poCode) {
        return programOutcomeRepository.findByPoCode(poCode);
    }
    
    // Update Program Outcome
    public ProgramOutcome updateProgramOutcome(Long id, String poDescription) {
        Optional<ProgramOutcome> optionalPO = programOutcomeRepository.findById(id);
        if (optionalPO.isPresent()) {
            ProgramOutcome programOutcome = optionalPO.get();
            programOutcome.setPoDescription(poDescription);
            return programOutcomeRepository.save(programOutcome);
        }
        throw new RuntimeException("Program Outcome not found with ID: " + id);
    }
    
    // Soft delete Program Outcome (deactivate)
    public void deactivateProgramOutcome(Long id) {
        Optional<ProgramOutcome> optionalPO = programOutcomeRepository.findById(id);
        if (optionalPO.isPresent()) {
            ProgramOutcome programOutcome = optionalPO.get();
            programOutcome.setIsActive(false);
            programOutcomeRepository.save(programOutcome);
        } else {
            throw new RuntimeException("Program Outcome not found with ID: " + id);
        }
    }
    
    // Activate Program Outcome
    public void activateProgramOutcome(Long id) {
        Optional<ProgramOutcome> optionalPO = programOutcomeRepository.findById(id);
        if (optionalPO.isPresent()) {
            ProgramOutcome programOutcome = optionalPO.get();
            programOutcome.setIsActive(true);
            programOutcomeRepository.save(programOutcome);
        } else {
            throw new RuntimeException("Program Outcome not found with ID: " + id);
        }
    }
    
    // Hard delete Program Outcome (only if no mappings exist)
    public void deleteProgramOutcome(Long id) {
        Optional<ProgramOutcome> optionalPO = programOutcomeRepository.findById(id);
        if (optionalPO.isPresent()) {
            ProgramOutcome programOutcome = optionalPO.get();
            // Check if PO has any mappings
            if (programOutcome.getMappings() != null && !programOutcome.getMappings().isEmpty()) {
                throw new RuntimeException("Cannot delete Program Outcome with existing mappings. Deactivate instead.");
            }
            programOutcomeRepository.deleteById(id);
        } else {
            throw new RuntimeException("Program Outcome not found with ID: " + id);
        }
    }
    
    // Initialize Default Washington Accord Program Outcomes
    public List<ProgramOutcome> initializeDefaultProgramOutcomes(String createdBy) {
        List<ProgramOutcome> defaultPOs = List.of(
            ProgramOutcome.createDefaultPO("PO1", 
                "Engineering knowledge: Apply knowledge of mathematics, science, engineering fundamentals and engineering specialization to solve complex engineering problems.", 
                createdBy),
            ProgramOutcome.createDefaultPO("PO2", 
                "Problem analysis: Identify, formulate, research literature, and analyze complex engineering problems reaching substantiated conclusions using first principles of mathematics, natural sciences, and engineering sciences.", 
                createdBy),
            ProgramOutcome.createDefaultPO("PO3", 
                "Design/development of solutions: Design solutions for complex engineering problems and design system components or processes that meet the specified needs with appropriate consideration for the public health and safety, and the cultural, societal, and environmental considerations.", 
                createdBy),
            ProgramOutcome.createDefaultPO("PO4", 
                "Conduct investigations of complex problems: Use research-based knowledge and research methods including design of experiments, analysis and interpretation of data, and synthesis of the information to provide valid conclusions.", 
                createdBy),
            ProgramOutcome.createDefaultPO("PO5", 
                "Modern tool usage: Create, select, and apply appropriate techniques, resources, and modern engineering and IT tools including prediction and modeling to complex engineering activities with an understanding of the limitations.", 
                createdBy),
            ProgramOutcome.createDefaultPO("PO6", 
                "The engineer and society: Apply reasoning informed by the contextual knowledge to assess societal, health, safety, legal and cultural issues and the consequent responsibilities relevant to the professional engineering practice.", 
                createdBy),
            ProgramOutcome.createDefaultPO("PO7", 
                "Environment and sustainability: Understand the impact of the professional engineering solutions in societal and environmental contexts, and demonstrate the knowledge of, and need for sustainable development.", 
                createdBy),
            ProgramOutcome.createDefaultPO("PO8", 
                "Ethics: Apply ethical principles and commit to professional ethics and responsibilities and norms of the engineering practice.", 
                createdBy),
            ProgramOutcome.createDefaultPO("PO9", 
                "Individual and team work: Function effectively as an individual, and as a member or leader in diverse teams, and in multidisciplinary settings.", 
                createdBy),
            ProgramOutcome.createDefaultPO("PO10", 
                "Communication: Communicate effectively on complex engineering activities with the engineering community and with society at large, such as, being able to comprehend and write effective reports and design documentation, make effective presentations, and give and receive clear instructions.", 
                createdBy),
            ProgramOutcome.createDefaultPO("PO11", 
                "Project management and finance: Demonstrate knowledge and understanding of the engineering and management principles and apply these to one's own work, as a member and leader in a team, to manage projects and in multidisciplinary environments.", 
                createdBy),
            ProgramOutcome.createDefaultPO("PO12", 
                "Life-long learning: Recognize the need for, and have the preparation and ability to engage in independent and life-long learning in the broadest context of technological change.", 
                createdBy)
        );
        
        // Save only POs that don't already exist
        return defaultPOs.stream()
                .filter(po -> !programOutcomeRepository.existsByPoCode(po.getPoCode()))
                .map(programOutcomeRepository::save)
                .toList();
    }
    
    // Get default Program Outcomes
    public List<ProgramOutcome> getDefaultProgramOutcomes() {
        return programOutcomeRepository.findByIsDefaultTrueAndIsActiveTrue();
    }
    
    // Get custom Program Outcomes
    public List<ProgramOutcome> getCustomProgramOutcomes() {
        return programOutcomeRepository.findByIsDefaultFalseAndIsActiveTrue();
    }
    
    // Search Program Outcomes
    public List<ProgramOutcome> searchProgramOutcomes(String searchTerm) {
        List<ProgramOutcome> codeResults = programOutcomeRepository.findByPoCodeContainingIgnoreCaseAndIsActiveTrue(searchTerm);
        List<ProgramOutcome> descResults = programOutcomeRepository.findByDescriptionContainingIgnoreCaseAndIsActiveTrue(searchTerm);
        
        // Combine and deduplicate results
        codeResults.addAll(descResults);
        return codeResults.stream().distinct().toList();
    }
    
    // Get Program Outcomes by Module
    public List<ProgramOutcome> getProgramOutcomesByModule(String moduleCode) {
        return programOutcomeRepository.findByModuleCode(moduleCode);
    }
    
    // Get Program Outcomes with approved mappings
    public List<ProgramOutcome> getProgramOutcomesWithApprovedMappings() {
        return programOutcomeRepository.findWithApprovedMappings();
    }
    
    // Get statistics
    public long getTotalActiveProgramOutcomes() {
        return programOutcomeRepository.countByIsActiveTrue();
    }
    
    public long getTotalDefaultProgramOutcomes() {
        return programOutcomeRepository.countByIsDefaultTrue();
    }
    
    public long getTotalCustomProgramOutcomes() {
        return programOutcomeRepository.countByIsDefaultFalse();
    }
}