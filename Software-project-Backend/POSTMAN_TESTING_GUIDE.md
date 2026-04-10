# OBE System - Postman Testing Guide

## Quick Start

### Base URL
```
http://localhost:8080
```

### Pre-loaded Database Users

| User ID | Email | Password | Role |
|---------|-------|----------|------|
| danu | danu@gmail.com | 1234 | Admin |
| danu1 | danu1@gmail.com | 1234 | Lecture (Lecturer) |
| danu2 | danu2@gmail.com | 1234 | Superadmin |

**Note:** No registration needed - these users are already in the database!

---

## Import Postman Collection

1. Open Postman
2. Click **"Import"** button (top left)
3. Select file: `OBE_Complete_Postman_Collection.json`
4. Click **"Import"**

---

## Step-by-Step Testing

### Step 1: Login (Get JWT Token)

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
    "userID": "danu1",
    "password": "1234"
}
```

**Expected Response:**
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userID": "danu1",
    "usertype": "Lecture"
}
```

**Important:** The Postman collection automatically saves the token to `{{jwt_token}}` variable!

---

### Step 2: Initialize Program Outcomes (Admin Required)

Login as **danu** or **danu2** first!

**Endpoint:** `POST /api/program-outcomes/initialize-defaults`

**Headers:**
```
Authorization: Bearer {{jwt_token}}
```

**What it does:** Creates 12 Washington Accord standard Program Outcomes (PO1-PO12)

**Expected Response:**
```json
{
    "status": "SUCCESS",
    "message": "Initialized 12 default program outcomes",
    "data": [...]
}
```

---

### Step 3: Create a Module (Admin Required)

Login as **danu** or **danu2** first!

**Endpoint:** `POST /api/modules/create`

**Headers:**
```
Authorization: Bearer {{jwt_token}}
Content-Type: application/json
```

**Request Body:**
```json
{
    "moduleId": "CS101",
    "moduleName": "Introduction to Computer Science",
    "description": "Fundamental concepts of computer science"
}
```

---

### Step 4: Add Learning Outcome (Lecturer Required)

Login as **danu1** (Lecturer) first!

**Endpoint:** `POST /api/lospos/CS101/add`

**Headers:**
```
Authorization: Bearer {{jwt_token}}
Content-Type: application/json
```

**Request Body:**
```json
{
    "id": "LO1",
    "name": "Understanding Basic Algorithms",
    "description": "Students will be able to understand and implement basic sorting and searching algorithms"
}
```

---

### Step 5: Create LO-PO Mappings (Lecturer Required)

**Endpoint:** `POST /api/lo-po-mapping/create?loId=LO1`

**Headers:**
```
Authorization: Bearer {{jwt_token}}
Content-Type: application/json
```

**Request Body:**
```json
{
    "mappings": {
        "PO1": 3,
        "PO2": 3,
        "PO3": 2,
        "PO4": 1
    },
    "remarks": "Strong correlation with engineering knowledge and problem analysis"
}
```

**Mapping Weights:**
- `1` = Low correlation
- `2` = Medium correlation
- `3` = High correlation

---

### Step 6: Import Student Marks (Lecturer Required)

**Endpoint:** `POST /api/lospos/LO1/marks/import-obe`

**Headers:**
```
Authorization: Bearer {{jwt_token}}
```

**Body Type:** `form-data`

**Form Data:**
- `excelFile`: (Select your Excel file)
- `batch`: `22` (or your batch year)
- `loNumber`: `1` (optional)

**Excel File Format Example:**

| Index No | LO1 Marks |
|----------|-----------|
| 2019/CS/001 | 85 |
| 2019/CS/002 | 90 |
| 2019/CS/003 | 75 |

---

## Complete Workflow Example

### Scenario: Setting up a complete OBE system for CS101 module

1. **Login as Superadmin (danu2)**
   ```
   POST /api/auth/login
   Body: {"userID": "danu2", "password": "1234"}
   ```

2. **Initialize Program Outcomes**
   ```
   POST /api/program-outcomes/initialize-defaults
   ```

3. **Create Module CS101**
   ```
   POST /api/modules/create
   Body: {"moduleId": "CS101", "moduleName": "Intro to CS", ...}
   ```

