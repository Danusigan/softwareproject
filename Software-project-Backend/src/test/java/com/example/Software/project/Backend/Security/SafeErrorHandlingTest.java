package com.example.Software.project.Backend.Security;

import com.example.Software.project.Backend.RestController.UserRestController;
import com.example.Software.project.Backend.Service.UserService;
import com.example.Software.project.Backend.Service.AuditLogService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.*;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class SafeErrorHandlingTest {
    private static final String PRIVATE = "SQL SELECT password FROM user; token=private-marker; E:/private/database.sql";
    private static final String ID = "3d9ccf45-d7a1-4cd9-8b50-3b019d20ab5e";
    MockMvc mvc;
    UserService users;
    @BeforeEach void setup() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test-admin", null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("superadmin"))));
        users = mock(UserService.class);
        var controller = new UserRestController();
        var jwt = mock(JwtUtil.class);
        when(jwt.extractRole(anyString())).thenReturn("superadmin");
        ReflectionTestUtils.setField(controller, "userService", users);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwt);
        mvc = MockMvcBuilders.standaloneSetup(new FailureController(), controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ErrorResponseAdvice())
                .addFilters((request, response, chain) -> {
                    request.setAttribute(AuditLogService.REQUEST_ID, ID);
                    chain.doFilter(request, response);
                }).build();
    }
    @AfterEach void clearAuthentication() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    @RestController static class FailureController {
        @GetMapping("/failure/{kind}") Object fail(@PathVariable String kind) {
            throw switch (kind) {
                case "database" -> new DataAccessResourceFailureException(PRIVATE);
                case "conflict" -> new DataIntegrityViolationException(PRIVATE);
                case "invalid" -> new IllegalArgumentException(PRIVATE);
                case "missing" -> new ResponseStatusException(HttpStatus.NOT_FOUND, PRIVATE);
                case "record" -> new ResourceNotFoundException(PRIVATE);
                case "validation" -> new PublicValidationException("Email cannot be blank.");
                default -> new NullPointerException(PRIVATE);
            };
        }
        @PostMapping("/payload") Object payload(@RequestBody Map<String, String> body) { return body; }
        @GetMapping("/number") Object number(@RequestParam int count) { return Map.of("count", count); }
    }
    private void safe(ResultActions result, int expectedStatus) throws Exception {
        var body = result.andExpect(status().is(expectedStatus)).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("ERROR")).andExpect(jsonPath("$.requestId").value(ID))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("private-marker", "SELECT", "database.sql", "Exception", "stackTrace");
    }
    @Test void unexpectedErrorIsSafeAndCorrelated(CapturedOutput output) throws Exception {
        safe(mvc.perform(get("/failure/unexpected")), 500);
        assertThat(output.getAll()).doesNotContain(PRIVATE).contains(ID);
    }
    @Test void databaseFailureIs500() throws Exception { safe(mvc.perform(get("/failure/database")), 500); }
    @Test void constraintConflictIs409() throws Exception { safe(mvc.perform(get("/failure/conflict")), 409); }
    @Test void arbitraryArgumentMessageIsNotPublic() throws Exception { safe(mvc.perform(get("/failure/invalid")), 400); }
    @Test void notFoundReasonIsNotPublic() throws Exception { safe(mvc.perform(get("/failure/missing")), 404); }
    @Test void missingDomainRecordIs404WithoutLeakingDetails() throws Exception { safe(mvc.perform(get("/failure/record")), 404); }
    @Test void authoredValidationRemainsUseful() throws Exception {
        safe(mvc.perform(get("/failure/validation")).andExpect(jsonPath("$.message").value("Email cannot be blank.")), 400);
    }
    @Test void malformedJsonIsSafe() throws Exception { safe(mvc.perform(post("/payload").contentType("application/json").content("{private-marker")), 400); }
    @Test void missingParameterIs400() throws Exception { safe(mvc.perform(get("/number")), 400); }
    @Test void invalidParameterIs400() throws Exception { safe(mvc.perform(get("/number").param("count", PRIVATE)), 400); }
    @Test void unsupportedContentTypeIs415() throws Exception { safe(mvc.perform(post("/payload").contentType("text/plain").content(PRIVATE)), 415); }
    @Test void wrongMethodIs405AndHasAllowHeader() throws Exception {
        safe(mvc.perform(get("/payload")).andExpect(header().string("Allow", "POST")), 405);
    }
    @Test void unknownEndpointIs404() throws Exception { safe(mvc.perform(get("/missing-private-marker")), 404); }
    @Test void controllerCatchDoesNotTurnDatabaseFailureInto400(CapturedOutput output) throws Exception {
        when(users.updateLecturer(anyString(), any(), any())).thenThrow(new DataAccessResourceFailureException(PRIVATE));
        safe(mvc.perform(put("/api/auth/lecturers/test-user").header("Authorization", "Bearer test")
                .contentType("application/json").content("{}")), 500);
        assertThat(output.getAll()).doesNotContain(PRIVATE);
    }
    @Test void controllerCatchPreserves409Conflict() throws Exception {
        when(users.updateLecturer(anyString(), any(), any())).thenThrow(new DataIntegrityViolationException(PRIVATE));
        safe(mvc.perform(put("/api/auth/lecturers/test-user").header("Authorization", "Bearer test")
                .contentType("application/json").content("{}")), 409);
    }
    @Test void jwtLookupDatabaseFailureProducesSafe500() throws Exception {
        var filter = new JwtRequestFilter();
        var details = mock(org.springframework.security.core.userdetails.UserDetailsService.class);
        var jwt = mock(JwtUtil.class);
        when(jwt.extractUsername(anyString())).thenReturn("test-user");
        when(details.loadUserByUsername("test-user")).thenThrow(new DataAccessResourceFailureException(PRIVATE));
        ReflectionTestUtils.setField(filter, "userDetailsService", details);
        ReflectionTestUtils.setField(filter, "jwtUtil", jwt);
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/modules");
        request.addHeader("Authorization", "Bearer private-marker");
        request.setAttribute(AuditLogService.REQUEST_ID, ID);
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains(ID).doesNotContain("private-marker", "SELECT", "database.sql");
        verifyNoInteractions(chain);
    }
}
