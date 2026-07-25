-- V0.7 账号永久进度、因果点和解锁记录幂等迁移，不删除历史数据。
USE xiuxian_game;

-- MySQL 8.0 不支持 ALTER TABLE ADD COLUMN IF NOT EXISTS，使用元数据检查保持幂等。
SET @v07_causality_points_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
   AND table_name = 'user_account' AND column_name = 'causality_points') = 0,
  'ALTER TABLE user_account ADD COLUMN causality_points INT NOT NULL DEFAULT 0', 'SELECT 1'
);
PREPARE v07_causality_points_stmt FROM @v07_causality_points_sql;
EXECUTE v07_causality_points_stmt;
DEALLOCATE PREPARE v07_causality_points_stmt;

SET @v07_total_earned_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
   AND table_name = 'user_account' AND column_name = 'total_causality_earned') = 0,
  'ALTER TABLE user_account ADD COLUMN total_causality_earned INT NOT NULL DEFAULT 0', 'SELECT 1'
);
PREPARE v07_total_earned_stmt FROM @v07_total_earned_sql;
EXECUTE v07_total_earned_stmt;
DEALLOCATE PREPARE v07_total_earned_stmt;

SET @v07_total_spent_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
   AND table_name = 'user_account' AND column_name = 'total_causality_spent') = 0,
  'ALTER TABLE user_account ADD COLUMN total_causality_spent INT NOT NULL DEFAULT 0', 'SELECT 1'
);
PREPARE v07_total_spent_stmt FROM @v07_total_spent_sql;
EXECUTE v07_total_spent_stmt;
DEALLOCATE PREPARE v07_total_spent_stmt;

CREATE TABLE IF NOT EXISTS unlock_record (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  unlock_id VARCHAR(60) NOT NULL,
  cost INT NOT NULL,
  unlocked_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_unlock_record_user_unlock (user_id, unlock_id),
  KEY idx_unlock_record_user (user_id, unlocked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @v07_settlement_user_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
   AND table_name = 'run_settlement' AND column_name = 'user_id') = 0,
  'ALTER TABLE run_settlement ADD COLUMN user_id VARCHAR(36) NULL', 'SELECT 1'
);
PREPARE v07_settlement_user_stmt FROM @v07_settlement_user_sql;
EXECUTE v07_settlement_user_stmt;
DEALLOCATE PREPARE v07_settlement_user_stmt;

SET @v07_settlement_character_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
   AND table_name = 'run_settlement' AND column_name = 'character_id') = 0,
  'ALTER TABLE run_settlement ADD COLUMN character_id VARCHAR(36) NULL', 'SELECT 1'
);
PREPARE v07_settlement_character_stmt FROM @v07_settlement_character_sql;
EXECUTE v07_settlement_character_stmt;
DEALLOCATE PREPARE v07_settlement_character_stmt;

SET @v07_causality_earned_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
   AND table_name = 'run_settlement' AND column_name = 'causality_earned') = 0,
  'ALTER TABLE run_settlement ADD COLUMN causality_earned INT NOT NULL DEFAULT 0', 'SELECT 1'
);
PREPARE v07_causality_earned_stmt FROM @v07_causality_earned_sql;
EXECUTE v07_causality_earned_stmt;
DEALLOCATE PREPARE v07_causality_earned_stmt;
