# common 模块文档

> **模块标识**：`com.gomatch:common`  
> **职责**：定义客户端与服务端共用的通信协议常量与编解码工具。

---

## 1. 模块概览

`common` 是 Gomatch 的通信基础模块，被 `client` 和 `server` 同时依赖。  
它只包含一个类——`Protocol.java`，通过 Maven 安装到本地仓库后被两端引用。

```
common/
└── src/main/java/com/gomatch/common/
    └── Protocol.java
```

**安装命令**（首次或 common 变更后必须执行）：

```powershell
mvn install -pl common -am
```

---

## 2. 通信协议设计

### 2.1 报文格式

所有消息以 **`|`（竖线）** 为分隔符，采用纯文本 TCP 流传输，每条消息占一行：

```
COMMAND|param1|param2|...
```

| 要素 | 说明 |
|------|------|
| 分隔符 | `|`（`Protocol.SEPARATOR`） |
| 传输层 | TCP 长连接，`BufferedReader.readLine()` 逐行读取 |
| 编码 | UTF-8（平台默认） |

### 2.2 示例报文

```
# 客户端发起登录
LOGIN|alice|[redacted-credential]

# 服务端登录成功响应（包含用户详情）
LOGIN_OK|1|alice|1500|10|3|1|1

# 落子同步（行 7 列 8）
MOVE|7|8

# 对局结束
GAME_OVER|black|alice
```

---

## 3. Protocol 类详解

**全限定名**：`com.gomatch.common.Protocol`  
**文件路径**：[Protocol.java](../common/src/main/java/com/gomatch/common/Protocol.java)  
**类型**：工具类（仅含 `public static final` 常量和静态方法）

### 3.1 客户端 → 服务端指令常量

| 常量名 | 值 | 用途 |
|--------|----|------|
| `LOGIN` | `"LOGIN"` | 用户登录，附带用户名和密码 |
| `REGISTER` | `"REGISTER"` | 用户注册，附带用户名和密码 |
| `CREATE_ROOM` | `"CREATE_ROOM"` | 创建普通对战房间 |
| `JOIN_ROOM` | `"JOIN_ROOM"` | 加入指定房间（含密码可选） |
| `LEAVE_ROOM` | `"LEAVE_ROOM"` | 离开当前房间 |
| `READY` | `"READY"` | 玩家准备就绪 |
| `MOVE` | `"MOVE"` | 落子，附带行列坐标 |
| `CHAT` | `"CHAT"` | 发送房间聊天消息 |
| `SURRENDER` | `"SURRENDER"` | 认输 |
| `REQUEST_UNDO` | `"REQUEST_UNDO"` | 申请悔棋 |
| `REQUEST_DRAW` | `"REQUEST_DRAW"` | 申请求和 |
| `REPLY_UNDO` | `"REPLY_UNDO"` | 回应悔棋请求（accept/reject） |
| `REPLY_DRAW` | `"REPLY_DRAW"` | 回应求和请求（accept/reject） |
| `CHANGE_PWD` | `"CHANGE_PWD"` | 修改密码 |
| `RANKING` | `"RANKING"` | 请求排行榜数据 |
| `ROOM_LIST` | `"ROOM_LIST"` | 请求当前房间列表 |
| `QUICK_MATCH` | `"QUICK_MATCH"` | 加入快速匹配队列 |
| `RANKED_MATCH` | `"RANKED_MATCH"` | 加入排位匹配队列 |
| `SPECTATE` | `"SPECTATE"` | 观战指定房间 |
| `ADD_FRIEND` | `"ADD_FRIEND"` | 发起好友申请 |
| `ACCEPT_FRIEND` | `"ACCEPT_FRIEND"` | 接受好友申请 |
| `REJECT_FRIEND` | `"REJECT_FRIEND"` | 拒绝好友申请 |
| `DELETE_FRIEND` | `"DELETE_FRIEND"` | 删除好友 |
| `FRIEND_LIST` | `"FRIEND_LIST"` | 获取好友列表 |
| `FRIEND_REQUESTS` | `"FRIEND_REQUESTS"` | 获取收到的好友申请 |
| `SAVE_GAME` | `"SAVE_GAME"` | 保存当前棋局 |
| `LOAD_GAME` | `"LOAD_GAME"` | 加载已保存棋局 |
| `DELETE_SAVE` | `"DELETE_SAVE"` | 删除存档 |
| `SAVE_LIST` | `"SAVE_LIST"` | 获取存档列表 |
| `CHAT_HISTORY` | `"CHAT_HISTORY"` | 获取房间聊天历史 |
| `WATCHER_HISTORY` | `"WATCHER_HISTORY"` | 获取观战历史记录 |
| `LOGOUT` | `"LOGOUT"` | 退出登录 |

