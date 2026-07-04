# Gomatch 网络五子棋联机对战平台需求文档

## 1. 项目概述

Gomatch 是一个基于 **Swing + Socket 多线程服务端 + JDBC** 三段式架构的五子棋对战平台，支持玩家在线实时对弈、房间管理、聊天、观战、战绩统计与排行榜等功能。面向校园/局域网场景，提供轻量级的联机对战体验。

---

## 2. 技术栈需求

### 2.1 技术栈
| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 运行环境 | JDK | 21 LTS | 跨平台运行基础，LTS 版本稳定支持到 2031 年 |
| 客户端 UI | JavaFX (FXML + CSS) | 21 | 构建桌面窗口、棋盘绘制、按钮/列表/对话框等交互组件；FXML 布局 + CSS 样式 |
| 网络通信 | Java Socket（TCP） | JDK 内置 | 客户端与服务端之间的实时双向数据传输；自定义文本协议 |
| 并发模型 | Java 原生多线程 | JDK 内置 | 每连接分配独立 `ClientHandler` 线程；`Room` 对象同步锁保障并发安全 |
| 数据持久化 | JDBC + MySQL | MySQL 8.x | 用户注册/登录、战绩写入与查询、排行榜聚合查询 |
| 构建工具 | Maven | 3.9+ | 多模块管理（client/server/common），统一依赖版本与打包 |
| 数据库连接池 | HikariCP | 5.x | 替代手动 `DriverManager.getConnection()`，提供连接复用与超时管理 |
| JSON 序列化 | Gson | 2.10+ | 用户/棋谱/房间列表等数据在 Socket 传输时的序列化/反序列化 |
| 日志框架 | SLF4J + Logback | SLF4J 2.x / Logback 1.4+ | 替代 `System.out.println`，按级别输出，便于调试与线上追踪 |
| 密码加密 | BCrypt | - | 注册时加盐哈希，登录时比对哈希值；避免明文存储与彩虹表攻击 |
| 单元测试 | JUnit 5 | - | 覆盖五连判定算法、协议编解码、DAO 层 CRUD 等核心逻辑 |

### 2.2 部署模型
```
客户端（JavaFX） ←— TCP Socket —→ 服务端（多线程） ←— JDBC —→ MySQL
```
客户端启动独立接收线程读取服务端推送；服务端为每个连接分配独立线程，`Room` 对象管理状态机与广播。

---

## 3. 功能需求

### 3.1 用户模块

#### 3.1.1 注册（FR-001）
- 输入：用户名、密码、确认密码。
- 校验：用户名唯一、长度 4–16 位、密码长度 ≥6。
- 结果：注册成功后自动跳转登录。

#### 3.1.2 登录（FR-002）
- 输入：用户名、密码。
- 校验：与数据库 `t_user` 比对。
- 结果：登录成功后进入游戏大厅，并建立 Socket 连接。
- 异常：同一账号重复登录需给出提示（可选：顶号或拒绝）。

#### 3.1.3 个人中心（FR-003）
- 显示：用户名、头像（默认）、积分、胜场、负场、平局。
- 支持：修改密码。

### 3.2 游戏大厅模块

#### 3.2.1 房间列表（FR-004）
- 实时显示当前房间列表：房间号、房间名、房主、当前人数/上限（2/2）、状态（等待中/对局中）。
- 支持：刷新列表。

#### 3.2.2 创建房间（FR-005）
- 输入：房间名、密码（可选）。
- 行为：创建后房主进入房间，房间状态为“等待中”。
- 服务端：生成唯一 roomId，维护 Room 对象。

#### 3.2.3 加入房间（FR-006）
- 支持双击或按钮加入房间。
- 若房间满员或已开始，给出提示。
- 支持带密码的房间校验。

#### 3.2.4 快速匹配（FR-007，可选）
- 系统自动将等待中的两名玩家匹配到同一房间。

### 3.3 棋盘对战模块

#### 3.3.1 棋盘界面（FR-008）
- 15×15 标准五子棋棋盘。
- 显示双方用户名、执子颜色、当前轮次倒计时（可选）。
- 支持实时落子、悔棋请求、求和请求。

