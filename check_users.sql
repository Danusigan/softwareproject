-- Check all users and their types
SELECT User_ID, email, user_type FROM User;

-- If there's a superadmin user, show it
SELECT User_ID, email, user_type FROM User WHERE user_type LIKE '%[Ss]uper%' OR user_type LIKE '%admin%';

-- Update all user_type values to lowercase for consistency
UPDATE User SET user_type = LOWER(user_type);

-- Verify the update
SELECT User_ID, email, user_type FROM User;
