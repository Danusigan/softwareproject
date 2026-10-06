# Application security feature progress

User-selected scope: implement these eight features one at a time. Test and fix each feature, then hand it back for the user's Git push before starting the next. This sequence takes precedence over the broader roadmap in document 11.

| Order | Feature | Status |
|---|---|---|
| 1 | BCrypt password hashing | Implemented and automated checks passed; ready for user push |
| 2 | Role-based access control | Pending |
| 3 | Login lockout | Pending |
| 4 | Account input validation | Pending |
| 5 | Excel upload validation | Pending |
| 6 | Audit logging | Pending |
| 7 | Environment-based secret protection | Pending |
| 8 | Safer error handling | Pending |

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
