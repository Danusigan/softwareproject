# Security implementation checklist

Review date: 2026-10-05. Source revision: `6d00d6b`, plus the working tree.

Status: **Planning complete; remediation and fresh security verification pending.**

## How to use this plan

This checklist follows a review of all eleven existing Markdown documents in `security/` (`Security_plan.md` and `01` through `10`), compared with selected current implementation files. It supersedes their completion claims for planning this new pass, while preserving them as historical evidence. This is a source review, not a new penetration test or a full endpoint audit. No security implementation, database changes, secret rotation, or active scans were performed during this review. The existing frontend lockfile modification belongs to earlier work.

Implement in the order below. Each change should include its regression tests and evidence before its checkbox is closed. P0 means first remediation work; P1 means required hardening; P2 means production/release controls. These are implementation priorities, not formal CVSS scores. Production controls become release blockers if the application is already externally accessible.

## What the current code actually contains

Paths in this table are relative to `Software-project-Backend/src/main/java/com/example/Software/project/Backend/` unless otherwise stated.

| Area | Current source evidence | Action |
|---|---|---|
| Password hashing | `Security/SecurityConfig.java` uses BCrypt. `Service/UserService.java` hashes account creation and self-service password changes, but `updateLecturer` and `updateAdmin` directly call `setPassword(password)`. | Fix the two plaintext update paths first; check all other credential writes. Database contents have not been inspected. |
| Authentication | Method security, narrowed public routes, five-failure/15-minute account lockout, and two-hour JWTs are already present. | Preserve working controls and add missing lifecycle/concurrency tests. |
| JWT/account state | `Security/JwtRequestFilter.java` loads the current user without handling a missing user, and creates an authenticated token without checking the loaded account-state flags. Many controllers separately authorize from the JWT role claim. | Make account state and current server-side authorities authoritative; handle filter failures explicitly. |
| Role authorization | Manual role checks plus some `@PreAuthorize` annotations exist. Actual authorities are lowercase `admin`, `superadmin`, `lecture`. | Do not repeat the historical claim that all endpoints lack RBAC. Consolidate and test the actual policy. Uppercase authority examples in document 08 do not match current code. |
| Module ownership | `Model/Module.java` has lecturer assignments; module lists are filtered. `ModuleRestController.getModuleById` does not apply an assignment check. `ModuleService.getModulesForLecturer` includes unassigned modules for everyone. | Close direct-object and indirect-object gaps; choose an explicit policy for unassigned modules. |
| CQI/reporting | CQI controller/pages now exist. Some CQI actions and `Reporting/Progress/ProgressAccess.java` check ownership. CQI finalize/trigger controller paths check role but do not supply actor identity to the called service methods. | Include these live features in the authorization audit. Do not treat historical CQI exclusions as current scope. |
| Password reset | Reset tokens expire after 30 minutes and have a used flag. They are stored directly; reset accepts six-character passwords while creation/change uses a different policy. | Unify policy, protect reset-token storage, throttle recovery, and verify atomic consumption and session invalidation. |
| Uploads | `Service/FileValidationService.java` and multipart limits already exist: 5 MB/file, 6 MB/request; `.xls` and `.xlsx` allowed. MIME type may be absent or generic. | Retest every import path and strengthen workbook/content/resource validation. |
| Audit/errors | Audit entity/service and global exception handler exist. Audit calls cover selected login/account/PO/mapping actions; many controllers still return `e.getMessage()`. | Complete coverage and sanitize errors handled locally as well as globally. |
| Secrets/profiles | Main properties require environment placeholders for DB credentials and JWT secret. Main properties also activate `dev` and verbose logging by default; a quieter `prod` profile exists. | Verify all profiles, remove unsafe default behavior, and rotate any historically exposed credentials that remain valid. Environment placeholders do not prove rotation. |
| Database schema | Flyway V1-V10 and `ddl-auto=validate` now exist. `Service/LegacySchemaFixService.java` still performs startup DDL and ignores failures. | Preserve migrations; move legacy startup changes into reviewed migrations and separate runtime/migration DB privileges. |
| Frontend/network | Services largely use relative API paths; the historical hardcoded-localhost finding is no longer representative. JWTs remain in localStorage. nginx has a broad CSP allowing HTTP/HTTPS and inline content. CQI has a separate CORS annotation. | Verify all API routing; harden token handling, CSP, and central CORS settings. |
| Deployment/CI | Production Compose, AWS infrastructure, SonarQube workflow steps, image publishing and deployment jobs exist. CodeQL still lists JavaScript only; image build depends on backend/frontend jobs. | Review actual release gates and production exposure. Old claims of local-only scope and no publishing pipeline are stale; a live deployment was not checked. |

