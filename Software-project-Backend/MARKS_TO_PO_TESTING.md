# Marks → LO → CQI → PO → Reports: how it works and how it is tested

Written 2026-09-24 after the pipeline test work. Everything under "Test results" was run for real
on that date; everything under "Not covered" is what the tests do **not** prove.

## 1. How the flow works

```
Excel upload ─► StudentMark rows + AssessmentTemplate/Item (max marks)
                    │
                    ├─► LO % per student ──► CQI (batch level)
                    │
                    └─► LO pass/fail per student ──► PO credits (student level) ──► Reports
```

### 1.1 Marks upload — `ExcelImportService`
- Bulk sheet: `Student Index | LO1 (max=50) | LO2 (max=20) …`. Max marks come from the header
  (`max=`) or an explicit per-LO map; no header max means 100.
- Whole file is rejected (nothing saved) if: an LO id is unknown, a **student is not already in
  the database**, or any score is negative or above the LO's max.
- `AB`, `MC`, `N/A` and blank cells are **not attended**: no mark is stored, they are not zero.
- Re-uploading the same assignment label replaces its marks and its template. An upload with no
  label replaces every earlier mark for that LO and batch, so upload the final exam first.
- `OBEController` triggers `POAttainmentService.recalculateForModule` after each upload, edit
  and delete (threshold 50). The service itself does not.

### 1.2 A student's LO % — `POAttainmentService.loPercentageByStudent`
- Marks are grouped per assessment (mark type + assignment label). A student is scored only
  against the assessments they **have marks for**: `sum(their marks) / sum(those assessments' max)`.
- Not attended is therefore left out, **not counted as zero**. A makeup assignment uploaded later
  simply adds to the student's result.
- A student with no marks for the LO is not in that LO's results at all.
- If no max marks are known, the raw total is treated as an already-computed percentage (legacy).

### 1.3 CQI — `CQIService`
Two separate values, sent by the Marks Workbench page to `POST /api/cqi/finalize/{moduleId}`:
- **studentPassThreshold** — a student passes an LO at or above this % of its marks.
- **batchTarget** — a CQI action is raised for an LO when the % of the batch that passed is
  **below** this. Exactly on the target does not trigger CQI.

Either may be omitted; the LO's stored threshold (default 50) is then used for it. Values must be
0–100. Rules: one open action (PLANNED / IN_PROGRESS) per LO at a time; an approved plan is
IN_PROGRESS; the next batch's result is linked to it and closes it (COMPLETED) once the target is
met. CQI **never changes PO credits**.

### 1.4 PO credits — `POAttainmentService.calculateStudentPOCredits`
- For every LO a student passes (LO % ≥ threshold), the student earns the full weight of each
  mapped LO→PO mapping. Fail or no marks earns nothing.
- Maximum credits per PO = sum of the weights of all its mappings.
- Results are saved per module and batch in `student_po_credit`, overwriting the previous save.
- `getStudentPOSummary` adds a student's saved credits across modules and batches, and each
  module/batch entry carries a `cqiActions` list (`losId`, `status`, `nextSemAttainment`) as a
  flag only.

### 1.5 Reports
| Report | Endpoint | Built from |
|---|---|---|
| PO reports (admin only, names students) | `/api/reports/po/student`, `/student/batch`, `/batch` | saved `student_po_credit` rows |
| Batch attainment (anonymous) | `/api/reports/batches/modules`, `/attainment` | LO evidence, `LoAttainmentCalculator` |
| Student progress (curriculum-versioned, PDF, snapshots) | `/api/reports/progress/...` | its own configured curriculum, offerings and enrolments |

The PO report marks a PO Attained / Not attained / No evidence from the student's credit
percentage against `studentThreshold` (default 40).

## 2. Worked fixture used by the pipeline test
One module `EC5001`, batch `24`, four students. Header max marks: LO1 = 50, LO2 = 20.

| | LO1 /50 | LO2 /20 |
|---|---|---|
| S1 | 40 (80%) | 15 (75%) |
| S2 | 30 (60%) | 5 (25%) |
| S3 | 20 (40%) | 18 (90%) |
| S4 | 10 (20%) | 4 (20%) |

LO thresholds 50 / 60. Approved mappings: LO1→PO1 weight 3, LO2→PO1 weight 2, LO2→PO2 weight 1
(PO1 max 5, PO2 max 1). Expected values are worked out by hand, not recomputed by the code:
- Batch attainment: LO1 = 2 of 4 = 50%, LO2 = 2 of 4 = 50%.
- CQI: LO1 (50 vs 50) no action; LO2 (50 vs 60) one PLANNED action.
- PO credits (PO1, PO2): S1 5,1 · S2 3,0 · S3 2,1 · S4 0,0.

## 3. How the tests work
`MarksToPoPipelineTest` (`src/test/.../Service/`) is a `@DataJpaTest` on H2 that imports the
**real** `ExcelImportService`, `POAttainmentService`, `CQIService` and `AttainmentService`.
Nothing is mocked. It builds a real `.xlsx` in memory, uploads it, then calls the same steps the
controllers call, in the same order (`recalculateForModule`, then `finalizeModuleAttainment`),
and asserts through the public service methods only. Hibernate builds the H2 schema because the
Flyway scripts are MySQL-flavoured. The `user` table cannot be created on H2 (reserved word),
which logs a harmless DDL warning; nothing in these tests needs it.

