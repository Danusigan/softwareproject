# Contributing Guidelines

Welcome to the Software Project! This document outlines development workflow and standards.

---

## 🚀 Getting Started

### Prerequisites
- **Java 17+**
- **Node.js 18+**
- **Docker & Docker Compose**
- **Git**

### Quick Start
```bash
git clone https://github.com/Danusigan/softwareproject.git
cd softwareproject

make setup
make run
```

---

## 📋 Development Workflow

### 1. Create a Feature Branch
```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/bug-name
```

### 2. Make Your Changes
- Follow code standards
- Commit frequently with clear messages

### 3. Test Locally
```bash
make test
make test-backend
make test-frontend
```

### 4. Push & Create Pull Request
```bash
git push origin feature/your-feature-name
```

---

## 📝 Code Standards

### Java (Backend)
- Use Spring Boot conventions
- Single responsibility principle
- Meaningful names
- Unit tests for business logic

### JavaScript/React (Frontend)
- Use functional components with hooks
- Keep components under 200 lines
- Meaningful variable/function names
- Add prop validation

### Commits
```
Format: <type>(<scope>): <subject>

Types: feat, fix, docs, style, refactor, perf, test, chore
Scope: module or feature name
Subject: clear, concise, lowercase

Examples:
- feat(auth): add JWT token refresh
- fix(api): handle null pointer exception
- docs: update setup instructions
```

---

## 🧪 Testing Requirements

### Backend
- Unit tests for business logic
- Integration tests for API endpoints
- Use `@SpringBootTest`

```bash
cd Software-project-Backend
mvn clean test
```

### Frontend
- Tests for critical components
- User interaction testing

```bash
cd softwareproject_frontend
npm test
```

---

## 🐳 Docker Workflow

```bash
make docker-build      # Build images
make docker-up         # Start services
make docker-logs       # View logs
make docker-down       # Stop services
```

---

## 🔒 Security Guidelines

- ❌ Never commit secrets, credentials, or API keys
- ✅ Use `.env.example` for templates
- ✅ Use GitHub Secrets for CI/CD
- ✅ Keep dependencies updated

---

## 📞 Need Help?

- Check [CI_CD_SETUP.md](./CI_CD_SETUP.md) for pipeline details
- Review GitHub Actions logs for failures
- Create an GitHub issue

---

## ✨ Thank You!

Thank you for contributing to this project! 🎉
