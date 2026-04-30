# Project Init

## Purpose

This note records the current implemented logic of the Outcome Based Quality Assurance system and the international standards gap analysis, so it can be used as a project reference.

---

## Backend Logic Implemented So Far

### Core Data Model

- `Student` stores student identity and profile data.
- `Module` stores department modules.
- `Los` stores Learning Outcomes and links each LO to a module.
- `StudentMark` stores marks per student, LO, batch, and mark type.
- `MarkType` enum supports `FINAL_EXAM` and `ASSIGNMENT`.
- `ProgramOutcome` stores the 12 Programme Outcomes (aligned with Washington Accord Graduate Attributes).
- `OutcomeMapping` stores LO → PO mappings with weight (0–3), approval status, and audit fields.
- `User` stores system users with roles: `superadmin`, `admin`, `lecture`.

### Main Backend Features

1. **Learning Outcome management**
   - Create, edit, delete, and fetch LOs.
   - Fetch all LOs for a module.
   - Fetch LO batches and LO mapping data.

2. **PO / LO mapping management**
   - Save outcome mappings with correlation weight (0 = none, 1 = low, 2 = medium, 3 = high).
   - Approve/reject mappings as admin.
   - Show mapping status and feedback.

3. **Student mark management**
   - Upload single-LO marks from Excel.
   - Upload bulk workbook marks for multiple LOs.
   - Store batch and mark type with each mark.

4. **Excel export and template workflow**
   - Generate export report with pass/fail result.
   - Generate blank Excel template for manual fill.
   - Use threshold-based pass/fail logic.
   - Apply Excel formatting for headers and status cells.

5. **PO Attainment calculation**
   - `POAttainmentService.calculateStudentPOCredits()` — per-student PO credit calculation.
   - If student passes an LO → add 100% of each mapped PO weight.
   - Export PO attainment as Excel.

6. **Trend and analysis**
   - `TrendService` — course-level and LO-level trends across batches.
   - `AttainmentService.getPOAttainment(moduleId)` — flat map of PO scores for charts.

### Export and Template Workflow in Backend

- Export endpoint: `POST /api/obe/export/marks`
- Template endpoint: `POST /api/obe/template/marks`
- Bulk upload endpoint: `POST /api/obe/marks/upload-bulk`
- PO attainment endpoint: `POST /api/obe/po-attainment`
- PO attainment export: `POST /api/obe/export/po-attainment`

The export flow:
- Reads selected LO IDs, mark type, batch, and threshold.
- Loads stored marks for those filters.
- Builds a workbook with student index in column 1.
- Writes pass/fail status for each LO using `score >= threshold`.
- Returns the workbook as a downloadable `.xlsx` file.

The template flow:
- Reads selected LO IDs, mark type, and batch.
- Generates an empty workbook with LO headers.
- Includes rows for manual Excel entry.
- Returns the workbook as a downloadable `.xlsx` file.

The bulk upload flow:
- Accepts the completed Excel workbook.
- Format: `Student Index | LO1 | LO2 | LO3 | ...`
- Parses and stores marks in the database.

### Backend Validation and Security

- JWT authorization is required on all endpoints.
- Lecturer role is required for export, template, and upload operations.
- Admin and superadmin are supported where applicable.
- Input validation checks losIds, markType, batch, and threshold.
- CORS configured for `http://localhost:5173`.

---

## Frontend Logic Implemented So Far

### Application Routing

| Route | Page |
|-------|------|
| `/` | Landing page |
| `/loginpage` | Login page |
| `/forgottenpassword` | Password reset |
| `/admin-dashboard` | Admin dashboard |
| `/super-admin-dashboard` | Super admin dashboard |
| `/lecturer-dashboard` | Lecturer dashboard |
| `/modules` | Module list |
| `/lo-detail/:loId` | LO detail page |
| `/lo-detail/:loId/add-results` | Single-LO upload (legacy) |
| `/lo-detail/:loId/comparisons` | Comparison page |
| `/marks-workbench/:moduleId` | Bulk Excel workflow page |
| `/program-outcomes` | Programme outcomes management |
| `/create-lo-mapping/:moduleId` | LO mapping creation |
| `/lo-po-mappings` | LO-PO mapping management |

