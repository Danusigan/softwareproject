# OBQA Security Program — Master Plan (Phases 1–6)

Status: **All 6 phases complete.** Program moves to Phase 7+ (active testing) if/when you choose to proceed. This document is the living index for the whole security program; each phase updates its status here as it completes.

## Goal

Turn OBQA into a professionally secured web application and produce documented proof of that — structured as an OWASP Top 10 / ASVS-aligned assessment report suitable for an academic/accreditation deliverable — before any active penetration testing begins. Phases 1–6 cover planning, architecture review, threat modeling, secure design review, implementation, and code security review. Active testing (Phase 7+, e.g. OWASP ZAP scans, manual exploit attempts) follows once Phase 6 closes out remediation.

## Seed findings (from initial codebase exploration, confirmed in code)

| Area | Finding | File |
|---|---|---|
| AuthZ | No `@PreAuthorize`/role checks anywhere in 15 REST controllers; `SecurityConfig` only requires `.authenticated()` | `Software-project-Backend/src/main/java/.../Security/SecurityConfig.java` |
| Password storage | `NoOpPasswordEncoder` (plaintext) | `Security/SecurityConfig.java:78-80` |
| Secrets | DB password + `jwt.secret` hardcoded in properties files; dev file gitignored now but has prior commit history | `application.properties`, `application-dev.properties` |
| Client auth | JWT in `localStorage`; role gate is a client-side string match only | `softwareproject_frontend/src/components/ProtectedRoute.jsx` |
| File upload | Excel bulk import has no type/size validation | `Service/ExcelImportService.java` |
| Config | `ddl-auto=update` in prod, DEBUG logging of SQL/security, no `spring-boot-starter-validation`, several empty stub controllers | `application.properties` |
| Frontend config | API base URL hardcoded `localhost:8080` in ~20 files instead of env-driven | various pages/services |
| Dead code | `softwareproject_frontend/src/src/` duplicate tree is unreferenced (confirmed: `main.jsx` only imports top-level `App.jsx`) | — |
| Roles | Only `superadmin`, `admin`, `lecture` exist in `User.usertype`; no "Student" role/login anywhere despite `Student` entity existing | `Model/User.java` |

## Responsibility split (applies to every phase)

- **Claude does** — reading code, writing docs/diagrams, editing source, running local commands (`mvn`, `npm`, `docker compose`, dependency scanners) and reporting results.
- **You do externally** — human decisions, credentials only you should hold, or actions outside this repo: approving destructive git-history operations, choosing production hosting/domain, storing generated secrets in a password manager, manually exercising the UI for report screenshots, and signing off at phase checkpoints.

## Phase index

| # | Phase | Deliverable | Status |
|---|---|---|---|
| 1 | Planning & Preparation | `01-system-review.md`, `02-asset-inventory.md`, `03-security-objectives.md`, `04-security-requirements.md`, `05-scope-and-rules-of-engagement.md` + working Docker Compose test environment | **Complete** |
| 2 | Security Architecture Review | `06-architecture-review.md` | **Complete** |
| 3 | Threat Modeling | `07-threat-model.md` | **Complete** |
| 4 | Secure Design Review | `08-secure-design-review.md` | **Complete** |
| 5 | Security Implementation | Code changes (RBAC, BCrypt, validation, headers, rate limiting, audit log, secrets externalized) | **Complete** |
| 6 | Code Security Review | `09-code-security-review.md` | **Complete** |

## Phase 5 progress log

Fixed so far: BCrypt password hashing (+ re-seed), RBAC gap closure (`debug/user`, `create-test-user`, PO catalog — see correction note in `06-architecture-review.md`), secrets externalized to env vars (DB password + JWT secret no longer hardcoded), login lockout (5 failed attempts → 15 min lock), Excel file upload validation (extension/content-type/size, 5MB cap), and a global exception handler (prevents raw exception/stack-trace leakage and a pre-existing bug where uncaught exceptions produced a confusing opaque 403 instead of a real error).

