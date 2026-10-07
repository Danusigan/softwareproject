package com.example.Software.project.Backend.Security;

import com.example.Software.project.Backend.Model.*;
import com.example.Software.project.Backend.Repository.*;
import com.example.Software.project.Backend.Reporting.Progress.ProgressAccess;
import com.example.Software.project.Backend.Service.ExcelImportService;
import com.example.Software.project.Backend.Service.FileValidationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

/** Shared API policy. Endpoint annotations supply their operation and resolved request arguments. */
@Component("accessPolicy")
@Transactional(readOnly = true)
public class AccessPolicy {
    private final ModuleRepository modules;
    private final LosRepository los;
    private final OutcomeMappingRepository mappings;
    private final AssessmentTemplateRepository templates;
    private final AssessmentItemRepository items;
    private final StudentMarkRepository marks;
    private final CqiActionRepository cqi;
    private final ProgressAccess progress;
    private final StudentPoCreditRepository credits;
    private final ObjectProvider<ExcelImportService> imports;
    private final FileValidationService files;
    private final ObjectMapper json;

    public AccessPolicy(ModuleRepository modules, LosRepository los, OutcomeMappingRepository mappings,
            AssessmentTemplateRepository templates, AssessmentItemRepository items, StudentMarkRepository marks,
            CqiActionRepository cqi, ProgressAccess progress, StudentPoCreditRepository credits, ObjectProvider<ExcelImportService> imports,
            FileValidationService files, ObjectMapper json) {
        this.modules=modules; this.los=los; this.mappings=mappings; this.templates=templates;
        this.items=items; this.marks=marks; this.cqi=cqi; this.progress=progress; this.credits=credits; this.imports=imports;
        this.files=files; this.json=json;
    }

    public boolean moduleAllowed(String id) {
        if (CurrentUser.admin()) return true;
        if (!"lecture".equals(CurrentUser.role()) || id == null) return false;
        return modules.findById(id).filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .map(m -> m.getAssignedLecturers() != null && m.getAssignedLecturers().stream()
                    .anyMatch(u -> CurrentUser.username().equals(u.getUserID()))).orElse(false);
    }
    public void requireModule(String id) { if (!moduleAllowed(id)) deny(); }
    public void requireLo(String id) {
        var lo=los.findById(id).orElseThrow(AccessPolicy::denied);
        requireModule(lo.getModuleId());
    }
    public void requireTemplate(String id) {
        var template=templates.findById(id).orElseThrow(AccessPolicy::denied);
        requireModule(template.getModule() == null ? null : template.getModule().getModuleId());
        for(var item:items.findByAssessmentTemplate_IdOrderByQuestionNumber(id)) {
            if(item.getLos()!=null) requireLo(item.getLos().getId());
        }
    }
    private static AccessDeniedException denied() { return new AccessDeniedException("Access denied"); }
    private static void deny() { throw denied(); }

