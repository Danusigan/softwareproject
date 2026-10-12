# Application audit logging

Feature 6 of the agreed eight-feature security sequence. Verification completed on 2026-10-12; final results are in the progress document.

## Events and privacy

The security-chain audit filter records every non-read API request, selected report/export/download reads, and failed or denied API requests. This covers account edits/deletes, password changes/recovery, module assignments, marks/student imports, CQI transitions and alternate approval routes without depending on each controller remembering an audit call.

HTTP action names identify the controller and method, for example `HTTP UserRestController.updateLecturer`. Each record includes authenticated actor (or `ANONYMOUS`), route pattern and bounded path identifiers, outcome (`SUCCESS`, `FAILURE` or `DENIED`), UTC timestamp, HTTP method/status and a server-generated request ID. The response exposes that ID in `X-Request-ID`; incoming values are not trusted. Unmatched requests use a fixed resource label rather than copying arbitrary URLs. Ordinary successful reads, health checks and OPTIONS requests do not flood the log.

Existing semantic LOGIN, CREATE_USER, PO and mapping events remain alongside the HTTP event and share its request ID. A committed password recovery now adds PASSWORD_RESET with the recovered account identity. Public login HTTP events are anonymous; the existing LOGIN event records the account attempting login. HTTP outcomes describe the request, not every internal business operation.

No request/response bodies, upload contents, Authorization headers, query strings, passwords, reset tokens or JWTs are copied into HTTP audit rows. All fields are sanitized and bounded to the existing 255-character schema limits. Additional redaction removes secret-like assignments and JWT-shaped strings. Mapping approval remarks are no longer copied into audit details. Default HTTP body and JDBC bind logging are suppressed in application configuration.

## Persistence and failure behavior

Audit writes use an independent REQUIRES_NEW transaction and flush before completion. Successful service events raised within an active business transaction wait until its commit, so rollback produces no false success event. Failure events can persist even when the business transaction rolls back. Request events are recorded after the controller/service returns, including validation errors and authorization denials.

Audit storage failures do not undo or fail an otherwise valid business operation. They increment `app.audit.write.failures` and emit a sanitized server error containing only the action and exception class. Cleanup failures use `app.audit.cleanup.failures`. Later calls retry normally. This is best-effort logging: events lost during a storage outage are not automatically replayed; monitor these counters/logs. No durable outbox is introduced in this feature.

New audit timestamps use UTC and an audit-only JDBC converter. The live MySQL test caught a 5.5-hour offset caused by interpreting UTC LocalDateTime through the JVM's local Timestamp conversion; the converter now supplies the intended UTC instant to the configured UTC JDBC connection. No global application clock, other entity timestamp mapping or schema is changed. Existing audit history is not rewritten.

## Retention and access

- `AUDIT_RETENTION_DAYS` / `app.audit.retention-days`: default 90; accepted range 1-3650.
- `AUDIT_CLEANUP_CRON` / `app.audit.cleanup-cron`: default daily at 03:00 UTC. Spring's `-` value disables scheduling when needed.
- Cleanup runs transactionally; failures are recorded and do not disable subsequent scheduled attempts.
- No API for browsing, editing or deleting audit rows is added. Normal application requests can append events, not modify audit history. Database administrators retain their existing database permissions; this is not a cryptographically tamper-evident log.

## Verification and commit scope

Final results: **329 backend tests passed** (no failures/errors/skips), **42 frontend tests passed**, and the listed live obqa/browser checks passed. Original Admin, Lecturer and SuperAdmin credentials and dashboards remain working. Details are also recorded in [12-application-security-progress.md](12-application-security-progress.md).


Tests cover actor/outcome/request-ID attribution, denied and failed requests, login/recovery records, privacy, field bounds, UTC conversion, successful-commit versus rollback behavior, retention and recovery from simulated audit-storage failures. Separate filter tests cover successful exports, skipped ordinary reads, denied reads and unexpected exceptions. Existing application regressions are also run.

Live checks use local `obqa` and disposable accounts/modules/records. They verify real stored timestamps, correlation IDs, selected actions, secret exclusion and upload audit outcomes. Original account login/dashboard checks are repeated. Temporary business records are removed; audit evidence is retained. Retention deletion is tested only in isolated H2, not by purging the user's existing history.

Commit backend source/tests, this document and the progress document. There are no frontend source changes, database migrations or local credential-file changes.

Suggested commit: `feat(security): record safe transactional application audit events`

Stop after this feature for the user's commit/push. Feature 7 (environment-based secret protection) has not started.
