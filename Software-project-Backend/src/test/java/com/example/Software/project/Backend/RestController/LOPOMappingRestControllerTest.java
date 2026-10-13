package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.OutcomeMapping;
import com.example.Software.project.Backend.Repository.UserAccessRepository;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.AuditLogService;
import com.example.Software.project.Backend.Service.LOPOMappingService;
import com.example.Software.project.Backend.Service.ProgramOutcomeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for LOPOMappingRestController's admin-only endpoints.
 *
 * The controller does NOT use Spring Security's @PreAuthorize/hasRole — it manually
 * decodes the "Authorization" header via JwtUtil and gates access with isAdmin()/isLecturer()
 * helpers (see LOPOMappingRestController lines 36-58). These tests exercise that gate
 * directly through HTTP, with the Spring Security filter chain disabled (addFilters = false)
 * so the controller's own role check — the thing actually protecting these endpoints in
 * production — is what's under test, not the servlet filter chain.
 */
@WebMvcTest(controllers = LOPOMappingRestController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LOPOMappingRestController admin-endpoint access tests")
class LOPOMappingRestControllerTest {
    private void authenticate(String role) {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.TestingAuthenticationToken("staff", null, role));
    }
    @org.junit.jupiter.api.BeforeEach @org.junit.jupiter.api.AfterEach
    void clearAuthentication() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }

    // These slice tests isolate controller validation; RbacIntegrationTest exercises the real policy.
    @MockBean(name="accessPolicy") private com.example.Software.project.Backend.Security.AccessPolicy accessPolicy;
    @org.junit.jupiter.api.BeforeEach void permitPolicyInSlice() {
        when(accessPolicy.allow(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap())).thenReturn(true);
    }


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LOPOMappingService mappingService;

    @MockBean
    private ProgramOutcomeService poService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuditLogService auditLogService;
    @MockBean
    private UserAccessRepository userAccessRepository;

    @Test
    @DisplayName("GET /admin/pending is rejected with 403 for a lecturer token")
    void adminPending_rejectsLecturerToken() throws Exception {
        authenticate("lecture");

        mockMvc.perform(get("/api/lo-po-mapping/admin/pending")
                .header("Authorization", "Bearer lecturer.jwt"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    @DisplayName("GET /admin/pending succeeds with 200 for an admin token")
    void adminPending_allowsAdminToken() throws Exception {
        authenticate("admin");
        when(mappingService.getPendingMappings()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/lo-po-mapping/admin/pending")
                .header("Authorization", "Bearer admin.jwt"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /admin/pending is rejected with 403 when the token has no valid role")
    void adminPending_rejectsUnparseableToken() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/lo-po-mapping/admin/pending")
                .header("Authorization", "Bearer garbage.jwt"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /admin/{id}/approve is rejected with 403 for a lecturer token")
    void approve_rejectsLecturerToken() throws Exception {
        authenticate("lecture");

        mockMvc.perform(put("/api/lo-po-mapping/admin/1/approve")
                .header("Authorization", "Bearer lecturer.jwt")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /admin/{id}/reject requires non-blank rejection remarks even for an admin")
    void reject_requiresRemarks() throws Exception {
        authenticate("admin");

        mockMvc.perform(put("/api/lo-po-mapping/admin/1/reject")
                .header("Authorization", "Bearer admin.jwt")
                .contentType("application/json")
                .content("{\"adminRemarks\": \"   \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Rejection remarks are required."));
    }

    @Test
    @DisplayName("PUT /admin/{id}/reject succeeds for an admin token with remarks supplied")
    void reject_succeedsWithRemarks() throws Exception {
        authenticate("admin");
        when(jwtUtil.extractUsername("admin.jwt")).thenReturn("admin1");
        OutcomeMapping rejected = new OutcomeMapping();
        rejected.setId(1L);
        rejected.setWeight(2);
        rejected.setStatus(OutcomeMapping.ApprovalStatus.REJECTED);
        when(mappingService.rejectMapping(1L, "admin1", "not sufficient")).thenReturn(rejected);

        mockMvc.perform(put("/api/lo-po-mapping/admin/1/reject")
                .header("Authorization", "Bearer admin.jwt")
                .contentType("application/json")
                .content("{\"adminRemarks\": \"not sufficient\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /create is rejected with 403 without a valid lecturer/admin token")
    void create_rejectsWithoutValidToken() throws Exception {
        authenticate("student");

        mockMvc.perform(post("/api/lo-po-mapping/create")
                .param("loId", "LO001")
                .header("Authorization", "Bearer student.jwt")
                .contentType("application/json")
                .content("{\"mappings\": {\"PO1\": 3, \"PO2\": 1}}"))
            .andExpect(status().isForbidden());
    }
}
