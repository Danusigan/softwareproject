package com.example.Software.project.Backend.Reporting.Progress;

import com.example.Software.project.Backend.Service.ModuleService;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static com.example.Software.project.Backend.Reporting.Progress.ProgressStore.str;

@Service
public class ProgressAccess {
    private final ProgressStore store;
    private final ModuleService modules;
    public ProgressAccess(ProgressStore store, ModuleService modules) { this.store = store; this.modules = modules; }
    public String role(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        return auth.getAuthorities().stream().map(a -> a.getAuthority().toLowerCase(Locale.ROOT))
                .filter(r -> Set.of("admin", "superadmin", "lecture", "student").contains(r)).findFirst().orElse("");
    }
    public boolean admin(Authentication auth) { return Set.of("admin", "superadmin").contains(role(auth)); }
    public void requireAdmin(Authentication auth) {
        if (!admin(auth)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "QA administrator access required");
    }
    public List<Map<String,Object>> search(String query, Authentication auth) {
        String role=role(auth);
        String base="select s.student_id,s.student_name,s.batch,p.curriculum_code,p.academic_status from students s left join qa_student_programme p on p.student_id=s.student_id where (locate(lower(?1),lower(s.student_id))>0 or locate(lower(?1),lower(s.student_name))>0)";
        String end=" order by s.student_id limit 100";
        if(Set.of("admin","superadmin").contains(role)) return store.rows(base+end,query);
        if("student".equals(role)) return store.rows(base+" and p.account_username=?2"+end,query,auth.getName());
        if(!"lecture".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Report access is not available for this role");
        var ids=modules.getModulesForLecturer(auth.getName()).stream().map(m->m.getModuleId()).toList();
        if(ids.isEmpty()) return List.of();
        // One scoped search query; student count does not increase the query count.
        String enrolment="select o.module_id from qa_module_enrolment e join qa_module_offering o on o.code=e.offering_code where e.student_id=s.student_id";
        String questions="select l.module_id from student_assessment_score q join assessment_item i on i.id=q.assessment_item_id join los l on l.id=i.los_id where q.student_id=s.student_id";
        String legacy="select l.module_id from StudentMark m join los l on l.id=m.los_id where m.student_id=s.student_id";
        String scope=" and (exists("+enrolment+") or exists("+questions+") or exists("+legacy+"))"+
                " and not exists("+enrolment+" and o.module_id not in (?2))"+
                " and not exists("+questions+" and l.module_id not in (?2))"+
                " and not exists("+legacy+" and l.module_id not in (?2))";
        return store.rows(base+scope+end,query,ids);
    }
    public boolean allowed(String student, Authentication auth) {
        String role = role(auth);
        if (Set.of("admin", "superadmin").contains(role)) return true;
        if ("student".equals(role)) return !store.rows("select student_id from qa_student_programme where student_id=?1 and account_username=?2", student, auth.getName()).isEmpty();
        if (!"lecture".equals(role)) return false;
        Set<String> visible = new HashSet<>(modules.getModulesForLecturer(auth.getName()).stream().map(m -> m.getModuleId()).toList());
        List<Map<String,Object>> scope = store.rows("select distinct o.module_id from qa_module_enrolment e join qa_module_offering o on o.code=e.offering_code where e.student_id=?1 " +
                "union select distinct l.module_id from student_assessment_score s join assessment_item i on i.id=s.assessment_item_id join los l on l.id=i.los_id where s.student_id=?1 " +
                "union select distinct l.module_id from StudentMark s join los l on l.id=s.los_id where s.student_id=?1", student);
        return !scope.isEmpty() && scope.stream().allMatch(r -> visible.contains(str(r,"module_id")));
    }
    public void requireStudent(String student, Authentication auth) {
        if (!allowed(student, auth)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account cannot access the student's complete academic report");
    }
}
