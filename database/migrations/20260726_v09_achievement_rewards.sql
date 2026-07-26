-- V0.9 成就奖励和结算积分明细。只新增字段，不删除历史记录。
USE xiuxian_game;

SET @v09_reward_causality_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
   AND table_name = 'achievement_record' AND column_name = 'reward_causality') = 0,
  'ALTER TABLE achievement_record ADD COLUMN reward_causality INT NOT NULL DEFAULT 0 AFTER trigger_run_id', 'SELECT 1'
);
PREPARE v09_reward_causality_stmt FROM @v09_reward_causality_sql;
EXECUTE v09_reward_causality_stmt;
DEALLOCATE PREPARE v09_reward_causality_stmt;
