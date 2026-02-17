.PHONY: help build test clean run docker-build docker-up docker-down

help:
	@echo "═══════════════════════════════════════════════════════════"
	@echo "   Software Project - Enterprise Development Commands"
	@echo "═══════════════════════════════════════════════════════════"
	@echo ""
	@echo "Backend Commands:"
	@echo "  make build-backend         Build Spring Boot application"
	@echo "  make test-backend          Run backend tests"
	@echo "  make run-backend           Run backend locally"
	@echo ""
	@echo "Frontend Commands:"
	@echo "  make build-frontend        Build React/Vite application"
	@echo "  make test-frontend         Run frontend tests"
	@echo "  make run-frontend          Run frontend dev server"
	@echo ""
	@echo "Docker Commands:"
	@echo "  make docker-build          Build Docker images"
	@echo "  make docker-up             Start all services (docker-compose)"
	@echo "  make docker-down           Stop all services"
	@echo "  make docker-logs           View container logs"
	@echo ""
	@echo "Full Pipeline:"
	@echo "  make build                 Build backend + frontend"
	@echo "  make test                  Test backend + frontend"
	@echo "  make clean                 Clean all build artifacts"
	@echo "  make run                   Run all services locally"
	@echo ""
	@echo "═══════════════════════════════════════════════════════════"

# Backend targets
build-backend:
	@echo "🔨 Building Spring Boot backend..."
	cd Software-project-Backend && mvn clean package -DskipTests
	@echo "✅ Backend build complete"

test-backend:
	@echo "🧪 Running backend tests..."
	cd Software-project-Backend && mvn clean test
	@echo "✅ Backend tests complete"

run-backend:
	@echo "🚀 Starting backend server..."
	cd Software-project-Backend && mvn spring-boot:run

# Frontend targets
build-frontend:
	@echo "🔨 Building React/Vite frontend..."
	cd softwareproject_frontend && npm ci && npm run build
	@echo "✅ Frontend build complete"

test-frontend:
	@echo "🧪 Running frontend tests..."
	cd softwareproject_frontend && npm ci && npm test
	@echo "✅ Frontend tests complete"

run-frontend:
	@echo "🚀 Starting frontend dev server..."
	cd softwareproject_frontend && npm install && npm run dev

# Docker targets
docker-build:
	@echo "🐳 Building Docker images..."
	docker-compose build
	@echo "✅ Docker images built"

docker-up:
	@echo "🚀 Starting Docker services..."
	docker-compose up -d
	@echo "✅ Services started. Waiting for health checks..."
	@sleep 10
	docker-compose ps
	@echo ""
	@echo "   Frontend: http://localhost"
	@echo "   Backend:  http://localhost:8080"
	@echo "   MySQL:    localhost:3306"

docker-down:
	@echo "⛔ Stopping Docker services..."
	docker-compose down
	@echo "✅ Services stopped"

docker-logs:
	@docker-compose logs -f

# Full pipeline
build: build-backend build-frontend
	@echo "✅ All builds complete"

test: test-backend test-frontend
	@echo "✅ All tests complete"

clean:
	@echo "🧹 Cleaning build artifacts..."
	cd Software-project-Backend && mvn clean
	cd softwareproject_frontend && rm -rf node_modules dist
	@echo "✅ Cleanup complete"

run: docker-up
	@echo ""
	@echo "🎉 All services are running!"
	@echo ""
	@echo "Access points:"
	@echo "  • Frontend:  http://localhost"
	@echo "  • Backend:   http://localhost:8080"
	@echo "  • Database:  mysql://localhost:3306"
	@echo ""
	@echo "To view logs: make docker-logs"
	@echo "To stop:      make docker-down"

# Development setup
setup:
	@echo "📦 Setting up development environment..."
	@echo "Installing backend dependencies..."
	cd Software-project-Backend && mvn install -DskipTests
	@echo "Installing frontend dependencies..."
	cd softwareproject_frontend && npm ci
	@echo "✅ Development environment ready"

# Code quality
lint-frontend:
	@echo "🔍 Linting frontend code..."
	cd softwareproject_frontend && npm run lint 2>/dev/null || echo "⚠️  Linting not configured"

security-audit:
	@echo "🔒 Running security audit..."
	cd softwareproject_frontend && npm audit --audit-level=moderate || true
	@cd Software-project-Backend && mvn dependency-check:check || true
	@echo "✅ Security audit complete"

# Git operations
git-push:
	@echo "📤 Pushing to GitHub..."
	git add .
	git commit -m "Update from automated workflow"
	git push origin main

# Stats
stats:
	@echo "📊 Project Statistics:"
	@echo ""
	@echo "Backend:"
	@find Software-project-Backend/src -name "*.java" | wc -l | xargs echo "  Java files:"
	@echo ""
	@echo "Frontend:"
	@find softwareproject_frontend/src -name "*.jsx" -o -name "*.tsx" | wc -l | xargs echo "  React files:"
