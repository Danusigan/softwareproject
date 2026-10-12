# Application security feature progress

User-selected scope: implement these eight features one at a time. Test and fix each feature, then hand it back for the user's Git push before starting the next. This sequence takes precedence over the broader roadmap in document 11.

| Order | Feature | Status |
|---|---|---|
| 1 | BCrypt password hashing | Implemented and automated checks passed; ready for user push |
| 2 | Role-based access control | Implemented; automated and live obqa checks passed; ready for user commit/push |
| 3 | Login lockout | Implemented; automated and live obqa checks passed; ready for user commit/push |
| 4 | Account input validation | Implemented; automated and live obqa checks passed; ready for user commit/push |
| 5 | Excel upload validation | Implemented; automated and live obqa checks passed; ready for user commit/push |
| 6 | Audit logging | Implemented; automated and live obqa checks passed; ready for user commit/push |
| 7 | Environment-based secret protection | Implemented; automated and live obqa checks passed; ready for user commit/push |
| 8 | Safer error handling | Implemented; automated and live obqa checks passed; ready for user commit/push |

## Feature 8: safer error handling - 2026-10-12

Completed safe exception handling across controllers, reporting endpoints, authentication filters and the global MVC boundary. See [19-safer-error-handling.md](19-safer-error-handling.md) for status mapping, compatible response contracts and operational limits.

- Replaced raw exception messages/class-name fallbacks in API responses with shared safe messages. Explicit account/upload validation remains useful; unexpected failures return 500, database constraint conflicts 409, missing records 404, and malformed requests retain the appropriate 4xx status. Authentication/RBAC/lockout behavior remains intact.
- JSON API errors carry the server-generated audit request ID and no-store headers; filter authentication failures follow the same convention. Existing success responses and legacy plain-text error contracts are preserved.
- Spring Boot fallback errors exclude exception names, messages, binding details and stack traces even with `?trace=true`. Logging uses exception class/request ID rather than raw exception text or submitted email. The framework exception resolver and request/response body processors no longer emit raw details under general debug mode.
- Regression testing found legacy Excel exception wrappers hiding validation types; fixed them so invalid workbooks remain 400 with rollback, while genuine infrastructure failures remain 500. Explicit missing-record exceptions distinguish absence from server failures in domain services.
- Final full backend suite: **359 passed**, zero failures/errors/skips (`mvn -B test`), including **16 safe-error tests** covering injected SQL/token/file-path sentinels, controller-local catches, malformed requests, wrong methods/content types, missing resources, useful validation, conflicts, correlation, log privacy and JWT database lookup failures. Existing hashing, RBAC, lockout, account validation, upload rollback, audit and secret configuration suites passed. The final two body-logger configuration guards were additionally verified in the running application.
- Frontend regression suite: **42 passed across 7 files** (`npm test -- --maxWorkers=1`). No frontend source or generated build output changed.
- Backend started via the private environment launcher against **obqa**; health UP, existing schema unchanged. Live 400/401/403/404/405/415 responses were safe, uncached and correlated; fallback `trace`/`message` query parameters exposed no internal details. Database faults/conflicts were injected in automated tests, not by interrupting the real database.
- Original Admin, Lecturer and SuperAdmin credentials passed browser login/dashboard/API checks. Live account changes, invalid input, failed login, denied actions, module assignments, report access, recovery and audit correlation/privacy passed.
- Browser/API upload checks passed for file type/empty/size controls, spoofed Excel, actual HTTP 413, student/LO/question templates, valid replacement and invalid-mark rollback. Disposable data was removed; original affected-table counts were restored, with normal audit/access records retained. No outbound email was sent.
- Runtime logging after live flows contained no DB password, JWT signing key, JWT-shaped tokens or request credential payloads. Ordinary-word matches for legacy passwords occurred only in fixed startup text and were reviewed separately. Git whitespace checks passed and no unmerged entries remain.

Backend/frontend remain on ports 8080/5173. Evidence is in ignored `.codex-tmp/errors-*.log`. All eight selected application features are now implemented; historical credential rotation documented in feature 7 remains a deployment follow-up. No commit or push was performed.

