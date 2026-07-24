CREATE DATABASE IF NOT EXISTS xiuxian_game
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'xiuxian'@'localhost' IDENTIFIED BY 'xiuxian_dev';
CREATE USER IF NOT EXISTS 'xiuxian'@'127.0.0.1' IDENTIFIED BY 'xiuxian_dev';

GRANT ALL PRIVILEGES ON xiuxian_game.* TO 'xiuxian'@'localhost';
GRANT ALL PRIVILEGES ON xiuxian_game.* TO 'xiuxian'@'127.0.0.1';
FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS skill_config (
  card_id VARCHAR(60) PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  rarity VARCHAR(20) NOT NULL,
  description VARCHAR(240) NOT NULL,
  effect_text VARCHAR(240) NOT NULL,
  archetype VARCHAR(20) NOT NULL,
  health_on_claim INT NOT NULL DEFAULT 0,
  spirit_on_claim INT NOT NULL DEFAULT 0,
  lifespan_on_claim INT NOT NULL DEFAULT 0,
  karma_on_claim INT NOT NULL DEFAULT 0,
  battle_health_bonus INT NOT NULL DEFAULT 0,
  battle_spirit_bonus INT NOT NULL DEFAULT 0,
  combat_damage_bonus INT NOT NULL DEFAULT 0,
  combat_block_bonus INT NOT NULL DEFAULT 0,
  combat_spirit_gain INT NOT NULL DEFAULT 0,
  combat_poison_bonus INT NOT NULL DEFAULT 0,
  battle_weight INT NOT NULL DEFAULT 0,
  elite_weight INT NOT NULL DEFAULT 0,
  treasure_weight INT NOT NULL DEFAULT 0,
  config_version INT NOT NULL DEFAULT 1,
  enabled BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS item_config LIKE skill_config;
CREATE TABLE IF NOT EXISTS talisman_config LIKE skill_config;

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

CREATE TABLE IF NOT EXISTS run_settlement (
  id VARCHAR(36) PRIMARY KEY,
  run_id VARCHAR(36) NOT NULL UNIQUE,
  player_name VARCHAR(32) NOT NULL,
  origin VARCHAR(32) NOT NULL,
  status VARCHAR(20) NOT NULL,
  ending_id VARCHAR(40) NULL,
  ending_title VARCHAR(120) NOT NULL,
  floor_reached INT NOT NULL,
  turn INT NOT NULL,
  health INT NOT NULL,
  spirit INT NOT NULL,
  lifespan INT NOT NULL,
  karma INT NOT NULL,
  spirit_stones INT NOT NULL,
  active_cards INT NOT NULL,
  elite_count INT NOT NULL,
  score INT NOT NULL,
  run_seed BIGINT NOT NULL,
  settled_at DATETIME(6) NOT NULL,
  KEY idx_run_settlement_score (score, settled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS run_shop (
  id VARCHAR(36) PRIMARY KEY,
  run_id VARCHAR(36) NOT NULL,
  node_id VARCHAR(36) NOT NULL,
  refresh_count INT NOT NULL DEFAULT 0,
  refresh_limit INT NOT NULL DEFAULT 2,
  removal_used BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY idx_run_shop_run_node (run_id, node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS run_shop_offer (
  id VARCHAR(36) PRIMARY KEY,
  shop_id VARCHAR(36) NOT NULL,
  run_id VARCHAR(36) NOT NULL,
  card_id VARCHAR(60) NOT NULL,
  category VARCHAR(20) NOT NULL,
  archetype VARCHAR(20) NOT NULL,
  name VARCHAR(80) NOT NULL,
  rarity VARCHAR(20) NOT NULL,
  description VARCHAR(240) NOT NULL,
  effect_text VARCHAR(240) NOT NULL,
  price INT NOT NULL,
  slot_number INT NOT NULL,
  generation INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL,
  KEY idx_shop_offer_shop_status (shop_id, status, slot_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
