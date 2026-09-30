# Deployment — LO-PO Analytics System

Production runs on **AWS** (EC2 + RDS MySQL), deployed automatically by **GitHub Actions** on every
push to `main`:

```
git push ─► GitHub Actions: tests ─► images to Docker Hub ─► Ansible deploy ─► EC2 app server ─► RDS
```

The full setup guide and operations runbook is in **[infra/README.md](infra/README.md)**.

| Path | Purpose |
|---|---|
| `.github/workflows/ci-cd.yml` | Pipeline: CodeQL, tests, SonarQube → build & push images → deploy |
| `infra/terraform/` | AWS infrastructure (VPC, EC2, RDS, security groups, GitHub OIDC deploy role) |
| `infra/ansible/` | Server setup (`site.yml`), per-push deploy (`deploy.yml`), first-login bootstrap (`create-superadmin.yml`) |
| `deploy/docker-compose.prod.yml` | What runs on the app server (frontend + backend; DB is RDS) |
| `Software-project-Backend/Dockerfile`, `softwareproject_frontend/Dockerfile` | Image builds |

Local development is unchanged: `docker compose up` with the root `docker-compose.yml` (see `.env.example`).
