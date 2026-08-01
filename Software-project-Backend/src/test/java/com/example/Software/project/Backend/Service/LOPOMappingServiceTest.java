package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LOPOMappingService's approval workflow and mapping-rule validation
 * (min/max mapping count, mandatory primary-focus weight, weight range).
 */
@DisplayName("LOPOMappingService Tests")
class LOPOMappingServiceTest {

    @Mock
    private OutcomeMappingRepository mappingRepository;

    @Mock
    private ProgramOutcomeRepository poRepository;

    @Mock
    private LosRepository losRepository;

    @InjectMocks
    private LOPOMappingService lopoMappingService;

    private Los los1;
    private ProgramOutcome po1;
    private ProgramOutcome po2;
    private ProgramOutcome po3;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        los1 = new Los();
        los1.setId("LO001");
        los1.setName("Learning Outcome 1");

        po1 = new ProgramOutcome();
        po1.setId("PO001");
        po1.setPoId("PO1");

        po2 = new ProgramOutcome();
        po2.setId("PO002");
        po2.setPoId("PO2");

        po3 = new ProgramOutcome();
        po3.setId("PO003");
        po3.setPoId("PO3");

        when(mappingRepository.save(any(OutcomeMapping.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---- createMappings validation ----

    @Test
    @DisplayName("createMappings rejects fewer than 2 PO mappings")
    void createMappings_rejectsFewerThanTwoMappings() {
        when(losRepository.findById("LO001")).thenReturn(Optional.of(los1));
        Map<String, Integer> mappings = new HashMap<>(Map.of("PO1", 3));

        assertThrows(IllegalArgumentException.class,
            () -> lopoMappingService.createMappings("LO001", mappings, "lecturer1", "remarks"));
    }

    @Test
    @DisplayName("createMappings rejects more than 5 PO mappings")
    void createMappings_rejectsMoreThanFiveMappings() {
        when(losRepository.findById("LO001")).thenReturn(Optional.of(los1));
        Map<String, Integer> mappings = new HashMap<>(Map.of(
            "PO1", 3, "PO2", 1, "PO3", 1, "PO4", 1, "PO5", 1, "PO6", 1
        ));

        assertThrows(IllegalArgumentException.class,
            () -> lopoMappingService.createMappings("LO001", mappings, "lecturer1", "remarks"));
    }

    @Test
    @DisplayName("createMappings rejects mappings with no primary-focus (weight 3) entry")
    void createMappings_rejectsWithoutPrimaryFocus() {
        when(losRepository.findById("LO001")).thenReturn(Optional.of(los1));
        Map<String, Integer> mappings = new HashMap<>(Map.of("PO1", 1, "PO2", 2));

        assertThrows(IllegalArgumentException.class,
            () -> lopoMappingService.createMappings("LO001", mappings, "lecturer1", "remarks"));
    }

    @Test
    @DisplayName("createMappings rejects a weight outside the 1-3 range")
    void createMappings_rejectsWeightOutsideRange() {
        when(losRepository.findById("LO001")).thenReturn(Optional.of(los1));
        Map<String, Integer> mappings = new HashMap<>(Map.of("PO1", 3, "PO2", 4));

        assertThrows(IllegalArgumentException.class,
            () -> lopoMappingService.createMappings("LO001", mappings, "lecturer1", "remarks"));
    }

    @Test
    @DisplayName("createMappings throws when the Learning Outcome does not exist")
    void createMappings_throwsWhenLoNotFound() {
        when(losRepository.findById("LO999")).thenReturn(Optional.empty());
        Map<String, Integer> mappings = new HashMap<>(Map.of("PO1", 3, "PO2", 1));

        assertThrows(RuntimeException.class,
            () -> lopoMappingService.createMappings("LO999", mappings, "lecturer1", "remarks"));
    }

    @Test
    @DisplayName("createMappings saves one PENDING mapping per PO for a valid request")
    void createMappings_savesPendingMappingsForValidRequest() {
        when(losRepository.findById("LO001")).thenReturn(Optional.of(los1));
        when(poRepository.findById("PO1")).thenReturn(Optional.of(po1));
        when(poRepository.findById("PO2")).thenReturn(Optional.of(po2));
        Map<String, Integer> mappings = new HashMap<>(Map.of("PO1", 3, "PO2", 1));

        List<OutcomeMapping> result = lopoMappingService.createMappings("LO001", mappings, "lecturer1", "remarks");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(m -> m.getStatus() == OutcomeMapping.ApprovalStatus.PENDING));
        assertTrue(result.stream().allMatch(m -> "lecturer1".equals(m.getMappedBy())));
        verify(mappingRepository, times(2)).save(any(OutcomeMapping.class));
    }

    // ---- approve / reject ----

    @Test
    @DisplayName("approveMapping sets status APPROVED and records reviewer info")
    void approveMapping_setsApprovedAndReviewer() {
        OutcomeMapping mapping = new OutcomeMapping(los1, po1, 3, "lecturer1");
        mapping.setId(1L);
        when(mappingRepository.findById(1L)).thenReturn(Optional.of(mapping));

        OutcomeMapping result = lopoMappingService.approveMapping(1L, "admin1", "looks good");

        assertEquals(OutcomeMapping.ApprovalStatus.APPROVED, result.getStatus());
        assertEquals("admin1", result.getReviewedBy());
        assertEquals("looks good", result.getAdminRemarks());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    @DisplayName("rejectMapping sets status REJECTED and records reviewer info")
    void rejectMapping_setsRejectedAndReviewer() {
        OutcomeMapping mapping = new OutcomeMapping(los1, po1, 3, "lecturer1");
        mapping.setId(1L);
        when(mappingRepository.findById(1L)).thenReturn(Optional.of(mapping));

        OutcomeMapping result = lopoMappingService.rejectMapping(1L, "admin1", "insufficient evidence");

        assertEquals(OutcomeMapping.ApprovalStatus.REJECTED, result.getStatus());
        assertEquals("admin1", result.getReviewedBy());
        assertEquals("insufficient evidence", result.getAdminRemarks());
    }

    @Test
    @DisplayName("approveMapping throws when the mapping does not exist")
    void approveMapping_throwsWhenNotFound() {
        when(mappingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> lopoMappingService.approveMapping(99L, "admin1", "n/a"));
    }

    @Test
    @DisplayName("bulkApproveMappingsForLO only approves mappings that are still PENDING")
    void bulkApprove_onlyApprovesPendingMappings() {
        OutcomeMapping pending = new OutcomeMapping(los1, po1, 3, "lecturer1");
        pending.setId(1L);
        pending.setStatus(OutcomeMapping.ApprovalStatus.PENDING);

        OutcomeMapping alreadyApproved = new OutcomeMapping(los1, po2, 2, "lecturer1");
        alreadyApproved.setId(2L);
        alreadyApproved.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);

        OutcomeMapping rejected = new OutcomeMapping(los1, po3, 1, "lecturer1");
        rejected.setId(3L);
        rejected.setStatus(OutcomeMapping.ApprovalStatus.REJECTED);

        when(mappingRepository.findByLearningOutcome_Id("LO001"))
            .thenReturn(Arrays.asList(pending, alreadyApproved, rejected));

        List<OutcomeMapping> result = lopoMappingService.bulkApproveMappingsForLO("LO001", "admin1", "bulk ok");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(OutcomeMapping.ApprovalStatus.APPROVED, pending.getStatus());
        assertEquals(OutcomeMapping.ApprovalStatus.APPROVED, alreadyApproved.getStatus(), "was already approved, stays approved");
        assertEquals(OutcomeMapping.ApprovalStatus.REJECTED, rejected.getStatus(), "rejected mappings are not touched by bulk approve");
        verify(mappingRepository, times(1)).save(any(OutcomeMapping.class));
    }

    // ---- update / delete guardrails ----

    @Test
    @DisplayName("updateMapping refuses to change an already-approved mapping")
    void updateMapping_refusesApprovedMapping() {
        OutcomeMapping mapping = new OutcomeMapping(los1, po1, 3, "lecturer1");
        mapping.setId(1L);
        mapping.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        when(mappingRepository.findById(1L)).thenReturn(Optional.of(mapping));

        assertThrows(IllegalArgumentException.class,
            () -> lopoMappingService.updateMapping(1L, 2, "trying to sneak a change in"));
    }

    @Test
    @DisplayName("updateMapping resubmits a rejected mapping as PENDING and clears the prior review")
    void updateMapping_resubmitsRejectedMappingAsPending() {
        OutcomeMapping mapping = new OutcomeMapping(los1, po1, 1, "lecturer1");
        mapping.setId(1L);
        mapping.setStatus(OutcomeMapping.ApprovalStatus.REJECTED);
        mapping.setReviewedBy("admin1");
        mapping.setAdminRemarks("not enough justification");
        when(mappingRepository.findById(1L)).thenReturn(Optional.of(mapping));

        OutcomeMapping result = lopoMappingService.updateMapping(1L, 3, "added more justification");

        assertEquals(OutcomeMapping.ApprovalStatus.PENDING, result.getStatus());
        assertNull(result.getReviewedBy());
        assertNull(result.getReviewedAt());
        assertNull(result.getAdminRemarks());
        assertEquals(3, result.getWeight());
    }

    @Test
    @DisplayName("deleteMapping refuses to delete an already-approved mapping")
    void deleteMapping_refusesApprovedMapping() {
        OutcomeMapping mapping = new OutcomeMapping(los1, po1, 3, "lecturer1");
        mapping.setId(1L);
        mapping.setStatus(OutcomeMapping.ApprovalStatus.APPROVED);
        when(mappingRepository.findById(1L)).thenReturn(Optional.of(mapping));

        assertThrows(IllegalArgumentException.class, () -> lopoMappingService.deleteMapping(1L));
        verify(mappingRepository, never()).delete(any(OutcomeMapping.class));
    }

    @Test
    @DisplayName("deleteMapping deletes a pending mapping")
    void deleteMapping_deletesPendingMapping() {
        OutcomeMapping mapping = new OutcomeMapping(los1, po1, 3, "lecturer1");
        mapping.setId(1L);
        mapping.setStatus(OutcomeMapping.ApprovalStatus.PENDING);
        when(mappingRepository.findById(1L)).thenReturn(Optional.of(mapping));

        lopoMappingService.deleteMapping(1L);

        verify(mappingRepository, times(1)).delete(mapping);
    }
}
