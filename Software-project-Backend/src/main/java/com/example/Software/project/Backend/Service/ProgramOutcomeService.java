package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.ProgramOutcome;
import com.example.Software.project.Backend.Repository.ProgramOutcomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProgramOutcomeService {

    @Autowired
    private ProgramOutcomeRepository poRepository;

    private String normalizePoId(String poId) {
        return poId == null ? null : poId.trim().toUpperCase();
    }

    // Keep code and POID identical to avoid dual-key drift.
    private void syncCodeWithPoId(ProgramOutcome po) {
        String normalizedPoId = normalizePoId(po.getPoId());
        po.setPoId(normalizedPoId);
        po.setCode(normalizedPoId);
    }

    // Create PO
    public ProgramOutcome createPO(ProgramOutcome po) {
        syncCodeWithPoId(po);
        validateProgramOutcome(po);
        
        // POID is the single unique identity.
        if (poRepository.existsById(po.getPoId())) {
            throw new IllegalArgumentException("Program Outcome with ID '" + po.getPoId() + "' already exists");
        }

        if (po.getIsDefault() == null) {
            po.setIsDefault(false);
        }
        if (po.getIsActive() == null) {
            po.setIsActive(true);
        }
        
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        return poRepository.save(po);
    }

    // Get all active POs
    public List<ProgramOutcome> getAllActivePOs() {
        return poRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    // Get all POs (including inactive)
    public List<ProgramOutcome> getAllPOs() {
        return poRepository.findAllByOrderByDisplayOrderAsc();
    }

    // Get PO by ID
    public Optional<ProgramOutcome> getPOById(String poId) {
        if (poId == null) {
            return Optional.empty();
        }

        String normalized = normalizePoId(poId);
        return poRepository.findById(normalized);
    }

    // Get PO by code
    public Optional<ProgramOutcome> getPOByCode(String code) {
        return poRepository.findByCode(code);
    }

    // Get default Washington Accord POs
    public List<ProgramOutcome> getDefaultPOs() {
        return poRepository.findByIsDefaultTrueOrderByDisplayOrderAsc();
    }

    // Get custom POs (non-default)
    public List<ProgramOutcome> getCustomPOs() {
        return poRepository.findByIsDefaultFalseOrderByDisplayOrderAsc();
    }

    // Update PO
    public ProgramOutcome updatePO(String poId, ProgramOutcome updateData) {
        ProgramOutcome existingPO = getPOById(poId)
            .orElseThrow(() -> new RuntimeException("Program Outcome not found with ID: " + poId));

        // Update fields if provided
        if (updateData.getTitle() != null) existingPO.setTitle(updateData.getTitle());
        if (updateData.getDescription() != null) existingPO.setDescription(updateData.getDescription());
        if (updateData.getCategory() != null) existingPO.setCategory(updateData.getCategory());
        if (updateData.getPerformanceIndicators() != null) existingPO.setPerformanceIndicators(updateData.getPerformanceIndicators());
        if (updateData.getDisplayOrder() != null) existingPO.setDisplayOrder(updateData.getDisplayOrder());
        if (updateData.getIsActive() != null) existingPO.setIsActive(updateData.getIsActive());

        // CRITICAL: Always sync code to poId - never let them diverge
        syncCodeWithPoId(existingPO);

        existingPO.setUpdatedAt(LocalDateTime.now());
        return poRepository.save(existingPO);
    }

    // Soft delete PO (set inactive)
    public void softDeletePO(String poId) {
        ProgramOutcome po = getPOById(poId)
            .orElseThrow(() -> new RuntimeException("Program Outcome not found with ID: " + poId));
        
        if (po.getIsDefault()) {
            throw new IllegalArgumentException("Cannot delete default Washington Accord Program Outcomes");
        }
        
        po.setIsActive(false);
        po.setUpdatedAt(LocalDateTime.now());
        poRepository.save(po);
    }

    // Hard delete PO (only for custom POs)
    public void hardDeletePO(String poId) {
        ProgramOutcome po = getPOById(poId)
            .orElseThrow(() -> new RuntimeException("Program Outcome not found with ID: " + poId));
        
        if (po.getIsDefault()) {
            throw new IllegalArgumentException("Cannot delete default Washington Accord Program Outcomes");
        }
        
        poRepository.delete(po);
    }

    // Restore soft-deleted PO
    public ProgramOutcome restorePO(String poId) {
        ProgramOutcome po = getPOById(poId)
            .orElseThrow(() -> new RuntimeException("Program Outcome not found with ID: " + poId));
        
        po.setIsActive(true);
        po.setUpdatedAt(LocalDateTime.now());
        return poRepository.save(po);
    }

    // Initialize default Washington Accord POs
    @Transactional
    public void initializeDefaultPOs() {
        int createdCount = 0;
        int skippedCount = 0;

        // Create 12 Washington Accord Program Outcomes
        String[][] defaultPOs = {
            {"PO1", "Engineering Knowledge", "Apply the knowledge of mathematics, science, engineering fundamentals, and an engineering specialization to the solution of complex engineering problems.", "Knowledge & Understanding", "Demonstrates mastery of engineering fundamentals, mathematics, and science principles"},
            {"PO2", "Problem Analysis", "Identify, formulate, review research literature, and analyze complex engineering problems reaching substantiated conclusions using first principles of mathematics, natural sciences, and engineering sciences.", "Intellectual Skills", "Defines and analyzes complex problems using systematic approaches"},
            {"PO3", "Design/Development of Solutions", "Design solutions for complex engineering problems and design system components or processes that meet the specified needs with appropriate consideration for the public health and safety, and the cultural, societal, and environmental considerations.", "Intellectual Skills", "Creates innovative solutions considering multiple constraints and stakeholder needs"},
            {"PO4", "Investigation", "Conduct investigations of complex problems using research-based knowledge and research methods including design of experiments, analysis and interpretation of data, and synthesis of the information to provide valid conclusions.", "Intellectual Skills", "Designs and conducts systematic investigations using appropriate methodologies"},
            {"PO5", "Modern Tool Usage", "Create, select, and apply appropriate techniques, resources, and modern engineering and IT tools including prediction and modeling to complex engineering activities with an understanding of the limitations.", "Practical Skills", "Effectively utilizes contemporary tools and technologies with awareness of limitations"},
            {"PO6", "The Engineer and Society", "Apply reasoning informed by the contextual knowledge to assess societal, health, safety, legal and cultural issues and the consequent responsibilities relevant to the professional engineering practice.", "Professional Skills", "Evaluates engineering solutions within broader societal context"},
            {"PO7", "Environment and Sustainability", "Understand the impact of the professional engineering solutions in societal and environmental contexts, and demonstrate the knowledge of, and need for sustainable development.", "Professional Skills", "Considers environmental impact and sustainability in engineering decisions"},
            {"PO8", "Ethics", "Apply ethical principles and commit to professional ethics and responsibilities and norms of the engineering practice.", "Attitudes & Values", "Demonstrates ethical reasoning and professional responsibility"},
            {"PO9", "Individual and Team Work", "Function effectively as an individual, and as a member or leader in diverse teams, and in multidisciplinary settings.", "Interpersonal Skills", "Collaborates effectively in diverse teams and demonstrates leadership capabilities"},
            {"PO10", "Communication", "Communicate effectively on complex engineering activities with the engineering community and with society at large, such as, being able to comprehend and write effective reports and design documentation, make effective presentations, and give and receive clear instructions.", "Communication Skills", "Communicates technical information effectively to various audiences"},
            {"PO11", "Project Management and Finance", "Demonstrate knowledge and understanding of the engineering and management principles and apply these to one's own work, as a member and leader in a team, to manage projects and in multidisciplinary environments.", "Management Skills", "Applies project management principles and understands financial implications"},
            {"PO12", "Life-long Learning", "Recognize the need for, and have the preparation and ability to engage in independent and life-long learning in the broadest context of technological change.", "Learning Skills", "Demonstrates commitment to continuous professional development"}
        };

        for (int i = 0; i < defaultPOs.length; i++) {
            String[] poData = defaultPOs[i];
            ProgramOutcome po = new ProgramOutcome();
            po.setPoId(poData[0]);
            String normalizedPoId = normalizePoId(poData[0]);
            if (poRepository.existsById(normalizedPoId)) {
                skippedCount++;
                continue;
            }
            po.setTitle(poData[1]);
            po.setDescription(poData[2]);
            po.setCategory(poData[3]);
            po.setPerformanceIndicators(poData[4]);
            po.setIsDefault(true);
            po.setIsActive(true);
            po.setDisplayOrder(i + 1);
            po.setCreatedBy("SYSTEM");
            syncCodeWithPoId(po);
            
            poRepository.save(po);
            createdCount++;
            System.out.println("Initialized default PO: " + po.getPoId() + " - " + po.getTitle());
        }
        
        System.out.println("Washington Accord defaults initialization complete. Created: " + createdCount + ", skipped(existing): " + skippedCount);
    }

    // Validation method
    private void validateProgramOutcome(ProgramOutcome po) {
        if (po.getPoId() == null || po.getPoId().trim().isEmpty()) {
            throw new IllegalArgumentException("PO ID is required");
        }
        if (po.getTitle() == null || po.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("PO Title is required");
        }
        if (po.getDescription() == null || po.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("PO Description is required");
        }
    }

    // Reorder POs
    public void reorderPOs(List<String> poIds) {
        for (int i = 0; i < poIds.size(); i++) {
            String poId = normalizePoId(poIds.get(i));
            Optional<ProgramOutcome> poOpt = poRepository.findById(poId);
            if (poOpt.isPresent()) {
                ProgramOutcome po = poOpt.get();
                po.setDisplayOrder(i + 1);
                po.setUpdatedAt(LocalDateTime.now());
                poRepository.save(po);
            }
        }
    }
}