Suggested commit: `feat(security): return safe consistent API errors`

## Feature 7: environment-based secret protection - 2026-10-12

Implemented required environment configuration, local startup support and build exclusions. See [18-environment-secret-protection.md](18-environment-secret-protection.md) for setup, scope and credential-history limitations.

- Moved the existing local database/JWT values privately from the ignored development properties file to ignored `.env.backend.local`. The new PowerShell launcher loads literal values into the backend process, validates required settings, respects existing shell values and restores loaded variables on exit. The Docker `.env`, obqa database credentials and all application passwords were preserved.
- MySQL configuration rejects missing/blank/unresolved credentials before datasource creation. JWT validation rejects missing, short and common placeholder keys without returning their contents. Base configuration no longer selects dev implicitly; log verbosity is reduced and management configuration values remain hidden.
- Local credential files are excluded from Maven resources and Docker build contexts. Expanded environment ignore rules and blank-secret examples prevent accidental commits. Compose requires database/JWT secrets and a separate local MySQL root password.
- Removed a current database password from the historical asset inventory. Existing Git history was not rewritten and credentials were not rotated: coordinate rotation before deployment. This is an outstanding operational exposure, not something environment placeholders retroactively fix.
- Full backend suite: **343 passed**, zero failures/errors/skips (`mvn -B test`), including 14 new secret-configuration/JWT cases. Backend packaging passed (`mvn -B -DskipTests package`). JAR inspection confirmed no local development configuration or current backend DB/JWT secrets in packaged application classes/resources.
- Frontend: **42 passed across 7 files** (`npm test -- --maxWorkers=1`). The first concurrent run passed 36 tests but had one worker-startup timeout; rerunning after the backend suite finished passed all 42. No assertions were disabled and no frontend source/build output changed.
- PowerShell loader checks passed for literal metacharacters, precedence, restoration on success/failure, duplicate entries, weak keys and safe errors. Local and production Compose configuration validation passed without starting containers; empty required secrets were rejected. The packaged production application also exited with a safe configuration error when database credentials were absent.
- Backend started using the new launcher against **obqa**; health UP and Flyway V10 unchanged. Original Admin, Lecturer and SuperAdmin browser logins/dashboard API checks passed with unchanged passwords.
- Live regression checks passed for account validation/updates, failed and successful login, role denial, module assignments, report access, password recovery, audit correlation/privacy, real upload HTTP 413, student/LO/question templates, invalid marks and rollback. Disposable records were removed and original affected-table counts restored; normal audit/access records remain. No external emails were sent.
- Tracked project files (excluding vendored `.codex-tmp` tools) and the startup log were scanned without printing values: neither current backend database password nor JWT key remained. Environment files are ignored, safe examples are visible to Git, `git diff --check` passed and no unmerged entries remain.

Backend/frontend remain available on ports 8080/5173. Start future local backend sessions from the repository root with `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-backend.ps1`. Test output is in ignored `.codex-tmp/secrets-*.log`; local database backups were retained. Feature 8 has not started; no commit or push was performed.

Suggested commit: `feat(security): externalize and validate backend secrets`

## Feature 6: audit logging - 2026-10-12

Completed shared request audit coverage, safe event storage, configurable retention and recovery-event logging. See [17-audit-logging.md](17-audit-logging.md) for event scope, transaction/failure behavior and operational settings.

