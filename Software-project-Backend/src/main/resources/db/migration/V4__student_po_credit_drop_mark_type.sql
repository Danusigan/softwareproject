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
--
-- student_id is only covered by uk_student_po_credit (as its leading column) - nothing else
-- indexes it - and that same column backs FK FKspc_student. Dropping uk_student_po_credit as
-- its own statement, with the FK still attached, either fails outright (real MySQL: error 1553,
-- "needed in a foreign key constraint") or silently rebinds the FK to whatever index happens to
-- exist at that moment instead of the new composite one (H2), which then blocks cleanup of that
-- stand-in index instead. Dropping the FK constraint itself first sidesteps both: there is
-- nothing for either engine to keep an index "needed" for while the index is swapped.
ALTER TABLE `student_po_credit` DROP FOREIGN KEY `FKspc_student`;
ALTER TABLE `student_po_credit` DROP INDEX `uk_student_po_credit`;
ALTER TABLE `student_po_credit` DROP COLUMN `mark_type`;
ALTER TABLE `student_po_credit` ADD UNIQUE KEY `uk_student_po_credit` (`student_id`,`po_id`,`module_id`,`batch`);
ALTER TABLE `student_po_credit` ADD CONSTRAINT `FKspc_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`);
