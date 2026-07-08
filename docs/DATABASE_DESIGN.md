# GoMatch 五子棋项目 - 数据库设计文档

## 1. 设计概述

本文档详细描述 GoMatch 五子棋项目的数据库设计，包括：
- 核心业务表设计
- Socket会话与消息持久化
- 棋局保存与恢复机制（通过工程文件URI实现）
- Repository接口规范
- 数据模型定义

### 1.1 部署架构

```mermaid
flowchart LR
    subgraph 单机服务器(Server)
        A[游戏服务端程序]
        B[MySQL数据库]
        C[Redis缓存]
        D[文件存储]
    end
    
    subgraph 客户端(Client)
        E[玩家1电脑]
        F[玩家2电脑]
    end
    
    E --> A
    F --> A
    A --> B
    A --> C
    A --> D
```

**说明**：游戏服务端程序、MySQL数据库、Redis缓存、文件存储部署在**同一台服务器**上。

### 1.2 编码规范

**全链路UTF-8编码**，确保中文正常显示：

| 层级 | 配置项 | 值 |
|------|--------|-----|
| 数据库 | character_set_server | utf8mb4 |
| 数据库 | collation_server | utf8mb4_unicode_ci |
| 数据库 | character_set_database | utf8mb4 |
| 表 | DEFAULT CHARSET | utf8mb4 |
| 表 | COLLATE | utf8mb4_unicode_ci |
| 连接 | JDBC URL参数 | useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci |
| Java源码 | 文件编码 | UTF-8 |
| 日志文件 | 编码 | UTF-8 |

**为什么用utf8mb4而不是utf8？**
- MySQL的`utf8`只支持3字节Unicode字符，无法存储emoji表情和部分生僻汉字
- `utf8mb4`支持4字节Unicode字符，完整兼容所有Unicode字符

## 2. 数据库表设计

### 2.1 表关系图

```mermaid
erDiagram
    t_user ||--o{ t_game_record : "参与"
    t_user ||--o| t_rank : "拥有"
    t_user ||--o{ t_game_save : "保存"
    t_user ||--o{ t_socket_session : "连接"
    t_user ||--o{ t_chat_message : "发送"
    t_user ||--o{ t_room_watcher : "观战"
    t_user ||--o{ t_game_action : "操作"
    t_user ||--o{ t_friend : "好友"
    t_user ||--o{ t_friend_request : "好友请求"
    t_game_record ||--o| t_game_save : "关联"
    t_game_record ||--o{ t_game_action : "关联"
    t_room ||--o{ t_user : "成员"
    t_room ||--o{ t_chat_message : "消息"
    t_room ||--o{ t_room_watcher : "观战者"
    
    t_user {
        bigint user_id PK "用户ID"
        varchar username UK "用户名"
        varchar password "密码"
        varchar email UK "邮箱"
        varchar avatar "头像"
        varchar role "角色"
        varchar status "状态"
        int score "积分"
        int win_count "胜场数"
        int lose_count "负场数"
        int draw_count "平局数"
        int ai_win_count "AI胜场"
        int ai_lose_count "AI负场"
        int version "版本号(乐观锁)"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
    }
    
    t_game_record {
        bigint record_id PK "记录ID"
        bigint player1_id FK "玩家1ID"
        bigint player2_id FK "玩家2ID"
        varchar game_type "游戏类型"
        varchar result "结果"
        text board_data "棋谱数据(JSON)"
        int duration "时长(秒)"
        int move_count "落子数"
        datetime create_time "创建时间"
    }
    
    t_rank {
        bigint rank_id PK "排名ID"
        bigint user_id FK UK "用户ID"
        int rank "排名"
        int score "积分"
        decimal win_rate "胜率"
        decimal ai_win_rate "AI胜率"
        int version "版本号(乐观锁)"
        datetime update_time "更新时间"
    }
    
    t_game_save {
        bigint save_id PK "保存ID"
        bigint user_id FK "用户ID"
        bigint record_id FK "关联对局ID"
        varchar file_uri UK "棋局文件URI"
        varchar save_name "保存名称"
        text board_data "棋盘状态(JSON)"
        int current_player "当前执子方"
        varchar game_status "游戏状态"
        varchar checksum "文件校验和"
        int file_size "文件大小(字节)"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
    }
    
    t_socket_session {
        bigint session_id PK "会话ID"
        bigint user_id FK "用户ID"
        varchar session_key UK "会话密钥"
        varchar client_ip "客户端IP"
        varchar connection_id "连接ID"
        varchar status "连接状态"
        bigint room_id FK "当前房间ID"
        datetime last_heartbeat "最后活跃时间"
        datetime disconnect_time "断开时间"
        varchar disconnect_reason "断开原因"
        int reconnect_attempts "重连尝试次数"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
    }
    
    t_chat_message {
        bigint message_id PK "消息ID"
        bigint room_id FK "房间ID"
        bigint user_id FK "发送者ID"
        varchar content "消息内容"
        varchar message_type "消息类型"
        datetime create_time "创建时间"
    }
    
    t_room {
        bigint room_id PK "房间ID"
        varchar room_name "房间名称"
        varchar password "房间密码"
        int max_players "最大人数"
        int current_players "当前人数"
        int watcher_count "观战人数"
        varchar status "房间状态"
        bigint host_id FK "房主ID"
        boolean allow_watch "允许观战"
        int version "版本号(乐观锁)"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
    }
    
    t_room_watcher {
        bigint watcher_id PK "观战记录ID"
        bigint room_id FK "房间ID"
        bigint user_id FK "观战者ID"
        datetime join_time "加入时间"
        datetime leave_time "离开时间"
    }
    
    t_game_action {
        bigint action_id PK "操作ID"
        bigint record_id FK "对局ID"
        bigint user_id FK "操作用户ID"
        varchar action_type "操作类型"
        varchar action_status "操作状态"
        int move_number "操作时落子数"
        varchar reject_reason "拒绝原因"
        datetime create_time "创建时间"
    }
    
    t_friend {
        bigint friend_id PK "好友关系ID"
        bigint user_id FK "用户ID"
        bigint friend_user_id FK "好友ID"
        varchar relation_type "关系类型"
        datetime create_time "创建时间"
    }
    
    t_friend_request {
        bigint request_id PK "请求ID"
        bigint from_user_id FK "发起者ID"
        bigint to_user_id FK "接收者ID"
        varchar status "请求状态"
        datetime create_time "创建时间"
        datetime respond_time "响应时间"
    }
```

### 2.2 表详细设计

#### 2.2.1 t_user（用户表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| user_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 用户唯一标识 |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(100) | NOT NULL | 密码(明文) |
| email | VARCHAR(100) | UNIQUE | 邮箱 |
| avatar | VARCHAR(255) | NULL | 头像URL |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'PLAYER' | 角色: PLAYER(玩家)/ADMIN(管理员) |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | 状态: ACTIVE(活跃)/BANNED(封禁)/INACTIVE(未激活) |
| score | INT | DEFAULT 1000 | 积分(初始1000) |
| win_count | INT | DEFAULT 0 | 胜场数 |
| lose_count | INT | DEFAULT 0 | 负场数 |
| draw_count | INT | DEFAULT 0 | 平局数 |
| ai_win_count | INT | DEFAULT 0 | AI对战胜场数 |
| ai_lose_count | INT | DEFAULT 0 | AI对战负场数 |
| version | INT | DEFAULT 0 | 版本号(乐观锁) |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引设计**：
| 索引名 | 字段 | 类型 |
|--------|------|------|
| PRIMARY | user_id | 主键 |
| uk_username | username | 唯一索引 |
| uk_email | email | 唯一索引 |
| idx_role | role | 普通索引 |
| idx_status | status | 普通索引 |
| idx_score | score | 普通索引 |
| idx_role_status | role, status | 联合索引 |

