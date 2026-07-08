# server 模块文档

> **模块标识**：`com.gomatch:server`  
> **职责**：多线程 Socket 服务端，实现用户管理、房间管理、游戏逻辑、数据持久化和 Redis 缓存。

---

## 1. 模块概览

```
server/src/main/
├── java/com/gomatch/server/
│   ├── ServerMain.java                 # 服务器入口，TCP 监听 + 定时任务
│   ├── dao/                            # 数据访问层（JDBC + HikariCP）
│   │   ├── DBUtil.java                 # 数据库连接池工具
│   │   ├── UserDAO.java                # 用户 CRUD
│   │   ├── RecordDAO.java              # 对战记录
│   │   ├── ChatDAO.java                # 聊天消息持久化
│   │   ├── FriendDAO.java              # 好友关系
│   │   ├── GameSaveDAO.java            # 棋局存档
│   │   ├── GameActionDAO.java          # 游戏操作记录
│   │   ├── RankDAO.java                # 排行榜物化表
│   │   ├── RoomDAO.java                # 房间状态持久化
│   │   ├── RoomWatcherDAO.java         # 观战记录
│   │   └── SocketSessionDAO.java       # Socket 会话持久化
│   ├── game/
│   │   ├── Game.java                   # 五子棋核心逻辑（棋盘、判胜、悔棋）
│   │   └── AIPlayer.java               # 服务端 AI 对战逻辑
│   ├── handler/
│   │   └── ClientHandler.java          # 每连接一个线程，协议解析与命令分发
│   ├── redis/
│   │   └── RedisManager.java           # Redis 连接管理（Lettuce）
│   ├── room/
│   │   ├── Room.java                   # 房间实体（玩家/观众/游戏状态）
│   │   └── RoomManager.java            # 房间单例管理器，提供匹配功能
│   └── session/
│       └── SessionManager.java         # 用户会话管理（内存 + Redis 双写）
└── resources/
    └── logback.xml                     # SLF4J/Logback 日志配置
```

---

## 2. 启动流程

```
ServerMain.main()
  ├─ DBUtil.init()                    → 初始化 HikariCP 连接池（localhost:3306/gomatch）
  ├─ RoomManager.getInstance()        → 初始化房间管理单例
  ├─ RedisManager.getInstance().init()→ 连接 Redis（localhost:6379，可选）
  ├─ initRankMaterializedView()       → 刷新排行榜物化表，每 5 分钟自动刷新
  ├─ initRoomPersistence()            → 每 1 分钟同步内存房间到 DB，清理 30 分钟前结束的房间
  ├─ addShutdownHook(shutdown)        → JVM 退出时优雅关闭
  └─ startListening()                 → 绑定 8888 端口，循环 accept 连接
       └─ 每次连接：new ClientHandler(socket) → new Thread → thread.start()
```

**并发模型**：每个客户端连接创建一个独立 `Thread`（非线程池），命名为 `client-<port>`。最大连接数限制 `MAX_CONNECTIONS = 100`。

---

## 3. 核心类详解

### 3.1 `ServerMain` — 服务器入口

**文件**：[ServerMain.java](../server/src/main/java/com/gomatch/server/ServerMain.java)

| 常量/字段 | 值 | 说明 |
|-----------|-----|------|
| `PORT` | `8888` | 监听端口 |
| `MAX_CONNECTIONS` | `100` | 最大并发连接数 |
| `connectionCount` | `AtomicInteger` | 当前活跃连接计数（线程安全） |
| `running` | `volatile boolean` | 服务器运行标志 |

| 方法 | 说明 |
|------|------|
| `initRankMaterializedView()` | 启动时刷新并启动每 5 分钟自动刷新任务 |
| `initRoomPersistence()` | 启动每 1 分钟房间状态同步任务，清理 30 分钟前结束房间 |
| `startListening()` | 主监听循环，`ServerSocket.accept()` → 创建 Handler 线程 |
| `shutdown()` | 优雅关闭：停止定时任务 → 关闭 Redis → 关闭 DB |
| `getActiveConnections()` | 获取当前活跃连接数 |

