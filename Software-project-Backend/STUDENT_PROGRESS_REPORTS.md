# Student academic progress and Programme Outcome reports

Open `/student-reports` in the React application. This page replaces the former batch-scoped individual report UI. It searches by student index or name, includes students without marks, and produces an immutable academic-history snapshot with a matching PDF. Batch reports and their existing calculations remain available. The legacy `/api/reports/students` analysis endpoints are retained for compatibility; they are not used by this page and must not be treated as graduation reports.

## Architecture and changed files

The project remains React 18 / Vite, Spring Boot 3.2.2 / Java 17+, JPA/Hibernate, MySQL and OpenPDF. No new frontend framework, UI library, state manager or ORM was added.

New backend files are in `src/main/java/com/example/Software/project/Backend/Reporting/Progress/`:

- `AttainmentCalculator`: pure decimal LO/PO calculation and attempt selection, independent of the database and UI.
- `ProgressConfiguration`: validated administrator-only publication of curriculum/policy versions, student-programme assignments, offerings and attempts.
- `ProgressStore`: parameterized native queries through the existing JPA `EntityManager`; transactions remain Spring/JPA-managed. Native SQL is used for the migration-owned reporting tables so Hibernate does not create or alter them.
- `ProgressAccess`: current authenticated roles, explicit student-account ownership and complete-history lecturer access checks. Search uses bounded, database-scoped queries rather than one query per student.
- `ProgressService` / `ProgressReport`: bulk evidence loading, academic and outcome decisions, immutable JSON snapshots and audit records.
- `ProgressController`: report/configuration APIs. The response contains overview, chronological attempts, LO results, PO results and detailed evidence together, avoiding additional evidence requests.
- `ProgressPdf`: formats the saved backend result; it does not recalculate attainment.
- `ProgressMigrations`: runs the versioned reporting migrations after the existing Hibernate schema bootstrap.

Frontend changes: `src/pages/StudentReportsPage.jsx`, its tests and CSS, plus `src/components/ProgressConfigurationPanel.jsx`. Existing authentication, header, footer, routing and Axios conventions are reused. The QA administration panel provides validated configuration templates and an existing-code catalogue. Normal report users do not edit thresholds.

`StudentAssessmentScore` now validates non-null marks before persistence: finite, nonnegative, and no greater than the item's positive maximum. Existing imports remain the source of question marks. Reporting also checks evidence independently, including pre-existing invalid records.

## Migration and historical data

Flyway 10.10.0 is added for version-controlled migrations. `src/main/resources/db/progress/V1__student_progress.sql` creates:

- `qa_programme`, `qa_curriculum`, `qa_curriculum_module`
- `qa_curriculum_lo`, `qa_curriculum_po`, `qa_curriculum_mapping`
- `qa_student_programme`, `qa_academic_period`, `qa_module_offering`
- `qa_offering_assessment`, `qa_offering_item`, `qa_module_enrolment`
- `qa_report_snapshot`, `qa_report_audit`

These tables reference existing students, modules, LOs, POs, templates and assessment items; they do not duplicate marks or recreate the original catalogue. Versioned LO/PO definitions are intentional historical revisions. Credit/threshold/weight columns use DECIMAL; keys, unique constraints, checks and indexes are included. Existing score columns remain unchanged to preserve import compatibility.

The existing project manages its original schema with Hibernate `ddl-auto=update`. Reporting tables are not JPA entities and are managed only by Flyway. The custom migrator uses `progress_schema_history`, baselines the legacy schema at version 0, and applies V1 without replacing existing tables. Keep `spring.flyway.enabled=false`: this disables Spring's early automatic migrator, not the custom reporting migrator. Do not enable both. No manual SQL changes are necessary.

Run the backend normally against your configured development database:

```powershell
cd Software-project-Backend
# JAVA_HOME must point to a JDK 17 or later.
.\mvnw.cmd spring-boot:run
```

The migrator executes before report services become available and fails startup on a migration failure. Back up a deployed database using the university's usual release process before applying schema changes. The migration has been exercised on isolated H2 in MySQL compatibility mode; the live MySQL database was not modified during verification.

Publishing a curriculum copies the selected source definitions and **approved positive** LO-to-PO mappings. Later edits to the source catalogue do not change these records. Duplicate approved mappings are rejected. Create a new curriculum code/version for changed thresholds or mappings. The student batch must match the curriculum cohort. Student curriculum reassignment is deliberately rejected and requires a reviewed data migration.

Creating an offering freezes assessment-item LO mapping, question label, assessment name and maximum mark. Recorded question scores remain in the existing score table. The offering must match the assessment's module, cohort, year and semester. Create the complete assessment structure before publishing an offering. Foreign keys protect referenced historical definitions from deletion.

