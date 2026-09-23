-- Removes mark_type from student_po_credit: PO attainment is one calculation per module/batch
-- that pools evidence from every mark type (a student's Final Exam and Assignment marks for the
-- same LO both count toward whether they passed it - see
-- POAttainmentService.calculateStudentPOCredits). Splitting the saved credit by mark_type never
-- matched that semantics; every reader (PoReportService, the cross-module student summary) was
-- already merging the two rows back together before use.
--
-- student_po_credit is a derived cache, not an audit trail (see V3__student_po_credit.sql) - it
-- exists only to survive past the request that computed it, and every row is overwritten
-- wholesale the next time its module/batch is recalculated (on a marks upload/edit/delete, or
-- the manual "Calculate PO Attainment" button). Clearing it here is safe: it repopulates
-- automatically the next time marks are touched, or immediately via a manual recalculation.
DELETE FROM `student_po_credit`;

-- Split into separate statements (rather than one multi-clause ALTER TABLE) for H2's
-- MySQL-compatibility mode, used in tests - see V1__baseline_legacy_schema.sql's header comment
-- on why H2 is in play here at all.
ALTER TABLE `student_po_credit` DROP INDEX `uk_student_po_credit`;
ALTER TABLE `student_po_credit` DROP COLUMN `mark_type`;
ALTER TABLE `student_po_credit` ADD UNIQUE KEY `uk_student_po_credit` (`student_id`,`po_id`,`module_id`,`batch`);
