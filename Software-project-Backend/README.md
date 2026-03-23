# 🎓 OBE Management System - Backend

> **A robust, secure, and intelligent backend for Outcome-Based Education (OBE) management.**  
> Built with **Spring Boot 3**, **Java 17**, and **MySQL**.

---

## 🚀 Overview

This backend system powers an advanced **Outcome-Based Education (OBE)** platform. It manages the entire lifecycle of academic assessment, from defining **Learning Outcomes (LOs)** and **Program Outcomes (POs)** to calculating attainment levels and analyzing year-over-year performance trends.

It features **Role-Based Access Control (RBAC)**, ensuring secure access for Superadmins, Admins, and Lecturers.

---

## ✨ Key Features

### 🔐 Security & User Management
*   **JWT Authentication:** Secure stateless authentication with Bearer tokens.
*   **RBAC:** Strict role enforcement (Superadmin, Admin, Lecture).
*   **Secure Password Handling:** (Ready for BCrypt integration).

### 📚 Academic Structure
*   **Dynamic Modules:** Create and manage academic modules.
*   **Learning Outcomes (LOs):** Define granular outcomes linked to modules.
*   **Assignments:** Create assignments linked to specific LOs and Academic Years (Batches).

### 🧠 OBE Intelligence
*   **Outcome Mapping:** Flexible mapping of LOs to POs with weighted correlations (0-3).
*   **Attainment Calculation:** Automatically calculates LO attainment based on student marks (Threshold: 50%).
*   **PO Attainment:** Weighted average calculation based on approved mappings.

### 📊 Analytics & Trends
*   **Trend Analysis:** Compare performance across different Academic Years (e.g., "23rd Batch" vs "24th Batch").
*   **Deep Dive:** Analyze trends at both the **Module** level and individual **LO** level.
*   **Excel Import:** Bulk upload student marks via Excel/CSV with automatic data cleaning.

---

## 🛠️ Tech Stack

*   **Framework:** Spring Boot 3.2.2
*   **Language:** Java 17
*   **Database:** MySQL 8.0
*   **ORM:** Hibernate / Spring Data JPA
*   **Security:** Spring Security + JWT (jjwt)
*   **Tools:** Apache POI (Excel Processing), Lombok, Maven

---

## 🔌 API Documentation

### 1. Authentication
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Login and get JWT token | Public |
| `POST` | `/api/auth/add-user` | Register new users | Superadmin/Admin |

### 2. Modules & LOs
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/modules/create` | Create a new module | Admin |
| `GET` | `/api/modules/all` | Get all modules | Any User |
| `POST` | `/api/lospos/{moduleId}/add` | Add LO to a module | Lecture |
| `GET` | `/api/lospos/module/{moduleId}` | Get LOs for a module | Any User |

### 3. Assignments & Marks
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/assignments/{losId}/add` | Create Assignment (Form-Data) | Lecture |
| `POST` | `/api/obe/marks/upload/{assignmentId}` | Upload Marks (Excel) | Lecture |

### 4. OBE & Analytics
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/obe/po/create` | Define Program Outcome | Admin |
| `POST` | `/api/obe/mappings/bulk-save` | Map LOs to POs | Lecture |
| `PUT` | `/api/obe/admin/approve-mapping/{id}` | Approve Mapping | Admin |
| `GET` | `/api/obe/reports/course/{moduleId}` | Get PO Attainment Report | Any User |
| `GET` | `/api/obe/analysis/trend/{moduleId}` | Get Module Trend | Any User |
| `GET` | `/api/obe/analysis/trend/lo/{moduleId}` | Get Detailed LO Trend | Any User |

---

## ⚙️ Setup & Installation

### Prerequisites
*   Java 17+
*   MySQL Server
*   Maven

### Step 1: Database Configuration
This project uses **Spring Profiles** to avoid Git conflicts.
1.  **Do NOT** edit `src/main/resources/application.properties`.
2.  Create a **new file** named `src/main/resources/application-dev.properties`.
3.  Add your local credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/YOUR_DB_NAME
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

### Step 2: Run the Application
```bash
mvn spring-boot:run
```
The server will start on `http://localhost:8080`.

---

## 📈 OBE Logic Explained

### Attainment Calculation
*   **Level 3:** >80% of students scored ≥ 50 marks.
*   **Level 2:** >70% of students scored ≥ 50 marks.
*   **Level 1:** >60% of students scored ≥ 50 marks.
*   **Level 0:** Otherwise.

### PO Calculation Formula
```
PO_Score = Σ (LO_Level * Mapping_Weight) / Σ (Mapping_Weights)
```
*Only **APPROVED** mappings are used in the calculation.*

---

## 🤝 Contributing
1.  Clone the repo.
2.  Create your `application-dev.properties`.
3.  **Never** push your private properties file (it is ignored by `.gitignore`).

---
*Generated for Software Project Backend*