#### 2.2.2 t_game_record（游戏记录表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| record_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录唯一标识 |
| player1_id | BIGINT | NOT NULL, FK | 玩家1ID |
| player2_id | BIGINT | NOT NULL, FK | 玩家2ID |
| game_type | VARCHAR(20) | NOT NULL, DEFAULT 'PVP' | 游戏类型: PVP(双人)/PVE(人机) |
| winner_id | BIGINT | FK, NULL | 胜者ID(平局为NULL) |
| result | VARCHAR(10) | NOT NULL | 结果: win/lose/draw |
| board_data | TEXT | NOT NULL | 完整棋谱(JSON) |
| duration | INT | NULL | 对局时长(秒) |
| move_count | INT | DEFAULT 0 | 落子总数 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引设计**：
| 索引名 | 字段 | 类型 |
|--------|------|------|
| PRIMARY | record_id | 主键 |
| idx_player1 | player1_id | 普通索引 |
| idx_player2 | player2_id | 普通索引 |
| idx_game_type | game_type | 普通索引 |
| idx_create_time | create_time | 普通索引 |

#### 2.2.3 t_rank（排行榜表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| rank_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 排名ID |
| user_id | BIGINT | NOT NULL, UNIQUE, FK | 用户ID |
| rank | INT | DEFAULT 0 | 当前排名 |
| score | INT | DEFAULT 0 | 当前积分 |
| win_rate | DECIMAL(5,2) | DEFAULT 0.00 | 胜率 |
| ai_win_rate | DECIMAL(5,2) | DEFAULT 0.00 | AI对战胜率 |
| version | INT | DEFAULT 0 | 版本号(乐观锁) |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引设计**：
| 索引名 | 字段 | 类型 |
|--------|------|------|
| PRIMARY | rank_id | 主键 |
| uk_user_id | user_id | 唯一索引 |
| idx_rank | rank | 普通索引 |
| idx_score | score | 普通索引 |

#### 2.2.4 t_game_save（棋局保存表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| save_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 保存记录ID |
| user_id | BIGINT | NOT NULL, FK | 保存用户ID |
| record_id | BIGINT | FK, NULL | 关联对局记录ID(正在进行的对局为NULL) |
| file_uri | VARCHAR(500) | NOT NULL, UNIQUE | 棋局工程文件URI |
| save_name | VARCHAR(100) | NULL | 保存名称(便于识别) |
| board_data | TEXT | NOT NULL | 当前棋盘状态(JSON) |
| current_player | INT | NOT NULL | 当前执子方(1:黑棋, 2:白棋) |
| game_status | VARCHAR(20) | NOT NULL | 游戏状态: playing/paused/finished |
| checksum | VARCHAR(64) | NULL | 文件校验和(SHA-256) |
| file_size | INT | DEFAULT 0 | 文件大小(字节) |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引设计**：
| 索引名 | 字段 | 类型 |
|--------|------|------|
| PRIMARY | save_id | 主键 |
| uk_file_uri | file_uri | 唯一索引 |
| idx_user_id | user_id | 普通索引 |
| idx_record_id | record_id | 普通索引 |

#### 2.2.5 t_socket_session（Socket会话表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| session_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 会话ID |
| user_id | BIGINT | NOT NULL, FK | 用户ID |
| session_key | VARCHAR(64) | NOT NULL, UNIQUE | 会话密钥(UUID) |
| client_ip | VARCHAR(50) | NULL | 客户端IP地址 |
| connection_id | VARCHAR(64) | NULL | Socket连接唯一标识 |
| room_id | BIGINT | FK, NULL | 当前所在房间ID |
| status | VARCHAR(20) | NOT NULL | 连接状态: connected/disconnected/reconnecting |
| last_heartbeat | DATETIME | NULL | 最后活跃时间 |
| disconnect_time | DATETIME | NULL | 断开连接时间 |
| disconnect_reason | VARCHAR(50) | NULL | 断开原因: timeout/network/normal |
| reconnect_attempts | INT | DEFAULT 0 | 重连尝试次数 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引设计**：
| 索引名 | 字段 | 类型 |
|--------|------|------|
| PRIMARY | session_id | 主键 |
| uk_session_key | session_key | 唯一索引 |
| idx_user_id | user_id | 普通索引 |
| idx_status | status | 普通索引 |
| idx_last_heartbeat | last_heartbeat | 普通索引 |
| idx_room_id | room_id | 普通索引 |
| idx_room_status | room_id, status | 联合索引 |

**设计说明**：
- `session_key`：用于断线重连时验证身份，客户端保存该密钥
- `connection_id`：Socket连接的唯一标识，服务端用于管理连接
- `last_heartbeat`：每次查询会话时自动更新，用于跟踪用户活跃状态和断线检测
- `status`：支持状态查询，便于管理在线用户

#### 2.2.6 t_chat_message（聊天消息表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| message_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 消息ID |
| room_id | BIGINT | NOT NULL, FK | 房间ID |
| user_id | BIGINT | NOT NULL, FK | 发送者ID |
| content | VARCHAR(500) | NOT NULL | 消息内容 |
| message_type | VARCHAR(20) | NOT NULL | 消息类型: text/system/game |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引设计**：
| 索引名 | 字段 | 类型 |
|--------|------|------|
| PRIMARY | message_id | 主键 |
| idx_room_id | room_id | 普通索引 |
| idx_user_id | user_id | 普通索引 |
| idx_create_time | create_time | 普通索引 |

**设计说明**：
- `message_type`：区分普通消息、系统消息、游戏消息
- `room_id`：关联房间，支持房间内消息历史查询
- 消息内容限制500字符，避免过大数据

#### 2.2.7 t_room（房间表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| room_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 房间ID |
| room_name | VARCHAR(100) | NOT NULL | 房间名称 |
| password | VARCHAR(100) | NULL | 房间密码(可选) |
| max_players | INT | DEFAULT 2 | 最大人数 |
| current_players | INT | DEFAULT 0 | 当前人数 |
| watcher_count | INT | DEFAULT 0 | 观战人数 |
| status | VARCHAR(20) | NOT NULL | 房间状态: waiting/playing/finished |
| host_id | BIGINT | NOT NULL, FK | 房主ID |
| allow_watch | BOOLEAN | DEFAULT TRUE | 是否允许观战 |
| version | INT | DEFAULT 0 | 版本号(乐观锁) |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引设计**：
| 索引名 | 字段 | 类型 |
|--------|------|------|
| PRIMARY | room_id | 主键 |
| idx_status | status | 普通索引 |
| idx_host_id | host_id | 普通索引 |
| idx_allow_watch | allow_watch | 普通索引 |
| idx_status_players | status, current_players | 联合索引 |
| idx_watch_status | allow_watch, status | 联合索引 |

**设计说明**：
- 房间主要在内存中管理，数据库作为持久化备份
- `status`：支持房间列表查询（只显示waiting状态）
- `password`：支持密码房间
- `allow_watch`：控制是否允许其他玩家观战

#### 2.2.8 t_room_watcher（房间观战表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| watcher_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 观战记录ID |
| room_id | BIGINT | NOT NULL, FK | 房间ID |
| user_id | BIGINT | NOT NULL, FK | 观战者ID |
| join_time | DATETIME | NOT NULL | 加入观战时间 |
| leave_time | DATETIME | NULL | 离开观战时间 |

**索引设计**：
| 索引名 | 字段 | 类型 |
|--------|------|------|
| PRIMARY | watcher_id | 主键 |
| idx_room_id | room_id | 普通索引 |
| idx_user_id | user_id | 普通索引 |
| uk_room_user | room_id, user_id | 唯一索引 |

**设计说明**：
- 记录玩家观战历史，支持观战人数统计
- `uk_room_user`：防止同一用户重复观战同一房间
- `leave_time`：NULL表示正在观战

#### 2.2.9 t_game_action（游戏操作表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| action_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 操作记录ID |
| record_id | BIGINT | NOT NULL, FK | 关联对局ID |
| user_id | BIGINT | NOT NULL, FK | 操作用户ID |
| action_type | VARCHAR(20) | NOT NULL | 操作类型: REGRET(悔棋)/SURRENDER(投降)/DRAW(求和)/RESUME(继续) |
| action_status | VARCHAR(20) | NOT NULL | 操作状态: PENDING(待确认)/ACCEPTED(已接受)/REJECTED(已拒绝)/CANCELLED(已取消) |
| move_number | INT | NOT NULL | 操作发起时的落子数(用于悔棋回退) |
| reject_reason | VARCHAR(100) | NULL | 拒绝原因 |
| opponent_response | DATETIME | NULL | 对手响应时间 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引设计**：
| 索引名 | 字段 | 类型 |
|--------|------|------|
| PRIMARY | action_id | 主键 |
| idx_record_id | record_id | 普通索引 |
| idx_user_id | user_id | 普通索引 |
| idx_action_type | action_type | 普通索引 |
| idx_action_status | action_status | 普通索引 |
| idx_record_status | record_id, action_status | 联合索引 |

