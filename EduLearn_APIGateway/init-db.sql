-- ═══════════════════════════════════════════════════════════════
--  EduLearn LMS — MySQL Database Initialisation
--  Creates all service databases in one shot.
--  The root password is set via MYSQL_ROOT_PASSWORD in docker-compose.
-- ═══════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS edulearn_auth       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS edulearn_course     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS edulearn_lesson     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS edulearn_enrollment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS edulearn_payment    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS edulearn_progress   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS edulearn_assessment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS edulearn_discussion CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS edulearn_notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant all privileges on every edulearn_* database to root (already implicit for root)
-- Add a dedicated app user for production (optional but recommended)
-- CREATE USER IF NOT EXISTS 'edulearn'@'%' IDENTIFIED BY 'edulearn_pass';
-- GRANT ALL PRIVILEGES ON `edulearn_%`.* TO 'edulearn'@'%';
-- FLUSH PRIVILEGES;