## 0. Establish a safe and reproducible baseline

- [ ] Create a dedicated remediation branch; preserve unrelated working-tree changes.
- [ ] Inventory all actual controller mappings, including `Reporting/`, CQI, student management, profile/password change, recovery, exports and bulk operations. Produce an endpoint/method/role/object-scope matrix.
- [ ] Confirm whether any deployment is externally accessible. Keep active security tests on an isolated local stack with synthetic data; production configuration review does not authorize production attacks.
- [ ] Record baseline backend/frontend tests and a reproducible build using `npm ci`. Resolve the previously observed `dist/assets` permissions issue if it still blocks the normal build.
- [ ] Back up any data that must survive account/schema changes and rehearse restore. Never use `docker compose down -v` against data that must be retained.
- [ ] Replace assumptions in documents 01-08 with the current inventory; retain old results as dated history. Do not label unfinished ZAP/dependency scans in document 10 as passed.
- [ ] Prepare synthetic SuperAdmin, Admin, Lecturer A and Lecturer B accounts, assigned/unassigned modules, mixed-module students, marks and CQI records. Repair the seed script's table-name casing and broad error suppression; fail on real seeding errors.

Done when: the stack and role fixtures are reproducible, baseline results are recorded, and every live route appears in the access matrix.

## 1. P0: Close credential-write regressions and unsafe defaults

Requirements: REQ-CRYPTO-01/02, REQ-AC-03, REQ-CONF-02.

- [ ] Route `updateLecturer`, `updateAdmin`, account creation, self-change and recovery through a shared password-policy/hash service. Preserve blank-password-as-no-change only where that is the documented update behavior.
- [ ] Check for previously stored plaintext credentials using counts/classification only; do not print passwords or hashes. Plan a controlled reset or one-time migration with backup. Do not silently enable plaintext login fallback or double-hash existing hashes.
- [ ] Use separate account request/response DTOs; prevent role, lock-state, or other server-managed fields from being mass-assigned. Verify every response omits password hashes and reset credentials.
- [ ] Remove automatic `dev` activation from shared defaults; keep verbose SQL/security logging in explicit local development only. Verify test-user bootstrap and debug endpoints are absent/denied in production, with no known test accounts auto-created there.
- [ ] Scan tracked files and history for secrets with redacted output, including old security/Postman documentation. Rotate any still-valid exposed DB/JWT/mail/cloud credentials. Removing text or rewriting history does not revoke a credential.
- [ ] Document how environment variables reach both manual Spring Boot runs and Docker. A root `.env` is not automatically loaded by every process; never put secrets in frontend `VITE_*` variables.

Done when: every password write stores a valid hash, replacement passwords can log in, old passwords fail, no response exposes credentials, and a production boot cannot enable the test bootstrap path. Secret rotation is evidenced without recording values. Any history rewrite remains a separate explicitly approved operation.

## 2. P0: Enforce role and object access consistently

Requirements: REQ-AC-01/02; complete the previously deferred ownership work.

- [ ] Define the intended permissions per action. Preserve the existing role names; use lowercase `hasAuthority`/`hasAnyAuthority` consistently instead of mixing uppercase or `ROLE_` conventions.
- [ ] Replace controller JWT role decoding with authorization based on the authenticated principal and current server-side authorities. Centralize checks in a reusable policy/service.
- [ ] Reuse existing module assignments; enforce access on direct module reads and on LO, mark, assessment, mapping, CQI, student-report and export IDs resolved back to their modules.
- [ ] Apply the same checks to list/search, bulk upload, batch delete, download and indirect/nested resource paths. Filtering the module picker alone is insufficient.
- [ ] Treat unassigned modules as inaccessible to lecturers by default in the proposed policy; backfill explicit assignments before enabling it. If open-module access is required, define its exact allowed actions instead of inheriting blanket access.
- [ ] Audit CQI finalize/trigger routes, PO catalog aliases under `/api/obe`, and every alternate approval/delete route for equivalent authorization.
- [ ] Preserve existing report privacy checks; define what a lecturer may see for a student spanning multiple modules. The `student` branch in `ProgressAccess` does not establish a supported student login: reconcile it with `User`, which accepts only three staff roles.
- [ ] Enforce allowed approval/status transitions server-side and prevent request bodies from overriding actors, ownership or approval state.

Done when: tests through the real security filter chain prove anonymous rejection, Lecturer A/B isolation, Admin/SuperAdmin separation, and valid authorized workflows. A changed ID, role in localStorage, stale JWT role or alternative endpoint must not bypass policy. Adopt consistent 401 for failed authentication and 403 (or deliberately concealed 404) for denied resource access.

