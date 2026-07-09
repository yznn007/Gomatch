# Gomatch 项目文档中心

欢迎来到 Gomatch 五子棋对战平台的文档中心。本文档对项目现有的全部设计、规范、部署以及模块细节文档进行分类整理，方便团队成员进行协作开发。

---

## 1. 核心设计与规范文档

| 文档名称 | 格式 | 核心内容与阅读目的 |
| :--- | :---: | :--- |
| [Gomatch 联机平台需求文档](requirements.md) | `.md` | 项目概述、技术栈选型、详细功能范围定义、客户端/服务端各模块开发需求。 |
| [产品需求文档 (PRD)](PRD.md) | `.md` | 精简版功能列表、业务背景与实训目标定义。 |
| [技术架构设计文档](TECHNICAL_ARCHITECTURE.md) | `.md` | 总体技术架构设计、包结构划分、网络传输层设计以及各端核心类设计示意。 |
| [开发规范标准](DEV_STANDARD.md) | `.md` | 团队 Git Commit 规范（Conventional Commits）与 Git 分支开发约定。 |
| [客户端前端设计规范](CLIENT_STYLE_GUIDE.md) | `.md` | 双主题磨砂卡片、极简底线控件、8px圆角与交互反馈、自适应布局与沉浸式弹窗规范。 |
| [AI Agent 自动化开发上手指引](../AGENTS.md) | `.md` | AI 协助开发的命令约定、UI 风格设计约束（清透白磨砂/暗夜黑磨砂双主题）与文档更新约定。 |

---

## 2. 数据库设计与部署文档

