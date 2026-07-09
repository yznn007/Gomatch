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
| JSON 序列化 | org.json / Gson | - |
| 日志 | SLF4J + Logback | SLF4J 2.x / Logback 1.4+ |
| 密码存储 | 明文 | - |
| Redis 客户端 | Lettuce | - |

## 快速启动

### 环境要求

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 21 LTS（必须） | JavaFX 21 依赖，需配置 `JAVA_HOME` |
| Maven | 3.9+ | 项目构建工具，需配置 `PATH` |
| MySQL | 8.x | 数据库，账号：`gomatch`，密码：`gomatch` |
| Redis | 7.x（必须） | 会话持久化、排行榜缓存、匹配队列 |

### 启动步骤

```powershell
# 1. 安装 common 到本地 Maven 仓库（首次或 common 变更后必须执行）
mvn install -pl common -am

# 2. 确保 MySQL 和 Redis 已启动

# 3. 启动服务端（先启动，可按成员选择对应脚本）
.\scripts\ssc_server.ps1

# 4. 启动客户端（后启动，可按成员选择对应脚本）
.\scripts\ssc_client.ps1
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
│       ├── Protocol.java        # 协议常量
│       └── Message.java         # JSON 消息模型
├── database/                    # 数据库模块（工具类）
├── sql/                         # SQL 脚本
├── docs/                        # 文档目录
├── scripts/                     # 启动脚本目录
│   ├── *_client.ps1             # 客户端启动脚本（按成员命名）
│   └── *_server.ps1             # 服务端启动脚本（按成员命名）
└── pom.xml                      # Maven 父 POM
```

## 文档索引

项目所有设计、架构、数据库、规范以及模块开发文档均已整合在文档中心：

*   **[项目文档中心索引](docs/README.md)**：按阅读场景组织需求、架构、模块、UI、启动联调和实训资料。
*   **[客户端 UI 规范](docs/CLIENT_STYLE_GUIDE.md)**：双主题磨砂风格与可复用按钮、卡片、布局 CSS 模板。
*   **[common 协议文档](docs/MODULE_COMMON.md)**：JSON 消息协议、`Message` 模型与 `Protocol` 常量。

## 开发命令

### 编译

```powershell
# 编译服务端
mvn clean compile -pl server -am

# 编译客户端
mvn clean compile -pl client -am

# 安装 common 到本地 Maven 仓库
mvn install -pl common -am

# 运行测试
mvn test -pl server -am
```

### 手动启动

```powershell
# 启动服务端
mvn -f server/pom.xml exec:java -Dexec.mainClass="com.gomatch.server.ServerMain"

# 启动客户端（另一个终端）
mvn -f client/pom.xml javafx:run
```

## 注意事项

1. **JDK 版本**：JavaFX 21 依赖 JDK 17+，必须使用 JDK 21
2. **数据库配置**：修改 `database/src/main/resources/database.properties` 配置数据库连接
3. **Redis 配置**：Redis 为必需组件，用于会话持久化、排行榜缓存和匹配队列
4. **端口配置**：服务端默认监听 8888 端口

