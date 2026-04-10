# Testing Guide - Excel Export Feature

## Overview
Complete testing guide for the new Excel export functionality with mark types and multiple LOs.

---

## Prerequisites

1. **Database Setup:**
   - Database migrated with `mark_type` column
   - Sample data imported (students, modules, LOs, marks)

2. **Application Running:**
   ```bash
   java -jar target/Software-project-Backend-0.0.1-SNAPSHOT.jar
   ```

3. **Testing Tools:**
   - Postman or curl for API testing
   - Excel reader (Excel, LibreOffice, Google Sheets)
   - Browser developer console

---

## Test Data Preparation

### 1. Create Sample Students
```sql
INSERT INTO student (student_id, student_name, email) VALUES
('EN001', 'Alice Johnson', 'alice@example.com'),
('EN002', 'Bob Smith', 'bob@example.com'),
('EN003', 'Charlie Brown', 'charlie@example.com'),
('EN004', 'Diana Prince', 'diana@example.com'),
('EN005', 'Eve Wilson', 'eve@example.com');
```

### 2. Create Sample Module
```sql
INSERT INTO module (module_id, module_name) VALUES
('CSC101', 'Introduction to Programming');
```

### 3. Create Sample LOs
```sql
INSERT INTO los (id, name, description, module_id) VALUES
('LO1', 'Understanding Variables', 'Learn about programming variables', 'CSC101'),
('LO2', 'Control Flow', 'Learn about loops and conditionals', 'CSC101'),
('LO3', 'Functions', 'Learn about function creation and calls', 'CSC101');
```

### 4. Import Sample Marks (FINAL_EXAM)
```sql
INSERT INTO student_mark (student_id, los_id, score, batch, mark_type) VALUES
-- Student EN001
('EN001', 'LO1', 85.5, '24', 'FINAL_EXAM'),
('EN001', 'LO2', 90.0, '24', 'FINAL_EXAM'),
('EN001', 'LO3', 88.5, '24', 'FINAL_EXAM'),
-- Student EN002
('EN002', 'LO1', 45.0, '24', 'FINAL_EXAM'),
('EN002', 'LO2', 55.5, '24', 'FINAL_EXAM'),
('EN002', 'LO3', 48.0, '24', 'FINAL_EXAM'),
-- Student EN003
('EN003', 'LO1', 92.0, '24', 'FINAL_EXAM'),
('EN003', 'LO2', 88.0, '24', 'FINAL_EXAM'),
('EN003', 'LO3', 95.0, '24', 'FINAL_EXAM'),
-- Student EN004
('EN004', 'LO1', 35.0, '24', 'FINAL_EXAM'),
('EN004', 'LO2', 40.0, '24', 'FINAL_EXAM'),
('EN004', 'LO3', 38.0, '24', 'FINAL_EXAM'),
-- Student EN005
('EN005', 'LO1', 75.5, '24', 'FINAL_EXAM'),
('EN005', 'LO2', 82.0, '24', 'FINAL_EXAM'),
('EN005', 'LO3', 78.5, '24', 'FINAL_EXAM');
```

### 5. Import Sample Marks (ASSIGNMENT)
```sql
INSERT INTO student_mark (student_id, los_id, score, batch, mark_type) VALUES
-- Student EN001
('EN001', 'LO1', 95.0, '24', 'ASSIGNMENT'),
('EN001', 'LO2', 92.5, '24', 'ASSIGNMENT'),
('EN001', 'LO3', 90.0, '24', 'ASSIGNMENT'),
-- Student EN002
('EN002', 'LO1', 60.0, '24', 'ASSIGNMENT'),
('EN002', 'LO2', 65.0, '24', 'ASSIGNMENT'),
('EN002', 'LO3', 62.0, '24', 'ASSIGNMENT'),
-- Student EN003
('EN003', 'LO1', 88.0, '24', 'ASSIGNMENT'),
('EN003', 'LO2', 90.5, '24', 'ASSIGNMENT'),
('EN003', 'LO3', 92.0, '24', 'ASSIGNMENT'),
-- Student EN004
('EN004', 'LO1', 50.0, '24', 'ASSIGNMENT'),
('EN004', 'LO2', 48.5, '24', 'ASSIGNMENT'),
('EN004', 'LO3', 52.0, '24', 'ASSIGNMENT'),
-- Student EN005
('EN005', 'LO1', 85.0, '24', 'ASSIGNMENT'),
('EN005', 'LO2', 88.0, '24', 'ASSIGNMENT'),
('EN005', 'LO3', 86.5, '24', 'ASSIGNMENT');
```

### 6. Get Authentication Token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "lecture_user",
    "password": "password123"
  }'
```

Response will include token:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Login successful"
}
```

---

