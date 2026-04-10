# Excel Template Feature - User Guide

## Overview
Users can now download an empty Excel template with LO headers, fill in student marks, and upload the file back to the system.

---

## 📋 Two-Part Workflow

### Part 1: Download Template
1. Select Learning Outcomes (LOs)
2. Select mark type (FINAL_EXAM or ASSIGNMENT)
3. Select batch year
4. Click **"Download Template"**
5. Excel file downloads with:
   - Student Index column
   - Columns for each selected LO
   - 10 empty rows to fill in
   - Instructions sheet with guidelines

### Part 2: Upload Marks
1. Open downloaded template in Excel
2. Fill in student indices (EN001, EN002, etc.)
3. Fill in marks for each LO (0-100)
4. Save the file
5. Upload back to the system
6. System validates and imports the marks

---

## 🎯 API Endpoints

### 1. Generate Template
**Endpoint:** `POST /api/obe/template/marks`

**Request:**
```json
{
  "losIds": ["LO1", "LO2", "LO3"],
  "markType": "FINAL_EXAM",
  "batch": "24"
}
```

**Response:**
- File: `mark_template_24_final_exam.xlsx`
- Contains 2 sheets:
  - Sheet 1: "Mark Template" (empty rows for data entry)
  - Sheet 2: "Instructions" (usage guidelines)

### 2. Upload Marks
**Endpoint:** `POST /api/lospos/{loId}/marks/import-obe`
(Existing endpoint - now accepts filled template)

**Request:**
```
POST /api/lospos/{loId}/marks/import-obe
- Parameter: excelFile (the filled template)
- Parameter: batch
- Parameter: loNumber (optional)
- Header: markType (FINAL_EXAM or ASSIGNMENT)
```

---

## 📑 Template Structure

### Mark Template Sheet
```
┌──────────────┬──────────┬──────────┬──────────┐
│Student Index │   LO1    │   LO2    │   LO3    │
├──────────────┼──────────┼──────────┼──────────┤
│              │          │          │          │
│              │          │          │          │
│              │          │          │          │
└──────────────┴──────────┴──────────┴──────────┘
```

**Cells colored in light yellow** = User input area

### Instructions Sheet
Provides step-by-step instructions:
1. Fill Student Index column
2. Fill marks (0-100)
3. Use decimal format (85.50)
4. Don't modify headers
5. Don't change column order
6. Save as .xlsx
7. Upload back to system

---

## 💡 Example: Complete Workflow

### Step 1: Download Template
```bash
curl -X POST http://localhost:8080/api/obe/template/marks \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "losIds": ["LO1", "LO2"],
    "markType": "FINAL_EXAM",
    "batch": "24"
  }' -o template.xlsx
```

### Step 2: Fill Template in Excel
```
Student Index | LO1  | LO2
EN001        | 85.5 | 90.0
EN002        | 45.0 | 55.5
EN003        | 92.0 | 88.0
```

### Step 3: Upload Filled Template
```bash
curl -X POST http://localhost:8080/api/lospos/LO1/marks/import-obe \
  -H "Authorization: Bearer TOKEN" \
  -F "excelFile=@template.xlsx" \
  -F "batch=24" \
  -F "loNumber=LO1"
```

---

## 🔐 Security

- **Authentication:** JWT token required
- **Authorization:** Lecture/Admin/Superadmin only
- **Validation:** All marks validated (0-100 range)
- **Data Protection:** Only for specified batch and LOs

---

## 📊 Template Features

### Visual Design
- **Header Row:** Dark blue background, white text
- **Input Cells:** Light yellow background (easy to identify)
- **Borders:** All cells have clear borders
- **Columns:** Auto-sized for readability

### Included Sheets
1. **Mark Template Sheet**
   - Student Index column
   - LO columns (one per selected LO)
   - 10 pre-formatted empty rows
   - Ready for data entry

2. **Instructions Sheet**
   - Clear step-by-step instructions
   - Formatting guidelines
   - What NOT to do
   - Example format

---

## ✅ Quality Checks

The system validates uploaded templates:

### Before Import
- ✅ File is valid Excel format
- ✅ Student indices not empty
- ✅ Marks are numeric values
- ✅ Marks in range 0-100
- ✅ Correct number of columns
- ✅ Column headers match expected LOs

### During Import
- ✅ Student records created if needed
- ✅ Marks stored with correct mark type
- ✅ Batch information preserved
- ✅ LO associations verified

### After Import
- ✅ Marks visible in reports
- ✅ Pass/fail calculated correctly
- ✅ Export reports include new marks
- ✅ Statistics updated

