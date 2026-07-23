-- V0.5 战斗深度幂等迁移：只新增战斗数值和战斗存档，不删除历史数据。
ALTER TABLE skill_config
  ADD COLUMN IF NOT EXISTS combat_damage_bonus INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS combat_block_bonus INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS combat_spirit_gain INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS combat_poison_bonus INT NOT NULL DEFAULT 0;

ALTER TABLE item_config
  ADD COLUMN IF NOT EXISTS combat_damage_bonus INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS combat_block_bonus INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS combat_spirit_gain INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS combat_poison_bonus INT NOT NULL DEFAULT 0;

ALTER TABLE talisman_config
  ADD COLUMN IF NOT EXISTS combat_damage_bonus INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS combat_block_bonus INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS combat_spirit_gain INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS combat_poison_bonus INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS run_combat (
  id VARCHAR(36) PRIMARY KEY,
  run_id VARCHAR(36) NOT NULL,
  node_id VARCHAR(36) NOT NULL,
  enemy_id VARCHAR(40) NOT NULL,
  enemy_name VARCHAR(80) NOT NULL,
  enemy_type VARCHAR(20) NOT NULL,
  enemy_description VARCHAR(240) NOT NULL,
  max_health INT NOT NULL,
  health INT NOT NULL,
  enemy_block INT NOT NULL DEFAULT 0,
  enemy_power INT NOT NULL DEFAULT 0,
  enemy_poison INT NOT NULL DEFAULT 0,
  player_block INT NOT NULL DEFAULT 0,
  player_poison INT NOT NULL DEFAULT 0,
  intent VARCHAR(20) NOT NULL,
  intent_value INT NOT NULL,
  turn INT NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  combat_log TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  KEY idx_run_combat_run_status (run_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
