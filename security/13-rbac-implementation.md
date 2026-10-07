# Feature 2: role-based access control

## Implemented rules

| Action | Lecturer | Admin | SuperAdmin |
|---|---|---|---|
| Manage admin accounts | No | No | Yes |
| Manage lecturers, module assignments, modules and POs | No | Yes | Yes |
| LO, assessment, marks, module reports and exports | Assigned modules | All modules | All modules |
| Submit mappings and CQI plans | Assigned modules | All modules | All modules |
| Approve/reject mappings and review CQI | No | Yes | Yes |
| Permanent PO deletion, including the legacy endpoint | No | No | Yes |
| Own profile, password and inbox | Yes | Yes | Yes |

Unassigned modules are accessible only to Admin and SuperAdmin. Assign lecturers using the existing module editor or Manage Lecturers page. No existing assignments are changed automatically.

## Enforcement

- `AccessPolicy` applies method authorization to every current application controller endpoint. A coverage test fails if an application endpoint has no explicit policy annotation.
- JWT authentication loads current database authorities on each request. Removing an account or changing its role/assignments takes effect for existing tokens. Client-side role edits do not grant API permission.
- Authorization checks IDs in paths, queries, request bodies and Excel metadata. LO, mapping, mark and template IDs resolve to their owning module. Mixed-module requests and attempts to overwrite another module's template are denied.
- Module, mapping, statistics and CQI lists respect lecturer assignments. Complete student reports require access to all contributing modules; saved historical PO credits are checked too. Global PO reports remain administrator-only.
- Anonymous/invalid credentials receive HTTP 401; authenticated users denied by the policy receive HTTP 403. Existing input validation remains separate.
- SuperAdmin can create either an Admin or Lecturer as requested. A lecturer-creation request no longer accidentally creates an Admin. Submission/review identity fields are server-controlled.
- The development test-account helper requires SuperAdmin and the dev profile.
- Frontend route permissions include administrator access to lecturer workflows. Header links expose the module workspace and administration as appropriate; assignment hints describe the new rule.

## Verification

Final verification evidence is recorded in [12-application-security-progress.md](12-application-security-progress.md).

Backend regression tests use isolated H2 databases, real security filters and transactions. They exercise successful operations and denials, stale tokens, removed assignments, deleted accounts, mixed IDs, metadata, approval boundaries and historical student credits. Existing controller slice tests isolate validation; the RBAC integration tests use the real policy.

Live verification uses the local `obqa` database and existing accounts. Passwords and environment files are not included in this feature's commit. No schema migration is required for RBAC on the reconciled V10 schema.

Suggested commit: `feat(security): enforce role and module access controls`

Stop after committing and pushing this feature. Login lockout is the next separate work item.