#### 3.3.2 落子同步（FR-009）
- 玩家在己方回合点击棋盘落子。
- 客户端将落子坐标发送至服务端。
- 服务端校验合法性（位置为空、当前轮次、未结束）后广播给房间内所有客户端（包括观战者）。

#### 3.3.3 胜负判定（FR-010）
- 每次落子后服务端执行五连判定：横、竖、左斜、右斜四个方向。
- 若出现五子连珠，则对局结束，胜者积分增加，败者积分减少（或仅记录胜负）。
- 若棋盘填满且无五连，判定平局。

#### 3.3.4 轮次控制（FR-011）
- 黑先白后，交替落子。
- 服务端维护当前执棋方，拒绝非当前回合玩家的落子请求。

#### 3.3.5 悔棋/求和（FR-012，进阶）
- 玩家可发起悔棋，需对方同意。
- 玩家可发起求和，需对方同意。

### 3.4 聊天模块

#### 3.4.1 房间聊天（FR-013）
- 房间内玩家与观战者可发送文本消息。
- 服务端将消息广播给房间所有成员。
- 支持显示发送者昵称。

### 3.5 战绩与排行榜模块

#### 3.5.1 战绩查询（FR-014）
- 玩家可查看自己的历史对局记录：对手、结果、时间、棋谱回放（可选）。

#### 3.5.2 排行榜（FR-015）
- 按积分/胜率排序展示前 N 名玩家。
- 支持实时刷新。

### 3.6 观战模块（FR-016，进阶）
- 非对局玩家可进入“对局中”的房间观战。
- 实时同步棋盘状态与聊天消息。

### 3.7 断线处理（FR-017）

#### 3.7.1 断线判负（基础）
- 某玩家 Socket 断开后，若对局进行中，则判定该玩家负。
- 通知对手与观战者。

#### 3.7.2 断线重连（挑战）
- 玩家掉线后可在一定时间内重连回同一房间，恢复棋盘状态。

### 3.8 人机对战（FR-018，挑战）
- 玩家可选择与简单 AI 对弈。
- AI 采用简单启发式策略（如连子评分、随机落子）。

---

## 4. 非功能需求

| 编号 | 需求 | 说明 |
|------|------|------|
| NFR-001 | 性能 | 局域网内落子延迟 ≤ 100ms |
| NFR-002 | 并发 | 服务端同时支持 ≥ 50 房间或 ≥ 100 连接 |
| NFR-003 | 可靠性 | 断线后 5 秒内完成判负或重连处理 |
| NFR-004 | 安全性 | 密码 BCrypt 加密存储，SQL 注入通过预编译防范 |
| NFR-005 | 可用性 | 界面操作反馈明确，异常提示友好 |
| NFR-006 | 可维护性 | 服务端按模块分层：网络层、业务层、数据层 |

---

## 5. 角色与权限

| 角色 | 权限 |
|------|------|
| 游客 | 仅可浏览排行榜（可选） |
| 玩家 | 登录、大厅、创建/加入房间、对弈、聊天、查看战绩/排行榜、个人中心 |
| 管理员 | 玩家所有权限 + 查看所有房间、强制解散房间、查看所有用户、重置密码 |

---

## 6. 界面清单（≥10）

| 编号 | 界面名称 | 说明 |
|------|----------|------|
| UI-001 | 登录界面 | 用户名、密码、注册入口 |
| UI-002 | 注册界面 | 用户名、密码、确认密码、返回登录 |
| UI-003 | 游戏大厅（LobbyUI） | 头像/昵称、匹配/排位入口、排行榜抽屉、好友/设置按钮 |
| UI-004 | 匹配大厅（CasualLobbyUI） | 房间列表 + 观战列表、创建/加入/快速匹配 |
| UI-005 | 排位大厅（RankedLobbyUI） | 段位显示、开始匹配按钮 |
| UI-006 | 创建房间（CreateRoomUI） | 房间名、密码（可选）、确认/取消 |
| UI-007 | 房间等待界面（RoomWaitingUI） | 显示房主/玩家列表、开始按钮（房主）、聊天框 |
| UI-006 | 棋盘对战界面 | 15×15 棋盘、双方信息、聊天框、悔棋/求和/认输按钮 |
| UI-007 | 对局结束界面 | 胜负结果、返回大厅、再来一局 |
| UI-008 | 聊天界面/面板 | 消息列表、输入框、发送按钮 |
| UI-009 | 战绩界面 | 历史对局列表、详情/回放按钮 |
| UI-010 | 排行榜界面 | 排名、用户名、积分、胜率 |
| UI-011 | 个人中心 | 用户信息、修改密码 |
| UI-012 | 关于界面 | 产品名称、版本、开发者信息 |

