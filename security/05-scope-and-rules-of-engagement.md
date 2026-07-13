# 05 — Scope and Rules of Engagement

## In scope

- Local Docker Compose deployment (`docker-compose.yml`): MySQL container, Spring Boot backend container, nginx-served frontend container — all run on this machine, isolated from any real data.
- All 15 REST controllers under `/api/**` (9 live, 6 empty stubs — see `01-system-review.md`).
- JWT authentication flow end to end (issuance, validation, expiry).
- Excel bulk upload/download flow (`ExcelImportService`/`ExcelExportService`).
- Frontend route protection (`ProtectedRoute.jsx`) and client-side token handling.
- CI/CD configuration review (`.github/workflows/*.yml`) — **read-only analysis**, no live pipeline attacks.
- Source code and dependency review (backend `pom.xml`, frontend `package.json`).

## Out of scope

- Any production/publicly deployed instance — none currently exists (confirmed via `README.md`, which only documents local Docker/manual run).
- Third-party services (none integrated currently).
- Denial-of-service testing against shared/external infrastructure.
- Social engineering or phishing simulations.
- The dead `softwareproject_frontend/src/src/` duplicate tree — confirmed unreferenced by `main.jsx`; excluded from testing, flagged for deletion in Phase 6.
- GitHub-hosted CI runners/`ghcr.io` registry itself (only the *configuration* of the pipeline is reviewed, not the platforms).

## Test data policy

- All testing runs against **synthetic seed data only** (see Phase 1 test-environment setup): one SuperAdmin, one Admin, one Lecturer test account, plus dummy modules/marks — never real student or staff data.
- No production database exists yet, so this is a straightforward rule, not a live constraint — documented here so it remains the rule if/when a real deployment is created later.

## Planned test types (for Phase 7+, after Phase 6 remediation closes)

- Authenticated and unauthenticated REST API testing (curl/Postman — the repo already has Postman collections in `Software-project-Backend/` that can seed test cases).
- Automated scanning: OWASP ZAP baseline scan against the local Docker Compose stack.
- Manual authorization/RBAC bypass testing (cross-role and cross-object access attempts, per `REQ-AC-01`/`REQ-AC-02` in `04-security-requirements.md`).
- Static analysis: review existing CI CodeQL findings; run SonarQube against `sonar-project.properties`.
- Dependency/SCA scanning: `mvn dependency-check:check`, `npm audit`.

## Rules

1. All testing targets the local Docker Compose stack only, never any external system.
2. No destructive actions against shared git branches; all remediation work happens on feature branches, reviewed before merge to `main`.
3. Any git-history-rewriting operation (e.g., purging the historical DB password) requires explicit user approval before being run — it is destructive and out of scope for Claude to perform unilaterally.
4. Testing is entirely self-authorized: the project owner is testing their own system, no third-party authorization is required.
5. All secrets generated during remediation (new DB password, new JWT signing key) are handed off to the user to store in their own password manager — they are not to be the sole record-holder.

## Timeline / roles

- Tester: project owner, assisted by Claude Code for analysis, documentation, and implementation.
- Phase 1 (this phase): planning docs + environment setup — completed in this session.
- Phases 2–6: sequential, each producing one numbered doc under `security/`, with checkpoints after Phases 3, 4, and 5 for the user to confirm judgment calls (risk prioritization, password/session policy, secret rotation approach).
- Phase 7+ (active testing): scheduled after Phase 6 remediation closes out; not detailed further here.