---

### 3.2 `ClientHandler` — 消息分发核心

**文件**：[ClientHandler.java](../server/src/main/java/com/gomatch/server/handler/ClientHandler.java)  
**实现**：`Runnable`（每连接一个独立线程）

#### 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `socket` | `Socket` | 客户端连接 |
| `userId` | `long` | 认证后的用户 ID |
| `username` | `String` | 认证后的用户名 |
| `authenticated` | `boolean` | 是否已完成登录认证 |
| `currentRoom` | `Room` | 当前所在房间 |

#### 静态共享 DAO（所有 Handler 线程共享同一实例）

```java
private static final UserDAO userDAO = new UserDAO();
private static final RecordDAO recordDAO = new RecordDAO();
private static final ChatDAO chatDAO = new ChatDAO();
private static final FriendDAO friendDAO = new FriendDAO();
private static final RoomWatcherDAO roomWatcherDAO = new RoomWatcherDAO();
private static final GameSaveDAO gameSaveDAO = new GameSaveDAO();
private static final RankDAO rankDAO = new RankDAO();
private static final RoomManager roomManager = RoomManager.getInstance();
private static final SessionManager sessionManager = SessionManager.getInstance();
```

#### 命令分发表（`dispatch` 方法）

| 客户端指令 | 处理方法 | 说明 |
|------------|----------|------|
| `LOGIN` | `handleLogin` | 验证凭据，创建 Session，回复 `LOGIN_OK` |
| `REGISTER` | `handleRegister` | 注册新用户，自动登录 |
| `CREATE_ROOM` | `handleCreateRoom` | 创建普通房间 |
| `CREATE_AI_ROOM` | `handleCreateAiRoom` | 创建人机对战房间 |
| `JOIN_ROOM` | `handleJoinRoom` | 加入已有房间（含密码校验） |
| `LEAVE_ROOM` | `handleLeaveRoom` | 离开当前房间 |
| `MOVE` | `handleMove` | 落子，校验合法性，同步给所有玩家和观众 |
| `CHAT` | `handleChat` | 广播聊天，持久化到 DB |
| `SURRENDER` | `handleSurrender` | 认输，触发游戏结束流程 |
| `SPECTATE` | `handleSpectate` | 加入观战，获取历史棋局 |
| `ROOM_LIST` | `handleRoomList` | 返回当前所有等待中的房间列表 |
| `QUICK_MATCH` | `handleQuickMatch` | 快速匹配（Redis 队列） |
| `RANKED_MATCH` | `handleRankedMatch` | 排位匹配（ELO 分数 Redis Sorted Set） |
| `RANKING` | `handleRanking` | 返回排行榜数据（Redis + DB 物化表） |
| `QUERY_RECORD` | `handleQueryRecord` | 返回用户对战记录 |
| `REQUEST_UNDO` | `handleRequestUndo` | 发起悔棋请求，通知对手 |
| `REPLY_UNDO` | `handleReplyUndo` | 同意/拒绝悔棋 |
| `REQUEST_DRAW` | `handleRequestDraw` | 发起求和请求 |
| `REPLY_DRAW` | `handleReplyDraw` | 同意/拒绝求和 |
| `RECONNECT` | `handleReconnect` | 断线重连，恢复棋局状态 |
| `CHANGE_PASSWORD` | `handleChangePassword` | 修改密码 |
| `ADD_FRIEND` | `handleAddFriend` | 发起好友申请 |
| `ACCEPT_FRIEND` | `handleAcceptFriend` | 接受好友申请 |
| `REJECT_FRIEND` | `handleRejectFriend` | 拒绝好友申请 |
| `DELETE_FRIEND` | `handleDeleteFriend` | 删除好友 |
| `FRIEND_LIST` | `handleFriendList` | 返回好友列表及在线状态 |
| `FRIEND_REQUESTS` | `handleFriendRequests` | 返回收到的好友申请 |
| `SAVE_GAME` | `handleSaveGame` | 保存当前棋局 |
| `LOAD_GAME` | `handleLoadGame` | 加载存档并重建棋局 |
| `DELETE_SAVE` | `handleDeleteSave` | 删除指定存档 |
| `SAVE_LIST` | `handleSaveList` | 返回用户存档列表 |
| `CHAT_HISTORY` | `handleChatHistory` | 返回房间聊天历史 |
| `WATCHER_HISTORY` | `handleWatcherHistory` | 返回观战历史 |
| `LOGOUT` | `handleLogout` | 退出登录，清理会话，离开房间 |