**设计说明**：
- 记录游戏中的特殊操作（悔棋、投降、求和）
- `move_number`：悔棋时回退到该落子数之前的状态
- `action_status`：跟踪操作的审批状态
- `reject_reason`：记录拒绝操作的原因

#### 2.2.10 t_friend（好友关系表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| friend_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 好友关系ID |
| user_id | BIGINT | NOT NULL, FK | 用户ID |
| friend_user_id | BIGINT | NOT NULL, FK | 好友ID |
| relation_type | VARCHAR(20) | NOT NULL, DEFAULT 'FRIEND' | 关系类型: FRIEND(好友)/BLACKLIST(黑名单) |
| remark | VARCHAR(50) | NULL | 好友备注 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引设计**：
| 索引名 | 字段 | 类型 |
|--------|------|------|
| PRIMARY | friend_id | 主键 |
| idx_user_id | user_id | 普通索引 |
| idx_friend_user_id | friend_user_id | 普通索引 |
| uk_user_friend | user_id, friend_user_id | 唯一索引 |
| idx_relation_type | relation_type | 普通索引 |

**设计说明**：
- 双向好友关系，A加B为好友时，同时创建两条记录(A→B, B→A)
- `relation_type`：支持好友和黑名单两种关系
- `remark`：支持自定义好友备注

#### 2.2.11 t_friend_request（好友请求表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| request_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 请求ID |
| from_user_id | BIGINT | NOT NULL, FK | 发起者ID |
| to_user_id | BIGINT | NOT NULL, FK | 接收者ID |
| status | VARCHAR(20) | NOT NULL | 状态: PENDING(待处理)/ACCEPTED(已接受)/REJECTED(已拒绝)/EXPIRED(已过期) |
| message | VARCHAR(200) | NULL | 请求附言 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| respond_time | DATETIME | NULL | 响应时间 |

**索引设计**：
| 索引名 | 字段 | 类型 |
|--------|------|------|
| PRIMARY | request_id | 主键 |
| idx_from_user | from_user_id | 普通索引 |
| idx_to_user | to_user_id | 普通索引 |
| uk_from_to | from_user_id, to_user_id | 唯一索引 |
| idx_status | status | 普通索引 |
| idx_to_status | to_user_id, status | 联合索引 |

**设计说明**：
- 记录好友请求状态，防止重复请求（uk_from_to）
- `message`：支持请求时附带消息
- 请求超时自动标记为EXPIRED（建议24小时）

### 2.3 索引优化策略

| 表名 | 优化场景 | 索引方案 |
|------|----------|----------|
| t_user | 登录查询 | username唯一索引 |
| t_user | 积分排序 | score普通索引 |
| t_game_record | 个人战绩查询 | player1_id, player2_id联合索引 |
| t_game_record | 时间排序 | create_time普通索引 |
| t_rank | 排行榜查询 | score降序索引 |
| t_game_save | 文件恢复查询 | file_uri唯一索引 |
| t_game_save | 用户存档列表 | user_id普通索引 |
| t_socket_session | 断线重连 | session_key唯一索引 |
| t_socket_session | 在线状态查询 | user_id+status联合索引 |
| t_socket_session | 活跃状态跟踪 | last_heartbeat普通索引 |
| t_chat_message | 房间消息查询 | room_id+create_time联合索引 |
| t_room | 大厅房间列表 | status+current_players联合索引 |
| t_room | 观战房间查询 | allow_watch+status联合索引 |
| t_room_watcher | 房间观战者 | room_id普通索引 |
| t_room_watcher | 用户观战记录 | user_id普通索引 |
| t_game_action | 对局操作记录 | record_id普通索引 |
| t_socket_session | 断线重连定位 | room_id+status联合索引 |
| t_friend | 好友列表查询 | user_id普通索引 |
| t_friend | 好友关系查询 | user_id+friend_user_id联合索引 |
| t_friend_request | 待处理请求 | to_user_id+status联合索引 |
| t_friend_request | 发送请求 | from_user_id普通索引 |

## 3. Socket会话管理机制

### 3.1 连接流程

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant Socket as SocketServer
    participant SessionRepo as SocketSessionRepository
    participant DB as MySQL
    
    Client->>Socket: 发起连接(携带sessionKey)
    Socket->>SessionRepo: findBySessionKey(sessionKey)
    SessionRepo->>DB: SELECT * FROM t_socket_session WHERE session_key = ?
    alt 会话存在且未过期
        DB-->>SessionRepo: 返回会话记录
        SessionRepo-->>Socket: 返回SocketSession
        Socket->>SessionRepo: updateStatus(sessionId, "connected")
        SessionRepo->>DB: UPDATE t_socket_session SET status = 'connected'
        Socket-->>Client: 连接成功(恢复状态)
    else 会话不存在或已过期
        DB-->>SessionRepo: 返回空
        SessionRepo-->>Socket: 返回null
        Socket->>SessionRepo: createSession(userId)
        SessionRepo->>DB: INSERT INTO t_socket_session
        DB-->>SessionRepo: 返回sessionKey
        SessionRepo-->>Socket: 返回新会话
        Socket-->>Client: 连接成功(新会话)
    end
```

### 3.2 会话活跃状态维护

> 已移除独立的心跳包机制。`last_heartbeat` 字段在每次 `SessionManager.getSession*()` 查询会话时自动更新（调用 `SocketSessionDAO.updateHeartbeat()`），用于跟踪用户活跃状态。断线由 Socket 连接断开事件直接触发。

### 3.3 断线重连流程

```mermaid
flowchart TD
    A[检测断线] --> B[启动重连定时器]
    B --> C{重连成功}
    C -->|是| D[发送sessionKey验证]
    D --> E{会话有效}
    E -->|是| F[恢复对局状态]
    E -->|否| G[创建新会话]
    G --> H[重新加入房间]
    H --> F
    C -->|否| I{重连次数<最大次数}
    I -->|是| J[等待后重试]
    J --> C
    I -->|否| K[判定断线方负]
    K --> L[结算结果]
    L --> M[更新数据库]
```

## 4. 棋局保存与恢复机制

### 4.1 设计思路

通过"数据库记录 + 文件存储"的方式实现棋局保存与恢复：
- **数据库记录**：保存棋局元数据和关键状态
- **文件存储**：保存完整的棋局工程文件，通过URI引用

```mermaid
flowchart TD
    A[用户保存棋局] --> B[生成棋局工程文件]
    B --> C[存储文件到服务器]
    C --> D[获取文件URI]
    D --> E[写入t_game_save表]
    E --> F[保存成功]
    
    G[用户恢复棋局] --> H[查询t_game_save表]
    H --> I[获取file_uri]
    I --> J[读取工程文件]
    J --> K[解析棋盘状态]
    K --> L[恢复棋局]
