# 逆命仙途

一个由 Java 后端驱动的文字修仙肉鸽游戏原型。

开发路线请查看：[DEVELOPMENT_PLAN.md](./DEVELOPMENT_PLAN.md)

## 技术栈

- 后端：Java 21+、Spring Boot、Spring Data JPA
- 数据库：MySQL 8
- 前端：React、TypeScript、Vite
- 部署辅助：Docker Compose

## 当前 MVP

- 创建角色并选择出身
- 开启一局修仙旅程
- 随机事件与三选一决策
- 每局 seed 生成 10 层路线图，每层多个节点并通过连线限制相邻选择
- 普通战斗、精英战斗、随机事件、休息闭关、坊市商店、秘境宝藏、渡劫 Boss
- 节点内容按权重抽取，支持稀有事件、可重复事件和多种特殊结局
- 初始功法/符箓构筑，战斗、精英和秘境节点提供奖励三选一
- 卡牌效果会影响后续战斗结算，奖励选择支持存档恢复
- 灵石资源与休息/闭关节点卡牌升级，升级状态支持存档恢复
- 生命、灵力、寿元、因果属性变化
- 存档到 MySQL
- React 前端展示路线图、节点连线、当前事件和修仙日志

## 启动数据库

```powershell
docker compose up -d mysql
```

如果本机已经有 MySQL 8，可以使用 `database/init.sql` 创建项目数据库和项目账号：

```powershell
mysql -u root -p < database/init.sql
```

该脚本只操作 `xiuxian_game` 数据库和 `xiuxian` 项目账号，不会删除其他数据库或用户数据。

## 启动后端

```powershell
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

## 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 主要接口

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| POST | `/api/game/runs` | 创建一局游戏并生成路线图 |
| GET | `/api/game/runs/{id}` | 查询游戏存档 |
| POST | `/api/game/runs/{id}/nodes/{nodeId}/enter` | 进入当前可达节点 |
| POST | `/api/game/runs/{id}/choices` | 提交事件选择并解锁下一跳 |
| POST | `/api/game/runs/{id}/rewards/{rewardId}/claim` | 领取一张构筑奖励并解锁路线 |
| POST | `/api/game/runs/{id}/upgrades/{cardId}` | 在闭关节点消耗灵石升级卡牌 |
| POST | `/api/game/runs/{id}/upgrades/skip` | 跳过本次闭关升级并解锁路线 |

## 默认数据库配置

数据库地址：`localhost:3306/xiuxian_game`

用户名：`xiuxian`

密码：`xiuxian_dev`
