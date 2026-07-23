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
  battle_weight INT NOT NULL DEFAULT 0,
  elite_weight INT NOT NULL DEFAULT 0,
  treasure_weight INT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS item_config LIKE skill_config;
CREATE TABLE IF NOT EXISTS talisman_config LIKE skill_config;

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