### Major non-security bug found and fixed: `OBEController` was completely unreachable

While testing file upload validation, discovered that `OBEController` (`/api/obe/**` — 23+ endpoints: all Program Outcome CRUD under `/po/*`, all marks upload/export, attainment calculation, trend analysis, reports) had the wrong package declaration: `package com.example.Software.project.BackendRestController;` instead of `com.example.Software.project.Backend.RestController;` (missing dot — a sibling package, not a sub-package of the main app's `com.example.Software.project.Backend`). Spring Boot's default component scan only covers sub-packages of the main application class's package, so **this controller was never registered as a Spring bean and none of its endpoints were ever reachable** — every request to `/api/obe/**` silently 404'd (masked as a confusing error until the exception handler fix above made it visible). This is a pure functional bug, not introduced by this security work, and predates it. Fixed by correcting the package declaration; verified `/api/obe/po/all` and the marks upload endpoints now respond correctly.

**Practical implication:** this also means the Phase 2/3 analysis of `OBEController`'s authorization (which assumed its manual `isAdmin`/`isLecture` checks were live, reachable security controls) was analyzing code that couldn't actually be hit over HTTP. Now that it's reachable, those same manual checks are real and active — re-verified working correctly in this session's testing (e.g., `isLecture` gate on marks upload).

Also added: audit logging (`AuditLog` entity/table, `AuditLogService`, 90-day retention via a daily `@Scheduled` purge) wired into login success/failure, account-lockout events, user creation (`add-admin`/`add-lecture`), Program Outcome create/update/delete/permanent-delete, and LO/PO mapping approve/reject — verified end-to-end.

### Security headers + CORS review

Backend response headers were already reasonable by default — Spring Security's `HeaderWriterFilter` adds `X-Content-Type-Options: nosniff` and `X-Frame-Options: DENY` automatically with no explicit config needed; verified via a live response. CSP/HSTS weren't added to the API itself (CSP is meaningful for HTML-rendering contexts — the frontend's nginx config already sets one; HSTS needs HTTPS, deferred per Phase 4). No further backend header changes made.

**CORS: found and fixed another real, pre-existing bug.** `SecurityConfig`'s CORS bean only allowed origin `http://localhost:5173` (the Vite dev server) — but the actual Dockerized frontend is served from `http://localhost` (port 80). Verified via a simulated preflight: a browser loading the Dockerized frontend and calling the backend would get a hard CORS rejection on every API call, making the containerized deployment unusable from a real browser (curl-based testing throughout this whole session never hit this, since curl doesn't enforce CORS). Additionally, **every one of the 8 REST controllers had its own identical `@CrossOrigin(origins = "http://localhost:5173", ...)` annotation**, which overrides the global CORS bean per-controller — so fixing only `SecurityConfig` would not have been sufficient. Fixed by adding `http://localhost` to the global CORS origins list and removing all 8 redundant per-controller `@CrossOrigin` annotations (consolidating on the single global source, closing the Phase 2-flagged duplication risk in the same pass). Verified preflight requests now succeed from both origins.

### Input validation

Added `spring-boot-starter-validation`. `User.username`/`email` get `@NotBlank`/`@Email`; `@Valid` added to `add-admin`/`add-lecture`/`add-user` (deliberately **not** to login — a password-policy check must never reject an existing, previously-valid credential at sign-in time). Password strength (min 8 chars, upper+lower+digit, per the Phase 4 decision) is enforced explicitly in `UserService.addUser()` against the raw password before hashing, rather than as a Bean Validation annotation on the entity — the `password` field stores the BCrypt hash once persisted, so an entity-level `@Pattern` constraint would incorrectly re-validate the hash on every save. Verified: invalid email, blank email, and weak password all return clean 400s with field-specific messages; valid data still succeeds; login is unaffected.

### Regression caught and fixed during final re-verification

Adding the global exception handler's catch-all `@ExceptionHandler(Exception.class)` (added earlier in this phase) had an unintended side effect: it was catching `AccessDeniedException` — the exception `@PreAuthorize` denials throw — **before** Spring Security's own filter chain could translate it into a proper 403, producing a 500 instead. Caught this in the final full-stack re-verification pass (a lecturer token hitting the admin-only PO catalog got 500, not the expected 403). Fixed by adding explicit `AccessDeniedException` → 403 and `AuthenticationException` → 401 handlers ahead of the generic catch-all. Re-verified: all RBAC checks now return the correct status codes again.

Also fixed during re-verification: the `failed_login_attempts` column (added for login lockout) is `NOT NULL` with no DB-level default, which broke the seed script's raw SQL insert for the SuperAdmin test account (Java's `= 0` field initializer only applies when creating objects through JPA, not raw SQL). Fixed both the entity (`columnDefinition = "INT DEFAULT 0"`) and the seed script (explicit column in the INSERT).

