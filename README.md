# Gomatch

网络五子棋联机对战平台。基于 **JavaFX + Socket 多线程服务端 + JDBC + Redis** 架构，支持玩家实时对弈、房间管理、聊天、观战、战绩排行等功能。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 LTS |
| 客户端 UI | JavaFX (FXML + CSS) | 21 |
| 网络通信 | Java Socket（TCP） | JDK 内置 |
| 并发模型 | Java 原生多线程 | JDK 内置 |
| 数据持久化 | JDBC + MySQL | MySQL 8.x |
| 缓存 | Redis | 7.x |
| 连接池 | HikariCP | 5.x |
| JSON 序列化 | Gson | 2.10+ |
| 日志 | SLF4J + Logback | SLF4J 2.x / Logback 1.4+ |
| 密码加密 | BCrypt | - |
| Redis 客户端 | Lettuce | - |

## 功能特性

### 核心功能
- ✅ 用户注册/登录/退出登录
- ✅ 房间管理（创建、加入、离开、密码房间）
- ✅ 实时对弈（15×15 棋盘、落子同步、五连判定）
- ✅ 聊天系统（房间聊天、消息持久化）
- ✅ 观战功能（实时观看对局）
- ✅ 战绩查询（历史对局记录）
- ✅ 排行榜（积分排名、胜率统计）
- ✅ 个人中心（用户信息、修改密码）

### 进阶功能
- ✅ 快速匹配（自动配对对手）
- ✅ 排位匹配（ELO 积分算法）
- ✅ 人机对战（简单 AI 启发式评分）
- ✅ 悔棋/求和（双方协商）
- ✅ 断线重连（5 分钟会话保留）
- ✅ 好友系统（添加、接受、删除好友）
- ✅ 棋局保存/加载（支持存档管理）
- ✅ 观战记录（历史观战查询）

### 后端优化
- ✅ Redis 会话持久化（服务重启不丢会话）
- ✅ Redis 排行榜缓存（O(logN) 查询性能）
- ✅ Redis 匹配队列（跨服务器匹配）
- ✅ 房间定时持久化（自动备份到数据库）
- ✅ 排行榜物化表（定时刷新）

## 快速启动

### 环境要求
- JDK 21（必须，JavaFX 21 依赖）
- MySQL 8.x（数据库）
- Redis 7.x（缓存，可选但推荐）

### 启动步骤

```powershell
# 1. 启动服务端（先启动）
.\scripts\run-server.ps1

# 2. 启动客户端（后启动）
.\scripts\run-client.ps1
```

### 手动启动

```powershell
# 编译项目
mvn clean compile -pl server -am

# 启动服务端
mvn -f server/pom.xml exec:java -Dexec.mainClass="com.gomatch.server.ServerMain"

# 启动客户端（另一个终端）
mvn -f client/pom.xml javafx:run
```

## 项目结构

```
Gomatch/
├── client/                      # 客户端模块（JavaFX）
│   ├── src/main/java/com/gomatch/client/
│   │   ├── ui/                  # FXML Controller + ScreenManager
│   │   ├── network/             # Socket 连接、消息分发
│   │   └── model/               # 数据模型
│   └── src/main/resources/
│       ├── fxml/                # FXML 布局文件
│       └── css/style.css        # 全局样式
├── server/                      # 服务端模块（多线程 Socket）
│   ├── src/main/java/com/gomatch/server/
│   │   ├── handler/             # ClientHandler（协议解析）
│   │   ├── room/                # Room + RoomManager
│   │   ├── game/                # Game（棋盘、AI）
│   │   ├── session/             # SessionManager（会话管理）
│   │   ├── redis/               # RedisManager（缓存）
│   │   └── dao/                 # 数据访问层
│   └── src/main/resources/      # 配置文件
├── common/                      # 公共模块（协议定义）
│   └── src/main/java/com/gomatch/common/
│       └── Protocol.java        # 协议常量与编解码
├── database/                    # 数据库模块（工具类）
├── sql/                         # SQL 脚本
├── docs/                        # 文档目录
├── scripts/                     # 启动脚本目录
│   ├── run-client.ps1           # 客户端启动脚本
│   ├── run-server.ps1           # 服务端启动脚本
│   ├── ssc_client.ps1           # 客户端启动脚本 (ssc)
│   └── ssc_server.ps1           # 服务端启动脚本 (ssc)
└── pom.xml                      # Maven 父 POM
```

## 文档索引

### 模块详细文档

| 文件 | 说明 |
|------|------|
| [docs/MODULE_COMMON.md](docs/MODULE_COMMON.md) | **common 模块**：协议常量全览、报文格式、编解码工具方法 |
| [docs/MODULE_CLIENT.md](docs/MODULE_CLIENT.md) | **client 模块**：JavaFX 界面、网络层、场景管理、Controller 一览、AI 说明 |
| [docs/MODULE_SERVER.md](docs/MODULE_SERVER.md) | **server 模块**：服务器架构、DAO 层、房间管理、Session、Redis 缓存、游戏流程 |

### 项目文档

| 文件/目录 | 说明 |
|-----------|------|
| [docs/README.md](docs/README.md) | **项目文档中心索引** (全文档总揽与索引导航) |
| [docs/requirements.md](docs/requirements.md) | 需求与系统设计文档 |
| [docs/TECHNICAL_ARCHITECTURE.md](docs/TECHNICAL_ARCHITECTURE.md) | 技术架构文档（协议、类设计、数据库） |
| [docs/DEV_LOG.md](docs/DEV_LOG.md) | 详细开发过程与更新记录 |
| [docs/DEV_STANDARD.md](docs/DEV_STANDARD.md) | 项目团队开发规范 (Git Commit/分支标准) |
| [AGENTS.md](AGENTS.md) | AI Agent 自动化开发上手指引与约定 |
| [docs/SERVER_STARTUP.md](docs/SERVER_STARTUP.md) | 服务端启动指南（环境配置、启动命令、常见问题） |
| [sql/init.sql](sql/init.sql) | MySQL 数据库初始化建表脚本 |
| [scripts/run-client.ps1](scripts/run-client.ps1) | 客户端一键启动脚本 |
| [scripts/run-server.ps1](scripts/run-server.ps1) | 服务端一键启动脚本 |
| [scripts/ssc_client.ps1](scripts/ssc_client.ps1) | 客户端一键启动脚本 (ssc) |
| [scripts/ssc_server.ps1](scripts/ssc_server.ps1) | 服务端一键启动脚本 (ssc) |

## 开发命令

```powershell
# 编译服务端
mvn clean compile -pl server -am

# 编译客户端
mvn clean compile -pl client -am

# 安装 common 到本地 Maven 仓库
mvn install -pl common -am

# 运行服务端
mvn -f server/pom.xml exec:java -Dexec.mainClass="com.gomatch.server.ServerMain"

# 运行客户端
mvn -f client/pom.xml javafx:run

# 运行测试
mvn test -pl server -am
```

## 注意事项

1. **JDK 版本**：JavaFX 21 依赖 JDK 17+，必须使用 JDK 21
2. **数据库配置**：修改 `database/src/main/resources/database.properties` 配置数据库连接
3. **Redis 配置**：Redis 为可选配置，用于会话持久化和排行榜缓存
4. **端口配置**：服务端默认监听 8888 端口