```

### 4.2 棋局文件URI格式

```
格式: game://{server}/{version}/{user_id}/{timestamp}/{hash}
示例: game://localhost:8080/v1/1/20260101120000/abc123
```

| 部分 | 说明 |
|------|------|
| game:// | 协议标识 |
| server | 服务器地址 |
| version | 版本号 |
| user_id | 用户ID |
| timestamp | 保存时间戳 |
| hash | 文件校验码 |

### 4.3 棋局工程文件结构

```json
{
    "version": "1.0",
    "metadata": {
        "saveId": 1,
        "userId": 1,
        "recordId": null,
        "saveTime": "2026-01-01 12:00:00",
        "saveName": "关键对局"
    },
    "gameState": {
        "boardSize": 15,
        "board": [
            [0, 0, 0, ...],
            [0, 1, 0, ...],
            ...
        ],
        "currentPlayer": 1,
        "moveHistory": [
            {"x": 7, "y": 7, "player": 1, "time": "12:00:00"},
            {"x": 8, "y": 8, "player": 2, "time": "12:00:05"},
            ...
        ],
        "gameStatus": "playing",
        "winner": null,
        "duration": 60
    },
    "players": {
        "player1": {"userId": 1, "username": "player1", "pieceColor": 1},
        "player2": {"userId": 2, "username": "player2", "pieceColor": 2}
    }
}
```

## 5. Repository接口设计

### 5.1 接口分层架构

```mermaid
flowchart TD
    subgraph 业务层
        A[UserService]
        B[GameService]
        C[RankService]
        D[SocketService]
        E[RoomService]
        F[ChatService]
    end
    
    subgraph Repository层
        G[UserRepository]
        H[GameRecordRepository]
        I[RankRepository]
        J[GameSaveRepository]
        K[SocketSessionRepository]
        L[RoomRepository]
        M[ChatMessageRepository]
    end
    
    subgraph 数据层
        N[MySQL]
        O[文件存储]
    end
    
    A --> G
    B --> H
    B --> J
    C --> I
    D --> K
    E --> L
    F --> M
    G --> N
    H --> N
    I --> N
    J --> N
    J --> O
    K --> N
    L --> N
    M --> N
