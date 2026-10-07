# Feature 3: login lockout

## Application behavior

- All three roles lock after five consecutive incorrect-password attempts for the same account. The fifth failure returns the usual HTTP 401; subsequent login attempts return HTTP 423 while the lock is active, including attempts with the correct password.
- The lock lasts 15 minutes. Further attempts do not extend its deadline or increase the counter beyond five.
- The first incorrect attempt after expiry starts a new window at one. A successful login resets the counter and clears an expired lock.
- Existing bearer tokens are rejected while the account is locked, preserving the behavior introduced in RBAC. This is a temporary account check, not permanent token revocation; an otherwise valid token can be used after the lock expires.
- Unknown usernames receive the same incorrect-credentials response and do not create account state. Infrastructure failures return a generic server error and do not count as password failures.
- The login page displays the backend lockout message and does not establish a session for a rejected login.

## Implementation

`UserRepository` reads scalar login state with `SELECT ... FOR UPDATE`. Transactional `UserService` methods update only the counter and deadline, serializing concurrent attempts without overwriting passwords, roles or email addresses. Scalar reads avoid reusing an older User entity from the authentication request's persistence context.

The successful-login reset checks the locked state again under the same row lock. An authentication that started before another request locked the account cannot clear that active lock.

`UserRestController` counts `BadCredentialsException` only. The previous broad exception handler also counted database and other server failures as bad passwords. Existing password hashing, permissions, lockout duration and attempt limit remain in place.

The existing `failed_login_attempts` and `locked_until` columns are used; no schema migration is needed on the reconciled local V10 database.

## Verification and handoff

See [12-application-security-progress.md](12-application-security-progress.md) for final automated and live verification results. Integration tests cover every role, expiry, successful reset, concurrent failures, active-lock preservation, unknown users and rejection of previously issued tokens. Controller tests cover infrastructure errors separately.

Suggested commit: `fix(security): make login lockout concurrency safe`

Account input validation is the next feature. Do not start it until this feature has been committed and pushed by the user.
