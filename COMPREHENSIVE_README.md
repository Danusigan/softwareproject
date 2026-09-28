# LO-PO Analytics (OBQA) - Comprehensive Documentation

**Learning Outcome → Program Outcome Attainment and Accreditation Reporting System**  
University of Ruhuna, Faculty of Engineering

![Version](https://img.shields.io/badge/Version-2.0.0-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-green)
![React](https://img.shields.io/badge/React-18-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)

---

## 📋 Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [System Components](#system-components)
5. [Features](#features)
6. [Role-Based Access Control](#role-based-access-control)
7. [Database Schema](#database-schema)
8. [API Endpoints](#api-endpoints)
9. [Frontend Pages & Components](#frontend-pages--components)
10. [Setup & Installation](#setup--installation)
11. [Running the Application](#running-the-application)
12. [Testing](#testing)
13. [Security Features](#security-features)
14. [Known Issues & Limitations](#known-issues--limitations)

---

## 🎯 Project Overview

The **LO-PO Analytics System** is an enterprise-grade application designed for academic institutions to:

- **Define Learning Outcomes (LOs)** per module
- **Map LOs to Program Outcomes (POs)** for accreditation
- **Upload and Track Student Assessment Marks**
- **Calculate PO Attainment** from assessment results
- **Generate Accreditation Reports** for compliance
- **Implement Continuous Quality Improvement (CQI)** cycles
- **Track Student Progress** across curriculum versions with retake policies
- **Produce Batch-Level Analytics** for program analysis

**Key Users:**
- **Lecturers**: Create LOs, upload marks, propose LO-PO mappings, fill CQI improvement plans
- **Admins**: Manage modules, lecturers, students, approve mappings, review CQI plans
- **Super Admins**: Create admins, full system access
- **Students**: View personal progress reports (future phase)

---

## 🏗️ Architecture

### Layered Architecture
```
┌─────────────────────────────────────────┐
│        React Frontend (Port 5173)       │
│     TailwindCSS, React Router, Axios    │
└──────────────┬──────────────────────────┘
               │ REST API (Port 8080)
┌──────────────▼──────────────────────────┐
│      Spring Boot REST Controllers       │
│      (19+ Controllers)                  │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│   Service Layer (22+ Services)          │
│   Business Logic, Calculations          │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  Data Access Layer (JPA Repositories)   │
│  Spring Data JPA, Hibernate             │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│    MySQL 8.0 (InnoDB)                   │
│    27 Tables, Flyway Migrations         │
└─────────────────────────────────────────┘
```

---

## 💻 Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.2
- **Language**: Java 17
- **ORM**: Spring Data JPA / Hibernate
- **Security**: Spring Security + JWT
- **Database**: MySQL 8.0
- **Excel**: Apache POI
- **PDF**: OpenPDF
- **Build**: Maven
- **Testing**: JUnit 5, Mockito

### Frontend
- **UI Framework**: React 18
- **Build Tool**: Vite 5
- **Styling**: TailwindCSS
- **Routing**: React Router 7
- **HTTP Client**: Axios
- **Testing**: Jest, React Testing Library

### DevOps
- **Containerization**: Docker, Docker Compose
- **CI/CD**: GitHub Actions
- **Secrets Management**: GitHub Secrets
- **Database Migration**: Flyway

---

## 🔧 System Components

### Backend Components

#### 1. **REST Controllers** (19 Controllers)

**User Management**
- `UserRestController` - User CRUD, authentication, password reset
- `ManageAdminsPage` - Admin management interface
- `ManageLecturersPage` - Lecturer management interface

**Module & LO Management**
- `ModuleRestController` - CRUD operations for modules
- `LosRestController` - Learning Outcome creation/management
- `AssessmentController` - Assessment template management
- `AssessmentService` - Assessment item operations

**Mapping & Outcome Management**
- `LOPOMappingRestController` - LO-PO mapping CRUD
- `ProgramOutcomeRestController` - Program Outcome management
- `MappingRestController` - Outcome mapping operations
- `AdminMappingRestController` - Admin approval workflows
- `LosPosRestController` - LO-POS relationship management
- `IntegratedLORestController` - Integrated LO operations

**Assessment & Marks**
- `OBEController` - Marks upload, Excel export, templates
- `AttainmentRestController` - Attainment calculations
- `StudentController` - Student management

**Reporting**
- `ProgressController` - Student progress reports (curriculum-versioned)
- `BatchReportController` - Batch-level anonymized reports
- `PoReportController` - Program Outcome attainment reports
- `ReportsRestController` - General reporting endpoints

**Quality Improvement**
- `CqiActionController` - CQI action management
- `AnalysisRestController` - Data analysis endpoints

#### 2. **Service Layer** (22+ Services)

**Core Services**
| Service | Purpose |
|---------|---------|
| `UserService` | User authentication, registration, password management |
| `ModuleService` | Module CRUD with cascade deletion handling |
| `LosService` | Learning Outcome management with dependencies |
| `ProgramOutcomeService` | PO CRUD and management |
| `StudentService` | Student records management |
| `AssessmentService` | Assessment template and item management |

**Mapping & Attainment**
| Service | Purpose |
|---------|---------|
| `LOPOMappingService` | LO-PO mapping creation and approval |
| `MappingService` | Legacy mapping operations |
| `LosPosService` | Legacy LOS-POS relationship management |
| `POAttainmentService` | PO attainment calculation from marks |
| `AttainmentService` | LO attainment calculation for batches |

**Reporting & Analysis**
| Service | Purpose |
|---------|---------|
| `ProgressService` | Student progress report generation (curriculum-aware) |
| `BatchReportService` | Batch-level attainment calculation (anonymized) |
| `PoReportService` | PO-level attainment reporting |
| `CQIService` | CQI action lifecycle: trigger → plan → approve → close |
| `TrendService` | Trend analysis across semesters |

**File Operations**
| Service | Purpose |
|---------|---------|
| `ExcelImportService` | Import marks from Excel (with validation) |
| `ExcelExportService` | Export marks/reports to Excel |
| `FileValidationService` | Validate uploaded files (type, size, content) |

**Infrastructure**
| Service | Purpose |
|---------|---------|
| `EmailService` | Email notifications (password reset, etc.) |
| `PasswordResetService` | Token-based password reset flow |
| `AuditLogService` | Audit trail logging |
| `CustomUserDetailsService` | Spring Security integration |
| `DataInitializationService` | Seed test data |
| `LegacySchemaFixService` | Handle legacy database migrations |

#### 3. **Data Models** (20+ Entity Classes)

**User & Authentication**
```
User (Core user record)
├── username: String
├── email: String
├── password: String (BCrypt hashed)
├── usertype: Enum (SUPERADMIN, ADMIN, LECTURE)
├── accountLocked: boolean
├── loginAttempts: int
└── Timestamps

PasswordResetToken
├── token: String (UUID)
├── user: User (FK)
└── expiryTime: LocalDateTime
```

**Academic Structure**
```
Module (Course/Unit)
├── moduleId: String (Primary Key)
├── moduleName: String
├── assignedLecturers: List<User> (ManyToMany)
└── Timestamps

Los (Learning Outcome)
├── id: String
├── name: String
├── module: Module (FK)
├── attainmentThreshold: Double (0-100)
└── description: Text

ProgramOutcome (Program-level outcome)
├── poId: String
├── code: String
├── description: Text
└── Timestamps

OutcomeMapping (LO → PO mapping)
├── id: Long
├── los: Los (FK)
├── programOutcome: ProgramOutcome (FK)
├── weight: Integer
├── status: Enum (PROPOSED, APPROVED, REJECTED)
├── mapBy: String (lecturer username)
├── approvedBy: String (admin username)
└── Timestamps
```

**Assessment & Marks**
```
AssessmentTemplate (Per-module template)
├── id: String
├── module: Module (FK)
├── name: String
└── assessmentItems: List<AssessmentItem>

AssessmentItem (Question/Component)
├── id: Long
├── assessmentTemplate: AssessmentTemplate (FK)
├── los: Los (FK)
├── name: String
├── maxMarks: Integer
└── markType: MarkType (NUMERIC, PERCENTAGE)

StudentMark (Individual mark record)
├── id: Long
├── student: Student (FK)
├── los: Los (FK)
├── markLabel: String (e.g., "Assignment 1")
├── mark: Double
└── Timestamps

StudentAssessmentScore (Question-level score)
├── id: Long
├── student: Student (FK)
├── assessmentItem: AssessmentItem (FK)
├── score: Double
└── Timestamps
```

**Student & Attainment**
```
Student (Student record)
├── studentId: String (e.g., "EG/2024/6555")
├── firstName: String
├── lastName: String
├── email: String
└── Timestamps

StudentPoCredit (PO credit earned per module)
├── id: Long
├── student: Student (FK)
├── po: ProgramOutcome (FK)
├── moduleId: String
├── batch: String
├── creditsEarned: Integer
├── maxCredits: Integer
└── markType: MarkType
```

**Quality Improvement**
```
CqiAction (CQI improvement cycle)
├── id: Long
├── module: Module (FK)
├── los: Los (FK, nullable)
├── batch: String
├── attainmentScore: Double
├── targetScore: Double
├── rootCause: Text
├── actionPlan: Text
├── actionType: CqiActionType (ADD_LAB_SESSION, REVISE_ASSESSMENT, etc.)
├── targetAttainment: Double
├── deadline: LocalDate
├── status: CqiStatus (PLANNED, IN_PROGRESS, COMPLETED)
├── submitted: boolean
├── approvedBy: String
├── nextSemAttainment: Double
└── Timestamps
```

**Audit & Reporting**
```
AuditLog (System audit trail)
├── id: Long
├── actor: String
├── action: String
├── resource: String
├── resourceId: String
├── timestamp: LocalDateTime
└── details: Text

TrendReportDTO (Trend analysis)
├── semester: String
├── poCode: String
├── attainmentScore: Double
└── studentCount: Integer
```

---

## ✨ Features

### 1. **User Management** ✅
- [x] User registration (by admin)
- [x] JWT-based authentication
- [x] Role-based access control (SUPERADMIN, ADMIN, LECTURE)
- [x] BCrypt password hashing
- [x] Login lockout after 5 failed attempts
- [x] Password reset via email tokens
- [x] User profile management

### 2. **Module Management** ✅
- [x] Create, read, update, delete modules
- [x] Assign lecturers to modules
- [x] Module visibility (unassigned modules visible to all lecturers)
- [x] ❌ **BUG**: Module deletion fails due to FK constraint from `qa_curriculum_module`

### 3. **Learning Outcome Management** ✅
- [x] Create LOs per module
- [x] Set LO attainment thresholds
- [x] Edit/view LO details
- [x] Cascade deletion of related data

### 4. **Assessment & Marks** ✅
- [x] Create assessment templates per module
- [x] Define assessment items (questions) with max marks
- [x] Bulk upload student marks (Excel)
- [x] Question-wise mark uploads
- [x] Mark validation (0-100, within max marks)
- [x] Support multiple assignments per LO
- [x] Null marks (missing assessments) not counted as zero
- [x] Export marks to Excel

### 5. **Learning Outcome → Program Outcome Mapping** ✅
- [x] Lecturers propose LO-PO mappings with weights
- [x] Admins review and approve/reject mappings
- [x] Track mapping status (PROPOSED, APPROVED, REJECTED)
- [x] Audit trail of mapping changes
- [x] Prevent duplicate mappings

### 6. **PO Attainment Calculation** ✅
- [x] Calculate per-LO percentage from assessment marks
- [x] Calculate per-student PO attainment
- [x] Calculate batch-level PO attainment
- [x] Multi-assignment support (accumulates across assignments)
- [x] ❌ **LIMITATION**: Credits calculated but NOT persisted to database

### 7. **Continuous Quality Improvement (CQI)** ✅
- [x] Auto-trigger CQI action when LO attainment < threshold
- [x] Lecturers fill improvement plans (root cause, action, deadline)
- [x] CQI action types: ADD_LAB_SESSION, REVISE_ASSESSMENT, CHANGE_TEACHING_METHOD, ADD_RESOURCE, REDESIGN_LO
- [x] Admin approval workflow
- [x] Link next semester results to track effectiveness
- [x] CQI history & trend analysis
- [x] Generate CQI audit trail for accreditation

### 8. **Student Progress Reports (Curriculum-Versioned)** ✅
- [x] Configure curriculum with modules, LOs, POs
- [x] Define academic periods (semesters)
- [x] Record student module attempts with status (PASS, FAIL, ABSENT, EXEMPT, WITHDRAWN)
- [x] Retake policies:
  - [x] **OFFICIAL**: Exactly one explicitly marked official attempt
  - [x] **LATEST_COMPLETED**: Highest attempt number among PASS/FAIL
  - [x] **BEST**: Highest final mark among PASS/FAIL attempts
- [x] Generate immutable student progress snapshots
- [x] PDF export of progress reports
- [x] Student curriculum assignment
- [x] Academic status tracking (IN_PROGRESS, COMPLETED, WITHDRAWN)
- [x] GPA calculation (credit-weighted if configured)
- [x] Graduation requirements check

### 9. **Batch-Level Reports** ✅
- [x] Anonymized batch attainment reports
- [x] Per-LO pass rates
- [x] Per-PO attainment
- [x] Batch comparison
- [x] Export to Excel/PDF

### 10. **Excel Import/Export** ✅
- [x] Marks upload templates
- [x] Question-wise mark templates
- [x] Bulk mark import with validation
- [x] Export attainment data
- [x] Export PO credits
- [x] Export by-LO pass rates

### 11. **Reporting & Analytics** ✅
- [x] Module-level PO attainment
- [x] Trend analysis (semester-over-semester)
- [x] Batch comparison reports
- [x] Per-student progress tracking
- [x] PDF generation for official reports

### 12. **Security** ✅
- [x] JWT authentication with expiry
- [x] RBAC (Role-Based Access Control)
- [x] BCrypt password hashing
- [x] CORS protection
- [x] Login attempt lockout
- [x] File upload validation
- [x] SQL injection prevention (parameterized queries)
- [x] Audit logging of all actions
- [x] Password reset via secure tokens
- [x] No plaintext secrets in code

### 13. **Database** ✅
- [x] Flyway schema migrations
- [x] 27 tables across legacy and Progress subsystems
- [x] Proper FK relationships
- [x] Cascade delete handling
- [x] Timestamp auditing (createdAt, updatedAt)
- [x] H2 for testing, MySQL for production

### 14. **Frontend UI/UX** ✅
- [x] Responsive design (TailwindCSS)
- [x] Role-based dashboards
  - Lecturer dashboard with assigned modules
  - Admin dashboard with CQI review queue
  - Super admin dashboard with user management
- [x] Data validation before submission
- [x] Loading states and error handling
- [x] Modal dialogs for confirmations
- [x] Search and filter capabilities
- [x] Table pagination and sorting
- [x] Color-coded status indicators

---

## 👥 Role-Based Access Control

### Roles & Permissions

| Feature | Super Admin | Admin | Lecturer | Student |
|---------|------------|-------|----------|---------|
| **User Management** | | | | |
| Create/View Admins | ✅ | ❌ | ❌ | ❌ |
| Create/Manage Lecturers | ✅ | ✅ | ❌ | ❌ |
| Create/Manage Students | ✅ | ✅ | ❌ | ❌ |
| Reset Passwords | ✅ | ✅ | ❌ | ❌ |
| **Module Management** | | | | |
| Create/Edit/Delete Modules | ✅ | ✅ | ❌ | ❌ |
| Assign Lecturers | ✅ | ✅ | ❌ | ❌ |
| View Assigned Modules | ✅ | ✅ | ✅ | ❌ |
| **LO Management** | | | | |
| Create/Edit/Delete LOs | ✅ | ❌ | ✅* | ❌ |
| Propose LO-PO Mappings | ✅ | ❌ | ✅ | ❌ |
| **Assessment** | | | | |
| Upload Marks | ✅ | ❌ | ✅ | ❌ |
| Create Assessments | ✅ | ❌ | ✅ | ❌ |
| **Mapping Approval** | | | | |
| Approve LO-PO Mappings | ✅ | ✅ | ❌ | ❌ |
| **CQI** | | | | |
| Trigger CQI | ✅ | ✅ | Auto | ❌ |
| Submit CQI Plans | ✅ | ❌ | ✅ | ❌ |
| Approve CQI Plans | ✅ | ✅ | ❌ | ❌ |
| View CQI History | ✅ | ✅ | ✅* | ❌ |
| **Reports** | | | | |
| View All Reports | ✅ | ✅ | ✅* | ❌ |
| Generate Batch Reports | ✅ | ✅ | ❌ | ❌ |
| View Student Progress | ✅ | ✅ | ✅* | ✅* |
| **Configuration** | | | | |
| Configure Curriculum | ✅ | ✅ | ❌ | ❌ |
| Configure Academic Periods | ✅ | ✅ | ❌ | ❌ |
| Record Enrolments | ✅ | ✅ | ❌ | ❌ |

*Only for modules/students they're assigned to

---

## 🗄️ Database Schema

### Schema Overview

**Legacy Schema (V1)** - 13 Tables
```
users, students, modules, los, los_pos, program_outcomes, 
outcome_mappings, assessment_templates, assessment_items,
student_marks, student_assessment_scores, cqi_actions, audit_logs
```

**Progress Schema (V2)** - 14 Tables
```
qa_programme, qa_curriculum, qa_curriculum_module, qa_curriculum_lo,
qa_curriculum_po, qa_curriculum_mapping, qa_student_programme,
qa_academic_period, qa_module_offering, qa_offering_assessment,
qa_module_enrolment, qa_offering_item, qa_report_snapshot, qa_report_audit
```

### Key Tables

**users** (Authentication & Authorization)
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(255) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL (BCrypt),
  usertype ENUM('SUPERADMIN', 'ADMIN', 'LECTURE') NOT NULL,
  account_locked BOOLEAN DEFAULT FALSE,
  login_attempts INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**modules** (Course Units)
```sql
CREATE TABLE modules (
  module_id VARCHAR(255) PRIMARY KEY,
  module_name VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**los** (Learning Outcomes)
```sql
CREATE TABLE los (
  id VARCHAR(255) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  module_id VARCHAR(255) NOT NULL,
  attainment_threshold DOUBLE,
  description TEXT,
  FOREIGN KEY (module_id) REFERENCES modules(module_id)
);
```

**student_marks** (Assessment Marks)
```sql
CREATE TABLE student_marks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id VARCHAR(255) NOT NULL,
  los_id VARCHAR(255) NOT NULL,
  mark_label VARCHAR(255),
  mark DOUBLE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (student_id) REFERENCES students(student_id),
  FOREIGN KEY (los_id) REFERENCES los(id)
);
```

**outcome_mappings** (LO → PO Mappings)
```sql
CREATE TABLE outcome_mappings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  los_id VARCHAR(255) NOT NULL,
  po_id VARCHAR(255) NOT NULL,
  weight INT NOT NULL,
  status ENUM('PROPOSED', 'APPROVED', 'REJECTED') DEFAULT 'PROPOSED',
  mapped_by VARCHAR(255),
  approved_by VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (los_id) REFERENCES los(id),
  FOREIGN KEY (po_id) REFERENCES program_outcomes(po_id)
);
```

**cqi_actions** (Improvement Actions)
```sql
CREATE TABLE cqi_actions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  module_id VARCHAR(255) NOT NULL,
  los_id VARCHAR(255),
  batch VARCHAR(255) NOT NULL,
  attainment_score DOUBLE,
  target_score DOUBLE,
  reason TEXT,
  action_description TEXT,
  action_type ENUM('ADD_LAB_SESSION', 'REVISE_ASSESSMENT', ...),
  status ENUM('PLANNED', 'IN_PROGRESS', 'COMPLETED') DEFAULT 'PLANNED',
  submitted BOOLEAN DEFAULT FALSE,
  created_by VARCHAR(255),
  approved_by VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (module_id) REFERENCES modules(module_id),
  FOREIGN KEY (los_id) REFERENCES los(id)
);
```

---

## 🔌 API Endpoints

### Authentication
```
POST   /api/auth/register          - User registration
POST   /api/auth/login             - User login (returns JWT)
POST   /api/auth/refresh           - Refresh JWT token
POST   /api/auth/logout            - Logout
```

### User Management
```
GET    /api/users/all              - List all users (admin)
GET    /api/users/{id}             - Get user details
PUT    /api/users/{id}             - Update user
DELETE /api/users/{id}             - Delete user
POST   /api/users/change-password  - Change password
POST   /api/auth/forgot-password   - Request password reset
POST   /api/auth/reset-password    - Reset password with token
```

### Module Management
```
GET    /api/modules/all            - List all modules (role-aware)
GET    /api/modules/{id}           - Get module details
POST   /api/modules/create         - Create module (admin)
PUT    /api/modules/{id}           - Update module (admin)
DELETE /api/modules/{id}           - Delete module (admin)
```

### Learning Outcomes
```
GET    /api/los/module/{moduleId}  - Get LOs for module
GET    /api/los/{id}               - Get LO details
POST   /api/los/create             - Create LO (lecturer)
PUT    /api/los/{id}               - Update LO (lecturer)
DELETE /api/los/{id}               - Delete LO (lecturer)
GET    /api/los/all                - List all LOs
```

### Program Outcomes
```
GET    /api/program-outcomes/all   - List all POs
GET    /api/program-outcomes/{id}  - Get PO details
POST   /api/program-outcomes       - Create PO (admin)
PUT    /api/program-outcomes/{id}  - Update PO (admin)
DELETE /api/program-outcomes/{id}  - Delete PO (admin)
```

### LO-PO Mapping
```
GET    /api/mappings/all           - List all mappings
GET    /api/mappings/lo/{loId}     - Get mappings for LO
POST   /api/mappings/create        - Create mapping (lecturer)
PUT    /api/mappings/{id}/approve  - Approve mapping (admin)
PUT    /api/mappings/{id}/reject   - Reject mapping (admin)
DELETE /api/mappings/{id}          - Delete mapping
GET    /api/mappings/pending       - Get pending mappings (admin)
```

### Assessment & Marks
```
POST   /api/obe/marks/upload       - Upload marks (bulk)
POST   /api/obe/marks/upload-question-wise - Upload question-wise marks
GET    /api/obe/marks/{studentId}/{moduleId} - Get student marks
GET    /api/obe/export/marks       - Export marks as Excel
GET    /api/obe/template/marks     - Get mark upload template
GET    /api/obe/template/marks-question-wise - Get Q-wise template
POST   /api/assessment/create      - Create assessment template
GET    /api/assessment/{id}        - Get assessment template
```

### Attainment Calculation
```
GET    /api/attainment/lo/{loId}/batch/{batch} - LO attainment for batch
GET    /api/attainment/po/{poId}/module/{moduleId} - PO attainment
GET    /api/attainment/student/{studentId} - Student attainment
POST   /api/attainment/calculate   - Trigger calculation
```

### CQI Management
```
GET    /api/cqi/my-plans           - Get lecturer's CQI plans
POST   /api/cqi/{id}/submit        - Submit CQI plan (lecturer)
GET    /api/cqi/pending            - Get pending plans (admin)
PUT    /api/cqi/{id}/approve       - Approve plan (admin)
PUT    /api/cqi/{id}/return        - Return for revision (admin)
GET    /api/cqi/module/{moduleId}/history - CQI history
POST   /api/cqi/finalize/{moduleId} - Finalize batch attainment
```

### Reports
```
GET    /api/reports/batch          - Get batch reports
GET    /api/reports/batch/{id}/pdf - Download batch report PDF
GET    /api/reports/po/{poId}      - Get PO attainment report
GET    /api/reports/trend/{moduleId} - Get trend analysis
```

### Progress Reports (Curriculum-Versioned)
```
GET    /api/reports/progress/students?q=... - Search students
POST   /api/reports/progress/students/snapshots?studentId=... - Generate snapshot
GET    /api/reports/progress/snapshots/{reference} - Read snapshot
GET    /api/reports/progress/snapshots/{reference}/pdf - Download PDF
GET    /api/reports/progress/configuration - Get config
POST   /api/reports/progress/configuration/curricula - Configure curriculum
PUT    /api/reports/progress/configuration/student-programmes - Assign curriculum
POST   /api/reports/progress/configuration/offerings - Configure offerings
PUT    /api/reports/progress/configuration/enrolments - Record attempt
```

### Excel Export
```
GET    /api/obe/export/po-attainment - Export PO attainment
GET    /api/obe/export/marks-per-lo-threshold - Export by-LO metrics
GET    /api/obe/marks/export/module/{moduleId} - Export module marks
```

---

## 🎨 Frontend Pages & Components

### Shared Components
- **Header** (`header.jsx`) - Navigation, user menu, logout
- **Footer** (`footer.jsx`) - Footer with links
- **ProtectedRoute** - Role-based route protection
- **ModuleModal** - Create/edit module modal
- **MultiSelectAutocomplete** - Lecturer selection component
- **ProgressConfigurationPanel** - Curriculum configuration UI

### Pages by Role

#### 📚 Lecturer Pages
- **`lecturerdashboard.jsx`** - Assigned modules, CQI overview
- **`modulespage.jsx`** - View assigned modules, manage LOs
- **`LODetailPage.jsx`** - View/edit individual LO
- **`CreateLOWithMappingPage.jsx`** - Create LO and propose mappings
- **`MarksWorkbenchPage.jsx`** - Upload marks, manage assessments
- **`MyCqiPlansPage.jsx`** - View own CQI plans, submit improvements
- **`StudentReportsPage.jsx`** - Generate and view student progress
- **`StudentPOSummaryPage.jsx`** - Student PO attainment summary

#### 👨‍💼 Admin Pages
- **`admindashboard.jsx`** - Admin overview, CQI review queue
- **`CqiReviewPage.jsx`** - Review and approve CQI plans
- **`ManageLecturersPage.jsx`** - Create, edit, delete lecturers
- **`ManageStudentsPage.jsx`** - Manage student records
- **`LOPOMappingManagementPage.jsx`** - Approve/reject LO-PO mappings
- **`ProgramOutcomesPage.jsx`** - CRUD Program Outcomes
- **`BatchReportsPage.jsx`** - Generate batch-level reports
- **`PoReportsPage.jsx`** - Generate PO attainment reports
- **`ComparisonPage.jsx`** - Compare batch/semester trends

#### 🔑 Super Admin Pages
- **`superadmindashboard.jsx`** - Super admin overview
- **`ManageAdminsPage.jsx`** - Create and manage admins

#### 🌐 Public Pages
- **`landingpage.jsx`** - Landing page
- **`loginpage.jsx`** - User login
- **`forgottenpasword.jsx`** - Password reset request
- **`resetpassword.jsx`** - Password reset form
- **`AddResultsPage.jsx`** - Alternative mark entry

---

## 🚀 Setup & Installation

### Prerequisites
```bash
# Check versions
java -version              # Java 17+
mvn -v                     # Maven 3.8+
node -v                    # Node 18+
npm -v                     # npm 8+
docker --version           # Docker 20+
docker-compose --version   # Docker Compose 2.0+
```

### Clone Repository
```bash
git clone https://github.com/Danusigan/softwareproject.git
cd softwareproject
```

### Environment Setup

**Create `.env` file** (root directory):
```env
DB_USERNAME=root
DB_PASSWORD=password
DB_NAME=obqa
DB_HOST=mysql
DB_PORT=3306

SPRING_PROFILE=dev
JWT_SECRET=your-secret-key-here-min-32-chars
JWT_EXPIRATION=86400000

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### Option 1: Docker (Recommended)

```bash
# Build images
docker-compose build

# Start services (MySQL + Backend + Frontend)
docker-compose up -d

# Check logs
docker-compose logs -f

# Access
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# PhpMyAdmin: http://localhost:8081 (optional)

# Stop
docker-compose down
```

### Option 2: Local Development

**Terminal 1: Backend**
```bash
cd Software-project-Backend

# Install dependencies (first time only)
mvn clean install

# Set environment variables
export DB_USERNAME=root
export DB_PASSWORD=password
export DB_NAME=obqa

# Run
mvn spring-boot:run

# Backend available at http://localhost:8080
```

**Terminal 2: Frontend**
```bash
cd softwareproject_frontend

# Install dependencies (first time only)
npm install

# Run dev server
npm run dev

# Frontend available at http://localhost:5173
```

### Database Setup

**MySQL 8.0 Installation**
```bash
# macOS
brew install mysql

# Linux (Ubuntu)
sudo apt-get install mysql-server

# Windows
# Download from https://dev.mysql.com/downloads/mysql/
```

**Create Database**
```sql
CREATE DATABASE obqa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'appuser'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON obqa.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;
```

---

## ▶️ Running the Application

### Development Mode

**Backend** (Spring Boot)
```bash
cd Software-project-Backend
mvn spring-boot:run

# Runs on http://localhost:8080
# Swagger API docs: http://localhost:8080/swagger-ui.html (if enabled)
```

**Frontend** (Vite Dev Server)
```bash
cd softwareproject_frontend
npm run dev

# Runs on http://localhost:5173 with hot reload
```

### Production Build

**Backend**
```bash
cd Software-project-Backend
mvn clean package -DskipTests

# Creates Software-project-Backend/target/application.jar
java -jar target/application.jar \
  --spring.datasource.username=$DB_USERNAME \
  --spring.datasource.password=$DB_PASSWORD \
  --spring.datasource.url=jdbc:mysql://localhost:3306/obqa
```

**Frontend**
```bash
cd softwareproject_frontend
npm run build

# Creates dist/ folder with optimized build
# Deploy dist/ contents to web server (nginx, Apache, etc.)
```

### Docker Production

```bash
# Build production images
docker-compose -f docker-compose.prod.yml build

# Start services
docker-compose -f docker-compose.prod.yml up -d

# Scale containers
docker-compose up -d --scale backend=3
```

---

## 🧪 Testing

### Backend Tests
```bash
cd Software-project-Backend

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Frontend Tests
```bash
cd softwareproject_frontend

# Run tests
npm test

# Run with coverage
npm test -- --coverage

# Watch mode
npm test -- --watch
```

### Integration Testing
```bash
# Backend integration tests with real database
mvn verify

# Frontend integration tests (if configured)
npm run test:integration
```

### Manual Testing

**Test Account Credentials** (default seed data):
```
Lecturer:
  Username: danu1
  Password: 1234
  
Admin:
  Username: admin
  Password: admin123

Module: EC6306
Batch: 24 (for marks)
```

---

## 🔐 Security Features

### Authentication & Authorization
- ✅ **JWT Tokens** - Stateless authentication with 24-hour expiry
- ✅ **RBAC** - Three roles (SUPERADMIN, ADMIN, LECTURE) with fine-grained permissions
- ✅ **Password Hashing** - BCrypt with salt
- ✅ **Login Lockout** - Account locked after 5 failed attempts
- ✅ **Password Reset** - Secure token-based reset via email

### Data Security
- ✅ **SQL Injection Prevention** - Parameterized queries (JPA)
- ✅ **XSS Prevention** - React's automatic escaping
- ✅ **CSRF Protection** - Spring Security CSRF tokens
- ✅ **File Upload Validation** - Type, size, and content checks
- ✅ **Secrets Management** - Environment variables (no hardcoded secrets)

### API Security
- ✅ **CORS Protection** - Configured for localhost:5173
- ✅ **Request Validation** - Input sanitization
- ✅ **Rate Limiting** - (Configurable in Spring Security)
- ✅ **Error Handling** - Global exception handler (no stack trace leakage)

### Audit & Compliance
- ✅ **Audit Logging** - All CRUD operations logged with actor/timestamp
- ✅ **Immutable Snapshots** - Student progress reports locked after generation
- ✅ **Timestamp Auditing** - createdAt, updatedAt on all entities
- ✅ **User Attribution** - Who proposed/approved mappings

### Infrastructure Security
- ✅ **HTTPS/TLS** - SSL certificates (in production)
- ✅ **Database Encryption** - MySQL connections encrypted
- ✅ **GitHub Secrets** - Sensitive data in GitHub Actions
- ✅ **Docker Security** - Non-root containers, read-only filesystems

---

## ⚠️ Known Issues & Limitations

### Bugs
1. **Module Deletion Fails** ❌
   - **Issue**: FK constraint from `qa_curriculum_module` prevents deletion
   - **Root Cause**: `ModuleService.deleteModule()` doesn't clean up Progress schema tables
   - **Fix**: Add cascade delete for `qa_*` tables before module delete
   - **Impact**: Admins cannot delete modules with curriculum references
   - **Status**: PENDING

### Limitations
1. **Student Credits Not Persisted**
   - Calculated in `POAttainmentService` but NOT saved to database
   - Only exists in Excel/JSON exports
   - Lost on page refresh
   - **Fix**: Implement `StudentPoCredit` entity persistence

2. **No Component-Level Retakes**
   - System supports module-level retakes (entire module repeat)
   - Doesn't support partial retakes (e.g., CA-only or exam-only repeat)
   - **Scope**: Future enhancement

3. **No Student Login Portal**
   - Students cannot view own progress via UI
   - Only admin/lecturer can generate reports
   - **Scope**: Future phase

4. **No Compensation/Condonation Rules**
   - Graduation only checks credits + GPA
   - No allowance for failing modules if GPA meets requirement
   - **Scope**: Future enhancement

5. **No Transfer Credit Support**
   - Cannot credit external qualifications
   - All credits must come from configured curriculum
   - **Scope**: Future enhancement

6. **No Professional Registration Integration**
   - Reports don't map to professional body requirements
   - **Scope**: Future enhancement

7. **Limited PDF Font Support**
   - Only Latin characters supported in PDF
   - Non-Latin script universities need custom fonts
   - **Fix**: Supply approved embedded font

### Performance Considerations
- Large batch reports (1000+ students) may take 30+ seconds
- Excel exports with question-wise marks are memory-intensive
- CQI trend calculation scans entire semester history

### Browser Support
- Chrome 90+ (tested)
- Firefox 88+ (tested)
- Safari 14+ (tested)
- Edge 90+ (tested)
- IE 11: NOT supported (ES6 syntax)

---

## 📚 Additional Documentation

| Document | Purpose |
|----------|---------|
| [CLAUDE.md](./CLAUDE.md) | Technical project specifications |
| [STUDENT_PROGRESS_REPORTS.md](./Software-project-Backend/STUDENT_PROGRESS_REPORTS.md) | Progress reporting system details |
| [BATCH_REPORTS.md](./Software-project-Backend/BATCH_REPORTS.md) | Batch reporting system details |
| [DEPLOYMENT.md](./DEPLOYMENT.md) | Production deployment guide |
| [CI_CD_SETUP.md](./CI_CD_SETUP.md) | GitHub Actions CI/CD configuration |
| [CONTRIBUTING.md](./CONTRIBUTING.md) | Development guidelines |

---

## 🤝 Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for development guidelines, code standards, and pull request process.

## 📝 License

This project is licensed under the MIT License - see LICENSE file for details.

## 📞 Support

For issues, bug reports, or feature requests, please open a GitHub issue or contact the development team.

---

**Last Updated**: September 28, 2026  
**Version**: 2.0.0  
**Maintained By**: University of Ruhuna, Faculty of Engineering  
**Repository**: [github.com/Danusigan/softwareproject](https://github.com/Danusigan/softwareproject)