    public boolean allow(String operation, Map<String,Object> args) {
        try { return authorize(operation, args); }
        catch (AccessDeniedException | IllegalArgumentException | org.springframework.web.server.ResponseStatusException exception) { return false; }
    }
    public boolean importLos(String[] ids) {
        if(CurrentUser.admin()) return true;
        try { if(ids==null || ids.length==0)return false;for(String id:ids)requireLo(id.trim());return true; }
        catch(AccessDeniedException exception){return false;}
    }
    public boolean importTemplate(String id) {
        if(CurrentUser.admin())return true;
        try {if(id==null)return false;requireTemplate(id.trim());return true;}catch(AccessDeniedException exception){return false;}
    }
    private boolean authorize(String operation, Map<String,Object> args) {
        String role=CurrentUser.role();
        // Public operations retain their independent token/profile checks.
        if(Set.of("UserRestController.loginUser", "UserRestController.forgotPassword",
                "UserRestController.resetPassword").contains(operation)) return true;
        if(role.isEmpty()) return false;
        String controller=operation.substring(0,operation.indexOf('.'));
        String method=operation.substring(operation.indexOf('.')+1);
        boolean admin=CurrentUser.admin();
        if(!Set.of("UserRestController","ModuleRestController","ProgramOutcomeRestController",
                "StudentController","ProfileRestController","OBEController","LosRestController",
                "LOPOMappingRestController","IntegratedLORestController","AssessmentController",
                "CqiActionController","BatchReportController","PoReportController","ProgressController").contains(controller)) return false;
        if(operation.equals("OBEController.deletePO") || method.toLowerCase(Locale.ROOT).contains("harddelete") || method.toLowerCase(Locale.ROOT).contains("permanent"))
            return "superadmin".equals(role);
        if(controller.equals("UserRestController")) {
            if(Set.of("addAdmin","getAllAdmins","updateAdmin","deleteAdmin","createTestUser").contains(method)) return "superadmin".equals(role);
            return admin;
        }
        if(controller.equals("StudentController") || controller.equals("PoReportController")) return admin;
        if(controller.equals("ProfileRestController")) return true; // methods derive owner from Authentication
        if(controller.equals("ProgramOutcomeRestController")) {
            return admin || Set.of("getAllActivePOs","getPOById","getDefaultPOs","getCustomPOs","getPOsByCategory").contains(method);
        }
        if(controller.equals("ModuleRestController") && !Set.of("getAllModules","getModuleById").contains(method)) return admin;
        if(controller.equals("LOPOMappingRestController") && Set.of("getDeletedMappings","restoreMapping",
                "getPendingMappings","approveMapping","rejectMapping","bulkApproveMappingsForLO","getModuleMappingReport","healthCheck").contains(method)) return admin;
        if(controller.equals("CqiActionController") && Set.of("getPending","getLoHistory","approvePlan","returnPlan",
                "createPoCqiPlan","getCqiPlansForBatch","updateCqiPlanStatus","updateCqiPlanDetails").contains(method)) return admin;
        if(controller.equals("OBEController") && Set.of("createPO","updatePO","deletePO","approveMapping").contains(method)) return admin;
        // All administrators have access to module data; superadmin-only paths were handled above.
        if(admin) return true;
        if(controller.equals("ProgressController")) return true; // ProgressAccess checks configuration, students and saved snapshots
        if(controller.equals("BatchReportController")) return true; // service scopes both options and selected modules
        if(controller.equals("OBEController") && method.equals("getStudentPOSummary")) {
            String studentId=Objects.toString(args.get("studentId"), "").trim();
            progress.requireStudent(studentId, SecurityContextHolder.getContext().getAuthentication());
            for(var credit:credits.findByStudent_StudentId(studentId)) requireModule(credit.getModule().getModuleId());
            return true;
        }
        if(controller.equals("OBEController") && method.equals("getOverallPOAttainment")) return false;

        int scoped=0;
        for(var entry:args.entrySet()) {
            String name=entry.getKey(); Object value=entry.getValue();
            if(value == null || value instanceof String s && s.isBlank()) continue;
            if(name.equals("token") || name.equals("authorization") || name.equals("auth")) continue;
            if(operation.equals("OBEController.generateQuestionMarkTemplate") && name.equals("templateId")) {
                if(templates.existsById(value.toString())) requireTemplate(value.toString());
                continue; // A new ID is allowed only with an authorized module below.
            }
            if(name.equals("id")) {
                if(controller.equals("ModuleRestController")) {requireModule(value.toString()); scoped++;}
                if(controller.equals("LosRestController")) {requireLo(value.toString()); scoped++;}
                if(controller.equals("CqiActionController")) {
                    var action=cqi.findById(Long.valueOf(value.toString())).orElseThrow(AccessPolicy::denied);
                    requireModule(action.getModule()!=null?action.getModule().getModuleId():action.getModuleId()); scoped++;
                }
            } else if(value instanceof MultipartFile file) {
                files.validateExcelFile(file);
                Map<String,String> meta=imports.getObject().readMetadata(file);
                for(String key:List.of("MODULE_ID","TEMPLATE_ID","LO_IDS","LOS_IDS")) {
                    if(meta.containsKey(key) && !meta.get(key).isBlank()) scoped+=scope(key, json.valueToTree(meta.get(key)));
                }
            } else if(value instanceof Los lo) {
                if(lo.getModuleId()!=null){requireModule(lo.getModuleId());scoped++;}
                if(lo.getId()!=null && los.existsById(lo.getId())){requireLo(lo.getId());scoped++;}
            } else if(value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof OutcomeMapping) {
                for(Object object:list) {
                    OutcomeMapping mapping=(OutcomeMapping)object;
                    if(mapping.getId()!=null) requireLo(mappings.findById(mapping.getId()).orElseThrow(AccessPolicy::denied).getLearningOutcomeId());
                    if(mapping.getLearningOutcome()==null)deny();
                    requireLo(mapping.getLearningOutcome().getId()); scoped++;
                }
            } else scoped+=scope(name,json.valueToTree(value));
        }
        // Create requests must not overwrite a pre-existing object in somebody else's module.
        if(controller.equals("AssessmentController") && method.equals("createTemplate") && args.get("payload") instanceof Map<?,?> body
                && body.get("id")!=null && templates.existsById(body.get("id").toString())) requireTemplate(body.get("id").toString());
        if(scoped>0) return true;
        return Set.of("ModuleRestController.getAllModules", "LOPOMappingRestController.getAllMappings",
                "LOPOMappingRestController.getMappingStatistics", "LOPOMappingRestController.getProgramOutcomes",
                "CqiActionController.getMyPlans", "LosRestController.exportExcel", "OBEController.getAllPOs", "OBEController.getPOById").contains(operation);
    }