Existing marks are not automatically assigned a curriculum, semester attempt or pass decision. Unassigned question marks appear separately with warnings and do not contribute. Legacy LO totals have no reliable maximum or attempt identity; they remain in the marks system and are excluded from attainment, preventing duplicate counting of import mirrors.

## Configuration workflow

Sign in as `admin` or `superadmin`, open the QA configuration panel on the report page, and load the existing codes. Configure in this order:

1. Publish the curriculum and calculation policy using existing module, LO and PO codes. Enter approved credits and LO/PO thresholds; select which POs and modules are required. Only the existing approved positive mappings are copied. To change mapping strengths, use the existing mapping approval workflow and publish a new curriculum.
2. Assign each student's curriculum. Optionally link an **existing** student-role login by username. A student index is never assumed to be a login. Existing user provisioning remains unchanged.
3. Create each academic period and module offering with its existing assessment template IDs. Dates use `YYYY-MM-DD`; these dates determine chronological display.
4. Record student module attempts, official selection, official pass/fail result, final mark/grade and optional grade points. Update the student's academic status when study is complete.
5. Search for the student and select **Generate and preview**. Use **Download full PDF** to download that snapshot.

Curriculum request shape (replace all placeholder values and null thresholds/credits with approved configuration):

```json
{
  "code": "<curriculum-code>",
  "programmeCode": "<programme-code>",
  "programmeName": "<programme-name>",
  "university": "<university-name>",
  "version": "<curriculum-version>",
  "cohort": "<student-batch>",
  "policyVersion": "<approved-policy-version>",
  "retakePolicy": "OFFICIAL",
  "creditWeighted": true,
  "graduationConfigured": false,
  "requiredCredits": null,
  "minimumGpa": null,
  "modules": [{"moduleId": "<existing-module>", "credits": null, "compulsory": true}],
  "los": [{"loId": "<existing-lo>", "threshold": null}],
  "pos": [{"poId": "<existing-po>", "threshold": null, "minimumEvidence": 1, "required": true}]
}
```

Thresholds are percentages from 0 through 100. Credits/weights are positive. New decimal configuration accepts at most four decimal places. Every configured module needs an LO and there must be a required PO. A required PO without mappings is reported as insufficient evidence, never achieved.

## Calculations and policies

**LO percentage** = `100 × sum(obtained mapped question marks) / sum(mapped question maximum marks)`.

Complete, valid evidence is compared with the curriculum's LO threshold. The equality boundary achieves the outcome. Missing marks are null, not zero. An ongoing attempt is `IN_PROGRESS`; an empty or invalid evidence set is `INSUFFICIENT_EVIDENCE`. Completed valid results are `ACHIEVED` or `NOT_ACHIEVED`. Withdrawals, exemptions and absences do not establish an LO score even if old question records exist.

**PO percentage** = `sum(LO percentage × mapping weight × credit weight) / sum(mapping weight × credit weight)`.

Credit weight is the saved module credit value when `creditWeighted=true`, otherwise 1. Both successful and unsuccessful complete LOs contribute. A provisional available-evidence percentage may be displayed, but every applicable mapped LO must be complete and the configured minimum number of distinct module-LO results must be present before a PO is achieved. Missing mappings and missing selections never become zero-weight successes. Evidence details contain marks, maxima, effective weights, weighted numerator values, denominator and percentage-point contributions.

BigDecimal is used throughout new calculations, with DECIMAL128 precision for division. Threshold comparisons precede display rounding; LO comparisons use exact cross-products. Display uses two decimals, HALF_UP. Thus 59.9999% can display as 60.00% and still correctly fail a 60% threshold.

Retake policies:

- `OFFICIAL`: exactly one explicitly marked official attempt. Ambiguous or absent selection gives insufficient evidence.
- `LATEST_COMPLETED`: highest attempt number among PASS/FAIL attempts.
- `BEST`: highest official final mark among completed PASS/FAIL attempts, breaking ties by attempt number. Missing final marks are ineligible.

All attempts remain visible. Evidence from different attempts is never combined. A student's profile row is locked while changing attempts to prevent concurrent duplicate attempt numbers or official selections. Attempted credits include repeats except withdrawals/exemptions. Completed credits count a selected PASS once per module. Exemptions do not automatically award credit or PO evidence.

Academic completion is independent of PO attainment. Until `graduationConfigured=true`, it is `POLICY_NOT_CONFIGURED`. Once explicitly configured, completion requires total approved credits, selected PASS results for compulsory modules, the configured minimum GPA if any, completed academic status, and no unassigned question marks. Pending studies stay `IN_PROGRESS`. GPA is the credit-weighted average of selected completed attempts' supplied official grade points; missing required grade points prevent a completion decision. `minimumGpa=null` explicitly configures no GPA rule. No grade-to-GPA or pass-mark rule is invented.

