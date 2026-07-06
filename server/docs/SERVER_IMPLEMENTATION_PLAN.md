# Gomatch Server 模块实现方案

> 基于 requirements.md、DATABASE_DESIGN.md、TECHNICAL_ARCHITECTURE.md
> 生成日期：2026-07-05
> 更新日期：2026-07-06（所有功能已实现）
> 决策人确认：Q1-B, Q2-A, Q3-A, Q4-A, Q5-A, Q6-B, Q7-A

---

## 1. 架构总览

### 1.1 模块职责

Server 模块负责：
- **TCP Socket 长连接监听**（8888 端口）
- 用户认证（注册/登录/退出登录）
- 房间生命周期管理（创建、加入、离开、密码房间）
- 游戏状态同步与胜负判定（15×15 棋盘）
- 聊天消息转发与持久化
- 观战功能（spectate）
- 对局记录持久化
- 排行榜聚合查询（Redis 缓存 + 物化表）
- 快速匹配与排位匹配（ELO 算法）
- 人机对战（AI 启发式评分）
- 好友系统（添加、接受、删除好友）
- 断线重连（5 分钟会话保留）
- 棋局保存/加载
- Redis 缓存优化

### 1.2 技术选型确认

| 组件 | 方案 | 备注 |
|------|------|------|
| 网络通信 | **TCP Socket** | Java 原生 ServerSocket + Socket，每连接一线程 |
| 密码存储 | **BCrypt** | 注册时加盐哈希，登录时比对 |
| 会话管理 | **SessionManager** | 内存 + Redis 持久化，支持断线重连 |
| 广播机制 | Room.broadcast() | 遍历 players + observers |
| 步数格式 | JSON 数组（棋谱格式） | Gson 序列化 |
| 快速匹配 | **Redis List** | 跨服务器匹配队列 |
| 排位匹配 | **Redis Sorted Set** | ELO 算法，积分差距阈值 200 |
| 观战功能 | 房间 observers 列表 + 广播 | 仅接收不干预，记录观战历史 |
| 断线处理 | **SessionManager + Redis** | 5 分钟会话保留，支持重连恢复 |
| 排行榜 | **Redis Sorted Set + 物化表** | O(logN) 查询，每 5 分钟刷新 |
| AI 对战 | 启发式评分策略 | 连子数打分 + 最高分落子 |

---

## 2. Redis 集成方案

### 2.1 依赖引入

**pom.xml 新增依赖**：

```xml
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
    <version>6.3.0.RELEASE</version>
</dependency>
```

### 2.2 Redis 五大用途

#### （1）会话持久化

**Key 设计**：
```
session:{token}  ->  JSON 字符串（会话数据）
  Value: {"userId":1,"username":"player1","score":1000,"roomId":"r01","lastActiveTime":1234567890}
  TTL: 300秒（5分钟）
```

**操作**：
- 登录后：SET session:{token} <sessionData>
- 重连时：GET session:{token} 恢复会话
- 更新房间：更新 sessionData.roomId

#### （2）排行榜缓存

**Key 设计**：
```
rank:leaderboard  ->  Sorted Set（score: userId）
  Value: {1000: "1", 980: "2", 950: "3", ...}
```

**操作**：
- 更新积分：ZADD rank:leaderboard score userId
- 获取 Top N：ZREVRANGE rank:leaderboard 0 N-1
- 获取排名：ZREVRANK rank:leaderboard userId

#### （3）匹配队列

**快速匹配 Key**：
```
match:queue  ->  List（userId:score）
  Value: ["1:1000", "2:980", "3:950"]
```

**排位匹配 Key**：
```
match:ranked  ->  Sorted Set（score: userId:score）
  Value: {1000: "1:1000", 980: "2:980"}
```

#### （4）在线状态追踪

**Key 设计**：
```
online:{userId}  ->  String（token）
  TTL: 300秒（5分钟）
```

**操作**：
- 登录/心跳：SET online:{userId} token
- 退出/断线：DEL online:{userId}
- 检查在线：EXISTS online:{userId}

#### （5）Pub/Sub 实时同步（可选）

**Channel 设计**：
```
gomatch:game:{roomId}  ->  游戏事件频道
```

### 2.3 Redis 连接配置

**从 database.properties 读取**：
```properties
redis.host=localhost
redis.port=6379
redis.password=${GOMATCH_REDIS_PASSWORD}
redis.timeout=3000
redis.database=0
```

