# common 模块文档

> **模块标识**：`com.gomatch:common`  
> **职责**：定义客户端与服务端共用的协议常量与 JSON 消息模型。

---

## 1. 模块概览

`common` 是 Gomatch 的通信基础模块，被 `client` 和 `server` 同时依赖。当前主协议是 JSON 行消息：每条 TCP 消息占一行，由 `Message` 负责序列化和解析，由 `Protocol` 统一维护命令字常量。

```
common/
└── src/main/java/com/gomatch/common/
    ├── Message.java     # JSON 消息模型与 builder
    └── Protocol.java    # 客户端/服务端共享命令常量
```

**安装命令**（首次或 common 变更后必须执行）：

```powershell
mvn install -pl common -am
```

---

## 2. 通信协议设计

### 2.1 报文格式

```json
{"cmd":"COMMAND","data":{"key":"value"}}
```

| 要素 | 说明 |
|------|------|
| 消息格式 | JSON 对象，必须包含 `cmd`，`data` 可为空 |
| 传输层 | TCP 长连接，`BufferedReader.readLine()` 逐行读取 |
| 序列化类 | `com.gomatch.common.Message` |
| 命令常量 | `com.gomatch.common.Protocol` |
| 数据字段 | 使用 `org.json.JSONObject` 承载 |

`Message.toJson()` 在 `data` 为空时会只输出 `cmd` 字段；接收端通过 `Message.fromJson(line)` 解析。

### 2.2 示例报文

```json
{"cmd":"LOGIN","data":{"username":"alice","password":"[redacted-credential]"}}
```

```json
{"cmd":"LOGIN_OK","data":{"userId":1,"username":"alice","score":1500,"wins":10,"losses":3,"draws":1,"role":"PLAYER"}}
```

```json
{"cmd":"MOVE","data":{"x":7,"y":8}}
```

```json
{"cmd":"GAME_OVER","data":{"winnerId":1,"winnerColor":"black","reason":"five"}}
```

---

## 3. Message 类详解

**全限定名**：`com.gomatch.common.Message`  
**文件路径**：[Message.java](../common/src/main/java/com/gomatch/common/Message.java)  
**类型**：不可变消息对象，内部持有 `cmd` 与 `JSONObject data`。

### 3.1 创建消息

```java
Message login = Message.of(
    Protocol.LOGIN,
    "username", "alice",
    "password", "[redacted-credential]"
);
```

```java
Message gameStart = Message.builder(Protocol.GAME_START)
    .set("blackUserId", 1)
    .set("whiteUserId", 2)
    .set("blackUsername", "alice")
    .set("whiteUsername", "bob")
    .build();
```

```java
Message surrender = Message.cmdOnly(Protocol.SURRENDER);
```

### 3.2 序列化与解析

```java
String json = login.toJson();
Message parsed = Message.fromJson(json);
String cmd = parsed.getCmd();
String username = parsed.getString("username");
```

### 3.3 读取字段

| 方法 | 用途 |
|------|------|
| `getCmd()` | 获取命令字 |
| `getData()` | 获取完整 `JSONObject` |
| `getString(key)` | 读取字符串，默认空字符串 |
| `getInt(key)` | 读取整数，默认 `0` |
| `getLong(key)` | 读取长整数，默认 `0` |
| `getBoolean(key)` | 读取布尔值，默认 `false` |
| `optBoolean(key, defaultValue)` | 读取布尔值并指定默认值 |
| `optLong(key, defaultValue)` | 读取长整数并指定默认值 |
| `has(key)` | 判断字段是否存在 |
| `hasData()` | 判断 `data` 是否有内容 |

---

## 4. Protocol 类详解

**全限定名**：`com.gomatch.common.Protocol`  
**文件路径**：[Protocol.java](../common/src/main/java/com/gomatch/common/Protocol.java)  
**类型**：协议常量类，仅维护 `public static final String` 命令字。

### 4.1 客户端 -> 服务端指令