4. **Logout and Login as Lecturer (danu1)**
   ```
   POST /api/auth/login
   Body: {"userID": "danu1", "password": "1234"}
   ```

5. **Add Learning Outcomes (LO1, LO2, LO3)**
   ```
   POST /api/lospos/CS101/add
   Body: {"id": "LO1", "name": "...", ...}
   ```

6. **Create LO-PO Mappings**
   ```
   POST /api/lo-po-mapping/create?loId=LO1
   Body: {"mappings": {"PO1": 3, "PO2": 2}, ...}
   ```

7. **Import Student Marks**
   ```
   POST /api/lospos/LO1/marks/import-obe
   Form-data: excelFile, batch
   ```

8. **View Reports**
   ```
   GET /api/obe/reports/course/CS101
   ```

---

## Quick Testing Checklist

- [ ] Login successful with danu1 (Lecturer)
- [ ] Login successful with danu (Admin)
- [ ] Initialize Program Outcomes (12 POs created)
- [ ] Create module CS101
- [ ] Get all modules (CS101 appears)
- [ ] Add LO1 to CS101
- [ ] Get LOs for CS101 (LO1 appears)
- [ ] Create LO-PO mappings for LO1
- [ ] Get mappings for LO1 (mappings appear)
- [ ] View module overview

---

## Common Endpoints Reference

### Authentication
- `POST /api/auth/login` - Login (no auth required)

### Modules
- `GET /api/modules/all` - List all modules
- `POST /api/modules/create` - Create module (Admin)
- `GET /api/modules/{id}` - Get module details

### Program Outcomes
- `GET /api/program-outcomes/all` - List all POs
- `POST /api/program-outcomes/initialize-defaults` - Initialize default POs (Admin)

### Learning Outcomes
- `GET /api/lospos/module/{moduleId}` - Get LOs for module
- `POST /api/lospos/{moduleId}/add` - Add LO (Lecturer)
- `GET /api/lospos/{loId}/marks` - Get marks for LO
- `POST /api/lospos/{loId}/marks/import-obe` - Import marks (Lecturer)

### LO-PO Mappings
- `POST /api/lo-po-mapping/create?loId={loId}` - Create mappings (Lecturer)
- `GET /api/lo-po-mapping/module/{moduleId}` - Get module mappings
- `GET /api/lo-po-mapping/admin/pending` - Get pending approvals (Admin)
- `PUT /api/lo-po-mapping/admin/{id}/approve` - Approve mapping (Admin)

### Reports
- `GET /api/obe/reports/course/{moduleId}` - PO attainment report
- `GET /api/obe/analysis/trend/{moduleId}` - Trend analysis
- `POST /api/obe/export/marks` - Export marks to Excel

---

## Troubleshooting

### Issue: "401 Unauthorized"
**Solution:** Make sure you're logged in and the `{{jwt_token}}` variable is set. Check the Authorization header.

### Issue: "403 Forbidden"
**Solution:** You're logged in but don't have permission. Check if you're using the correct user role:
- Admin operations: Use **danu** or **danu2**
- Lecturer operations: Use **danu1**

### Issue: "Could not resolve placeholder 'jwt.secret'"
**Solution:** Check that `application.properties` contains:
```properties
jwt.secret=YourSecretKeyForJWTTokenMustBeAtLeast32CharactersLongForHS256Algorithm
```

### Issue: Foreign key constraint errors
**Solution:** Drop and recreate the database:
```sql
DROP DATABASE lopo;
CREATE DATABASE lopo;
```
Then restart the application.

---

## Response Format

Most endpoints return this format:

**Success:**
```json
{
    "status": "SUCCESS",
    "message": "Operation completed successfully",
    "data": { ... }
}
```

**Error:**
```json
{
    "status": "ERROR",
    "message": "Error description",
    "data": null
}
```

---

## Tips

1. **Use Environment Variables** in Postman for `base_url` and `jwt_token`
2. **The collection auto-saves tokens** - just run the login request!
3. **Test in order** - Initialize POs → Create Module → Add LOs → Create Mappings
4. **Check console logs** in Postman to see saved tokens
5. **Use Postman Tests tab** to see automatic token saving scripts

---

## Need Help?

- Check application logs in your IDE
- Verify database connection on port 3307
- Ensure Spring Boot application is running
- Check that users exist in database (danu, danu1, danu2)