### Final full-stack verification (clean reset)

Ran a complete `docker compose down -v && up` + re-seed + verification pass as the last step: all 3 role logins, RBAC (lecturer blocked from admin endpoints, admin blocked from superadmin-only permanent-delete, unauthenticated requests properly rejected), file upload validation, CORS from both real origins, login lockout (5 failures → 423 on the 6th), and audit log population (19 entries across LOGIN/CREATE_USER events) — all confirmed working together from a clean state, not just individually.

## Deferred to Phase 6 / not done in this pass

- Git history still contains the old exposed DB password — rotation vs. history rewrite is still your call (see Phase 1 notes).
- JWT revocation/refresh, DB least-privilege user, Flyway/Liquibase migrations, automated backups, encryption at rest, API versioning — all explicitly deferred per the Phase 4 design decisions as accepted residual risk, not oversights.
- CodeQL still doesn't cover the Java backend (CI/CD config change, outside application code).
- `.env.example`'s `DB_USERNAME` and the port-conflict/JDBC fixes from Phase 1 are already corrected in the repo; not revisited here.

## Phase 6 summary

Full detail in `security/09-code-security-review.md`. Headlines:

- Re-reviewed all Phase 5 security code (auth, authz, validation, error handling, file upload) — confirmed consistent, found one pre-existing low-severity edge case (`JwtRequestFilter` doesn't catch `UsernameNotFoundException` for a deleted user with a still-valid token — fails safe, just an ugly error instead of a clean 401).
- **`npm audit`**: found 13 frontend dependency vulnerabilities (5 high, 7 moderate) including in `axios` (SSRF, auth bypass) and `react-router` (open redirect) — both production dependencies. Fixed 11/13 via `npm audit fix`; verified the build still works. 2 remaining need a breaking Vite major-version upgrade (dev-server-only impact) — deliberately deferred to a dedicated, separately-tested task rather than force-upgrading during this pass.
- **Backend dependency scan**: OWASP Dependency-Check could not complete in this session (NVD database download got stuck in this environment, compounded by a stale lock from an interrupted attempt — cleaned up). Did a manual, appropriately-hedged review of pinned versions instead (Spring Boot 3.2.2 is dated; jjwt 0.11.5 and Apache POI 5.2.4 are functionally fine as used but could be bumped). Recommend running the automated scan yourself with more time or an NVD API key, or wiring it into CI.
- Secrets sweep: clean, no live secrets found. One stale doc (`POSTMAN_TESTING_GUIDE.md`) still shows the old hardcoded JWT secret as troubleshooting text — harmless now, just outdated.
- Per your instruction: the 6 empty stub controllers and the dead `src/src/` frontend tree were **not deleted**, only re-flagged for visibility.

## Phase 1 environment setup — done

Docker Desktop 29.6.1 installed by the user. Stack is up via `docker-compose.yml` (MySQL 8.0 + Spring Boot backend + nginx/React frontend), isolated from the user's real local MySQL (`LOPOmapping` on port 3306 — deliberately left untouched; the containerized MySQL uses host port 3307 instead, see below).

**Seeded test accounts** (via `security/seed-test-data.sh`, re-runnable after any `docker compose down -v && up`):
| Role | Username | Password |
|---|---|---|
| SuperAdmin | `superadmin_test` | `SuperAdminTest123!` |
| Admin | `admin` | `password123` |
| Lecturer | `lecturer_test` | `LecturerTest123!` |

All three confirmed logging in and receiving a valid JWT after a clean `docker compose down -v && up -d` + re-seed cycle.

### Real bugs found and fixed while standing up the environment

These aren't security findings — they're pre-existing operational bugs that blocked the environment from running at all. Fixed as minimal, targeted patches so Phase 1 could complete; **Phase 6 (Code Security Review) should re-verify these are still correct** once Phase 5 changes land:

1. **`.env.example`**: `DB_USERNAME=root` — the official MySQL image rejects `MYSQL_USER=root` (root must be configured via `MYSQL_ROOT_PASSWORD` only). Fixed locally to `appuser`; `.env.example` itself should be corrected too.
2. **`docker-compose.yml`**: MySQL host port hardcoded to `3306:3306`, which collided with the user's existing local MySQL. Changed to `${DB_HOST_PORT:-3306}:3306` (defaults unchanged for everyone else; this session uses `3307` via local `.env`).
3. **`docker-compose.yml`**: backend JDBC URL was missing `allowPublicKeyRetrieval=true`, which MySQL 8's default `caching_sha2_password` auth requires without SSL — backend couldn't connect to the DB at all. Fixed (matches what `application.properties`'s own local JDBC URL already had).
4. **`softwareproject_frontend/Dockerfile`**: switches to non-root `USER nginx` but never grants that user write access to `/var/cache/nginx`, `/var/log/nginx`, or the pid file — nginx crash-looped on every start. Fixed with a proper `chown`.
5. **`softwareproject_frontend/Dockerfile` + `docker-compose.yml`**: healthcheck used `wget ... http://localhost/health`; nginx only listens on IPv4, so `localhost` resolving to `::1` inside the container caused permanent "unhealthy" status even though the app was serving fine. Fixed to `127.0.0.1`. **Note:** `docker-compose.yml` has its own `healthcheck:` block per service that fully overrides the Dockerfile's `HEALTHCHECK` — both had to be fixed, not just one.
6. **`Software-project-Backend/pom.xml` + `SecurityConfig.java`**: the backend Dockerfile's healthcheck targeted `/actuator/health`, but `spring-boot-starter-actuator` was never added as a dependency — the endpoint didn't exist, so the request fell through to the "authenticate everything" rule and returned 403 forever. Added the dependency, permitted only `/actuator/health` in `SecurityConfig`, and set `management.endpoint.health.show-details=never` so no internal details leak to unauthenticated callers.
7. **`docker-compose.yml`**: backend healthcheck used `curl`, which isn't installed in the `eclipse-temurin:17-jre-alpine` runtime image at all. Switched to `wget` (available via busybox), consistent with the frontend.

### Notable finding surfaced along the way (feeds Phase 2/3/6)

`GET /api/auth/debug/user/{username}` is fully unauthenticated (falls under the `/api/auth/**` permitAll rule) and returns a user's email and role by username with no auth check — an information-disclosure / enumeration issue. Add to threat model and requirements; likely delete or lock down in Phase 5.

To bring the environment up again at any time: `docker compose up -d` (or `down -v && up -d` for a fully clean reset), then `sh security/seed-test-data.sh`.

## Sequencing notes

- Phases run 1 → 6 in order; each doc traces back to real findings, not generic boilerplate.
- Brief pause after Phases 3, 4, and 5 for external sign-off on judgment calls (risk priority, password/session policy, secret rotation & git-history decisions).
- See individual phase docs for full detail; this file stays a short index.
