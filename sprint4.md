# OBE System — Sprint 4 Application Guide

**Department of Electrical & Information Engineering (DEIE), University of Ruhuna**
A web-based system for automating Outcome-Based Education (OBE) assurance: LO-PO mapping, marks management, attainment calculation, and Continuous Quality Improvement (CQI).

---

## Tech Stack

| Layer     | Technology                                              |
|-----------|---------------------------------------------------------|
| Frontend  | React 18, Vite, TailwindCSS, React Router v6, Axios    |
| Backend   | Spring Boot 3.2, Spring Data JPA, Spring Security (JWT)|
| Database  | MySQL                                                   |
| Reports   | Apache POI (Excel .xlsx generation & import)           |

---

## User Roles

| Role        | Access Level                                                        |
|-------------|---------------------------------------------------------------------|
| Super Admin | Can create Admin accounts only                                      |
| Admin       | Full access — modules, teachers, POs, LO-PO mapping approval       |
| Lecturer    | Manages LOs, uploads marks, views analytics for assigned modules    |

---

## Application Routes

| URL                                  | Page                       | Role Required   |
|--------------------------------------|----------------------------|-----------------|
| `/`                                  | Landing Page               | Public          |
| `/loginpage`                         | Login                      | Public          |
| `/forgottenpassword`                 | Forgot Password            | Public          |
| `/super-admin-dashboard`             | Super Admin Dashboard      | Super Admin     |
| `/admin-dashboard`                   | Admin Dashboard            | Admin           |
| `/lecturer-dashboard`                | Lecturer Dashboard         | Lecturer        |
| `/modules`                           | Modules Overview           | Any logged-in   |
| `/create-lo-mapping/:moduleId`       | Create LO + PO Mapping     | Any logged-in   |
| `/lo-po-mappings`                    | LO-PO Mapping Management   | Any logged-in   |
| `/marks-workbench/:moduleId`         | Marks Workbench            | Any logged-in   |
| `/lo-detail/:loId/comparisons`       | LO Analytics & Comparison  | Any logged-in   |
| `/program-outcomes`                  | Program Outcomes           | Admin only      |

---

## Page-by-Page Navigation Guide

---

### 1. Landing Page `/`

The public entry point of the system.

| Button / Link                  | Action                                                                            |
|-------------------------------|------------------------------------------------------------------------------------|
| **"Access your dashboard"**   | If logged in → goes to your role's dashboard. If not → goes to `/loginpage`        |
| Header **"Login"** link        | → `/loginpage`                                                                    |

---

### 2. Login Page `/loginpage`

| Field / Button               | Action                                                                         |
|-----------------------------|---------------------------------------------------------------------------------|
| Username, Password fields   | Enter your credentials                                                          |
| **Role dropdown**           | Select your role (Admin / Lecturer / SuperAdmin) — required before submitting   |
| **"Login"** button          | Authenticates and redirects based on role:                                      |
|                             | → Super Admin → `/super-admin-dashboard`                                        |
|                             | → Admin → `/admin-dashboard`                                                    |
|                             | → Lecturer → `/lecturer-dashboard`                                              |
| **"Forgot Password?"** link | → `/forgottenpassword`                                                          |
| **"Remember Me"** checkbox  | Keeps session active beyond browser close (stores token in localStorage)        |

---

### 3. Forgot Password Page `/forgottenpassword`

| Field / Button          | Action                                    |
|------------------------|-------------------------------------------|
| Email input field      | Enter registered email address            |
| **"Send Reset Link"**  | Submits request to reset password via email|
| Back / Header links    | → `/loginpage`                            |

---

### 4. Super Admin Dashboard `/super-admin-dashboard`

> Super Admins can only create Admin accounts.

| Button / Card            | Action                                                          |
|-------------------------|-----------------------------------------------------------------|
| **"Add Admin"** card    | Slides open right side panel with the Add Admin form           |
| Side panel **"×"** button | Closes the side panel                                        |
| **"Submit"** in form    | Creates a new Admin account via `POST /api/auth/add-user`      |

---

### 5. Admin Dashboard `/admin-dashboard`

The main control panel for Admins.

#### Top Bar
| Button                  | Action                              |
|------------------------|--------------------------------------|
| **"View Modules"**     | → `/modules` (Modules overview page) |

