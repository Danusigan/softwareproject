# 09 — Code Security Review (Phase 6)

Final pass over the Phase 5 changes plus a dependency/secrets sweep, before active testing (Phase 7+).

## Auth / authorization re-review

Re-read `SecurityConfig`, `GlobalExceptionHandler`, `JwtRequestFilter`, `JwtUtil`, `CustomUserDetailsService`, and `User` end to end. All Phase 5 target designs are correctly implemented and consistent with each other:

- BCrypt encoder wired through `AuthenticationProvider`; `CustomUserDetailsService` correctly reports `isAccountNonLocked` from `User.isCurrentlyLocked()`.
- `@EnableMethodSecurity` + `@PreAuthorize` on `debug/user`, PO catalog GETs, and permanent-delete are all active and consistent with `SecurityConfig`'s narrowed `permitAll` list.
- `GlobalExceptionHandler`'s `AccessDeniedException`/`AuthenticationException` handlers (added to fix the regression caught during Phase 5's final verification) correctly sit ahead of the generic catch-all — confirmed no other Spring Security exception type is at risk of being shadowed the same way (`LockedException` is caught locally in `UserRestController` before it would ever reach the global handler).

**One pre-existing, low-severity finding (not introduced by this work, not fixed in this pass):** `JwtRequestFilter.doFilterInternal` calls `userDetailsService.loadUserByUsername(username)` for a validly-signed, non-expired JWT without a try/catch. If the referenced user was deleted after the token was issued, `UsernameNotFoundException` propagates out of the filter chain uncaught — filters run outside Spring MVC dispatch, so `GlobalExceptionHandler` never sees it either. Net effect: a deleted-user's still-valid token causes an ugly unhandled exception/500 instead of a clean 401, on a request that should be rejected anyway. Fails safe (no unauthorized access granted) — cosmetic/robustness gap, not a vulnerability. Worth a follow-up try/catch around that call if revisited.

## Input validation / error handling / file upload re-review

Confirmed via re-reading (not just the Phase 5 test suite): `@Valid` is applied only to account-creation endpoints, never to login, exactly as designed — a password-policy tightening can never lock out an existing account at sign-in. `FileValidationService` is called before `ExcelImportService`/POI touches the stream in all 4 upload endpoints. `spring.servlet.multipart.max-file-size`/`max-request-size` match the validator's 5MB constant. No drift found between the two.

## Frontend re-review

No frontend code changed during Phase 5 (all fixes were backend-only), so this is a fresh confirmation rather than a diff review:
- No `dangerouslySetInnerHTML`, `innerHTML`, `eval(`, or `document.write` anywhere in `src/` — grep-clean.
- JWT + role still stored in `localStorage` across ~11 files; `ProtectedRoute` is still a client-side-only gate. Both remain accepted residual risk per the Phase 4 decision (server-side enforcement is the real control; client-side state was never meant to be trusted).
- `softwareproject_frontend/src/src/` dead tree still present, unreferenced. **Per your instruction, left in place rather than deleted** — flagging again here for visibility, not deleting.

## Dependency scan — frontend (`npm audit`)

Found 13 vulnerabilities (5 high, 7 moderate, 1 low), including in **direct, production-relevant dependencies**:
- `axios` — SSRF via `NO_PROXY` bypass, auth bypass via prototype pollution in `validateStatus`, JSON response tampering
- `react-router`/`react-router-dom` — open redirect via protocol-relative URL (`//host`)
- `form-data` (transitive, via axios) — CRLF injection

Ran `npm audit fix` (non-breaking): **resolved 11 of 13**. Verified `npm run build` still succeeds afterward — no regression.

**2 remaining, deliberately not force-fixed:** `esbuild`/`vite` — "esbuild enables any website to send requests to the dev server and read the response." This only affects the **Vite dev server** (`npm run dev`), not the production build served by nginx in Docker — the actual deployed app is unaffected. Fixing requires `npm audit fix --force`, which upgrades Vite 5 → 8 (three major versions), a breaking change with real risk of breaking the build/config without dedicated testing time. **Recommendation:** schedule this as a standalone task with time to test the upgrade, not bundle it into this security pass.

## Dependency scan — backend

**Automated OWASP Dependency-Check did not complete in this session.** It requires downloading the full NVD CVE database on first run, which got stuck in this environment (an NVD API rate-limit wait loop with no clear time bound, compounded by an orphaned process holding a stale lock file after an interrupted attempt — cleaned up, but the tool itself needs 10+ minutes minimum and ideally an NVD API key to run reliably). Recommend running `./mvnw org.owasp:dependency-check-maven:check` yourself with more time, or via CI where it can run unattended (see also: CodeQL currently doesn't cover the Java backend either — Phase 2 finding, still open).

**In lieu of that, a manual review of pinned versions** (informed judgment, not a verified CVE-database match — treat as a starting point, not a substitute for the automated scan):
- **Spring Boot 3.2.2** (parent, pins most versions) — released Jan 2024; by the current date this is a dated line with multiple patch/minor releases since. Recommend moving to the latest 3.2.x patch at minimum, or the current stable minor line, as routine maintenance.
- **jjwt 0.11.5** — functionally fine as used here (explicit `SignatureAlgorithm.HS256`, no "alg: none" exposure), but the 0.12.x line is current; no urgent risk, low-cost to upgrade when convenient.
- **Apache POI 5.2.4** — reasonable; the new `FileValidationService` size/extension checks (Phase 5) meaningfully reduce exposure to malicious-file-driven parsing issues regardless of POI's own patch level.
- **MySQL Connector/J** — version follows Spring Boot's dependency management; no action identified without the automated scan.

## Secrets sweep (final)

Grepped the full repo for AWS-style keys, PEM private key headers, GitHub tokens, and generic `password=`/`secret=` literal patterns: **clean** — no live secrets found in source. One stale documentation reference found: `Software-project-Backend/POSTMAN_TESTING_GUIDE.md:297` still shows the old hardcoded `jwt.secret` value as troubleshooting advice — harmless (not a real credential anymore, `application.properties` now requires `${JWT_SECRET}`), but the doc is out of date and would mislead anyone following it. Not fixed in this pass (docs cleanup, not a security control) — flagging for your awareness.

## Stub controllers

Per your instruction, the 6 empty stub controllers (`AdminMappingRestController`, `AnalysisRestController`, `AttainmentRestController`, `MappingRestController`, `LosPosRestController`, `ReportsRestController`) are **kept as-is**, not deleted — reserved for potential future implementation.

## Summary: what's left open going into Phase 7+

| Item | Status |
|---|---|
| Frontend dependency vulnerabilities | 11/13 fixed; 2 remaining need a deliberate, separately-tested Vite major upgrade |
| Backend dependency CVE scan | Not completed automatically this session — recommend running standalone with more time/an NVD API key |
| `JwtRequestFilter` deleted-user edge case | Low-severity, fails safe, not fixed |
| Stale JWT secret reference in Postman docs | Cosmetic, not fixed |
| Git history still contains old DB password | Your call (Phase 1/5 note) — rotate vs. rewrite history |
| JWT revocation/refresh, DB least-privilege, migrations, backups, encryption at rest, API versioning | Accepted residual risk per Phase 4 — not in scope for this program |
| CodeQL doesn't cover the Java backend | Open (Phase 2 finding, CI/CD config change) |
