# Gomatch 项目文档中心

本文档是 Gomatch 的文档入口。新成员优先从这里进入，不需要在 `docs/` 目录里逐个猜文件用途。

## 0. 文档职责边界

为减少重复维护，后续按下面的“唯一权威来源”更新：

| 信息类型 | 唯一权威来源 |
|----------|--------------|
| 需求范围、功能编号、验收标准 | `requirements.md` |
| 汇报/答辩用产品叙述、核心流程 | `PRD.md` |
| 跨模块架构、运行链路、模块边界 | `TECHNICAL_ARCHITECTURE.md` |
| JSON 协议模型、命令常量、消息示例 | `MODULE_COMMON.md` |
| 客户端路由、Controller、网络层、预览模式 | `MODULE_CLIENT.md` |
| 服务端 Handler、Room、匹配、Session、DAO | `MODULE_SERVER.md` |
| MySQL 表、Redis Key、索引、事务、存储方案 | `DATABASE_DESIGN.md` |
| JavaFX 视觉、主题、组件模板 | `CLIENT_STYLE_GUIDE.md` |
| 启动命令、阅读顺序、资料入口 | 本文档 |

其他文档需要引用这些来源，不再复制完整表结构、协议清单或启动命令。

## 1. 推荐阅读顺序

| 场景 | 推荐阅读 |
|------|----------|
| 快速了解项目 | [根 README](../README.md) -> [requirements.md](requirements.md) -> [TECHNICAL_ARCHITECTURE.md](TECHNICAL_ARCHITECTURE.md) |
| 启动和联调 | 本文档第 6 节 -> [MODULE_CLIENT.md](MODULE_CLIENT.md) -> [MODULE_SERVER.md](MODULE_SERVER.md) |
| 做客户端 UI | [CLIENT_STYLE_GUIDE.md](CLIENT_STYLE_GUIDE.md) -> [MODULE_CLIENT.md](MODULE_CLIENT.md) -> `client/src/main/resources/css/style.css` |
| 改通信协议 | [MODULE_COMMON.md](MODULE_COMMON.md) -> [MODULE_CLIENT.md](MODULE_CLIENT.md) -> [MODULE_SERVER.md](MODULE_SERVER.md) |
| 改数据库或缓存 | [DATABASE_DESIGN.md](DATABASE_DESIGN.md) -> [MODULE_SERVER.md](MODULE_SERVER.md) -> `sql/init.sql` |
| 写实训报告 | [PRD.md](PRD.md) -> [TECHNICAL_ARCHITECTURE.md](TECHNICAL_ARCHITECTURE.md) -> [DATABASE_DESIGN.md](DATABASE_DESIGN.md) -> 官方模板 |

## 2. 核心需求与架构

| 文档 | 用途 | 维护重点 |
|------|------|----------|
| [requirements.md](requirements.md) | 需求范围、功能清单、验收标准 | 需求范围或验收标准变化时同步 |
| [PRD.md](PRD.md) | 产品目标、用户价值、核心流程 | 面向报告和答辩的产品叙述，不重复技术细节 |
| [TECHNICAL_ARCHITECTURE.md](TECHNICAL_ARCHITECTURE.md) | 总体架构、核心链路、模块边界 | 跨模块设计变化时同步，不重复协议/数据库明细 |
| [DATABASE_DESIGN.md](DATABASE_DESIGN.md) | MySQL 表结构、Redis Key、持久化和缓存设计 | 表结构、索引、Redis 策略变化时同步 |

## 3. 模块文档

| 文档 | 对应模块 | 主要内容 |
|------|----------|----------|
| [MODULE_COMMON.md](MODULE_COMMON.md) | `common/` | `Protocol` 常量、`Message` JSON 协议模型、客户端/服务端共享消息格式 |
| [MODULE_CLIENT.md](MODULE_CLIENT.md) | `client/` | JavaFX 启动流程、ScreenManager、网络层、页面路由、Controller、AI、UI 规范 |
| [MODULE_SERVER.md](MODULE_SERVER.md) | `server/` | Socket 多线程服务端、ClientHandler、Room/RoomManager、DAO、Redis、ELO、断线重连 |

## 4. 开发规范与 UI 规范

