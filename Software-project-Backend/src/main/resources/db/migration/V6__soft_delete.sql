-- Soft-delete support for the 6 entities that were hard-delete-only (ProgramOutcome already
-- had its own isActive-based soft delete, added separately - see ProgramOutcomeService).
-- deleted_by stores the acting username as plain text, matching this schema's existing
-- convention for actor columns (created_by, mapped_by, reviewed_by, approved_by are all
-- varchar, not FK-constrained) rather than a foreign key to `user`.
--
-- One ADD COLUMN per ALTER TABLE statement - H2 in MySQL-compatibility mode (used for
-- local/CI testing, see V1__baseline_legacy_schema.sql) doesn't accept MySQL's comma-separated
-- multi-column ALTER TABLE ... ADD COLUMN a, ADD COLUMN b syntax.

ALTER TABLE `modules` ADD COLUMN `is_deleted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `modules` ADD COLUMN `deleted_at` datetime(6) DEFAULT NULL;
ALTER TABLE `modules` ADD COLUMN `deleted_by` varchar(255) DEFAULT NULL;

ALTER TABLE `los` ADD COLUMN `is_deleted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `los` ADD COLUMN `deleted_at` datetime(6) DEFAULT NULL;
ALTER TABLE `los` ADD COLUMN `deleted_by` varchar(255) DEFAULT NULL;

ALTER TABLE `lo_po_mappings` ADD COLUMN `is_deleted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `lo_po_mappings` ADD COLUMN `deleted_at` datetime(6) DEFAULT NULL;
ALTER TABLE `lo_po_mappings` ADD COLUMN `deleted_by` varchar(255) DEFAULT NULL;

ALTER TABLE `assessment_template` ADD COLUMN `is_deleted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `assessment_template` ADD COLUMN `deleted_at` datetime(6) DEFAULT NULL;
ALTER TABLE `assessment_template` ADD COLUMN `deleted_by` varchar(255) DEFAULT NULL;

ALTER TABLE `studentmark` ADD COLUMN `is_deleted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `studentmark` ADD COLUMN `deleted_at` datetime(6) DEFAULT NULL;
ALTER TABLE `studentmark` ADD COLUMN `deleted_by` varchar(255) DEFAULT NULL;

ALTER TABLE `cqi_action` ADD COLUMN `is_deleted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `cqi_action` ADD COLUMN `deleted_at` datetime(6) DEFAULT NULL;
ALTER TABLE `cqi_action` ADD COLUMN `deleted_by` varchar(255) DEFAULT NULL;

CREATE INDEX `idx_modules_is_deleted` ON `modules` (`is_deleted`);
CREATE INDEX `idx_los_is_deleted` ON `los` (`is_deleted`);
CREATE INDEX `idx_lo_po_mappings_is_deleted` ON `lo_po_mappings` (`is_deleted`);
CREATE INDEX `idx_assessment_template_is_deleted` ON `assessment_template` (`is_deleted`);
CREATE INDEX `idx_studentmark_is_deleted` ON `studentmark` (`is_deleted`);
CREATE INDEX `idx_cqi_action_is_deleted` ON `cqi_action` (`is_deleted`);
