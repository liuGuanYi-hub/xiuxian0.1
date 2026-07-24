-- V0.6 配置中心幂等迁移：新增事件配置、配置版本和操作日志，不删除历史数据。
ALTER TABLE skill_config
  ADD COLUMN IF NOT EXISTS config_version INT NOT NULL DEFAULT 1;

ALTER TABLE item_config
  ADD COLUMN IF NOT EXISTS config_version INT NOT NULL DEFAULT 1;

ALTER TABLE talisman_config
  ADD COLUMN IF NOT EXISTS config_version INT NOT NULL DEFAULT 1;

-- 兼容 V0.6 迁移前已经存在的卡牌：只修正无效版本号，不覆盖卡牌内容。
UPDATE skill_config SET config_version = 1 WHERE config_version <= 0;
UPDATE item_config SET config_version = 1 WHERE config_version <= 0;
UPDATE talisman_config SET config_version = 1 WHERE config_version <= 0;

CREATE TABLE IF NOT EXISTS event_config (
  event_id VARCHAR(80) PRIMARY KEY,
  config_type VARCHAR(20) NOT NULL DEFAULT 'EVENT',
  title VARCHAR(120) NOT NULL,
  description VARCHAR(500) NOT NULL,
  rarity VARCHAR(20) NOT NULL DEFAULT '普通',
  repeatable BOOLEAN NOT NULL DEFAULT FALSE,
  config_version INT NOT NULL DEFAULT 1,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  choices_json TEXT NOT NULL,
  next_event_ids_json TEXT NOT NULL,
  node_weights_json TEXT NOT NULL,
  updated_by VARCHAR(80) NOT NULL DEFAULT 'bootstrap',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  KEY idx_event_config_type_enabled (config_type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS config_operation_log (
  id VARCHAR(36) PRIMARY KEY,
  config_type VARCHAR(20) NOT NULL,
  config_id VARCHAR(80) NULL,
  action VARCHAR(30) NOT NULL,
  config_version INT NOT NULL DEFAULT 0,
  operator_name VARCHAR(80) NOT NULL,
  result VARCHAR(20) NOT NULL,
  message VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL,
  KEY idx_config_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
