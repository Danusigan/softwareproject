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
