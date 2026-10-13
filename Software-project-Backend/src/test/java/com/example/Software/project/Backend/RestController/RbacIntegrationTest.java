package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module;
import com.example.Software.project.Backend.Repository.*;
import com.example.Software.project.Backend.Security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:rbac;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "logging.level.org.springframework=WARN"})
@AutoConfigureMockMvc
@Transactional
class RbacIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ModuleRepository modules;
    @Autowired LosRepository los;
    @Autowired AssessmentTemplateRepository templates;
    @Autowired ProgramOutcomeRepository outcomes;
    @Autowired OutcomeMappingRepository mappings;
    @Autowired StudentMarkRepository marks;
    @Autowired StudentRepository students;
    @Autowired StudentPoCreditRepository credits;
    @Autowired JwtUtil jwt;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
    @Autowired RequestMappingHandlerMapping requestMappingHandlerMapping;

    @BeforeEach void fixtures() {
        for(String role:List.of("superadmin","admin","lecture"))
            users.save(new User(role,role+"@example.test",encoder.encode("Password123!"),role));
        users.save(new User("other","other@example.test",encoder.encode("Password123!"),"lecture"));
        for(String id:List.of("OWN","OTHER","OPEN")) {
            Module m=new Module();m.setModuleId(id);m.setModuleName(id);
            m.setAssignedLecturers(new ArrayList<>(id.equals("OPEN")?List.of():List.of(users.findByUsername(id.equals("OWN")?"lecture":"other").orElseThrow())));
            modules.save(m);
            Los lo=new Los();lo.setId(id+" LO");lo.setName("LO1");lo.setModule(m);los.save(lo);
        }
        AssessmentTemplate t=new AssessmentTemplate();t.setId("OTHER-TEMPLATE");t.setModule(modules.findById("OTHER").orElseThrow());templates.save(t);
        users.flush();modules.flush();los.flush();templates.flush();
    }
    String token(String user) {return "Bearer "+jwt.generateToken(user,users.findByUsername(user).orElseThrow().getUsertype());}

    @Test void anonymousAndMalformedTokensAre401() throws Exception {
        mvc.perform(get("/api/modules/all")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/modules/all").header("Authorization","Bearer garbage")).andExpect(status().isUnauthorized());
    }
    @Test void lecturerListContainsOnlyExplicitAssignments() throws Exception {
        var result=mvc.perform(get("/api/modules/all").header("Authorization",token("lecture")))
                .andExpect(status().isOk()).andReturn();
        var data=json.readTree(result.getResponse().getContentAsString()).get("data");
        assertEquals(1,data.size());assertEquals("OWN",data.get(0).get("moduleId").asText());
    }
    @ParameterizedTest
    @CsvSource({"GET,/api/modules/OTHER", "GET,/api/modules/OPEN", "GET,/api/lospos/OTHER LO",
        "GET,/api/lospos/module/OTHER", "GET,/api/obe/assessment/template/OTHER-TEMPLATE",
        "GET,/api/obe/assessment/templates/OTHER", "GET,/api/lo-po-mapping/module/OTHER",
        "GET,/api/los-with-mapping/OTHER LO/details", "GET,/api/obe/reports/course/OTHER",
        "GET,/api/obe/analysis/trend/OTHER", "GET,/api/obe/marks/available/module/OTHER",
        "POST,/api/cqi/finalize/OTHER?batch=22", "POST,/api/cqi/trigger-po/OTHER?batch=22",
        "GET,/api/lo-po-mapping/admin/pending", "GET,/api/cqi/pending", "GET,/api/auth/admins",
        "GET,/api/auth/lecturers", "DELETE,/api/program-outcomes/PO1/permanent",
        "POST,/api/modules/create", "POST,/api/obe/po/create", "POST,/api/obe/po-attainment/overall"})
    void lecturerCannotCrossRoleOrModuleBoundary(String method,String path) throws Exception {
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method),path)
                .header("Authorization",token("lecture")).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }
    @ParameterizedTest @CsvSource({"lecture,OWN", "admin,OTHER", "superadmin,OPEN"})
    void permittedModuleReadsStillWork(String user,String module) throws Exception {
        mvc.perform(get("/api/modules/"+module).header("Authorization",token(user))).andExpect(status().isOk());
    }
    @Test void mixedBodyIdsAreRejectedBeforeExportOrTemplateCreation() throws Exception {
        for(String path:List.of("/api/obe/export/marks","/api/obe/template/marks","/api/obe/po-attainment")) {
            mvc.perform(post(path).header("Authorization",token("lecture")).contentType("application/json")
                    .content("{\"moduleId\":\"OWN\",\"losIds\":[\"OWN LO\",\"OTHER LO\"]}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/obe/assessment/template").header("Authorization",token("lecture"))
                .contentType("application/json").content("{\"moduleId\":\"OWN\",\"items\":[{\"loId\":\"OTHER LO\"}]}"))
                .andExpect(status().isForbidden());
    }
    @Test void existingTemplateCannotBeOverwrittenViaAllowedModule() throws Exception {
        mvc.perform(post("/api/obe/assessment/template").header("Authorization",token("lecture"))
                .contentType("application/json").content("{\"id\":\"OTHER-TEMPLATE\",\"moduleId\":\"OWN\"}"))
                .andExpect(status().isForbidden());
        assertEquals("OTHER",templates.findById("OTHER-TEMPLATE").orElseThrow().getModule().getModuleId());
    }
    @Test void staleRoleClaimCannotRetainAdminPrivileges() throws Exception {
        String old=token("admin");User user=users.findByUsername("admin").orElseThrow();user.setUsertype("lecture");users.saveAndFlush(user);
        mvc.perform(get("/api/auth/lecturers").header("Authorization",old)).andExpect(status().isForbidden());
        mvc.perform(get("/api/reports/po/batch?batch=22").header("Authorization",old)).andExpect(status().isForbidden());
    }
    @Test void deletedAccountTokenIs401() throws Exception {
        String old=token("superadmin");users.delete(users.findByUsername("superadmin").orElseThrow());users.flush();
        mvc.perform(get("/api/modules/all").header("Authorization",old)).andExpect(status().isUnauthorized());
    }
    @Test void adminCannotCreateAdminsOrPermanentlyDelete() throws Exception {
        mvc.perform(post("/api/auth/add-admin").header("Authorization",token("admin")).contentType("application/json")
                .content("{\"userID\":\"newadmin\",\"email\":\"new@example.test\",\"password\":\"Password123!\",\"usertype\":\"admin\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/program-outcomes/PO1/permanent").header("Authorization",token("admin")))
                .andExpect(status().isForbidden());
    }
    @Test void superadminCreatesLecturerWithoutElevatingRole() throws Exception {
        mvc.perform(post("/api/auth/add-lecture").header("Authorization",token("superadmin")).contentType("application/json")
                .content("{\"userID\":\"newlecturer\",\"email\":\"new@example.test\",\"password\":\"Password123!\",\"usertype\":\"lecture\",\"failedLoginAttempts\":999}"))
                .andExpect(status().isOk());
        User created=users.findByUsername("newlecturer").orElseThrow();
        assertEquals("lecture",created.getUsertype());assertEquals(0,created.getFailedLoginAttempts());
    }
    @Test void allApplicationEndpointsHaveAnExplicitPolicy() {
        requestMappingHandlerMapping.getHandlerMethods().forEach((mapping,handler)->{
            if(handler.getBeanType().getPackageName().startsWith("com.example.Software.project.Backend"))
                assertNotNull(handler.getMethodAnnotation(org.springframework.security.access.prepost.PreAuthorize.class),handler.toString());
        });
    }

    @Test void lecturerCanEditAssignedLoButCannotEditAnotherModule() throws Exception {
        mvc.perform(put("/api/lospos/OWN LO").header("Authorization",token("lecture"))
            .contentType("application/json").content("{\"name\":\"Updated\",\"description\":\"New description\"}"))
            .andExpect(status().isOk());
        mvc.perform(put("/api/lospos/OTHER LO").header("Authorization",token("lecture"))
            .contentType("application/json").content("{\"name\":\"Stolen\"}"))
            .andExpect(status().isForbidden());
        assertEquals("Updated",los.findById("OWN LO").orElseThrow().getName());
        assertEquals("LO1",los.findById("OTHER LO").orElseThrow().getName());
    }

    @Test void removingAssignmentRevokesExistingTokenImmediately() throws Exception {
        String existingToken=token("lecture");
        Module module=modules.findById("OWN").orElseThrow();module.getAssignedLecturers().clear();modules.flush();
        mvc.perform(get("/api/modules/OWN").header("Authorization",existingToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/modules/all").header("Authorization",existingToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test void lecturerSubmitsAndOnlyAdminApprovesMapping() throws Exception {
        outcomes.saveAndFlush(new ProgramOutcome("PO1","PO1","Knowledge","Description"));
        outcomes.saveAndFlush(new ProgramOutcome("PO2","PO2","Skills","Description"));
        mvc.perform(post("/api/lo-po-mapping/create").param("loId","OWN LO").header("Authorization",token("lecture"))
            .contentType("application/json").content("{\"mappings\":{\"PO1\":3,\"PO2\":2},\"approvalStatus\":\"APPROVED\"}"))
            .andExpect(status().isOk());
        OutcomeMapping mapping=mappings.findAll().get(0);
        assertEquals(OutcomeMapping.ApprovalStatus.PENDING,mapping.getStatus());
        String route="/api/lo-po-mapping/admin/"+mapping.getId()+"/approve";
        mvc.perform(put(route).header("Authorization",token("lecture")).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(put(route).header("Authorization",token("admin")).contentType("application/json").content("{}"))
            .andExpect(status().isOk());
        assertEquals(OutcomeMapping.ApprovalStatus.APPROVED,mappings.findById(mapping.getId()).orElseThrow().getStatus());
    }

    @Test void mappingListsAndStatisticsExcludeOtherModules() throws Exception {
        var po=outcomes.save(new ProgramOutcome("PO1","PO1","Knowledge","Description"));
        for(String id:List.of("OWN LO","OTHER LO"))mappings.save(new OutcomeMapping(los.findById(id).orElseThrow(),po,3,"lecture"));
        mappings.flush();
        mvc.perform(get("/api/lo-po-mapping/all").header("Authorization",token("lecture")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(get("/api/lo-po-mapping/statistics").header("Authorization",token("lecture")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalMappings").value(1));
    }

    @Test void assessmentCreationWorksForAllStaffWithinScope() throws Exception {
        for(String user:List.of("lecture","admin","superadmin")) {
            mvc.perform(post("/api/obe/assessment/template").header("Authorization",token(user))
                .contentType("application/json").content("{\"moduleId\":\"OWN\",\"items\":[{\"loId\":\"OWN LO\",\"questionNumber\":1,\"maxMarks\":100}]}"))
                .andExpect(status().isOk());
        }
    }

    @Test void uploadMetadataCannotOverrideScopeToAnotherModule() throws Exception {
        long originalMarks=marks.count();
        try(var workbook=new org.apache.poi.xssf.usermodel.XSSFWorkbook();var output=new java.io.ByteArrayOutputStream()) {
            var sheet=workbook.createSheet("METADATA");var row=sheet.createRow(0);
            row.createCell(0).setCellValue("LO_IDS");row.createCell(1).setCellValue("OTHER LO");
            workbook.createSheet("Marks");workbook.write(output);
            var file=new org.springframework.mock.web.MockMultipartFile("excelFile","marks.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",output.toByteArray());
            mvc.perform(multipart("/api/obe/marks/upload-bulk").file(file).param("losIds","OWN LO")
                .header("Authorization",token("lecture"))).andExpect(status().isForbidden());
        }
        assertEquals(originalMarks,marks.count());
    }

    @Test void legacyPermanentDeleteAlsoRequiresSuperadmin() throws Exception {
        outcomes.saveAndFlush(new ProgramOutcome("PO1","PO1","Knowledge","Description"));
        mvc.perform(delete("/api/obe/po/PO1").header("Authorization",token("admin"))).andExpect(status().isForbidden());
        assertTrue(outcomes.existsById("PO1"));
        mvc.perform(delete("/api/obe/po/PO1").header("Authorization",token("superadmin"))).andExpect(status().isOk());
        assertFalse(outcomes.existsById("PO1"));
    }

    @Test void completeStudentSummaryRequiresStudentScope() throws Exception {
        mvc.perform(get("/api/obe/po-attainment/student-summary").param("studentId","OUTSIDE")
            .header("Authorization",token("lecture"))).andExpect(status().isForbidden());
    }

    @Test void studentSummaryCannotLeakMarksFromOtherModules() throws Exception {
        Student student=students.save(new Student("S1","Test Student","student@example.test"));
        StudentMark own=new StudentMark();own.setStudent(student);own.setLos(los.findById("OWN LO").orElseThrow());own.setScore(75.0);marks.saveAndFlush(own);
        mvc.perform(get("/api/obe/po-attainment/student-summary").param("studentId","S1")
            .header("Authorization",token("lecture"))).andExpect(status().isOk());
        StudentMark other=new StudentMark();other.setStudent(student);other.setLos(los.findById("OTHER LO").orElseThrow());other.setScore(90.0);marks.saveAndFlush(other);
        mvc.perform(get("/api/obe/po-attainment/student-summary").param("studentId","S1")
            .header("Authorization",token("lecture"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/obe/po-attainment/student-summary").param("studentId","S1")
            .header("Authorization",token("admin"))).andExpect(status().isOk());
    }

    @Test void developmentAccountHelperIsNotPublicOrAdminAccessible() throws Exception {
        mvc.perform(post("/api/auth/create-test-user")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/create-test-user").header("Authorization",token("admin")))
            .andExpect(status().isForbidden());
    }

    @Test void historicalCreditsOutsideAssignmentCannotLeakThroughSummary() throws Exception {
        Student student=students.save(new Student("S1","Test Student","student@example.test"));
        StudentMark own=new StudentMark();own.setStudent(student);own.setLos(los.findById("OWN LO").orElseThrow());own.setScore(75.0);marks.saveAndFlush(own);
        var po=outcomes.save(new ProgramOutcome("PO1","PO1","Knowledge","Description"));
        StudentPoCredit credit=new StudentPoCredit();credit.setStudent(student);credit.setProgramOutcome(po);
        credit.setModule(modules.findById("OTHER").orElseThrow());credit.setBatch("22");credit.setCreditsEarned(2);credit.setMaxCredits(3);credit.setThreshold(50);credits.saveAndFlush(credit);
        mvc.perform(get("/api/obe/po-attainment/student-summary").param("studentId","S1")
            .header("Authorization",token("lecture"))).andExpect(status().isForbidden());
    }
}