| 常量 | 值 | 用途 |
|------|----|------|
| `LOGIN` | `LOGIN` | 登录 |
| `REGISTER` | `REGISTER` | 注册 |
| `CREATE_ROOM` | `CREATE_ROOM` | 创建普通房间 |
| `JOIN_ROOM` | `JOIN_ROOM` | 加入房间 |
| `LEAVE_ROOM` | `LEAVE_ROOM` | 离开房间 |
| `READY` | `READY` | 房间内准备 |
| `MOVE` | `MOVE` | 落子 |
| `CHAT` | `CHAT` | 发送聊天 |
| `SURRENDER` | `SURRENDER` | 认输 |
| `REQUEST_UNDO` / `REPLY_UNDO` | 同名值 | 悔棋申请与回应 |
| `REQUEST_DRAW` / `REPLY_DRAW` | 同名值 | 求和申请与回应 |
| `REQUEST_REMATCH` / `REPLY_REMATCH` | 同名值 | 再来一局申请与回应 |
| `CHANGE_PWD` | `CHANGE_PASSWORD` | 修改密码 |
| `ROOM_LIST` | `ROOM_LIST` | 请求房间列表 |
| `QUICK_MATCH` / `QUICK_READY` / `CANCEL_MATCH` | 同名值 | 快速匹配、快速匹配准备、取消匹配 |
| `RANKED_MATCH` / `RANKED_READY` | 同名值 | 排位匹配与排位准备 |
| `SPECTATE` / `LEAVE_SPECTATE` | 同名值 | 观战与离开观战 |
| `ADD_FRIEND` / `ACCEPT_FRIEND` / `REJECT_FRIEND` / `DELETE_FRIEND` | 同名值 | 好友操作 |
| `FRIEND_LIST` / `FRIEND_REQUESTS` | 同名值 | 好友列表与好友请求 |
| `QUERY_RECORD` | `QUERY_RECORD` | 查询对战记录 |
| `RANKING` | `RANKING` | 请求排行榜（可携带 type 区分全服/好友分榜） |
| `SAVE_GAME` / `LOAD_GAME` / `DELETE_SAVE` / `SAVE_LIST` | 同名值 | 棋局存档操作 |
| `CHAT_HISTORY` / `WATCHER_HISTORY` | 同名值 | 聊天历史与观战历史 |
| `CREATE_AI_ROOM` / `AI_MATCH` | 同名值 | 人机对战 |
| `RECONNECT` | `RECONNECT` | 断线重连 |
| `HEARTBEAT` | `HEARTBEAT` | 心跳 |
| `UPLOAD_AVATAR` / `GET_AVATAR` / `DELETE_AVATAR` | 同名值 | 头像数据 |
| `BAN_USER` / `UNBAN_USER` / `FORCE_CLOSE_ROOM` / `USER_LIST` / `ADMIN_STATS` / `UPDATE_USER_ROLE` | 同名值 | 管理端操作 |
| `LOGOUT` | `LOGOUT` | 退出登录 |
| `FORGOT_PASSWORD` / `VERIFY_FORGOT_CODE` / `FORGOT_PWD_RESET` | 同名值 | 忘记密码：发送验证码 / 校验验证码 / 重置密码 |

### 4.2 服务端 -> 客户端响应

