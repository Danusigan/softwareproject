# 08 — Secure Design Review (Phase 4)

For each area: current design → required design → target decision to implement in Phase 5. Decisions marked **(user decision)** were confirmed directly; everything else follows from the findings in `04-security-requirements.md` and `07-threat-model.md`.

## Authentication mechanism

- **Current**: username/password via `POST /api/auth/login`, checked by `DaoAuthenticationProvider` + `CustomUserDetailsService`, issuing an HS256 JWT on success.
- **Required**: same shape (username/password → JWT) is appropriate for this app's size — no need for OAuth/SSO complexity. The mechanism itself is fine; what's broken is the password check underneath it (see next section) and the lack of throttling.
- **Target decision**: keep the login mechanism as-is; fix password verification and add lockout (below).

## Password management

- **Current**: `NoOpPasswordEncoder` — passwords stored and compared in plaintext.
- **Required**: passwords hashed with BCrypt (Spring Security's `BCryptPasswordEncoder`, default strength 10).
- **Target decision (user decision)**: minimum 8 characters, must include mixed case + at least one number. Enforced at registration/user-creation time (`UserRestController.addAdmin/addLecture/addUser`) via Bean Validation, not just client-side. Existing seeded test accounts will need re-creation after the encoder switches (plaintext values won't match a BCrypt hash).

## Session / JWT management

- **Current**: HS256, 2-hour expiry, no refresh token, no revocation/blacklist, authorities not `ROLE_`-prefixed.
- **Required**: bounded lifetime (have it), tamper-proof (have it via signature). Missing: revocation on logout, and any real distinction between short-lived access and longer-lived refresh.
- **Target decision (user decision)**: keep 2-hour expiry — no change needed for this app's usage pattern. Do **not** implement refresh tokens or a revocation list in this pass (adds real complexity — stateful blacklist or short-token/refresh-token pair — disproportionate to this app's risk level and academic-project scope). Document as an accepted residual risk (Medium — see threat model risk #8) rather than building it now. Do fix the `ROLE_` prefix issue so any future `hasRole(...)` usage isn't silently broken.

## RBAC design

- **Current**: `SecurityConfig` only checks `.authenticated()`; `User.isAdmin()/isLecturer()/isSuperAdmin()` exist but are only invoked manually inside `UserRestController`. The other 8 live controllers have zero role checks. Several endpoints are named `/admin/...` in the path with no enforcement behind the name.
- **Required**: every endpoint that should be role-restricted must enforce it server-side, consistently, not per-controller ad hoc.
- **Target decision**: use `@PreAuthorize("hasAuthority('ADMIN')")` / `hasAuthority('SUPERADMIN')` / `hasAuthority('LECTURE')` (matching the raw, unprefixed authority already granted by `CustomUserDetailsService` — simpler than re-prefixing everything with `ROLE_` and switching to `hasRole`), applied per-endpoint in Phase 5 based on the mapping already implied by existing manual checks and endpoint naming (e.g., anything under `/admin/...` paths → SuperAdmin or Admin only; mapping approval → Admin/SuperAdmin; marks upload → Lecture/Admin/SuperAdmin owning that module). Object-level ownership checks (a Lecturer can only touch their own module's data) are a second pass after role-level checks land — larger change, sequence after basic RBAC is in place.

## Input validation strategy

- **Current**: no `spring-boot-starter-validation` dependency; no `@Valid`/Bean Validation annotations found on request DTOs/entities used as `@RequestBody`.
- **Required**: validate all external input — required fields, string length limits, email format, enum-constrained fields (e.g., `usertype`) — at the controller boundary, returning 400 with a clear message rather than letting bad data reach the service/DB layer.
- **Target decision**: add `spring-boot-starter-validation`; annotate `User` and other `@RequestBody` types with `@NotBlank`, `@Email`, `@Size`, etc.; add `@Valid` to controller method parameters.

## Output encoding strategy

- **Current**: no `dangerouslySetInnerHTML` or raw `innerHTML` usage found in the frontend during Phase 1/2 exploration — React's default JSX escaping is relied on and appears intact.
- **Required**: keep relying on React's default escaping; explicitly forbid raw HTML injection points going forward.
- **Target decision**: no code change needed now; Phase 6 re-confirms this grep is still clean after Phase 5 changes (a Phase 5 change touching rendering could reintroduce risk).

## Error handling strategy

- **Current**: ad hoc per controller — mostly `Map.of("message", ..., "status", "ERROR")` with varying HTTP status codes; some generic exception messages potentially leak internals (e.g., `"Failed to add admin: " + e.getMessage()` returns raw exception text to the client).
- **Required**: consistent error shape across the API; internal exception details (stack traces, raw driver/DB error text) never returned to the client — log them server-side, return a generic message + code to the caller.
- **Target decision**: introduce a single `@ControllerAdvice`/`@ExceptionHandler` for uncaught exceptions returning a generic `{status: "ERROR", message: "..."}`without `e.getMessage()` passthrough; keep existing specific handled cases but strip raw exception text from client-facing messages.

## Logging strategy

- **Current**: DEBUG-level Hibernate SQL, Spring Security, and Hikari logging always on (not profile-gated) — verbose, could leak query parameter values in logs. No audit log of authentication events or privileged actions at all.
- **Required**: DEBUG-level framework logging restricted to local dev only; a dedicated audit log (separate from framework debug logs) recording who did what and when for login attempts (success/failure) and privileged actions (mapping approval/rejection, marks edits, PO changes, admin/lecturer creation).
- **Target decision (user decision on retention)**: add an `AuditLog` entity/table (actor, action, target, timestamp, outcome) written to on login and on each privileged write endpoint once RBAC lands; retain audit records for **90 days** by default (reasonable for an academic-project deployment; easy to extend later if a real retention policy is needed). Gate DEBUG SQL/security logging behind the `dev` profile only — default/`prod` profile logs at `INFO` or above.

## File upload security design

- **Current**: 4+ Excel upload endpoints (`ExcelImportService`, Apache POI) with no content-type check, no file extension allowlist, no max size enforced before parsing.
- **Required**: reject non-`.xlsx` files by content-type and extension before handing to POI; cap upload size (e.g., a few MB is more than enough for a marks spreadsheet) via `spring.servlet.multipart.max-file-size`/`max-request-size` plus an explicit check in the controller/service.
- **Target decision**: set multipart size limits in `application.properties`; add a small validation helper (extension + magic-byte/content-type check) called at the start of every upload endpoint before POI parses the stream.

## API security design

- **Current**: no versioning (`/api/v1/...`), no rate limiting anywhere, CORS scoped correctly to `http://localhost:5173` for dev.
- **Required**: rate limiting at minimum on `/api/auth/login` (per threat model risk #5); versioning is a nice-to-have, not urgent for a system with one frontend consumer — skip for now, don't over-engineer.
- **Target decision (user decision on lockout)**: implement login throttling as **account lockout after 5 consecutive failed attempts** (temporary lockout window, e.g. 15 minutes, tracked via a failed-attempt counter + timestamp on the `User` row or a small companion table) rather than a generic IP-based rate limiter — simpler to reason about and matches what was actually asked for. No API versioning work in this pass.

## Database security design

- **Current**: single `appuser`/`root`-level DB credential used for all application access; `ddl-auto=update`; DB password hardcoded in properties files.
- **Required**: application connects with a least-privilege DB user (read/write on application tables only, no DDL/admin grants); schema changes go through a reviewed path rather than silent auto-update in any environment beyond local dev; credentials externalized to environment variables (already mostly true for the Docker path — `docker-compose.yml` already uses `${DB_USERNAME}`/`${DB_PASSWORD}` — the gap is `application.properties`/`application-dev.properties` still having a literal fallback).
- **Target decision**: remove the hardcoded literal DB password from `application.properties`/`application-dev.properties`, requiring it to come from an environment variable with no insecure default; keep `ddl-auto=update` for local `dev` profile (acceptable for an active-development academic project) but note it as a residual, accepted risk rather than building a full Flyway/Liquibase migration pipeline in this pass (would be disproportionate effort for the project's current stage — revisit if it moves toward a real production deployment).

## Backup strategy

- **Current**: none. `mysql_data` is a Docker named volume with no backup/export process.
- **Required**: some minimal, documented way to export/restore the database — doesn't need to be sophisticated for an academic project, but "no backup at all" is a real availability risk given `ddl-auto=update` can already cause accidental schema drift.
- **Target decision**: document a simple manual backup procedure (`mysqldump` on demand before risky changes) in the deployment docs; no automated scheduled backup system — disproportionate to this project's current scale and hosting (local Docker only, not a live production service with real users yet).

## Encryption strategy

- **In transit (HTTPS)**: **(user decision)** skip for now — the app is only tested locally, not exposed on the public internet. Documented here as a **required item before any real/public deployment** (self-signed cert is fine for further local testing if ever needed; a real cert, e.g. via Let's Encrypt, is required once there's a real domain).
- **At rest (DB encryption)**: not currently configured (MySQL's transparent data encryption / disk-level encryption not enabled). Given passwords will move to BCrypt in Phase 5 (the highest-value at-rest protection for the most sensitive column), and the DB currently runs in a local Docker volume rather than a shared/exposed host, full disk/TDE encryption is not prioritized in this pass — revisit if/when this moves to a shared or cloud-hosted database.
- **Target decision**: no encryption-at-rest work in Phase 5; HTTPS requirement documented and deferred until real deployment; BCrypt password hashing (already planned) is the concrete at-rest improvement landing in this program.

## Summary of target decisions carried into Phase 5

| Area | Decision |
|---|---|
| Password hashing | BCrypt, policy: min 8 chars + mixed case + number |
| JWT lifetime | Unchanged, 2 hours |
| JWT revocation/refresh | Not implemented — accepted residual risk |
| RBAC | `@PreAuthorize(hasAuthority(...))` per endpoint, no `ROLE_` re-prefix needed |
| Object-level ownership checks | Second pass, after role-level RBAC lands |
| Input validation | `spring-boot-starter-validation` + `@Valid` on request DTOs |
| Output encoding | No change — re-verify in Phase 6 |
| Error handling | Global `@ControllerAdvice`, strip raw exception text from responses |
| Logging / audit | New `AuditLog` table for auth + privileged actions, 90-day retention; DEBUG SQL logging gated to `dev` profile only |
| File upload | Extension + content-type allowlist + max size, enforced before POI parsing |
| Rate limiting | Account lockout after 5 failed logins (not generic IP rate limiting) |
| API versioning | Not implemented — out of scope |
| DB credentials | Remove hardcoded fallback password from properties files, env-var only |
| DB least privilege | Out of scope this pass — accepted residual risk |
| Schema migrations | Keep `ddl-auto=update` for dev — accepted residual risk |
| Backups | Manual `mysqldump` procedure documented, no automation |
| HTTPS | Deferred until real/public deployment |
| DB encryption at rest | Not implemented this pass — BCrypt is the concrete at-rest win |