```

### 5.2 UserRepository

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| findById | Long userId | User | 根据ID查询用户 |
| findByUsername | String username | User | 根据用户名查询 |
| findByEmail | String email | User | 根据邮箱查询 |
| save | User user | User | 保存用户 |
| update | User user | User | 更新用户 |
| deleteById | Long userId | void | 删除用户 |
| existsByUsername | String username | boolean | 用户名是否存在 |
| existsByEmail | String email | boolean | 邮箱是否存在 |
| updateScore | Long userId, int score | int | 更新积分 |
| updateWinCount | Long userId | int | 增加胜场数 |
| updateLoseCount | Long userId | int | 增加负场数 |
| updateDrawCount | Long userId | int | 增加平局数 |
| findByScoreRange | int minScore, int maxScore | List\<User\> | 按积分范围查询 |

### 5.3 GameRecordRepository

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| findById | Long recordId | GameRecord | 根据ID查询记录 |
| findByUserId | Long userId, int page, int size | List\<GameRecord\> | 查询用户战绩(分页) |
| findRecent | int limit | List\<GameRecord\> | 查询最近对局 |
| findByTimeRange | Date startTime, Date endTime | List\<GameRecord\> | 按时间范围查询 |
| save | GameRecord record | GameRecord | 保存对局记录 |
| deleteById | Long recordId | void | 删除记录 |
| countByUserId | Long userId | int | 统计用户对局数 |
| countByResult | Long userId, String result | int | 统计胜负平次数 |

### 5.4 RankRepository

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| findByUserId | Long userId | Rank | 查询用户排名 |
| findAll | int page, int size | List\<Rank\> | 查询所有排名(分页) |
| findTopN | int n | List\<Rank\> | 查询前N名 |
| findByScoreDesc | int page, int size | List\<Rank\> | 按积分降序查询 |
| save | Rank rank | Rank | 保存排名 |
| update | Rank rank | Rank | 更新排名 |
| deleteById | Long rankId | void | 删除排名 |
| refreshRank | void | void | 刷新所有排名 |
| updateUserRank | Long userId | void | 更新单个用户排名 |

### 5.5 GameSaveRepository（棋局保存接口）

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| findById | Long saveId | GameSave | 根据ID查询存档 |
| findByFileUri | String fileUri | GameSave | 根据文件URI查询 |
| findByUserId | Long userId, int page, int size | List\<GameSave\> | 查询用户存档(分页) |
| findByRecordId | Long recordId | List\<GameSave\> | 查询对局关联存档 |
| findByStatus | String status | List\<GameSave\> | 按状态查询 |
| save | GameSave save | GameSave | 保存存档 |
| update | GameSave save | GameSave | 更新存档 |
| deleteById | Long saveId | void | 删除存档 |
| deleteByFileUri | String fileUri | void | 根据URI删除 |
| countByUserId | Long userId | int | 统计用户存档数 |

### 5.6 SocketSessionRepository（Socket会话接口）

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| findById | Long sessionId | SocketSession | 根据ID查询会话 |
| findBySessionKey | String sessionKey | SocketSession | 根据会话密钥查询 |
| findByUserId | Long userId | SocketSession | 根据用户ID查询会话 |
| findByStatus | String status | List\<SocketSession\> | 按状态查询 |
| findExpiredSessions | Date timeout | List\<SocketSession\> | 查询过期会话 |
| save | SocketSession session | SocketSession | 保存会话 |
| update | SocketSession session | SocketSession | 更新会话 |
| updateStatus | Long sessionId, String status | int | 更新连接状态 |
| updateHeartbeat | Long sessionId | int | 更新活跃时间 |
| updateConnectionId | Long sessionId, String connectionId | int | 更新连接ID |
| deleteById | Long sessionId | void | 删除会话 |
| deleteByUserId | Long userId | void | 删除用户所有会话 |
| countByStatus | String status | int | 统计在线人数 |

### 5.7 RoomRepository（房间接口）

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| findById | Long roomId | Room | 根据ID查询房间 |
| findByStatus | String status, int page, int size | List\<Room\> | 按状态查询房间 |
| findByHostId | Long hostId | List\<Room\> | 查询房主创建的房间 |
| findByName | String roomName | List\<Room\> | 按名称查询 |
| save | Room room | Room | 保存房间 |
| update | Room room | Room | 更新房间 |
| updateStatus | Long roomId, String status | int | 更新房间状态 |
| updatePlayerCount | Long roomId, int count | int | 更新人数 |
| deleteById | Long roomId | void | 删除房间 |
| countByStatus | String status | int | 统计房间数 |

### 5.8 ChatMessageRepository（聊天消息接口）

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| findById | Long messageId | ChatMessage | 根据ID查询消息 |
| findByRoomId | Long roomId, int page, int size | List\<ChatMessage\> | 查询房间消息 |
| findByUserId | Long userId, int page, int size | List\<ChatMessage\> | 查询用户发送的消息 |
| findRecentByRoomId | Long roomId, int limit | List\<ChatMessage\> | 查询房间最近消息 |
| save | ChatMessage message | ChatMessage | 保存消息 |
| deleteById | Long messageId | void | 删除消息 |
| deleteByRoomId | Long roomId | void | 删除房间所有消息 |
| countByRoomId | Long roomId | int | 统计房间消息数 |

### 5.9 RoomWatcherRepository（房间观战接口）

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| findById | Long watcherId | RoomWatcher | 根据ID查询 |
| findByRoomId | Long roomId | List\<RoomWatcher\> | 查询房间观战者 |
| findByUserId | Long userId | List\<RoomWatcher\> | 查询用户观战记录 |
| findByRoomIdAndUserId | Long roomId, Long userId | RoomWatcher | 查询特定观战记录 |
| save | RoomWatcher watcher | RoomWatcher | 保存观战记录 |
| updateLeaveTime | Long watcherId, Date leaveTime | int | 更新离开时间 |
| deleteById | Long watcherId | void | 删除观战记录 |
| deleteByRoomId | Long roomId | void | 删除房间所有观战记录 |
| countByRoomId | Long roomId | int | 统计房间观战人数 |

### 5.10 GameActionRepository（游戏操作接口）

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| findById | Long actionId | GameAction | 根据ID查询 |
| findByRecordId | Long recordId | List\<GameAction\> | 查询对局操作记录 |
| findByUserId | Long userId | List\<GameAction\> | 查询用户操作记录 |
| findByRecordIdAndStatus | Long recordId, String status | List\<GameAction\> | 查询对局待处理操作 |
| save | GameAction action | GameAction | 保存操作记录 |
| updateStatus | Long actionId, String status | int | 更新操作状态 |
| updateRejectReason | Long actionId, String reason | int | 更新拒绝原因 |
| deleteById | Long actionId | void | 删除操作记录 |
| deleteByRecordId | Long recordId | void | 删除对局所有操作记录 |

### 5.11 FriendRepository（好友关系接口）

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| findById | Long friendId | Friend | 根据ID查询 |
| findByUserId | Long userId | List\<Friend\> | 查询用户好友列表 |
| findByUserIdAndRelationType | Long userId, String relationType | List\<Friend\> | 按关系类型查询 |
| findByUserIdAndFriendUserId | Long userId, Long friendUserId | Friend | 查询好友关系 |
| existsByUserIdAndFriendUserId | Long userId, Long friendUserId | boolean | 是否存在好友关系 |
| save | Friend friend | Friend | 保存好友关系 |
| updateRemark | Long friendId, String remark | int | 更新好友备注 |
| deleteById | Long friendId | void | 删除好友关系 |
| deleteByUserIdAndFriendUserId | Long userId, Long friendUserId | void | 删除双向好友关系 |
| countByUserId | Long userId | int | 统计好友数量 |

### 5.12 FriendRequestRepository（好友请求接口）

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| findById | Long requestId | FriendRequest | 根据ID查询 |
| findByFromUserId | Long fromUserId | List\<FriendRequest\> | 查询发出的请求 |
| findByToUserId | Long toUserId | List\<FriendRequest\> | 查询收到的请求 |
| findByToUserIdAndStatus | Long toUserId, String status | List\<FriendRequest\> | 查询待处理请求 |
| findByFromUserIdAndToUserId | Long fromUserId, Long toUserId | FriendRequest | 查询特定请求 |
| existsByFromUserIdAndToUserId | Long fromUserId, Long toUserId | boolean | 是否存在请求 |
| save | FriendRequest request | FriendRequest | 保存请求 |
| updateStatus | Long requestId, String status | int | 更新请求状态 |
| updateRespondTime | Long requestId, Date respondTime | int | 更新响应时间 |
| deleteById | Long requestId | void | 删除请求 |
| deleteExpiredRequests | Date expireTime | int | 删除过期请求 |
| countByToUserIdAndStatus | Long toUserId, String status | int | 统计待处理请求数 |

## 6. 数据模型定义

### 6.1 User 实体

```java
public class User {
    private Long userId;
    private String username;
    private String password;
    private String email;
    private String avatar;
    private String role;
    private String status;
    private Integer score;
    private Integer winCount;
    private Integer loseCount;
    private Integer drawCount;
    private Integer aiWinCount;
    private Integer aiLoseCount;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
```

### 6.2 GameRecord 实体

```java
public class GameRecord {
    private Long recordId;
    private Long player1Id;
    private Long player2Id;
    private String gameType;
    private Long winnerId;
    private String result;
    private String boardData;
    private Integer duration;
    private Integer moveCount;
    private Date createTime;
}
```

### 6.3 Rank 实体

```java
public class Rank {
    private Long rankId;
    private Long userId;
    private Integer rank;
    private Integer score;
    private BigDecimal winRate;
    private BigDecimal aiWinRate;
    private Integer version;
    private Date updateTime;
}
```

### 6.4 GameSave 实体

```java
public class GameSave {
    private Long saveId;
    private Long userId;
    private Long recordId;
    private String fileUri;
    private String saveName;
    private String boardData;
    private Integer currentPlayer;
    private String gameStatus;
    private String checksum;
    private Integer fileSize;
    private Date createTime;
    private Date updateTime;
}
```

### 6.5 SocketSession 实体

```java
public class SocketSession {
    private Long sessionId;
    private Long userId;
    private String sessionKey;
    private String clientIp;
    private String connectionId;
    private Long roomId;
    private String status;
    private Date lastHeartbeat;
    private Date disconnectTime;
    private String disconnectReason;
    private Integer reconnectAttempts;
    private Date createTime;
    private Date updateTime;
}
```

### 6.6 ChatMessage 实体

```java
public class ChatMessage {
    private Long messageId;
    private Long roomId;
    private Long userId;
    private String content;
    private String messageType;
    private Date createTime;
}
```

### 6.7 Room 实体

```java
public class Room {
    private Long roomId;
    private String roomName;
    private String password;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private Integer watcherCount;
    private String status;
    private Long hostId;
    private Boolean allowWatch;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
```

### 6.8 RoomWatcher 实体

```java
public class RoomWatcher {
    private Long watcherId;
    private Long roomId;
    private Long userId;
    private Date joinTime;
    private Date leaveTime;
}
```

### 6.9 GameAction 实体

```java
public class GameAction {
    private Long actionId;
    private Long recordId;
    private Long userId;
    private String actionType;
    private String actionStatus;
    private Integer moveNumber;
    private String rejectReason;
    private Date opponentResponse;
    private Date createTime;
}
```

### 6.10 Friend 实体

```java
public class Friend {
    private Long friendId;
    private Long userId;
    private Long friendUserId;
    private String relationType;
    private String remark;
    private Date createTime;
}
```

### 6.11 FriendRequest 实体

```java
public class FriendRequest {
    private Long requestId;
    private Long fromUserId;
    private Long toUserId;
    private String status;
    private String message;
    private Date createTime;
    private Date respondTime;
}
```

### 6.12 DTO 定义

#### UserDTO（用户信息传输对象）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| userId | Long | 用户ID |
| username | String | 用户名 |
| email | String | 邮箱 |
| avatar | String | 头像URL |
| score | Integer | 积分 |
| winCount | Integer | 胜场数 |
| loseCount | Integer | 负场数 |
| drawCount | Integer | 平局数 |

#### GameRecordDTO（对局记录传输对象）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| recordId | Long | 记录ID |
| player1Id | Long | 玩家1ID |
| player1Name | String | 玩家1名称 |
| player2Id | Long | 玩家2ID |
| player2Name | String | 玩家2名称 |
| winnerId | Long | 胜者ID |
| result | String | 结果 |
| boardData | String | 棋谱数据 |
| duration | Integer | 时长 |
| createTime | Date | 创建时间 |

#### GameSaveDTO（棋局保存传输对象）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| saveId | Long | 保存ID |
| userId | Long | 用户ID |
| recordId | Long | 关联对局ID |
| fileUri | String | 文件URI |
| saveName | String | 保存名称 |
| boardData | String | 棋盘状态 |
| currentPlayer | Integer | 当前执子方 |
| gameStatus | String | 游戏状态 |
| createTime | Date | 创建时间 |

#### SocketSessionDTO（Socket会话传输对象）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| sessionId | Long | 会话ID |
| userId | Long | 用户ID |
| sessionKey | String | 会话密钥 |
| status | String | 连接状态 |
| lastHeartbeat | Date | 最后活跃时间 |

#### ChatMessageDTO（聊天消息传输对象）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| messageId | Long | 消息ID |
| roomId | Long | 房间ID |
| userId | Long | 发送者ID |
| username | String | 发送者名称 |
| content | String | 消息内容 |
| messageType | String | 消息类型 |
| createTime | Date | 创建时间 |

#### RoomDTO（房间传输对象）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| roomId | Long | 房间ID |
| roomName | String | 房间名称 |
| passwordRequired | Boolean | 是否需要密码 |
| maxPlayers | Integer | 最大人数 |
| currentPlayers | Integer | 当前人数 |
| status | String | 房间状态 |
| hostId | Long | 房主ID |
| hostName | String | 房主名称 |
| createTime | Date | 创建时间 |

## 7. SQL脚本

### 7.1 MySQL配置（my.cnf / my.ini）

确保MySQL服务端全局UTF-8编码：

```ini
[mysqld]
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
default-storage-engine = InnoDB
max_connections = 1000
wait_timeout = 600
interactive_timeout = 600

[mysql]
default-character-set = utf8mb4

[client]
default-character-set = utf8mb4
```

**验证编码配置**：
```sql
SHOW VARIABLES LIKE 'character_set_%';
SHOW VARIABLES LIKE 'collation_%';
```

**预期结果**：
| 变量名 | 值 |
|--------|-----|
| character_set_server | utf8mb4 |
| collation_server | utf8mb4_unicode_ci |
| character_set_database | utf8mb4 |
| character_set_client | utf8mb4 |
| character_set_connection | utf8mb4 |

### 7.2 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS gomatch 
    DEFAULT CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE gomatch;

-- 设置会话编码（确保当前连接也是UTF-8）
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET CHARACTER SET utf8mb4;
```

### 7.3 创建表

```sql
CREATE TABLE IF NOT EXISTS `t_user` (
    `user_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码(加密)',
    `email` VARCHAR(100) UNIQUE COMMENT '邮箱',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'PLAYER' COMMENT '角色: PLAYER/ADMIN',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/BANNED/INACTIVE',
    `score` INT DEFAULT 1000 COMMENT '积分',
    `win_count` INT DEFAULT 0 COMMENT '胜场数',
    `lose_count` INT DEFAULT 0 COMMENT '负场数',
    `draw_count` INT DEFAULT 0 COMMENT '平局数',
    `ai_win_count` INT DEFAULT 0 COMMENT 'AI对战胜场数',
    `ai_lose_count` INT DEFAULT 0 COMMENT 'AI对战负场数',
    `version` INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_username` (`username`),
    INDEX `idx_role` (`role`),
    INDEX `idx_status` (`status`),
    INDEX `idx_role_status` (`role`, `status`),
    INDEX `idx_score` (`score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `t_game_record` (
    `record_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `player1_id` BIGINT NOT NULL COMMENT '玩家1ID',
    `player2_id` BIGINT NOT NULL COMMENT '玩家2ID',
    `game_type` VARCHAR(20) NOT NULL DEFAULT 'PVP' COMMENT '游戏类型: PVP/PVE',
    `winner_id` BIGINT COMMENT '胜者ID(平局为NULL)',
    `result` VARCHAR(10) NOT NULL COMMENT '结果: win/lose/draw',
    `board_data` TEXT NOT NULL COMMENT '棋谱数据(JSON格式)',
    `duration` INT COMMENT '对局时长(秒)',
    `move_count` INT DEFAULT 0 COMMENT '落子总数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_player1` (`player1_id`),
    INDEX `idx_player2` (`player2_id`),
    INDEX `idx_game_type` (`game_type`),
    INDEX `idx_create_time` (`create_time`),
    FOREIGN KEY (`player1_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`player2_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`winner_id`) REFERENCES `t_user`(`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏记录表';

CREATE TABLE IF NOT EXISTS `t_rank` (
    `rank_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '排名ID',
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    `rank` INT DEFAULT 0 COMMENT '排名',
    `score` INT DEFAULT 0 COMMENT '积分',
    `win_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT '胜率',
    `ai_win_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT 'AI对战胜率',
    `version` INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_rank` (`rank`),
    INDEX `idx_score` (`score`),
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜表';

CREATE TABLE IF NOT EXISTS `t_game_save` (
    `save_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '保存ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `record_id` BIGINT COMMENT '关联对局ID',
    `file_uri` VARCHAR(500) NOT NULL UNIQUE COMMENT '棋局文件URI',
    `save_name` VARCHAR(100) COMMENT '保存名称',
    `board_data` TEXT NOT NULL COMMENT '棋盘状态(JSON)',
    `current_player` INT NOT NULL COMMENT '当前执子方',
    `game_status` VARCHAR(20) NOT NULL COMMENT '游戏状态',
    `checksum` VARCHAR(64) COMMENT '文件校验和(SHA-256)',
    `file_size` INT DEFAULT 0 COMMENT '文件大小(字节)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_record_id` (`record_id`),
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`record_id`) REFERENCES `t_game_record`(`record_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='棋局保存表';

CREATE TABLE IF NOT EXISTS `t_socket_session` (
    `session_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `session_key` VARCHAR(64) NOT NULL UNIQUE COMMENT '会话密钥',
    `client_ip` VARCHAR(50) COMMENT '客户端IP',
    `connection_id` VARCHAR(64) COMMENT '连接ID',
    `room_id` BIGINT COMMENT '当前所在房间ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'connected' COMMENT '连接状态',
    `last_heartbeat` DATETIME COMMENT '最后活跃时间',
    `disconnect_time` DATETIME COMMENT '断开连接时间',
    `disconnect_reason` VARCHAR(50) COMMENT '断开原因',
    `reconnect_attempts` INT DEFAULT 0 COMMENT '重连尝试次数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_last_heartbeat` (`last_heartbeat`),
    INDEX `idx_room_id` (`room_id`),
    INDEX `idx_room_status` (`room_id`, `status`),
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`room_id`) REFERENCES `t_room`(`room_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Socket会话表';

CREATE TABLE IF NOT EXISTS `t_room` (
    `room_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '房间ID',
    `room_name` VARCHAR(100) NOT NULL COMMENT '房间名称',
    `password` VARCHAR(100) COMMENT '房间密码',
    `max_players` INT DEFAULT 2 COMMENT '最大人数',
    `current_players` INT DEFAULT 0 COMMENT '当前人数',
    `watcher_count` INT DEFAULT 0 COMMENT '观战人数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'waiting' COMMENT '房间状态',
    `host_id` BIGINT NOT NULL COMMENT '房主ID',
    `allow_watch` BOOLEAN DEFAULT TRUE COMMENT '是否允许观战',
    `version` INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_status` (`status`),
    INDEX `idx_host_id` (`host_id`),
    INDEX `idx_allow_watch` (`allow_watch`),
    INDEX `idx_status_players` (`status`, `current_players`),
    INDEX `idx_watch_status` (`allow_watch`, `status`),
    FOREIGN KEY (`host_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间表';

CREATE TABLE IF NOT EXISTS `t_chat_message` (
    `message_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    `room_id` BIGINT NOT NULL COMMENT '房间ID',
    `user_id` BIGINT NOT NULL COMMENT '发送者ID',
    `content` VARCHAR(500) NOT NULL COMMENT '消息内容',
    `message_type` VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '消息类型',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_room_id` (`room_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`),
    FOREIGN KEY (`room_id`) REFERENCES `t_room`(`room_id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

CREATE TABLE IF NOT EXISTS `t_room_watcher` (
    `watcher_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '观战记录ID',
    `room_id` BIGINT NOT NULL COMMENT '房间ID',
    `user_id` BIGINT NOT NULL COMMENT '观战者ID',
    `join_time` DATETIME NOT NULL COMMENT '加入观战时间',
    `leave_time` DATETIME COMMENT '离开观战时间',
    INDEX `idx_room_id` (`room_id`),
    INDEX `idx_user_id` (`user_id`),
    UNIQUE INDEX `uk_room_user` (`room_id`, `user_id`),
    FOREIGN KEY (`room_id`) REFERENCES `t_room`(`room_id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间观战表';

CREATE TABLE IF NOT EXISTS `t_game_action` (
    `action_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '操作记录ID',
    `record_id` BIGINT NOT NULL COMMENT '关联对局ID',
    `user_id` BIGINT NOT NULL COMMENT '操作用户ID',
    `action_type` VARCHAR(20) NOT NULL COMMENT '操作类型',
    `action_status` VARCHAR(20) NOT NULL COMMENT '操作状态',
    `move_number` INT NOT NULL COMMENT '操作时落子数',
    `reject_reason` VARCHAR(100) COMMENT '拒绝原因',
    `opponent_response` DATETIME COMMENT '对手响应时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_record_id` (`record_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_action_type` (`action_type`),
    INDEX `idx_action_status` (`action_status`),
    INDEX `idx_record_status` (`record_id`, `action_status`),
    FOREIGN KEY (`record_id`) REFERENCES `t_game_record`(`record_id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏操作表';

CREATE TABLE IF NOT EXISTS `t_friend` (
    `friend_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友关系ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_user_id` BIGINT NOT NULL COMMENT '好友ID',
    `relation_type` VARCHAR(20) NOT NULL DEFAULT 'FRIEND' COMMENT '关系类型',
    `remark` VARCHAR(50) COMMENT '好友备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_friend_user_id` (`friend_user_id`),
    UNIQUE INDEX `uk_user_friend` (`user_id`, `friend_user_id`),
    INDEX `idx_relation_type` (`relation_type`),
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`friend_user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

CREATE TABLE IF NOT EXISTS `t_friend_request` (
    `request_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '请求ID',
    `from_user_id` BIGINT NOT NULL COMMENT '发起者ID',
    `to_user_id` BIGINT NOT NULL COMMENT '接收者ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '请求状态',
    `message` VARCHAR(200) COMMENT '请求附言',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `respond_time` DATETIME COMMENT '响应时间',
    INDEX `idx_from_user` (`from_user_id`),
    INDEX `idx_to_user` (`to_user_id`),
    UNIQUE INDEX `uk_from_to` (`from_user_id`, `to_user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_to_status` (`to_user_id`, `status`),
    FOREIGN KEY (`from_user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`to_user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友请求表';
```

### 7.4 初始化数据

```sql
INSERT INTO `t_user` (`username`, `password`, `email`, `role`, `status`, `score`) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '[redacted-email]', 'ADMIN', 'ACTIVE', 9999),
('player1', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '[redacted-email]', 'PLAYER', 'ACTIVE', 1000),
('player2', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '[redacted-email]', 'PLAYER', 'ACTIVE', 1000);
```

### 7.5 JDBC连接配置

确保Java应用连接数据库时使用UTF-8编码：

```java
// JDBC URL配置
String url = "jdbc:mysql://localhost:3306/gomatch?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci&serverTimezone=Asia/Shanghai";

// HikariCP连接池配置
HikariConfig config = new HikariConfig();
config.setJdbcUrl(url);
config.setUsername("gomatch");
config.setPassword("password");
config.setMaximumPoolSize(60);
config.setMinimumIdle(10);
config.setConnectionTimeout(30000);
config.setIdleTimeout(600000);
config.setMaxLifetime(1800000);
```

**连接池初始化时执行编码设置**：
```java
// 在连接池初始化后执行
try (Connection conn = dataSource.getConnection()) {
    try (Statement stmt = conn.createStatement()) {
        stmt.execute("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
        stmt.execute("SET CHARACTER SET utf8mb4");
    }
} catch (SQLException e) {
    // 处理异常
}
```

## 8. Socket协议与数据库交互设计

### 8.1 Socket消息类型

| 消息类型 | 方向 | 说明 | 数据库操作 |
|----------|------|------|------------|
| CONNECT | Client→Server | 客户端连接 | 插入/更新t_socket_session |
| DISCONNECT | Client→Server | 客户端断开 | 更新t_socket_session状态 |
| MOVE | Client→Server | 落子请求 | 实时内存更新，对局结束后写入t_game_record |
| CHAT | Client→Server | 聊天消息 | 插入t_chat_message |
| CREATE_ROOM | Client→Server | 创建房间 | 插入t_room |
| JOIN_ROOM | Client→Server | 加入房间 | 更新t_room.current_players |
| LEAVE_ROOM | Client→Server | 离开房间 | 更新t_room.current_players |
| GAME_OVER | Server→Client | 游戏结束 | 插入t_game_record，更新t_user积分/胜负场 |
| SAVE_GAME | Client→Server | 保存棋局 | 插入t_game_save，写入文件存储 |
| LOAD_GAME | Client→Server | 恢复棋局 | 查询t_game_save，读取文件存储 |

### 8.2 实时数据与持久化数据分离

```mermaid
flowchart LR
    subgraph 实时数据(内存)
        A[房间状态]
        B[棋盘状态]
        C[当前玩家]
        D[Socket连接]
    end
    
    subgraph 持久化数据(数据库)
        E[t_user]
        F[t_game_record]
        G[t_socket_session]
        H[t_chat_message]
        I[t_game_save]
    end
    
    A -.-> H
    B -.-> F
    B -.-> I
    C -.-> F
    D -.-> G
    E -.-> F
    E -.-> H
    
    style A fill:#f9f,stroke:#333
    style B fill:#f9f,stroke:#333
    style C fill:#f9f,stroke:#333
    style D fill:#f9f,stroke:#333
    style E fill:#bbf,stroke:#333
    style F fill:#bbf,stroke:#333
    style G fill:#bbf,stroke:#333
    style H fill:#bbf,stroke:#333
    style I fill:#bbf,stroke:#333
```

**设计原则**：
- 实时数据（棋盘状态、连接状态）优先保存在内存中，保证低延迟
- 持久化数据（用户信息、对局记录、消息历史）写入数据库
- 关键状态变更时，异步写入数据库（如每N步落子后保存一次）

## 9. 事务与并发

### 9.1 事务边界

| 操作 | 事务要求 | 说明 |
|------|----------|------|
| 用户注册 | REQUIRED | 保证用户数据完整 |
| 用户登录 | SUPPORTS | 只读操作 |
| 对局结束结算 | REQUIRED | 更新战绩、积分、排名 |
| 保存棋局 | REQUIRED | 写入数据库和文件 |
| 恢复棋局 | SUPPORTS | 只读操作 |
| 创建房间 | REQUIRED | 写入房间数据 |
| 发送消息 | REQUIRED | 写入消息数据 |
| Socket连接 | REQUIRED | 写入会话数据 |

### 9.2 并发控制

| 场景 | 策略 | 说明 |
|------|------|------|
| 积分更新 | 乐观锁 | 使用版本号或CAS操作 |
| 排名刷新 | 悲观锁 | 锁定排行榜表 |
| 棋局保存 | 唯一性约束 | file_uri唯一索引 |
| 用户状态更新 | 行锁 | 通过主键锁定 |
| 房间人数更新 | 行锁 | 通过room_id锁定 |
| Socket会话更新 | 行锁 | 通过session_key锁定 |

## 10. 多线程与分布式架构设计

### 10.1 分布式锁机制

针对双服务器实时下棋场景，需要使用分布式锁保证同一棋局在同一时刻只有一个服务器处理。

#### 10.1.1 数据库分布式锁表

```sql
CREATE TABLE IF NOT EXISTS `t_distributed_lock` (
    `lock_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '锁ID',
    `lock_key` VARCHAR(128) NOT NULL UNIQUE COMMENT '锁键',
    `owner` VARCHAR(64) NOT NULL COMMENT '锁持有者(服务器标识)',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_lock_key` (`lock_key`),
    INDEX `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分布式锁表';
```

**锁键设计**：
| 场景 | 锁键格式 | 示例 |
|------|----------|------|
| 棋局操作 | game:{gameId} | game:123 |
| 用户操作 | user:{userId} | user:456 |
| 房间操作 | room:{roomId} | room:789 |

#### 10.1.2 分布式锁接口

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| tryLock | String lockKey, String owner, long timeoutMs | boolean | 尝试获取锁 |
| unlock | String lockKey, String owner | void | 释放锁 |
| renewLock | String lockKey, String owner, long timeoutMs | boolean | 续期锁 |
| isLocked | String lockKey | boolean | 检查锁是否存在 |

### 10.2 乐观锁版本号

为高并发更新场景添加版本号字段，防止数据覆盖。

#### 10.2.1 需要乐观锁的表

| 表名 | 新增字段 | 说明 |
|------|----------|------|
| t_user | version INT DEFAULT 1 | 用户数据版本号 |
| t_rank | version INT DEFAULT 1 | 排名数据版本号 |
| t_room | version INT DEFAULT 1 | 房间数据版本号 |

#### 10.2.2 更新SQL示例

```sql
-- 更新用户积分（乐观锁）
UPDATE t_user 
SET score = score + 100, version = version + 1 
WHERE user_id = ? AND version = ?;

-- 更新房间人数（乐观锁）
UPDATE t_room 
SET current_players = current_players + 1, version = version + 1 
WHERE room_id = ? AND version = ? AND current_players < max_players;
```

### 10.3 数据库连接池配置

针对多线程环境，配置合理的连接池参数。

#### 10.3.1 HikariCP配置建议

| 参数 | 值 | 说明 |
|------|-----|------|
| maximumPoolSize | 60 | 最大连接数（CPU核心数×5） |
| minimumIdle | 10 | 最小空闲连接数 |
| connectionTimeout | 30000 | 连接超时时间(ms) |
| idleTimeout | 600000 | 空闲连接超时时间(ms) |
| maxLifetime | 1800000 | 连接最大生命周期(ms) |
| transactionIsolation | TRANSACTION_READ_COMMITTED | 事务隔离级别 |

#### 10.3.2 连接池监控

| 监控指标 | 说明 | 告警阈值 |
|----------|------|----------|
| activeConnections | 活跃连接数 | > 80% of maxPoolSize |
| idleConnections | 空闲连接数 | < minimumIdle |
| connectionWaitTime | 连接等待时间 | > 500ms |
| poolUtilization | 连接池利用率 | > 90% |

### 10.4 读写分离策略

针对高并发场景，采用主从复制实现读写分离。

#### 10.4.1 读写分配规则

| 操作类型 | 数据库 | 说明 |
|----------|--------|------|
| 用户注册/登录 | 主库 | 写操作 |
| 创建/加入房间 | 主库 | 写操作 |
| 落子操作 | 主库 | 写操作（实时性要求高） |
| 聊天消息 | 主库 | 写操作 |
| 个人战绩查询 | 从库 | 读操作 |
| 排行榜查询 | 从库 | 读操作 |
| 用户信息查询 | 从库 | 读操作 |
| 房间列表查询 | 从库 | 读操作 |

#### 10.4.2 数据一致性保障

| 场景 | 策略 | 说明 |
|------|------|------|
| 对局结束结算 | 同步写入主库 | 确保数据不丢失 |
| 排行榜刷新 | 异步同步到从库 | 允许短暂不一致 |
| 战绩查询 | 从库读取 | 接受秒级延迟 |

### 10.5 双客户端架构设计

针对两台服务器实时下棋场景，需要保证数据同步和状态一致性。

#### 10.5.1 服务器架构图

```mermaid
flowchart LR
    subgraph Server1[服务器1]
        A[SocketServer]
        B[GameEngine]
        C[RoomManager]
        D[LocalCache]
    end
    
    subgraph Server2[服务器2]
        E[SocketServer]
        F[GameEngine]
        G[RoomManager]
        H[LocalCache]
    end
    
    subgraph 共享存储
        I[MySQL主库]
        J[Redis缓存]
        K[分布式锁]
        L[文件存储]
    end
    
    A --> I
    A --> J
    B --> K
    C --> J
    D --> J
    
    E --> I
    E --> J
    F --> K
    G --> J
    H --> J
    
    I --> K
    J --> K
```

#### 10.5.2 服务器通信协议

| 消息类型 | 说明 | 数据结构 |
|----------|------|----------|
| GAME_SYNC | 棋局状态同步 | gameId, boardData, currentPlayer |
| ROOM_MIGRATE | 房间迁移通知 | roomId, targetServer |
| SESSION_RELOCATE | 会话重定位 | sessionKey, targetServer |
| BROADCAST_MESSAGE | 广播消息 | roomId, message |

#### 10.5.3 棋局状态同步流程

```mermaid
sequenceDiagram
    participant S1 as Server1
    participant S2 as Server2
    participant Redis as Redis缓存
    participant DB as MySQL主库
    
    S1->>Redis: SET game:{gameId} (棋局状态)
    S1->>DB: INSERT/UPDATE t_game_record
    Redis-->>S2: 订阅game:{gameId}变化
    S2->>Redis: GET game:{gameId}
    Redis-->>S2: 返回棋局状态
    S2->>S2: 更新本地缓存
```

### 10.6 多客户端并发处理

针对多个客户端同时请求的场景，需要保证请求顺序和数据一致性。

#### 10.6.1 请求队列设计

| 队列类型 | 用途 | 处理策略 |
|----------|------|----------|
| MoveQueue | 落子请求队列 | FIFO，单线程处理 |
| ChatQueue | 聊天消息队列 | FIFO，批量写入 |
| SaveQueue | 保存请求队列 | 合并写入，减少IO |

#### 10.6.2 请求去重

| 场景 | 去重策略 | 说明 |
|------|----------|------|
| 重复落子请求 | 请求ID + 时间戳 | 同一请求5秒内只处理一次 |
| 重复加入房间 | 用户ID + 房间ID | 同一用户同一房间只允许加入一次 |
| 重复创建房间 | 用户ID + 房间名 | 同一用户同房间名只允许创建一次 |

## 11. 角色与权限设计

### 11.1 角色定义

| 角色 | 标识 | 说明 |
|------|------|------|
| 普通玩家 | PLAYER | 可以下棋、聊天、查看战绩、保存棋局 |
| 管理员 | ADMIN | 除玩家权限外，可管理用户、房间、查看所有数据 |

### 11.2 用户状态

| 状态 | 标识 | 说明 |
|------|------|------|
| 活跃 | ACTIVE | 正常使用 |
| 封禁 | BANNED | 被管理员封禁，无法登录 |
| 未激活 | INACTIVE | 注册后未激活邮箱 |

### 11.3 权限矩阵

| 功能 | 普通玩家 | 管理员 |
|------|----------|--------|
| 用户登录 | ✅ | ✅ |
| 用户注册 | ✅ | ❌ |
| 创建房间 | ✅ | ✅ |
| 加入房间 | ✅ | ✅ |
| 落子下棋 | ✅ | ✅ |
| 聊天 | ✅ | ✅ |
| 保存/恢复棋局 | ✅ | ✅ |
| 查看个人战绩 | ✅ | ✅ |
| 查看排行榜 | ✅ | ✅ |
| 查看个人中心 | ✅ | ✅ |
| 管理用户（封禁/解封） | ❌ | ✅ |
| 管理房间（强制关闭） | ❌ | ✅ |
| 查看所有用户数据 | ❌ | ✅ |
| 查看所有对局记录 | ❌ | ✅ |
| 查看所有聊天记录 | ❌ | ✅ |
| 管理系统配置 | ❌ | ✅ |
| 查看系统日志 | ❌ | ✅ |

### 11.4 角色转换流程

```mermaid
flowchart TD
    A[注册新用户] --> B[角色=PLAYER]
    B --> C[状态=INACTIVE]
    C --> D{激活邮箱?}
    D -->|是| E[状态=ACTIVE]
    D -->|否| F[保持INACTIVE]
    
    E --> G{管理员操作}
    G -->|提升权限| H[角色=ADMIN]
    G -->|封禁| I[状态=BANNED]
    
    I --> J{管理员操作}
    J -->|解封| E
    J -->|删除| K[删除用户]
    
    H --> L{管理员操作}
    L -->|降级权限| E
```

### 11.5 Repository接口扩展

**UserRepository新增方法**：

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| findByRole | String role | List\<User\> | 按角色查询用户 |
| findByStatus | String status | List\<User\> | 按状态查询用户 |
| findByRoleAndStatus | String role, String status | List\<User\> | 按角色和状态查询 |
| updateRole | Long userId, String role | int | 更新用户角色 |
| updateStatus | Long userId, String status | int | 更新用户状态 |
| countByRole | String role | int | 统计角色人数 |
| countByStatus | String status | int | 统计状态人数 |

## 12. 数据安全

### 12.1 加密策略

| 数据项 | 加密方式 | 说明 |
|--------|----------|------|
| 密码 | 明文 | 实训演示环境直接存储 |
| 文件URI | Base64编码 | 防止敏感信息泄露 |
| 房间密码 | 明文 | 实训演示环境直接存储 |
| 棋谱数据 | 无 | 非敏感数据 |

### 12.2 访问控制

| 角色 | 可访问表 | 权限 |
|------|----------|------|
| 普通玩家(ACTIVE) | t_user(本人), t_game_record, t_rank, t_game_save(本人), t_socket_session(本人), t_chat_message(本人), t_room | SELECT, INSERT, UPDATE |
| 普通玩家(BANNED) | 无 | 封禁状态无法登录 |
| 普通玩家(INACTIVE) | t_user(本人) | 仅可激活账户 |
| 管理员(ADMIN) | 所有表 | ALL |

### 12.3 数据清理

| 数据项 | 清理策略 | 说明 |
|--------|----------|------|
| 过期存档 | 定时任务 | 超过30天的存档自动清理 |
| 文件存储 | 关联删除 | 数据库记录删除时同步删除文件 |
| 过期会话 | 定时任务 | 超过1小时的断开会话自动清理 |
| 旧消息 | 定时任务 | 超过30天的消息自动清理 |
| 过期锁 | 定时任务 | 超过超时时间的锁自动释放 |

