-- Soft delete for students who already have marks/credits/reports (hard delete would orphan
-- or be blocked by FKs). Same columns as V6__soft_delete.sql; one ADD COLUMN per statement
-- for H2 MySQL-mode compatibility.
ALTER TABLE `students` ADD COLUMN `is_deleted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `students` ADD COLUMN `deleted_at` datetime(6) DEFAULT NULL;
ALTER TABLE `students` ADD COLUMN `deleted_by` varchar(255) DEFAULT NULL;
CREATE INDEX `idx_students_is_deleted` ON `students` (`is_deleted`);
