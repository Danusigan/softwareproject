# Software Project - Full Stack Application

[![CI/CD Pipeline](https://github.com/Danusigan/softwareproject/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/Danusigan/softwareproject/actions)
[![Code Quality](https://sonarcloud.io/api/project_badges/measure?project=Danusigan_softwareproject&metric=alert_status)](https://sonarcloud.io/dashboard?id=Danusigan_softwareproject)

A **production-ready, enterprise-grade** full-stack application with automated CI/CD, comprehensive testing, and cloud-native architecture.

---

## 🏗️ Architecture

### Technology Stack
- **Backend**: Spring Boot 3.2 + Java 17 + MySQL 8.0
- **Frontend**: React 18 + Vite 5 + TailwindCSS
- **DevOps**: Docker, Docker Compose, GitHub Actions
- **Code Quality**: SonarQube, CodeQL, Trivy
- **Security**: GitHub Secrets, JWT, SSL/TLS

### Infrastructure
```
┌─────────────────────────────────────────────────┐
│         GitHub Actions (CI/CD)                  │
│  ├─ Build & Test (Backend + Frontend)          │
│  ├─ Code Quality Analysis                       │
│  ├─ Security Scanning                           │
│  └─ Docker Image Push (Production)              │
└─────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────┐
│    GitHub Container Registry (ghcr.io)          │
│  ├─ Backend Image                               │
│  └─ Frontend Image                              │
└─────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────┐
│     Docker Compose (Local/Production)           │
│  ├─ Frontend (Nginx)   :80                      │
│  ├─ Backend (Spring)   :8080                    │
│  └─ Database (MySQL)   :3306                    │
└─────────────────────────────────────────────────┘
```

---

## ✨ Features

### ✅ Backend
- RESTful API architecture
- JWT authentication & authorization
- Spring Data JPA for database operations
- Comprehensive error handling
- API documentation with Spring Doc OpenAPI
- Health checks & monitoring

### ✅ Frontend
- Modern React with functional components
- Responsive design with TailwindCSS
- React Router for navigation
- Axios for API communication
- Optimized build with Vite

### ✅ DevOps
- Automated testing (JUnit, npm)
- Code quality gates (SonarQube, CodeQL)
- Security scanning (Trivy, npm audit)
- Docker containerization with multi-stage builds
- CI/CD pipeline with GitHub Actions
- Encrypted secrets management with GitHub Secrets

---

## 🚀 Quick Start

### Prerequisites
```bash
# Check versions
java -version          # Should be 17+
node -v               # Should be 18+
docker --version      # Latest
```

### Local Development

#### Option 1: Docker (Recommended)
```bash
# Clone repository
git clone https://github.com/Danusigan/softwareproject.git
cd softwareproject

# Copy environment template
cp .env.example .env

# Start all services
make run

# Access
# - Frontend: http://localhost
# - Backend:  http://localhost:8080
# - MySQL:    localhost:3306 (root:test_password)

# View logs
make docker-logs

# Stop services
make docker-down
```

#### Option 2: Manual (Development)
```bash
# Terminal 1: Backend
cd Software-project-Backend
mvn spring-boot:run

# Terminal 2: Frontend
cd softwareproject_frontend
npm install
npm run dev

# Terminal 3: Database (Docker only)
docker run -d -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=test_password \
  -e MYSQL_DATABASE=softwareproject \
  mysql:8.0
```

---

## 📦 Available Commands

### Development
```bash
make help              # Show all available commands
make setup             # Install dependencies
make run               # Start all services
make build             # Build backend + frontend
make test              # Run all tests
make clean             # Clean build artifacts
```

### Backend
```bash
make build-backend     # Build Spring Boot
make test-backend      # Run backend tests
make run-backend       # Run backend locally
```

### Frontend
```bash
make build-frontend    # Build React/Vite
make test-frontend     # Run frontend tests
make run-frontend      # Start dev server
make lint-frontend     # Lint code
```

### Docker
```bash
make docker-build      # Build images
make docker-up         # Start containers
make docker-down       # Stop containers
make docker-logs       # View logs
```

### Quality & Security
```bash
make security-audit    # Run security checks
make stats             # Show code statistics
```

---

## 🔄 CI/CD Pipeline

### Automated on Every Push

1. **Quality Gate**
   - CodeQL security analysis
   - SonarQube code quality scan

2. **Backend Testing**
   - JDK 17 setup
   - Maven build
   - Unit & integration tests
   - Test coverage reports

3. **Frontend Testing**
   - Node.js setup
   - npm build verification
   - Security audit

4. **Dependency Scanning**
   - Trivy vulnerability scan
   - npm audit

5. **Docker Build** (main branch only)
   - Multi-stage builds
   - Push to GitHub Container Registry
   - Automatic versioning

### View Pipeline Status
- Go to: **Actions** tab in GitHub repository
- Check build logs and test reports
- Review security scanning results

### For Detailed Guide
See [CI_CD_SETUP.md](./CI_CD_SETUP.md)

---

## 🧪 Testing

### Run Tests Locally
```bash
# All tests
make test

# Backend only
make test-backend
cd Software-project-Backend && mvn clean test jacoco:report

# Frontend only
make test-frontend
cd softwareproject_frontend && npm test -- --coverage
```

### Test Coverage
- Backend: JUnit + JaCoCo (Target: 70%+)
- Frontend: Vitest/Jest (Target: 60%+)

---

## 🔐 Security

### Implemented Security Measures
✅ **Docker**
- Non-root user execution
- Health checks
- Minimal base images

✅ **Web Security**
- Security headers (CSP, X-Frame-Options, etc.)
- HTTPS ready
- CORS configuration

✅ **Code Security**
- CodeQL analysis
- Trivy scanning
- SonarQube quality gates
- npm audit for dependencies

✅ **Secrets Management**
- GitHub Secrets for CI/CD
- Environment variables (never hardcoded)
- `.env` file (not committed)

### Best Practices
- Never commit `.env` or passwords
- Use GitHub Secrets for sensitive data
- Keep dependencies updated
- Regular security audits

---

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| [CI_CD_SETUP.md](./CI_CD_SETUP.md) | Complete CI/CD guide |
| [CONTRIBUTING.md](./CONTRIBUTING.md) | Development guidelines |
| [Makefile](./Makefile) | Available commands |
| [docker-compose.yml](./docker-compose.yml) | Local development setup |

---

## 🛠️ Troubleshooting

### Backend Issues
```bash
# Database connection error?
docker-compose ps    # Check MySQL is running
docker-compose logs mysql  # View MySQL logs

# Port 8080 in use?
lsof -i :8080 && kill -9 <PID>

# Tests failing?
cd Software-project-Backend
mvn clean test -X  # Verbose output
```

### Frontend Issues
```bash
# Build fails?
rm -rf node_modules package-lock.json
npm ci
npm run build

# Dev server won't start?
npm install
npm run dev -- --host
```

### Docker Issues
```bash
# Remove everything and start fresh
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

---

## 📊 Project Statistics

```bash
make stats    # Show code statistics
```

---

## 🤝 Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for:
- Development workflow
- Code standards
- Testing requirements
- Commit conventions
- PR checklist

### Quick Contribution Steps
1. Create feature branch: `git checkout -b feature/name`
2. Make changes & test: `make test`
3. Commit with clear message
4. Push to GitHub
5. Create Pull Request
6. Wait for CI/CD checks to pass
7. Request code review
8. Merge after approval

---

## 📝 License

This project is private. All rights reserved.

---

## 📞 Support & Communication

- **Issues**: Create GitHub issue with details
- **Discussions**: Use GitHub Discussions
- **Pull Requests**: Follow CONTRIBUTING.md guidelines

---

## ✅ Deployment Checklist

Before deploying to production:
- [ ] All tests passing
- [ ] Code quality gates met (SonarQube)
- [ ] Security scans clean (CodeQL, Trivy)
- [ ] Docker images built and tagged
- [ ] Environment variables configured
- [ ] Database migrations applied
- [ ] API documentation updated
- [ ] Rollback plan documented

---

## 🎯 Roadmap

- [ ] Kubernetes deployment configuration
- [ ] Azure/AWS cloud integration
- [ ] API versioning strategy
- [ ] GraphQL support
- [ ] WebSocket real-time features
- [ ] Advanced caching layer (Redis)

---

## 📚 Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [SonarQube Quality Gates](https://docs.sonarqube.org/latest/user-guide/quality-gates/)

---

**Last Updated**: February 17, 2026  
**Version**: 1.0.0 - Enterprise Grade

Made with ❤️ using professional DevOps standards.