---

### 3.3 `Room` — 房间实体

**文件**：[Room.java](../server/src/main/java/com/gomatch/server/room/Room.java)

#### 枚举类型

| 枚举 | 值 | 说明 |
|------|----|------|
| `Status` | `WAITING`, `PLAYING`, `FINISHED` | 房间状态 |
| `GameType` | `NORMAL`, `RANKED` | 对战类型 |

#### 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `roomId` | `String` | UUID 前 8 位，全局唯一 |
| `roomName` | `String` | 房间名称 |
| `password` | `String` | 可选密码（null 表示无密码） |
| `status` | `Status` | 当前状态 |
| `game` | `Game` | 棋局实例 |
| `players` | `CopyOnWriteArrayList<ClientHandler>` | 玩家列表（最多 2 人）|
| `observers` | `CopyOnWriteArrayList<ClientHandler>` | 观众列表 |
| `blackUserId` / `whiteUserId` | `long` | 黑白方用户 ID |
| `aiMode` | `boolean` | 是否为人机对战模式 |
| `aiPlayer` | `AIPlayer` | 人机对战 AI 实例 |
| `gameType` | `GameType` | 对战类型（普通/排位） |
| `undoRequesterId` / `drawRequesterId` | `Long` | 悔棋/求和请求方 ID |
| `currentRecordId` | `long` | 当前对局在 DB 中的记录 ID |
| `gameStartTime` | `Instant` | 对局开始时间 |

#### 关键方法

| 方法 | 说明 |
|------|------|
| `join(handler)` | 玩家加入房间，广播 `PLAYER_ENTER` |
| `leave(handler)` | 玩家离开，处理游戏中断（判负），广播 `PLAYER_LEAVE`；若房间空则销毁 |
| `startGame()` | 开始对局，创建 DB 记录，随机分配黑白，广播 `GAME_START` |
| `move(handler, x, y)` | 验证并落子，同步给双方和观众，判断胜负 |
| `broadcast(msg)` | 向所有玩家广播 |
| `broadcastAll(msg)` | 向玩家 + 观众全体广播 |
| `checkPassword(input)` | 密码校验（无密码房间始终通过） |

---

### 3.4 `RoomManager` — 房间管理器

**文件**：[RoomManager.java](../server/src/main/java/com/gomatch/server/room/RoomManager.java)  
**模式**：单例（`getInstance()`）

#### 核心数据结构

```java
// 线程安全的房间注册表，roomId → Room
private final Map<String, Room> rooms = new ConcurrentHashMap<>();
```

#### 主要方法

| 方法 | 说明 |
|------|------|
| `createRoom(name, owner)` | 创建普通房间，UUID 生成 roomId，房主自动加入 |
| `createRoom(name, owner, password)` | 创建密码房间 |
| `createAiRoom(name, owner)` | 创建人机对战房间（`aiMode=true`） |
| `createRankedRoom(name, owner)` | 创建排位赛房间（`GameType.RANKED`） |
| `getRoom(roomId)` | 根据 ID 查找房间 |
| `getAllRooms()` | 返回不可变房间视图（用于 RoomDAO 同步）|
| `findRoomByUserId(userId)` | 断线重连时查找玩家所在房间 |
| `removeRoom(roomId)` | 销毁房间 |
| `quickMatch(player)` | 快速匹配（Redis List 队列），匹配成功立即开始游戏 |
| `rankedMatch(player, score)` | 排位匹配（Redis Sorted Set），按 ELO 分差寻找对手 |
| `joinExistingRoom(player, roomId, password)` | 玩家加入已有房间，校验状态和密码 |
| `leaveRoom(player)` | 玩家离开当前房间 |

