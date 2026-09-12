package com.example.Software.project.Backend.Reporting.Progress;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Model.Module;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.Software.project.Backend.Reporting.Progress.ProgressStore.*;

@Service
@Transactional
public class ProgressConfiguration {
    public record ModuleRule(String moduleId, BigDecimal credits, boolean compulsory) {}
    public record LoRule(String loId, BigDecimal threshold) {}
    public record PoRule(String poId, BigDecimal threshold, int minimumEvidence, boolean required) {}
    public record Curriculum(String code, String programmeCode, String programmeName, String university,
            String version, String cohort, String policyVersion, String retakePolicy, boolean creditWeighted,
            boolean graduationConfigured, BigDecimal requiredCredits, BigDecimal minimumGpa,
            List<ModuleRule> modules, List<LoRule> los, List<PoRule> pos) {}
    public record Profile(String studentId, String curriculumCode, String accountUsername, String academicStatus) {}
    public record Offering(String code, String curriculumCode, String moduleId, String periodCode,
            String academicYear, String semester, LocalDate startsOn, List<String> assessmentIds) {}
    public record Enrolment(String studentId, String offeringCode, int attemptNumber, boolean official,
            String status, BigDecimal finalMark, String grade, BigDecimal gradePoints) {}
    private final ProgressStore store;
    private final ProgressAccess access;
    public ProgressConfiguration(ProgressStore store, ProgressAccess access) { this.store = store; this.access = access; }

    public Map<String,Object> catalog(Authentication auth) {
        access.requireAdmin(auth);
        return Map.of("curricula", store.rows("select * from qa_curriculum order by code"),
                "modules", store.rows("select module_id,module_name from modules order by module_id"),
                "los", store.rows("select id,module_id,name,attainment_threshold from los order by id"),
                "pos", store.rows("select po_id,po_code,title from program_outcomes order by po_code"),
                "mappings", store.rows("select los_id,program_outcome_id,weight,status from lo_po_mappings"),
                "assessments", store.rows("select id,module_id,batch,academic_year,semester from assessment_template order by id"),
                "offerings", store.rows("select * from qa_module_offering order by code"));
    }

    public void curriculum(Curriculum c, Authentication auth) {
        access.requireAdmin(auth);
        text(c.code(),80); text(c.programmeCode(),80); text(c.programmeName(),255); text(c.university(),255);
        text(c.version(),80); text(c.cohort(),50); text(c.policyVersion(),80);
        require(c.retakePolicy()!=null&&Set.of("OFFICIAL","LATEST_COMPLETED","BEST").contains(c.retakePolicy()), "Choose a supported retake policy");
        require(c.modules()!=null && !c.modules().isEmpty() && c.los()!=null && c.pos()!=null && !c.pos().isEmpty(), "Modules, LOs and POs are required");
        require(c.modules().stream().noneMatch(Objects::isNull)&&c.los().stream().noneMatch(Objects::isNull)&&c.pos().stream().noneMatch(Objects::isNull),"Configuration lists cannot contain null records");
        require(!c.graduationConfigured() || c.requiredCredits()!=null, "Approved graduation rules require total credits");
        if(c.requiredCredits()!=null) positive(c.requiredCredits());
        if(c.minimumGpa()!=null) require(c.minimumGpa().signum()>=0 && c.minimumGpa().scale()<=4,"Invalid minimum GPA");
        require(store.rows("select code from qa_curriculum where code=?1",c.code()).isEmpty(),"Curriculum is immutable; use a new code and version");
        var programme=store.one("select * from qa_programme where code=?1",c.programmeCode());
        if(programme.isEmpty()) store.execute("insert into qa_programme(code,name,university) values (?1,?2,?3)",c.programmeCode(),c.programmeName(),c.university());
        else require(c.programmeName().equals(str(programme,"name")) && c.university().equals(str(programme,"university")),"Programme details differ from the saved programme");
        store.execute("insert into qa_curriculum(code,programme_code,version,cohort,policy_version,retake_policy,credit_weighted,graduation_configured,required_credits,minimum_gpa,created_by) values (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11)",
                c.code(),c.programmeCode(),c.version(),c.cohort(),c.policyVersion(),c.retakePolicy(),c.creditWeighted(),c.graduationConfigured(),c.requiredCredits(),c.minimumGpa(),auth.getName());
        Set<String> modules=new HashSet<>(), los=new HashSet<>(), pos=new HashSet<>();
        for(var m:c.modules()) {
            require(modules.add(m.moduleId()),"Duplicate module"); positive(m.credits());
            Module source=store.find(Module.class,m.moduleId()); require(source!=null,"Unknown module");
            store.execute("insert into qa_curriculum_module values (?1,?2,?3,?4,?5)",c.code(),m.moduleId(),source.getModuleName(),m.credits(),m.compulsory());
        }
        for(var l:c.los()) {
            require(los.add(l.loId()),"Duplicate LO"); AttainmentCalculator.validateThreshold(l.threshold()); decimal(l.threshold());
            Los source=store.find(Los.class,l.loId()); require(source!=null && modules.contains(source.getModuleId()),"LO must belong to a curriculum module");
            store.execute("insert into qa_curriculum_lo values (?1,?2,?3,?4,?5,?6)",c.code(),l.loId(),source.getModuleId(),source.getName(),source.getDescription(),l.threshold());
        }
        for(String module:modules) require(c.los().stream().anyMatch(l -> module.equals(store.find(Los.class,l.loId()).getModuleId())),"Each module needs at least one LO");
        for(var p:c.pos()) {
            require(pos.add(p.poId()),"Duplicate PO"); AttainmentCalculator.validateThreshold(p.threshold()); decimal(p.threshold());
            require(p.minimumEvidence()>=1,"Minimum PO evidence must be positive");
            ProgramOutcome source=store.find(ProgramOutcome.class,p.poId()); require(source!=null,"Unknown PO");
            store.execute("insert into qa_curriculum_po values (?1,?2,?3,?4,?5,?6,?7)",c.code(),p.poId(),source.getCode(),source.getTitle()+": "+Objects.toString(source.getDescription(),""),p.threshold(),p.minimumEvidence(),p.required());
        }
        require(c.pos().stream().anyMatch(PoRule::required),"At least one required PO is needed");
        Set<String> seen=new HashSet<>();
        for(var m:store.rows("select los_id,program_outcome_id,weight from lo_po_mappings where status='APPROVED' and weight>0")) {
            String lo=str(m,"los_id"), po=str(m,"program_outcome_id");
            if(!los.contains(lo)||!pos.contains(po)) continue;
            require(seen.add(lo+"|"+po),"Resolve duplicate approved LO-to-PO mappings before publishing");
            store.execute("insert into qa_curriculum_mapping values (?1,?2,?3,?4)",c.code(),lo,po,dec(m,"weight"));
        }
    }

