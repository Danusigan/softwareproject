# Frontend Feature Logic for Backend Checklist

## Purpose

This document maps the completed backend requirements in `Software-project-Backend/FINAL_CHECKLIST.md` to the frontend behavior that must exist in the React app.

The frontend must support the same workflow already used in the project UI:

- Blue/indigo glass-card layout
- Rounded cards and buttons
- Lecturer-focused dashboard navigation
- Header/footer layout consistency
- Existing single-LO upload flow as a fallback path

## Coverage Review

### Backend requirements that the frontend must expose

1. Mark type selection
2. Multiple LO selection
3. Template download
4. Student index column in Excel workflow
5. LO columns after the index column
6. Pass/fail export using threshold
7. Default template download for manual fill and upload
8. Bulk upload for multiple LOs in one workbook

### Current frontend state

- Login, landing, lecturer dashboard, and LO detail pages already exist.
- The app currently supports a single-LO upload path through `AddResultsPage`.
- The backend checklist adds a bulk Excel workflow that should be exposed in a dedicated frontend screen.
- The UI should not replace the current design language; it should extend it.

## Frontend Screens

### 1. Lecturer Dashboard

Role:

- Primary entry point for lecturers.

Frontend behavior:

- Show assigned modules.
- Open a module action panel.
- Add a clear entry point to the bulk marks workflow.
- Keep the existing single-LO result upload path visible for legacy use.

Required actions:

- Open module details.
- Open LO creation.
- Open LO-PO mapping management.
- Open bulk marks workflow for the selected module.

### 2. LO Detail Page

Role:

- Used for reviewing one learning outcome and its result history.

Frontend behavior:

- Show LO details and mapping feedback.
- Keep single-LO upload behavior available.
- Add a direct button to the bulk marks workflow using the parent module id.

### 3. Single-LO Upload Page

Role:

- Preserve the current one-LO upload/import flow.

Frontend behavior:

- Upload one Excel file for one LO.
- Update batch when needed.
- Keep this screen as a fallback and editing flow.

### 4. Bulk Marks Workflow Page

Role:

- The main new feature page for the checklist-backed Excel workflow.

Frontend behavior:

- Load module details and all LOs for the module.
- Let the lecturer select one or more LOs.
- Let the lecturer choose mark type.
- Let the lecturer choose batch.
- Let the lecturer enter or accept a default threshold.
- Allow template download.
- Allow report export.
- Allow bulk workbook upload.

## Workflow Logic

### Step A: Select LOs

- Fetch the module LOs from the backend.
- Display each LO as a selectable card.
- Keep the order stable because the Excel columns depend on it.
- Allow select all and clear selection.

### Step B: Choose mark metadata

- Mark type values:
  - `FINAL_EXAM`
  - `ASSIGNMENT`
- Batch should be editable and prefilled with a sensible default.
- Threshold should default to `50` for the export flow.

### Step C: Download template

Request:

- `POST /api/obe/template/marks`

Payload:

- `losIds: string[]`
- `markType: string`
- `batch: string`

Frontend result:

- Download the returned `.xlsx` file.
- Keep the filename from the response headers when possible.

### Step D: Fill and upload workbook

Request:

- `POST /api/obe/marks/upload-bulk`

Payload:

- `excelFile`
- `losIds` as comma-separated text in form-data
- `batch`
- `markType`

Frontend result:

- Accept drag-and-drop or file picker upload.
- Show validation errors when the file is missing or has the wrong extension.
- Show success or failure messages from the backend.

### Step E: Export report

Request:

- `POST /api/obe/export/marks`

Payload:

- `losIds: string[]`
- `markType: string`
- `batch: string`
- `threshold: number`

Frontend result:

- Download the generated report.
- Display pass/fail intent clearly in the UI.

## UI Rules

- Keep the same visual system used elsewhere in the project.
- Use the existing `Header` and `Footer` components.
- Use glass-card sections, rounded corners, and subtle indigo/emerald accents.
- Show success/error banners near the top of the page.
- Disable actions until the required inputs are selected.
- Keep legacy screens usable so the new workflow does not break the current single-LO path.

## Routing Plan

Recommended route additions:

- `/marks-workbench/:moduleId` for the bulk workflow

Recommended entry points:

- Lecturer dashboard module actions
- LO detail page action buttons

## Validation Plan

After implementation, the frontend should be checked for:

- No broken routes
- No missing imports
- Valid API payloads for the three checklist endpoints
- Download behavior for blob responses
- Upload behavior for multipart form-data
- Consistent role protection for lecturer/admin users

## Notes for Future Feature Work

- Keep the single-LO upload screen because it still serves the existing import flow.
- Use the bulk workflow for the checklist-backed template/export/upload process.
- If the backend adds more export options later, extend the bulk workflow page instead of creating a second parallel screen.