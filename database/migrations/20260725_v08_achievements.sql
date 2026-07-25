-- V0.8 成就记录表。只新增数据，不删除历史记录。
USE xiuxian_game;

CREATE TABLE IF NOT EXISTS achievement_record (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  achievement_id VARCHAR(60) NOT NULL,
  trigger_run_id VARCHAR(36) NULL,
  awarded_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_achievement_record_user_achievement (user_id, achievement_id),
  KEY idx_achievement_record_user (user_id, awarded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
