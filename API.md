# 逆命仙途 API 接口说明

## 通用约定

- Base URL：`http://localhost:8080/api`
- 所有写接口返回完整 `GameRunView`，前端不自行结算属性、价格或随机结果。
- 错误格式：`{"message":"具体原因"}`。
- 除 `/auth/**`、`/admin/**` 和公开排行榜外，游戏接口需要 `Authorization: Bearer <JWT>`；未登录统一返回 HTTP 401。
- `GameRunView` 中的 `build`、`buildStats` 只统计 `ACTIVE` 卡牌；被移除的记录保留在数据库历史中。
- 已登录的 `GameRunView.accountProgress` 返回当前账号永久进度；匿名兼容模式下该字段为 `null`。

## 账号、JWT 与角色

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/register` | 注册账号，返回 JWT 和初始角色 |
| POST | `/auth/login` | 校验密码并返回 JWT |
| POST | `/auth/logout` | 无状态登出；客户端删除 JWT |
| GET | `/players/me` | 查询当前账号及角色列表 |
| POST | `/players` | 创建当前账号下的角色 |
| GET | `/game/runs` | 按更新时间倒序查询当前账号的存档摘要 |

注册用户名使用 3-40 位字母、数字或下划线，密码长度为 8-120 位。密码服务端使用 PBKDF2WithHmacSHA256 哈希保存，不保存明文。开始新局时服务端只接受当前 JWT 所属账号下的 `characterId`，并以数据库角色的道号和出身为准。

存档读取、事件选择、战斗、奖励、坊市和结算接口都会按 `user_id` 过滤。旧版本没有账号归属的匿名存档会保留在数据库中，但在账号鉴权模式下不可被新账号读取或认领。

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
- 新环境可执行 `database/init.sql`；已有环境按顺序执行 `database/migrations/20260725_v03_account_auth.sql`、`database/migrations/20260723_v04_build_extension.sql`、`database/migrations/20260723_v05_combat_depth.sql`、`database/migrations/20260724_v06_config_center.sql`、`database/migrations/20260724_v06_leaderboard.sql` 和 `database/migrations/20260725_v07_permanent_progress.sql`。
- `run_build_item` 保存领取/购买时的卡牌快照，后续修改配置不会改变历史存档。

## 结算与排行榜

- 游戏进入 `DEAD` 或 `ASCENDED` 时，服务端在 `run_settlement` 生成一份唯一结算快照；重复恢复存档不会重复生成。
- 结算积分由服务端计算：层数、回合、气血、灵力、寿元、正因果、灵石、有效卡牌、精英挑战和飞升状态都会影响分数。
- `GameRunView.settlement` 在终局返回结算快照，进行中的旅程为 `null`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/leaderboard?limit=10` | 查询积分最高的已结算旅程，服务端将 limit 限制在 1~50 |

`SettlementView` 包含：结局、抵达层数、回合、终局属性、有效卡牌数、精英挑战数、积分、因果点发放数和结算时间。排行榜只读取快照，不重新计算历史积分。