- API mutations, sensitive report/export reads and denied/failed requests now record actor, operation, bounded resource identifiers, outcome, HTTP status, UTC timestamp and a server-generated request ID. Existing semantic login/account/PO/mapping records are retained; password recovery records its account after commit.
- Request bodies, upload contents, query strings, Authorization headers and response payloads are not copied into audit records. Approval remarks were removed from audit details. Fields are bounded/sanitized; default HTTP body and JDBC bind logging are suppressed.
- Success service events wait for business commit. Independent audit transactions preserve failure events across rollback. Storage/cleanup failures have sanitized logs and counters and do not poison business commits. Default retention is configurable at 90 days, with daily 03:00 UTC cleanup.
- Final backend suite: **329 passed**, zero failures/errors/skips (`mvn -B test`), including 10 audit integration tests and 6 filter tests. Tests cover rollback, commit, storage failure/recovery, retention failure/recovery, correlation, actor/outcome, privacy, UTC conversion, sensitive reads and thrown exceptions.
- Frontend regression suite: **42 passed across 7 files** (`npm test -- --maxWorkers=1`). No frontend source changes were needed for this feature.
- Live MySQL verification identified and fixed a 5.5-hour JDBC timestamp offset. The audit-only converter now writes the intended UTC instant with the existing UTC JDBC connection; other application timestamps and existing history were not rewritten.
- Live **obqa** checks passed for account edits, bad input, failed/successful login, denied actions, module assignments, report access and recovery. Stored request IDs, actors, outcomes and UTC timestamps matched actual requests; tested passwords, private email and reset/JWT tokens were absent from the new audit rows.
- Browser/API upload regressions passed, including actual HTTP 413, downloaded student/LO/question templates, invalid/empty mark rejection and replacement rollback. Each instrumented upload had exactly one HTTP audit event with the matching outcome. Temporary data was removed and original affected-table counts restored.
- Original Admin, Lecturer and SuperAdmin credentials still logged in and loaded their dashboards without uncaught browser errors or failed dashboard API requests. Temporary account/module/token records were removed; audit/access evidence remains. Retention purging was tested only in H2, not against the user's history.
- `git diff --check` passed; no unmerged entries remain. No schema migration, local credential-file or generated frontend changes are included. Backend/frontend remain running on ports 8080/5173.

Suggested commit: `feat(security): record safe transactional application audit events`

Stop here for the user's commit/push. Part 7 (environment-based secret protection) has not started. Earlier checkpoints below are historical.

## Feature 5: Excel upload validation ? 2026-10-09

Implemented shared workbook validation, template/row checks and transactional replacement handling. See [16-excel-upload-validation.md](16-excel-upload-validation.md) for limits, supported formats and compatibility details.

- Reject empty/oversized files, spoofed or malformed Excel content, unsafe cells, oversized workbook structures, incorrect headers, duplicate/missing student IDs and invalid/non-finite/out-of-range marks.
- Keep support for `.xls` and `.xlsx` and the application's existing template layouts. An unfilled marks template cannot clear existing results.
- Replace the older frontend delete-before-upload sequence with one transactional backend replacement, including attachment storage. Invalid student edits, mark replacements and question imports preserve existing data.
- Final full backend suite: **313 passed**, no failures/errors/skips (`mvn -B test`), including 12 real-controller/persistence upload integration cases, workbook-limit tests and existing BCrypt/RBAC/lockout/input-validation coverage.
- Frontend: **42 passed across 7 files** (`npm test -- --maxWorkers=1`). Production build passed; existing non-blocking build warnings remain.
- Live browser and HTTP checks against local **obqa** passed: client-side extension/empty/size rejection, server-side spoofed-file rejection, actual multipart HTTP 413, and successful import of edited student/LO/question templates downloaded from this application.
- Live database assertions confirmed rejected student edits, invalid and empty marks templates, and failed replacements preserved previous records/scores/attachments. Valid replacements succeeded. All temporary records were deleted and original counts of the eight affected tables were restored; normal access/audit records were generated.
- Original Admin, Lecturer and SuperAdmin credentials still signed in and loaded their dashboards without uncaught browser errors or failed dashboard API calls.
- `git diff --check` passed; no unmerged entries or source conflict markers found. Generated frontend output was restored. No schema migration, credential or environment-file change is included.
- Backend and frontend are running on ports 8080 and 5173. Use **http://localhost:5173/**.

Suggested commit: `feat(security): validate Excel uploads and preserve rejected replacements`

Stop here for the user's commit/push. Part 6 (audit logging) has not started. The sections below retain earlier feature checkpoints.

## Feature 4: account input validation ? 2026-10-08

