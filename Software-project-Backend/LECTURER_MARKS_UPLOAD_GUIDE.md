# Lecturer Marks Upload Guide - Module EC6306

## Quick Start

**File to Import:** `OBE_Lecturer_Marks_Upload.json`

**User:** danu1 / 1234 (Lecturer)

**Module:** EC6306

---

## Step-by-Step Process

### Step 1: Login as Lecturer
```
POST http://localhost:8080/api/auth/login

{
    "userID": "danu1",
    "password": "1234"
}
```

**Response:** JWT token (automatically saved)

---

### Step 2: View Module EC6306
```
GET http://localhost:8080/api/modules/EC6306
Headers: Authorization: Bearer {token}
```

**Response:** Module details

---

### Step 3: Get Learning Outcomes for EC6306
```
GET http://localhost:8080/api/lospos/module/EC6306
```

**Response:** List of LOs (e.g., LO1, LO2, LO3)

**Note:** You'll need these LO IDs for the next steps!

---

### Step 4: Download Excel Template
```
POST http://localhost:8080/api/obe/template/marks
Headers: Authorization: Bearer {token}

{
    "losIds": ["LO1", "LO2", "LO3"],
    "markType": "FINAL_EXAM",
    "batch": "22"
}
```

**Response:** Excel file downloads
- Filename: `mark_template_22_final_exam.xlsx`
- Contains: Student Index | LO1 | LO2 | LO3

**Parameters:**
- **losIds**: List of LO IDs from Step 3
- **markType**: `FINAL_EXAM` or `ASSIGNMENT`
- **batch**: Batch year (e.g., "22", "23", "24")

---

### Step 5: Fill Excel Template

**Open the downloaded Excel file:**

| Student Index | LO1 | LO2 | LO3 |
|---------------|-----|-----|-----|
| EN001 | 85.5 | 90.0 | 88.0 |
| EN002 | 78.0 | 82.5 | 91.0 |
| EN003 | 92.0 | 88.0 | 85.5 |

**Rules:**
- Enter marks between 0-100
- Use decimal format (85.50)
- Don't change header row
- Save as Excel (.xlsx)

---

### Step 6: Upload Marks for Each LO

**Upload for LO1:**
```
POST http://localhost:8080/api/lospos/LO1/marks/import-obe
Headers: Authorization: Bearer {token}
Body: form-data
- excelFile: [Select your filled Excel file]
- batch: 22
- loNumber: 1
```

**Upload for LO2:**
```
POST http://localhost:8080/api/lospos/LO2/marks/import-obe
Headers: Authorization: Bearer {token}
Body: form-data
- excelFile: [Select same Excel file]
- batch: 22
- loNumber: 2
```

**Upload for LO3:**
```
POST http://localhost:8080/api/lospos/LO3/marks/import-obe
Headers: Authorization: Bearer {token}
Body: form-data
- excelFile: [Select same Excel file]
- batch: 22
- loNumber: 3
```

**Important:**
- Use the **same Excel file** for all LOs
- Change `loNumber` (1, 2, 3...) for each LO column
- Change URL path (LO1, LO2, LO3...) to match

---

### Step 7: Verify Uploaded Marks

```
GET http://localhost:8080/api/lospos/LO1/marks
GET http://localhost:8080/api/lospos/LO1/batches/22/marks
```

---

## Postman Instructions

### 1. Import Collection
1. Open Postman
2. Click **Import**
3. Select file: `OBE_Lecturer_Marks_Upload.json`
4. Click **Import**

### 2. Run Requests in Order

**Request 1:** Login as Lecturer  
→ Token saved automatically

**Request 2:** Get Module EC6306 Details  
→ View module info

**Request 3:** Get Learning Outcomes for EC6306  
→ Note down LO IDs

**Request 4:** Download Excel Template  
→ Update losIds with actual LO IDs  
→ Excel file downloads

**Request 5-7:** Upload Marks  
→ Select your filled Excel file  
→ Run for each LO (LO1, LO2, LO3)

**Request 8-9:** Verify Marks  
→ Check uploaded data

---

## Complete Example

### Scenario: Upload marks for EC6306 with 3 LOs

**1. Login**
```json
{"userID": "danu1", "password": "1234"}
```

**2. Get LOs**
```
GET /api/lospos/module/EC6306
Response: [{"id": "LO1"}, {"id": "LO2"}, {"id": "LO3"}]
```

**3. Download Template**
```json
{
    "losIds": ["LO1", "LO2", "LO3"],
    "markType": "FINAL_EXAM",
    "batch": "22"
}
```

**4. Fill Excel**
```
EN001 | 85.5 | 90.0 | 88.0
EN002 | 78.0 | 82.5 | 91.0
```

**5. Upload (repeat for each LO)**
```
LO1: loNumber=1
LO2: loNumber=2
LO3: loNumber=3
```

---

## Tips

✅ **Always download fresh template** for each upload session  
✅ **Use same Excel file** for all LOs in one module  
✅ **Change loNumber** to match the column (1, 2, 3...)  
✅ **Match LO ID** in URL path (LO1, LO2, LO3...)  
✅ **Verify after upload** using verification requests  

---

## Troubleshooting

**Issue:** "401 Unauthorized"  
**Solution:** Run Login request first

**Issue:** "Module not found"  
**Solution:** Check module ID is exactly "EC6306"

**Issue:** "LO not found"  
**Solution:** Get actual LO IDs from Step 3

**Issue:** "Invalid Excel format"  
**Solution:** Use template from Step 4, don't modify headers

**Issue:** "Marks not appearing"  
**Solution:** Check batch value matches in all requests

---

## Quick Reference

**Base URL:** http://localhost:8080

**Lecturer:** danu1 / 1234

**Module:** EC6306

**Endpoints:**
- Login: `POST /api/auth/login`
- Module: `GET /api/modules/EC6306`
- LOs: `GET /api/lospos/module/EC6306`
- Template: `POST /api/obe/template/marks`
- Upload: `POST /api/lospos/{loId}/marks/import-obe`
- Verify: `GET /api/lospos/{loId}/marks`