---

### 3.5 `Game` — 五子棋核心逻辑

**文件**：[Game.java](../server/src/main/java/com/gomatch/server/game/Game.java)

#### 棋盘参数

| 常量 | 值 | 说明 |
|------|----|------|
| `SIZE` | `15` | 棋盘大小（15×15） |
| `EMPTY` | `0` | 空位 |
| `BLACK` | `1` | 黑棋 |
| `WHITE` | `2` | 白棋 |

#### 主要方法

| 方法 | 说明 |
|------|------|
| `move(x, y, userId)` | 落子：校验坐标合法性、位置是否占用、记录历史；自动切换玩家；调用 `checkWin` |
| `checkWin(x, y)` | 四方向检测连五（横、纵、正斜、反斜），返回 `true` 表示当前玩家获胜 |
| `getWinner()` | 返回胜方颜色（`BLACK`/`WHITE`/`EMPTY`）|
| `undo()` | **synchronized** 悔棋：移除最后一步历史，还原棋盘，切回上一玩家 |
| `reset()` | 重置棋盘（当前未使用，预留给棋局重开） |
| `getMoveHistory()` | 返回只读的落子历史列表（用于观战回放） |

#### `MoveRecord` 内部类

```java
public static class MoveRecord {
    public int x;       // 行
    public int y;       // 列
    public int color;   // BLACK=1 / WHITE=2
    public long userId; // 落子用户 ID
}
```

---

### 3.6 `SessionManager` — 用户会话管理

**文件**：[SessionManager.java](../server/src/main/java/com/gomatch/server/session/SessionManager.java)  
**模式**：单例；启动时创建守护线程定期清理过期会话

#### `Session` 内部类字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `token` | `String` | UUID 随机 Token |
| `userId` | `long` | 用户 ID |
| `username` | `String` | 用户名 |
| `score` | `int` | 当前积分 |
| `handler` | `ClientHandler` | 关联的网络处理器（断线后置 null）|
| `lastActiveTime` | `Instant` | 最后活跃时间 |
| `roomId` | `String` | 当前所在房间 ID |

**会话超时**：`SESSION_TIMEOUT = 300` 秒（5 分钟），守护线程每分钟清理

#### 主要方法

| 方法 | 说明 |
|------|------|
| `createSession(userId, ...)` | 创建内存 Session，同步写入 Redis 和 SocketSessionDAO |
| `getSessionByToken(token)` | 优先查内存；未命中时从 Redis 恢复（支持服务重启后恢复） |
| `getSessionByUserId(userId)` | 按用户 ID 查询 Session |
| `removeSession(token)` | 删除 Session，同步删除 Redis 和 SocketSessionDAO |
| `updateSessionRoom(userId, roomId)` | 更新 Session 中的房间 ID |
| `updateSessionHandler(userId, handler)` | 断线重连时绑定新的 Handler |

---

### 3.7 `RedisManager` — Redis 缓存层

**文件**：[RedisManager.java](../server/src/main/java/com/gomatch/server/redis/RedisManager.java)  
**客户端**：Lettuce（异步 Redis 客户端），使用同步 API  
**模式**：单例；Redis 不可用时优雅降级（`connected=false`）

#### 功能分区

| 功能 | Redis 数据结构 | Key 前缀 | 说明 |
|------|---------------|----------|------|
| 用户会话 | Hash | `session:` | Token → 用户信息 JSON |
| 在线状态 | String | `online:` | userId → token |
| 快速匹配队列 | List | `match:queue` | FIFO 匹配队列 |
| 排位匹配队列 | Sorted Set | `ranked:queue` | 以 ELO 分数为 score |
| 排行榜缓存 | Sorted Set | `rank:board` | 全服排行 |

#### 主要方法