#### Module Cards (one per existing module)
| Button           | Action                                                                  |
|-----------------|-------------------------------------------------------------------------|
| **Edit icon** (pencil) | Opens inline edit dialog — update Module ID and Module Name        |
| **Delete icon** (trash) | Confirms deletion → deletes module + all LOs, marks, mappings, CQI actions (full cascade) |
| **"Save"** in edit dialog | Submits update via `PUT /api/modules/:id`                      |

#### Action Cards (bottom section)
| Card                        | Action                                                             |
|----------------------------|---------------------------------------------------------------------|
| **"Add a Teacher"** card   | Slides open right side panel with the Add Teacher form             |
| **"Create the Module"** card | Slides open right side panel with the Create Module form         |
| **"Program Outcomes"** card | → `/program-outcomes`                                             |
| **"LO-PO Mappings"** card  | → `/lo-po-mappings`                                               |

#### Add Teacher Side Panel
| Field / Button       | Description                                               |
|---------------------|-----------------------------------------------------------|
| Username, Email, Password | Fill in teacher details                          |
| User Type dropdown  | Lecture (default)                                         |
| **"Add Teacher"**   | Creates lecturer account via `POST /api/auth/add-user`    |
| **"×"** button      | Closes the panel                                          |

#### Create Module Side Panel
| Field / Button       | Description                                                        |
|---------------------|--------------------------------------------------------------------|
| Module ID           | Uppercase letters and digits only (e.g., `EC5439`)                |
| Module Name         | Full module name                                                   |
| **"Create Module"** | Creates module via `POST /api/modules/create`                      |
| **"×"** button      | Closes the panel                                                   |

---

### 6. Lecturer Dashboard `/lecturer-dashboard`

Shows all available modules as cards. Lecturers interact with each module through an action menu.

#### Module Cards
| Interaction            | Action                                                              |
|-----------------------|----------------------------------------------------------------------|
| **Click a module card** | Opens a dropdown action menu on the card                         |

#### Module Card Action Menu (appears on click)
| Menu Item                    | Action                                                                        |
|-----------------------------|--------------------------------------------------------------------------------|
| **"Create LO"**             | Opens `/create-lo-mapping/:moduleId` in a **new browser tab/window**          |
| **"View / Manage LOs"**     | Opens the LO detail slide-in panel on the right side of screen                |
| **"View LO-PO Mappings"**   | → `/lo-po-mappings`                                                           |
| **"Marks & Analytics"**     | → `/marks-workbench/:moduleId`                                                |

#### LO Detail Side Panel (opens when "View / Manage LOs" is clicked)
| Button / Element              | Action                                                                    |
|------------------------------|----------------------------------------------------------------------------|
| **"Marks & Analytics"** button | → `/marks-workbench/:moduleId`                                          |
| **"Create LO + PO Mapping"** button | Opens `/create-lo-mapping/:moduleId` in a new browser window      |
| **"View LO-PO Mappings"** button | → `/lo-po-mappings`                                                  |
| **Edit icon** (on each LO row) | Opens edit LO dialog — update LO number and description                |
| **Delete icon** (on each LO row) | Confirms deletion → deletes LO + all its marks and mappings          |
| **"Save"** in edit dialog    | Submits update via `PUT /api/lospos/:loId`                                |
| **"×"** button (top right)   | Closes the side panel                                                     |

---

### 7. Modules Page `/modules`

A read-only overview of all modules displayed as coloured cards.

| Interaction              | Action                                                       |
|-------------------------|--------------------------------------------------------------|
| **Click a module card** | Opens a "View" modal showing module details                  |
| **Edit icon** (on card) | Opens the Edit modal — modify Module ID and Name            |
| **Delete icon** (on card) | Opens Delete confirmation modal                            |
| **"Confirm Delete"** in modal | Deletes the module via `DELETE /api/modules/:id`      |
| **"Save"** in edit modal | Updates the module via `PUT /api/modules/:id`               |
| **"×"** or **"Close"**  | Dismisses the active modal                                   |

---

### 8. Create LO with Mapping Page `/create-lo-mapping/:moduleId`

> Opens in a new browser window. Used by lecturers to create a new Learning Outcome and map it to Program Outcomes in one step.

