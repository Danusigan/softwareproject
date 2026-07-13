# OBQA Security Program — Master Plan (Phases 1–6)

Status: **Phase 2 complete, Phase 3 next**. This document is the living index for the whole security program; each phase updates its status here as it completes.

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
| 3 | Threat Modeling | `07-threat-model.md` | Pending |
| 4 | Secure Design Review | `08-secure-design-review.md` | Pending |
| 5 | Security Implementation | Code changes (RBAC, BCrypt, validation, headers, rate limiting, audit log, secrets externalized) | Pending |
| 6 | Code Security Review | `09-code-security-review.md` | Pending |

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
