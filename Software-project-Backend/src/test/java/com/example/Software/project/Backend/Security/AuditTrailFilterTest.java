package com.example.Software.project.Backend.Security;
import com.example.Software.project.Backend.Service.AuditLogService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditTrailFilterTest {
 AuditLogService audit=mock(AuditLogService.class); AuditTrailFilter filter=new AuditTrailFilter(audit);
 public static class ExampleController {public void updateMarks(){} public void exportStudentReport(){} public void listModules(){}}
 @BeforeEach void actor(){SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("staff",null,"ROLE_ADMIN"));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 MockHttpServletRequest request(String method,String operation) throws Exception {
  var request=new MockHttpServletRequest(method,"/api/data/private-id");
  request.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,new HandlerMethod(new ExampleController(),ExampleController.class.getMethod(operation)));
  request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,"/api/data/{id}");
  request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,Map.of("id","private-id"));return request;
 }
 @Test void successfulMutationRecordsOnlySafeMetadata() throws Exception {
  var request=request("PUT","updateMarks");request.setContent("password=DoNotStore".getBytes());request.addHeader("Authorization","Bearer DoNotStore");request.setQueryString("token=DoNotStore");
  var response=new MockHttpServletResponse();filter.doFilter(request,response,(q,r)->{});
  verify(audit).record(eq("staff"),eq("HTTP ExampleController.updateMarks"),eq("/api/data/{id}; id=private-id"),eq("SUCCESS"),eq("method=PUT; status=200"),eq(response.getHeader("X-Request-ID")));
 }
 @Test void sensitiveGetExportIsRecorded() throws Exception {
  filter.doFilter(request("GET","exportStudentReport"),new MockHttpServletResponse(),(q,r)->{});
  verify(audit).record(eq("staff"),eq("HTTP ExampleController.exportStudentReport"),anyString(),eq("SUCCESS"),anyString(),anyString());
 }
 @Test void ordinaryReadsDoNotFloodTheAuditLog() throws Exception {
  filter.doFilter(request("GET","listModules"),new MockHttpServletResponse(),(q,r)->{});verifyNoInteractions(audit);
 }
 @Test void deniedReadsAreRecorded() throws Exception {
  var response=new MockHttpServletResponse();filter.doFilter(request("GET","listModules"),response,(q,r)->response.setStatus(403));
  verify(audit).record(eq("staff"),anyString(),anyString(),eq("DENIED"),eq("method=GET; status=403"),anyString());
 }
 @Test void thrownOperationIsRecordedAsFailure() throws Exception {
  assertThrows(ServletException.class,()->filter.doFilter(request("POST","updateMarks"),new MockHttpServletResponse(),(q,r)->{throw new ServletException("password=DoNotStore");}));
  verify(audit).record(eq("staff"),anyString(),anyString(),eq("FAILURE"),eq("method=POST; status=500"),anyString());
 }
 @Test void healthAndPreflightDoNotGenerateEvents() throws Exception {
  filter.doFilter(new MockHttpServletRequest("GET","/actuator/health"),new MockHttpServletResponse(),(q,r)->{});
  filter.doFilter(request("OPTIONS","listModules"),new MockHttpServletResponse(),(q,r)->{});verifyNoInteractions(audit);
 }
}