## Test Cases

### Test 1: Export FINAL_EXAM marks with default threshold

**Test ID:** TEST_EXPORT_001  
**Description:** Export final exam marks for all 3 LOs with default threshold (50)

**Request:**
```bash
curl -X POST http://localhost:8080/api/obe/export/marks \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "losIds": ["LO1", "LO2", "LO3"],
    "markType": "FINAL_EXAM",
    "batch": "24"
  }'
```

**Expected Results:**
- Status: 200 OK
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Filename: `marks_report_24_final_exam.xlsx`
- File contains:
  - Header row: Index Number | LO1 | LO2 | LO3
  - 5 student rows with Pass/Fail status
  - EN001: Pass, Pass, Pass
  - EN002: Fail, Pass, Fail
  - EN003: Pass, Pass, Pass
  - EN004: Fail, Fail, Fail
  - EN005: Pass, Pass, Pass

**Verification Checklist:**
- [ ] File downloads successfully
- [ ] File is valid Excel format
- [ ] File contains all expected students
- [ ] Pass/Fail status is correct
- [ ] Column headers show LO names
- [ ] Formatting is correct (colors, borders)

---

### Test 2: Export ASSIGNMENT marks with custom threshold

**Test ID:** TEST_EXPORT_002  
**Description:** Export assignment marks with threshold = 70

**Request:**
```bash
curl -X POST http://localhost:8080/api/obe/export/marks \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "losIds": ["LO1", "LO2", "LO3"],
    "markType": "ASSIGNMENT",
    "batch": "24",
    "threshold": 70
  }'
```

**Expected Results:**
- Status: 200 OK
- Filename: `marks_report_24_assignment.xlsx`
- File contains:
  - EN001: Pass, Pass, Pass
  - EN002: Fail, Fail, Fail
  - EN003: Pass, Pass, Pass
  - EN004: Fail, Fail, Fail
  - EN005: Pass, Pass, Pass

---

### Test 3: Export single LO

**Test ID:** TEST_EXPORT_003  
**Description:** Export only LO1 marks

**Request:**
```bash
curl -X POST http://localhost:8080/api/obe/export/marks \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "losIds": ["LO1"],
    "markType": "FINAL_EXAM",
    "batch": "24"
  }'
```

**Expected Results:**
- Status: 200 OK
- File contains only:
  - Index Number | LO1

---

### Test 4: Error - Missing LO IDs

**Test ID:** TEST_ERROR_001  
**Description:** Try to export without specifying LO IDs

**Request:**
```bash
curl -X POST http://localhost:8080/api/obe/export/marks \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "losIds": [],
    "markType": "FINAL_EXAM",
    "batch": "24"
  }'
```

**Expected Response (400):**
```json
{
  "message": "Error: losIds list cannot be empty",
  "status": "ERROR"
}
```

---

### Test 5: Error - Invalid Mark Type

**Test ID:** TEST_ERROR_002  
**Description:** Try to export with invalid mark type

**Request:**
```bash
curl -X POST http://localhost:8080/api/obe/export/marks \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "losIds": ["LO1"],
    "markType": "INVALID_TYPE",
    "batch": "24"
  }'
```

**Expected Response (400):**
```json
{
  "message": "Error: Invalid markType. Must be FINAL_EXAM or ASSIGNMENT",
  "status": "ERROR"
}
```

---

### Test 6: Error - Missing Batch

**Test ID:** TEST_ERROR_003  
**Description:** Try to export without batch

**Request:**
```bash
curl -X POST http://localhost:8080/api/obe/export/marks \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "losIds": ["LO1"],
    "markType": "FINAL_EXAM"
  }'
```

**Expected Response (400):**
```json
{
  "message": "Error: batch is required",
  "status": "ERROR"
}
```

---

### Test 7: Error - Unauthorized Access

**Test ID:** TEST_ERROR_004  
**Description:** Try to export without authentication

**Request:**
```bash
curl -X POST http://localhost:8080/api/obe/export/marks \
  -H "Content-Type: application/json" \
  -d '{
    "losIds": ["LO1"],
    "markType": "FINAL_EXAM",
    "batch": "24"
  }'
```

**Expected Response (401):**
```json
{
  "message": "Unauthorized",
  "status": "ERROR"
}
```

---

### Test 8: Error - Insufficient Permissions

**Test ID:** TEST_ERROR_005  
**Description:** Try to export as student user

**Request:**
```bash
curl -X POST http://localhost:8080/api/obe/export/marks \
  -H "Authorization: Bearer STUDENT_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "losIds": ["LO1"],
    "markType": "FINAL_EXAM",
    "batch": "24"
  }'
```

**Expected Response (403):**
```json
{
  "message": "Access Denied: Only Lecturers/Admins can export marks",
  "status": "ERROR"
}
```