### Marks Workbench Page (Main Workflow)

The bulk workflow page at `/marks-workbench/:moduleId` is the primary UI.

It allows the lecturer to:
- Load a module and its LOs.
- Select one or more LOs.
- Select batch, mark type, threshold.
- Download Excel template.
- Upload completed workbook.
- Export pass/fail report.
- Calculate and export PO attainment (per-student credit view).

### Frontend Services

`marksService.js` wrappers:
- `getModule(moduleId)`
- `getModuleLos(moduleId)`
- `downloadTemplate({ losIds, markType, batch })`
- `exportMarks({ losIds, markType, batch, threshold })`
- `uploadBulk({ excelFile, losIds, batch, markType })`
- `getPOAttainment({ losIds, markType, batch, threshold })`
- `exportPOAttainment({ losIds, markType, batch, threshold })`

### Frontend Support

- `ProtectedRoute` guards role-based access.
- Axios interceptors handle token attachment and auth expiry.
- Header and footer components provide consistent layout.

---

## Current End-to-End Workflow

1. Lecturer logs in.
2. Lecturer opens the lecturer dashboard.
3. Lecturer selects a module.
4. Lecturer opens the bulk workflow page.
5. Lecturer selects LOs, batch, mark type, and threshold.
6. Lecturer downloads the Excel template.
7. Lecturer fills the template in Excel.
8. Lecturer uploads the completed workbook.
9. Lecturer exports the pass/fail report or the PO attainment report.

---

## Files That Anchor the Current Implementation

**Backend:**
- `Software-project-Backend/src/main/java/com/example/Software/project/Backend/RestController/OBEController.java`
- `Software-project-Backend/src/main/java/com/example/Software/project/Backend/Service/ExcelExportService.java`
- `Software-project-Backend/src/main/java/com/example/Software/project/Backend/Service/ExcelImportService.java`
- `Software-project-Backend/src/main/java/com/example/Software/project/Backend/Service/POAttainmentService.java`
- `Software-project-Backend/src/main/java/com/example/Software/project/Backend/Repository/StudentMarkRepository.java`

**Frontend:**
- `softwareproject_frontend/src/App.jsx`
- `softwareproject_frontend/src/pages/lecturerdashboard.jsx`
- `softwareproject_frontend/src/pages/LODetailPage.jsx`
- `softwareproject_frontend/src/pages/MarksWorkbenchPage.jsx`
- `softwareproject_frontend/src/services/marksService.js`

---

## New Entities Added (OBA Upgrade — In Progress)

These entities were created as part of the Gap 1 upgrade to bring the system to international OBA standard:

### `AssessmentTemplate`
Groups a set of assessment items (questions) for a specific module, batch, mark type, and semester.
- Fields: `id`, `name`, `module`, `batch`, `markType`, `semester`, `academicYear`, `createdBy`, `createdAt`, `updatedAt`, `items`
- Table: `assessment_template`

### `AssessmentItem`
Represents one question (Q1, Q2, Q3…) inside an AssessmentTemplate, mapped to exactly one LO.
- Fields: `id`, `questionLabel`, `questionNumber`, `maxMarks`, `los`, `assessmentTemplate`
- Table: `assessment_item`

### `StudentAssessmentScore`
Stores a student's raw score for a single question (AssessmentItem).
- Fields: `id`, `student`, `assessmentItem`, `score`
- Table: `student_assessment_score`
- Unique constraint: one score per student per item.

### `CqiAction`
Records a Continuous Quality Improvement action raised when an LO or PO misses its attainment target.
- Fields: `id`, `module`, `los` (nullable), `programOutcome` (nullable), `batch`, `semester`, `academicYear`, `actionDescription`, `reason`, `responsibleStaff`, `status` (PLANNED / IN_PROGRESS / COMPLETED), `createdBy`, `createdAt`, `updatedAt`
- Table: `cqi_action`