| Field / Section           | Description                                                              |
|--------------------------|--------------------------------------------------------------------------|
| **LO ID** field          | The LO identifier (e.g., `LO1`). Stored as `{moduleId} {loId}` in DB    |
| **Name / Description**   | Free-text description of the learning outcome                            |
| **Program Outcomes list**| Grouped by category (Knowledge, Skills, Professional, etc.)              |
| **Correlation Weight**   | 0 = No Correlation, 1 = Low, 2 = Medium, 3 = High                       |
| **Lecturer Remarks**     | Optional notes for each PO mapping                                       |
| **"Submit LO with Mappings"** | Creates the LO and all selected PO mappings (status = PENDING)     |
| **Back / close window**  | Returns to Lecturer Dashboard                                            |

> Mappings are created with status **PENDING** — they require Admin approval before being counted in attainment.

---

### 9. LO-PO Mapping Management Page `/lo-po-mappings`

Central page for viewing and managing all LO-PO mappings across all modules.

#### Filters
| Filter           | Options                                       |
|-----------------|-----------------------------------------------|
| Module filter   | Dropdown of all modules                       |
| Status filter   | All / Pending / Approved / Rejected           |
| Search          | Free text — searches by LO or PO name         |

#### View Mode Buttons
| Button      | Effect                                        |
|------------|-----------------------------------------------|
| **List**   | Table/list view of mappings                   |
| **Grid**   | Card grid view                                |
| **Matrix** | LO vs PO correlation matrix view              |

#### Mapping Actions (role-dependent)
| Role       | Button               | Action                                                         |
|-----------|---------------------|----------------------------------------------------------------|
| Admin     | **Approve/Reject**  | Opens approval modal — set status and add admin remarks        |
| Lecturer  | **Edit**            | Opens edit modal — change correlation weight or add remarks    |

#### Approval Modal (Admin only)
| Field / Button   | Description                                          |
|-----------------|------------------------------------------------------|
| Status dropdown | Approved / Rejected                                  |
| Remarks field   | Admin feedback shown back to the lecturer            |
| **"Submit"**    | Updates mapping status via `PUT /api/mappings/:id`   |
| **"Cancel"**    | Closes the modal                                     |

---

### 10. Marks Workbench Page `/marks-workbench/:moduleId`

The most feature-rich page. Used by lecturers to manage student marks, download templates, upload results, and view analytics.

#### Batch Tabs (top navigation)
| Element                     | Action                                                           |
|----------------------------|------------------------------------------------------------------|
| **Batch tab** (e.g., "23") | Switches the active batch — all data below updates accordingly  |
| **"+ Add Batch"** button   | Reveals input to type a new batch number and set its threshold   |
| **"Add"** button (after input) | Creates the new batch tab and sets it as active            |

#### Marks Records Area (main section per batch)
| Button / Element            | Action                                                                   |
|----------------------------|--------------------------------------------------------------------------|
| **"Upload New Marks"**     | Opens the upload workflow panel (slides in from right or expands)        |
| **"Edit"** on existing entry | Re-opens the upload workflow pre-filled with that entry's settings    |
| Existing entry rows        | Show assignment label, mark type, LO count, and actions                  |

#### Upload Workflow — Option Selection Screen
When "Upload New Marks" is clicked, two cards appear:

| Card                         | Action                                                           |
|-----------------------------|------------------------------------------------------------------|
| **"Download Template"** card | Enters download-template mode — lets you configure and download a pre-formatted Excel file |
| **"Upload Marks"** card      | Enters upload-marks mode — shows just the file picker for direct upload |

#### Upload Workflow — Download Template Mode

**Step 1: Assignment Details**
| Field               | Description                                         |
|--------------------|-----------------------------------------------------|
| Mark Type          | Final Exam / Assignment                             |
| Assignment Label   | Optional label (e.g., "Mid-Sem 1")                  |

**Step 2: Select LOs**
| Element              | Description                                       |
|---------------------|---------------------------------------------------|
| LO checkboxes       | Select which LOs to include in the template        |
| "Select All" toggle | Selects/deselects all LOs at once                  |

**Step 3: Template Configuration**

Two tabs:

**Q-wise Tab (Question-wise)**
| Field / Button              | Description                                                         |
|----------------------------|---------------------------------------------------------------------|
| Number of Questions         | How many question columns to include                               |
| LO assignment (per question)| Map each question to an LO from the selected list                  |
| Max marks (per question)    | Max score for that question                                         |
| **"Download Q-wise Template"** | Downloads `.xlsx` with one column per question, METADATA sheet embedded |