## APIs and permissions

All routes below require the application's authenticated Spring Security principal and return `Cache-Control: no-store`.

| Method | Route | Purpose |
|---|---|---|
| GET | `/api/reports/progress/students?q=...` | Search index/name; at most 100 authorized results |
| POST | `/api/reports/progress/students/{studentId}/snapshots` | Generate complete history, LO/PO calculations, warnings and audit snapshot |
| GET | `/api/reports/progress/snapshots/{reference}` | Read the same saved preview with current authorization |
| GET | `/api/reports/progress/snapshots/{reference}/pdf` | Download the saved snapshot as PDF |
| GET | `/api/reports/progress/configuration` | Administrator configuration catalogue |
| POST | `/api/reports/progress/configuration/curricula` | Publish immutable version and policy |
| PUT | `/api/reports/progress/configuration/student-programmes` | Assign curriculum/account or update academic status |
| POST | `/api/reports/progress/configuration/offerings` | Publish period/offering and assessment definitions |
| PUT | `/api/reports/progress/configuration/enrolments` | Create/update explicit attempt and official result |

Admin/superadmin can access all records. A lecturer needs access to **every module in the student's recorded history**, using existing module visibility rules. Partial lecturer access is rejected rather than presented as a complete graduation report. A student can access only the explicitly linked student profile. Every snapshot download rechecks authorization. UUID report references are not authorization tokens. Unsupported roles are denied. Raw database identity keys for assessment items are not exposed in reports.

The frontend never submits calculated attainment or thresholds during generation. Filters change visible detail sections; academic/PO conclusions and the downloaded PDF always retain the full saved history. This prevents a selected successful semester from appearing to establish graduation.

Snapshots retain the exact calculated data and policy version; generation, preview and PDF requests are recorded in `qa_report_audit` with actor and timestamp. Configure access/retention for these confidential database records under the university's existing policy. No report contents are written to application logs by this module.

## Tests and sample report

New tests are `AttainmentCalculatorTest`, `ProgressIntegrationTest`, and the rewritten `StudentReportsPage.test.jsx`. They use synthetic data, not production records. Integration tests run migrations, service/API permission checks, version isolation, snapshot stability, retakes, mark validation and PDF extraction. Frontend tests cover search, errors, filtering, configuration validation and exact-snapshot download.

```powershell
cd Software-project-Backend
.\mvnw.cmd '-Dtest=AttainmentCalculatorTest,ProgressIntegrationTest' test
```

The integration fixture produces:

- `target/report-samples/academic-progress-sample.pdf`
- `target/report-samples/academic-progress-sample.json`

It verifies LO1 evidence 8/10 + 16/20 = 80%, LO2 = 60%, mapping weights 3:1, and PO1 = 75% against a 65% threshold. Equal module credit weights leave the 75% result unchanged.

Run the full backend build without a MySQL test server:

```powershell
.\mvnw.cmd '-Dspring.flyway.enabled=false' '-Dspring.datasource.url=jdbc:h2:mem:fullsuite;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1' '-Dspring.datasource.driver-class-name=org.h2.Driver' '-Dspring.datasource.username=sa' '-Dspring.datasource.password=' '-Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect' '-Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect' '-Dspring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl' verify
```

```powershell
cd ../softwareproject_frontend
npm test
npm run build
npm run lint
npx prettier --check src/pages/StudentReportsPage.jsx src/pages/StudentReportsPage.test.jsx src/components/ProgressConfigurationPanel.jsx src/pages/studentReports.css
```

The repository has no standalone Java lint/format configuration or TypeScript type-check command; Maven compiles Java with release 17. Existing H2 repository tests may emit warnings about the legacy `User` table name even when their assertions pass. The dedicated progress integration database configures `NON_KEYWORDS=USER`.

## University approval and limits

QA must approve curriculum membership/cohort, thresholds, mapping strengths, compulsory modules, credits, minimum evidence, retake selection, grade points, graduation rules and staff scope. The implementation assumes all configured LO-to-PO mappings are required evidence for that PO. Elective choice rules, compensation/condonation, transfer credit and professional registration are not inferred. Keep graduation rules unconfigured if the university uses additional rules not represented here.

Existing historical data must be explicitly configured before complete reports can be produced. The interface does not provision student logins, issue digital signatures or decide professional qualification. Current built-in PDF fonts target Latin text; a university requiring other scripts should supply an approved embedded font before using those names in official PDFs. The final awarding decision remains with the university or authorized body.