| Pipeline test | What it proves |
|---|---|
| `uploadedMarksTriggerCqiOnlyForTheLoBelowItsThreshold` | upload → LO % → only LO2 raises a CQI action (50% vs 60%), LO1 exactly on target does not |
| `cqiUsesSeparateStudentPassThresholdAndBatchTarget` | pass mark 70 / target 20 → no action; pass 60 / target 60 → two actions recording target 60 |
| `uploadedMarksAccumulateIntoEachStudentsPoCredits` | each student's PO1/PO2 credits match the table above |
| `completingACqiCycleLeavesPoCreditsUntouched` | plan approved, next batch meets target → COMPLETED with next-batch 100%, PO credits unchanged |
| `poSummaryFlagsTheCqiCycleForTheContributingBatch` | PO summary flags PLANNED then COMPLETED, credits unchanged |
| `missedAssessmentIsNotAttendedRatherThanZeroForBothCqiAndPo` | absent student scored on what they sat (60/80 = 75%) by CQI and PO alike; a later makeup upload changes the result |

Other tests in the chain (all existing, mock-based unless noted): `ExcelImportServiceTest`
(parsing and validation, incl. unknown-student rejection), `LoAttainmentCalculatorTest`,
`CQIServiceTest`, `POAttainmentServiceTest`, `AttainmentServiceTest`, `PoReportServiceTest`,
`BatchReportTest` / `BatchReportRepositoryTest`, `AttainmentCalculatorTest` and
`ProgressIntegrationTest` (Progress reports on H2, incl. API and PDF), `OBEControllerTest`.

Some tests began green because the behaviour already existed; those are pinned to hand-computed
numbers. Where behaviour changed, the test was written first and seen to fail (CQI flag on the PO
summary, separate thresholds, the missed-assessment rule), then the code was changed.

## 4. Test results (2026-09-24)

Backend, `./mvnw test`: **175 run, 174 passed, 1 error.**

| Test class | Tests | Result |
|---|---|---|
| Service.MarksToPoPipelineTest | 6 | pass |
| Service.POAttainmentServiceTest | 16 | pass |
| Service.CQIServiceTest | 9 | pass |
| Service.AttainmentServiceTest | 7 | pass |
| Service.ExcelImportServiceTest | 10 | pass |
| Service.ExcelExportServiceTest | 6 | pass |
| Service.LOPOMappingServiceTest | 14 | pass |
| Service.StudentServiceTest | 8 | pass |
| Service.TrendServiceTest | 3 | pass |
| Service.CustomUserDetailsServiceTest | 3 | pass |
| Reporting.PoReportServiceTest | 12 | pass |
| Reporting.BatchReportTest | 11 | pass |
| Reporting.BatchReportRepositoryTest | 1 | pass |
| Reporting.LoAttainmentCalculatorTest | 7 | pass |
| Reporting.Progress.AttainmentCalculatorTest | 17 | pass |
| Reporting.Progress.ProgressIntegrationTest | 13 | pass |
| Repository.DeleteOrderingRepositoryTest | 2 | pass |
| RestController.OBEControllerTest | 10 | pass |
| RestController.LOPOMappingRestControllerTest | 7 | pass |
| RestController.UserRestControllerTest | 10 | pass |
| Model.OutcomeMappingTest | 2 | pass |
| SoftwareProjectBackendApplicationTests | 1 | **error** |

The one error is `SoftwareProjectBackendApplicationTests`: it starts the whole app against a
real MySQL and fails with `Access denied for user 'root'@'localhost'` (needs `DB_USERNAME` and
`DB_PASSWORD` for a reachable database). It is an environment failure, not a code failure, but
it also means **Flyway and the real schema were not exercised in this run.**

Frontend, `npx vitest run`: **24 passed in 5 files.** `vite build` succeeds.

Run one class: `./mvnw -q -o test -Dtest=MarksToPoPipelineTest` from `Software-project-Backend`.

## 5. Will it all work without breaking? — honest answer
It cannot be guaranteed. What the tests do show is that every stage, and the stages joined
together on H2 with hand-checked numbers, behave as described above. What they do not show:

1. **Real MySQL and Flyway** were not run (the app-context test needs credentials). H2 is built by
   Hibernate, so a MySQL-only schema problem would not be caught.
2. **Controller wiring is not in the pipeline test.** It calls `recalculateForModule` and
   `finalizeModuleAttainment` itself; `OBEControllerTest` and `CqiActionController` were not
   tested together with them. The new `studentPassThreshold` / `batchTarget` request parameters and
   their 0–100 check have no controller-level test.
3. **Report generation is not in the pipeline test.** `PoReportService`, the batch report and the
   Progress reports have their own passing tests, but none is fed by an Excel upload. The Progress
   reports also need curriculum, mappings and enrolments configured beforehand and do not use the
   CQI or PO-credit calculations above.
4. **Frontend:** the Marks Workbench page changes (Student pass mark and Batch target inputs, PO
   panel removed) build and the existing tests pass, but no test covers the new inputs, and it was
   not opened in a browser. The two values are saved per batch in the browser's localStorage only,
   so they can differ between lecturers and devices.
5. **Known behaviours you may not expect:**
   - PO credits use **all** mappings of an LO, including PENDING and REJECTED ones (the code
     comment says this is deliberate); `AttainmentService.getPOAttainment` uses approved only.
   - The automatic recalculation after each upload uses threshold 50, not the workbench's
     student pass mark. A manual `calculateStudentPOCredits` call can use another threshold, and
     the next upload overwrites it.
   - A PO summary adds credits across every batch and module saved for the student.
   - Students with no marks at all for an LO are invisible to that LO's CQI count.
6. **Other uncommitted work** (the `ExcelImportService` unknown-student rule) is included in these
   results; nothing here is committed yet.

To raise confidence further: run the app-context test against a real MySQL, add a controller-level
test for the upload → recalculation → CQI calls, and add a test that feeds the pipeline's PO credits
into `PoReportService`.
