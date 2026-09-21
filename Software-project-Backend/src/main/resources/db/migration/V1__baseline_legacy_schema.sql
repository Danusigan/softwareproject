-- Baseline schema for the 13 tables that were previously auto-managed by
-- Hibernate's ddl-auto=update. Captured from a live database that Hibernate
-- created from the current entity mappings (via mysqldump --no-data), so this
-- matches exactly what ddl-auto=validate expects. Tables are ordered so every
-- foreign key references a table already created earlier in this script -
-- deliberately not relying on MySQL's FOREIGN_KEY_CHECKS pragma, since H2 (used
-- for local/CI testing in MySQL-compatibility mode) doesn't honor it and still
-- enforces creation order.

CREATE TABLE `modules` (
  `module_id` varchar(255) NOT NULL,
  `module_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user` (
  `User_ID` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `failed_login_attempts` int NOT NULL DEFAULT '0',
  `locked_until` datetime(6) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `user_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`User_ID`),
  UNIQUE KEY `UK_e6gkqunxajvyxl5uctpl2vl2p` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `program_outcomes` (
  `po_id` varchar(255) NOT NULL,
  `category` varchar(255) DEFAULT NULL,
  `po_code` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `po_description` text,
  `display_order` int DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `is_default` tinyint(1) DEFAULT '0',
  `performance_indicators` text,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`po_id`),
  UNIQUE KEY `UK_f1c4lshx0g53vlqp9i62km4f5` (`po_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `students` (
  `student_id` varchar(255) NOT NULL,
  `academic_year` varchar(255) DEFAULT NULL,
  `batch` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `student_name` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(255) DEFAULT NULL,
  `actor` varchar(255) DEFAULT NULL,
  `details` varchar(255) DEFAULT NULL,
  `outcome` varchar(255) DEFAULT NULL,
  `target` varchar(255) DEFAULT NULL,
  `timestamp` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `assessment_template` (
  `id` varchar(255) NOT NULL,
  `academic_year` varchar(255) DEFAULT NULL,
  `assignment_label` varchar(255) DEFAULT NULL,
  `batch` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `mark_type` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `semester` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `module_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKm3q5r2pxqimva2jn37cfhvcev` (`module_id`),
  CONSTRAINT `FKm3q5r2pxqimva2jn37cfhvcev` FOREIGN KEY (`module_id`) REFERENCES `modules` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `los` (
  `id` varchar(255) NOT NULL,
  `attainment_threshold` double DEFAULT NULL,
  `batch` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `description` text,
  `file_name` varchar(255) DEFAULT NULL,
  `marks_csv_file` longblob,
  `name` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `module_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmpikka0k63wlf75xke1ml4o67` (`module_id`),
  CONSTRAINT `FKmpikka0k63wlf75xke1ml4o67` FOREIGN KEY (`module_id`) REFERENCES `modules` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `module_lecturers` (
  `module_id` varchar(255) NOT NULL,
  `lecturer_username` varchar(255) NOT NULL,
  KEY `FKncth8c6hxu6lbu5r340o4nylm` (`lecturer_username`),
  KEY `FKoxrrx81c5mpp55f28huovebr8` (`module_id`),
  CONSTRAINT `FKncth8c6hxu6lbu5r340o4nylm` FOREIGN KEY (`lecturer_username`) REFERENCES `user` (`User_ID`),
  CONSTRAINT `FKoxrrx81c5mpp55f28huovebr8` FOREIGN KEY (`module_id`) REFERENCES `modules` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `assessment_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `max_marks` double NOT NULL,
  `question_label` varchar(255) NOT NULL,
  `question_number` int NOT NULL,
  `template_id` varchar(255) NOT NULL,
  `los_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9pmqhmsgvkd3erpd2tuf5ului` (`template_id`),
  KEY `FKmpp4k3pyfgnqahx745nbqfqxe` (`los_id`),
  CONSTRAINT `FK9pmqhmsgvkd3erpd2tuf5ului` FOREIGN KEY (`template_id`) REFERENCES `assessment_template` (`id`),
  CONSTRAINT `FKmpp4k3pyfgnqahx745nbqfqxe` FOREIGN KEY (`los_id`) REFERENCES `los` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cqi_action` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `academic_year` varchar(255) DEFAULT NULL,
  `action_description` text,
  `action_type` enum('ADD_LAB_SESSION','REVISE_ASSESSMENT','CHANGE_TEACHING_METHOD','ADD_RESOURCE','REDESIGN_LO') DEFAULT NULL,
  `admin_comment` text,
  `approved_by` varchar(255) DEFAULT NULL,
  `attainment_score` double DEFAULT NULL,
  `batch` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `deadline` date DEFAULT NULL,
  `module_id` varchar(255) DEFAULT NULL,
  `next_sem_attainment` double DEFAULT NULL,
  `responsible_staff` varchar(255) DEFAULT NULL,
  `reason` text,
  `semester` varchar(255) DEFAULT NULL,
  `status` enum('PLANNED','IN_PROGRESS','COMPLETED') NOT NULL,
  `submitted` bit(1) NOT NULL,
  `target_attainment` double DEFAULT NULL,
  `target_score` double DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `los_id` varchar(255) DEFAULT NULL,
  `po_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKij01nwdkuuc0197c0p81lvprt` (`los_id`),
  KEY `FK70ai3c09kv3kqldcuje45dtwm` (`module_id`),
  KEY `FKlajsbq5uq36cnt5gyj5h2jgr7` (`po_id`),
  CONSTRAINT `FK70ai3c09kv3kqldcuje45dtwm` FOREIGN KEY (`module_id`) REFERENCES `modules` (`module_id`),
  CONSTRAINT `FKij01nwdkuuc0197c0p81lvprt` FOREIGN KEY (`los_id`) REFERENCES `los` (`id`),
  CONSTRAINT `FKlajsbq5uq36cnt5gyj5h2jgr7` FOREIGN KEY (`po_id`) REFERENCES `program_outcomes` (`po_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `lo_po_mappings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_remarks` text,
  `lecturer_remarks` text,
  `lospos_id` varchar(255) DEFAULT NULL,
  `mapped_at` datetime(6) DEFAULT NULL,
  `mapped_by` varchar(255) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `reviewed_by` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `weight` int NOT NULL,
  `los_id` varchar(255) NOT NULL,
  `program_outcome_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8gq87y2hpka7rmagly7nuj4qx` (`los_id`),
  KEY `FKro1n2gugbvgpfsfxljaw9xlj3` (`program_outcome_id`),
  CONSTRAINT `FK8gq87y2hpka7rmagly7nuj4qx` FOREIGN KEY (`los_id`) REFERENCES `los` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `student_assessment_score` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `score` double DEFAULT NULL,
  `assessment_item_id` bigint DEFAULT NULL,
  `student_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK7fyykw1yrdag6akckdj27gc9t` (`student_id`,`assessment_item_id`),
  KEY `FKet0wgju6f82asx46muvnsi65u` (`assessment_item_id`),
  CONSTRAINT `FKet0wgju6f82asx46muvnsi65u` FOREIGN KEY (`assessment_item_id`) REFERENCES `assessment_item` (`id`),
  CONSTRAINT `FKfxv4o4f95ndhfm9gqh5hktjag` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `studentmark` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assignment_label` varchar(255) DEFAULT NULL,
  `batch` varchar(255) DEFAULT NULL,
  `mark_type` enum('FINAL_EXAM','ASSIGNMENT') DEFAULT NULL,
  `score` double DEFAULT NULL,
  `los_id` varchar(255) NOT NULL,
  `student_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhjqfju3cums3n1y49uc34lcas` (`los_id`),
  KEY `FK3e4yk3lxqta7uej9n7yhjndxa` (`student_id`),
  CONSTRAINT `FK3e4yk3lxqta7uej9n7yhjndxa` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`),
  CONSTRAINT `FKhjqfju3cums3n1y49uc34lcas` FOREIGN KEY (`los_id`) REFERENCES `los` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
