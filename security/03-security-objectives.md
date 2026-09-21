# 03 — Security Objectives

Objectives are stated in CIA + Accountability terms, each tied to a concrete OBQA scenario and prioritized by how directly the current codebase already fails it (see `02-asset-inventory.md` for the underlying assets).

## Confidentiality

| Objective | Scenario | Priority |
|---|---|---|
| Student marks and PII must be readable only by the student's own module lecturer(s) and admins — never by other lecturers, other students, or unauthenticated requests. | A Lecturer for Module A should not be able to pull marks for Module B via `/api/obe/*`. | Critical |
| Credentials must never be recoverable in plaintext from the database or logs. | A DB dump today would expose every user's real password (`NoOpPasswordEncoder`). | Critical |
| Secrets (DB password, JWT signing key) must not be discoverable from source code or git history. | Both are currently hardcoded in `application.properties`. | Critical |
| JWTs must not be trivially stealable via client-side script injection. | Token lives in `localStorage`, readable by any injected JS. | High |

## Integrity

| Objective | Scenario | Priority |
|---|---|---|
| Marks, LO/PO mappings, and CQI approvals must be modifiable only by the roles authorized to do so. | Today, any authenticated user (Lecturer, Admin, or SuperAdmin) can call any endpoint — a Lecturer could approve their own mapping via `/api/lo-po-mapping` since no server-side RBAC exists. | Critical |
| Uploaded Excel files must be validated before being parsed/trusted. | `ExcelImportService` has no type/size checks today. | High |
| Database schema changes must go through a reviewed migration path, not silent auto-update. | `ddl-auto=update` is active even outside `dev` profile. | Medium |

## Availability

| Objective | Scenario | Priority |
|---|---|---|
| Bulk Excel upload must not allow a single file to exhaust backend memory/CPU. | No size limit currently enforced on the multipart upload endpoint. | High |
| Login endpoint must resist brute-force/credential-stuffing without degrading service for legitimate users. | No rate limiting or lockout exists on `/api/auth/login` today. | High |
| Core marks/reporting workflow must remain available through the accreditation reporting cycle (the primary business driver for this whole system). | Downtime during a reporting deadline undermines the accreditation submission itself. | Medium |

## Accountability

| Objective | Scenario | Priority |
|---|---|---|
| Every authentication event and privileged action (mapping approval, marks edit, CQI action change) must be attributable to a specific user and timestamp. | No audit log exists anywhere in the system today — this is a pre-existing tracked gap (project memory: international-standards gap #7), and also blocks Repudiation-category threats identified in Phase 3. | High |
| Security-relevant configuration (who can do what) must be explicit in code, not just implied by a model method that's never invoked. | `isAdmin()`/`isLecturer()`/`isSuperAdmin()` exist on `User.java` but are never called by the Spring Security filter chain. | Critical |

## How priorities map to phases

- **Critical** objectives (server-side RBAC, password hashing, secrets management) become the first items implemented in Phase 5, verified structurally in Phase 2 and threat-modeled explicitly in Phase 3.
- **High** objectives (file upload validation, rate limiting, audit logging, JWT storage) follow immediately after.
- **Medium** objectives (schema migration discipline, general availability) are addressed opportunistically during Phase 5/6 without blocking the Critical/High work.