### `CqiStatus` (enum)
`PLANNED` | `IN_PROGRESS` | `COMPLETED`

---

## New Services Added (OBA Upgrade — In Progress)

### `AssessmentService`
- `createTemplate(req, createdBy)` — Create a template with questions and Q→LO mapping.
- `getTemplatesForModule(moduleId)` — List templates for a module.
- `deleteTemplate(templateId)` — Cascade delete scores and items.
- `generateQuestionTemplate(templateId)` — Generate Excel: `Student Index | Q1 | Q2 | Q3 | …`
- `importQuestionScores(templateId, file)` — Parse uploaded Excel, store per-question scores.

### `ObaAttainmentService`
- `calculateAttainment(templateId, threshold, targetPct)` — Full attainment report:
  - LO Attainment % = (students scoring ≥ threshold% of LO max marks / total students) × 100
  - PO Attainment = weighted average of LO attainments using CO-PO mapping strength
  - Student-level breakdown per question
  - Gap flags: marks LOs/POs that miss the attainment target
- `getLoAttainmentSummary(moduleId, threshold, targetPct)` — Summary across all templates for a module.

### `CqiService`
- `createAction(req, createdBy)` — Create a CQI action manually.
- `updateAction(id, req)` — Update description, status, responsible staff, etc.
- `deleteAction(id)` — Delete an action.
- `getActionsForModule(moduleId)` — Fetch all CQI actions for a module.
- `autoFlagGaps(attainmentResult, moduleId, createdBy)` — Auto-create PLANNED actions for every LO and PO that missed the attainment target.

---

## New Repositories Added (OBA Upgrade — In Progress)

- `AssessmentTemplateRepository`
- `AssessmentItemRepository`
- `StudentAssessmentScoreRepository`
- `CqiActionRepository`

---

## Planned But Not Yet Implemented (OBA Upgrade)

The following were planned in the session but not yet created due to interruption:

1. `AssessmentController.java` — REST endpoints for all new OBA features.
2. `assessmentService.js` — Frontend API wrappers for new endpoints.
3. `AssessmentSetupPage.jsx` — UI to create a template and define Q→LO mappings.
4. `AttainmentReportPage.jsx` — UI to view LO%, PO% attainment and student breakdown.
5. `CqiActionsPage.jsx` — UI to manage CQI improvement actions.
6. Updates to `App.jsx` — Add 3 new routes.
7. Updates to `lecturerdashboard.jsx` — Navigation buttons to new pages.

---

---

# International Standards Analysis

## Reference Frameworks

| Framework | Body | Scope |
|-----------|------|-------|
| ABET EC2000 | Accreditation Board for Engineering and Technology (USA) | Engineering programs, 42 countries |
| Washington Accord | International Engineering Alliance | 25 signatory nations, mutual recognition |
| NBA | National Board of Accreditation (India) | Engineering and technical programs |
| MQF / MQF 2.0 | Malaysian Qualifications Agency | All higher education levels, 8 qualification levels |
| AACCUP OBQA | Philippines accreditation body | University-wide quality assurance |

All five frameworks converge on three core pillars:
- **OBE** — Outcomes-Based Education (define, teach, assess)
- **OBA** — Outcomes-Based Assessment (measure attainment)
- **CQI** — Continuous Quality Improvement (act on results, close the loop)

---

## Gap 1: Outcome Hierarchy Is Incomplete ✅ PARTIALLY ADDRESSED

### What international standards require

```
Institution Vision / Mission
        ↓
Programme Educational Objectives (PEOs)
        ↓
Programme Outcomes / PLOs
  (Washington Accord: 12 Graduate Attributes)
        ↓
Course Learning Outcomes / LOs
        ↓
Assessment Items (Question level)
```

### Current system status

- LOs linked to modules. ✓
- POs defined (12, aligned with Washington Accord). ✓
- CO-PO mapping with weight (0–3). ✓
- Assessment Items (Q1, Q2…) → new entities created. ✓ (backend only)
- PEOs not implemented (out of scope for now). —
- Vision/Mission not in system. —

### What was added in this session

