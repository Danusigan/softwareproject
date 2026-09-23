package com.example.Software.project.Backend.Reporting.Progress;

import com.example.Software.project.Backend.Model.Student;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import static com.example.Software.project.Backend.Reporting.Progress.ProgressStore.*;
import static com.example.Software.project.Backend.Reporting.Progress.ProgressReport.*;
import static com.example.Software.project.Backend.Reporting.Progress.AttainmentCalculator.*;

@Service
@Transactional
public class ProgressService {
    private final ProgressStore store;
    private final ProgressAccess access;
    private final AttainmentCalculator calculator;
    private final ObjectMapper json;
    public ProgressService(ProgressStore store, ProgressAccess access, AttainmentCalculator calculator, ObjectMapper json) {
        this.store=store; this.access=access; this.calculator=calculator; this.json=json;
    }
    public List<Map<String,Object>> search(String query, Authentication auth) {
        access.role(auth);
        ProgressConfiguration.require(query!=null&&query.length()<=100,"Search is too long");
        // LOCATE treats percent/underscore literally, unlike an unescaped LIKE expression.
        return access.search(query.trim(),auth);
    }

    public ProgressReport generate(String id, Authentication auth) {
        ProgressConfiguration.text(id,255); access.requireStudent(id,auth);
        Student student=store.find(Student.class,id);
        if(student==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Student not found");
        Map<String,Object> identity=new LinkedHashMap<>();
        identity.put("studentId",id); identity.put("studentName",student.getStudentName()); identity.put("email",student.getEmail()); identity.put("cohort",student.getBatch());
        var profile=store.one("select p.curriculum_code,p.academic_status,c.*,g.name programme_name,g.university from qa_student_programme p join qa_curriculum c on c.code=p.curriculum_code join qa_programme g on g.code=c.programme_code where p.student_id=?1",id);
        identity.put("curriculum",str(profile,"curriculum_code")); identity.put("programme",str(profile,"programme_name")); identity.put("university",str(profile,"university")); identity.put("academicStatus",str(profile,"academic_status"));
        String code=str(profile,"curriculum_code");
        Map<String,Object> policy=new LinkedHashMap<>();
        for(String key:List.of("policy_version","version","retake_policy","credit_weighted","graduation_configured","required_credits","minimum_gpa")) policy.put(key,profile.get(key));
        List<String> warnings=new ArrayList<>();
        var unassigned=store.rows("select t.id assessment,t.academic_year,t.semester,l.module_id,l.id lo_id,i.question_label question,s.score obtained,i.max_marks maximum from student_assessment_score s join assessment_item i on i.id=s.assessment_item_id join assessment_template t on t.id=i.template_id join los l on l.id=i.los_id where s.student_id=?1 and not exists(select 1 from qa_offering_item qi join qa_module_enrolment e on e.offering_code=qi.offering_code where e.student_id=s.student_id and qi.item_id=i.id) order by t.academic_year,t.semester,l.module_id,t.id,i.question_number",id);
        if(!unassigned.isEmpty()) warnings.add("Question marks exist outside configured enrolments. They are displayed as unassigned evidence and excluded from attainment; assign their offerings before drawing conclusions.");
        var legacy=store.rows("select l.module_id,l.id lo_id,s.score obtained,s.assignment_label assessment,s.batch from StudentMark s join los l on l.id=s.los_id where s.student_id=?1",id);
        if(!legacy.isEmpty()) warnings.add("Legacy LO totals are retained in the marks system but excluded: they have no reliable question maximum or attempt identity. Question-level evidence takes precedence.");
        List<ModuleResult> modules=new ArrayList<>(); List<ProgrammeResult> pos=new ArrayList<>();
        if(profile.isEmpty()) warnings.add("Programme, curriculum, enrolments and attainment policy are not configured for this student. No graduation or PO decision can be made.");
        else {
            var moduleRules=store.rows("select * from qa_curriculum_module where curriculum_code=?1 order by module_id",code);
            var loRules=store.rows("select * from qa_curriculum_lo where curriculum_code=?1 order by lo_id",code);
            var attempts=store.rows("select e.*,o.module_id,p.academic_year,p.semester,p.starts_on from qa_module_enrolment e join qa_module_offering o on o.code=e.offering_code join qa_academic_period p on p.code=o.period_code where e.student_id=?1 order by p.starts_on,e.attempt_number",id);
            var marks=store.rows("select qi.*,s.score from qa_offering_item qi join qa_module_enrolment e on e.offering_code=qi.offering_code left join student_assessment_score s on s.assessment_item_id=qi.item_id and s.student_id=e.student_id where e.student_id=?1 order by qi.offering_code,qi.item_id",id);
            Map<String,List<Map<String,Object>>> marksByOffering=marks.stream().collect(Collectors.groupingBy(r->str(r,"offering_code")));
            for(var rule:moduleRules) {
                String module=str(rule,"module_id");
                var moduleAttempts=attempts.stream().filter(a->module.equals(str(a,"module_id"))).toList();
                var chosen=calculator.select(moduleAttempts.stream().map(a->new Attempt(str(a,"offering_code"),integer(a,"attempt_number"),str(a,"status"),bool(a,"official"),dec(a,"final_mark"))).toList(),str(profile,"retake_policy"));
                String selected=chosen.map(Attempt::offering).orElse(null);
                var definitions=loRules.stream().filter(l->module.equals(str(l,"module_id"))).toList();
                List<ModuleAttempt> history=new ArrayList<>();
                for(var a:moduleAttempts) {
                    String offering=str(a,"offering_code"), status=str(a,"status");
                    List<LearningResult> results=learning(definitions,marksByOffering.getOrDefault(offering,List.of()),"IN_PROGRESS".equals(status),Set.of("PASS","FAIL","IN_PROGRESS").contains(status));
                    history.add(new ModuleAttempt(offering,integer(a,"attempt_number"),str(a,"academic_year"),str(a,"semester"),str(a,"starts_on"),status,offering.equals(selected),dec(a,"final_mark"),str(a,"grade"),dec(a,"grade_points"),results));
                }
                List<LearningResult> selectedLos=history.stream().filter(ModuleAttempt::selected).findFirst().map(ModuleAttempt::los)
                        .orElseGet(()->learning(definitions,List.of(),false,false));
                if(selected==null) warnings.add(module+": no unambiguous attempt selected by "+str(profile,"retake_policy")+" policy.");
                modules.add(new ModuleResult(module,str(rule,"module_name"),dec(rule,"credits"),bool(rule,"compulsory"),selected,history,selectedLos));
            }
            var mappings=store.rows("select * from qa_curriculum_mapping where curriculum_code=?1 order by lo_id,po_id",code);
            for(var po:store.rows("select * from qa_curriculum_po where curriculum_code=?1 order by code",code)) {
                List<Evidence> evidence=new ArrayList<>();
                for(var mapping:mappings) {
                    if(!Objects.equals(str(po,"po_id"),str(mapping,"po_id"))) continue;
                    for(var module:modules) for(var lo:module.los()) if(lo.loId().equals(str(mapping,"lo_id")))
                        evidence.add(new Evidence(module.moduleId(),lo.loId(),lo.result().percentage(),dec(mapping,"weight"),bool(profile,"credit_weighted")?module.credits():BigDecimal.ONE,lo.result().status(),lo.marks()));
                }
                pos.add(new ProgrammeResult(str(po,"po_id"),str(po,"code"),str(po,"description"),bool(po,"required"),integer(po,"minimum_evidence"),calculator.po(evidence,dec(po,"threshold"),integer(po,"minimum_evidence"))));
            }
        }
        Summary summary=summary(modules,pos,profile,unassigned.isEmpty(),warnings);
        String conclusion=conclusion(summary);
        ProgressReport report=new ProgressReport(UUID.randomUUID().toString(),Instant.now(),identity,policy,summary,modules,pos,unassigned,warnings,conclusion);
        store.execute("insert into qa_report_snapshot(reference,student_id,generated_by,policy_version,content) values (?1,?2,?3,?4,?5)",report.reference(),id,auth.getName(),str(profile,"policy_version"),serialize(report));
        audit(report.reference(),auth,"GENERATE");
        return report;
    }

    private List<LearningResult> learning(List<Map<String,Object>> definitions,List<Map<String,Object>> rows,boolean inProgress,boolean usable) {
        return definitions.stream().map(d->{
            List<Mark> marks=rows.stream().filter(r->Objects.equals(str(d,"lo_id"),str(r,"lo_id")))
                    .map(r->new Mark(str(r,"assessment_name"),str(r,"question"),dec(r,"score"),dec(r,"maximum"))).toList();
            Result result=calculator.lo(usable?marks:List.of(),dec(d,"threshold"),inProgress);
            return new LearningResult(str(d,"lo_id"),str(d,"name"),str(d,"description"),result,marks);
        }).toList();
    }
    private Summary summary(List<ModuleResult> modules,List<ProgrammeResult> pos,Map<String,Object> profile,boolean assigned,List<String> warnings) {
        BigDecimal attempted=BigDecimal.ZERO,completed=BigDecimal.ZERO,points=BigDecimal.ZERO,gpaCredits=BigDecimal.ZERO;
        boolean missingGpa=false,compulsoryMet=true,pending=false;
        for(var module:modules) {
            attempted=attempted.add(module.credits().multiply(BigDecimal.valueOf(module.attempts().stream().filter(a->!Set.of("WITHDRAWN","EXEMPT").contains(a.status())).count())));
            var selected=module.attempts().stream().filter(ModuleAttempt::selected).findFirst();
            boolean passed=selected.isPresent() && "PASS".equals(selected.get().status());
            if(passed) completed=completed.add(module.credits());
            if(module.compulsory()&&!passed) compulsoryMet=false;
            if(module.attempts().stream().anyMatch(a->"IN_PROGRESS".equals(a.status()))) pending=true;
            if(selected.isPresent() && Set.of("PASS","FAIL").contains(selected.get().status())) {
                if(selected.get().gradePoints()==null) missingGpa=true;
                else { points=points.add(selected.get().gradePoints().multiply(module.credits())); gpaCredits=gpaCredits.add(module.credits()); }
            }
        }
        BigDecimal gpa=missingGpa||gpaCredits.signum()==0?null:points.divide(gpaCredits,MathContext.DECIMAL128);
        String academic;
        if(!bool(profile,"graduation_configured")) { academic="POLICY_NOT_CONFIGURED"; warnings.add("Academic graduation rules require QA configuration and approval. Exemptions and absences never imply a pass or outcome evidence."); }
        else if("IN_PROGRESS".equals(str(profile,"academic_status"))||pending) academic="IN_PROGRESS";
        else if("WITHDRAWN".equals(str(profile,"academic_status"))) academic="REQUIREMENTS_NOT_MET";
        else if(dec(profile,"minimum_gpa")!=null&&gpa==null) { academic="IN_PROGRESS"; warnings.add("Required GPA cannot be calculated because grade points are incomplete."); }
        else academic=completed.compareTo(dec(profile,"required_credits"))>=0&&compulsoryMet&&(dec(profile,"minimum_gpa")==null||gpa.compareTo(dec(profile,"minimum_gpa"))>=0)?"REQUIREMENTS_MET":"REQUIREMENTS_NOT_MET";
        if(!assigned&&"REQUIREMENTS_MET".equals(academic)) { academic="IN_PROGRESS"; warnings.add("Academic completion cannot be confirmed while marks remain outside the enrolment history."); }
        var required=pos.stream().filter(ProgrammeResult::required).map(p->p.calculation().result().status()).toList();
        Status po=required.isEmpty()||required.contains(Status.INSUFFICIENT_EVIDENCE)?Status.INSUFFICIENT_EVIDENCE:
                required.contains(Status.IN_PROGRESS)?Status.IN_PROGRESS:required.contains(Status.NOT_ACHIEVED)?Status.NOT_ACHIEVED:Status.ACHIEVED;
        var los=modules.stream().flatMap(m->m.los().stream()).map(l->l.result().status()).toList();
        boolean complete=assigned&&!profile.isEmpty()&&!los.isEmpty()&&los.stream().allMatch(s->s==Status.ACHIEVED||s==Status.NOT_ACHIEVED)&&pos.stream().allMatch(p->Set.of(Status.ACHIEVED,Status.NOT_ACHIEVED).contains(p.calculation().result().status()));
        if(!complete) warnings.add("Outcome evidence is incomplete. Provisional PO percentages use only complete LO evidence, including unsuccessful LOs; an incomplete PO cannot be achieved.");
        return new Summary(attempted,completed,gpa,academic,po,complete?"COMPLETE":"INCOMPLETE",los.stream().filter(s->s==Status.ACHIEVED).count(),los.stream().filter(s->s==Status.NOT_ACHIEVED).count(),pos.stream().filter(p->p.calculation().result().status()==Status.ACHIEVED).count(),pos.stream().filter(p->p.calculation().result().status()==Status.NOT_ACHIEVED).count());
    }
    private String conclusion(Summary s) {
        return switch(s.academicStatus()) {
            case "REQUIREMENTS_MET" -> s.poStatus()==Status.ACHIEVED ? "The student has satisfied the configured academic graduation requirements and demonstrated the required Programme Outcome attainment levels." : s.poStatus()==Status.NOT_ACHIEVED ? "Academic requirements are met, but one or more required POs are not achieved." : "Academic requirements are met, but PO evidence is incomplete.";
            case "REQUIREMENTS_NOT_MET" -> "Academic requirements have not been met.";
            case "IN_PROGRESS" -> "Academic study is still in progress.";
            default -> "Academic graduation policy is not configured; an overall completion decision is unavailable.";
        };
    }
    public ProgressReport snapshot(String reference,Authentication auth,String action) {
        ProgressConfiguration.require(reference!=null&&reference.matches("[a-f0-9-]{36}"),"Invalid report reference");
        var row=store.one("select student_id,content from qa_report_snapshot where reference=?1",reference);
        if(row.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Report not found");
        access.requireStudent(str(row,"student_id"),auth);
        try { ProgressReport report=json.readValue(str(row,"content"),ProgressReport.class); audit(reference,auth,action); return report; }
        catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Saved report cannot be read",e); }
    }
    private String serialize(ProgressReport report) {
        try { return json.writeValueAsString(report); } catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Report could not be saved",e); }
    }
    private void audit(String reference,Authentication auth,String action) {
        store.execute("insert into qa_report_audit(reference,snapshot_reference,actor,action) values (?1,?2,?3,?4)",UUID.randomUUID().toString(),reference,auth.getName(),action);
    }
}
