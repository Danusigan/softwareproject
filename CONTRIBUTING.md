# Contributing Guidelines

Welcome to the Software Project! This document outlines the development workflow, standards, and best practices for contributing.

---

## 🚀 Getting Started

### Prerequisites
- **Java 17+** (Spring Boot backend)
- **Node.js 18+** (React/Vite frontend)
- **Docker & Docker Compose** (recommended for local dev)
- **Git**

### Quick Start
```bash
# Clone the repository
git clone https://github.com/Danusigan/softwareproject.git
cd softwareproject

# Setup development environment
make setup

# Start all services
make run

# Access the application
# Frontend: http://localhost
# Backend: http://localhost:8080
```

Or start individual services:
```bash
make run-backend        # Start Spring Boot
make run-frontend       # Start Vite dev server
```

---

## 📋 Development Workflow

### 1. Create a Feature Branch
```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/bug-name
git checkout -b chore/maintenance-task
```

### 2. Make Your Changes
- Follow code standards (see below)
- Commit frequently with clear messages
- Keep commits atomic and logically organized

### 3. Test Locally
```bash
# Run all tests
make test

# Or specific tests
make test-backend
make test-frontend

# Check code quality
make lint-frontend
make security-audit
```

### 4. Push & Create Pull Request
```bash
git push origin feature/your-feature-name
```

Then create a PR on GitHub with:
- Clear title and description
- Reference to related issues
- Screenshots (if UI changes)

### 5. CI/CD Validation
Your PR will automatically:
- ✅ Build both backend and frontend
- ✅ Run all tests
- ✅ Execute code quality checks
- ✅ Scan for security vulnerabilities
- ✅ Publish test reports

All checks **must pass** before merging.

---

## 📝 Code Standards

### Java (Backend)
```java
// Follow Spring Boot conventions
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // Single responsibility principle
    // DRY (Don't Repeat Yourself)
    // Use meaningful names
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        // Implementation
    }
}
```

**Standards:**
- Use Spring Boot best practices
- Keep classes focused (SRP)
- Write unit tests for business logic
- Add meaningful JavaDoc comments
- Follow `camelCase` naming

### JavaScript/React (Frontend)
```javascript
// Use functional components with hooks
export function UserProfile({ userId }) {
  const [user, setUser] = useState(null);
  
  // Meaningful component/function names
  // Keep components small and focused
  
  return <div>{/* JSX */}</div>;
}
```

**Standards:**
- Use functional components (not class components)
- Use hooks for state management
- Keep components under 200 lines
- Use meaningful variable/function names
- Add prop validation or TypeScript

### Commits
```
Format: <type>(<scope>): <subject>

Types: feat, fix, docs, style, refactor, perf, test, chore
Scope: module or feature name
Subject: clear, concise, lowercase

Examples:
- feat(auth): add JWT token refresh mechanism
- fix(api): handle null pointer exception in UserService
- docs: update CI/CD setup instructions
- test(frontend): add unit tests for LoginPage component
```

---

## 🗂️ Project Structure

```
softwareproject/
├── Software-project-Backend/      # Spring Boot API
│   ├── src/main/java/            # Source code
│   ├── src/test/java/            # Tests
│   ├── pom.xml                   # Maven config
│   └── Dockerfile                # Production image
├── softwareproject_frontend/       # React/Vite app
│   ├── src/                      # Source code
│   ├── package.json              # Dependencies
│   ├── Dockerfile                # Production image
│   └── nginx.conf                # Web server config
├── .github/workflows/
│   ├── ci-cd.yml                # Main CI/CD pipeline
│   └── ci.yml                    # (Deprecated)
├── docker-compose.yml             # Local development
├── Makefile                       # Common commands
├── CI_CD_SETUP.md                # Pipeline documentation
└── CONTRIBUTING.md               # This file
```

---

## 🔒 Security Guidelines

### General
- ❌ Never commit secrets, credentials, or API keys
- ✅ Use `.env.example` for environment template
- ✅ Use GitHub Secrets for CI/CD credentials
- ✅ Keep dependencies updated

### Backend
```java
// ❌ Wrong
String password = "admin123";  // Never hardcode!

// ✅ Correct
String password = System.getenv("APP_PASSWORD");
```

### Frontend
```javascript
// ❌ Wrong
const apiKey = "sk_live_abc123";  // Never hardcode!

// ✅ Correct
const apiKey = import.meta.env.VITE_API_KEY;
```

### Dependencies
```bash
# Check for vulnerabilities
make security-audit

# Keep packages updated
npm audit fix
mvn dependency:update-check
```

---

## 🧪 Testing Requirements

### Backend
- Minimum 70% code coverage
- Unit tests for all business logic
- Integration tests for API endpoints
- Use `@SpringBootTest` for integration tests

```bash
# Run tests with coverage
cd Software-project-Backend
mvn clean test jacoco:report
```

### Frontend
- Tests for critical components
- User interaction testing
- Use `vitest` or `jest`

```bash
# Run tests
cd softwareproject_frontend
npm test

# Check coverage
npm test -- --coverage
```

---

## 🐳 Docker Workflow

### Build Images
```bash
make docker-build
```

### Run Services Locally
```bash
make docker-up
```

### View Logs
```bash
make docker-logs
```

### Stop Services
```bash
make docker-down
```

---

## 📊 Performance Guidelines

- Keep component bundle size < 500KB
- Optimize images (WebP, compression)
- Use lazy loading for routes
- Cache API responses appropriately
- Set database indexes on frequently queried columns

---

## 🐛 Bug Reports

When reporting bugs, include:
1. Steps to reproduce
2. Expected behavior
3. Actual behavior
4. Environment details (OS, browser, versions)
5. Screenshots/logs if applicable

---

## 📚 Documentation

Update documentation when:
- Adding new features
- Changing API endpoints
- Modifying configuration
- Updating dependencies

Use clear language and include examples.

---

## 🔄 Code Review Checklist

Before submitting PR, ensure:
- [ ] Code follows project standards
- [ ] Tests pass locally (`make test`)
- [ ] No console warnings/errors
- [ ] Commits have clear messages
- [ ] Documentation is updated
- [ ] No sensitive data in commits
- [ ] `package-lock.json` committed (frontend)
- [ ] `pom.xml` updated (if dependencies changed)

---

## 🚨 Troubleshooting

### Backend Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080
# Kill process
kill -9 <PID>
```

### Frontend Build Issues
```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install
npm run build
```

### Docker Issues
```bash
# Remove all containers and volume
docker-compose down -v

# Rebuild
docker-compose build --no-cache
docker-compose up -d
```

---

## 📞 Need Help?

- Check [CI_CD_SETUP.md](./CI_CD_SETUP.md) for pipeline details
- Review GitHub Actions logs for CI/CD failures
- Create an issue with detailed description
- Contact the DevOps team

---

## ✨ Thank You!

Thank you for contributing to this project! Your work helps make this better for everyone. 🎉
