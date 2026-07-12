# Gomatch — AGENTS.md

## 项目本质

学生实训项目，JDK 21 + **JavaFX** + Socket 多线程服务端 + JDBC + MySQL + Redis。Maven 多模块。

## 架构

- **四模块**：`client/`（JavaFX FXML + CSS）、`server/`（多线程 Socket + 业务 DAO）、`common/`（JSON 消息与协议常量）、`database/`（数据库实体/仓储代码，当前服务端优先使用 `server/dao`）
- **通信**：JSON 消息协议 `{"cmd":"COMMAND","data":{"key":"value"}}`，TCP 长连接
- **并发**：每连接一个 `ClientHandler` 线程，`Room` 对象内 synchronized 隔离

## 运行命令

```powershell
# 1. 安装 common 到本地 Maven 仓库（首次或 common 变更后）
mvn install -pl common -am

# 2. 初始化数据库（首次）：执行 sql/init.sql 建表与种子数据

# 3. 启动服务端（需先启动 MySQL 与 Redis）
.\scripts\ssc_server.ps1

# 4. 启动客户端
.\scripts\ssc_client.ps1

# 或手动：
mvn -f client/pom.xml javafx:run
```

**关键**：JavaFX 21 依赖 JDK 17+，若系统默认 Java 不是 JDK 21，请确保 `JAVA_HOME` 指向 JDK 21。`scripts/` 目录下按成员命名的客户端脚本已内置此逻辑。

## 客户端架构

### 入口

```
ClientMain (extends Application)
  → ScreenManager.init(stage)
  → ScreenManager.switchTo("/fxml/LoginUI.fxml")
```

### 场景管理

`ScreenManager` 是唯一导航入口：
- `switchTo(fxmlPath)` — 切换主场景，保留窗口位置/大小，首次自动居中。返回 Controller。
- `showDialog(fxmlPath)` — 创建模态弹窗（如 GameOver）。
- 全局 CSS `/css/style.css` 自动注入每个 Scene。

### 网络层

`ServerConnector` — 单例，通过 `ClientMain.getConnector()` 获取：
- `connect(host, port)` → 启动接收线程 → `Platform.runLater` 分发消息
- 各 Controller 实现 `NetworkListener` 接口，注册到 connector

### 预览模式

无服务器时点登录页"浏览模式"可走通全部界面。各 Controller 通过 `setPreviewMode(true)` 绕过网络调用，用假数据填充。

### UI 约定

- FXML 命名空间 **不含版本号**：`xmlns="http://javafx.com/javafx"`（写 `/21` 会导致 FXML LoadException）
- 双主题磨砂风格：清透白磨砂原色作为基础 CSS，暗夜黑磨砂只通过 Scene 根节点 `.theme-dark` 覆盖层生效；禁止新增未加 `.theme-dark` 作用域的暗色全局覆盖
- 极简底划线控件：文本框与所有按钮元素（包含主次按钮、控制按钮、匹配卡片入口）高度统一样式，去除四面外边框与卡片阴影，仅留底部 1.5px 极简下划线描边
- 按钮圆角与交互反馈：统一使用 8px 圆角结构。鼠标悬停时使用板岩灰/冷灰 Cover 遮罩，点击按下（armed）时切换为轻量高光反馈；浅色与暗色分别在基础 CSS 和 `.theme-dark` 中定义
- 显式定义 hover/pressed/focused 伪类，全界面杜绝鲜活天蓝色和高饱和霓虹色；动态生成控件的内联文字色必须使用 `ScreenManager.themeColor(light, dark)` 保证主题切换后可读

### FXML ↔ Controller 映射

`fx:id="xxx"` ↔ `@FXML private Type xxx;`
`onAction="#handleXxx"` ↔ `@FXML private void handleXxx()`
`fx:controller="com.gomatch.client.ui.XxxController"`

### 页面路由

