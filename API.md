# 逆命仙途 API 接口说明

## 通用约定

- Base URL：`http://localhost:8080/api`
- 所有写接口返回完整 `GameRunView`，前端不自行结算属性、价格或随机结果。
- 错误格式：`{"message":"具体原因"}`。
- `GameRunView` 中的 `build`、`buildStats` 只统计 `ACTIVE` 卡牌；被移除的记录保留在数据库历史中。

## 旅程与节点

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/game/runs` | 创建新局并生成 seed 路线图 |
| GET | `/game/runs/{id}` | 恢复完整存档，包括路线、构筑、坊市和待处理操作 |
| POST | `/game/runs/{id}/nodes/{nodeId}/enter` | 进入相邻的 `AVAILABLE` 节点 |
| POST | `/game/runs/{id}/choices` | 提交 `{"choiceIndex":0,"requestId":"uuid"}` |

进入 `BATTLE`、`ELITE` 或 `BOSS` 节点后，服务端会创建可恢复的 `run_combat` 战斗快照；此时不能提交事件选择，必须先完成战斗。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/game/runs/{id}/combat/actions` | 提交 `{"action":"STRIKE"}`，行动包括 `STRIKE`、`GUARD`、`MEDITATE`、`TECHNIQUE`、`PURIFY` |

战斗行动由服务端扣除灵力、结算伤害、护盾、中毒和敌人意图。每次响应都返回最新的 `combat`；战斗胜利后 `combat` 置空并进入奖励或渡劫结局，刷新页面可从 `GET /game/runs/{id}` 恢复当前回合。

## 构筑奖励与强化

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/game/runs/{id}/rewards/{rewardId}/claim` | 领取一张奖励卡；其余奖励失效 |
| POST | `/game/runs/{id}/upgrades/{cardId}` | 在闭关节点升级有效卡牌 |
| POST | `/game/runs/{id}/upgrades/skip` | 跳过本次升级 |

`BuildCardView` 和 `RewardOfferView` 包含：`cardId`、`category`、`archetype`、名称、稀有度、描述、效果文本；构筑卡额外包含 `upgradeLevel`。

## 卡牌移除

| 方法 | 路径 | 费用 | 说明 |
| --- | --- | --- | --- |
| POST | `/game/runs/{id}/removals/{cardId}` | 免费 | `auction` 天关黑市事件的一次特殊移除 |
| POST | `/game/runs/{id}/removals/skip` | 0 | 保留当前构筑并离开黑市 |
| POST | `/game/runs/{id}/shops/remove/{cardId}` | 30 灵石 | 当前坊市最多一次 |

`cardId` 参数实际传入 `BuildCardView.id`（本局卡牌快照 ID），不是配置表中的配置 `cardId`。最后一张有效卡牌不能移除。

## 坊市

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/game/runs/{id}/shops/{offerId}/buy` | 购买一张 `ACTIVE` 商品，可连续购买直到灵石不足或售罄 |
| POST | `/game/runs/{id}/shops/refresh` | 第一次 10 灵石、第二次 15 灵石，最多刷新 2 次；旧商品变为 `EXPIRED` |
| POST | `/game/runs/{id}/shops/leave` | 坊市状态变为 `CLOSED`，节点变为 `CLEARED` 并解锁下一跳 |

`ShopView` 包含 `refreshCount`、`refreshLimit`、服务端计算的 `nextRefreshCost`、`removalCost`、`removalUsed` 和当前 `ACTIVE` 商品列表。商品价格由稀有度决定：普通 20、稀有 35、传说 55 灵石。

## 构筑统计

`BuildStatsView` 返回：

- `activeCards`：有效卡牌数量
- `categoryCounts`：功法、法宝、符箓数量
- `archetypeCounts`：剑修、丹修、体修、鬼修数量
- `synergies`：每个流派的数量、是否激活和效果说明
- `battleHealthBonus`、`battleSpiritBonus`、`battleLifespanBonus`、`battleKarmaBonus`：服务端汇总的当前战斗加成
- `combatDamageBonus`、`combatBlockBonus`、`combatSpiritGain`、`combatPoisonBonus`：回合战斗中的伤害、护盾、调息和战技中毒加成

## 配置与迁移

- 初始 25 张卡牌位于 `backend/src/main/resources/card-config.json`，其中新增 8 张战斗专属卡牌。
- 应用启动时只补充配置表缺失记录，不覆盖已有配置。
- 新环境可执行 `database/init.sql`；已有环境按顺序执行 `database/migrations/20260723_v04_build_extension.sql` 和 `database/migrations/20260723_v05_combat_depth.sql`。
- `run_build_item` 保存领取/购买时的卡牌快照，后续修改配置不会改变历史存档。
