# Gomatch 项目文档中心

本文档是 Gomatch 的文档入口。新成员优先从这里进入，不需要在 `docs/` 目录里逐个猜文件用途。

## 0. 文档职责边界

为减少重复维护，后续按下面的“唯一权威来源”更新：

| 信息类型 | 唯一权威来源 |
|----------|--------------|
| 产品目标、需求范围、功能编号、验收标准、核心流程 | `PRD.md` |
| 跨模块架构、运行链路、模块边界 | `TSD.md` |
| JSON 协议模型、命令常量、消息示例 | `COMMON.md` |
| 客户端路由、Controller、网络层、预览模式 | `CLIENT.md` |
| 服务端 Handler、Room、匹配、Session、DAO | `SERVER.md` |
| MySQL 表、Redis Key、索引、事务、存储方案 | `DB_DESIGN.md` |
| JavaFX 视觉、主题、组件模板 | `UI_GUIDELINE.md` |
| 启动命令、阅读顺序、资料入口 | 本文档 |

其他文档需要引用这些来源，不再复制完整表结构、协议清单或启动命令。

## 1. 推荐阅读顺序

| 场景 | 推荐阅读 |
|------|----------|
| 快速了解项目 | [根 README](../README.md) -> [PRD.md](PRD.md) -> [TSD.md](TSD.md) |
| 启动和联调 | 本文档第 6 节 -> [CLIENT.md](CLIENT.md) -> [SERVER.md](SERVER.md) |
| 做客户端 UI | [UI_GUIDELINE.md](UI_GUIDELINE.md) -> [CLIENT.md](CLIENT.md) -> `client/src/main/resources/css/style.css` |
| 改通信协议 | [COMMON.md](COMMON.md) -> [CLIENT.md](CLIENT.md) -> [SERVER.md](SERVER.md) |
| 改数据库或缓存 | [DB_DESIGN.md](DB_DESIGN.md) -> [SERVER.md](SERVER.md) -> `sql/init.sql` |
| 写实训报告 | [PRD.md](PRD.md) -> [TSD.md](TSD.md) -> [DB_DESIGN.md](DB_DESIGN.md) -> 官方模板 |

## 2. 核心需求与架构

| 文档 | 用途 | 维护重点 |
|------|------|----------|
| [PRD.md](PRD.md) | 产品目标、需求范围、功能清单、验收标准、核心流程 | 需求或产品目标变化时同步 |
| [TSD.md](TSD.md) | 总体架构、核心链路、模块边界 | 跨模块设计变化时同步，不重复协议/数据库明细 |
| [DB_DESIGN.md](DB_DESIGN.md) | MySQL 表结构、Redis Key、持久化和缓存设计 | 表结构、索引、Redis 策略变化时同步 |

## 3. 模块文档

| 文档 | 对应模块 | 主要内容 |
|------|----------|----------|
| [COMMON.md](COMMON.md) | `common/` | `Protocol` 常量、`Message` JSON 协议模型、客户端/服务端共享消息格式 |
| [CLIENT.md](CLIENT.md) | `client/` | JavaFX 启动流程、ScreenManager、网络层、页面路由、Controller、AI、UI 规范 |
| [SERVER.md](SERVER.md) | `server/` | Socket 多线程服务端、ClientHandler、Room/RoomManager、DAO、Redis、ELO、断线重连 |

## 4. 开发规范与 UI 规范

| 文档 | 用途 |
|------|------|
| [DEV_GUIDELINE.md](DEV_GUIDELINE.md) | Git 提交、分支、代码规范、FXML/Controller 映射、文档维护约定 |
| [UI_GUIDELINE.md](UI_GUIDELINE.md) | 双主题磨砂 UI、底划线控件、响应式布局、弹窗规范、可复用按钮/卡片/布局 CSS 模板 |
| [DEV_LOG.md](DEV_LOG.md) | 开发过程记录；每次完成任务后追加一行 |
| [releases/README.md](releases/README.md) | 客户端版本说明：标签同名 Markdown、Release 编写模板 |
| [../AGENTS.md](../AGENTS.md) | AI 协作约定、运行命令、项目架构、UI 约束、文档维护规则 |

## 5. 实训交付文档

> 以下文档按学校交付要求规划，部分尚未编写。对应官方模板见 `school_materials/`。

| 文档 | 状态 | 用途 | 对应学校模板 |
|------|------|------|-------------|
| [TEST.md](TEST.md) | 已编写 | 测试用例与缺陷记录：冒烟/边界/越权/并发 | `school_materials/测试用例表.pdf` |
| DEFENSE_GUIDE.md | 待编写 | 答辩 PPT 结构与内容说明、时间分配、追问预案 | `school_materials/答辩PPT结构与内容说明.pdf` |
| DELIVERY_SPEC.md | 待编写 | 项目交付文档清单与规范、部署说明骨架、自检 checklist | `school_materials/项目交付文档清单与规范.pdf` |
| ADJUSTMENT_PROMPTS.md | 待编写 | 局部调整场景提示词库：定位→改一处→验证的打法 | `school_materials/局部调整场景提示词库.pdf` |

## 6. 实训资料与交付材料

