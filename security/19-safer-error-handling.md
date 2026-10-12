# Feature 8: safer error handling

Controllers, reporting endpoints, authentication filters and the global MVC handler now share safe error classification. Library/database exception text is not used as a response message, including exceptions caught directly by older controllers.

## Behavior

| Failure | HTTP status |
|---|---|
| Invalid values, malformed JSON, missing fields/parameters, invalid Excel | 400 |
| Missing/invalid authentication | 401 |
| Denied permission | 403 |
| Missing endpoint or explicitly identified missing record | 404 |
| Wrong HTTP method (with Allow header) | 405 |
| Unsupported response format | 406 |
| Database constraint conflict | 409 |
| Upload too large | 413 |
| Unsupported request content type | 415 |
| Existing login lockout | 423 |
| Unexpected code/database/infrastructure failure | 500 |

Existing account duplicate checks remain input validation (400); unexpected database constraint violations map to 409. The existing login lockout and generic password-recovery responses remain intact. Forgot-password still returns its generic success response after an internal mail failure to avoid revealing account existence.

`PublicValidationException` is reserved for application-authored account/upload guidance; it must never wrap arbitrary exception text or secrets. Published messages are bounded and control characters removed. Other argument errors get generic guidance. `ResourceNotFoundException` distinguishes a missing business record from an unexpected failure without publishing its diagnostic text. Excel import services preserve validation exception types so bad uploads remain 400 and transactions still roll back.

Existing success responses and controller error envelopes remain compatible. JSON error maps add `status: ERROR`, `requestId` and `Cache-Control: no-store`; API requests use the server-generated audit correlation ID from `X-Request-ID`. Existing plain-text error routes keep their response type. Filter-generated authentication errors also include the ID and no-store policy. Requests outside the audit filter's `/api/` scope can show `requestId: unavailable`.

Unexpected failures are logged using exception class and request ID rather than exception messages, request values or stack traces. Password-recovery logs no longer include the submitted email or mail exception; notification and attainment-recalculation failures use the same safe logger. The MVC exception resolver's raw debug exception output and request/response body processor logs are suppressed. Operators must not enable verbose request/body/JDBC logging on a deployed service.

Spring Boot's fallback `/error` configuration excludes messages, exception names, binding details and stack traces, including requests with `?trace=true`. Security checks continue to protect routes; error handling does not broaden access.

## Verification and scope

`SafeErrorHandlingTest` injects SQL/token/path sentinels into unexpected errors, database failures, conflicts, invalid arguments, missing records, malformed JSON and controller-local catches. It checks response privacy, HTTP status, no-store behavior and request IDs, and exercises database failures in JWT lookup. Existing account, RBAC, lockout, upload rollback and audit regression suites remain required.

Live checks against local **obqa** exercise original logins, safe 400/401/403/404/405/415 responses, trace-query suppression, account/recovery flows, role denial, audit privacy, valid imports and rejected uploads. Internal database outages are injected in automated tests; the real database is not deliberately interrupted. Temporary test records are cleaned up; ordinary audit/access records remain.

See `12-application-security-progress.md` for final test counts and evidence. These eight application features do not establish that the whole system is vulnerability-free; historical credential rotation from feature 7 remains a deployment follow-up.

Suggested commit: `feat(security): return safe consistent API errors`
