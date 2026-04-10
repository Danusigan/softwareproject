# How to Download Excel Template in Postman

## The "Gibberish" Text is Actually the Excel File!

When you see this in Postman:
```
PKmtï¿½\[Content_Types].xmlï¿½TIn1ï¿½ï¿½ï¿½ï¿½hlï¿½...
```

**This is CORRECT!** ✅ It means the Excel file was generated successfully. The "gibberish" is the binary Excel file data.

---

## How to Save the Excel File in Postman

### Method 1: Use "Send and Download" (Recommended)

1. In the request (e.g., "4. Download Excel Template")
2. Click the **dropdown arrow** next to "Send" button
3. Select **"Send and Download"**
4. The Excel file will download to your Downloads folder
5. File name: `mark_template_22_final_exam.xlsx`

### Method 2: Save Response Manually

1. Click **Send** button normally
2. In the response area, click **"Save Response"** dropdown (top right)
3. Select **"Save to a file"**
4. Choose location and save as `.xlsx` file
5. Name it: `mark_template_22_final_exam.xlsx`

---

## Step-by-Step with Screenshots Description

### Step 1: Prepare the Request
```
POST http://localhost:8080/api/obe/template/marks
Headers: Authorization: Bearer {token}
Body:
{
    "losIds": ["LO1", "LO2", "LO3"],
    "markType": "FINAL_EXAM",
    "batch": "22"
}
```

### Step 2: Click "Send and Download"
- Look for the **Send** button
- Click the small **dropdown arrow** next to it
- Select **"Send and Download"**

### Step 3: File Downloads Automatically
- Excel file saves to your Downloads folder
- Filename: `mark_template_22_final_exam.xlsx`

### Step 4: Open the Excel File
- Navigate to Downloads folder
- Double-click `mark_template_22_final_exam.xlsx`
- Excel opens with:
  - Sheet 1: "Mark Template" (empty rows for filling)
  - Sheet 2: "Instructions"

---

## What You Should See in Excel

### Sheet 1: Mark Template

| Student Index | LO1 | LO2 | LO3 |
|---------------|-----|-----|-----|
| (empty)       |     |     |     |
| (empty)       |     |     |     |
| (empty)       |     |     |     |
| ...10 rows... |     |     |     |

**Header row:** Dark blue background, white text
**Data rows:** Light yellow background (ready for input)

### Sheet 2: Instructions

```
Mark Entry Template - Instructions

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

## If "Send and Download" is Not Available

### Alternative: Use Postman Console

1. Open Postman Console: `View → Show Postman Console`
2. Send the request normally
3. In response, right-click anywhere
4. Select **"Save Response to File"**
5. Save as `.xlsx` file

---

## Common Issues

### Issue 1: "File won't open in Excel"
**Solution:**
- Make sure you saved with `.xlsx` extension
- Don't save as `.txt` or other format
- Use "Send and Download" method

### Issue 2: "I see gibberish text"
**Solution:**
- This is normal! It's the Excel binary data
- Don't try to read it in Postman
- Use "Send and Download" to save it properly

### Issue 3: "Download button not working"
**Solution:**
- Check Postman version (update if needed)
- Use "Save Response to File" instead
- Manually save response as `.xlsx`

---

## Verify the Download Worked

After downloading:

1. ✅ File size should be ~10-15 KB
2. ✅ Extension is `.xlsx`
3. ✅ Opens in Microsoft Excel or compatible software
4. ✅ Contains 2 sheets: "Mark Template" and "Instructions"
5. ✅ Headers show: Student Index | LO1 | LO2 | LO3

---

## Complete Workflow Example

### 1. Request Template
```
POST /api/obe/template/marks
{
    "losIds": ["LO1", "LO2", "LO3"],
    "markType": "FINAL_EXAM",
    "batch": "22"
}
```

### 2. Download
- Click dropdown next to "Send"
- Select "Send and Download"
- File saves: `mark_template_22_final_exam.xlsx`

### 3. Open in Excel
- Go to Downloads folder
- Double-click the file
- Template opens with empty rows

### 4. Fill Data
| Student Index | LO1  | LO2  | LO3  |
|---------------|------|------|------|
| EN001         | 85.5 | 90.0 | 88.0 |
| EN002         | 78.0 | 82.5 | 91.0 |
| EN003         | 92.0 | 88.0 | 85.5 |

### 5. Save
- File → Save (or Ctrl+S)
- Keep filename or rename if needed

### 6. Upload
```
POST /api/lospos/LO1/marks/import-obe
Form-data:
- excelFile: [select saved file]
- batch: 22
- loNumber: 1
```

---

## Browser Alternative (If Postman Fails)

If Postman download isn't working, use browser:

### Using cURL
```bash
curl -X POST http://localhost:8080/api/obe/template/marks \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"losIds":["LO1","LO2","LO3"],"markType":"FINAL_EXAM","batch":"22"}' \
  -o template.xlsx
```

### Using Browser Fetch (Console)
```javascript
fetch('http://localhost:8080/api/obe/template/marks', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    losIds: ["LO1", "LO2", "LO3"],
    markType: "FINAL_EXAM",
    batch: "22"
  })
})
.then(response => response.blob())
.then(blob => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'template.xlsx';
  a.click();
});
```

---

## Summary

✅ **The "gibberish" in Postman response is normal** - it's the Excel binary data
✅ **Use "Send and Download"** button to save properly
✅ **File should be `.xlsx` format** and open in Excel
✅ **Template contains 2 sheets** with headers and instructions
✅ **Fill the template** and upload back to system

**The API is working correctly!** You just need to download the response as a file instead of viewing it as text. 🎯
