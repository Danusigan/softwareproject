# OBE System API - Quick Reference

Base URL: http://localhost:8080

Lecturer User: danu1 / 1234
Module: EC6306

WORKFLOW: Lecturer Marks Upload for EC6306

=== STEP 1: LOGIN ===
POST /api/auth/login
{"userID": "danu1", "password": "1234"}

=== STEP 2: VIEW MODULE EC6306 ===
GET /api/modules/EC6306
(Authorization: Bearer {token})

=== STEP 3: GET LEARNING OUTCOMES ===
GET /api/lospos/module/EC6306
(Returns list of LOs - note the LO IDs)

=== STEP 4: DOWNLOAD EXCEL TEMPLATE ===
POST /api/obe/template/marks
(Authorization: Bearer {token})
{
    "losIds": ["LO1", "LO2", "LO3"],
    "markType": "FINAL_EXAM",
    "batch": "22"
}
(Downloads: mark_template_22_final_exam.xlsx)

=== STEP 5: FILL EXCEL (MANUAL) ===
Open template, fill:
Student Index | LO1 | LO2 | LO3
EN001        | 85.5| 90.0| 88.0
EN002        | 78.0| 82.5| 91.0
Save file.

=== STEP 6: UPLOAD MARKS ===
Upload for LO1:
POST /api/lospos/LO1/marks/import-obe
Form-data: excelFile=[file], batch=22, loNumber=1

Upload for LO2:
POST /api/lospos/LO2/marks/import-obe
Form-data: excelFile=[file], batch=22, loNumber=2

Upload for LO3:
POST /api/lospos/LO3/marks/import-obe
Form-data: excelFile=[file], batch=22, loNumber=3

=== STEP 7: VERIFY MARKS ===
GET /api/lospos/LO1/marks
GET /api/lospos/LO1/batches/22/marks

---

POSTMAN FILE: OBE_Lecturer_Marks_Upload.json
(Import this file - contains all requests above)
