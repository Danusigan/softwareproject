-- Persisted per-module PO credit outcomes.
--
-- POAttainmentService.calculateStudentPOCredits already computed these numbers on the fly;
-- this table gives the calculation a permanent home instead of losing the result the moment
-- the browser tab is closed. One row = "this student earned N of M possible credits toward
-- this PO, from this module's LOs, under this batch/mark-type combination".
--
-- Recalculating a module/batch/mark-type overwrites its rows (delete-then-insert) rather than
-- accumulating history — this table holds the current state, not an audit trail.
--
-- Rows from every module feed the cross-module summary in
-- POAttainmentService.getStudentPOSummary: for a given student and PO, summing credits_earned
-- and max_credits across all of that student's rows (regardless of module) gives their
-- cumulative standing toward that PO across the whole programme so far.
-- student_id/po_id/module_id/batch are narrower than the varchar(255) they reference
-- (students.student_id, program_outcomes.po_id, modules.module_id) - deliberately so.
-- The composite UNIQUE KEY below spans all four plus mark_type; at varchar(255) each with
-- utf8mb4 (4 bytes/char) that index alone is ~4KB, over InnoDB's 3072-byte key limit
-- (MySQL error 1071). Real values are short (module ids like "EC4356", batch like "24"),
-- so narrower columns here are both what the index limit requires and what the data is.
CREATE TABLE `student_po_credit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` varchar(100) NOT NULL,
  `po_id` varchar(50) NOT NULL,
  `module_id` varchar(50) NOT NULL,
  `batch` varchar(20) NOT NULL,
  `mark_type` enum('FINAL_EXAM','ASSIGNMENT') NOT NULL,
  `credits_earned` int NOT NULL,
  `max_credits` int NOT NULL,
  `threshold` int NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_po_credit` (`student_id`,`po_id`,`module_id`,`batch`,`mark_type`),
  KEY `FKspc_po` (`po_id`),
  KEY `FKspc_module` (`module_id`),
  CONSTRAINT `FKspc_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`),
  CONSTRAINT `FKspc_po` FOREIGN KEY (`po_id`) REFERENCES `program_outcomes` (`po_id`),
  CONSTRAINT `FKspc_module` FOREIGN KEY (`module_id`) REFERENCES `modules` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
