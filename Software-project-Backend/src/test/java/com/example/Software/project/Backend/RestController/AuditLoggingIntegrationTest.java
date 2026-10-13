package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.example.Software.project.Backend.Service.AuditLogService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:audit-logging;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.flyway.enabled=false",
 "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","logging.level.org.springframework=WARN",
 "logging.level.org.springframework.web=INFO","app.audit.retention-days=30","app.audit.cleanup-cron=-"})
@AutoConfigureMockMvc @ExtendWith(OutputCaptureExtension.class)
class AuditLoggingIntegrationTest {
 @Autowired MockMvc mvc; @Autowired JwtUtil jwt; @Autowired PasswordEncoder encoder;
 @Autowired UserRepository users; @Autowired StudentRepository students; @Autowired PasswordResetTokenRepository resets;
 @Autowired AuditLogService audit; @SpyBean AuditLogRepository logs;
 @Autowired PlatformTransactionManager transactions; @Autowired JdbcTemplate jdbc; @Autowired MeterRegistry meters;
 String actor, lecturer, password="AuditOriginal123!";
 @BeforeEach void seed() {
  actor="a"+UUID.randomUUID().toString().replace("-","");lecturer=actor+"l";
  users.saveAndFlush(new User(actor,actor+"@example.test",encoder.encode(password),"superadmin"));
  users.saveAndFlush(new User(lecturer,lecturer+"@example.test",encoder.encode(password),"lecture"));
 }
 String token(String name,String role){return "Bearer "+jwt.generateToken(name,role);}
 List<AuditLog> events(String action,String target){return logs.findAll().stream().filter(e->action.equals(e.getAction())&&target.equals(e.getTarget())).toList();}
 AuditLog requestEvent(MvcResult result) {
  String id=result.getResponse().getHeader("X-Request-ID");assertNotNull(id);UUID.fromString(id);
  List<AuditLog> found=logs.findAll().stream().filter(e->e.getAction().startsWith("HTTP ")&&e.getDetails().contains(id)).toList();
  assertEquals(1,found.size(),"Exactly one HTTP audit event per request");return found.get(0);
 }
 @Test void accountEditRecordsActorOutcomeCorrelationWithoutPasswordOrEmail() throws Exception {
  String secret="ReplacementSecret123!",email="private+"+actor+"@example.test", auth=token(actor,"superadmin");
  MvcResult result=mvc.perform(put("/api/auth/lecturers/"+lecturer).header("Authorization",auth).header("X-Request-ID","untrusted-id")
   .contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\""+secret+"\"}")).andExpect(status().isOk()).andReturn();
  AuditLog event=requestEvent(result);assertEquals(actor,event.getActor());assertEquals("SUCCESS",event.getOutcome());
  assertTrue(event.getTarget().contains(lecturer));assertTrue(event.getDetails().contains("status=200"));
  String data=event.getTarget()+event.getDetails()+event.getActor();
  assertFalse(data.contains(secret));assertFalse(data.contains(email));assertFalse(data.contains(auth));assertFalse(data.contains("untrusted-id"));
  assertTrue(Duration.between(event.getTimestamp(),LocalDateTime.now(ZoneOffset.UTC)).abs().getSeconds()<10);
 }
 @Test void validationFailureAndForbiddenActionAreNotSuccessfulEvents() throws Exception {
  var invalid=mvc.perform(put("/api/auth/lecturers/"+lecturer).header("Authorization",token(actor,"superadmin"))
   .contentType("application/json").content("{\"password\":\"weak\"}")).andExpect(status().isBadRequest()).andReturn();
  assertEquals("FAILURE",requestEvent(invalid).getOutcome());
  var denied=mvc.perform(delete("/api/auth/lecturers/"+actor).header("Authorization",token(lecturer,"lecture"))).andExpect(status().isForbidden()).andReturn();
  assertEquals("DENIED",requestEvent(denied).getOutcome());assertEquals(lecturer,requestEvent(denied).getActor());assertTrue(users.existsById(actor));
 }
 @Test void invalidJwtIsNotCopiedToAudit() throws Exception {
  String fake="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzZWNyZXQifQ.invalid";
  var result=mvc.perform(delete("/api/auth/lecturers/"+lecturer).header("Authorization","Bearer "+fake)).andExpect(status().isUnauthorized()).andReturn();
  AuditLog event=requestEvent(result);assertEquals("ANONYMOUS",event.getActor());assertEquals("DENIED",event.getOutcome());assertFalse(event.getDetails().contains(fake));
 }
 @Test void successfulAndFailedLoginsKeepTheirSemanticEvents() throws Exception {
  for(String pw:List.of("Incorrect123!",password)) mvc.perform(post("/api/auth/login").contentType("application/json")
   .content("{\"userID\":\""+actor+"\",\"password\":\""+pw+"\"}")).andExpect(status().is(pw.equals(password)?200:401));
  var found=events("LOGIN",actor);assertEquals(2,found.size());assertTrue(found.stream().anyMatch(e->e.getOutcome().equals("SUCCESS")));assertTrue(found.stream().anyMatch(e->e.getOutcome().equals("FAILURE")));
  assertTrue(found.stream().allMatch(e->e.getDetails().contains("requestId=")));
 }
 @Test void passwordRecoveryRecordsAccountButNeverToken() throws Exception {
  String reset="reset-secret-"+actor;
  resets.saveAndFlush(new PasswordResetToken(reset,lecturer,LocalDateTime.now().plusMinutes(10)));
  var result=mvc.perform(post("/api/auth/reset-password").contentType("application/json")
   .content("{\"token\":\""+reset+"\",\"newPassword\":\"Replacement123!\"}")).andExpect(status().isOk()).andReturn();
  AuditLog event=events("PASSWORD_RESET",lecturer).get(0);assertEquals(lecturer,event.getActor());
  assertFalse(event.getDetails().contains(reset));assertFalse(requestEvent(result).getDetails().contains(reset));
 }
 @Test void successIsDeferredUntilCommitAndFailureSurvivesRollback() {
  var tx=new TransactionTemplate(transactions);
  tx.executeWithoutResult(status->{audit.log(actor,"COMMITTED",actor,"SUCCESS",null);assertTrue(events("COMMITTED",actor).isEmpty());});
  assertEquals(1,events("COMMITTED",actor).size());
  tx.executeWithoutResult(status->{audit.log(actor,"ROLLED_BACK",actor,"SUCCESS",null);audit.log(actor,"REJECTED",actor,"FAILURE",null);status.setRollbackOnly();});
  assertTrue(events("ROLLED_BACK",actor).isEmpty());assertEquals(1,events("REJECTED",actor).size());
 }
 @Test void storageFailureDoesNotPoisonBusinessCommitAndLaterWritesRecover(CapturedOutput output) {
  double before=meters.counter("app.audit.write.failures").count();
  doThrow(new DataIntegrityViolationException("password=NeverPrintThis123!")).when(logs).saveAndFlush(argThat(e->"STORAGE_FAILURE".equals(e.getAction())));
  new TransactionTemplate(transactions).executeWithoutResult(status->{
   students.saveAndFlush(new Student(actor,"Business commit",null));
   audit.log(actor,"STORAGE_FAILURE",actor,"FAILURE",null);
  });
  assertTrue(students.existsById(actor));assertEquals(before+1,meters.counter("app.audit.write.failures").count());
  assertFalse(output.getAll().contains("NeverPrintThis123!"));
  audit.log(actor,"RECOVERED",actor,"SUCCESS",null);assertEquals(1,events("RECOVERED",actor).size());
 }
 @Test void retentionUsesConfiguredDaysAndCanRecoverFromCleanupFailure() {
  audit.log(actor,"OLD",actor,"SUCCESS",null);audit.log(actor,"RECENT",actor,"SUCCESS",null);
  jdbc.update("UPDATE audit_log SET timestamp=? WHERE actor=? AND action='OLD'",LocalDateTime.now(ZoneOffset.UTC).minusDays(31),actor);
  jdbc.update("UPDATE audit_log SET timestamp=? WHERE actor=? AND action='RECENT'",LocalDateTime.now(ZoneOffset.UTC).minusDays(29),actor);
  double before=meters.counter("app.audit.cleanup.failures").count();
  doThrow(new DataIntegrityViolationException("private SQL failure")).when(logs).deleteByTimestampBefore(any());
  audit.purgeOldEntries();assertEquals(before+1,meters.counter("app.audit.cleanup.failures").count());assertEquals(1,events("OLD",actor).size());
  reset(logs);audit.purgeOldEntries();assertTrue(events("OLD",actor).isEmpty());assertEquals(1,events("RECENT",actor).size());
 }
 @Test void fieldsAreBoundedAndControlCharactersAndSecretsAreRemoved() {
  audit.log(actor+"\r\nforged","SANITIZE", "x".repeat(300),"FAILURE","password=Hidden123 token=SensitiveToken secret=PrivateValue");
  AuditLog event=logs.findAll().stream().filter(e->e.getAction().equals("SANITIZE")).findFirst().orElseThrow();
  assertFalse(event.getActor().contains("\n"));assertEquals(255,event.getTarget().length());
  assertFalse(event.getDetails().contains("Hidden123"));assertFalse(event.getDetails().contains("SensitiveToken"));assertFalse(event.getDetails().contains("PrivateValue"));
 }
 @Test void auditTimestampConversionUsesUtcRatherThanJvmLocalTime() {
  var converter=new AuditTimestampConverter();
  var utc=LocalDateTime.of(2026,10,12,6,30);
  assertEquals(utc.toInstant(ZoneOffset.UTC),converter.convertToDatabaseColumn(utc).toInstant());
  assertEquals(utc,converter.convertToEntityAttribute(java.sql.Timestamp.from(utc.toInstant(ZoneOffset.UTC))));
 }
}