| 文档 | 用途 |
|------|------|
| [DEV_STANDARD.md](DEV_STANDARD.md) | Git 提交、分支、代码规范、FXML/Controller 映射、文档维护约定 |
| [CLIENT_STYLE_GUIDE.md](CLIENT_STYLE_GUIDE.md) | 双主题磨砂 UI、底划线控件、响应式布局、弹窗规范、可复用按钮/卡片/布局 CSS 模板 |
| [DEV_LOG.md](DEV_LOG.md) | 开发过程记录；每次完成任务后追加一行 |
| [../AGENTS.md](../AGENTS.md) | AI 协作约定、运行命令、项目架构、UI 约束、文档维护规则 |

## 5. 实训资料与交付材料

| 文件 | 用途 |
|------|------|
| [基于CS模式应用软件开发实训动员会-软件24.ppt](school_materials/基于CS模式应用软件开发实训动员会-软件24.ppt) | 学校实训说明与进度安排 |
| [A1-学生实操手册.pdf](school_materials/A1-学生实操手册.pdf) | 官方实操步骤参考 |
| [A1-学生版讲义.pdf](school_materials/A1-学生版讲义.pdf) | Java 图形界面和 C/S 项目基础讲义 |
| [CS实训_Java图形界面开发_十四款可选项目详解_v1.pdf](school_materials/CS实训_Java图形界面开发_十四款可选项目详解_v1.pdf) | 可选项目与技术点参考 |
| [基于CS模式应用软件开发实训报告模板.doc](school_materials/基于CS模式应用软件开发实训报告模板.doc) | 结课报告模板 |
| [PRD.docx](PRD.docx) / [PRD.pdf](PRD.pdf) | PRD 的 Word/PDF 版本，适合提交或展示 |

## 6. 启动与联调

### 6.1 环境要求

| 软件 | 版本要求 | 用途 |
|------|----------|------|
| JDK | 21 LTS | JavaFX 21 与后端运行环境 |
| Maven | 3.9+ | 多模块构建 |
| MySQL | 8.x | 用户、房间、战绩、好友、配置等持久化 |
| Redis | 7.x（必需） | 会话、排行榜、在线状态、匹配队列缓存 |

### 6.2 常用命令

```powershell
# 安装 common 到本地 Maven 仓库（首次或 common 变更后）
mvn install -pl common -am

# 编译服务端
mvn clean compile -pl server -am

# 编译客户端
mvn clean compile -pl client -am

# 启动服务端（推荐脚本）
.\scripts\ssc_server.ps1

# 启动客户端（推荐脚本）
.\scripts\ssc_client.ps1
```

### 6.3 数据库初始化

```powershell
mysql -u root -p
source sql/init.sql
```

数据库和 Redis 连接参数维护在 `database/src/main/resources/database.properties`。团队成员本机路径和 JDK 安装位置不同，文档命令中不要写死环境变量设置；优先使用 `scripts/` 下的启动脚本。

## 7. 文档维护规则

| 变更类型 | 必须同步 |
|----------|----------|
| 需求、功能范围、验收标准变化 | `requirements.md`、`PRD.md` |
| 架构、协议、跨模块流程变化 | `TECHNICAL_ARCHITECTURE.md`、对应 `MODULE_*.md` |
| 数据库表、Redis Key、DAO 行为变化 | `DATABASE_DESIGN.md`、`MODULE_SERVER.md` |
| 客户端页面、路由、网络层、UI 行为变化 | `MODULE_CLIENT.md`、必要时同步 `CLIENT_STYLE_GUIDE.md` |
| UI 组件样式、主题、布局约束变化 | `CLIENT_STYLE_GUIDE.md`、必要时同步 `AGENTS.md` |
| 每次完成开发或文档整理 | `DEV_LOG.md` |

## 8. 当前文档状态说明

*   当前主协议是 JSON 行消息：`{"cmd":"COMMAND","data":{"key":"value"}}`。
*   `common/` 目前包含 `Protocol.java` 与 `Message.java`，不再使用旧的竖线分隔协议作为主路径。
*   服务端业务 DAO 以 `server/dao` 为准，`database/` 模块保留数据库实体/仓储参考代码。
*   `docs/school_materials/` 存放学校官方 PDF、PPT、DOC 原始资料，文件保持原名；`docs/` 根层保留项目协作文档和交付导出件。