---

## 7. 数据库设计

### 7.1 表结构

#### t_user（用户表）
| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | BIGINT PK AUTO_INCREMENT | 用户ID |
| username | VARCHAR(32) UNIQUE NOT NULL | 用户名 |
| password | VARCHAR(64) NOT NULL | MD5加密密码 |
| nickname | VARCHAR(32) | 昵称 |
| score | INT DEFAULT 1000 | 积分 |
| win_count | INT DEFAULT 0 | 胜场 |
| lose_count | INT DEFAULT 0 | 负场 |
| draw_count | INT DEFAULT 0 | 平局 |
| role | TINYINT DEFAULT 0 | 0玩家 1管理员 |
| created_at | DATETIME | 注册时间 |

#### t_room（房间表，可内存 + 可选持久化）
| 字段 | 类型 | 说明 |
|------|------|------|
| room_id | VARCHAR(32) PK | 房间唯一ID |
| room_name | VARCHAR(64) | 房间名 |
| owner_id | BIGINT | 房主用户ID |
| password | VARCHAR(32) | 房间密码（空表示公开） |
| status | TINYINT | 0等待中 1对局中 2已结束 |
| created_at | DATETIME | 创建时间 |

#### t_game_record（对局记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| record_id | BIGINT PK AUTO_INCREMENT | 记录ID |
| room_id | VARCHAR(32) | 房间ID |
| black_user_id | BIGINT | 执黑玩家 |
| white_user_id | BIGINT | 执白玩家 |
| winner_id | BIGINT NULL | 胜者ID，NULL为平局 |
| result | TINYINT | 0黑胜 1白胜 2平局 3断线判负 |
| steps | TEXT | 棋谱JSON（如 [[7,7],[6,6],...]） |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |

#### t_rank（排行榜，可聚合视图或物化表）
- 可由 `t_user` 按 `score` 排序聚合得到，无需单独建表；若性能要求高可定时刷新到 `t_rank` 表。

### 7.2 E-R 关系
- 一个用户可参与多局对战（1:N）。
- 一个房间可产生一条对战记录（1:1）。

---

## 8. 服务端设计

### 8.1 核心类

| 类名 | 职责 |
|------|------|
| `GomatchServer` | 服务端入口，监听端口，接收连接 |
| `ClientHandler` | 每个连接的线程，解析协议并分发请求 |
| `RoomManager` | 管理所有房间，单例 |
| `Room` | 房间状态机：等待中、对局中、已结束 |
| `Game` | 棋盘状态、落子校验、胜负判定 |
| `Player` | 玩家连接、用户信息、执子颜色 |
| `Protocol` | 消息协议常量与编解码 |
| `DBUtil` | JDBC 连接池/工具类 |
| `UserDAO` / `RecordDAO` | 数据访问对象 |

### 8.2 关键流程

#### 落子同步流程
```
ClientA 落子 -> ClientA 发送 MOVE x y
                -> Server ClientHandler 接收
                -> Room 校验回合与坐标
                -> Game 更新棋盘 + 判定胜负
                -> Room 广播 MOVE x y color [result] 给 ClientA/B/Observers
                -> 若结束，持久化 record，更新 t_user 积分
```

#### 断线处理流程
```
Socket 异常/关闭 -> ClientHandler 捕获
                -> Room.onPlayerDisconnect(player)
                -> 若游戏中：判定该玩家负，广播结果
                -> 若等待中：从房间移除，若房间空则解散
```

---

## 9. 通信协议设计（自定义文本协议）

格式：`COMMAND|param1|param2|...`

### 9.1 客户端 -> 服务端

