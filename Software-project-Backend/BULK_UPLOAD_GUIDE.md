# ✅ BULK MARKS UPLOAD - Single Excel File for All LOs

## What's New?

**NEW FEATURE:** Upload marks for ALL LOs in ONE request!

No need to upload separately for each LO. Just upload once!

---

## Quick Workflow

### 1. Login
```
POST /api/auth/login
{"userID": "danu1", "password": "1234"}
```

### 2. Get Learning Outcomes
```
GET /api/lospos/module/EC6306
```
Response: LO1, LO2, LO3

### 3. Download Template
```
POST /api/obe/template/marks
{
    "losIds": ["LO1", "LO2", "LO3"],
    "markType": "FINAL_EXAM",
    "batch": "22"
}
```
Use "Send and Download" in Postman

### 4. Fill Excel
| Student Index | LO1  | LO2  | LO3  |
|---------------|------|------|------|
| EN001         | 85.5 | 90.0 | 88.0 |
| EN002         | 78.0 | 82.5 | 91.0 |

### 5. Upload (ONE REQUEST)
```
POST /api/obe/marks/upload-bulk

Form-data:
- excelFile: [your filled Excel file]
- losIds: LO1,LO2,LO3
- batch: 22
- markType: FINAL_EXAM
```

**Done! All marks uploaded at once!** ✅

---

## Detailed Steps

### Step 1: Prepare Excel File

Download template and fill with data:

| Student Index | LO1  | LO2  | LO3  |
|---------------|------|------|------|
| EN001         | 85.5 | 90.0 | 88.0 |
| EN002         | 78.0 | 82.5 | 91.0 |
| EN003         | 92.0 | 88.0 | 85.5 |
| EN004         | 75.5 | 80.0 | 78.0 |

Save the file.

### Step 2: Upload in Postman

**Request:** Upload All Marks (Bulk Upload)

**Method:** POST

**URL:** `http://localhost:8080/api/obe/marks/upload-bulk`

**Headers:**
```
Authorization: Bearer {your_token}
```

**Body:** form-data

| Key | Value | Type |
|-----|-------|------|
| excelFile | [Select your file] | File |
| losIds | LO1,LO2,LO3 | Text |
| batch | 22 | Text |
| markType | FINAL_EXAM | Text |

**Click Send!**

---

## Important: losIds Parameter

The `losIds` parameter must match your Excel column order:

**Example 1:**
```
Excel: Student Index | LO1 | LO2 | LO3
losIds: LO1,LO2,LO3
```

**Example 2:**
```
Excel: Student Index | LO3 | LO1 | LO2
losIds: LO3,LO1,LO2
```

**The order matters!** LO IDs must match left-to-right column order.

---

## Response Examples

### Success Response
```json
{
    "message": "Successfully imported 12 marks for 3 LOs",
    "status": "SUCCESS",
    "data": {
        "losCount": 3,
        "batch": "22",
        "markType": "FINAL_EXAM"
    }
}
```

### Error Response
```json
{
    "message": "Failed to import marks: Learning Outcome not found: LO4",
    "error": "Learning Outcome not found: LO4",
    "status": "ERROR"
}
```

---

## Postman Collection

**File:** `OBE_Lecturer_Marks_Upload.json`

**Requests:**
1. Login as Lecturer
2. Get Module EC6306 Details
3. Get Learning Outcomes for EC6306
4. Download Excel Template
5. **Upload All Marks (Bulk Upload)** ← NEW!
6. Verify Uploaded Marks
7. Get Marks by Batch

---

## Common Issues

### Issue: "Learning Outcome not found"

**Cause:** LO ID doesn't exist in database

**Solution:**
1. Get actual LO IDs from Step 3
2. Use exact LO IDs (case-sensitive)
3. Make sure LOs exist for EC6306 module

### Issue: "Invalid losIds format"

**Cause:** losIds parameter is wrong

**Solution:**
- Use comma-separated format: `LO1,LO2,LO3`
- No spaces: ❌ `LO1, LO2, LO3`
- Correct: ✅ `LO1,LO2,LO3`
- Match column order in Excel

### Issue: "Some marks not imported"

**Cause:** Empty cells or invalid values

**Solution:**
- Fill all mark cells with numbers 0-100
- Use decimal format: 85.5, not 85,5
- Don't leave blank cells (use 0 if needed)

---

## Comparison: Old vs New Method

### ❌ Old Method (3 separate uploads)
```
1. Upload LO1 → POST /api/lospos/LO1/marks/import-obe
2. Upload LO2 → POST /api/lospos/LO2/marks/import-obe
3. Upload LO3 → POST /api/lospos/LO3/marks/import-obe
```

### ✅ New Method (1 bulk upload)
```
1. Upload All → POST /api/obe/marks/upload-bulk
```

**Much simpler!**

---

## Excel Format

### Required Structure

**Column 0:** Student Index (text)
**Column 1:** First LO marks (number)
**Column 2:** Second LO marks (number)
**Column 3:** Third LO marks (number)
...

### Valid Mark Values

- Numbers: `85.5`, `90.0`, `78.25`
- Range: 0-100
- Format: Decimal (use `.` not `,`)

### Invalid Values (Will be skipped)

- Empty cells
- Text: `"Absent"`, `"N/A"`
- Special codes: `AB`, `MC`

---

## Testing Checklist

- [ ] Login successful
- [ ] Got LO IDs from module EC6306
- [ ] Downloaded template
- [ ] Filled Excel with student data
- [ ] losIds matches column order
- [ ] Uploaded with bulk endpoint
- [ ] Success response received
- [ ] Verified marks in system

---

## Quick Reference

**Endpoint:** `POST /api/obe/marks/upload-bulk`

**Parameters:**
- `excelFile` (File) - Your filled Excel template
- `losIds` (Text) - Comma-separated LO IDs (e.g., LO1,LO2,LO3)
- `batch` (Text) - Batch year (e.g., 22)
- `markType` (Text) - FINAL_EXAM or ASSIGNMENT

**Headers:**
- `Authorization: Bearer {token}`

**Response:** JSON with success/error message

---

## Summary

✅ **One upload for all LOs**
✅ **Single Excel file**
✅ **Faster and simpler**
✅ **No need to repeat uploads**
✅ **All marks imported at once**

**Import the updated Postman collection and try it!** 🚀