**LO-wise Tab**
| Field / Button             | Description                                                          |
|---------------------------|----------------------------------------------------------------------|
| Max marks (per LO)        | Input field per selected LO — defaults to 100                        |
| **"Download LO-wise Template"** | Downloads `.xlsx` with one column per LO, LO_MAX_MARKS in METADATA |

#### Upload Workflow — Upload Marks Mode
| Element                      | Action                                                             |
|-----------------------------|---------------------------------------------------------------------|
| Drag-and-drop zone / file picker | Select or drop your filled-in `.xlsx` file                  |
| **"Upload Marks"** button   | Uploads the file via `POST /api/obe/marks/upload` (auto-detects Q-wise or LO-wise from METADATA sheet) |
| **"Cancel"** or **"×"**     | Closes the upload panel without uploading                          |

---

> **Template METADATA sheet — editable fields before re-uploading:**
>
> | METADATA key      | Can you change it? | Effect                                      |
> |------------------|--------------------|---------------------------------------------|
> | `BATCH`          | Yes                | Marks saved under the new batch number       |
> | `MARK_TYPE`      | Yes                | Mark type overridden on upload               |
> | `ASSIGNMENT_LABEL` | Yes             | Label updated                                |
> | `LO_MAX_MARKS`   | Yes (LO-wise only) | New max marks used for validation and stored |
> | Max marks in column header | Only if `LO_MAX_MARKS` is also blank in METADATA | Falls back to header |
> | `LO_IDS`         | No — do not change | LO IDs must match records in the database    |

---

#### Analytics Section (below marks records)

| Control / Button                    | Action                                                         |
|------------------------------------|----------------------------------------------------------------|
| **LO checkboxes** (analytics)       | Select which LOs to include in analytics                      |
| **Mark Type** dropdown              | Filter analytics by mark type                                  |
| **"Calculate PO Attainment"** button | Runs PO attainment calculation and shows result table        |
| PO Attainment result table          | Shows each PO, its attainment %, and PASS/FAIL status         |

#### Charts (auto-rendered when marks exist)
| Chart                   | What it shows                                                                      |
|------------------------|------------------------------------------------------------------------------------|
| **Average Score bar chart** | Average student score per batch (normalized to %, using max marks from AssessmentItems) |
| **Pass/Fail pie chart** | % of students who passed vs failed for the selected batch (normalized to threshold %) |
| **Trend line chart**    | Average % across batches with IMPROVED / STABLE / DECLINED status and delta (absolute ppt change) |

| Chart navigation button        | Action                                |
|-------------------------------|---------------------------------------|
| **Batch selector** (pie chart) | Switches the pie chart to show a specific batch's pass/fail split |

---

### 11. LO Analytics Page `/lo-detail/:loId/comparisons`

Detailed analytics view for a single LO across all batches.

| Element / Button     | Description                                                         |
|--------------------|----------------------------------------------------------------------|
| Batch selector tabs | Switch between batches to see batch-specific data                   |
| Average score chart | Normalized average % per batch                                      |
| Pass/Fail pie chart | Pass rate % for the selected batch                                  |
| Trend comparison    | Batch-over-batch change with IMPROVED / STABLE / DECLINED badge      |
| Delta indicator     | Shows absolute percentage point change between consecutive batches  |
| **Back** / Header   | → Previous page                                                     |

---

### 12. Program Outcomes Page `/program-outcomes`

> Admin access only.

Manage the Programme Outcomes (POs) aligned to Washington Accord categories.

| Button / Filter         | Action                                                                 |
|------------------------|------------------------------------------------------------------------|
| **Category filter** tabs | Filter POs by category (Knowledge, Skills, Professional, etc.)      |
| **Search** field        | Filter by PO ID or title                                              |
| **"Show Inactive"** toggle | Include/exclude deactivated POs in the list                       |
| **"+ Create PO"** button | Opens Create PO modal                                               |
| **Click a PO card**     | Opens View PO modal — shows full details and performance indicators   |
| **Edit icon**           | Opens Edit PO modal — update title, description, category, indicators |
| **Delete icon**         | Opens Delete confirmation modal                                       |
| **"Save"** in modal     | Submits create/update via `POST` or `PUT /api/program-outcomes/...`   |
| **"×"** / **"Cancel"**  | Closes the modal                                                      |

