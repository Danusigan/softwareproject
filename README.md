> **Local backend startup (security feature 7):** From this folder run
> `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-backend.ps1`.
> On a new machine, first copy `.env.backend.example` to `.env.backend.local` and set your private values.
> See [environment setup](security/18-environment-secret-protection.md) for IDE and Docker configuration.

# Software Project - Full Stack Application

[![CI/CD Pipeline](https://github.com/Danusigan/softwareproject/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/Danusigan/softwareproject/actions)

A **production-ready, enterprise-grade** full-stack application with automated CI/CD, comprehensive testing, and cloud-native architecture.

---

## 🏗️ Architecture

### Technology Stack
- **Backend**: Spring Boot 3.2 + Java 17 + MySQL 8.0
- **Frontend**: React 18 + Vite 5 + TailwindCSS
- **DevOps**: Docker, Docker Compose, GitHub Actions
- **Security**: GitHub Secrets, JWT, SSL/TLS

---

## ✨ Features

### ✅ Backend
- RESTful API architecture
- JWT authentication & authorization
- Spring Data JPA for database operations
- Comprehensive error handling

### ✅ Frontend
- Modern React with functional components
- Responsive design with TailwindCSS
- React Router for navigation
- Optimized build with Vite

### ✅ DevOps
- Automated testing (JUnit, npm)
- Code quality gates (CodeQL)
- Security scanning
- Docker containerization
- CI/CD pipeline with GitHub Actions
- Encrypted secrets management

---

## 🚀 Quick Start

### Prerequisites
```bash
java -version          # Should be 17+
node -v               # Should be 18+
docker --version      # Latest
```

### Local Development

#### Option 1: Docker (Recommended)
```bash
git clone https://github.com/Danusigan/softwareproject.git
cd softwareproject

cp .env.example .env

make run

# Access
# - Frontend: http://localhost
# - Backend:  http://localhost:8080
```

#### Option 2: Manual
```bash
# Terminal 1: Backend
cd Software-project-Backend
mvn spring-boot:run

# Terminal 2: Frontend
cd softwareproject_frontend
npm install
npm run dev
```

---

## 📦 Available Commands

```bash
make help              # Show all commands
make setup             # Install dependencies
make run               # Start all services
make build             # Build backend + frontend
make test              # Run all tests
make clean             # Clean artifacts
```

---

## 🔐 GitHub Secrets Setup

Currently using test configuration. No setup required yet.

**When deploying**, add these secrets to:  
**Settings → Secrets and Variables → Actions**

- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_NAME`

---

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| [README.md](./README.md) | This file |
| [CI_CD_SETUP.md](./CI_CD_SETUP.md) | CI/CD pipeline guide |
| [CONTRIBUTING.md](./CONTRIBUTING.md) | Development guidelines |
| [Makefile](./Makefile) | Available commands |

---

## 🧪 Testing

```bash
make test              # Run all tests
make test-backend      # Backend tests only
make test-frontend     # Frontend tests only
```

---

## 🐳 Docker

```bash
make docker-build      # Build images
make docker-up         # Start containers
make docker-down       # Stop containers
make docker-logs       # View logs
```

---

## 🤝 Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

---

**Last Updated**: February 17, 2026  
**Version**: 1.0.0 - Enterprise Grade