**初始化**（Lettuce 客户端）：
```java
RedisURI uri = RedisURI.builder()
    .withHost(host)
    .withPort(port)
    .withTimeout(Duration.ofMillis(timeout))
    .withDatabase(database)
    .build();
RedisClient redisClient = RedisClient.create(uri);
RedisCommands<String, String> syncCommands = redisClient.connect().sync();
```

---

## 3. 核心文件实现状态

### 3.1 ClientHandler.java — 命令处理实现

**当前状态**：✅ 所有方法已实现

**已实现方法清单**：

| 方法 | 参数来源 | 核心逻辑 | 响应消息 |
|------|---------|---------|---------|
| handleLogin | parts[1]=username, parts[2]=password | 调 UserDAO.login()，创建 Session | LOGIN_OK|userId|username|score|token 或 LOGIN_FAIL|reason |
| handleRegister | parts[1]=username, parts[2]=password | 校验长度，调 UserDAO.register() | REGISTER_OK 或 REGISTER_FAIL|reason |
| handleLogout | 无 | 离开房间，清除 Session | LOGOUT_OK |
| handleCreateRoom | parts[1]=roomName, parts[2]=password(可选) | 调 RoomManager.createRoom() | JOIN_OK|roomId|roomName|players/max|hasPassword |
| handleCreateAiRoom | parts[1]=roomName | 调 RoomManager.createAiRoom() | JOIN_OK|roomId|roomName|1/1|AI |
| handleJoinRoom | parts[1]=roomId, parts[2]=password(可选) | 查 Room，校验密码，调 Room.join() | JOIN_OK 或 JOIN_FAIL|reason |
| handleLeaveRoom | 无 | 从房间移除，通知对手 | PLAYER_LEAVE|userId|username 广播 |
| handleMove | parts[1]=x, parts[2]=y | 调 Game.move()，校验合法性，判定胜负 | MOVE|x|y|color 广播 |
| handleChat | parts[1]=message | 转发聊天内容，保存到数据库 | CHAT|userId|username|message 广播 |
| handleSurrender | 无 | 判负，保存记录 | GAME_OVER|winnerId|reason 广播 |
| handleSpectate | parts[1]=roomId | 加入房间观战，记录开始时间 | JOIN_OK|roomId|SPECTATOR |
| handleRoomList | 无 | 获取房间列表 | RANKING_LIST|roomId|roomName|status|players|max |
| handleQuickMatch | 无 | 加入 Redis 匹配队列 | JOIN_OK|roomId|roomName|2/2 |
| handleRankedMatch | 无 | 加入 Redis 排位队列（ELO） | JOIN_OK|roomId|roomName|2/2|RANKED |
| handleRanking | 无 | 从 Redis 获取排行榜 | RANKING_LIST|rank|name|score|winRate |
| handleQueryRecord | parts[1]=page, parts[2]=size | 查 t_game_record | RECORD_LIST|recordId|player1Id|player1Name|... |
| handleChangePassword | parts[1]=oldPassword, parts[2]=newPassword | 校验旧密码，更新新密码 | CHANGE_PASSWORD_OK 或 CHANGE_PASSWORD_FAIL|reason |
| handleReconnect | parts[1]=token | 从 Redis 恢复会话 | RECONNECT_OK|userId|username|...|roomId|... |
| handleRequestUndo | 无 | 请求悔棋（需至少2步） | UNDO_REQUESTED|requesterId|requesterName |
| handleReplyUndo | parts[1]=1/0 | 同意/拒绝悔棋 | UNDO_ACCEPTED / UNDO_REJECTED |
| handleRequestDraw | 无 | 请求求和 | DRAW_REQUESTED|requesterId|requesterName |
| handleReplyDraw | parts[1]=1/0 | 同意/拒绝求和 | GAME_OVER|0|draw / DRAW_REJECTED |
| handleAddFriend | parts[1]=friendId, parts[2]=message(可选) | 发送好友请求 | ADD_FRIEND_OK / ADD_FRIEND_FAIL|reason |
| handleAcceptFriend | parts[1]=requestId | 接受好友请求 | ACCEPT_FRIEND_OK / ACCEPT_FRIEND_FAIL|reason |
| handleRejectFriend | parts[1]=requestId | 拒绝好友请求 | REJECT_FRIEND_OK / REJECT_FRIEND_FAIL|reason |
| handleDeleteFriend | parts[1]=friendId | 删除好友关系 | DELETE_FRIEND_OK / DELETE_FRIEND_FAIL|reason |
| handleFriendList | 无 | 获取好友列表（含在线状态） | FRIEND_LIST|friendId|friendName|friendScore|... |
| handleFriendRequests | 无 | 获取待处理好友请求 | FRIEND_REQUEST_LIST|requestId|fromUserId|fromUsername|... |
| handleSaveGame | parts[1]=saveName(可选) | 保存当前棋局状态 | SAVE_GAME_OK|saveId|saveName |
| handleLoadGame | parts[1]=saveId | 加载棋局存档 | LOAD_GAME_OK|saveId|saveName|boardData|... |
| handleDeleteSave | parts[1]=saveId | 删除棋局存档 | DELETE_SAVE_OK / DELETE_SAVE_FAIL|reason |
| handleSaveList | parts[1]=page, parts[2]=size | 获取存档列表 | SAVE_LIST|saveId|saveName|gameStatus|createTime|... |
| handleChatHistory | parts[1]=page, parts[2]=size | 获取房间聊天历史 | CHAT_HISTORY|messageId|userId|username|content|createTime|... |
| handleWatcherHistory | parts[1]=page, parts[2]=size | 获取观战历史记录 | WATCHER_HISTORY|roomId|joinTime|leaveTime|... |

