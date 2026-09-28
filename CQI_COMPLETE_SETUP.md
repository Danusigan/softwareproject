# CQI Complete Setup - Final

## System Architecture

### 1️⃣ **PO Reports Page** (`/po-reports`)
Shows batch PO attainment with ability to create **PO CQI plans**

**Sections:**
- 🎯 **CQI Plans for Batch {N}** [PROGRAM OUTCOMES ONLY]
  - Active PO plans (can edit/mark complete)
  - Admin creates directly (no approval)
  - Status: IN_PROGRESS → COMPLETED

- 📋 **CQI Review History** [LEARNING OUTCOMES - READ ONLY]
  - Historical LO plans (reference/audit trail)
  - Cannot edit (read-only)

---

### 2️⃣ **CQI Review Queue** (`/cqi-review`)
Shows **LO CQI plans** requiring admin review/approval

**Sections:**
- ⏳ **Pending LO CQI Plans** [AWAITING APPROVAL]
  - LO plans submitted by lecturers
  - Admin can: Approve or Return for Revision
  - Status: PLANNED (submitted=true)
  - Count badge shows number pending

- 📋 **Approved & Completed Plans** [HISTORY]
  - LO plans already approved (status: IN_PROGRESS)
  - LO plans completed (status: COMPLETED)
  - Read-only (grayed out, no edit buttons)
  - Shows approval details

---

## Flow Diagram

```
┌─────────────────────────────────────────────┐
│         PO Reports Page                      │
│  Generate Batch PO Attainment Report        │
│  ├─ [Table: POs with "+ CQI" buttons]       │
│  ├─ 🎯 CQI Plans for Batch 24               │
│  │  ├─ PO001 (IN_PROGRESS) [editable]       │
│  │  └─ PO002 (IN_PROGRESS) [editable]       │
│  │                                           │
│  └─ 📋 CQI Review History                   │
│     ├─ LO: EC6544 LO2 (PLANNED)             │
│     └─ LO: EC4356 LO2 (PLANNED)             │
└─────────────────────────────────────────────┘
                    ↓
        Admin creates PO plan directly
        (No approval needed)
                    ↓
            [Database Saved]

────────────────────────────────────────────────

┌─────────────────────────────────────────────┐
│       CQI Review Queue Page                  │
│  Review LO CQI Plans Submitted by Lecturers │
│                                              │
│  ⏳ Pending LO CQI Plans                    │
│     └─ LO: EC6544 LO1                       │
│        Root cause: ...                      │
│        Action: ...                          │
│        [Approve] [Return for Revision]      │
│                                              │
│  📋 Approved & Completed Plans              │
│     ├─ LO: EC6544 LO1 (IN_PROGRESS)        │
│     └─ LO: EC4356 LO2 (COMPLETED)          │
│        (read-only, no buttons)              │
└─────────────────────────────────────────────┘
```

---

## Data Filtering Logic

### PO Plans (from PO Reports)
```javascript
Filter: !plan.losId && plan.poId
Status: IN_PROGRESS (created directly by admin)
Actions: Edit details, Mark Completed
```

### LO Plans - Pending (from CQI Review)
```javascript
Filter: plan.losId && plan.status === 'PLANNED' && plan.submitted === true
Actions: Approve (→ IN_PROGRESS), Return for Revision
```

### LO Plans - History (from both pages)
```javascript
Filter: plan.losId && (plan.status === 'IN_PROGRESS' || plan.status === 'COMPLETED')
Actions: View only (read-only)
```

---

## Complete Testing Flow

### Step 1: Create PO CQI Plan
```
Navigate to PO Reports
├─ Generate Batch PO report (e.g., Batch 24)
├─ Click "+ CQI" on PO below target
├─ Fill form: Target & Planned Actions
└─ Plan appears in "CQI Plans for Batch" section
   └─ Status: IN_PROGRESS
   └─ Can mark COMPLETED
```

### Step 2: View LO CQI History
```
In same PO Reports page
└─ Scroll to "CQI Review History" section
   ├─ Shows historical LO plans
   ├─ Status: PLANNED, IN_PROGRESS, COMPLETED
   └─ Read-only (no edit buttons)
```

### Step 3: Review LO CQI Plans
```
Navigate to CQI Review Queue
├─ See "Pending LO CQI Plans" section (if any)
│  ├─ LO: EC6544 LO1
│  ├─ Root cause, Action plan shown
│  ├─ [Approve] button → Status: IN_PROGRESS
│  └─ [Return for Revision] → Lecturer re-edits
│
└─ See "Approved & Completed Plans" section
   ├─ Shows approved LO plans (IN_PROGRESS)
   └─ Shows completed LO plans (COMPLETED)
      └─ Read-only display
```

---

## Page Navigation

| Page | URL | Purpose | Shows |
|------|-----|---------|-------|
| PO Reports | `/po-reports` | Create PO plans | PO plans + LO history |
| CQI Review | `/cqi-review` | Review LO plans | Pending LO plans + history |

---

## Key Points

✅ **PO Plans** - Created directly by admin from PO Reports
✅ **LO Plans** - Submitted by lecturers, approved by admin in CQI Review
✅ **No Mixing** - PO and LO plans kept completely separate
✅ **History Tracking** - All plans kept for audit trail
✅ **Clear Sections** - Visual badges distinguish active from read-only
✅ **Role-Based** - Only admins can approve/edit plans

---

## Common Questions

**Q: Where do I create PO plans?**
A: PO Reports page → Generate report → Click "+ CQI" button

**Q: Where do I review LO plans?**
A: CQI Review Queue page → Approve or return for revision

**Q: Can I edit PO plans after creating?**
A: Yes, they stay editable until marked COMPLETED

**Q: Can I see old LO plans?**
A: Yes, in both places:
- LO history section in PO Reports (reference)
- Completed section in CQI Review (audit trail)

**Q: Are PO plans approved by anyone?**
A: No, admin creates them directly (no approval workflow)

**Q: Are LO plans approved?**
A: Yes, admin must approve or return them in CQI Review Queue