## 3. P1: Harden login, JWT lifecycle and recovery

Requirements: REQ-AUTH-01/02, REQ-STORAGE-01; expand scope for recovery.

- [ ] Catch missing-user and invalid/expired-token failures inside the filter; return controlled 401 responses. Do not rely on MVC advice to catch filter exceptions.
- [ ] Check enabled/locked/deleted account state before authenticating bearer requests. Define whether temporary login lockout also ends existing sessions, and test the chosen rule explicitly.
- [ ] Make lockout counters safe under concurrent failures and expiry; add bounded request throttling for login and forgot/reset-password. Cover unknown usernames and trusted proxy handling without allowing spoofed forwarding headers to bypass limits.
- [ ] Use one password policy across creation/change/admin-update/reset. Review the old eight-character composition policy as a product decision; while retaining BCrypt, handle its input-byte limit explicitly. Never apply new-password complexity checks to existing-password login.
- [ ] Store only a digest of reset tokens; consume them atomically once, reject expiry/reuse, invalidate older tokens, and verify two concurrent submissions cannot both succeed. Keep recovery responses generic and rate-limit mail delivery.
- [ ] Invalidate existing sessions after password reset/change, account deletion and privilege changes using a documented mechanism, such as a server-side token version. Add logout revocation if required by the chosen session design; refresh tokens are not mandatory to achieve revocation.
- [ ] Document the browser session design before changing it. Proposed production direction: an HttpOnly, Secure, SameSite cookie with coordinated CSRF protection and origin checks. If bearer tokens in localStorage are temporarily retained, record XSS exposure and a deadline rather than claiming equivalent protection. Cookie conversion must include frontend, CORS, CSRF, logout and deployment tests together.
- [ ] Prevent reset links/tokens from appearing in logs, analytics and referrers; clear the token from the browser URL when safe. Validate configured reset-link origin and SMTP transport settings.

Done when: malformed/expired/deleted-user tokens are cleanly rejected; password recovery cannot weaken policy; reset replay/concurrency, throttling and session invalidation tests pass.

## 4. P1: Validate requests and bound file processing

Requirements: REQ-INPUT-01/02, REQ-FILE-01, REQ-XSS-01.

- [ ] Add validated DTOs to module, LO/PO, marks, assessment, CQI, profile and reporting mutations. Bound lengths, collections, pagination and report ranges; reject invalid enums, non-finite numbers and marks outside valid ranges.
- [ ] Recheck parameter binding in all repositories, reporting SQL and dynamic queries. Do not infer safety just from JPA usage or flag all SQL concatenation as exploitable without checking its inputs.
- [ ] Call shared upload validation before parsing in every import route, including student imports. Explicitly decide whether both `.xls` and `.xlsx` are needed.
- [ ] Verify actual workbook structure rather than trusting extension/MIME; cap rows, columns, sheets, decompressed content and processing resources. Preserve POI archive protections and reject malformed/encrypted/unsupported files safely.
- [ ] Validate each imported record's ownership and business rules before writes; define atomic rejection versus explicit partial-import behavior and test rollback.
- [ ] Treat spreadsheet text as text on export; handle formula-like untrusted values deliberately. Recheck React rendering and links for unsafe HTML/URL handling.

Done when: oversized/spoofed/malformed workbooks and invalid records fail with controlled 400/413 responses without unauthorized or partial unintended writes; valid marks/student import and export still work.

## 5. P1: Complete audit trails and consistent error handling

Requirements: REQ-LOG-01, REQ-CONF-02.

- [ ] Replace controller-level raw exception responses with safe domain errors and generic internal errors. Preserve intended 400/401/403/404/409/413/429 status codes; ensure security exceptions do not become 500s.
- [ ] Record account updates/deletes, password changes/resets, assignments, marks changes/imports, CQI transitions, alternate approval paths and sensitive exports, in addition to the events already recorded.
- [ ] Include actor, action, resource, UTC time, outcome and correlation ID; redact secrets and bound/sanitize user-controlled fields. Avoid copying entire student records into logs.
- [ ] Define audit failure behavior and monitoring; test transaction rollbacks so success events correspond to committed changes and security failures remain observable.
- [ ] Make retention configurable (90 days is the historical default), restrict access and protect against tampering. Test scheduled retention and recovery from audit storage failure.

Done when: representative successful and denied actions have attributable, sanitized evidence, and internal exception text never reaches API consumers.

## 6. P2: Harden deployment and database operations