学校官方 PDF/PPT/DOC 原始资料存放在 `school_materials/`，保持原名。

| 文件 | 用途 |
|------|------|
| [00_参考案例_从需求到交付_总纲.pdf](school_materials/00_参考案例_从需求到交付_总纲.pdf) | 参考案例总纲 |
| [基于CS模式应用软件开发实训动员会-软件24.ppt](school_materials/基于CS模式应用软件开发实训动员会-软件24.ppt) | 学校实训说明与进度安排 |
| [CS实训_Java图形界面开发_十四款可选项目详解_v1.pdf](school_materials/CS实训_Java图形界面开发_十四款可选项目详解_v1.pdf) | 可选项目与技术点参考 |
| [CS实训_项目分组登记表.xlsx](school_materials/CS实训_项目分组登记表.xlsx) | 项目分组登记 |
| [A1-学生版讲义.pdf](school_materials/A1-学生版讲义.pdf) / [A1-学生实操手册.pdf](school_materials/A1-学生实操手册.pdf) | Java 图形界面与 C/S 基础讲义及实操 |
| [A2-学生版讲义.pdf](school_materials/A2-学生版讲义.pdf) / [A2-学生实操手册.pdf](school_materials/A2-学生实操手册.pdf) | A2 主题讲义及实操 |
| [A3-学生版讲义.pdf](school_materials/A3-学生版讲义.pdf) / [A3-学生实操手册.pdf](school_materials/A3-学生实操手册.pdf) | A3 主题讲义及实操 |
| [A4-学生版讲义.pdf](school_materials/A4-学生版讲义.pdf) / [A4-学生实操手册.pdf](school_materials/A4-学生实操手册.pdf) | A4 主题讲义及实操 |
| [A5-学生版讲义.pdf](school_materials/A5-学生版讲义.pdf) / [A5-学生实操手册.pdf](school_materials/A5-学生实操手册.pdf) | A5 主题讲义及实操 |
| [基于CS模式应用软件开发实训报告模板.doc](school_materials/基于CS模式应用软件开发实训报告模板.doc) | 结课报告模板 |
| [分工看板模板.pdf](school_materials/分工看板模板.pdf) | 分工看板模板 |
| [数据字典模板.pdf](school_materials/数据字典模板.pdf) | 数据字典模板 |
| [通信协议契约模板.pdf](school_materials/通信协议契约模板.pdf) | 通信协议契约模板 |
| [测试用例表.pdf](school_materials/测试用例表.pdf) | 测试用例表模板 |
| [答辩PPT结构与内容说明.pdf](school_materials/答辩PPT结构与内容说明.pdf) | 答辩 PPT 结构说明 |
| [项目交付文档清单与规范.pdf](school_materials/项目交付文档清单与规范.pdf) | 交付文档清单与规范 |
| [局部调整场景提示词库.pdf](school_materials/局部调整场景提示词库.pdf) | 局部调整场景提示词库 |

## 7. 启动与联调

### 7.1 环境要求

| 软件 | 版本要求 | 用途 |
|------|----------|------|
| JDK | 21 LTS | JavaFX 21 与后端运行环境 |
| Maven | 3.9+ | 多模块构建 |
| MySQL | 8.x | 用户、房间、战绩、好友、配置等持久化 |
| Redis | 7.x（必需） | 会话、排行榜、在线状态、匹配队列缓存 |

### 7.2 常用命令

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

### 7.3 数据库初始化

```bash
# macOS / Linux
./scripts/init-db.sh

# Windows PowerShell
.\scripts\init-db.ps1
```

数据库和 Redis 的运行时连接参数维护在项目根目录 `.env`，非敏感的连接池参数仍维护在 `database/src/main/resources/database.properties`。团队成员本机路径和 JDK 安装位置不同，文档命令中不要写死环境变量设置；优先使用 `scripts/` 下的启动脚本。

## 8. 文档维护规则

| 变更类型 | 必须同步 |
|----------|----------|
| 需求、功能范围、验收标准变化 | `PRD.md` |
| 架构、协议、跨模块流程变化 | `TSD.md`、对应模块文档 |
| 数据库表、Redis Key、DAO 行为变化 | `DB_DESIGN.md`、`SERVER.md` |
| 客户端页面、路由、网络层、UI 行为变化 | `CLIENT.md`、必要时同步 `UI_GUIDELINE.md` |
| UI 组件样式、主题、布局约束变化 | `UI_GUIDELINE.md`、必要时同步 `AGENTS.md` |
| 每次完成开发或文档整理 | `DEV_LOG.md` |

## 9. 当前文档状态说明

*   当前主协议是 JSON 行消息：`{"cmd":"COMMAND","data":{"key":"value"}}`。
*   `common/` 目前包含 `Protocol.java` 与 `Message.java`，不再使用旧的竖线分隔协议作为主路径。
*   服务端业务 DAO 以 `server/dao` 为准，`database/` 模块保留数据库实体/仓储参考代码。
*   `docs/school_materials/` 存放学校官方 PDF、PPT、DOC 原始资料，文件保持原名；`docs/` 根层保留项目协作文档和交付导出件。