    private int scope(String name, JsonNode value) {
        if(value==null || value.isNull())return 0;
        if(Set.of("moduleId","MODULE_ID").contains(name) && value.isValueNode()) {requireModule(value.asText());return 1;}
        if(Set.of("loId","losId","learningOutcomeId").contains(name) && value.isValueNode()) {requireLo(value.asText());return 1;}
        if(Set.of("templateId","TEMPLATE_ID").contains(name) && value.isValueNode()) {requireTemplate(value.asText());return 1;}
        if(name.equals("mappingId") && value.isValueNode()) {requireLo(mappings.findById(value.asLong()).orElseThrow(AccessPolicy::denied).getLearningOutcomeId());return 1;}
        if(name.equals("markId") && value.isValueNode()) {requireLo(marks.findById(value.asLong()).orElseThrow(AccessPolicy::denied).getLos().getId());return 1;}
        if(Set.of("losIds","loIds","losIdsParam","LO_IDS","LOS_IDS","moduleIds").contains(name)) {
            List<String> ids=new ArrayList<>();
            if(value.isArray())value.forEach(v->ids.add(v.asText()));else ids.addAll(Arrays.asList(value.asText().split(",")));
            for(String id:ids) {if(name.equals("moduleIds"))requireModule(id.trim());else requireLo(id.trim());}
            return ids.size();
        }
        if(Set.of("loThresholds","perLoMaxMarks").contains(name) && value.isObject()) {
            value.fieldNames().forEachRemaining(this::requireLo);return value.size();
        }
        if(name.equals("itemThresholds") && value.isObject()) {
            value.fieldNames().forEachRemaining(id -> {
                var item=items.findById(Long.valueOf(id)).orElseThrow(AccessPolicy::denied);
                requireLo(item.getLos().getId());
            });
            return value.size();
        }
        int count=0;
        if(value.isObject()) {
            var entries=value.fields();
            while(entries.hasNext()){var e=entries.next();count+=scope(e.getKey(),e.getValue());}
        } else if(value.isArray()) for(JsonNode child:value)count+=scope("",child);
        return count;
    }
}
