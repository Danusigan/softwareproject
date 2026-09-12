CREATE TABLE qa_programme (
 code VARCHAR(80) PRIMARY KEY, name VARCHAR(255) NOT NULL, university VARCHAR(255) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE qa_curriculum (
 code VARCHAR(80) PRIMARY KEY, programme_code VARCHAR(80) NOT NULL, version VARCHAR(80) NOT NULL,
 cohort VARCHAR(50) NOT NULL, policy_version VARCHAR(80) NOT NULL,
 retake_policy VARCHAR(30) NOT NULL, credit_weighted BOOLEAN NOT NULL,
 graduation_configured BOOLEAN NOT NULL, required_credits DECIMAL(12,4), minimum_gpa DECIMAL(8,4),
 created_by VARCHAR(255) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (programme_code) REFERENCES qa_programme(code),
 UNIQUE (programme_code,version,cohort),
 CHECK (retake_policy IN ('OFFICIAL','LATEST_COMPLETED','BEST')),
 CHECK (required_credits IS NULL OR required_credits > 0),
 CHECK (minimum_gpa IS NULL OR minimum_gpa >= 0),
 CHECK (graduation_configured = FALSE OR required_credits IS NOT NULL)
);
CREATE TABLE qa_curriculum_module (
 curriculum_code VARCHAR(80) NOT NULL, module_id VARCHAR(255) NOT NULL,
 module_name VARCHAR(255) NOT NULL, credits DECIMAL(12,4) NOT NULL, compulsory BOOLEAN NOT NULL,
 PRIMARY KEY(curriculum_code,module_id), FOREIGN KEY(curriculum_code) REFERENCES qa_curriculum(code),
 FOREIGN KEY(module_id) REFERENCES modules(module_id), CHECK (credits > 0)
);
CREATE TABLE qa_curriculum_lo (
 curriculum_code VARCHAR(80) NOT NULL, lo_id VARCHAR(255) NOT NULL, module_id VARCHAR(255) NOT NULL,
 name VARCHAR(255), description TEXT, threshold DECIMAL(7,4) NOT NULL,
 PRIMARY KEY(curriculum_code,lo_id), FOREIGN KEY(lo_id) REFERENCES los(id),
 FOREIGN KEY(curriculum_code,module_id) REFERENCES qa_curriculum_module(curriculum_code,module_id),
 CHECK(threshold BETWEEN 0 AND 100)
);
CREATE TABLE qa_curriculum_po (
 curriculum_code VARCHAR(80) NOT NULL, po_id VARCHAR(255) NOT NULL, code VARCHAR(255) NOT NULL,
 description TEXT, threshold DECIMAL(7,4) NOT NULL, minimum_evidence INTEGER NOT NULL, required BOOLEAN NOT NULL,
 PRIMARY KEY(curriculum_code,po_id), FOREIGN KEY(curriculum_code) REFERENCES qa_curriculum(code),
 FOREIGN KEY(po_id) REFERENCES program_outcomes(po_id), CHECK(threshold BETWEEN 0 AND 100), CHECK(minimum_evidence >= 1)
);
CREATE TABLE qa_curriculum_mapping (
 curriculum_code VARCHAR(80) NOT NULL, lo_id VARCHAR(255) NOT NULL, po_id VARCHAR(255) NOT NULL,
 weight DECIMAL(12,4) NOT NULL, PRIMARY KEY(curriculum_code,lo_id,po_id),
 FOREIGN KEY(curriculum_code,lo_id) REFERENCES qa_curriculum_lo(curriculum_code,lo_id),
 FOREIGN KEY(curriculum_code,po_id) REFERENCES qa_curriculum_po(curriculum_code,po_id), CHECK(weight > 0)
);
CREATE TABLE qa_student_programme (
 student_id VARCHAR(255) PRIMARY KEY, curriculum_code VARCHAR(80) NOT NULL,
 account_username VARCHAR(255) UNIQUE, academic_status VARCHAR(30) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(student_id) REFERENCES students(student_id), FOREIGN KEY(curriculum_code) REFERENCES qa_curriculum(code),
 CHECK(academic_status IN ('IN_PROGRESS','COMPLETED','WITHDRAWN'))
);
CREATE TABLE qa_academic_period (
 code VARCHAR(80) PRIMARY KEY, academic_year VARCHAR(50) NOT NULL, semester VARCHAR(50) NOT NULL,
 starts_on DATE NOT NULL, UNIQUE(academic_year,semester)
);
CREATE TABLE qa_module_offering (
 code VARCHAR(80) PRIMARY KEY, curriculum_code VARCHAR(80) NOT NULL, module_id VARCHAR(255) NOT NULL,
 period_code VARCHAR(80) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(curriculum_code,module_id) REFERENCES qa_curriculum_module(curriculum_code,module_id),
 FOREIGN KEY(period_code) REFERENCES qa_academic_period(code), UNIQUE(curriculum_code,module_id,period_code)
);
CREATE TABLE qa_offering_assessment (
 offering_code VARCHAR(80) NOT NULL, template_id VARCHAR(255) NOT NULL UNIQUE,
 PRIMARY KEY(offering_code,template_id), FOREIGN KEY(offering_code) REFERENCES qa_module_offering(code),
 FOREIGN KEY(template_id) REFERENCES assessment_template(id)
);
CREATE TABLE qa_module_enrolment (
 student_id VARCHAR(255) NOT NULL, offering_code VARCHAR(80) NOT NULL, attempt_number INTEGER NOT NULL,
 official BOOLEAN NOT NULL, status VARCHAR(30) NOT NULL, final_mark DECIMAL(7,4), grade VARCHAR(30), grade_points DECIMAL(8,4),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(student_id,offering_code), FOREIGN KEY(student_id) REFERENCES qa_student_programme(student_id),
 FOREIGN KEY(offering_code) REFERENCES qa_module_offering(code), CHECK(attempt_number > 0),
 CHECK(status IN ('IN_PROGRESS','PASS','FAIL','WITHDRAWN','EXEMPT','ABSENT')),
 CHECK(final_mark IS NULL OR final_mark BETWEEN 0 AND 100), CHECK(grade_points IS NULL OR grade_points >= 0)
);
CREATE TABLE qa_offering_item (
 offering_code VARCHAR(80) NOT NULL, item_id BIGINT NOT NULL, curriculum_code VARCHAR(80) NOT NULL,
 lo_id VARCHAR(255) NOT NULL, assessment_name VARCHAR(255) NOT NULL, question VARCHAR(255) NOT NULL,
 maximum DECIMAL(12,4) NOT NULL, PRIMARY KEY(offering_code,item_id),
 FOREIGN KEY(offering_code) REFERENCES qa_module_offering(code), FOREIGN KEY(item_id) REFERENCES assessment_item(id),
 FOREIGN KEY(curriculum_code,lo_id) REFERENCES qa_curriculum_lo(curriculum_code,lo_id), CHECK(maximum > 0)
);
CREATE INDEX qa_enrolment_offering ON qa_module_enrolment(offering_code);
CREATE INDEX qa_student_curriculum ON qa_student_programme(curriculum_code);
CREATE TABLE qa_report_snapshot (
 reference VARCHAR(36) PRIMARY KEY, student_id VARCHAR(255) NOT NULL, generated_by VARCHAR(255) NOT NULL,
 policy_version VARCHAR(80), generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 content LONGTEXT NOT NULL, FOREIGN KEY(student_id) REFERENCES students(student_id)
);
CREATE INDEX qa_snapshot_student ON qa_report_snapshot(student_id,generated_at);
CREATE TABLE qa_report_audit (
 reference VARCHAR(36) PRIMARY KEY, snapshot_reference VARCHAR(36) NOT NULL, actor VARCHAR(255) NOT NULL,
 action VARCHAR(30) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(snapshot_reference) REFERENCES qa_report_snapshot(reference)
);