- `AssessmentTemplate`, `AssessmentItem`, `StudentAssessmentScore` entities.
- Question-level Excel template generation and import.
- Q → LO mapping stored per assessment item.

---

## Gap 2: CO-PO Mapping Has No Strength Weight ✅ ALREADY DONE

The `OutcomeMapping` entity already has a `weight` field (0 = none, 1 = low, 2 = medium, 3 = high).
The `ObaAttainmentService` uses this weight when computing PO attainment as a weighted average.

---

## Gap 3: Attainment Calculation Is Pass/Fail Per Student, Not Programme-Level ✅ ADDRESSED

### What was added

- `ObaAttainmentService.calculateAttainment()` computes:
  - **LO Attainment %** = (students achieving ≥ threshold% of LO max marks / total students) × 100
  - **PO Attainment %** = weighted average of LO attainments using CO-PO strength weights
  - Per-student score breakdown per question
  - Gap flag: whether each LO/PO meets the configurable target %

### Standard formula implemented

```
LO Attainment % = (students scoring >= threshold% of max / total students) × 100

PO Attainment = Σ(LO_Att_i × Weight_i) / Σ(Weight_i)
  where Weight_i is the CO-PO mapping strength for each LO mapped to this PO.
```

---

## Gap 4: Assessment Types Are Too Limited — PENDING

Current system: `FINAL_EXAM` | `ASSIGNMENT` only.

### Still needed

- Expand `MarkType` enum: add `MIDTERM`, `QUIZ`, `LAB`, `PROJECT`, `PRESENTATION`, `VIVA`.
- Add weightage per assessment component per module.
- Compute weighted LO score from component scores before threshold.

---

## Gap 5: No Bloom's Taxonomy Tagging on LOs — PENDING

### Still needed

- Add `bloomLevel` field (integer 1–6) to `Los` entity.
- Add Bloom level selector in LO creation/edit UI.
- Show Bloom distribution chart on module overview.

| Level | Label | Key Verbs |
|-------|-------|-----------|
| 1 | Remember | Define, List, Recall |
| 2 | Understand | Explain, Summarise, Classify |
| 3 | Apply | Solve, Use, Demonstrate |
| 4 | Analyze | Compare, Differentiate, Examine |
| 5 | Evaluate | Justify, Critique, Assess |
| 6 | Create | Design, Construct, Formulate |

---

## Gap 6: No Rubric-Based Assessment — PENDING

### Still needed

- `Rubric` entity with criteria rows and level descriptors.
- Link rubrics to LOs and assessment components.
- Rubric scoring interface for lecturers.

---

## Gap 7: No CQI Workflow ✅ PARTIALLY ADDRESSED

### What was added

- `CqiAction` entity and `CqiStatus` enum (PLANNED / IN_PROGRESS / COMPLETED).
- `CqiService` with CRUD + `autoFlagGaps()` — auto-creates PLANNED actions for every LO/PO that missed its attainment target.
- Frontend CQI page still pending.

### Still needed

- `CqiActionsPage.jsx` (frontend).
- CQI dashboard showing: gaps found → plans recorded → implemented → resolved.

---

## Gap 8: No Indirect Assessment — PENDING

### Still needed

- Student satisfaction survey module.
- Exit survey and alumni survey module.
- Employer feedback module.
- Indirect attainment blending into PO attainment formula (typical 80% direct / 20% indirect).

---

## Gap 9: No Accreditation Report Generation — PENDING

### Still needed

- CO-PO matrix report with strength and attainment (Excel/PDF).
- LO attainment summary report per module per batch.
- PO attainment trend report across batches.
- CQI evidence summary report.
- NBA SAR / ABET Self-Study data export.

---

## Gap 10: No Analytics Dashboard — PENDING

### Still needed

- LO attainment bar charts per module.
- PO attainment trend line per programme.
- CO-PO heat map with strength + attainment overlay.
- Bloom's Taxonomy distribution chart.
- CQI funnel chart (gaps → plans → resolved).

---

## Gap 11: No Audit Log — PENDING

### Still needed

