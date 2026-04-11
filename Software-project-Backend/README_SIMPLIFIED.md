# OBE System - Lecturer Marks Upload

## Quick Setup

### 1. Import Postman Collection
**File:** `OBE_Lecturer_Marks_Upload.json`

In Postman:
1. Click **Import**
2. Select `OBE_Lecturer_Marks_Upload.json`
3. Click **Import**

### 2. Run the Workflow

The collection has **9 requests** in order:

✅ **1. Login as Lecturer** (danu1/1234)  
✅ **2. Get Module EC6306 Details**  
✅ **3. Get Learning Outcomes for EC6306**  
✅ **4. Download Excel Template**  
✅ **5-7. Upload Marks** (for each LO)  
✅ **8-9. Verify Uploaded Marks**

---

## Step-by-Step

### Step 1: Login
Click **"1. Login as Lecturer"** → Send

Token is automatically saved!

### Step 2-3: View Module
Click **"2. Get Module EC6306 Details"** → Send  
Click **"3. Get Learning Outcomes"** → Send

Note down the LO IDs (e.g., LO1, LO2, LO3)

### Step 4: Download Template
Click **"4. Download Excel Template"** → Update request body with your LO IDs → Send

Template downloads as: `mark_template_22_final_exam.xlsx`

### Step 5: Fill Excel (Outside Postman)
Open the downloaded Excel file:

| Student Index | LO1  | LO2  | LO3  |
|---------------|------|------|------|
| EN001         | 85.5 | 90.0 | 88.0 |
| EN002         | 78.0 | 82.5 | 91.0 |

Save the file!

### Step 6: Upload Marks
Click **"5. Upload Marks (LO1)"**:
1. Go to **Body** tab
2. Click **Select Files** for `excelFile`
3. Choose your filled Excel file
4. Update `batch` if needed
5. Click **Send**

Repeat for:
- **"6. Upload Marks (LO2)"** (loNumber=2)
- **"7. Upload Marks (LO3)"** (loNumber=3)

### Step 7: Verify
Click **"8. Verify Uploaded Marks"** → Send  
Click **"9. Get Marks by Batch"** → Send

Done! ✅

---

## Important Notes

### Login User
- **Username:** danu1
- **Password:** 1234
- **Role:** Lecturer

### Module
- **Module ID:** EC6306

### Excel Template Parameters
- **losIds:** List of LO IDs (e.g., ["LO1", "LO2", "LO3"])
- **markType:** "FINAL_EXAM" or "ASSIGNMENT"
- **batch:** Batch year (e.g., "22", "23", "24")

### Upload Parameters
- **excelFile:** Your filled Excel template
- **batch:** Must match template (e.g., "22")
- **loNumber:** Column number (1, 2, 3...)

---

## Template Structure

Downloaded Excel file has:

**Sheet 1: Mark Template**
```
┌───────────────┬─────┬─────┬─────┐
│ Student Index │ LO1 │ LO2 │ LO3 │
├───────────────┼─────┼─────┼─────┤
│               │     │     │     │  ← 10 empty rows
│               │     │     │     │
└───────────────┴─────┴─────┴─────┘
```

**Sheet 2: Instructions**
- Step-by-step filling guide
- Format requirements
- Common mistakes to avoid

---

## API Endpoints Used

| Step | Method | Endpoint |
|------|--------|----------|
| Login | POST | /api/auth/login |
| Module | GET | /api/modules/EC6306 |
| LOs | GET | /api/lospos/module/EC6306 |
| Template | POST | /api/obe/template/marks |
| Upload | POST | /api/lospos/{loId}/marks/import-obe |
| Verify | GET | /api/lospos/{loId}/marks |

---

## Troubleshooting

**"401 Unauthorized"**
→ Run Login request first (Request 1)

**"Module EC6306 not found"**
→ Check module exists in database

**"LO not found"**
→ Use correct LO IDs from Request 3

**"Invalid Excel format"**
→ Use template from Request 4, don't modify headers

**"Marks not showing"**
→ Check batch values match in all requests

---

## Files Overview

| File | Purpose |
|------|---------|
| `OBE_Lecturer_Marks_Upload.json` | Postman collection (IMPORT THIS) |
| `LECTURER_MARKS_UPLOAD_GUIDE.md` | Detailed guide |
| `API_QUICK_REFERENCE.md` | Quick command reference |
| `TEMPLATE_FEATURE_GUIDE.md` | Complete Excel feature docs |

---

## Need More Help?

See detailed guide: **LECTURER_MARKS_UPLOAD_GUIDE.md**

Base URL: **http://localhost:8080**

Start with: **Import OBE_Lecturer_Marks_Upload.json**
