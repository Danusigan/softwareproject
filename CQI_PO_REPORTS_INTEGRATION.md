# CQI Integration with PO Reports (Updated)

## Changes Made

### 1. Admin Dashboard
- ✅ Removed "Batch Reports" button
- ✅ Removed "View Modules" button
- Dashboard now only shows module management

### 2. PO Reports Page Integration
**File**: `softwareproject_frontend/src/pages/PoReportsPage.jsx`

Enhanced the **Batch PO Success Report** section with:
- Imported CqiPlanModal and CqiPlansDisplay components
- Added state for CQI modal management
- Added "+ CQI" button to each PO row with status ≠ "Attained"
- Shows CqiPlansDisplay after report loads
- Displays CqiPlanModal when button clicked

### 3. CQI Plan Features for PO Reports

**PO-Only (NOT LO)**:
- ✅ Plans created for `po.code` (Program Outcome ID)
- ✅ No LO involvement (`los_id` remains NULL)
- ✅ Status workflow: PLANNED → IN_PROGRESS → COMPLETED → CLOSED
- ✅ No approval needed (admin creates directly)

## 🧪 Testing Flow

### Step 1: Access PO Reports
1. Login as admin
2. Navigate to **PO Attainment Reports** (click link in navigation or direct URL)
3. Scroll to **"Batch PO Success Report"** section

### Step 2: Generate Report
1. Enter **Batch** number (e.g., "22")
2. Set **Student threshold** (e.g., 40%)
3. Set **Batch success target** (e.g., 60%)
4. Click **"Preview"** button

### Step 3: Create CQI Plan for PO
1. Report loads → Shows table with all POs
2. Find a PO with status **NOT "Attained"** or **NOT "Success"**
3. Click **"+ CQI"** button on that PO row
4. Modal appears:
   - PO code and title (auto-filled)
   - Current attainment % (read-only, from report)
   - Target attainment % (editable)
   - Planned actions (textarea)
5. Fill form with improvement details
6. Click **"Create Plan"** → Plan created instantly

### Step 4: View CQI Plans
1. After report loads, scroll below the report table
2. See **"CQI Plans for Batch {batch}"** section
3. View all created plans in card format:
   - PO code with status badge
   - Current vs target attainment
   - Planned actions summary
   - Created by admin
4. Click **"Mark Completed"** to update status

## Key Points

✅ **PO-Only**: CQI plans are created for Program Outcomes, NOT Learning Outcomes
✅ **Batch Scoped**: Each plan tied to a specific batch and PO
✅ **No Approval**: Plans go directly to "IN_PROGRESS" status
✅ **Auto Tracking**: Created date, updated date automatically managed
✅ **Status Flow**: PLANNED → IN_PROGRESS → COMPLETED → CLOSED
✅ **Data Persistence**: All plans stored in `cqi_action` table

## API Endpoints Called

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/reports/po/batch` | GET | Fetch batch PO success report |
| `/api/cqi/po/create` | POST | Create new PO CQI plan |
| `/api/cqi/batch/{batch}` | GET | List all plans for batch |
| `/api/cqi/{id}/status` | PUT | Update plan status |

## Database Records

**Table**: `cqi_action`
- `po_id` → Set (ProgramOutcome reference)
- `los_id` → NULL (no LO involvement)
- `batch` → Batch identifier
- `status` → Plan status
- `attainment_score` → Current PO attainment %
- `target_attainment` → Goal attainment %
- `action_plan` → Planned improvement actions
- `created_by`, `approved_by` → Admin username
- `created_at`, `updated_at` → Timestamps

## Troubleshooting

**"+ CQI" button doesn't appear?**
- Check PO status is NOT "Attained" or "Success"
- Only POs below target show CQI button

**Modal doesn't open?**
- Check browser console for errors
- Ensure admin has proper JWT token
- Verify `/api/cqi/po/create` endpoint is accessible

**CQI plans don't persist?**
- Check database connection
- Verify `cqi_action` table exists
- Check database logs for SQL errors

## Summary

CQI plans are now created from **PO Reports**, triggered when admin clicks "Preview" to generate a batch PO success report. Each PO below target can have a CQI plan created directly (no approval workflow). Plans are tracked with full history and status management, all at the PO level with no LO involvement.
