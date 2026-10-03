CREATE TABLE IF NOT EXISTS `user_access` (
  `username` varchar(255) NOT NULL,
  `firstAccess` datetime(6) DEFAULT NULL,
  `lastAccess` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `assignments` (
  `assignment_id` varchar(255) NOT NULL,
  `assignment_name` varchar(255) DEFAULT NULL,
  `academic_year` varchar(255) NOT NULL,
  `batch` varchar(255) DEFAULT NULL,
  `marks_csv_file` longblob,
  `fileName` varchar(255) DEFAULT NULL,
  `los_pos_id` varchar(255) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`assignment_id`),
  KEY `FKassignments_los` (`los_pos_id`),
  CONSTRAINT `FKassignments_los` FOREIGN KEY (`los_pos_id`) REFERENCES `los` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

