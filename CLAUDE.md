# LO-PO Analytics (OBQA)

Learning Outcome → Program Outcome attainment and accreditation-reporting system for the University of Ruhuna, Faculty of Engineering. Lecturers create Learning Outcomes per module and map them to Program Outcomes; admins review/approve mappings; the system computes PO attainment from assessment results and generates accreditation reports.

## Stack and layout

- **Backend**: `Software-project-Backend/` — Spring Boot 3.2.2, Java 17, Spring Data JPA/Hibernate, Spring Security (JWT), MySQL 8, Apache POI (Excel), OpenPDF (PDF). Package root `com.example.Software.project.Backend`, split into `Model`, `Repository`, `Service`, `RestController`, `Security`, `Reporting` (+ `Reporting/Progress`).
- **Frontend**: `softwareproject_frontend/` — React 18 + Vite 5, react-router-dom 7, axios, TailwindCSS, plain JS/JSX. Dev server on `:5173`, backend on `:8080`.
- **Roles**: `superadmin` (creates admins), `admin` (creates lecturers, CRUDs modules/POs, approves/rejects LO-PO mappings), `lecture` (CRUDs Learning Outcomes, uploads marks, proposes LO-PO mappings). Role lives on `User.usertype`, enforced server-side via Spring Security RBAC — this is the sole source of truth.

## The two report subsystems

Two report code paths coexist under `Reporting/`:

1. **`Reporting/Progress/*`** (`ProgressController`, `ProgressService`, `AttainmentCalculator`, etc.) — the current, canonical individual-student report system. Curriculum-versioned, retake-policy-aware, produces immutable JSON snapshots + audit trail. Documented in `Software-project-Backend/STUDENT_PROGRESS_REPORTS.md`. **Requires curriculum/LO-PO mappings/enrolment data to already be configured** for a batch before it can produce anything — it has no "just works off recorded marks" fallback.
2. **`BatchReportController`/`BatchReportService`/`BatchReportPdf`** — a separate, read-only, anonymized batch-level attainment report (no student names), documented in `Software-project-Backend/BATCH_REPORTS.md`. Distinct from Progress; keep it in mind before assuming there's one "report system."

A third, legacy per-student report system (`StudentReportController`/`Service`/`Pdf`) existed but has been removed — Progress fully superseded it and the frontend had already migrated away. Its one shared piece, the per-LO attainment calculation, was extracted to `Reporting/LoAttainmentCalculator` before deletion, since `BatchReportService` depends on it too.

## Schema management

Flyway owns the full schema (`spring.flyway.enabled=true`, Boot's native autoconfiguration, default location `Software-project-Backend/src/main/resources/db/migration`). `spring.jpa.hibernate.ddl-auto=validate` everywhere — Hibernate only checks its entity mappings against what Flyway created; it never creates or alters schema itself.

- `V1__baseline_legacy_schema.sql` creates the 13 tables that used to be auto-managed by Hibernate `ddl-auto=update` (User, Student, Module, Los, LosPos, ProgramOutcome, OutcomeMapping, AssessmentTemplate, AssessmentItem, StudentMark, StudentAssessmentScore, CqiAction, AuditLog + a `module_lecturers` join table). Captured from a live database Hibernate itself created from the entity mappings, so it matches `ddl-auto=validate` exactly. Table order matters — every FK references a table created earlier in the same file (H2, used for local/CI testing in MySQL-compatibility mode, doesn't honor `SET FOREIGN_KEY_CHECKS` the way real MySQL does, so ordering has to be correct regardless).
- `V2__student_progress.sql` creates the 14 `qa_*` reporting tables, which have FK references into V1's tables — must run after it.
- This replaced an earlier split (Hibernate `ddl-auto=update` for the 13 legacy tables + a hand-rolled `ProgressMigrations` runner manually driving a separate `Flyway` instance for the `qa_*` tables, specifically to avoid colliding with Boot's own Flyway autoconfig). `ProgressMigrations` is gone; Boot's native Flyway does everything now.
- **For a database that predates this consolidation** (has the old `progress_schema_history` table instead of Flyway's `flyway_schema_history`): set `spring.flyway.baseline-on-migrate=true` and `spring.flyway.baseline-version=2` for the first startup only — version 2, not 1, since that database already has both the legacy tables and the `qa_*` tables. Verified against a real MySQL scratch database seeded via the old regime.
- A `@DataJpaTest` that doesn't care about migrations (e.g. `BatchReportRepositoryTest`) should explicitly set `spring.flyway.enabled=false` + `spring.jpa.hibernate.ddl-auto=create-drop` in its own test properties — otherwise it inherits the real Flyway migrations, which are MySQL-flavored (`ENGINE=InnoDB` etc.) and won't parse against a plain (non-`MODE=MySQL`) embedded H2.

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