Implemented shared backend rules for new account details and password changes, plus frontend validation and guidance. See [15-account-input-validation.md](15-account-input-validation.md) for rules, compatibility and verification.

- Full backend suite: **276 passed**, no failures/errors/skips, including 25 new account-validation integration cases and existing BCrypt, RBAC and lockout coverage.
- Frontend suite: **34 passed across 6 files** (`npm test -- --maxWorkers=1`); production build passed.
- Backend and frontend ran against local **obqa**. Real browser forms rejected invalid usernames and weak passwords before submission, and created valid Admin and Lecturer accounts that could log in.
- Live API/MySQL checks rejected invalid and duplicate edits without changing stored email, password or role. An email-only edit preserved the password. Invalid recovery passwords left the token usable; a valid 72-byte password reset succeeded through the browser and allowed login.
- Existing Admin, Lecturer and SuperAdmin credentials still worked and loaded their dashboards. Temporary accounts and reset tokens were removed; original accounts were preserved. Normal access/audit records were generated.
- No schema, local credential or environment changes are included. Generated frontend output was restored; local verification helpers and logs are excluded from the source commit.

Suggested commit: `feat(security): validate account details and password changes`

Stop here for the user's commit/push. Part 5 (Excel upload validation) has not started. Earlier sections below retain their historical verification results.

## Feature 3: login lockout — 2026-10-07

Completed the existing five-failure/15-minute lockout implementation. Database row locking now preserves concurrent failure counts, expired locks start a fresh attempt window, active locks cannot be extended by retries or cleared by a late successful-login reset, and infrastructure failures do not count as password failures. See [14-login-lockout.md](14-login-lockout.md).

Verification:

- Focused lockout, controller and BCrypt tests: **30 passed**.
- Full backend suite: **251 passed**, no failures/errors/skips, including eight lockout integration cases, all RBAC tests and BCrypt regressions.
- Frontend regression suite: **28 passed** (`npm test -- --maxWorkers=1`). No frontend source changes were required; the existing login form displays the backend lockout message.
- Updated backend started successfully against local **obqa**. No schema migration or credential configuration change was required.
- Live MySQL checks on a disposable lecturer account: five incorrect passwords persisted a lock, correct-password attempts returned 423, the pre-lock JWT was rejected, repeated requests preserved the deadline, and concurrent incorrect-password requests stopped at exactly five failures without server errors.
- Live browser checks: locked login displayed the lockout message without creating a session; after simulated expiry, login succeeded and cleared the failure state. Expiry was simulated by backdating only the disposable account's stored deadline by 16 minutes. The first test used SQL `NOW()`, which did not match JDBC timestamp handling; it was corrected to adjust the stored value directly. No application clock or production lockout duration was changed.
- Existing Admin, Lecturer and SuperAdmin accounts still signed in with unchanged passwords and loaded their dashboards without failed API requests or uncaught browser errors.
- All disposable accounts were deleted in cleanup. Original users, passwords, module assignments and application records were preserved; normal login/access/audit records were generated.
- `git diff --check` passed. Backend and frontend remain available on ports 8080 and 5173.

Suggested commit: `fix(security): make login lockout concurrency safe`

Part 4 (account input validation) has not started. Hand this feature back for the user's commit and push first.

## Feature 2: role-based access control — 2026-10-07

Implemented the approved role matrix and module ownership rules. See [13-rbac-implementation.md](13-rbac-implementation.md) for permissions, enforcement and commit scope.

Verification:

- Full backend suite: **242 passed**, no failures/errors/skips (`mvn -B test`), including **44 real-policy RBAC integration cases** and the existing BCrypt regression tests.
- Frontend: **28 passed across 5 files** (`npm test -- --maxWorkers=1`). The initial parallel run timed out while starting workers and ran no tests; the single-worker rerun passed. No test assertions were disabled.
- Production frontend build passed. Existing plugin deprecation and large-bundle warnings remain non-blocking.
- Live backend started on port 8080 with local MySQL **obqa**, health UP, Flyway V10 validated with no migration needed.
- Existing Admin, Lecturer and SuperAdmin credentials passed browser login. Each role's module list matched actual database assignments (2 visible modules per existing account). Module/LO reads, account-management restrictions, mapping/CQI review restrictions, profile and CQI access were verified over real HTTP.
- Workspace, profile, CQI and both existing modules' marks-workbench browser pages loaded for each role with no uncaught JavaScript errors or failed page API requests. Admin/Lecturer could not open SuperAdmin account-management routes. Changing the lecturer's localStorage role did not grant API privileges.
- `git diff --check` passed; no unmerged Git entries remained. Backend and frontend were left running on ports 8080 and 5173.
- Positive mutations and cross-module denial cases use isolated H2 transactions; live obqa verification preserves existing application records and assignments, apart from normal login/access logging.

No credential or environment files are included in the feature commit. Generated build output was restored; local verification scripts and results stay outside the intended source commit. Part 3 has not started.

Suggested commit: `feat(security): enforce role and module access controls`

## Feature 1: BCrypt password hashing — 2026-10-05

Account creation, self-service password change, recovery and test-account creation already used the configured BCrypt encoder. `UserService.updateLecturer` and `updateAdmin` were the two application password-write paths still saving raw replacement passwords. Both now encode before saving. Null/empty/whitespace update fields continue to mean “leave password unchanged.” Password-policy changes belong to feature 4.

Added `PasswordHashingTest`: real Spring MVC controllers, services, BCrypt encoder, authentication provider and security filters, with mocked repositories and side effects. It verifies password updates followed by successful login, old-password rejection, unchanged hashes for email-only edits, account creation, self-change, recovery, and independent salts for equal passwords. Storage is simulated; these are not live MySQL/browser end-to-end tests.

Verification completed:

- `mvn -B -Dtest=PasswordHashingTest test`: 11 passed, no failures/errors.
- `mvn -B "-Dtest=UserRestControllerTest,CustomUserDetailsServiceTest" test`: 13 passed, no failures/errors.
- Frontend `npm test`: 24 passed across 5 files. Existing Vite/plugin deprecation warnings did not fail the suite.
- `git diff --check`: passed.

No live account data was read or changed. Previously stored plaintext passwords, if any, are not converted automatically: use the existing authorized password-reset/update flow to replace affected credentials. Restart/redeploy the backend to use the fix.

Feature files to include in the commit:

- `Software-project-Backend/src/main/java/com/example/Software/project/Backend/Service/UserService.java`
- `Software-project-Backend/src/test/java/com/example/Software/project/Backend/RestController/PasswordHashingTest.java`
- `security/12-application-security-progress.md`

Suggested commit message: `fix(security): hash admin and lecturer password updates`

The frontend lockfile modification and document 11 predate this feature. They were left unchanged. No commit or push was performed, and feature 2 has not started.

## Expanded application verification — 2026-10-05

The user requested backend startup and broader conflict checks before feature 2. Feature 1 is already in commit `fa517e0`; the additional integration test and this evidence update are a follow-up change.

- Full existing backend suite: **196 tests passed**, no failures/errors/skips (`mvn -B test`). Includes application context, repository, reporting, marks pipeline, services and controller coverage.
- Added `PasswordHashingIntegrationTest`: **2 additional tests passed**, using a real HTTP server and isolated H2 database. Both admin and lecturer password updates persist hashes, allow login with the replacement password, reject the original password, and allow a protected module request with the returned JWT.
- The HTTP test client's initial failure on an expected 401 was fixed by using `JdkClientHttpRequestFactory`. A concurrent Maven startup/test compilation also caused a transient missing-class failure; sequential execution passed. Run Maven builds/tests sequentially against the same target directory.
- Frontend: **24 tests passed** across 5 files. Normal `npm run build` passed after preserving the inaccessible old generated `dist` directory as `.codex-tmp/dist-before-security-check.bak` and creating fresh build output. No frontend source/config change was necessary. Chunk-size and plugin deprecation warnings remain non-blocking.
- MySQL-backed backend started on **8080**, health **UP**, against the separate `obqa_security_verify_20261005` schema with `ddl-auto=validate`. All **10 Flyway migrations** completed. Runtime datasource override was process-local; the user's `.env` and local database configuration were not edited.
- Live API checks against that MySQL schema: synthetic admin/lecturer account creation and password updates, replacement-password login, old-password rejection, and confirmation of BCrypt storage without printing password/hash values.
- Headless Chromium with the real frontend/API proxy: successful login and dashboard loading for **Lecturer, Admin and SuperAdmin**, without uncaught JavaScript errors or failed API responses on these paths. Evidence screenshots and the local smoke script are under the ignored backend `target/` directory. No mocked browser API responses were used.
- Git whitespace check passed; no unmerged entries or conflict markers were found in application source.