| 命令 | 参数 | 说明 |
|------|------|------|
| LOGIN | username\|password | 登录 |
| REGISTER | username\|password | 注册 |
| CREATE_ROOM | roomName\|password | 创建房间 |
| JOIN_ROOM | roomId\|password | 加入房间 |
| LEAVE_ROOM | - | 离开房间 |
| READY | - | 准备 |
| MOVE | x\|y | 落子 |
| CHAT | message | 聊天 |
| SURRENDER | - | 认输 |
| REQUEST_UNDO | - | 请求悔棋 |
| REQUEST_DRAW | - | 请求求和 |
| REPLY_UNDO | agree\|disagree | 响应悔棋 |
| REPLY_DRAW | agree\|disagree | 响应求和 |

### 9.2 服务端 -> 客户端

| 命令 | 参数 | 说明 |
|------|------|------|
| LOGIN_OK | userJson | 登录成功 |
| LOGIN_FAIL | reason | 登录失败 |
| ROOM_LIST | roomJsonList | 房间列表 |
| ROOM_CREATED | roomJson | 创建成功 |
| JOIN_OK | roomJson | 加入成功 |
| JOIN_FAIL | reason | 加入失败 |
| PLAYER_ENTER | userJson | 有玩家进入 |
| PLAYER_LEAVE | userId | 有玩家离开 |
| GAME_START | blackId\|whiteId | 开始对局 |
| MOVE | x\|y\|color | 同步落子 |
| GAME_OVER | result\|reason | 对局结束 |
| CHAT | userId\|nickname\|message | 聊天消息 |
| ERROR | reason | 通用错误 |

---

## 10. 技术难点与实现策略

| 难点 | 策略 |
|------|------|
| 实时双向通信 | 客户端独立接收线程 + 服务端推送；使用 TCP 长连接 |
| 轮次状态机 | `Room` 内维护 `currentTurn`（黑/白），每次落子切换 |
| 五连判定算法 | 以落子点为中心，四个方向分别向两侧计数 |
| 断线判负 | `ClientHandler` 捕获异常后通知 `Room` 处理 |
| 并发多房间隔离 | 每个 `Room` 独立加锁（synchronized），避免房间间相互影响 |
| 棋谱持久化 | 每步以 JSON 数组记录，对局结束时写入 `t_game_record` |

### 10.1 五连判定算法伪代码
```java
int[][] directions = {{1,0},{0,1},{1,1},{1,-1}};
for (int[] d : directions) {
    int count = 1;
    for (int sign = -1; sign <= 1; sign += 2) {
        int nx = x + sign * d[0];
        int ny = y + sign * d[1];
        while (inBounds(nx, ny) && board[nx][ny] == color) {
            count++;
            nx += sign * d[0];
            ny += sign * d[1];
        }
    }
    if (count >= 5) return true;
}
return false;
```

---

## 11. 实现计划

| 步骤 | 任务 | 前置 | 技术要点 |
|------|------|------|----------|
| 1 | 项目骨架搭建 | — | Maven 多模块（client/server/common），pom.xml 依赖管理 |
| 2 | 数据库初始化 | — | 建表 SQL（t_user / t_game_record），HikariCP 配置，DBUtil 封装 |
| 3 | 通信协议定义 | — | Protocol 常量类（命令字 + 分隔符），编解码工具方法 |
| 4 | 服务端主框架 | — | ServerMain 监听端口，ClientHandler 线程模型，异常处理骨架 |
| 5 | 注册 | 1–4 | 客户端表单 → Socket 发送 → 服务端校验 → JDBC INSERT |
| 6 | 登录 | 5 | Socket 连接建立与认证，服务端维护在线用户 Map |
| 7 | 大厅界面 + 房间列表 | 5–6 | Swing JTable / JList 实时刷新，服务端 RoomManager 单例 |
| 8 | 创建/加入房间 | 7 | Room 对象状态机（等待中→对局中→已结束） |
| 9 | 棋盘 UI | 7 | 15×15 棋盘绘制（JPanel + paintComponent），鼠标点击坐标映射 |
| 10 | 落子同步 | 8–9 | MOVE 命令收发，客户端接收线程更新棋盘 |
| 11 | 五连判胜 | 10 | 四方向扫描算法（横/竖/左斜/右斜），服务端判定后广播 GAME_OVER |
| 12 | 断线判负 + 认输 | 10 | ClientHandler 异常捕获 → Room 判负广播 |
| 13 | 战绩入库 | 11–12 | 对局结束时写入 t_game_record，更新 t_user 积分/胜负场 |
| 14 | 密码房间与解散 | 8 | 加入时校验密码；房主可解散房间 |
| 15 | 房间聊天 | 8 | CHAT 命令广播，Swing JTextArea + JScrollPane |
| 16 | 战绩查询 | 13 | 按用户 ID 查询 t_game_record，列表展示 |
| 17 | 排行榜 | 13 | t_user 按 score 降序排列 TOP 20 |
| 18 | 个人中心 | 6 | 显示用户信息，修改密码 |
| 19 | 观战功能 | 10 | 观战者进入对局中房间，同步接收 MOVE 广播 |
| 20 | 悔棋/求和 | 10 | 请求 → 对方确认 → 服务端执行/拒绝；悔棋需回滚棋盘 |
| 21 | 断线重连 | 12 | 重连后发送 RECONNECT，服务端推送棋盘快照 |
| 22 | 简单人机对战 | 11 | 本地 AI 线程，启发式评分（连子数打分 + 最高分落子） |
| 23 | 倒计时/防发呆 | 10 | 每回合 30 秒倒计时，超时判负 |