| 常量 | 值 | 用途 |
|------|----|------|
| `LOGIN_OK` / `LOGIN_FAIL` | 同名值 | 登录结果 |
| `REGISTER_OK` | `REGISTER_OK` | 注册成功 |
| `JOIN_OK` / `JOIN_FAIL` | 同名值 | 加入房间结果 |
| `PLAYER_ENTER` / `PLAYER_LEAVE` | 同名值 | 玩家进入/离开房间通知 |
| `GAME_START` | `GAME_START` | 游戏开始，携带双方用户与积分信息 |
| `MOVE_SYNC` | `MOVE` | 服务端同步落子，常量值与 `MOVE` 相同 |
| `GAME_OVER` | `GAME_OVER` | 游戏结束 |
| `CHAT_MSG` | `CHAT` | 聊天广播，常量值与 `CHAT` 相同 |
| `PWD_CHANGED` | `PWD_CHANGED` | 密码修改成功 |
| `RANKING_LIST` | `RANKING_LIST` | 排行榜数据 |
| `RECORD_LIST` / `GAME_RECORDS` / `GAME_RECORDS_LIST` | 同名值 | 对战记录数据 |
| `ROOM_CREATED` | `ROOM_CREATED` | 房间创建成功 |
| `USER_SCORE_UPDATE` | `USER_SCORE_UPDATE` | 用户积分更新 |
| `TURN_TIMEOUT` | `TURN_TIMEOUT` | 回合超时 |
| `MATCH_TIMEOUT` / `QUICK_MATCH_FOUND` / `RANKED_MATCH_FOUND` | 同名值 | 匹配状态：匹配超时 / 快速匹配成功 / 排位匹配成功 |
| `HEARTBEAT_OK` | `HEARTBEAT_OK` | 心跳响应 |
| `AVATAR_DATA` / `AVATAR_UPLOADED` / `AVATAR_DELETED` | 同名值 | 头像响应 |
| `ERROR` | `ERROR` | 通用错误响应 |
| `DISCONNECT` | `DISCONNECT` | 服务端主动断开通知 |
| `LOGOUT_OK` | `LOGOUT_OK` | 退出登录成功 |
| `REMATCH_REQUESTED` / `REMATCH_ACCEPTED` / `REMATCH_REJECTED` | 同名值 | 再来一局：收到请求 / 对方同意 / 对方拒绝 |
| `FORGOT_PWD_CODE` / `FORGOT_PWD_VERIFY_OK` / `FORGOT_PWD_OK` / `FORGOT_PWD_FAIL` | 同名值 | 忘记密码：发送验证码 / 验证通过 / 重置成功 / 操作失败 |

---

## 5. 使用示例

### 5.1 客户端发送命令

```java
ClientMain.getConnector().send(
    Message.of(Protocol.MOVE, "x", x, "y", y)
);
```

### 5.2 服务端构建响应

```java
send(Message.builder(Protocol.LOGIN_OK)
    .set("userId", user.getUserId())
    .set("username", user.getUsername())
    .set("score", user.getScore())
    .set("role", user.getRole())
    .build());
```

### 5.3 消息解析分发

```java
Message msg = Message.fromJson(line);
switch (msg.getCmd()) {
    case Protocol.LOGIN -> handleLogin(msg);
    case Protocol.MOVE -> handleMove(msg);
    default -> send(Message.of(Protocol.ERROR, "message", "未知命令"));
}
```

---

## 6. 依赖关系

```
client  --依赖--> common
server  --依赖--> common
```

`common` 依赖 `org.json`，由 Maven 统一管理。

---

## 7. 注意事项

> [!WARNING]
> 当前协议不再使用 `|` 分隔文本作为主路径。新增功能必须使用 `Message.of(...)` 或 `Message.builder(...)` 构建 JSON 消息，避免聊天内容、密码、昵称等字段因特殊字符破坏解析。

> [!NOTE]
> `MOVE_SYNC` 与 `CHAT_MSG` 的常量值分别复用 `MOVE` 与 `CHAT`。客户端和服务端通过接收上下文区分“请求”和“广播”语义。

> [!NOTE]
> 断线重连相关通知 `PLAYER_DISCONNECTED`、`PLAYER_RECONNECTED`、`RECONNECT_ATTEMPT`、`RECONNECT_FAILED` 及重连结果 `RECONNECT_OK`、`RECONNECT_FAIL` 目前以字面量形式在 `Room`/`ClientHandler` 中使用，尚未收入 `Protocol` 常量类。

> [!TIP]
> common 变更后要先执行 `mvn install -pl common -am`，再编译 `client` 或 `server`。
