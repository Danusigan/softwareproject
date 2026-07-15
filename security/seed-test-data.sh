#!/bin/sh
# Seeds synthetic test accounts (SuperAdmin/Admin/Lecturer) into the isolated
# Docker Compose test environment for the OBQA security assessment.
# Safe to re-run after `docker compose down -v && docker compose up -d`.
#
# Requires: docker compose stack running and healthy (mysql, backend, frontend).

set -e

BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
DB_CONTAINER="softwareproject-db"
DB_USER="${DB_USERNAME:-appuser}"
DB_PASS="${DB_PASSWORD:-test_password}"
DB_NAME="${DB_NAME:-test_db}"

echo "== Seeding SuperAdmin (direct SQL insert; no REST endpoint creates this role) =="
# Password is BCrypt-hashed (backend uses BCryptPasswordEncoder since Phase 5) —
# hash below is for 'SuperAdminTest123!', generated via: python3 -c "import bcrypt; print(bcrypt.hashpw(b'SuperAdminTest123!', bcrypt.gensalt()).decode())"
SUPERADMIN_HASH='$2b$12$04XibJToJEgJB13D5sbEkOU0ucaWrGeWft9RF.isu7X.EQ6hH6tFq'
docker exec "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -e \
  "INSERT INTO User (User_ID, email, password, user_type) VALUES ('superadmin_test', 'superadmin@test.local', '$SUPERADMIN_HASH', 'superadmin');" \
  2>/dev/null || echo "  (already exists, skipping)"

echo "== Seeding Admin (via built-in /api/auth/create-test-user) =="
curl -s -X POST "$BACKEND_URL/api/auth/create-test-user"
echo ""

echo "== Seeding Lecturer (via admin token) =="
ADMIN_TOKEN=$(curl -s -X POST "$BACKEND_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"userID":"admin","password":"password123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl -s -X POST "$BACKEND_URL/api/auth/add-lecture" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"userID":"lecturer_test","email":"lecturer@test.local","password":"LecturerTest123!","usertype":"lecture"}'
echo ""

echo "== Done. Test accounts: =="
echo "  superadmin_test / SuperAdminTest123!"
echo "  admin           / password123"
echo "  lecturer_test   / LecturerTest123!"
