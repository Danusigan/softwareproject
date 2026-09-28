# CQI for PO Feature Implementation

## Overview
Implemented a complete Continuous Quality Improvement (CQI) system for Program Outcomes (POs), allowing admins to create improvement plans directly from batch reports when PO attainment falls below targets.

## Changes Made

### 1. Header Navigation (Simplified)
**File**: `softwareproject_frontend/src/components/header.jsx`
- ✅ Removed "Student reports" link
- ✅ Removed "Batch reports" link  
- ✅ Removed "PO credit summary" link
- **Result**: Header now shows only HOME and ACCOUNT/LOGOUT

### 2. Backend API Endpoints
**File**: `Software-project-Backend/src/main/java/com/example/Software/project/Backend/RestController/CqiActionController.java`

Added three new admin-only endpoints:

#### POST `/api/cqi/po/create`
Creates a new CQI plan for a PO directly from batch report
```json
Request body:
{
  "poId": "PO001",
  "batch": "22",
  "moduleId": "EC6306",
  "currentAttainment": 65.5,
  "targetAttainment": 75.0,
  "plannedActions": "Increase lab sessions and improve assessment..."
}

Response: CqiAction object with status="IN_PROGRESS" (no approval needed)
```

#### GET `/api/cqi/batch/{batch}`
Lists all CQI plans for a specific batch
```
Returns: Array of CqiAction objects ordered by created date (newest first)
```

#### PUT `/api/cqi/{id}/status`
Updates CQI plan status: PLANNED → IN_PROGRESS → COMPLETED → CLOSED
```json
Request body:
{
  "status": "COMPLETED"
}
```

#### PUT `/api/cqi/{id}/details`
Updates CQI plan details (actions, targets)
```json
Request body:
{
  "plannedActions": "Updated action plan...",
  "targetAttainment": 80.0
}
```

### 3. Backend Service Logic
**File**: `Software-project-Backend/src/main/java/com/example/Software/project/Backend/Service/CQIService.java`

Added four new methods:
- `createPoCqiPlan()` - Creates PO-level CQI plans
- `getCqiPlansForBatch()` - Retrieves all plans for a batch
- `updateCqiPlanStatus()` - Changes plan status
- `updateCqiPlanDetails()` - Updates plan content

Also added:
- `ProgramOutcomeRepository` injection for PO lookups

**File**: `Software-project-Backend/src/main/java/com/example/Software/project/Backend/Repository/CqiActionRepository.java`

Added repository method:
- `findByBatchOrderByCreatedAtDesc(String batch)` - For retrieving batch plans chronologically

### 4. Frontend Components

#### New Component: `CqiPlanModal.jsx`
Modal form for creating CQI plans triggered from batch reports
- PO selection and display
- Current attainment (read-only)
- Target attainment input (0-100%)
- Planned actions textarea
- Create/Cancel buttons
- Error handling with user feedback

#### New Component: `CqiPlansDisplay.jsx`
Card-based display of all CQI plans for a batch
- Shows plan status with color-coded badges
- Displays current vs target attainment metrics
- Shows planned actions summary
- "Mark Completed" button for in-progress plans
- Auto-loads plans for batch
- Refresh capability

#### New Styles: `cqiPlans.css`
Complete styling for:
- Modal overlay and content
- Form inputs and layout
- Plan cards grid
- Status badges (In Progress, Completed, Planned, Closed)
- Responsive design for mobile

### 5. Batch Reports Integration
**File**: `softwareproject_frontend/src/pages/BatchReportsPage.jsx`

Enhanced with CQI plan capabilities:
- Imported CqiPlanModal and CqiPlansDisplay components
- Added state for modal and refresh trigger
- Added "Create CQI Plan" button to each PO card (only for POs below target)
- Displays modal when button clicked
- Shows CqiPlansDisplay section below PO tables
- Auto-refreshes plans when new plan created

**File**: `softwareproject_frontend/src/pages/batchReports.css`

Added styles:
- `.po-header` - Flexbox layout for PO title + button
- `.btn-cqi-create` - Blue button with hover effects
- Mobile responsive adjustments

## User Flow

### Creating a CQI Plan

