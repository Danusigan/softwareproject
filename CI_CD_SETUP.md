# Professional CI/CD Setup - Enterprise Standards

## 📋 Overview

This project uses **enterprise-grade CI/CD** with:
- ✅ Automated testing (Backend + Frontend)
- ✅ Code quality analysis (SonarQube, CodeQL)
- ✅ Security scanning (Trivy, npm audit)
- ✅ Docker containerization
- ✅ GitHub Secrets for secure credentials
- ✅ Multi-stage builds for optimization

---

## 🔐 GitHub Secrets Setup

Required secrets for CI/CD to work. Add these in: **Settings → Secrets and Variables → Actions**

| Secret Name | Value | Purpose |
|------------|-------|---------|
| `DB_USERNAME` | `root` | MySQL test user |
| `DB_PASSWORD` | `test_password` | MySQL test password |
| `DB_NAME` | `test_db` | MySQL test database |
| `SONAR_TOKEN` | *Your SonarCloud token* | Optional: Code quality analysis |

---

## 🚀 CI/CD Pipeline Stages

### 1. **Quality Gate** (On every push/PR)
- CodeQL security analysis
- SonarQube code quality checks
- Parallel execution (~3-5 min)

### 2. **Backend Testing**
- JDK 17 setup
- MySQL 8.0 Docker service
- Maven build & package
- Unit tests + integration tests
- JUnit reports + Codecov coverage

### 3. **Frontend Testing**
- Node.js 18 setup
- npm ci (clean install)
- Linting & code analysis
- Build verification
- npm audit (security)

### 4. **Dependency Scanning**
- Trivy vulnerability scan
- npm package audit
- SARIF report upload to GitHub

### 5. **Docker Build** (Main branch only)
- Multi-stage builds
- Push to GitHub Container Registry
- Automatic versioning
- Cache optimization

---

## 🐳 Docker Deployment

### Local Development
```bash
# Copy environment template
cp .env.example .env

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Services
- **Backend**: http://localhost:8080
- **Frontend**: http://localhost
- **MySQL**: localhost:3306

### Production Docker Images
```bash
# Backend image location
ghcr.io/Danusigan/softwareproject/backend:latest

# Frontend image location
ghcr.io/Danusigan/softwareproject/frontend:latest
```

---

## 📊 Test & Coverage

### Backend Testing
```bash
cd Software-project-Backend
mvn clean test
mvn clean test jacoco:report  # With coverage
```

### Frontend Testing
```bash
cd softwareproject_frontend
npm install
npm test
npm run lint
```

---

## 🔒 Security Best Practices Implemented

✅ **Non-root Docker containers** (runs as `app` user)
✅ **Health checks** for container orchestration
✅ **Security headers** in nginx (CSP, X-Frame-Options, etc)
✅ **Gzip compression** for performance
✅ **Git secrets scanning** (no credentials in code)
✅ **Vulnerability scanning** (Trivy, CodeQL)
✅ **Multi-stage Docker builds** (minimal image size)
✅ **Static analysis** (CodeQL, SonarQube)

---

## 📈 Monitoring & Reports

### GitHub Actions
- View runs: **Actions tab** in GitHub
- Check test reports: **Summary page** of each run
- Security scanning: **Security → Code scanning**

### Coverage
- CodeQL: GitHub Security tab
- Codecov: External badge/reports
- JUnit: Detailed test reports

---

## 🔄 CI/CD Workflow Triggers

| Event | Action |
|-------|--------|
| Push to `main` | Full CI/CD + Docker build |
| Push to `develop` | Full CI/CD (skip Docker) |
| Pull Request | Full CI/CD (skip Docker) |
| Manual Trigger | Workflow dispatch (if enabled) |

---

## 🛠️ GitHub Secrets (Additional Optional)

For SonarQube integration:
```bash
SONAR_TOKEN=your_sonarcloud_token_here
```

---

## 📝 Status Checks for PRs

Auto-runs on every PR:
- ✅ Backend Tests
- ✅ Frontend Tests  
- ✅ Code Quality Gate
- ✅ Security Scans

All must pass before merge to `main`.

---

## 🚨 Troubleshooting

**Backend tests fail:**
- Check MySQL container is healthy
- Verify DB credentials match secrets
- Review test logs in Actions

**Frontend build fails:**
- Ensure `package-lock.json` exists
- Check Node.js version (18+)
- Review npm error logs

**Docker build fails:**
- Ensure Dockerfile exists in each service folder
- Check registry credentials
- Verify image names

---

## 📚 Enterprise Standards Implemented

| Standard | Implementation |
|----------|-----------------|
| **Clean Code** | CodeQL + SonarQube scanning |
| **Testing** | Unit + Integration tests |
| **Security** | Trivy, CodeQL, npm audit |
| **Containerization** | Multi-stage Docker builds |
| **Secrets Management** | GitHub Secrets (encrypted) |
| **Reproducibility** | package-lock.json, Maven pom.xml |
| **Health Checks** | Docker health endpoints |
| **Logging** | Spring Boot + Docker logs |
| **Performance** | Gzip, caching, CDN-ready |

---

For questions or issues, check GitHub Actions logs or contact the DevOps team.
