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
- 初始事件位于 `backend/src/main/resources/event-config.json`，包含 28 个事件、6 个结局和 78 个事件选项。
- 应用启动时只补充配置表缺失记录，不覆盖已有配置。
- `event_config` 保存事件/结局正文、事件选项、后继事件、节点权重、稀有度、版本号和启用状态；游戏路线图和事件展示运行时从数据库缓存读取。
- `skill_config`、`item_config`、`talisman_config` 均带 `config_version` 和 `enabled` 字段；配置初始化只补充缺失卡牌。
- 新环境可执行 `database/init.sql`；已有环境按顺序执行 `database/migrations/20260723_v04_build_extension.sql`、`database/migrations/20260723_v05_combat_depth.sql`、`database/migrations/20260724_v06_config_center.sql` 和 `database/migrations/20260724_v06_leaderboard.sql`。
- `run_build_item` 保存领取/购买时的卡牌快照，后续修改配置不会改变历史存档。

## 结算与排行榜

- 游戏进入 `DEAD` 或 `ASCENDED` 时，服务端在 `run_settlement` 生成一份唯一结算快照；重复恢复存档不会重复生成。
- 结算积分由服务端计算：层数、回合、气血、灵力、寿元、正因果、灵石、有效卡牌、精英挑战和飞升状态都会影响分数。
- `GameRunView.settlement` 在终局返回结算快照，进行中的旅程为 `null`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/leaderboard?limit=10` | 查询积分最高的已结算旅程，服务端将 limit 限制在 1~50 |

`SettlementView` 包含：结局、抵达层数、回合、终局属性、有效卡牌数、精英挑战数、积分和结算时间。排行榜只读取快照，不重新计算历史积分。

## 配置中心接口

当前配置管理接口暂未接入账号鉴权，适用于本地开发和内网配置工具；生产环境接入 JWT 后再开放给管理员。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/config/events?includeDisabled=false` | 查询事件和结局配置，默认只返回启用项 |
| POST | `/admin/config/events/import` | 导入配置并执行全量引用校验，失败时不写入配置 |
| POST | `/admin/config/validate?operator=admin` | 校验当前启用配置并记录 `VALIDATE` 日志 |
| POST | `/admin/config/reload?operator=admin` | 从数据库重建事件缓存并记录 `RELOAD` 日志 |
| GET | `/admin/config/logs` | 查询最近 50 条初始化、导入、校验和重载日志 |

导入请求示例：

```json
{
  "operator": "admin",
  "configs": [
    {
      "configType": "EVENT",
      "eventId": "new_event",
      "title": "新事件",
      "description": "事件描述",
      "rarity": "稀有",
      "repeatable": false,
      "version": 2,
      "enabled": true,
      "choices": [],
      "nextEventIds": [],
      "nodeWeights": {"EVENT": 10}
    }
  ]
}
```