### Existing environment issues and verification limits

The normal local `obqa` database has tables but no Flyway schema-history table; default startup fails before serving requests. This remains unresolved for that original database. It was not automatically baselined or reset, because its schema must first be compared with the migrations and backed up. The running verification instance uses a separate schema containing synthetic data, not the user's existing records. Successful fresh-database migrations do not prove a legacy-data upgrade is safe.

Use **http://localhost:5173/** in the browser. The existing backend CORS allowlist accepts that origin (preflight 200) and rejects `http://127.0.0.1:5173` (preflight 403). This explains the first browser timeout; retesting with the configured origin passed. The earlier recommendation to use the 127.0.0.1 frontend URL did not account for this restriction. No CORS policy was broadened during the BCrypt feature.

These results cover the automated suite and specified live login/dashboard/password flows. They are not exhaustive manual validation of every screen or a guarantee of no application defects. Feature 2 remains pending.

Follow-up files to commit: `PasswordHashingIntegrationTest.java` and this progress document. Suggested message: `test(security): verify password updates through live HTTP and persistence`.

## Testing with the user's obqa database — 2026-10-06

The user explicitly requested using their local `obqa` database. The earlier original-database startup limitation is now resolved, and the backend runs against `obqa` with the existing local datasource settings (no verification-database override).

Procedure and evidence:

- Saved a full local dump, including triggers/routines/events, before changes. Latest pre-upgrade backup: `Software-project-Backend/target/local-db-backups/obqa-before-upgrade-20261006-084857.sql`. Backups are gitignored and contain sensitive original data; keep them private and preserve them outside build-output cleanup if needed.
- Restored the previous backup into `obqa_upgrade_rehearsal_20261005` and rehearsed the upgrade there first, confirming the backup could be restored.
- Inspected the legacy schema and `progress_schema_history`: the old reporting migration was already applied. Compared reporting columns/types/nullability with the fully migrated reference schema before baselining.
- Added the missing baseline security elements (`audit_log`, `user.failed_login_attempts`, `user.locked_until`), recorded a Flyway baseline at V2, then applied V3-V10 normally. No existing migration files were modified or fake SQL migration records inserted.
- Flyway validation passed. The final expected column types/nullability matched the clean reference. All **27 original tables retained their row counts** immediately after reconciliation, on both rehearsal and `obqa`.
- Changed only the gitignored local `application-dev.properties` Hibernate setting from `update` to `validate`; the datasource remains `obqa`. Schema changes are now tracked by Flyway for this local database.
- Converted **3 legacy plaintext password values** to BCrypt in one transaction, preserving each user's existing password. No passwords or hashes were printed. Already-BCrypt values were not double-hashed; no plaintext login fallback was introduced.
- Started the backend on port 8080, health **UP**. All three original credentials passed real HTTP login and protected module API checks.
- Headless browser verification against `http://localhost:5173`: the existing **Admin, Lecturer and SuperAdmin** accounts all signed in with their unchanged passwords and loaded their dashboards with no uncaught JavaScript errors or failed dashboard API responses.

This was local database reconciliation and verification for feature 1, not implementation of feature 2. Existing records were retained; normal login/access audit records were generated by the tests. The prior isolated verification and rehearsal schemas were retained, but the running application uses `obqa`. Local helper scripts and dumps are under ignored `target/`; they are not application startup hooks and should not be rerun blindly.