---

## 12. 项目目录建议

```
gomatch/                                # 项目根目录
├── client/                             # 客户端模块（JavaFX 桌面应用）
│   ├── src/main/java/com/gomatch/client/
│   │   ├── ClientMain.java             # 客户端入口（extends Application）
│   │   ├── ui/                         # FXML Controller + ScreenManager 导航
│   │   ├── network/                    # 网络层（Socket 连接、接收线程、消息分发）
│   │   └── model/                      # 数据模型（User/RoomInfo 等）
│   └── src/main/resources/
│       ├── fxml/                       # FXML 布局文件
│       └── css/style.css               # 全局 CSS（白色极简风）
│   └── pom.xml                         # 客户端 Maven 配置
├── server/                             # 服务端模块（多线程 Socket 服务）
│   ├── src/main/java/com/gomatch/server/
│   │   ├── ServerMain.java             # 服务端入口，监听端口，启动线程池
│   │   ├── handler/                    # ClientHandler：连接处理、协议分发
│   │   ├── room/                       # Room + RoomManager：房间状态机与广播
│   │   ├── game/                       # Game（棋盘状态、落子校验、五连判定）
│   │   ├── protocol/                   # 协议常量、编解码工具
│   │   └── dao/                        # 数据访问层（UserDAO / RecordDAO）
│   └── pom.xml                         # 服务端 Maven 配置
├── common/                             # 公共模块（两端共用）
│   └── src/main/java/com/gomatch/common/
│       └── Protocol.java               # 命令字常量、分隔符定义
├── sql/                                # 数据库脚本
│   └── init.sql                        # 建库建表 DDL + 初始数据
└── README.md                           # 项目说明（启动方式、配置说明）
```

**建议**：
- **Maven 多模块**将 client/server/common 分离，模块间通过 `pom.xml` 的 `<dependency>` 引用，避免代码耦合。
- **common** 模块专门存放两端共用的协议定义，不包含任何业务逻辑，保证 client 不依赖 server 的 DAO 等类。
- **分包原则**：按功能职责分包（ui/network/room/game/dao），而非按层分包（controller/service/dao），更符合本项目的 C/S 结构。
- **SQL 独立存放**在 `sql/` 下，与 Java 代码分离，便于 DBA 审查或直接导入。

---

## 13. 验收标准

1. 两名玩家能完整进行一局五子棋，胜负判定正确。
2. 落子后对方客户端能在 100ms 内显示。
3. 断线后另一方能收到胜利提示。
4. 战绩、积分正确写入数据库。
5. 排行榜按积分排序显示。
6. 界面数量 ≥ 10 个，功能完整。

---

**文档版本**：v1.1  
**编写日期**：2026-07-04  

**修订记录**
| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-07-04 | 初版 |
| v1.1 | 2026-07-04 | 优化章节结构，补充 1.2 产品定位、2.2 部署架构、2.3 通信模型；统一密码加密方案为 BCrypt |
