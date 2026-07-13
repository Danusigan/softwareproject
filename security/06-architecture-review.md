# 06 — Security Architecture Review (Phase 2)

## Overall system architecture

```mermaid
flowchart LR
    Browser["Browser (React SPA)"] -->|"http://localhost:8080 (hardcoded)"| Backend["Spring Boot API :8080"]
    Browser -.->|"served from"| Nginx["nginx (frontend container) :80"]
    Nginx -->|"/api/* proxy_pass (unused by app)"| Backend
    Backend -->|JDBC| DB[("MySQL 8.0 :3306")]
```

The frontend is built and served by nginx in the Docker Compose deployment, and `nginx.conf` does define an `/api/` reverse proxy to `http://backend:8080/`. However, the React code itself never calls a relative `/api/...` path — every service/page hardcodes `http://localhost:8080` directly (confirmed across `marksService.js`, `cqiService.js`, and most page components). **The nginx proxy is dead configuration**: in the containerized deployment, the browser talks to the backend container directly on port 8080 (exposed via `docker-compose.yml`), bypassing nginx entirely for API calls. This only works because both ports happen to be published to the host; it would break under any deployment where the backend isn't also directly internet-facing. This is an architectural inconsistency to resolve before any real deployment — either wire the frontend to use relative `/api/` paths (matching the nginx config that's already there), or remove the unused proxy block.

## Frontend architecture

- Routing: `react-router-dom` v6, all routes declared in `App.jsx`. Public: `/`, `/loginpage`, `/forgottenpassword`. Everything else wrapped in `<ProtectedRoute>`.
- `ProtectedRoute.jsx`: checks `authService.isLoggedIn()` and, if `requiredRole` is passed, compares it (case-insensitively) against `userType` read from `localStorage`. **This is a pure client-side gate** — it decides what to *render*, not what the server will *allow*. Several routes (`/modules`, `/marks-workbench/:moduleId`, `/lo-po-mappings`, etc.) pass no `requiredRole` at all, so any authenticated user of any role can navigate to them; the real access control has to come from the backend, which (per `04-security-requirements.md`, REQ-AC-01) currently doesn't enforce any.
- `axiosSetup.js`: a global request interceptor attaches `Authorization: Bearer <token>` to every axios call; a response interceptor logs out and redirects on 401. This is well-structured, but several pages bypass it by reading `localStorage.getItem('token')` and building headers manually per-call — a maintenance/consistency risk (a call that forgets the header silently becomes unauthenticated rather than failing loudly).
- Token storage: JWT + role + username in `localStorage` (keys: `token`, `username`, `userType`, `isLoggedIn`, `tokenExpiry`, `rememberMe`). Client-side `tokenExpiry` is just a locally-computed timestamp, not cryptographic — it's a UX nicety, not a control.

## Backend architecture

Layered structure under `com.example.Software.project.Backend`:
- `Model/` — JPA entities.
- `Repository/` — Spring Data JPA interfaces.
- `Service/` — business logic, Excel import/export, user management.
- `RestController/` — 15 classes; 9 have live `@RequestMapping`s, 6 are empty stubs with no mapping at all (`AdminMappingRestController`, `AnalysisRestController`, `AttainmentRestController`, `MappingRestController`, `LosPosRestController`, `ReportsRestController`) — dead code, candidates for deletion or implementation in Phase 6.
- `Security/` — `SecurityConfig`, `JwtUtil`, `JwtRequestFilter`.

The layering itself is conventional and reasonable. The architectural gap isn't the layering — it's that **authorization logic exists on the `User` model (`isAdmin()`, `isLecturer()`, `isSuperAdmin()`) but nothing in the architecture ever calls it**. Individual controllers (`UserRestController`) reimplement their own ad-hoc role checks by manually decoding the JWT's `role` claim from the raw `Authorization` header string (see `isSuperAdmin(String token)` / `isAdmin(String token)` private helpers) — duplicated per-controller rather than centralized. This is fragile: it only exists in `UserRestController`, not in any of the other 8 live controllers, which have zero role checks at all.

## Database architecture

- Schema: `User`, `Student`, `Module`, `Los`, `StudentMark`, `CqiAction`, `ProgramOutcome`, `LosPos`/mapping tables, plus `assessment_item`/`assessment_template`/`student_assessment_score` (seen in the live schema, not previously enumerated in Phase 1 — worth reconciling with `02-asset-inventory.md`).
- `User` table: PK `User_ID` (string, essentially a username), `email` (unique), `password` (plaintext today), `user_type`. No `created_at`/`updated_at`/audit columns anywhere.
- Managed entirely via Hibernate `ddl-auto=update` — no Flyway/Liquibase migration history. Fine for a single-developer academic project's current stage, but means there's no reviewable/rollback-able schema change log, and `update` mode can silently leave stale columns behind after a model change.
- DB access uses a single application-level MySQL user (`appuser` in the Docker setup; `root` in the checked-in local dev config) — no separation between, e.g., a read-only reporting role and the read-write application role.

## API architecture

