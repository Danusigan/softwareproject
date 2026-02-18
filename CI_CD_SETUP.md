# CI/CD Setup Guide

## 📋 Overview

This project uses **enterprise-grade CI/CD** with:
- ✅ Automated testing (Backend + Frontend)
- ✅ Code quality analysis (CodeQL)
- ✅ Docker containerization
- ✅ GitHub Secrets for secure credentials

---

## 🔐 GitHub Secrets Setup

Required secrets for CI/CD. Add in: **Settings → Secrets and Variables → Actions**

| Secret Name | Value | Purpose |
|------------|-------|---------|
| `DB_USERNAME` | `root` | MySQL test user |
| `DB_PASSWORD` | `test_password` | MySQL test password |
| `DB_NAME` | `test_db` | MySQL test database |

---

## 🚀 CI/CD Pipeline Stages

### 1. **Quality Gate**
- CodeQL security analysis
- Runs on every push/PR

### 2. **Backend Testing**
- JDK 17 setup
- MySQL 8.0 Docker service
- Maven build & package
- Unit & integration tests

### 3. **Frontend Testing**
- Node.js 18 setup
- npm build verification
- Code build

### 4. **Status Check**
- Final pipeline status
- Passes if all jobs succeed

---

## 🐳 Docker Deployment

### Local Development
```bash
docker-compose up -d
docker-compose logs -f
docker-compose down
```

### Services
- **Backend**: http://localhost:8080
- **Frontend**: http://localhost
- **MySQL**: localhost:3306

---

## 🔒 Security Best Practices

✅ Non-root Docker containers  
✅ Health checks  
✅ Git secrets scanning (no credentials in code)  
✅ GitHub Secrets for sensitive data  
✅ Multi-stage Docker builds  

---

## 📊 Monitoring

View runs: **Actions tab** in GitHub repository

Check:
- Build status
- Test results
- Security scans

---

## 🔄 Workflow Triggers

| Event | Action |
|-------|--------|
| Push to `main` | Full CI/CD |
| Push to `develop` | Full CI/CD |
| Pull Request | Full CI/CD |

---

## 🛠️ Troubleshooting

**Backend tests fail:**
- Check MySQL container is healthy
- Verify DB credentials in secrets
- Review test logs in Actions

**Frontend build fails:**
- Ensure `package-lock.json` exists
- Check Node.js version (18+)
- Review npm error logs

**Docker build fails:**
- Ensure Dockerfile exists
- Check registry credentials
- Verify image names

---

For more details, see [CONTRIBUTING.md](./CONTRIBUTING.md).