**核心字段**：
```java
private long userId;           // 登录后设置
private String username;       // 用户名
private boolean authenticated; // 认证状态
private Room currentRoom;      // 当前所在房间
```

### 3.2 Room.java — 广播与状态机扩展

**当前状态**：✅ 所有功能已实现

**新增方法**：
```java
// 广播消息给房间内所有人
public synchronized void broadcastAll(String... msg)

// 广播给玩家（不含观战者）
public synchronized void broadcastToPlayers(String... msg)

// 玩家观战
public synchronized void spectate(ClientHandler handler)

// 观战者离开
public synchronized void leaveObserver(ClientHandler handler)

// 开始游戏
public synchronized boolean startGame()

// 落子
public synchronized boolean makeMove(int x, int y, ClientHandler handler)

// 请求悔棋
public synchronized boolean requestUndo(long userId)

// 回复悔棋
public synchronized boolean replyUndo(long userId, boolean accept)

// 请求求和
public synchronized boolean requestDraw(long userId)

// 回复求和
public synchronized boolean replyDraw(long userId, boolean accept)

// 游戏结束
public synchronized void endGame(long winnerId, String reason)

// 断线重连
public synchronized boolean reconnect(ClientHandler handler)

// 断开连接
public synchronized void disconnect(ClientHandler handler)
```

**核心字段**：
```java
private String roomId;
private String roomName;
private String password;
private Status status;          // WAITING/PLAYING/FINISHED
private long hostId;
private long blackUserId;       // 执黑玩家ID
private long whiteUserId;       // 执白玩家ID
private List<ClientHandler> players;
private List<ClientHandler> observers;
private Game game;
private AIPlayer aiPlayer;      // AI对战模式
private boolean aiMode;         // 是否AI模式
private GameType gameType;      // PVP/RANKED
```

### 3.3 RoomManager.java — 匹配与房间管理

**当前状态**：✅ 所有功能已实现

**核心方法**：
```java
// 创建房间
public Room createRoom(String name, ClientHandler owner)
public Room createRoom(String name, ClientHandler owner, String password)

// 创建AI房间
public Room createAiRoom(String name, ClientHandler owner)

// 创建排位房间
public Room createRankedRoom(String name, ClientHandler owner)

// 加入房间
public boolean joinExistingRoom(ClientHandler handler, String roomId, String password)

// 离开房间
public void leaveRoom(ClientHandler handler)

// 快速匹配（Redis List）
public Room quickMatch(ClientHandler player)

// 排位匹配（Redis Sorted Set + ELO）
public Room rankedMatch(ClientHandler player, int score)

// 查找房间
public Room getRoom(String roomId)
public Room findRoomByUserId(long userId)
```

### 3.4 SessionManager.java — 会话管理

**当前状态**：✅ 所有功能已实现

**核心方法**：
```java
// 创建会话
public Session createSession(long userId, String username, int score, ...)

// 获取会话
public Session getSessionByToken(String token)
public Session getSessionByUserId(long userId)

// 移除会话
public void removeSession(String token)
public void removeSessionByUserId(long userId)

// 更新会话
public void updateSessionRoom(long userId, String roomId)
public void updateSessionHandler(long userId, ClientHandler handler)

// 定时清理
private void cleanupExpiredSessions()
```