9 live controllers under `/api/**` (`/api/auth`, `/api/modules`, `/api/obe`, `/api/obe/assessment`, `/api/cqi`, `/api/los-with-mapping`, `/api/lo-po-mapping`, `/api/program-outcomes`, `/api/lospos`). No API versioning (`/api/v1/...`) — any breaking change to a live endpoint has no migration path for existing frontend builds. No consistent error-response shape enforced across controllers (some return `Map.of("message", ..., "status", ...)`, ad hoc per method).

## Authentication architecture

End-to-end flow: `POST /api/auth/login` → `AuthenticationManager` → `DaoAuthenticationProvider` (uses `CustomUserDetailsService` + the configured `PasswordEncoder`, currently `NoOpPasswordEncoder`) → on success, `JwtUtil.generateToken(username, role)` issues an HS256 JWT with a `role` claim and 2-hour expiry → returned to client, stored in `localStorage`.

On subsequent requests, `JwtRequestFilter` (a `OncePerRequestFilter`) reads the `Authorization` header, extracts the username via `JwtUtil.extractUsername`, loads the user via `CustomUserDetailsService`, and — if `validateToken` passes (username match + not expired; signature already implicitly validated by `Jwts.parserBuilder().setSigningKey(...)` throwing on tamper) — populates `SecurityContextHolder` with a `UsernamePasswordAuthenticationToken` carrying the user's single authority (their raw `usertype` string, unprefixed).

Two architectural notes:
- `JwtRequestFilter` only special-cases `/api/auth/login` for "skip JWT parsing" (`requestPath.equals("/api/auth/login")`); it still attempts JWT parsing on every other request including the other permitAll auth endpoints, which is harmless but slightly inconsistent with intent.
- No refresh token, no revocation/blacklist — a leaked or stolen JWT is valid for the full 2 hours no matter what (e.g., logging out client-side does not invalidate the token server-side).

## Authorization architecture

This is the single largest architectural gap in the system: **authentication and authorization are architecturally decoupled from each other**, but only authentication was actually wired up. `SecurityConfig.securityFilterChain` has exactly one rule beyond the public auth endpoints: `.anyRequest().authenticated()`. There is no `@PreAuthorize`, no `hasAuthority(...)`/`hasRole(...)` in the filter chain, and no method-security annotations anywhere in the 9 live controllers. The role-check logic that exists (`User.isAdmin()`, `isSuperAdmin()`, `isLecturer()`, and `UserRestController`'s private `isAdmin(token)`/`isSuperAdmin(token)` helpers) is only ever invoked manually, and only inside `UserRestController` itself — the other 8 controllers (module management, OBE/marks, CQI, LO/PO mapping, program outcomes) have **no authorization checks of any kind**, meaning any authenticated user (regardless of role) can call any of their endpoints, including create/update/delete operations. Combined with `SimpleGrantedAuthority` authorities not being `ROLE_`-prefixed, even a future move to `hasRole("ADMIN")` would silently fail to match (Spring's `hasRole` expects/adds a `ROLE_` prefix; `hasAuthority` would be needed instead, or authorities must be re-prefixed).

## Deployment architecture

- `docker-compose.yml`: 3 services (`mysql`, `backend`, `frontend`), custom bridge network, named volume for MySQL data. All three now healthy per Phase 1 verification.
- CI/CD: `.github/workflows/ci-cd.yml` ("CI/CD - Enterprise Grade") runs on push/PR to `main`/`develop`: CodeQL analysis, backend build+test (against an ephemeral MySQL service container), frontend build+test. **CodeQL is configured for `javascript` only — the Java backend is not covered by CodeQL at all**, a gap worth flagging for Phase 6. `.github/workflows/ci.yml` is an explicitly deprecated placeholder pointing at `ci-cd.yml`. Contrary to the initial Phase 1 note, **this pipeline does not currently build or push Docker images to `ghcr.io`** — it only tests; no image-publishing job exists in the current workflow.
- `sonar-project.properties` exists for SonarQube but isn't invoked from either workflow file — static analysis coverage for the backend currently has no active tool wired into CI.

## Trust boundaries

```mermaid
flowchart TB
    subgraph Untrusted
        B["Browser / attacker-controlled client"]
    end
    subgraph "Trust boundary 1: network edge"
        N["nginx :80 (mostly bypassed — see Overall architecture)"]
    end
    subgraph "Trust boundary 2: JWT crossing"
        API["Spring Boot API :8080<br/>JwtRequestFilter validates signature + expiry<br/>NO role/object-level check beyond .authenticated()"]
    end
    subgraph "Trust boundary 3: data store"
        DB[("MySQL — app has full read/write via single DB user")]
    end
    B -->|"1"| N
    B -->|"direct, bypassing nginx"| API
    N --> API
    API -->|"2: JWT is sole identity proof"| API
    API -->|"3"| DB
```

The critical observation: trust boundary 2 (JWT validation) currently only proves *who* the caller is, not *what they're allowed to do*. Every live controller beyond `UserRestController` treats "authenticated" as sufficient for full read/write access to its resources. This is the primary input to Phase 3's threat model and Phase 5's RBAC implementation work.
