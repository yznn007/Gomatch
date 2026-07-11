# Gomatch

网络五子棋联机对战平台。基于 **JavaFX + Socket 多线程服务端 + JDBC + Redis** 架构，支持玩家实时对弈、房间管理、聊天、观战、战绩排行等功能。

## 技术栈

| 层级         | 技术                | 版本                     |
| ------------ | ------------------- | ------------------------ |
| 语言         | Java                | 21 LTS                   |
| 客户端 UI    | JavaFX (FXML + CSS) | 21                       |
| 网络通信     | Java Socket（TCP）  | JDK 内置                 |
| 并发模型     | Java 原生多线程     | JDK 内置                 |
| 数据持久化   | JDBC + MySQL        | MySQL 8.x                |
| 缓存         | Redis               | 7.x                      |
| 连接池       | HikariCP            | 5.x                      |
| JSON 序列化  | org.json / Gson     | -                        |
| 日志         | SLF4J + Logback     | SLF4J 2.x / Logback 1.4+ |
| 密码存储     | 明文                | -                        |
| Redis 客户端 | Lettuce             | -                        |

## 快速启动

### 环境要求

| 软件  | 版本要求       | 说明                                         |
| ----- | -------------- | -------------------------------------------- |
| JDK   | 21 LTS（必须） | JavaFX 21 依赖，需配置`JAVA_HOME`          |
| Maven | 3.9+           | 项目构建工具，需配置`PATH`                 |
| MySQL | 8.x            | 数据库运行凭据由根目录 `.env` 提供 |
| Redis | 7.x（必须）    | 会话持久化、排行榜缓存、匹配队列             |

### 启动步骤

```powershell
# 1. 启动 MySQL 和 Redis
# 终端 A - 启动 MySQL
net start MySQL80

# 终端 B - 启动 Redis
redis-server

# 2. 复制 `.env.example` 为 `.env`，填写 MySQL、Redis 与可选 OSS 凭据。
# 3. 初始化数据库（首次运行或数据库结构变更后执行）
mysql -u root -p < sql/init.sql

# 4. 安装 common 到本地 Maven 仓库（首次或 common 变更后必须执行）
mvn install -pl common -am

# 5. 启动服务端（先启动，可按成员选择对应脚本）
.\scripts\ssc_server.ps1

# 6. 启动客户端（后启动，可按成员选择对应脚本）
.\scripts\ssc_client.ps1
```

## 默认测试数据

`sql/init.sql` 's seed password is injected from local `.env`; the repository does not expose seed-account email addresses or passwords.

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

- **[INDEX](docs/README.md)**：项目文档入口

## 开发命令

```powershell
# 全量编译
mvn compile

# 编译服务端
mvn clean compile -pl server -am

# 编译客户端
mvn clean compile -pl client -am

# 安装 common 到本地 Maven 仓库
mvn install -pl common -am

# 运行测试
mvn test -pl server -am

# 启动服务端
mvn -f server/pom.xml exec:java -Dexec.mainClass="com.gomatch.server.ServerMain"

# 启动客户端
mvn -f client/pom.xml javafx:run
```

## 注意事项

1. **JDK 21**：JavaFX 21 依赖 JDK 17+，项目强制使用 JDK 21，启动脚本已内置 `JAVA_HOME` 检测。
2. **首次运行**：必须先执行 `mvn install -pl common -am` 和 `mysql -u root -p < sql/init.sql`。
3. **启动顺序**：MySQL → Redis → 初始化数据库 → 服务端 → 客户端，缺一不可。
4. **运行凭据**：将 `.env.example` 复制为 `.env`，填写 MySQL、Redis 与可选 OSS 变量；`.env` 不会提交到仓库，进程环境变量可覆盖它。
5. **端口**：服务端 TCP 8888、MySQL 3306、Redis 6379，确保未被占用。

