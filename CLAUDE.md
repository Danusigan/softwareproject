# LO-PO Analytics (OBQA)

Learning Outcome → Program Outcome attainment and accreditation-reporting system for the University of Ruhuna, Faculty of Engineering. Lecturers create Learning Outcomes per module and map them to Program Outcomes; admins review/approve mappings; the system computes PO attainment from assessment results and generates accreditation reports.

## Stack and layout

- **Backend**: `Software-project-Backend/` — Spring Boot 3.2.2, Java 17, Spring Data JPA/Hibernate, Spring Security (JWT), MySQL 8, Apache POI (Excel), OpenPDF (PDF). Package root `com.example.Software.project.Backend`, split into `Model`, `Repository`, `Service`, `RestController`, `Security`, `Reporting` (+ `Reporting/Progress`).
- **Frontend**: `softwareproject_frontend/` — React 18 + Vite 5, react-router-dom 7, axios, TailwindCSS, plain JS/JSX. Dev server on `:5173`, backend on `:8080`.
- **Roles**: `superadmin` (creates admins), `admin` (creates lecturers, CRUDs modules/POs, approves/rejects LO-PO mappings), `lecture` (CRUDs Learning Outcomes, uploads marks, proposes LO-PO mappings). Role lives on `User.usertype`, enforced server-side via Spring Security RBAC — this is the sole source of truth.

## The three report subsystems

Three report code paths coexist under `Reporting/`:

1. **`Reporting/Progress/*`** (`ProgressController`, `ProgressService`, `AttainmentCalculator`, etc.) — the current, canonical individual-student report system. Curriculum-versioned, retake-policy-aware, produces immutable JSON snapshots + audit trail. Documented in `Software-project-Backend/STUDENT_PROGRESS_REPORTS.md`. **Requires curriculum/LO-PO mappings/enrolment data to already be configured** for a batch before it can produce anything — it has no "just works off recorded marks" fallback.
2. **`StudentReportController`/`StudentReportService`/`StudentReportPdf`** — the legacy per-student report. The frontend no longer calls it (fully migrated to Progress), but **`BatchReportService` calls `StudentReportService.calculate()` directly** — so the service class is still a live dependency and can't be deleted without first extracting that method. The controller/PDF/routes are otherwise dead.
3. **`BatchReportController`/`BatchReportService`/`BatchReportPdf`** — a separate, read-only, anonymized batch-level attainment report (no student names), documented in `Software-project-Backend/BATCH_REPORTS.md`. Distinct from both of the above; keep it in mind before assuming there's one "report system."

## Schema management (a known split, not yet consolidated)

- `spring.jpa.hibernate.ddl-auto=update` auto-manages the ~13 original JPA-entity tables (User, Student, Module, Los, LosPos, ProgramOutcome, OutcomeMapping, AssessmentTemplate, AssessmentItem, StudentMark, StudentAssessmentScore, CqiAction, AuditLog).
- `spring.flyway.enabled=false` is **deliberately** set in `application.properties` — a hand-rolled `Reporting/Progress/ProgressMigrations` class builds its own `Flyway` instance (`classpath:db/progress`, table `progress_schema_history`) and runs it manually after Hibernate bootstraps, to avoid colliding with Boot's own Flyway autoconfig. Only one migration exists there: `V1__student_progress.sql`, creating the 14 `qa_*` tables — which have FK references into the legacy Hibernate-managed tables.
- **Do not flip `spring.flyway.enabled=true` without also consolidating**: Boot's autoconfig would try to run Flyway independently of `ProgressMigrations`, double-running migrations. A full consolidation plan (baseline-dump the 13 legacy tables into `V1`, renumber `qa_*` to `V2`, retire `ProgressMigrations`, switch `ddl-auto=validate`) exists but hasn't been executed yet.

## Datasource names — two different DBs in play

- `application.properties` (checked in): db name `obqa`, credentials from `${DB_USERNAME}`/`${DB_PASSWORD}` env vars, no insecure fallback.
- `application-dev.properties` (gitignored, per-developer): historically points at a different local db name (`LOPOmapping`) — don't assume the two are interchangeable when debugging schema issues.
- `docker-compose.yml` provisions a fresh db named `softwareproject`. Three different names have shown up across environments — check which one you're actually pointed at before assuming schema state.

## Security posture

RBAC, BCrypt password hashing, secrets-to-env-vars, login lockout, file-upload validation, a global exception handler, audit logging, and CORS fixes all landed via `security/Security_plan.md` (a phase-by-phase security hardening program — see that file and the other `security/0*.md` docs for full detail and current status). Check there before assuming something is still an open security gap.

## Test fixtures referenced in older material

Some now-deleted setup guides referenced a fixed test fixture: lecturer login `danu1`/`danu2` password `1234`, module `EC6306`. If you need a known-good manual test path and don't have your own seed data, that combination shows up in git history for the deleted `POSTMAN_TESTING_GUIDE.md`/`API_QUICK_REFERENCE.md`.

## Excel export / template endpoints

Several one-off feature-announcement docs for Excel import/export and mark-upload templates were consolidated away (they described features, not living APIs). The current endpoints live in `OBEController`: `/api/obe/marks/upload*`, `/api/obe/export/marks`, `/api/obe/template/marks`, `/api/obe/template/marks-question-wise`, `/api/obe/marks/export/module/{moduleId}`, `/api/obe/export/po-attainment`, `/api/obe/export/marks-per-lo-threshold` — read the controller directly rather than looking for a guide doc.
