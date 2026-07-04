# Gomatch — AGENTS.md

## 项目本质

学生实训项目，JDK 21 + **JavaFX** + Socket 多线程服务端 + JDBC + MySQL。Maven 多模块。

## 架构

- **三模块**：`client/`（JavaFX FXML + CSS）、`server/`（多线程 Socket）、`common/`（协议常量）
- **通信**：自定义文本协议 `COMMAND|param1|param2`，TCP 长连接
- **并发**：每连接一个 `ClientHandler` 线程，`Room` 对象内 synchronized 隔离

## 运行命令

```powershell
# 1. 安装 common 到本地 Maven 仓库（首次或 common 变更后）
$env:JAVA_HOME = "C:\Users\31816\scoop\apps\openjdk21\current"; mvn install -pl common -am

# 2. 启动客户端
.\run-client.ps1

# 或手动：
mvn -f client/pom.xml javafx:run
```

**关键**：JavaFX 21 依赖 JDK 17+，Maven 默认跑在 JDK 11（scoop 默认），必须设 `JAVA_HOME` 指向 JDK 21。`run-client.ps1` 已内置此逻辑。

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
- 白色极简风格，全局 CSS 禁用卡片阴影/渐变，用 `Separator` 分割区域
- 按钮区分 `btn-primary`（蓝）、`btn-secondary`（灰）、`btn-danger`（红）、`btn-link`
- 切角形状用 `-fx-shape` SVG 路径，非 `-fx-border-radius`
- hover/pressed 状态显式写三个伪类，不要用 derive

### FXML ↔ Controller 映射

`fx:id="xxx"` ↔ `@FXML private Type xxx;`
`onAction="#handleXxx"` ↔ `@FXML private void handleXxx()`
`fx:controller="com.gomatch.client.ui.XxxController"`

### 页面路由

```
LoginUI → 登录成功/浏览模式 → LobbyUI
LobbyUI → 对战 → CasualLobbyUI（房间列表+观战）
LobbyUI → 排位 → RankedLobbyUI（匹配按钮）
LobbyUI → 个人中心 → ProfileUI（含对战记录）
LobbyUI → 左上角头像 → ProfileUI
LobbyUI → 排行榜按钮 → 左侧抽屉展开
所有子页面 → 返回 → LobbyUI
```

## 分支

当前在 `feat/client-business`（前端业务开发），从 `dev` 切出。

## 文档维护

| 文档 | 触发时机 |
|------|----------|
| `docs/DEV_LOG.md` | 每次完成任务后追加日志 |
| `docs/requirements.md` | 需求/设计变更时同步 |
| `AGENTS.md` | 命令/架构/约定变化时 |
| `README.md` | 项目信息变化时 |
