# Account input validation

Implemented 2026-10-08 as feature 4 of the agreed eight-feature sequence.

## Application rules

- New usernames: 3?64 ASCII letters, digits, dots, underscores or hyphens; the first character must be a letter or digit. Existing usernames are not revalidated during login or account edits.
- Email: required and validated using the model's Jakarta email constraints; maximum 254 characters. Creation and email edits check case-insensitive uniqueness. Database unique constraints remain the final safeguard against simultaneous duplicate writes, with a safe conflict message.
- New passwords: retain the existing minimum of 8 characters with an ASCII uppercase letter, lowercase letter and digit; additionally reject control characters and passwords exceeding BCrypt's 72 UTF-8 byte limit. Non-ASCII characters can occupy multiple bytes. Passwords are not trimmed.
- The password rules apply to account creation, administrator/lecturer password replacement, self-service password change, recovery and the development test-account helper. The helper's newly created account default now satisfies the rules; existing account credentials are unaffected.
- Account edits accept only email and password. Attempts to include a username, role, lock state or other unsupported property are rejected. Explicit null/blank email is rejected. A missing or blank optional replacement password retains the existing password, preserving the previous edit-form behavior.
- All proposed email/password changes are validated before an account is modified. Existing BCrypt hashing, role authorization and lockout behavior remain covered by regression tests.
- Recovery validates the new password before consuming the token. Malformed recovery email requests return 400; valid unknown addresses retain the existing generic response.

The frontend account forms use matching username/password checks and visible password guidance. Profile password changes and recovery use the same frontend helper. Email fields enforce the maximum length. The lecturer form offers the Lecturer role; SuperAdmin manages administrators through the administrator page. Server validation remains authoritative for direct API requests.

## Verification

- `mvn -B test`: 276 passed, zero failures/errors/skips. The 25 new integration cases exercise real Spring security filters, controllers, services and H2 persistence with transaction isolation. Cases include invalid and duplicate details, partial-update prevention, 72-byte boundaries including multibyte passwords, unsupported update fields, optional blank passwords, recovery token preservation and legacy credential compatibility.
- `npm test -- --maxWorkers=1`: 34 passed across 6 files. Recovery tests verify rejected input does not call the API and accepted passwords are sent unchanged.
- `npm run build`: passed; existing non-blocking plugin deprecation and bundle-size warnings remain.
- Real browser and HTTP checks against local MySQL `obqa`: invalid username and weak-password form submissions blocked; valid Admin and Lecturer creation followed by successful login; invalid/duplicate edits returned 400 and preserved stored account data; email-only updates retained credentials; weak recovery retained its token and valid recovery accepted the 72-byte boundary.
- Existing Admin, Lecturer and SuperAdmin accounts logged in with unchanged passwords and loaded their dashboards without failed dashboard API requests or uncaught browser errors.
- Temporary accounts and the recovery token were deleted after verification. Existing application records were preserved, apart from normal access/audit logging. Test harness fixes addressed duplicate error-message locators and MySQL BIT output conversion; neither required weakening application validation.

Backend and frontend are available locally on ports 8080 and 5173. Use http://localhost:5173/ to match the existing CORS configuration. These results cover the automated suites and listed live flows, not exhaustive manual testing of every screen.

## Commit checkpoint

Include the backend validation/service/controller/repository changes and tests, frontend source changes and tests, this document and the progress document. Exclude local environment files, database backups, target helpers, logs and generated dist output.

Suggested message: `feat(security): validate account details and password changes`

Feature 5 has not started. Commit and push this feature before proceeding.
