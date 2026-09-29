-- Forgot-password / reset-password flow (Service/PasswordResetService, Model/PasswordResetToken).
-- One row per outstanding reset request; PasswordResetTokenRepository.deleteByUserId clears any
-- previous request before issuing a new one, and resetPassword marks the row used rather than
-- deleting it, so it is not reusable after a successful reset.
CREATE TABLE `password_reset_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `token` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `expiry_date` datetime(6) NOT NULL,
  `used` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_password_reset_token_token` (`token`),
  KEY `FKprt_user` (`user_id`),
  CONSTRAINT `FKprt_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`User_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
