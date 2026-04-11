# Excel Export Feature Implementation - Summary

## Overview
Successfully implemented a feature that allows users to:
1. Select mark type (Final Exam or Assignment)
2. Select multiple Learning Outcomes (LOs)
3. Select a batch year
4. Generate an Excel file with student marks and pass/fail status

## Changes Made

### 1. **New Model: MarkType.java**
- Location: `Model/MarkType.java`
- Enum with two values: `FINAL_EXAM`, `ASSIGNMENT`
- Used to categorize student marks by type

### 2. **Updated Model: StudentMark.java**
- Added new field: `MarkType markType`
- Database column: `mark_type` (ENUM)
- Allows distinction between exam and assignment marks
- Getters and setters added

### 3. **Updated Repository: StudentMarkRepository.java**
- Added new query methods:
  - `findByLosIdsAndMarkTypeAndBatch()` - Get marks for multiple LOs by type and batch
  - `findDistinctStudentsByLosIdsAndMarkTypeAndBatch()` - Get unique students
  - `findDistinctBatchesByLosIdsAndMarkType()` - Get available batches
- All queries support filtering by mark type

### 4. **New Service: ExcelExportService.java**
- Location: `Service/ExcelExportService.java`
- Main method: `generateMarksExcel(losIds, markType, batch, threshold)`
- Features:
  - First column: Student Index Number
  - Columns for each LO with LO names as headers
  - Displays "Pass (score)" or "Fail (score)" based on threshold
  - Color-coded cells:
    - Green for Pass
    - Red for Fail
  - Professional Excel formatting with borders and alignment

### 5. **Updated Service: ExcelImportService.java**
- Added new overload: `importMarksOBEFormat(losId, file, batch, markType)`
- When importing marks, now stores the mark type (defaults to FINAL_EXAM)
- Backward compatible with existing code

### 6. **Updated Controller: OBEController.java**
- Added new endpoint: `POST /api/obe/export/marks`
- Features:
  - Accepts JSON body with:
    - `losIds`: List of LO IDs to include
    - `markType`: "FINAL_EXAM" or "ASSIGNMENT"
    - `batch`: Batch year (e.g., "24", "25")
    - `threshold`: Pass threshold (optional, defaults to 50)
  - Authorization: Lecture/Admin/Superadmin only
  - Returns Excel file as downloadable attachment
  - Proper HTTP headers for file download

## API Endpoint

### Export Marks
**Endpoint:** `POST /api/obe/export/marks`

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body:**
```json
{
  "losIds": ["LO1", "LO2", "LO3"],
  "markType": "FINAL_EXAM",
  "batch": "24",
  "threshold": 50
}
```

**Response:**
- Status: 200 OK
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Body: Excel file bytes
- Filename: `marks_report_24_final_exam.xlsx`

**Error Response:**
```json
{
  "message": "Error description",
  "status": "ERROR",
  "error": "Details"
}
```

## Excel File Structure

### Headers (Row 1)
- Column A: "Index Number"
- Columns B+: LO Names (e.g., "LO1", "LO2", etc.)

### Data Rows
- Column A: Student Index (e.g., "EN001")
- Columns B+: Status and Score (e.g., "Pass (85.50)" or "Fail (35.00)")

### Formatting
- Header row: Dark blue background, white text, bold, centered
- Pass cells: Light green background, centered
- Fail cells: Red background, white text, centered
- All cells have borders for clarity
- Column widths are auto-adjusted

## Database Changes

### New Column in StudentMark table
```sql
ALTER TABLE StudentMark ADD COLUMN mark_type VARCHAR(50);
```

Note: Existing records can be migrated with default value `FINAL_EXAM`

## Frontend Integration

### Example JavaScript Call
```javascript
const requestBody = {
  losIds: ["LO1", "LO2"],
  markType: "FINAL_EXAM",
  batch: "24",
  threshold: 50
};

fetch('/api/obe/export/marks', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify(requestBody)
})
.then(response => response.blob())
.then(blob => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'marks_report.xlsx';
  a.click();
});
```

## Configuration

### Pass/Fail Threshold
- Default: 50
- Configurable per request via `threshold` parameter
- Example: `"threshold": 75` for 75% pass threshold

### Supported Mark Types
- `FINAL_EXAM`: Final examination marks
- `ASSIGNMENT`: Assignment marks

## Testing

1. **Import marks with mark type:**
   - POST `/api/lospos/{loId}/marks/import-obe` with `markType` parameter

2. **Export marks:**
   - POST `/api/obe/export/marks` with required parameters
   - Download generated Excel file
   - Verify columns and data are correct
   - Verify pass/fail status is calculated correctly

## Future Enhancements

1. Add support for weighted marks (different weights for different LOs)
2. Add support for multiple sheets (one per LO or per batch)
3. Add statistical analysis to Excel (averages, median, standard deviation)
4. Add student performance rankings
5. Add charts and graphs within Excel
6. Support for additional mark types (practical, viva, etc.)

## Files Modified/Created

**Created:**
- `Model/MarkType.java`
- `Service/ExcelExportService.java`

**Modified:**
- `Model/StudentMark.java`
- `Repository/StudentMarkRepository.java`
- `Service/ExcelImportService.java`
- `RestController/OBEController.java`

## Dependencies

All required dependencies are already in `pom.xml`:
- Apache POI 5.2.4 (for Excel generation)
- Spring Boot 3.2.2
- Spring Data JPA
- Spring Security

## Build Status
✅ **BUILD SUCCESS** - Project compiles without errors