### 3.5 RedisManager.java — Redis 缓存

**当前状态**：✅ 所有功能已实现

**核心方法**：
```java
// 会话管理
public void saveSession(String token, SessionData data)
public SessionData getSession(String token)
public void removeSession(String token)
public void updateSessionRoom(String token, String roomId)
public void updateSessionActivity(String token)

// 在线状态
public void setOnline(long userId, String token)
public void setOffline(long userId)
public boolean isOnline(long userId)

// 排行榜
public void updateRank(long userId, int score)
public List<Long> getTopRank(int limit)
public Long getRank(long userId)
public Integer getScore(long userId)
public void clearRank()

// 匹配队列
public void addMatchQueue(long userId, int score)
public String pollMatchQueue()
public void addRankedMatchQueue(long userId, int score)
public List<String> pollRankedMatchQueue(int score)

// 游戏状态缓存
public void saveGameState(String roomId, String boardJson)
public String getGameState(String roomId)
public void deleteGameState(String roomId)

// 分布式锁
public boolean acquireLock(String roomId)
public void releaseLock(String roomId)

// Pub/Sub
public void publish(String roomId, String message)
public void subscribe(String roomId, RedisPubSubListener<String, String> listener)
```

---

## 4. DAO 类实现状态

### 4.1 已实现的 DAO 类

| DAO 类 | 职责 | 状态 |
|--------|------|------|
| UserDAO | 用户注册、登录、积分更新、修改密码 | ✅ |
| RecordDAO | 战绩保存、查询、返回 recordId | ✅ |
| RankDAO | 排行榜物化表、Redis 同步、定时刷新 | ✅ |
| ChatDAO | 聊天消息持久化、历史查询 | ✅ |
| FriendDAO | 好友关系管理、好友请求 | ✅ |
| RoomWatcherDAO | 观战记录管理 | ✅ |
| GameSaveDAO | 棋局保存管理 | ✅ |
| GameActionDAO | 游戏操作记录（悔棋/求和） | ✅ |
| SocketSessionDAO | Socket 会话持久化 | ✅ |
| RoomDAO | 房间定时持久化 | ✅ |

---

## 5. 协议消息流

### 5.1 登录流程

```
客户端                          服务端
  |  LOGIN|user|pass            |
  |---------------------------->|
  |  LOGIN_OK|1|user|1000|token |
  |<----------------------------|
```

### 5.2 创建房间流程

```
客户端A                    服务端              客户端B
  | CREATE_ROOM|测试房间|pass  |
  |-------------------------->|
  | JOIN_OK|r01|测试房间|1/2|1 |
  |<--------------------------|
  |                           |
  | JOIN_ROOM|r01|pass        |
  |-------------------------->|
  | JOIN_OK|r01|测试房间|2/2|1 |
  |<--------------------------|
  | PLAYER_ENTER|B            |  广播
  |<--------------------------|------------------>|PLAYER_ENTER|A|
  | GAME_START|A|B            |  广播（满2人）
  |<--------------------------|------------------>|GAME_START|A|B|
```

### 5.3 对局流程

```
玩家A                    服务端                  玩家B
  | MOVE|7|7             |
  |---------------------->|
  | MOVE|7|7|1           |  广播
  |<----------------------|-------------------->|MOVE|7|7|1|
  |                       |
  | MOVE|7|8             |
  |---------------------->|
  | MOVE|7|8|2           |  广播
  |<----------------------|-------------------->|MOVE|7|8|2|
  |                       |
  | (一方五连)            |
  | GAME_OVER|winnerId|原因|
  |<----------------------|-------------------->|GAME_OVER|winnerId|原因|
  |                       |  保存记录到 DB
  |                       |  更新积分和排行榜
```

### 5.4 观战流程

```
客户端A（观战者）         服务端              房间内玩家
  | SPECTATE|r01         |
  |---------------------->|
  | JOIN_OK|r01|SPECTATOR|  广播给房间内
  |<----------------------|-------------------->|PLAYER_ENTER|A|SPECTATOR|
  |                       |
  | MOVE|7|7             |
  |<----------------------|-------------------->|MOVE|7|7|1|
  |                       |
  | CHAT|hello           |
  |---------------------->|
  | CHAT|A|hello         |  广播给所有人
  |<----------------------|-------------------->|CHAT|A|hello|
```

### 5.5 断线重连流程