    public void profile(Profile p, Authentication auth) {
        access.requireAdmin(auth); text(p.studentId(),255); text(p.curriculumCode(),80);
        require(p.academicStatus()!=null&&Set.of("IN_PROGRESS","COMPLETED","WITHDRAWN").contains(p.academicStatus()),"Invalid academic status");
        Student student=store.find(Student.class,p.studentId()); require(student!=null,"Unknown student");
        var curriculum=store.one("select * from qa_curriculum where code=?1",p.curriculumCode()); require(!curriculum.isEmpty(),"Unknown curriculum");
        require(student.getBatch()!=null && student.getBatch().equals(str(curriculum,"cohort")),"Student batch must match the curriculum cohort");
        String account=p.accountUsername()==null||p.accountUsername().isBlank()?null:p.accountUsername();
        if(account!=null) { User user=store.find(User.class,account); require(user!=null && "student".equalsIgnoreCase(user.getUsertype()),"Link an existing student account"); }
        var old=store.one("select * from qa_student_programme where student_id=?1",p.studentId());
        if(old.isEmpty()) store.execute("insert into qa_student_programme(student_id,curriculum_code,account_username,academic_status) values (?1,?2,?3,?4)",p.studentId(),p.curriculumCode(),account,p.academicStatus());
        else {
            require(p.curriculumCode().equals(str(old,"curriculum_code")),"Changing an assigned curriculum requires a reviewed data migration");
            store.execute("update qa_student_programme set account_username=?1,academic_status=?2,updated_at=CURRENT_TIMESTAMP where student_id=?3",account,p.academicStatus(),p.studentId());
        }
    }
    public void offering(Offering o, Authentication auth) {
        access.requireAdmin(auth); text(o.code(),80); text(o.periodCode(),80); text(o.academicYear(),50); text(o.semester(),50);
        require(o.startsOn()!=null && o.assessmentIds()!=null,"Period date and assessment list are required");
        require(!store.rows("select module_id from qa_curriculum_module where curriculum_code=?1 and module_id=?2",o.curriculumCode(),o.moduleId()).isEmpty(),"Unknown curriculum module");
        var period=store.one("select * from qa_academic_period where code=?1",o.periodCode());
        if(period.isEmpty()) store.execute("insert into qa_academic_period values (?1,?2,?3,?4)",o.periodCode(),o.academicYear(),o.semester(),java.sql.Date.valueOf(o.startsOn()));
        else require(o.academicYear().equals(str(period,"academic_year")) && o.semester().equals(str(period,"semester")) && o.startsOn().toString().equals(str(period,"starts_on")),"Period details differ from the saved period");
        store.execute("insert into qa_module_offering(code,curriculum_code,module_id,period_code) values (?1,?2,?3,?4)",o.code(),o.curriculumCode(),o.moduleId(),o.periodCode());
        String cohort=str(store.one("select cohort from qa_curriculum where code=?1",o.curriculumCode()),"cohort");
        for(String id:o.assessmentIds()) {
            AssessmentTemplate t=store.find(AssessmentTemplate.class,id);
            require(t!=null && t.getModule()!=null && o.moduleId().equals(t.getModule().getModuleId()) && cohort.equals(t.getBatch()) && o.academicYear().equals(t.getAcademicYear()) && o.semester().equals(t.getSemester()),"Assessment must match offering module, cohort and period");
            store.execute("insert into qa_offering_assessment values (?1,?2)",o.code(),id);
            var items=store.rows("select id,los_id,question_label,max_marks from assessment_item where template_id=?1",id);
            require(!items.isEmpty(),"An offering assessment needs question-level evidence definitions");
            for(var item:items) {
                positive(dec(item,"max_marks"));
                require(!store.rows("select lo_id from qa_curriculum_lo where curriculum_code=?1 and lo_id=?2 and module_id=?3",o.curriculumCode(),str(item,"los_id"),o.moduleId()).isEmpty(),"Assessment LO is outside the curriculum module");
                store.execute("insert into qa_offering_item values (?1,?2,?3,?4,?5,?6,?7)",o.code(),item.get("id"),o.curriculumCode(),str(item,"los_id"),t.getName()==null?id:t.getName(),str(item,"question_label"),dec(item,"max_marks"));
            }
        }
    }
    public void enrolment(Enrolment e, Authentication auth) {
        access.requireAdmin(auth); require(e.attemptNumber()>0,"Attempt number must be positive");
        require(e.status()!=null&&Set.of("IN_PROGRESS","PASS","FAIL","WITHDRAWN","EXEMPT","ABSENT").contains(e.status()),"Invalid enrolment status");
        if(e.finalMark()!=null) { AttainmentCalculator.validateThreshold(e.finalMark()); decimal(e.finalMark()); }
        if(e.grade()!=null) text(e.grade(),30);
        if(e.gradePoints()!=null) { require(e.gradePoints().signum()>=0,"Grade points cannot be negative"); decimal(e.gradePoints()); }
        // Lock the student's profile to serialize attempt/official-flag validation.
        var profile=store.one("select * from qa_student_programme where student_id=?1 for update",e.studentId());
        var offering=store.one("select * from qa_module_offering where code=?1",e.offeringCode());
        require(!profile.isEmpty()&&!offering.isEmpty()&&Objects.equals(str(profile,"curriculum_code"),str(offering,"curriculum_code")),"Enrolment must match the student's curriculum");
        for(var old:store.rows("select e.* from qa_module_enrolment e join qa_module_offering o on o.code=e.offering_code where e.student_id=?1 and o.module_id=?2 and e.offering_code<>?3",e.studentId(),str(offering,"module_id"),e.offeringCode())) {
            require(integer(old,"attempt_number")!=e.attemptNumber(),"Attempt number already exists for this module");
            require(!e.official()||!bool(old,"official"),"Clear the previous official attempt before selecting another");
        }
        if(store.rows("select student_id from qa_module_enrolment where student_id=?1 and offering_code=?2",e.studentId(),e.offeringCode()).isEmpty())
            store.execute("insert into qa_module_enrolment(student_id,offering_code,attempt_number,official,status,final_mark,grade,grade_points) values (?1,?2,?3,?4,?5,?6,?7,?8)",e.studentId(),e.offeringCode(),e.attemptNumber(),e.official(),e.status(),e.finalMark(),e.grade(),e.gradePoints());
        else store.execute("update qa_module_enrolment set attempt_number=?3,official=?4,status=?5,final_mark=?6,grade=?7,grade_points=?8,updated_at=CURRENT_TIMESTAMP where student_id=?1 and offering_code=?2",e.studentId(),e.offeringCode(),e.attemptNumber(),e.official(),e.status(),e.finalMark(),e.grade(),e.gradePoints());
    }
    static void require(boolean condition,String message) { if(!condition) throw new IllegalArgumentException(message); }
    static void text(String value,int length) { require(value!=null&&!value.isBlank()&&value.length()<=length,"Required text is empty or too long"); }
    static void decimal(BigDecimal value) { require(value.scale()<=4&&value.precision()<=12,"Use at most four decimal places and twelve digits"); }
    static void positive(BigDecimal value) { require(value!=null&&value.signum()>0,"Credits and weights must be positive"); decimal(value); }
}
