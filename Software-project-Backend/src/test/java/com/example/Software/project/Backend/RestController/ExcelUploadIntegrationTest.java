package com.example.Software.project.Backend.RestController;
import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import com.example.Software.project.Backend.Security.JwtUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:excel-upload;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.flyway.enabled=false",
 "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","logging.level.org.springframework=WARN"})
@AutoConfigureMockMvc
class ExcelUploadIntegrationTest {
 @Autowired MockMvc mvc; @Autowired JwtUtil jwt; @Autowired UserRepository users;
 @Autowired ModuleRepository modules; @Autowired StudentRepository students; @Autowired LosRepository los; @Autowired StudentMarkRepository marks;
 @Autowired AssessmentTemplateRepository templates; @Autowired AssessmentItemRepository items;
 @Autowired StudentAssessmentScoreRepository scores;
 String id, sid, token; Long originalMark;
 @BeforeEach void seed() {
  id="V"+UUID.randomUUID().toString().replace("-","").toUpperCase();sid=id+"s";
  users.saveAndFlush(new User("uploadadmin","upload@example.test","unused","superadmin"));
  token="Bearer "+jwt.generateToken("uploadadmin","superadmin");
  Student student=students.saveAndFlush(new Student(sid,"Original",null));
  com.example.Software.project.Backend.Model.Module module=new com.example.Software.project.Backend.Model.Module();module.setModuleId(id);module.setModuleName("Upload test");module=modules.saveAndFlush(module);
  Los lo=new Los();lo.setModule(module);lo.setId(id);lo.setName("LO1");lo.setFileName("original.xlsx");lo.setMarksCsvFile(new byte[]{1,2,3});lo=los.saveAndFlush(lo);
  StudentMark mark=new StudentMark();mark.setStudent(student);mark.setLos(lo);mark.setScore(40.0);mark.setBatch("24");mark.setMarkType(MarkType.FINAL_EXAM);
  originalMark=marks.saveAndFlush(mark).getId();
 }
 MockMultipartFile sheet(String field,String... rows) throws Exception {
  try(Workbook wb=new XSSFWorkbook();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
   Sheet sheet=wb.createSheet("Data");for(int r=0;r<rows.length;r++){Row row=sheet.createRow(r);String[] values=rows[r].split("\\|",-1);for(int c=0;c<values.length;c++)row.createCell(c).setCellValue(values[c]);}
   wb.write(out);return new MockMultipartFile(field,"upload.xlsx","application/octet-stream",out.toByteArray());
  }
 }
 @ParameterizedTest @ValueSource(strings={"/api/students/upload","/api/obe/marks/upload","/api/obe/marks/upload-bulk","/api/obe/marks/upload-question-wise","legacy","attachment"})
 void everyRouteRejectsSpoofedWorkbook(String path) throws Exception {
  String field=path.equals("/api/students/upload")||path.equals("legacy")?"file":"excelFile";
  if(path.equals("legacy"))path="/api/obe/marks/upload/"+id;
  if(path.equals("attachment"))path="/api/lospos/"+id+"/marks/import-obe";
  mvc.perform(multipart(path).file(new MockMultipartFile(field,"fake.xlsx","application/octet-stream","fake".getBytes()))
   .param("losIds",id).param("templateId","missing").param("batch","new").param("markType","FINAL_EXAM").header("Authorization",token)).andExpect(status().isBadRequest());
  assertEquals(40.0,marks.findById(originalMark).orElseThrow().getScore());
 }
 @Test void rejectedReplacementRollsBackDeletionAndAttachment() throws Exception {
  mvc.perform(multipart("/api/lospos/"+id+"/marks/import-obe").file(sheet("excelFile","Student Index|Marks",sid+"|50","unknown|60"))
   .param("batch","24").param("replaceBatch","24").header("Authorization",token)).andExpect(status().isBadRequest());
  assertTrue(marks.existsById(originalMark));assertEquals("original.xlsx",los.findById(id).orElseThrow().getFileName());
  assertArrayEquals(new byte[]{1,2,3},los.findById(id).orElseThrow().getMarksCsvFile());
 }
 @Test void validReplacementCommitsMarksAndAttachmentTogether() throws Exception {
  mvc.perform(multipart("/api/lospos/"+id+"/marks/import-obe").file(sheet("excelFile","Student Index|Marks",sid+"|75"))
   .param("batch","24").param("replaceBatch","24").header("Authorization",token)).andExpect(status().isOk());
  assertFalse(marks.existsById(originalMark));assertEquals(75.0,marks.findByLos_Id(id).get(0).getScore());assertEquals("upload.xlsx",los.findById(id).orElseThrow().getFileName());
 }
 @Test void invalidRosterRollsBackPreviouslyManagedStudentEdits() throws Exception {
  mvc.perform(multipart("/api/students/upload").file(sheet("file","Student ID|Student Name|Email|Academic Year|Batch",sid+"|Changed|||24","newstudent|New|invalid||24"))
   .header("Authorization",token)).andExpect(status().isBadRequest());
  assertEquals("Original",students.findById(sid).orElseThrow().getStudentName());assertFalse(students.existsById("newstudent"));
 }
 @ParameterizedTest @ValueSource(strings={"NaN", ""})
 void questionUploadRejectsBadScoreWithoutDeletingExistingScores(String invalidScore) throws Exception {
  AssessmentTemplate t=new AssessmentTemplate();t.setId(id);t.setName("test");t.setBatch("24");t.setMarkType("FINAL_EXAM");templates.saveAndFlush(t);
  AssessmentItem item=new AssessmentItem();item.setAssessmentTemplate(t);item.setLos(los.findById(id).orElseThrow());item.setMaxMarks(100.0);item.setQuestionLabel("Q1");item.setQuestionNumber(1);item=items.saveAndFlush(item);
  StudentAssessmentScore score=new StudentAssessmentScore();score.setStudent(students.findById(sid).orElseThrow());score.setAssessmentItem(item);score.setScore(40.0);Long old=scores.saveAndFlush(score).getId();
  mvc.perform(multipart("/api/obe/marks/upload-question-wise").file(sheet("excelFile","Title","Instructions","Student ID|Q1 (max=100)",sid+"|"+invalidScore))
   .param("templateId",id).param("batch","24").param("markType","FINAL_EXAM").header("Authorization",token)).andExpect(status().isBadRequest());
  assertEquals(40.0,scores.findById(old).orElseThrow().getScore());assertTrue(marks.existsById(originalMark));
 }
 @Test void lecturerGetsControlledErrorForMalformedMetadataWorkbook() throws Exception {
  users.saveAndFlush(new User("uploadlecture","uploadlecture@example.test","unused","lecture"));
  mvc.perform(multipart("/api/obe/marks/upload").file(new MockMultipartFile("excelFile","fake.xlsx","application/octet-stream","fake".getBytes()))
   .header("Authorization","Bearer "+jwt.generateToken("uploadlecture","lecture"))).andExpect(status().isBadRequest());
 }
}