```
客户端                    服务端                  Redis
  | RECONNECT|token      |
  |-------------------->|
  |                     | GET session:{token}
  |                     |<------------------|
  |                     | 返回 sessionData
  |                     |------------------>|
  | RECONNECT_OK|user    | 查找房间
  |<-------------------->|
  |                     | 恢复连接
  |                     | 广播 PLAYER_ENTER
```

### 5.6 快速匹配流程

```
玩家A                    服务端                  Redis
  | QUICK_MATCH         |
  |-------------------->|
  |                     | LPUSH match:queue "1:1000"
  |                     |<------------------|
  |                     | RPOP match:queue（检查队列长度）
  |                     |<------------------|
  |                     | 队列只有1人，等待
  | ERROR|等待对手...    |
  |<-------------------->|
                        |
玩家B                    |
  | QUICK_MATCH         |
  |-------------------->|
  |                     | LPUSH match:queue "2:980"
  |                     |<------------------|
  |                     | RPOP match:queue × 2
  |                     |<------------------|
  |                     | 创建房间，配对成功
  | JOIN_OK|r01|2/2     |
  |<-------------------->|
```

---

## 6. 数据库交互映射

| 业务操作 | 涉及表 | DAO 方法 |
|---------|--------|---------|
| 注册 | t_user | UserDAO.register() |
| 登录 | t_user | UserDAO.login() |
| 修改密码 | t_user | UserDAO.changePassword() |
| 创建房间 | t_room | RoomDAO.saveRoom()（定时） |
| 加入房间 | t_room | RoomDAO.updateRoom()（定时） |
| 观战加入 | t_room_watcher | RoomWatcherDAO.startWatching() |
| 观战离开 | t_room_watcher | RoomWatcherDAO.stopWatching() |
| 落子 | - | 内存 Game 对象 |
| 游戏操作 | t_game_action | GameActionDAO.saveAction() |
| 对局结束 | t_game_record | RecordDAO.saveRecord() |
| 更新积分 | t_user, t_rank | UserDAO.updateScore(), RankDAO.updateScore() |
| 排行榜 | t_rank, Redis | RankDAO.getRanking() |
| 聊天消息 | t_chat_message | ChatDAO.saveMessage() |
| 好友请求 | t_friend_request | FriendDAO.sendFriendRequest() |
| 好友关系 | t_friend | FriendDAO.acceptFriendRequest() |
| 棋局保存 | t_game_save | GameSaveDAO.saveGame() |
| Socket会话 | t_socket_session | SocketSessionDAO.saveSession() |

---

## 7. 实现优先级（已全部完成）

### 7.1 Phase 1：基础功能 ✅
1. handleLogin — 用户登录
2. handleRegister — 用户注册
3. handleCreateRoom / handleJoinRoom — 房间创建与加入
4. handleMove — 落子同步
5. handleLeaveRoom — 离开房间
6. Room.broadcast() — 消息广播
7. 五连判定算法 — Game.checkWin()

### 7.2 Phase 2：对局逻辑 ✅
8. handleChat — 聊天
9. handleSpectate — 观战加入
10. handleSurrender — 认输
11. 断线处理 — cleanup() + SessionManager
12. 对局记录保存 — RecordDAO.saveRecord()
13. 积分更新 — UserDAO.updateScore()

### 7.3 Phase 3：增强功能 ✅
14. 快速匹配队列 — Redis List
15. 排行榜查询 — Redis Sorted Set
16. 密码房间 — Room.password + 校验
17. 悔棋/求和 — Room.requestUndo/Draw + replyUndo/Draw
18. 断线重连 — SessionManager + Redis
19. 人机对战 — AIPlayer

### 7.4 Phase 4：进阶功能 ✅
20. 排位匹配 — ELO 算法 + Redis Sorted Set
21. 好友系统 — FriendDAO + 请求/接受/删除
22. 棋局保存 — GameSaveDAO
23. 聊天消息持久化 — ChatDAO
24. 观战记录 — RoomWatcherDAO
25. 游戏操作记录 — GameActionDAO
26. 排行榜物化表 — RankDAO + 定时刷新
27. 房间定时持久化 — RoomDAO + 定时任务
28. Socket会话持久化 — SocketSessionDAO
29. 退出登录 — handleLogout()

---

## 8. 定时任务

