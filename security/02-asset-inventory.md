# 02 — Project Asset Inventory

Sensitivity: **Critical** (compromise = severe/legal impact), **High** (significant business impact), **Medium**, **Low**.

## Data assets

| Asset | Location | Sensitivity | C / I / A impact if compromised |
|---|---|---|---|
| Student marks & grades | `StudentMark` table (MySQL) | Critical | C: exposes academic records of individuals. I: forged grades undermine accreditation validity. A: loss blocks reporting deadlines. |
| Student PII (name, batch, etc.) | `Student` table | Critical | C: privacy breach (FERPA-like exposure). |
| LO/PO mapping data + approval state | `LosPos`, mapping tables | High | I: false attainment claims threaten accreditation (ABET/NBA/Washington Accord/MQF) credibility. |
| CQI action plans | `CqiAction` and related | Medium | I: tampering hides real quality gaps. |
| User credentials | `User` table (`usertype`, password) | Critical | Currently stored **plaintext** (`NoOpPasswordEncoder`) — a DB leak directly exposes all admin/lecturer passwords. |
| Excel templates/uploads (marks) | Transient, via `ExcelImportService`/`ExcelExportService` | High | Uploaded files parsed server-side with no validation — availability/integrity risk (malformed/oversized files). |

## Users / identities

| Asset | Detail | Sensitivity |
|---|---|---|
| SuperAdmin account(s) | Full system control | Critical |
| Admin account(s) | Approves mappings, manages modules | High |
| Lecturer account(s) | Uploads marks, creates LOs/mappings | High |
| JWT bearer tokens | HS256, 2h expiry, stored in browser `localStorage` | Critical — sole proof of identity/role; XSS-exfiltratable since not httpOnly |

No authenticated "Student" identity exists in the system (see `01-system-review.md`).

## Servers / infrastructure

| Asset | Detail | Sensitivity |
|---|---|---|
| MySQL container | Port 3306, holds all persistent data | Critical |
| Spring Boot backend container | Port 8080, holds `jwt.secret` and DB credentials in memory/config | Critical |
| nginx/frontend container | Port 80, serves static SPA, reverse-proxies `/api/` | Medium |
| CI/CD pipeline | `.github/workflows/ci-cd.yml`, `ci.yml` — builds/tests, pushes images to `ghcr.io` | High — compromise could inject malicious code into published images |
| GitHub repository | Source of truth, git history | High — prior commit history may still contain the DB password (see Secrets below) |

## APIs

| Base path | AuthN required | Server-side RBAC enforced? |
|---|---|---|
| `/api/auth` | No (permitAll) | N/A (login endpoint) |
| `/api/modules` | Yes (`.authenticated()`) | **No** |
| `/api/obe`, `/api/obe/assessment` | Yes | **No** |
| `/api/cqi` | Yes | **No** |
| `/api/los-with-mapping` | Yes | **No** |
| `/api/lo-po-mapping` | Yes | **No** |
| `/api/program-outcomes` | Yes | **No** |
| `/api/lospos` | Yes | **No** |

Every non-login endpoint requires *some* valid JWT, but none enforce role — any authenticated user (Lecturer, Admin, or SuperAdmin) can currently call any endpoint. This is the single highest-priority finding across the whole inventory.

## Database

| Asset | Detail | Sensitivity |
|---|---|---|
| Schema | `Student`, `Module`, `Los`, `StudentMark`, `User`, `CqiAction`, `ProgramOutcome`, `LosPos`, mapping tables | Critical (aggregate) |
| Schema management | `ddl-auto=update` — auto-migrates schema on every prod boot, no reviewed migration path (no Flyway/Liquibase) | High risk of unintended schema drift/data loss |
| DB access | Single `root`-level MySQL user used by the app (no least-privilege app-specific DB user found) | High |

## Secrets

| Secret | Current location | Status |
|---|---|---|
| DB password (`Kopu2001`) | `application.properties:6-7`, `application-dev.properties:3-4` (plaintext) | Critical — hardcoded; dev file now gitignored but has prior commit history, likely already leaked |
| `jwt.secret` | `application.properties:34` (static string, with an inline comment warning it must change for production) | Critical — hardcoded; anyone with source access can forge tokens |
| `.env` values (root) | `.env.example` present with dummy/test values; real `.env` gitignored | Medium — process is correct, but backend doesn't use `.env`/env vars yet for its own secrets (see above) |

## Source code / build assets

| Asset | Detail | Sensitivity |
|---|---|---|
| GitHub repository (Kopuraj/OBQA project) | Full source + history | High |
| Docker images on `ghcr.io` | Built from `Dockerfile`s in both subprojects | Medium |
| Postman collections / API docs checked into `Software-project-Backend/` | Document full endpoint surface for anyone with repo access | Low-Medium (useful to testers, not independently a vulnerability) |

## How this feeds later phases

This inventory is the reference table for Phase 3 threat modeling (assets ↔ entry points ↔ STRIDE) and Phase 4/5 (which controls protect which asset). Any new asset discovered during later phases should be added back here to keep it authoritative.
