package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Repository.*;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for OBEController's role gate (isLecture/isAdmin, lines 1109-1135) and the
 * request validation on the two endpoints the Marks Workbench workflow depends on directly:
 * exporting a marks report and calculating PO attainment (see Project _init.md's
 * "Current End-to-End Workflow"). Like LOPOMappingRestController, this controller checks roles
 * manually rather than via @PreAuthorize, so the Spring Security filter chain is disabled
 * (addFilters = false) to test the controller's own gate.
 */
@WebMvcTest(controllers = OBEController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OBEController access-control and validation tests")
class OBEControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ProgramOutcomeRepository poRepo;
    @MockBean private OutcomeMappingRepository mapRepo;
    @MockBean private LosRepository losRepo;
    @MockBean private ExcelImportService excelService;
    @MockBean private ExcelExportService excelExportService;
    @MockBean private AttainmentService attainmentService;
    @MockBean private POAttainmentService poAttainmentService;
    @MockBean private TrendService trendService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private AssessmentTemplateRepository assessmentTemplateRepo;
    @MockBean private AssessmentItemRepository assessmentItemRepo;
    @MockBean private ModuleRepository moduleRepo;
    @MockBean private StudentMarkRepository studentMarkRepository;

    // ---- role gate ----

    @Test
    @DisplayName("POST /po/create is rejected with 403 for a lecturer token (admin-only endpoint)")
    void createPO_rejectsLecturerToken() throws Exception {
        when(jwtUtil.extractRole("lecturer.jwt")).thenReturn("lecture");

        mockMvc.perform(post("/api/obe/po/create")
                .header("Authorization", "Bearer lecturer.jwt")
                .contentType("application/json")
                .content("{\"poId\": \"PO1\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /export/marks is rejected with 403 without a lecturer/admin token")
    void exportMarks_rejectsUnauthorizedToken() throws Exception {
        when(jwtUtil.extractRole("student.jwt")).thenReturn("student");

        mockMvc.perform(post("/api/obe/export/marks")
                .header("Authorization", "Bearer student.jwt")
                .contentType("application/json")
                .content("{\"losIds\": [\"LO001\"], \"markType\": \"FINAL_EXAM\", \"batch\": \"20\"}"))
            .andExpect(status().isForbidden());
    }

    // ---- export/marks validation ----

    @Test
    @DisplayName("POST /export/marks rejects an empty losIds list with 400")
    void exportMarks_rejectsEmptyLosIds() throws Exception {
        when(jwtUtil.extractRole("lecturer.jwt")).thenReturn("lecture");

        mockMvc.perform(post("/api/obe/export/marks")
                .header("Authorization", "Bearer lecturer.jwt")
                .contentType("application/json")
                .content("{\"losIds\": [], \"markType\": \"FINAL_EXAM\", \"batch\": \"20\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Error: losIds list cannot be empty"));
    }

    @Test
    @DisplayName("POST /export/marks rejects a threshold outside 0-100 with 400")
    void exportMarks_rejectsOutOfRangeThreshold() throws Exception {
        when(jwtUtil.extractRole("lecturer.jwt")).thenReturn("lecture");

        mockMvc.perform(post("/api/obe/export/marks")
                .header("Authorization", "Bearer lecturer.jwt")
                .contentType("application/json")
                .content("{\"losIds\": [\"LO001\"], \"markType\": \"FINAL_EXAM\", \"batch\": \"20\", \"threshold\": 150}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Error: threshold must be between 0 and 100"));
    }

    @Test
    @DisplayName("POST /export/marks rejects an invalid markType with 400")
    void exportMarks_rejectsInvalidMarkType() throws Exception {
        when(jwtUtil.extractRole("lecturer.jwt")).thenReturn("lecture");

        mockMvc.perform(post("/api/obe/export/marks")
                .header("Authorization", "Bearer lecturer.jwt")
                .contentType("application/json")
                .content("{\"losIds\": [\"LO001\"], \"markType\": \"MIDTERM\", \"batch\": \"20\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Error: Invalid markType. Must be FINAL_EXAM or ASSIGNMENT"));
    }

    @Test
    @DisplayName("POST /export/marks returns the generated workbook as a downloadable file for a valid request")
    void exportMarks_returnsWorkbookForValidRequest() throws Exception {
        when(jwtUtil.extractRole("lecturer.jwt")).thenReturn("lecture");
        when(excelExportService.generateMarksExcel(anyList(), eq("FINAL_EXAM"), eq("20"), eq(50)))
            .thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(post("/api/obe/export/marks")
                .header("Authorization", "Bearer lecturer.jwt")
                .contentType("application/json")
                .content("{\"losIds\": [\"LO001\"], \"markType\": \"FINAL_EXAM\", \"batch\": \"20\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"marks_report_20_final_exam.xlsx\""));
    }

    // ---- po-attainment ----

    @Test
    @DisplayName("POST /po-attainment is rejected with 403 for a non-lecturer token")
    void poAttainment_rejectsUnauthorizedToken() throws Exception {
        when(jwtUtil.extractRole("student.jwt")).thenReturn("student");

        mockMvc.perform(post("/api/obe/po-attainment")
                .header("Authorization", "Bearer student.jwt")
                .contentType("application/json")
                .content("{\"losIds\": [\"LO001\"], \"markType\": \"FINAL_EXAM\", \"batch\": \"20\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /po-attainment returns 200 with the calculated credits for a lecturer token")
    void poAttainment_succeedsForLecturer() throws Exception {
        when(jwtUtil.extractRole("lecturer.jwt")).thenReturn("lecture");
        when(poAttainmentService.calculateStudentPOCredits(anyList(), eq("FINAL_EXAM"), eq("20"), eq(50), eq(0.0)))
            .thenReturn(Map.of("students", Collections.emptyList()));

        mockMvc.perform(post("/api/obe/po-attainment")
                .header("Authorization", "Bearer lecturer.jwt")
                .contentType("application/json")
                .content("{\"losIds\": [\"LO001\"], \"markType\": \"FINAL_EXAM\", \"batch\": \"20\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // ---- marks/upload-bulk ----

    @Test
    @DisplayName("POST /marks/upload-bulk is rejected with 403 without a lecturer/admin token")
    void uploadBulk_rejectsUnauthorizedToken() throws Exception {
        when(jwtUtil.extractRole("student.jwt")).thenReturn("student");

        mockMvc.perform(multipart("/api/obe/marks/upload-bulk")
                .file("excelFile", "content".getBytes())
                .param("losIds", "LO001")
                .param("batch", "20")
                .header("Authorization", "Bearer student.jwt"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /marks/upload-bulk rejects the request with 400 when losIds cannot be determined")
    void uploadBulk_rejectsWhenLosIdsMissing() throws Exception {
        when(jwtUtil.extractRole("lecturer.jwt")).thenReturn("lecture");
        when(excelService.readMetadata(any())).thenReturn(Collections.emptyMap());

        mockMvc.perform(multipart("/api/obe/marks/upload-bulk")
                .file("excelFile", "content".getBytes())
                .param("batch", "20")
                .header("Authorization", "Bearer lecturer.jwt"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Error: losIds cannot be determined. Use a template downloaded from this system."));
    }
}