---

## Data Hierarchy & Cascade Deletion

Understanding how data is linked is important. Deleting a parent automatically removes all children:

```
Module
 └── CqiAction (deleted first — references module_id and los_id)
 └── AssessmentTemplate (module_id nullified before module delete)
 └── Los (Learning Outcome)
      └── StudentMark (all student marks for this LO)
      └── AssessmentItem (template questions/LO entries)
           └── StudentAssessmentScore (per-question scores)
      └── CqiAction (per-LO CQI entries)
      └── lo_po_mappings (LO-PO correlation records)
```

> Deleting a **Module** removes everything above — all LOs, all student marks, all assessment data, all mappings, and all CQI actions. This is irreversible.

---

## Marks Template Format

### LO-wise Template (`TEMPLATE_TYPE = LO_WISE`)

| Column       | Content                          |
|-------------|----------------------------------|
| Col A       | Student Index                    |
| Col B, C... | One column per LO (e.g., "LO1 - Network Basics (max=50)") |

- **METADATA sheet** stores: `BATCH`, `MARK_TYPE`, `ASSIGNMENT_LABEL`, `LO_IDS`, `LO_MAX_MARKS` (e.g., `LO1:50,LO2:30`)
- Max marks validation on upload uses `LO_MAX_MARKS` from METADATA (header text is fallback only)

### Q-wise Template (`TEMPLATE_TYPE = Q_WISE`)

| Column       | Content                          |
|-------------|----------------------------------|
| Col A       | Student Index                    |
| Col B, C... | One column per question (e.g., "Q1 (max=10, LO1)") |

- **METADATA sheet** stores: `TEMPLATE_ID`, `BATCH`, `MARK_TYPE`, `ASSIGNMENT_LABEL`
- Max marks come from database `AssessmentItem` records — not from the file

---

## Running the Application

### Backend (Spring Boot)
```bash
cd Software-project-Backend
./mvnw spring-boot:run
# Runs on http://localhost:8080
```

### Frontend (React + Vite)
```bash
cd softwareproject_frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

### Database
- MySQL running on default port 3306
- Configure connection in `application.properties` (`spring.datasource.*`)
- JPA auto-creates/updates tables (`spring.jpa.hibernate.ddl-auto=update`)

---

## Key API Endpoints (Reference)

| Method | Endpoint                          | Description                              |
|--------|-----------------------------------|------------------------------------------|
| POST   | `/api/auth/login`                 | Login — returns JWT token                |
| POST   | `/api/auth/add-user`              | Create new user (Admin or Lecturer)      |
| GET    | `/api/modules/all`                | List all modules                         |
| POST   | `/api/modules/create`             | Create a module                          |
| DELETE | `/api/modules/:id`                | Delete module (cascade all data)         |
| GET    | `/api/lospos/module/:moduleId`    | Get all LOs for a module                 |
| DELETE | `/api/lospos/:loId`               | Delete an LO (cascade marks + mappings) |
| POST   | `/api/los-with-mapping`           | Create LO + PO mappings in one request  |
| GET    | `/api/lo-po-mappings`             | All LO-PO mappings (with filters)       |
| PUT    | `/api/lo-po-mappings/:id/approve` | Approve or reject a mapping (Admin)     |
| POST   | `/api/obe/template/marks`         | Generate Excel marks template            |
| POST   | `/api/obe/marks/upload`           | Upload filled-in marks template          |
| GET    | `/api/obe/attainment/po/:moduleId`| Calculate PO attainment for module       |
| GET    | `/api/obe/trend/lo/:courseId`     | LO-level average trend across batches    |
| GET    | `/api/obe/passrate/lo/:courseId`  | LO-level pass rate per batch             |
| GET    | `/api/program-outcomes`           | List all Program Outcomes                |
| POST   | `/api/program-outcomes`           | Create a Program Outcome (Admin)         |

---

## Session & Security

- JWT token stored in `localStorage` with a 2-hour expiry
- **Remember Me** checkbox extends the session beyond the browser session
- All protected routes redirect to `/` if the JWT is missing or expired
- Axios interceptors automatically attach the `Authorization: Bearer <token>` header to every request
- Role-based route guards: accessing `/admin-dashboard` as a Lecturer redirects to `/`

---

*Sprint 4 — DEIE OBE System, University of Ruhuna*