---

## 🎯 Frontend Integration

### React/JavaScript Example
```javascript
// Step 1: Download template
async function downloadTemplate() {
  const response = await fetch('/api/obe/template/marks', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      losIds: selectedLOs,
      markType: 'FINAL_EXAM',
      batch: '24'
    })
  });

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'mark_template.xlsx';
  link.click();
}

// Step 2: Upload filled template
async function uploadMarks(file) {
  const formData = new FormData();
  formData.append('excelFile', file);
  formData.append('batch', '24');
  formData.append('loNumber', 'LO1');

  const response = await fetch('/api/lospos/LO1/marks/import-obe', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });

  return await response.json();
}
```

---

## 🚀 Complete User Journey

```
┌─────────────────────────────────────────────────────┐
│ 1. FRONTEND: Select LOs & Mark Type                 │
│    - Choose LO1, LO2, LO3                           │
│    - Select FINAL_EXAM                              │
│    - Select Batch 24                                │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│ 2. CLICK: "Download Template"                       │
│    → GET /api/obe/template/marks                    │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│ 3. DOWNLOAD: mark_template_24_final_exam.xlsx       │
│    - Has empty rows                                 │
│    - LO headers already set                         │
│    - Instructions included                          │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│ 4. USER ACTION: Opens Excel                         │
│    - Fills in Student Indices (EN001, EN002...)     │
│    - Fills in Marks (85.5, 90.0...)                 │
│    - Saves file                                     │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│ 5. CLICK: "Upload Marks"                            │
│    - Select filled template file                    │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│ 6. UPLOAD: POST /api/lospos/{loId}/marks/import-obe │
│    - Validates file format                          │
│    - Validates mark values                          │
│    - Imports to database                            │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│ 7. SUCCESS: Marks stored in system                  │
│    - Available for reports                          │
│    - Visible in exports                             │
│    - Pass/fail calculated                           │
└─────────────────────────────────────────────────────┘
```

---

## 🔄 Error Handling

### Common Issues & Solutions

**"Template not generating"**
- Ensure losIds are valid LO IDs
- Check mark type is FINAL_EXAM or ASSIGNMENT
- Verify batch is provided

**"Upload rejected - invalid format"**
- Ensure file is .xlsx (Excel format)
- Don't modify header row
- Fill all required cells
- Use numeric marks (0-100)

**"Marks not appearing after upload"**
- Check batch matches template
- Verify LO IDs match
- Check student indices are correct
- Review system logs for errors

---

## 📈 Benefits

✅ **Easier Data Entry**
- Pre-formatted template
- Clear column structure
- Visual guidance (color coding)

✅ **Reduced Errors**
- No column misalignment
- Validation on upload
- Clear instructions

✅ **Better UX**
- Download → Fill → Upload workflow
- Familiar Excel interface
- No system learning curve

✅ **Batch Processing**
- Handle multiple students at once
- Multiple LOs in one file
- Efficient import

---

## 🎓 Best Practices

### For Users
1. ✅ Download fresh template each time
2. ✅ Fill all student indices
3. ✅ Enter marks as decimals (85.50)
4. ✅ Don't modify header row or column order
5. ✅ Save as Excel format (.xlsx)
6. ✅ Verify marks before uploading

### For System Admins
1. ✅ Monitor import logs
2. ✅ Validate marks periodically
3. ✅ Backup before bulk imports
4. ✅ Test with sample data first

---

## 📝 Template Example Output

When user downloads template for LO1, LO2, LO3:

**Filename:** `mark_template_24_final_exam.xlsx`

**Sheet 1: "Mark Template"**
```
Student Index | LO1 | LO2 | LO3
[empty row]   |     |     |
[empty row]   |     |     |
[empty row]   |     |     |
...
```

**Sheet 2: "Instructions"**
```
1. Fill in the 'Student Index' column with student IDs (e.g., EN001, EN002)
2. Fill in the mark columns with scores between 0 and 100
3. Use decimal values (e.g., 85.50, 92.00)
4. Do NOT modify the header row
5. Do NOT change the column order
6. Do NOT change sheet names
7. Save the file as Excel format (.xlsx)
8. Upload the completed file back to the system
```

---

## ✨ Summary

The template feature provides a **download → fill → upload** workflow for efficient mark entry:

1. **Download:** Get pre-formatted Excel template
2. **Fill:** Enter student indices and marks
3. **Upload:** Import marks back to system
4. **Verify:** System validates and stores marks
5. **Report:** Marks available in all reports

**Simple, efficient, and user-friendly!** 🎉