---

### Test 9: Performance Test - Large Dataset

**Test ID:** TEST_PERF_001  
**Description:** Export with 500 students and 10 LOs

**Request:**
```bash
curl -X POST http://localhost:8080/api/obe/export/marks \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "losIds": ["LO1", "LO2", "LO3", "LO4", "LO5", "LO6", "LO7", "LO8", "LO9", "LO10"],
    "markType": "FINAL_EXAM",
    "batch": "24"
  }'
```

**Expected Results:**
- Response time: < 2 seconds
- Status: 200 OK
- File size: < 5MB
- All data correctly populated

---

### Test 10: Threshold Boundary Test

**Test ID:** TEST_BOUNDARY_001  
**Description:** Test with score exactly at threshold

**Test Setup:**
```sql
INSERT INTO student_mark (student_id, los_id, score, batch, mark_type) VALUES
('EN006', 'LO1', 50.0, '24', 'FINAL_EXAM');
```

**Request:**
```bash
curl -X POST http://localhost:8080/api/obe/export/marks \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "losIds": ["LO1"],
    "markType": "FINAL_EXAM",
    "batch": "24",
    "threshold": 50
  }'
```

**Expected Results:**
- EN006 score of 50.0 should show as "Pass (50.00)"
- Green cell (Pass)

---

## Excel File Validation Checklist

After downloading each file, verify:

- [ ] File opens in Excel without errors
- [ ] All student records present
- [ ] All LO columns present
- [ ] Header row properly formatted (blue background, white text)
- [ ] Pass cells have green background
- [ ] Fail cells have red background
- [ ] Scores displayed as numbers with 2 decimal places
- [ ] No corruption or missing data
- [ ] Column widths are readable
- [ ] Borders are visible on all cells

---

## Automated Testing (Unit Tests)

### Sample Unit Test
```java
@SpringBootTest
@RunWith(SpringRunner.class)
public class ExcelExportServiceTest {
    
    @Autowired
    private ExcelExportService exportService;
    
    @Test
    public void testGenerateMarksExcel() throws IOException {
        List<String> losIds = Arrays.asList("LO1", "LO2");
        byte[] result = exportService.generateMarksExcel(
            losIds, "FINAL_EXAM", "24", 50
        );
        
        assertNotNull(result);
        assertTrue(result.length > 0);
        // Verify file is valid Excel
        assertTrue(result[0] == 0x50); // 'P' in PK signature
    }
    
    @Test
    public void testThresholdCalculation() throws IOException {
        byte[] result = exportService.generateMarksExcel(
            Arrays.asList("LO1"), "FINAL_EXAM", "24", 75
        );
        
        assertNotNull(result);
        // Score 75 should be Pass, 74.99 should be Fail
    }
}
```

---

## Integration Testing with Postman

### Import Collection
1. Create new Postman collection
2. Add requests from each test case above
3. Set environment variables:
   - `base_url`: http://localhost:8080
   - `token`: Your JWT token
4. Run collection sequentially

### Expected Test Results
- 10/10 test cases should pass
- All error cases should return expected error messages
- All success cases should return valid Excel files

---

## Regression Testing Checklist

After each code change:

- [ ] All export tests pass
- [ ] Import endpoint still works
- [ ] Database queries execute quickly
- [ ] No memory leaks (monitor heap)
- [ ] File downloads without corruption
- [ ] Excel file valid in multiple applications
- [ ] Authorization still works correctly
- [ ] Error handling unchanged

---

## Performance Benchmarks

Expected performance metrics:

| Metric | Target | Actual |
|--------|--------|--------|
| 100 students, 3 LOs | < 200ms | |
| 500 students, 10 LOs | < 1000ms | |
| 1000 students, 20 LOs | < 3000ms | |
| File size (500 students) | < 2MB | |
| Memory usage | < 100MB | |

---

## Troubleshooting Test Failures

### Test fails with "No data"
- Check database has sample data
- Verify batch matches in request and database
- Verify mark_type values are correct

### Test fails with "Invalid token"
- Get fresh token
- Verify user has Lecture or Admin role
- Check token expiration

### File corruption
- Check Excel output is valid
- Try opening in different application
- Check server logs for errors

---

## Test Report Template

```
Test Date: [DATE]
Tester: [NAME]
Build Version: [VERSION]

Test Results Summary:
- Total Tests: 10
- Passed: [X]
- Failed: [X]
- Skipped: [X]

Known Issues:
- [Issue 1]
- [Issue 2]

Sign-off:
Approved by: _______________
Date: _______________
```

---

## Conclusion

All tests should pass before the feature is considered ready for production. Document any failures and follow the troubleshooting steps to resolve them.