| 方法 | 说明 |
|------|------|
| `saveSession(token, data)` | 保存会话到 Redis Hash（5 分钟过期） |
| `getSession(token)` | 从 Redis 读取会话数据 |
| `removeSession(token)` | 删除 Redis 会话 |
| `updateSessionActivity(token)` | 重置 TTL（保活） |
| `setOnline(userId, token)` | 标记用户在线 |
| `setOffline(userId)` | 标记用户离线 |
| `addMatchQueue(userId, score)` | 加入快速匹配队列 |
| `pollMatchQueue()` | 从快速匹配队列取出对手 |
| `addRankedMatchQueue(userId, score)` | 加入排位匹配队列 |
| `pollRankedMatchQueue(score)` | 从排位队列取出积分相近（±200）的对手 |
| `close()` | 关闭 Lettuce 连接 |

> [!NOTE]
> Redis 为可选组件。若连接失败，`connected=false`，所有方法安全返回默认值，不影响核心游戏功能。

---

## 4. 数据访问层（DAO）

### 4.1 `DBUtil` — 连接池

**文件**：[DBUtil.java](../server/src/main/java/com/gomatch/server/dao/DBUtil.java)

| 参数 | 值 |
|------|----|
| 连接 URL | `jdbc:mysql://localhost:3306/gomatch` |
| 用户名 | `gomatch` |
| 密码 | `password` |
| 最大连接数 | `20` |
| 最小空闲 | `5` |
| 连接池 | HikariCP |

### 4.2 DAO 类汇总

| DAO 类 | 表名 | 主要操作 |
|--------|------|---------|
| `UserDAO` | `t_user` | 用户注册、登录验证、密码修改、积分更新（ELO） |
| `RecordDAO` | `t_record` | 创建对战记录、更新结果、查询历史 |
| `ChatDAO` | `t_chat_message` | 保存聊天消息、查询房间聊天历史 |
| `FriendDAO` | `t_friend` | 发起/接受/拒绝/删除好友关系 |
| `GameSaveDAO` | `t_game_save` | 保存/加载/删除棋局存档（含落子序列JSON）|
| `GameActionDAO` | `t_game_action` | 记录每一步落子操作（用于回放和审计）|
| `RankDAO` | `t_rank` | 排行榜物化表刷新和查询（每 5 分钟自动刷新）|
| `RoomDAO` | `t_room` | 房间状态同步到 DB（每 1 分钟），清理过期房间 |
| `RoomWatcherDAO` | `t_room_watcher` | 观战记录保存和查询 |
| `SocketSessionDAO` | `t_socket_session` | Socket 会话持久化，断线检测，自动清理 60 分钟前的过期会话 |

---

## 5. 游戏流程

### 5.1 普通对战流程

```
玩家A 创建房间 (CREATE_ROOM)
  → 服务端创建 Room，回复 ROOM_CREATED|roomId
  → 玩家A 进入 RoomWaitingUI

玩家B 加入房间 (JOIN_ROOM|roomId)
  → 服务端校验人数/状态/密码
  → Room.join(B)，广播 PLAYER_ENTER
  → 双方均收到 PLAYER_ENTER

双方均已就绪（2人到位后自动触发）
  → Room.startGame()
  → 创建 t_record 记录
  → 随机分配黑白
  → 广播 GAME_START|blackName|whiteName|先手颜色

对局进行中
  → 玩家发送 MOVE|row|col
  → Game.move() 校验落子合法性
  → 检查是否五连 (checkWin)
  → 广播 MOVE|row|col
  → 若获胜：广播 GAME_OVER|winner|winnerName，更新 t_record，更新 ELO 积分

对局结束
  → GameOverController 弹窗展示结果
  → 返回大厅
```

### 5.2 快速匹配流程

```
玩家发送 QUICK_MATCH
  → RoomManager.quickMatch(handler)
  → Redis addMatchQueue(userId, score)
  → Redis pollMatchQueue()
  → 若取到对手（userId不同）：
      → 查找对手 Handler（SessionManager）
      → createRoom → 双方 join → startGame
  → 若未取到：等待下次匹配请求
```

