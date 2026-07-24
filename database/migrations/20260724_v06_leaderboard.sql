-- V0.6 排行榜与结算快照幂等迁移：只新增结算表，不删除历史数据。
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