1. **Admin logs in** and navigates to Batch Reports (now available only programmatically via direct link or dashboard)
2. **Loads a batch** by entering batch number and selecting modules
3. **Clicks "Preview batch report"** button
4. **Reviews PO attainment** in the "PO attainment in the selected scope" section
5. **Finds a PO with status "Below target"**
6. **Clicks "Create CQI Plan"** button next to the PO
7. **Modal form appears** with:
   - PO code and title (auto-populated)
   - Current attainment % (read-only, from batch report)
   - Target attainment % (editable, defaults to current + 10%)
   - Planned actions textarea
8. **Fills in the form** with improvement actions and timeline
9. **Clicks "Create Plan"** → Plan is created with status "IN_PROGRESS"

### Managing CQI Plans

1. **Below the PO tables**, a "CQI Plans for Batch {batch}" section appears
2. **Shows all plans** for the batch in card format
3. **Each card displays**:
   - PO code and status badge
   - Current attainment and target
   - Planned actions summary
   - Created by username
   - Date created
4. **Admin can click "Mark Completed"** on in-progress plans
5. **Plan status transitions**: IN_PROGRESS → COMPLETED

## Database Schema

The existing `cqi_action` table is reused for PO-level plans:
- `id` - Auto-increment ID
- `po_id` - Foreign key to ProgramOutcome (for PO plans)
- `batch` - Batch identifier
- `module_id` - Optional module reference
- `status` - PLANNED, IN_PROGRESS, COMPLETED, CLOSED
- `attainment_score` - Current PO attainment %
- `target_attainment` - Goal attainment %
- `action_plan` - Planned improvement actions
- `created_by` - Admin username
- `approved_by` - Admin username (set on creation)
- `created_at`, `updated_at` - Timestamps
- `is_deleted`, `deleted_at`, `deleted_by` - Soft delete fields

## Key Features

✅ **No Approval Workflow** - Admin creates plans directly (submitted=true, status=IN_PROGRESS)
✅ **History Tracking** - Created date, updated date, closed date automatically tracked
✅ **Status Workflow** - PLANNED → IN_PROGRESS → COMPLETED → CLOSED
✅ **Role-Based Access** - Only admins can create/manage plans
✅ **Batch-Scoped Plans** - Each plan tied to a specific batch and PO
✅ **Attainment Metrics** - Tracks current vs target attainment
✅ **Action Documentation** - Stores planned improvement actions

## Testing Checklist

- [ ] Header no longer shows Student reports, Batch reports, PO credit summary links
- [ ] Backend compiles without errors
- [ ] Frontend compiles without errors
- [ ] Can access batch reports (via direct link or admin dashboard)
- [ ] Can generate batch report preview
- [ ] "Create CQI Plan" button appears for POs with status != "Achieved"
- [ ] Can click button and modal appears
- [ ] Modal form validates inputs
- [ ] Can submit form and plan is created
- [ ] Plans appear in "CQI Plans for Batch" section
- [ ] Can update plan status to "Completed"
- [ ] Plans persist after page refresh
- [ ] Proper error handling for API failures

## API Endpoints Summary

| Method | Endpoint | Role | Purpose |
|--------|----------|------|---------|
| POST | `/api/cqi/po/create` | Admin | Create PO CQI plan |
| GET | `/api/cqi/batch/{batch}` | Admin | List batch CQI plans |
| PUT | `/api/cqi/{id}/status` | Admin | Update plan status |
| PUT | `/api/cqi/{id}/details` | Admin | Update plan content |
| GET | `/api/cqi/my-plans` | Lecturer | List own plans |
| GET | `/api/cqi/pending` | Admin | Review pending LO plans |
| POST | `/api/cqi/{id}/submit` | Lecturer | Submit LO plan |
| PUT | `/api/cqi/{id}/approve` | Admin | Approve LO plan |

## Notes

- CQI plans inherit the `CqiAction` model which originally supported LO-level plans
- `los_id` remains null for PO plans (only `po_id` is set)
- The feature integrates with existing batch report infrastructure
- Soft delete support is already in place for all plans
- Admin must have proper JWT token with "admin" role