- `AuditLog` entity: userId, action type, entity type, entity id, timestamp, old/new value.
- Log all create/update/delete on LOs, mappings, marks, templates, CQI actions.
- Admin-only audit log viewer page.

---

## Gap 12: No Multi-Year Cycle Management — PENDING

### Still needed

- `AssessmentCycle` entity (programme, academic year, semester).
- Attach marks, attainment reports, and CQI actions to a cycle.
- Soft-delete / archive flag so historical cycles are never hard-deleted.
- ABET requires 6 years of CQI data at audit time.

---

## Implementation Priority Order

### Phase 1 — Foundation (Partially Done This Session)

| # | Item | Status |
|---|------|--------|
| 1 | CO-PO mapping strength (1/2/3) | ✅ Already in OutcomeMapping.weight |
| 2 | Assessment Items (Q→LO mapping) | ✅ Backend entities + services created |
| 3 | LO Attainment % calculation | ✅ ObaAttainmentService created |
| 4 | PO Attainment % via weighted average | ✅ ObaAttainmentService created |
| 5 | CQI action recording + auto-flagging | ✅ CqiService + CqiAction created |
| 6 | AssessmentController REST endpoints | 🔲 Pending |
| 7 | Frontend: AssessmentSetupPage | 🔲 Pending |
| 8 | Frontend: AttainmentReportPage | 🔲 Pending |
| 9 | Frontend: CqiActionsPage | 🔲 Pending |
| 10 | App.jsx route updates | 🔲 Pending |

### Phase 2 — Reporting

| # | Item | Status |
|---|------|--------|
| 11 | Bloom's Taxonomy level on LOs | 🔲 Pending |
| 12 | Expand MarkType enum | 🔲 Pending |
| 13 | Assessment component weightage | 🔲 Pending |
| 14 | CO-PO attainment Excel/PDF report | 🔲 Pending |
| 15 | LO/PO attainment summary report | 🔲 Pending |

### Phase 3 — CQI Loop

| # | Item | Status |
|---|------|--------|
| 16 | CQI dashboard (gap→plan→resolved funnel) | 🔲 Pending |
| 17 | Multi-batch CQI trend tracking | 🔲 Pending |
| 18 | AssessmentCycle entity | 🔲 Pending |

### Phase 4 — Analytics Dashboard

| # | Item | Status |
|---|------|--------|
| 19 | LO attainment bar charts | 🔲 Pending |
| 20 | PO attainment trend lines | 🔲 Pending |
| 21 | CO-PO heat map | 🔲 Pending |
| 22 | Bloom's distribution chart | 🔲 Pending |

### Phase 5 — Indirect Assessment

| # | Item | Status |
|---|------|--------|
| 23 | Student survey module | 🔲 Pending |
| 24 | Alumni/employer feedback | 🔲 Pending |
| 25 | Indirect attainment blending | 🔲 Pending |

### Phase 6 — Accreditation Reports

| # | Item | Status |
|---|------|--------|
| 26 | NBA SAR report generator | 🔲 Pending |
| 27 | ABET Self-Study export | 🔲 Pending |
| 28 | Washington Accord GA report | 🔲 Pending |

---

## Reference: Washington Accord 12 Graduate Attributes

1. Engineering Knowledge
2. Problem Analysis
3. Design and Development of Solutions
4. Investigations
5. Modern Tool Usage
6. The Engineer in Society
7. Environment and Sustainability
8. Ethics
9. Individual and Team Work
10. Communication
11. Project Management and Finance
12. Life-long Learning

---

## Reference: Standard CO-PO Attainment Formula

```
Direct LO Attainment (%) =
  (students scoring >= threshold% of LO max marks / total students) × 100

PO Attainment =
  Σ (LO_Attainment_i × Strength_i) / Σ Strength_i
  where Strength_i = CO-PO mapping weight (1, 2, or 3)

Final PO Attainment =
  (Direct × 0.80) + (Indirect × 0.20)   [when indirect assessment is added]

Programme is compliant if all POs reach attainment target (typically 60–70%).
```
