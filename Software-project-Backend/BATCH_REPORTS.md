# Batch attainment reports

The second reporting feature is read-only and lives alongside individual student
reports. No tables, saved policies, existing marks imports or existing attainment
services are changed. It reuses StudentReportService.calculate so the individual
and batch reports use the same evidence rules.

## Use

Restart the backend, sign in, and open **Batch reports** in the header
(`/batch-reports`). Enter a batch, load its accessible modules, select the modules
to include, set thresholds, preview, and download the PDF.

Initial report defaults are student LO threshold 50%, batch LO target 70%, and
PO target 70%. They are editable report assumptions, not an assertion of
department policy. Nothing is saved to the database.

Lecturers follow the existing module visibility policy, including unassigned
modules. Administrators and superadmins can select all modules associated with
the batch. A request explicitly naming an unavailable module is rejected.

A module is associated with a batch if its LO batch metadata, legacy mark records
or question assessment templates refer to that batch. The system has no approved
programme/enrolment roster, so this association and its limits are stated in the
report. Selecting fewer modules narrows PO evidence; results are never presented
as full-programme certification.

## Contents

- Selected batch/modules, generation time, scope and three report settings.
- Distinct students with marks in scope (not total enrolled students).
- Per-LO assessed, achieved, below-threshold and pending student counts.
- LO achievement percentages and evidence coverage, with target-line bar graphs.
- Module summary: achieved LOs / all defined LOs and status.
- Active PO summary, target-line graphs and approved mapping contribution tables.
- Incomplete-evidence warnings, review prompts, formulas and scope notes.
- Paginated PDF tables, page numbers and analysis-copy labels.

No individual student names, IDs, email addresses or marks are included in this
aggregate report.

## Calculation definitions

For each module, the population is the union of distinct students with any
question or legacy mark record in that module and batch. A student with no records
anywhere in that module cannot be counted without an enrolment roster.

Individual LO achievement:
sum of valid mapped question marks / sum of question maximum marks x 100,
compared with the selected student threshold. Legacy mirrors are not counted
twice. Missing or invalid scores and unmatched legacy totals retain Pending
status, as in the individual report.

LO achievement percentage:
achieved students / fully assessed students x 100.

Evidence coverage:
fully assessed students / module students with records x 100.

Missing students are excluded from the achievement denominator and counted as
pending; this prevents missing marks from being silently treated as failures.
If any member of the known module population lacks complete LO evidence, the LO
verdict remains Pending even when the available-data percentage is high.
No assessed students means no percentage and Not assessed.

A module achieves the report rule only when every defined LO achieves the batch
target with complete evidence. Its coverage percentage is achieved LOs / all
defined module LOs x 100. This is not the official module pass rate.

PO attainment:
sum(approved positive mapping weight x LO achievement percentage) /
sum(approved positive mapping weights).

The final PO verdict requires complete evidence from all its mapped LOs in the
selected scope. When some are incomplete, a provisional score is computed using
only complete mapped LOs; its denominator contains only their weights. The
complete/mapped count and individual contributions expose what is missing.
If none are complete the value is null. Repeated mappings for the same LO/PO
are excluded and force Pending rather than double weighting the LO.

Only active POs and approved, positive-weight mappings contribute. Unapproved,
rejected and zero-weight mappings are excluded. Active POs without eligible
mappings appear as Not mapped, with no numeric value. Since the existing data
does not link POs to a specific batch/programme, this list can include outcomes
outside that programme; the report explicitly notes that limitation.

These weighted LO rates are not percentages of students passing a PO and do not
reuse cohort level 0-3 as if it were an individual mark.

All stored assessments for the selected batch are included, as in the individual
report. There is no repeat-attempt resolution or separate period filter in this
version. Official module pass rates require approved grading rules and are not
inferred.

## API

All endpoints require a valid Bearer token and staff access.

- GET /api/reports/batches/modules?batch=22
- GET /api/reports/batches/attainment?batch=22&moduleIds=SE101&studentThreshold=50&loTarget=70&poTarget=70
- Add &format=pdf to download the report.
- Repeat moduleIds for multiple modules. Omitting it selects all available batch
  modules; the UI requires at least one selection.
- Invalid/non-finite settings are rejected. Responses use Cache-Control: no-store.
- A download uses saved data at download time; it may differ from an earlier
  preview if marks/mappings were edited in between.
- As with the individual PDF exporter, the built-in font supports Latin-script
  text. Sinhala/Tamil PDF output needs embedded fonts and script shaping.

## Tests

Backend:
mvn -Dtest=BatchReportTest,BatchReportRepositoryTest,StudentReportTest,StudentReportRepositoryTest test

Frontend:
npm test -- src/pages/BatchReportsPage.test.jsx src/pages/StudentReportsPage.test.jsx

Production frontend:
npm run build

Backend database tests use an isolated in-memory database and synthetic students.
The PDF test writes target/report-samples/batch-report-sample.pdf.
