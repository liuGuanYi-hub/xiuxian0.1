-- V0.4 构筑扩展幂等迁移：只新增字段、表和默认值，不删除任何存档数据。
ALTER TABLE run_build_item
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE game_run
  ADD COLUMN IF NOT EXISTS pending_shop_node_id VARCHAR(36) NULL,
  ADD COLUMN IF NOT EXISTS pending_removal_node_id VARCHAR(36) NULL;

CREATE TABLE IF NOT EXISTS skill_config LIKE xiuxian_game.skill_config;
CREATE TABLE IF NOT EXISTS item_config LIKE xiuxian_game.skill_config;
CREATE TABLE IF NOT EXISTS talisman_config LIKE xiuxian_game.skill_config;

CREATE TABLE IF NOT EXISTS run_shop LIKE xiuxian_game.run_shop;
CREATE TABLE IF NOT EXISTS run_shop_offer LIKE xiuxian_game.run_shop_offer;