## V0.7 永久因果与账号数据

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/account/progress` | 查询当前账号因果点、累计统计、永久解锁和最近 10 条结算 |
| POST | `/account/unlocks/{unlockId}` | 消耗因果点购买永久解锁，返回最新账号进度 |

当前永久解锁：

| ID | 名称 | 费用 | 后续新局效果 |
| --- | --- | ---: | --- |
| `first_breath` | 先天气血 | 5 | 初始气血 +8 |
| `spirit_spring` | 灵泉回响 | 15 | 初始灵力 +5 |
| `long_life` | 长生余烬 | 20 | 初始寿元 +5 |
| `karma_lens` | 因果观 | 25 | 初始因果 +2 |
| `wealth_memory` | 旧日宝藏 | 30 | 初始灵石 +20 |

V0.8 新增永久解锁：

| ID | 名称 | 费用 | 后续新局效果 |
| --- | --- | ---: | --- |
| `sword_bone` | 剑骨初鸣 | 30 | 初始气血 +6、初始灵力 +2 |
| `alchemy_ember` | 丹火余温 | 32 | 初始灵力 +4、初始寿元 +2 |
| `flesh_heart` | 不灭战躯 | 35 | 初始气血 +12 |
| `karma_tide` | 因果潮汐 | 40 | 初始因果 +3 |

死亡结算基础发放 3 点因果，飞升结算基础发放 20 点；抵达层数、精英挑战和正因果会提高发放量。发放结果写入 `run_settlement.causality_earned`，以结算快照保证幂等。永久进度按账号共享，角色之间不会互相覆盖。

## V0.8 成就与账号统计

`GET /account/progress` 在原有因果、永久解锁和结算历史之外返回：

- `totalRuns`、`completedRuns`：账号创建存档数和终局结算数
- `ascendedRuns`、`deadRuns`：飞升和陨落结算数
- `highestFloor`、`bestScore`：结算快照中的最高抵达层数和最高积分
- `achievementCount`、`achievements`：已解锁数量和完整成就目录

当前成就包括：踏出第一步、因果初结、劫后余生、白日飞升、深入天关、猎尽强敌、百炼成器、因果富足。成就记录写入 `achievement_record`，账号与成就组成唯一键，重复恢复不会重复达成。

迁移文件：`database/migrations/20260725_v08_achievements.sql`。

## V0.9 成就奖励与结算明细

成就对象新增：

- `rewardCausality`：首次达成时发放的因果点
- `awardedAt`：实际达成时间

账号进度新增 `totalAchievementRewards`，表示当前账号已领取的成就因果总额。首次达成时奖励由服务端写入账号余额和 `achievement_record`，重复恢复不会再次发放；迁移前已存在的 V0.8 记录默认奖励为 0，不会被历史迁移重复追发。

结算对象新增 `scoreBreakdown`：

| 字段 | 说明 |
| --- | --- |
| `progressBonus` | 抵达层数加分 |
| `turnBonus` | 回合数加分 |
| `healthBonus`、`spiritBonus`、`lifespanBonus` | 结算属性加分 |
| `karmaBonus`、`spiritStonesBonus` | 因果和灵石加分 |
| `buildBonus` | 有效卡牌加分 |
| `eliteBonus` | 精英节点加分 |
| `ascensionBonus` | 飞升额外加分 |
| `total` | 明细合计，等于结算 `score` |

## 配置中心接口

所有 `/api/admin/**` 配置中心接口都需要管理员令牌。请求可以使用 `X-Admin-Token`，也可以使用
`Authorization: Bearer <token>`。本地开发默认令牌为 `dev-admin-token`，生产环境必须通过
`ADMIN_CONFIG_TOKEN` 环境变量覆盖。管理员令牌与玩家 JWT 是两套独立凭证。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/config/events?includeDisabled=false` | 查询事件和结局配置，默认只返回启用项 |
| POST | `/admin/config/events/import` | 导入配置并执行全量引用校验，失败时不写入配置 |
| GET | `/admin/config/cards?includeDisabled=false` | 查询功法、法宝、符箓统一配置，默认只返回启用项 |
| POST | `/admin/config/cards/import` | 统一导入三类卡牌配置，导入后重建卡牌缓存并执行校验 |
| POST | `/admin/config/validate?operator=admin` | 校验当前启用配置并记录 `VALIDATE` 日志 |
| POST | `/admin/config/reload?operator=admin` | 从数据库重建事件缓存并记录 `RELOAD` 日志 |
| GET | `/admin/config/logs` | 查询最近 50 条初始化、导入、校验和重载日志 |

卡牌统一导入请求示例：

```json
{
  "operator": "admin",
  "configs": [
    {
      "cardId": "new_skill",
      "category": "功法",
      "name": "新功法",
      "rarity": "稀有",
      "description": "卡牌描述",
      "effectText": "领取后获得 1 点灵力",
      "archetype": "剑修",
      "healthOnClaim": 0,
      "spiritOnClaim": 1,
      "lifespanOnClaim": 0,
      "karmaOnClaim": 0,
      "battleHealthBonus": 0,
      "battleSpiritBonus": 0,
      "combatDamageBonus": 1,
      "combatBlockBonus": 0,
      "combatSpiritGain": 0,
      "combatPoisonBonus": 0,
      "battleWeight": 1,
      "eliteWeight": 2,
      "treasureWeight": 2,
      "version": 1,
      "enabled": true
    }
  ]
}
```

示例请求头：

```http
X-Admin-Token: dev-admin-token
Content-Type: application/json
```

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