### 3.2 服务端 → 客户端响应常量

| 常量名 | 值 | 用途 |
|--------|----|------|
| `LOGIN_OK` | `"LOGIN_OK"` | 登录成功，附带 userId、username、score 等 |
| `LOGIN_FAIL` | `"LOGIN_FAIL"` | 登录失败 |
| `REGISTER_OK` | `"REGISTER_OK"` | 注册成功 |
| `JOIN_OK` | `"JOIN_OK"` | 加入房间成功 |
| `JOIN_FAIL` | `"JOIN_FAIL"` | 加入房间失败（已满/密码错误等） |
| `PLAYER_ENTER` | `"PLAYER_ENTER"` | 其他玩家进入房间通知 |
| `PLAYER_LEAVE` | `"PLAYER_LEAVE"` | 其他玩家离开房间通知 |
| `GAME_START` | `"GAME_START"` | 游戏开始，附带双方信息和先手颜色 |
| `MOVE_SYNC` | `"MOVE"` | 落子同步（与客户端发送指令同值） |
| `GAME_OVER` | `"GAME_OVER"` | 游戏结束，附带获胜方信息 |
| `CHAT_MSG` | `"CHAT"` | 广播聊天消息 |
| `PWD_CHANGED` | `"PWD_CHANGED"` | 密码修改成功通知 |
| `RANKING_LIST` | `"RANKING_LIST"` | 返回排行榜数据 |
| `ERROR` | `"ERROR"` | 通用错误响应，附带错误描述 |
| `DISCONNECT` | `"DISCONNECT"` | 服务器主动断开连接通知 |
| `ROOM_CREATED` | `"ROOM_CREATED"` | 房间创建成功，附带 roomId |
| `GAME_RECORDS` | `"GAME_RECORDS"` | 返回对战记录（单条） |
| `GAME_RECORDS_LIST` | `"GAME_RECORDS_LIST"` | 返回对战记录列表 |
| `LOGOUT_OK` | `"LOGOUT_OK"` | 退出登录成功 |

---

## 4. 静态工具方法

### `build(String... parts) → String`

将多个字符串用 `|` 拼接成协议报文：

```java
Protocol.build("LOGIN", "alice", "[redacted-credential]")
// → "LOGIN|alice|[redacted-credential]"
```

### `parse(String msg) → String[]`

将收到的协议字符串拆分为数组，支持空值字段（`-1` 参数保留末尾空串）：

```java
String[] parts = Protocol.parse("LOGIN_OK|1|alice|1500|10|3|1");
// parts[0] = "LOGIN_OK"
// parts[1] = "1"         (userId)
// parts[2] = "alice"     (username)
// parts[3] = "1500"      (score)
```

---

## 5. 使用示例

### 服务端构建响应

```java
// ClientHandler 内部
send(Protocol.build(Protocol.LOGIN_OK, 
    String.valueOf(userId),
    username,
    String.valueOf(score)));
```

### 客户端发送命令

```java
// ServerConnector.send() 内部调用 Protocol.build
connector.send(Protocol.MOVE, String.valueOf(row), String.valueOf(col));
```

### 消息解析分发

```java
// ServerConnector 接收线程
String[] parts = Protocol.parse(line);
String cmd = parts[0];
String[] args = Arrays.copyOfRange(parts, 1, parts.length);
```

---

## 6. 依赖关系

```
client  ──依赖──▶  common
server  ──依赖──▶  common
```

`common` 本身无任何外部依赖，仅使用 Java 标准库。

---

## 7. 注意事项

> [!WARNING]
> `|` 在参数值中是非法字符。如传递含特殊字符的内容（如密码、聊天文本），需在应用层进行转义或 Base64 编码。

> [!NOTE]
> `MOVE_SYNC` 常量值为 `"MOVE"`，与 `MOVE` 相同。服务端向客户端同步落子使用与客户端发送相同的命令字，由上下文（接收方 vs 发送方）区分语义。