Requirements: REQ-CONF-01/02/03, REQ-HEADERS-01; production release gate.

- [ ] Centralize environment-specific CORS; remove conflicting controller annotations. Verify relative `/api` routing in Vite and nginx, including the optional comparison-page API override. Limit token attachment to the intended API origin.
- [ ] Configure and verify public HTTPS, redirects and HSTS at the actual TLS edge. Current production Compose exposes port 80; establish whether an external TLS terminator exists before assuming HTTPS is absent or present.
- [ ] Tighten CSP to required sources/directives, remove unnecessary broad HTTP/HTTPS and inline allowances, and verify headers on actual HTML, errors and relevant routes. Test UI behavior under the resulting policy.
- [ ] Review infrastructure security groups, DB/public exposure, container users/capabilities, secret delivery and SMTP settings. Require verified database server identity in production TLS, with the correct CA configuration.
- [ ] Separate migration credentials from least-privilege runtime DB credentials. Move `LegacySchemaFixService` startup DDL into new versioned migrations; do not rewrite already-applied Flyway migrations.
- [ ] Test both a fresh database and an upgrade from a representative legacy backup. Keep production `ddl-auto=validate`; review initialization services for default-account/data creation.
- [ ] Document encrypted backups, access/retention and restore procedure; perform an isolated restore and record recovery results. Inspect existing infrastructure backup/encryption settings before adding duplicate mechanisms.

Done when: staging runs with production settings, TLS/origin/header checks pass, runtime cannot perform unneeded DDL/admin actions, migrations succeed and a backup is actually restored.

## 7. P1/P2: Make security checks block unsafe releases

Requirement: REQ-DEP-01; applies across the earlier phases.

- [ ] Run fresh frontend and backend dependency scans; record exact lockfile/artifact versions, tool versions and timestamps. Historical counts, the earlier npm report and an interrupted scan are not current clearance.
- [ ] Select supported compatible versions from current advisories and upstream release notes. Upgrade frontend build tooling and backend dependencies in tested changes; do not use blind `npm audit fix --force` or equate every dev-only finding with production exploitability.
- [ ] Add maintained Java and JavaScript CodeQL coverage, completed dependency/container scans and secret scanning. Verify SonarQube results and quality-gate behavior instead of merely detecting a workflow step.
- [ ] Require security checks to pass before image publishing/deployment. Review dependency edges, least-privilege workflow permissions, action pinning, untrusted PR handling and secret exposure.
- [ ] Record any accepted finding with justification, owner and expiry. Fix reachable high-impact findings before release; report failed/incomplete scanners as incomplete, not successful.

Done when: a controlled failing security check prevents release and all required scanners produce reviewable artifacts for the candidate commit.

## 8. Verify the full application and refresh the evidence

- [ ] Run backend unit/integration tests and frontend tests/build. Add real-filter-chain integration tests where mocks would otherwise bypass authentication or authorization.
- [ ] Exercise login, all role dashboards, account management, module assignment, marks upload/export, LO/PO approvals, CQI, student reports and recovery using synthetic data.
- [ ] Repeat cross-role and cross-object tests, JWT misuse, input/error tests, upload bounds and recovery abuse tests against the isolated full stack.
- [ ] Complete a local ZAP baseline scan and authenticated manual coverage; a passive baseline alone does not establish authorization correctness. Save complete scanner reports and investigate findings.
- [ ] Update documents 01-10 and the master plan with current scope, corrected diagrams, requirement-to-test mappings and remaining risks. Link evidence to the tested commit/environment; never overwrite historical results as if they tested this revision.
- [ ] Record final release decision with no unresolved P0 issue, completed agreed P1 controls and production prerequisites met. A passing scan is evidence for specific checks, not a guarantee that the application has no vulnerabilities.

## Suggested delivery sequence

| Change set | Scope | Prerequisite |
|---|---|---|
| 1 | Baseline, credential-update fixes, safe account DTOs/defaults, focused tests | Baseline and protected data |
| 2 | Current-authority RBAC plus shared module/object policy and endpoint tests | Approved access matrix and assignment backfill plan |
| 3 | JWT/account lifecycle, consistent password policy and recovery | Credential fixes; session design recorded |
| 4 | Validation, bounded imports, error handling and audit completion | Shared authorization policy |
| 5 | Deployment/DB/CI hardening and dependency remediation | Compatible migrations and completed scan results |
| 6 | Full-stack regression, local security retest and updated reports | Previous change sets integrated |

## Reference guidance

The plan uses the repository's requirements as its main input. Current OWASP guidance supports checking permissions on every request, consistent password hashing, and recovery-token protection:

- [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [OWASP Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html)
