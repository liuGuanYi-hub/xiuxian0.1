-- V0.3 账号、角色和存档归属幂等迁移，不删除历史存档。
USE xiuxian_game;

CREATE TABLE IF NOT EXISTS user_account (
  id VARCHAR(36) PRIMARY KEY,
  username VARCHAR(40) NOT NULL UNIQUE,
  password_hash VARCHAR(300) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  KEY idx_user_account_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS player_character (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  name VARCHAR(32) NOT NULL,
  origin VARCHAR(32) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  KEY idx_player_character_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- MySQL 8.0 不支持 ALTER TABLE ADD COLUMN IF NOT EXISTS，使用元数据检查保持幂等。
SET @v03_user_id_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'game_run' AND column_name = 'user_id'
);
SET @v03_user_id_sql = IF(
  @v03_user_id_exists = 0,
  'ALTER TABLE game_run ADD COLUMN user_id VARCHAR(36) NULL',
  'SELECT 1'
);
PREPARE v03_user_id_stmt FROM @v03_user_id_sql;
EXECUTE v03_user_id_stmt;
DEALLOCATE PREPARE v03_user_id_stmt;

SET @v03_character_id_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'game_run' AND column_name = 'character_id'
);
SET @v03_character_id_sql = IF(
  @v03_character_id_exists = 0,
  'ALTER TABLE game_run ADD COLUMN character_id VARCHAR(36) NULL',
  'SELECT 1'
);
PREPARE v03_character_id_stmt FROM @v03_character_id_sql;
EXECUTE v03_character_id_stmt;
DEALLOCATE PREPARE v03_character_id_stmt;
