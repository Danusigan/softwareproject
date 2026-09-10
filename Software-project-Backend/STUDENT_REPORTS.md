# Individual student reports

This feature adds read-only student reporting. It does not change marks, imports,
existing attainment calculations, database tables, or saved thresholds.

## Use

Restart the backend after installing the new OpenPDF dependency (Java 17+).
Run the existing frontend and sign in as an administrator, superadmin, or lecturer.
Open **Student reports** in the header (route: /student-reports), enter a batch,
select a student, choose the report threshold, preview, and download the PDF.

Only students with recorded marks in accessible modules are offered. Lecturers
follow the existing module visibility policy, including unassigned modules.
Administrators and superadmins can report across all modules with student evidence.
The result is not a complete enrolment record or an official transcript.

## Contents

- Student ID, name, email, recorded academic year, selected batch, timestamp.
- Module-by-module LO summary, achieved/below-threshold/pending status and gap.
- LO score bars and threshold markers, question marks and maxima.
- Assignment/exam labels, academic years and semesters where recorded.
- Legacy LO totals, explicitly identified as reference data.
- Calculation notes, incomplete-evidence warnings, page numbers and confidentiality footer.

LO percentage uses sum(question scores) / sum(question maximum marks) x 100.
All questions must have finite, valid scores and positive maxima before the LO is
classified. Zero is a valid score; missing marks are not treated as zero. The
threshold is a report parameter, not a change to the department's stored policy.
Cohort attainment level bands are not applied to individual students.

All stored assessment templates for the chosen batch and evidenced modules are
included. This first version does not resolve repeat attempts or apply final
coursework/exam weighting. Year/semester are displayed on each assessment so
reviewers can identify scope. Module pass/fail and degree-level PO certification
are deliberately not inferred without their official rules.

Question records take precedence over legacy aggregate records sharing the same
assessment type and label. Legacy rows remain visible but are not summed again.
Unmatched legacy totals or incomplete question evidence keep an LO Pending:
the report does not assume a missing maximum is 100.

The built-in PDF font supports Latin-script text. Additional embedded fonts and
script shaping would be required for Sinhala/Tamil PDF text; the web preview
uses the browser's fonts.

## Read-only API

All requests require Authorization: Bearer <token>; responses use Cache-Control: no-store.

- GET /api/reports/students?batch=22
- GET /api/reports/students/individual?studentId=S1&batch=22&threshold=50
- GET /api/reports/students/individual?studentId=S1&batch=22&threshold=50&format=pdf

The PDF is generated from the current saved data at download time, so its
timestamp/data can differ from an earlier preview if marks were edited meanwhile.
No report is saved to the database. Treat downloaded files as confidential.

## Verification

Backend: mvn -Dtest=StudentReportTest,StudentReportRepositoryTest test
Frontend: npm test -- src/pages/StudentReportsPage.test.jsx
Frontend production build: npm run build

The backend tests use fixtures and an isolated in-memory database, never live
student data. A synthetic multi-page sample is written to
target/report-samples/student-report-sample.pdf by the PDF test.
