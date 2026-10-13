# Feature 7: environment-based secret protection

Database credentials, JWT signing keys and optional SMTP credentials belong to the backend process environment. No application password or database account was rotated during this change.

## Local Windows startup

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-backend.ps1
```

The current machine's existing local database/JWT values were moved privately from `application-dev.properties` to `.env.backend.local`. This file points at the existing **obqa** database. The separate root `.env` remains the Docker Compose configuration and is not loaded by this launcher.

On another machine, copy `.env.backend.example` to `.env.backend.local`, enter your local database credentials, and generate a JWT key using the example's command. Blank/example keys cannot start the application. The file format is literal `NAME=value`: do not add quotes, inline comments, or shell expressions. Values containing `$`, `#`, backticks or `=` are not executed or expanded. Shell environment values take precedence. Loaded variables are restored when the launcher exits. A `-EnvFile` argument selects another private file; `-CheckOnly` validates without starting Java or showing values.

For an IDE, configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET` in its private run environment. Maven/Java does **not** automatically load a root `.env`. Direct `mvn spring-boot:run` works when these variables are already present. Spring's `SPRING_DATASOURCE_*` variables are also supported by the application (used by Docker). The convenience launcher validates the documented `DB_*` interface.

Run the frontend separately with `npm run dev` from `softwareproject_frontend`, then open http://localhost:5173/. Never put backend credentials in `VITE_*` variables: frontend configuration is public.

## Application and packaging protections

- Required database/JWT placeholders have no usable credential fallback. MySQL credentials are validated before datasource beans initialize; isolated H2 tests may retain empty passwords.
- JWT construction rejects null, blank, unresolved, short and common example keys, using a fixed error without the supplied value. At least 32 UTF-8 bytes are required; generate random keys rather than choosing a phrase. This validation cannot prove randomness.
- The application no longer activates the local development profile implicitly. SQL/bind, connection-pool and security verbosity are reduced, and management configuration values are always hidden; only health remains exposed.
- Maven resources and Docker build contexts exclude the local development properties/environment files. A previously copied local properties file was removed from `target/classes`; local database backups under `target` were preserved.
- `.env` variants are ignored while `.env.example` and `.env.backend.example` remain reviewable. Docker Compose requires explicit database and JWT secrets, and local Docker uses a separately supplied MySQL root password. Example secret values are blank.
- SMTP credentials already use environment variables. They remain optional for startup; actual password-reset email delivery requires configured SMTP. No outbound email was sent during testing.

## Exposure and limits

A live database credential was removed from the older asset-inventory document. This removes it from the current tree, **not existing Git history**. The user should coordinate rotation of historically exposed credentials before deployment; this change preserves the local database account and existing JWT key to avoid breaking other consumers. No Git history rewrite, remote deployment, or credential rotation was performed.

Environment variables protect secrets from source control and build artifacts; they do not hide them from administrators or other processes with sufficient operating-system permissions. Keep local environment files private and provision production variables through deployment secrets.

## Verification

Verification results are recorded in `12-application-security-progress.md`. Backend configuration tests exercise missing/blank/unresolved database settings and valid/invalid JWT keys. `scripts/test-backend-environment.ps1` checks literal parsing, shell precedence, environment restoration and safe failures. Live checks use the existing obqa database and original account passwords.

Suggested commit: `feat(security): externalize and validate backend secrets`
