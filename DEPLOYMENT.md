# Deployment Guide — LO-PO Analytics System

## Overview

This project uses GitHub Actions CI/CD to automatically build, test, and deploy to an **Oracle Cloud Free Tier ARM VM** on every push to `main`.

```
Push to main
    │
    ├── quality-gate     (CodeQL security scan)
    ├── backend          (Maven build + tests)
    ├── frontend         (npm build)
    │
    ├── build-and-push   (Docker images → GHCR)  ← only on main
    │
    └── deploy           (SSH → docker compose up)  ← only on main
```

---

## Deployment-Related Files

### CI/CD Pipeline
| File | Purpose |
|------|---------|
| `.github/workflows/ci-cd.yml` | Full pipeline: test → build images → SSH deploy |

### Docker / Container
| File | Purpose |
|------|---------|
| `Software-project-Backend/Dockerfile` | Multi-stage Spring Boot image (ARM64, non-root) |
| `softwareproject_frontend/Dockerfile` | Multi-stage Vite → Nginx image (ARM64, non-root) |
| `softwareproject_frontend/nginx.conf` | Nginx config: serves React SPA + proxies `/api/*` to backend |
| `docker-compose.yml` | **Local dev only** — builds images from source, runs all 3 services |
| `deploy/docker-compose.prod.yml` | **Production** — pulls pre-built images from GHCR |

### Server Setup
| File | Purpose |
|------|---------|
| `deploy/server-setup.sh` | One-time bootstrap: installs Docker on the Oracle Cloud VM |

### Configuration
| File | Purpose |
|------|---------|
| `.env.example` | Template for local `.env` variables |
| `Software-project-Backend/src/main/resources/application.properties` | Backend config — reads `JWT_SECRET` from env in production |

---

## Architecture in Production

```
Browser
  │
  │  HTTP :80
  ▼
┌─────────────────────────────────────┐
│  Frontend Container (Nginx)         │
│  - Serves React/Vite static files   │
│  - Proxies /api/* → backend:8080    │
└─────────────────┬───────────────────┘
                  │ internal Docker network
                  ▼
┌─────────────────────────────────────┐
│  Backend Container (Spring Boot)    │
│  - Listens on :8080                 │
│  - Connects to MySQL container      │
└─────────────────┬───────────────────┘
                  │ internal Docker network
                  ▼
┌─────────────────────────────────────┐
│  MySQL 8.0 Container                │
│  - Data persisted in Docker volume  │
└─────────────────────────────────────┘
```

All 3 containers run on the same Oracle Cloud VM via `docker-compose.prod.yml`.  
Only port **80** is exposed to the internet. Backend and MySQL are internal only.

---

## Step-by-Step Deployment Setup

### Step 1 — Provision Oracle Cloud VM (you do this)

1. Sign up at [cloud.oracle.com](https://cloud.oracle.com) (Always Free account)
2. Create a **Compute Instance**:
   - Shape: `VM.Standard.A1.Flex` (Ampere ARM — Always Free)
   - Image: **Ubuntu 22.04**
   - Add your SSH public key during creation
   - Note the **public IP address**
3. Open port 80 in the **VCN Security List**:
   - Networking → Virtual Cloud Networks → your VCN → Security Lists
   - Add Ingress Rule: Source `0.0.0.0/0`, Protocol TCP, Port `80`

> **Oracle Cloud Gotcha:** Port 80 must be opened in **two** places:
> the VCN Security List (console) AND the OS firewall (ufw). The script below handles the OS firewall.

---

### Step 2 — Bootstrap the VM (run once via SSH)

SSH into your new VM, then run:

```bash
# Copy the setup script to the server
scp deploy/server-setup.sh ubuntu@<VM-IP>:~/server-setup.sh

# SSH in and run it
ssh ubuntu@<VM-IP>
bash ~/server-setup.sh
```

Then create the production environment file on the server:

```bash
mkdir -p ~/deploy
nano ~/deploy/.env
```

Paste this into the `.env` file (replace values with real secrets):

```env
PROD_DB_NAME=softwareproject
PROD_DB_USERNAME=appuser
PROD_DB_PASSWORD=<strong-random-password>
JWT_SECRET=<random-string-at-least-32-characters-long>
```

Log out and back in so Docker group permissions take effect:
```bash
exit
ssh ubuntu@<VM-IP>
```

---

### Step 3 — Add GitHub Secrets (you do this)

Go to: **GitHub repo → Settings → Secrets and Variables → Actions → New repository secret**

| Secret Name | Value |
|-------------|-------|
| `VPS_HOST` | Your Oracle Cloud VM public IP |
| `VPS_USER` | `ubuntu` (default for Ubuntu images) |
| `VPS_SSH_KEY` | Contents of your SSH **private key** file |
| `JWT_SECRET` | Same value you put in `~/deploy/.env` on the server |
| `PROD_DB_NAME` | `softwareproject` |
| `PROD_DB_USERNAME` | `appuser` |
| `PROD_DB_PASSWORD` | Same strong password from `~/deploy/.env` |

> Existing CI secrets (`DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`) are for test runs only — keep them.

---

### Step 4 — Make GHCR Packages Public (after first push)

After the first successful `build-and-push` job:

1. Go to your GitHub profile → **Packages**
2. Find `softwareproject/backend` and `softwareproject/frontend`
3. Package Settings → Change visibility → **Public**

This allows the VM to pull images without needing a Docker login. Do this once.

---

### Step 5 — Deploy

Push to `main`:

```bash
git push origin main
```

Watch: **GitHub → Actions tab** — all 5 jobs should go green.

Visit `http://<VM-IP>` in your browser — the login page should load.

---

## Local Development (unchanged)

The production changes do **not** affect local dev. Run as before:

```bash
# Terminal 1 — backend (Spring Boot on :8080)
cd Software-project-Backend
mvn spring-boot:run

# Terminal 2 — frontend (Vite on :5173)
cd softwareproject_frontend
npm run dev
```

Visit `http://localhost:5173`. Vite's dev proxy forwards all `/api/...` calls to `localhost:8080` automatically.

Alternatively, run everything with Docker locally:
```bash
docker-compose up -d        # uses docker-compose.yml (builds from source)
docker-compose logs -f
docker-compose down
```

---

## Image Registry

Docker images are stored in GitHub Container Registry (GHCR):

| Image | Tag |
|-------|-----|
| `ghcr.io/danusigan/softwareproject/backend` | `latest`, `<commit-sha>` |
| `ghcr.io/danusigan/softwareproject/frontend` | `latest`, `<commit-sha>` |

Images are built for **linux/arm64** (Oracle Ampere A1 architecture).

---

## Troubleshooting

**App not reachable at VM IP:**
- Check VCN Security List has port 80 open (Oracle Cloud Console)
- Check OS firewall: `sudo ufw status` on the VM (port 80 should show ALLOW)
- Check containers running: `docker compose -f ~/deploy/docker-compose.prod.yml ps`

**Backend container keeps restarting:**
- Check logs: `docker compose -f ~/deploy/docker-compose.prod.yml logs backend`
- Likely MySQL not ready yet — wait 60s after first `up`, it's normal on first boot

**Images not pulling on VM:**
- Ensure GHCR packages are set to Public (Step 4 above)
- Or run `docker login ghcr.io` on the VM with a GitHub PAT

**Frontend loads but API calls fail:**
- Open browser DevTools → Network — confirm requests go to `/api/...` (not `http://localhost:8080/api/...`)
- Check nginx logs: `docker compose -f ~/deploy/docker-compose.prod.yml logs frontend`
