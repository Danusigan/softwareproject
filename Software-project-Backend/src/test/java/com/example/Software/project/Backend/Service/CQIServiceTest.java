package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CQIService.checkAndTriggerCQI — the auto-flag logic that opens a
 * Continuous Quality Improvement action whenever an LO's batch attainment misses its
 * target, and must not spam a duplicate action while one is already open.
 */
@DisplayName("CQIService.checkAndTriggerCQI Tests")
class CQIServiceTest {

    @Mock
    private CqiActionRepository cqiActionRepository;
    @Mock
    private LosRepository losRepository;
    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private AttainmentService attainmentService;

    @InjectMocks
    private CQIService cqiService;

    private Module module;
    private Los los1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        module = new Module();
        module.setModuleId("MOD1");

        los1 = new Los();
        los1.setId("LO001");

        when(moduleRepository.findById("MOD1")).thenReturn(Optional.of(module));
        when(cqiActionRepository.save(any(CqiAction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("triggers a PLANNED action when batch attainment is below the LO's threshold")
    void triggersActionWhenBelowThreshold() {
        los1.setAttainmentThreshold(60.0);
        when(losRepository.findByModule_ModuleId("MOD1")).thenReturn(List.of(los1));
        when(attainmentService.calculateLoAttainmentForBatch("LO001", "20", 60.0)).thenReturn(45.0);
        when(cqiActionRepository.findByModule_ModuleIdAndLos_IdAndStatusIn(eq("MOD1"), eq("LO001"), anyList()))
            .thenReturn(Collections.emptyList());

        List<CqiAction> triggered = cqiService.checkAndTriggerCQI("MOD1", "20");

        assertEquals(1, triggered.size());
        CqiAction action = triggered.get(0);
        assertEquals(CqiStatus.PLANNED, action.getStatus());
        assertEquals(45.0, action.getAttainmentScore());
        assertEquals(60.0, action.getTargetScore());
        assertFalse(action.isSubmitted());
    }

    @Test
    @DisplayName("does not trigger an action when attainment meets or exceeds the threshold")
    void doesNotTriggerWhenAttainmentMeetsThreshold() {
        los1.setAttainmentThreshold(50.0);
        when(losRepository.findByModule_ModuleId("MOD1")).thenReturn(List.of(los1));
        when(attainmentService.calculateLoAttainmentForBatch("LO001", "20", 50.0)).thenReturn(50.0);

        List<CqiAction> triggered = cqiService.checkAndTriggerCQI("MOD1", "20");

        assertTrue(triggered.isEmpty());
        verify(cqiActionRepository, never()).save(any());
    }

    @Test
    @DisplayName("does not trigger an action when there are no marks yet for the batch")
    void doesNotTriggerWhenAttainmentIsNull() {
        los1.setAttainmentThreshold(50.0);
        when(losRepository.findByModule_ModuleId("MOD1")).thenReturn(List.of(los1));
        when(attainmentService.calculateLoAttainmentForBatch("LO001", "20", 50.0)).thenReturn(null);

        List<CqiAction> triggered = cqiService.checkAndTriggerCQI("MOD1", "20");

        assertTrue(triggered.isEmpty());
        verify(cqiActionRepository, never()).save(any());
    }

    @Test
    @DisplayName("does not open a duplicate action when one is already PLANNED or IN_PROGRESS for the LO")
    void doesNotDuplicateAnAlreadyOpenAction() {
        los1.setAttainmentThreshold(50.0);
        when(losRepository.findByModule_ModuleId("MOD1")).thenReturn(List.of(los1));
        when(attainmentService.calculateLoAttainmentForBatch("LO001", "20", 50.0)).thenReturn(30.0);

        CqiAction existing = new CqiAction();
        existing.setStatus(CqiStatus.IN_PROGRESS);
        when(cqiActionRepository.findByModule_ModuleIdAndLos_IdAndStatusIn(eq("MOD1"), eq("LO001"), anyList()))
            .thenReturn(List.of(existing));

        List<CqiAction> triggered = cqiService.checkAndTriggerCQI("MOD1", "20");

        assertTrue(triggered.isEmpty(), "must not open a second action while one is already open");
        verify(cqiActionRepository, never()).save(any());
    }

    @Test
    @DisplayName("defaults the threshold to 50% when the LO has no explicit attainmentThreshold")
    void defaultsThresholdWhenLoHasNone() {
        los1.setAttainmentThreshold(null);
        when(losRepository.findByModule_ModuleId("MOD1")).thenReturn(List.of(los1));
        when(attainmentService.calculateLoAttainmentForBatch("LO001", "20", 50.0)).thenReturn(40.0);
        when(cqiActionRepository.findByModule_ModuleIdAndLos_IdAndStatusIn(eq("MOD1"), eq("LO001"), anyList()))
            .thenReturn(Collections.emptyList());

        List<CqiAction> triggered = cqiService.checkAndTriggerCQI("MOD1", "20");

        assertEquals(1, triggered.size());
        assertEquals(50.0, triggered.get(0).getTargetScore());
    }

    @Test
    @DisplayName("assigns the first assigned lecturer as createdBy on the new action")
    void assignsFirstLecturerAsCreatedBy() {
        los1.setAttainmentThreshold(50.0);
        User lecturer1 = new User("lect1", "lect1@example.com", "pw", "lecture");
        User lecturer2 = new User("lect2", "lect2@example.com", "pw", "lecture");
        module.setAssignedLecturers(List.of(lecturer1, lecturer2));

        when(losRepository.findByModule_ModuleId("MOD1")).thenReturn(List.of(los1));
        when(attainmentService.calculateLoAttainmentForBatch("LO001", "20", 50.0)).thenReturn(30.0);
        when(cqiActionRepository.findByModule_ModuleIdAndLos_IdAndStatusIn(eq("MOD1"), eq("LO001"), anyList()))
            .thenReturn(Collections.emptyList());

        List<CqiAction> triggered = cqiService.checkAndTriggerCQI("MOD1", "20");

        assertEquals("lect1", triggered.get(0).getCreatedBy());
    }

    @Test
    @DisplayName("throws when the module does not exist")
    void throwsWhenModuleNotFound() {
        when(moduleRepository.findById("MOD_MISSING")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cqiService.checkAndTriggerCQI("MOD_MISSING", "20"));
    }

    // ---- linkNextSemesterResult ----

    @Test
    @DisplayName("linkNextSemesterResult closes the loop (COMPLETED) once the next semester meets the target")
    void linkNextSemesterResult_closesLoopWhenTargetMet() {
        CqiAction open = new CqiAction();
        open.setStatus(CqiStatus.IN_PROGRESS);
        open.setTargetScore(60.0);
        when(cqiActionRepository.findByModule_ModuleIdAndLos_IdAndStatusIn("MOD1", "LO001", List.of(CqiStatus.IN_PROGRESS)))
            .thenReturn(List.of(open));

        cqiService.linkNextSemesterResult("MOD1", "LO001", "21", 65.0);

        assertEquals(CqiStatus.COMPLETED, open.getStatus());
        assertEquals(65.0, open.getNextSemAttainment());
        verify(cqiActionRepository, times(1)).save(open);
        // Loop closed: must not re-evaluate for a new CQI cycle.
        verify(moduleRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("linkNextSemesterResult is a no-op when there is no open action for this module+LO")
    void linkNextSemesterResult_noOpWhenNoOpenAction() {
        when(cqiActionRepository.findByModule_ModuleIdAndLos_IdAndStatusIn("MOD1", "LO001", List.of(CqiStatus.IN_PROGRESS)))
            .thenReturn(Collections.emptyList());

        cqiService.linkNextSemesterResult("MOD1", "LO001", "21", 65.0);

        verify(cqiActionRepository, never()).save(any());
    }
}