```
LoginUI → 登录成功/浏览模式 → LobbyUI
LoginUI → 忘记密码 → ForgotPasswordUI（邮箱→验证码→重置）
LobbyUI → 对战 → CasualLobbyUI（房间列表+观战）
LobbyUI → 排位 → RankedLobbyUI（匹配按钮）
LobbyUI → AI 对战 → AiLobbyUI（难度选择）
LobbyUI → 个人中心 → ProfileUI（含对战记录）
LobbyUI → 左上角头像 → ProfileUI
LobbyUI → 好友 → FriendsUI（好友列表/请求/添加）
LobbyUI → 设置 → SettingsUI（主题/语言/音量等）
LobbyUI → 排行榜按钮 → 左侧抽屉展开
LobbyUI → 关于 → AboutUI
LobbyUI → 管理 → ManagementUI（仅 ADMIN）
所有子页面 → 返回 → LobbyUI
```


## 文档维护

| 文档 | 触发时机 |
|------|----------|
| `docs/DEV_LOG.md` | 每次完成任务后追加日志 |
| `docs/PRD.md` | 需求/设计变更时同步 |
| `AGENTS.md` | 命令/架构/约定变化时 |
| `README.md` | 项目信息变化时 |

## 发布流程

发布前必须同步版本号，详见 [`docs/releases/README.md`](docs/releases/README.md)。

## 文档编写规范

### 命令示例中的环境变量

> **禁止**在任何文档（`.md`）的命令示例中写入硬编码的环境变量设置语句。

**❌ 错误写法**（禁止）：

```powershell
$env:JAVA_HOME = "<你的JDK21路径>"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
mvn -f server/pom.xml exec:java ...
```

**✅ 正确写法**（允许）：

```powershell
# 直接写命令，前提条件用文字说明
mvn -f server/pom.xml exec:java ...
```

**原因**：项目是**多人协作**团队项目，每位成员的 JDK 安装路径各不相同（`<你的JDK21路径>`、`$HOME\scoop\apps\openjdk21\current` 等）。文档中写死路径会导致其他成员照抄后报错，增加协作摩擦。

**约定**：

- 若某命令依赖 JDK 21，**用文字注明前提**（如"需确保 JDK 21 已配置"），而非内嵌 `$env:JAVA_HOME=...` 设置
- 推荐用户使用 `scripts/` 目录下的启动脚本，脚本内已内置跨平台的 JDK 自动检测逻辑
- 若确实需要展示环境变量设置方式，用**占位符**而非具体路径，例如：`$env:JAVA_HOME = "<你的JDK21路径>"`

## Git 提交规范

项目提倡采用**细粒度提交**，并结合 Conventional Commits 规范。

### 格式约定
提交的 Header 前缀使用标准的英文类型与范围，而**提交消息的主题（Subject）和主体（Body）必须使用中文描述**。

基本格式：
```
<type>(<scope>): <中文简短描述>

<中文详细描述（可选，描述具体修改背景或原因）>
```

### 常用类型（Type）
- `feat`：新增功能（如安全加固、新协议对接等）
- `fix`：Bug 修复（如 UI 渲染异常、Redis 启动报错处理等）
- `style`：仅 UI 样式/排版相关的调整（如 FXML 内联样式清理、CSS 圆角调整、物理动画调优等）
- `refactor`：代码重构，既不属于 Bug 修复也不属于新增功能（如解耦消息解析器、封装配置读取类等）
- `perf`：性能优化（如减少不必要的 UI 刷新、数据库查询优化等）
- `test`：测试用例编写或重构（如 `SessionPolicy` 规则判定测试等）
- `build`：构建工具或外部依赖修改（如 `pom.xml` 中引入插件或测试依赖）
- `docs`：文档修改（如 `DEV_LOG.md` 等文档更新）
- `chore`：其他辅助工具、构建过程或辅助文件的日常琐事

> 分支命名、版本规范、工作流等完整内容详见 [`docs/DEV_GUIDELINE.md`](docs/DEV_GUIDELINE.md)。

### 提交与合并要求
- **禁止直接推送**：开发人员在个人分支修改后，必须推送到远程仓库并通过提交 **Pull Request (PR)** 进行合并，严禁直接 Push 到 `dev` 或 `main` 分支。
- **细粒度 Commit**：禁止将不相关的多个模块修改（如安全逻辑、UI 样式、持久化配置等）打包进一个庞大粗糙的 Commit。
- **逻辑独立**：逻辑独立的修改必须单独提交，以便于回滚、审查和追溯历史。