### 5.3 排位匹配流程

```
玩家发送 RANKED_MATCH
  → RoomManager.rankedMatch(handler, playerScore)
  → Redis addRankedMatchQueue(userId, score)  [ZSet, score为ELO]
  → Redis pollRankedMatchQueue(score)          [查找积分差在±200内的对手]
  → 匹配成功：createRankedRoom → join → startGame
  → 对局结束：ELO 算法计算积分变化，更新 t_user.score
```

### 5.4 断线重连流程

```
客户端发送 RECONNECT|userId|token
  → SessionManager.getSessionByToken(token)   [内存未命中时从 Redis 恢复]
  → RoomManager.findRoomByUserId(userId)       [查找玩家所在房间]
  → 若找到房间且游戏进行中：
      → 重绑定 Handler
      → 发送当前棋盘状态（落子历史回放）
      → 游戏继续
  → 若未找到：回复 LOGIN_OK，重新登录
```

### 5.5 悔棋/求和流程

```
玩家A 发送 REQUEST_UNDO
  → 记录 undoRequesterId = A.userId
  → 通知玩家B（UNDO_REQUEST）

玩家B 发送 REPLY_UNDO|accept
  → accept: Game.undo() → 广播新棋盘
  → reject: 通知玩家A拒绝

（求和流程类似，accept 触发平局结算）
```

---

## 6. ELO 积分算法

排位对战结束后，服务端通过 ELO 公式更新双方积分：

```
期望得分 = 1 / (1 + 10^((对手积分 - 己方积分) / 400))
新积分 = 当前积分 + K × (实际得分 - 期望得分)
```

- **K 因子**：通常取 32（在 UserDAO 中实现）
- **实际得分**：胜 = 1.0，负 = 0.0，平 = 0.5

---

## 7. 定时任务

| 任务名 | 线程名 | 频率 | 功能 |
|--------|--------|------|------|
| 排行榜刷新 | `rank-refresh` | 每 5 分钟 | 重新计算 t_rank 物化表，更新排名次序 |
| 房间持久化 | `room-persistence` | 每 1 分钟 | 同步内存房间状态到 t_room，清理 30 分钟前结束的房间 |
| 会话清理 | `session-cleanup` | 每 1 分钟 | 清理 300 秒未活跃的内存 Session 和 Redis Session |
| SocketSession 清理 | （由 SessionManager 触发） | 每次会话清理时 | 清理数据库中 60 分钟前过期的 Socket 会话记录 |

---

## 8. 日志配置

**文件**：[logback.xml](../server/src/main/resources/logback.xml)

- 使用 SLF4J + Logback 框架
- 默认日志级别：`INFO`（可调整为 `DEBUG` 查看详细落子日志）
- 输出到控制台，格式：`时间 [线程名] 级别 类名 - 消息`

---

## 9. 并发安全设计

| 机制 | 用途 |
|------|------|
| `ConcurrentHashMap` | `rooms`（房间注册表）、`sessionsByToken`、`sessionsByUserId` |
| `CopyOnWriteArrayList` | `Room.players`、`Room.observers`（读多写少的玩家列表）|
| `AtomicInteger` | `connectionCount`（连接计数）|
| `volatile` | `running`（服务器运行标志）、`connected`（Redis 连接状态）|
| `synchronized` | `Game.undo()`（悔棋防竞态）、`RedisManager.init()` |

---

## 10. 启动命令

```powershell
# 方式一：使用本机脚本（推荐）
.\scripts\ssc_server.ps1

# 方式二：Maven 直接运行
mvn -f server/pom.xml exec:java -Dexec.mainClass="com.gomatch.server.ServerMain"
```

**前置条件**：
1. MySQL 已启动，数据库 `gomatch` 已初始化（执行 `sql/init.sql`）
2. JDK 21 已配置
3. 已执行 `mvn install -pl common -am` 安装 common 模块