| 文档名称 | 格式 | 核心内容与阅读目的 |
| :--- | :---: | :--- |
| [数据库设计说明书](DATABASE_DESIGN.md) | `.md` | 详细的数据表设计（E-R模型、索引、列属性）、Session持久化方案与棋局存档机制。 |
| [服务端启动与部署指南](#5-服务端启动与部署指南) | `.md` | 见本文档第 5 节：运行环境依赖、初始化数据库、编译启动命令、常见问题及测试账号。 |
| [MySQL 数据库初始化脚本](../sql/init.sql) | `.sql` | 包含所有数据库表结构、外键、索引及默认账号初始数据的 SQL 脚本。 |

---

## 3. 模块级详细技术文档

| 文档名称 | 格式 | 核心内容与阅读目的 |
| :--- | :---: | :--- |
| [MODULE_COMMON.md (公共模块)](MODULE_COMMON.md) | `.md` | 详细规范的通信协议指令字定义（包含 39 个指令常量）、协议文本格式、build/parse 工具方法使用。 |
| [MODULE_CLIENT.md (客户端模块)](MODULE_CLIENT.md) | `.md` | JavaFX 无边框沉浸式窗口拉伸算法、动态粒子背景、所有 12 个 UI 页面路由及 Controller 设计、启发式 GomokuAI 逻辑。 |
| [MODULE_SERVER.md (服务端模块)](MODULE_SERVER.md) | `.md` | 基于 Java Socket 的多线程连接处理模型、内存/Redis双写会话机制（Session）、ELO 段位积分公式、协议消息流序列图、数据库交互映射、并发安全锁机制、后台定时调度任务及代码量统计。 |
| [DEV_LOG.md (开发过程记录)](DEV_LOG.md) | `.md` | 项目开发历史日志，包含每一天具体的任务实现、Bug 修复和性能优化记录。 |

---

## 4. 实训辅助与参考材料

此部分包含学校实训官方分发的辅助参考 PDF、演示 PPT 及报告模板等资料：

- **动员会与实操指南**：
  - [基于CS模式应用软件开发实训动员会-软件24.ppt](基于CS模式应用软件开发实训动员会-软件24.ppt) —— 实训背景说明与进度安排
  - [A1-学生实操手册.pdf](A1-学生实操手册.pdf) —— 官方五子棋实操分步指南
  - [A1-学生版讲义.pdf](A1-学生版讲义.pdf) —— 核心基础技术知识讲义
- **参考资料与报告**：
  - [CS实训_Java图形界面开发_十四款可选项目详解_v1.pdf](CS实训_Java图形界面开发_十四款可选项目详解_v1.pdf) —— Java 界面开发技术点详解
  - [基于CS模式应用软件开发实训报告模板.doc](基于CS模式应用软件开发实训报告模板.doc) —— 实训结束需提交的结课报告范本

---

## 5. 服务端启动与部署指南

### 5.1 环境要求

| 软件 | 版本要求 | 用途 |
|------|----------|------|
| JDK | 21 LTS | 运行环境 |
| Maven | 3.9+ | 项目构建工具 |
| MySQL | 8.x | 数据库存储 |
| Redis | 7.x（必需） | 缓存、会话持久化、排行榜 |

### 5.2 数据库初始化

```powershell
# 登录 MySQL 并执行初始化脚本
mysql -u root -p
source sql/init.sql
```

配置 `database/src/main/resources/database.properties`：

```properties
db.url=jdbc:mysql://localhost:3306/gomatch?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
db.username=${GOMATCH_DB_USERNAME}
db.password=${GOMATCH_DB_PASSWORD}
db.pool.maxSize=20
db.pool.minIdle=5

# Redis 配置（必需，服务端启动前必须可连接）
redis.host=localhost
redis.port=6379
redis.password=${GOMATCH_REDIS_PASSWORD}
redis.timeout=3000
redis.database=0
```

### 5.3 编译与启动

```powershell
# 1. 安装公共模块
mvn install -pl common -am

# 2. 编译服务端
mvn clean compile -pl server -am

# 3. 启动服务端（脚本推荐）
.\scripts\ssc_server.ps1

# 或手动启动
mvn -f server/pom.xml exec:java -Dexec.mainClass="com.gomatch.server.ServerMain"

# 4. 启动客户端
.\scripts\ssc_client.ps1
```

### 5.4 启动成功标志

```
[main] INFO  c.g.server.ServerMain - Gomatch Server starting...
[main] INFO  c.g.server.ServerMain - Server listening on port 8888
[main] INFO  c.g.server.ServerMain - Database connected successfully
[main] INFO  c.g.server.redis.RedisManager - Redis connected: localhost:6379
[main] INFO  c.g.server.ServerMain - Gomatch Server started successfully!
```

### 5.5 默认端口

| 服务 | 端口 | 配置位置 |
|------|------|----------|
| 游戏服务端 | 8888 | ServerMain.java |
| MySQL | 3306 | database.properties |
| Redis | 6379 | database.properties |

### 5.6 常见问题

| 问题 | 解决方案 |
|------|----------|
| Java 版本不匹配 | 确保 `JAVA_HOME` 指向 JDK 21，或使用 scripts 脚本（已内置自动查找） |
| 数据库连接失败 | 确认 MySQL 已启动，检查 database.properties 用户名密码，确认 gomatch 库已创建 |
| Redis 连接失败 | 启动 `redis-server`，然后重启服务端 |
| 端口占用 | `netstat -ano \| findstr :8888` 查找并 `taskkill /PID <PID> /F` |
| Maven 依赖慢 | 配置阿里云镜像（`~/.m2/settings.xml`） |

### 5.7 测试账号

| 用户名 | 密码 | 角色 | 积分 |
|--------|------|------|------|
| admin | password | ADMIN | 9999 |
| player1 | password | PLAYER | 1000 |
| player2 | password | PLAYER | 1000 |

### 5.8 调试模式

修改 `server/src/main/resources/logback.xml` 日志级别为 `DEBUG`，或使用远程调试：

```powershell
mvn -f server/pom.xml exec:java \
    -Dexec.mainClass="com.gomatch.server.ServerMain" \
    -Dexec.args="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

---

## 6. Windows 终端中文乱码修复指南

### 6.1 问题现象

服务端日志中的中文字符在 Windows 终端（PowerShell / CMD）中显示为乱码。

### 6.2 原因分析

| 组件 | 编码 | 说明 |
|------|------|------|
| Java 源代码 | UTF-8 | `.java` 文件保存为 UTF-8 |
| logback.xml | UTF-8 | `<charset>UTF-8</charset>` 已配置 |
| JVM 默认编码 | GBK | Windows 中文系统 `file.encoding` 默认 GBK |
| Windows 终端 | GBK | 代码页 936（简体中文 GBK） |

logback 将 UTF-8 字符串编码为字节流写入 stdout，但终端以 GBK 解码，导致乱码。

### 6.3 修复方案

在 `scripts/ssc_server.ps1` 中增加两项配置：

```powershell
# 1. 切换终端代码页为 UTF-8
chcp 65001 | Out-Null

# 2. 设置 JVM 默认编码为 UTF-8（已内置在启动脚本的 Maven exec 参数中）
"-Dfile.encoding=UTF-8"
```

### 6.4 注意事项

- `chcp 65001` 仅影响当前终端窗口，新开窗口需重新执行
- UTF-8 代码页（65001）在 Win10 1809+ 已完全支持
- 文件日志（`logs/gomatch-server.log`）不受终端编码影响，始终以 UTF-8 正确写入