| 任务 | 间隔 | 执行类 | 功能 |
|------|------|--------|------|
| 会话清理 | 1分钟 | SessionManager | 清理过期会话 |
| 排行榜刷新 | 5分钟 | ServerMain | 刷新物化表 + Redis |
| 房间持久化 | 1分钟 | ServerMain | 同步内存房间到数据库 |
| 过期房间清理 | 1分钟 | ServerMain | 删除30分钟前结束的房间 |
| Redis连接保活 | 30秒 | RedisManager | 保持连接活跃 |

---

## 9. 注意事项

### 9.1 TCP Socket 通信机制
- 服务端使用 ServerSocket(8888) 监听，accept() 阻塞等待连接
- 每个客户端连接创建独立 ClientHandler 线程
- ClientHandler 内部使用 BufferedReader 读取、PrintWriter 写入
- 协议格式：COMMAND|param1|param2，以 | 分隔符拼接

### 9.2 线程安全
- Room 的 join/leave/broadcast 方法已用 synchronized 保护
- RoomManager.rooms 使用 ConcurrentHashMap
- SessionManager 使用 ConcurrentHashMap + Redis
- ClientHandler 每个连接独立实例，无需额外同步

### 9.3 资源清理
- ClientHandler.cleanup() 关闭 Socket，更新会话状态
- 断线时保留会话 5 分钟，支持重连
- 服务器关闭时调用 DBUtil.close() + RedisManager.close()

### 9.4 协议规范
- 所有消息使用 Protocol.build(parts) 构建
- 解析使用 Protocol.parse(line)，注意 -1 参数保留尾部空段
- 响应消息前缀命令字，客户端根据命令字分发处理

### 9.5 密码策略
- 注册时使用 BCrypt.hashpw(password, BCrypt.gensalt())
- 登录时使用 BCrypt.checkpw(password, hash) 验证

### 9.6 断线策略
- 客户端断开后，保留会话 5 分钟（SessionManager + Redis）
- 房间状态保持，支持重连恢复
- 超时后自动清理会话，标记用户离线

### 9.7 观战功能规则
- 观战者只能接收广播，不能发送 MOVE/SURRENDER/REQUEST_UNDO/REQUEST_DRAW
- 观战者通过 broadcastAll() 接收消息
- 观战者不计入房间玩家数上限（2人），可无限加入
- 观战者可以聊天（CHAT 消息同样广播给所有玩家和观战者）
- 观战者离开时调用 leaveObserver() 并记录离开时间

### 9.8 匹配规则
- 快速匹配：先进先出，两人自动配对
- 排位匹配：ELO 算法，积分差距阈值 200，优先匹配差距最小的对手
- 匹配队列使用 Redis，支持跨服务器扩展

---

## 10. 依赖检查

### 10.1 pom.xml 已有依赖
| 依赖 | 用途 | 状态 |
|------|------|------|
| gomatch-common | 协议常量 | ✅ |
| HikariCP | 连接池 | ✅ |
| mysql-connector-j | MySQL 驱动 | ✅ |
| gson | JSON 序列化 | ✅ |
| logback-classic | 日志 | ✅ |
| lettuce-core | Redis 客户端 | ✅ |
| jbcrypt | BCrypt 密码加密 | ✅ |

---

## 11. 代码量统计

| 文件 | 当前行数 | 说明 |
|------|---------|------|
| ClientHandler.java | ~1388 | 协议分发 + 所有业务处理方法 |
| Room.java | ~300 | 房间状态机 + 游戏逻辑 |
| RoomManager.java | ~200 | 房间管理 + 匹配逻辑 |
| Game.java | ~150 | 棋盘状态 + 胜负判定 |
| AIPlayer.java | ~100 | AI 启发式评分 |
| SessionManager.java | ~200 | 会话管理 + Redis |
| RedisManager.java | ~400 | Redis 操作封装 |
| UserDAO.java | ~150 | 用户数据访问 |
| RecordDAO.java | ~100 | 战绩数据访问 |
| RankDAO.java | ~150 | 排行榜数据访问 |
| ChatDAO.java | ~80 | 聊天数据访问 |
| FriendDAO.java | ~150 | 好友数据访问 |
| RoomWatcherDAO.java | ~80 | 观战记录 |
| GameSaveDAO.java | ~100 | 棋局保存 |
| GameActionDAO.java | ~80 | 游戏操作记录 |
| SocketSessionDAO.java | ~120 | Socket会话持久化 |
| RoomDAO.java | ~100 | 房间持久化 |
| **合计** | **~3200** | **全部功能已实现** |

---

**文档版本**：v2.0
**最后更新**：2026-07-06
