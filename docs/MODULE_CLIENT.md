# client 模块文档

> **模块标识**：`com.gomatch:client`  
> **职责**：JavaFX 图形界面客户端，实现全部用户交互界面、网络通信和本地 AI 逻辑。

---

## 1. 模块概览

```
client/src/main/
├── java/com/gomatch/client/
│   ├── AppConfig.java                  # 全局配置与会话状态
│   ├── ClientMain.java                 # JavaFX Application 入口
│   ├── ai/
│   │   └── GomokuAI.java               # 本地启发式 AI
│   ├── model/
│   │   ├── GameRecord.java             # 对战记录数据模型
│   │   ├── RoomInfo.java               # 房间信息数据模型
│   │   └── User.java                   # 用户数据模型
│   ├── network/
│   │   ├── ClientConfig.java           # 网络配置加载与持久化
│   │   ├── NetworkListener.java        # 网络消息监听器接口
│   │   └── ServerConnector.java        # TCP 连接管理与消息收发
│   └── ui/
│       ├── AboutController.java        # 关于界面
│       ├── AutoCleanupController.java  # 控制器基类（自动清理监听器）
│       ├── BoardController.java        # 棋盘对战界面
│       ├── CasualLobbyController.java  # 休闲大厅（房间列表+观战）
│       ├── CreateRoomController.java   # 创建房间界面
│       ├── GameOverController.java     # 对局结束弹窗
│       ├── LobbyController.java        # 主大厅（匹配入口+排行榜抽屉）
│       ├── LoginController.java        # 登录界面
│       ├── ProfileController.java      # 个人中心（对战记录+改密码）
│       ├── RankedLobbyController.java  # 排位大厅（段位+匹配按钮）
│       ├── RankingController.java      # 排行榜界面
│       ├── RecordController.java       # 对战记录详情
│       ├── RegisterController.java     # 注册界面
│       ├── RoomWaitingController.java  # 房间等待界面
│       ├── ScreenManager.java          # 全局场景/窗口管理器
│       └── ShapeGridBackground.java    # 动态粒子点阵背景
└── resources/
    ├── client-config.json              # 客户端默认配置文件
    ├── css/style.css                   # 全局 CSS 样式表
    ├── fonts/
    │   ├── MapleMono-NF-CN-Regular.ttf # 内置等宽字体（Regular）
    │   └── MapleMono-NF-CN-Bold.ttf    # 内置等宽字体（Bold）
    ├── fxml/                           # 各界面 FXML 文件
    └── images/                         # 图片资源
```

---

## 2. 启动流程

```
main() → Application.launch()
  └─ ClientMain.start(Stage)
       ├─ 加载内置字体 (Maple Mono NF CN)
       ├─ 创建 ServerConnector 单例
       ├─ 注册全局断线监听（DISCONNECT → 弹窗 → 跳回 LoginUI）
       ├─ ScreenManager.init(stage)
       └─ ScreenManager.switchTo("/fxml/LoginUI.fxml")
```

---

## 3. 核心类详解

### 3.1 `ClientMain` — 应用入口

**文件**：[ClientMain.java](../client/src/main/java/com/gomatch/client/ClientMain.java)  
**继承**：`javafx.application.Application`

| 成员 | 类型 | 说明 |
|------|------|------|
| `connector` | `static ServerConnector` | 全局唯一连接器，通过 `getConnector()` 获取 |
| `disconnectHandled` | `static volatile boolean` | 防止断线事件重复处理的标志 |
| `start(Stage)` | 方法 | JavaFX 应用启动入口，初始化字体、连接器、ScreenManager |
| `stop()` | 方法 | 应用退出时关闭 Socket 连接 |
| `getConnector()` | `static` | 全局获取 `ServerConnector` 实例 |

---

### 3.2 `AppConfig` — 全局配置与会话

**文件**：[AppConfig.java](../client/src/main/java/com/gomatch/client/AppConfig.java)  
**类型**：工具类（`final`，私有构造）

| 常量/字段 | 类型 | 值 / 说明 |
|-----------|------|-----------|
| `SERVER_HOST` | `String` | 通过 `ClientConfig` 动态初始化，可被修改 — 服务器地址 |
| `SERVER_PORT` | `int` | 通过 `ClientConfig` 动态初始化，可被修改 — 服务器端口 |
| `GSON` | `Gson` | 共享 Gson 单例（线程安全） |
| `currentUser` | `User` | 当前登录用户，登录成功后设置 |
| `previewMode` | `boolean` | 预览模式开关（无需服务器即可浏览界面） |

> [!TIP]
> 客户端启动时会自动通过 `ClientConfig` 初始化服务器地址。用户可以在“系统设置”的界面中动态修改并刷新当前 IP 状态。

---

### 3.3 `ServerConnector` — 网络层

**文件**：[ServerConnector.java](../client/src/main/java/com/gomatch/client/network/ServerConnector.java)

#### 主要字段

| 字段 | 说明 |
|------|------|
| `socket` | TCP Socket 连接 |
| `reader/writer` | 行读/行写流 |
| `listeners` | `CopyOnWriteArrayList<NetworkListener>` — 线程安全的监听器列表 |
| `heartbeatTimer` | 心跳定时器（每 30 秒发送，90 秒无响应判定断线） |
| `lastHost/lastPort` | 记录连接地址，用于断线重连 |

#### 关键参数

| 常量 | 值 | 说明 |
|------|----|------|
| `HEARTBEAT_INTERVAL_SEC` | `30` | 心跳发送间隔（秒） |
| `HEARTBEAT_TIMEOUT_SEC` | `90` | 超时未收到响应判定断线（秒） |
| `MAX_RECONNECT_ATTEMPTS` | `3` | 最大重连尝试次数 |
| `RECONNECT_DELAY_SEC` | `5` | 重连间隔（秒） |

#### 主要方法

| 方法 | 说明 |
|------|------|
| `connect(host, port)` | 建立 TCP 连接，启动接收线程和心跳 |
| `send(String... parts)` | 构建协议报文并发送（调用 `Protocol.build`） |
| `addListener(listener)` | 注册消息监听器（去重）|
| `removeListener(listener)` | 移除指定监听器 |
| `clearListeners()` | **切换页面前调用**，清除所有旧监听，防止内存泄漏 |
| `disconnect()` | 关闭连接，停止心跳和接收线程 |
| `isConnected()` | 检查当前连接状态 |

#### 消息分发流程

```
接收线程读到一行
  → Protocol.parse(line) 解析为 [cmd, args...]
  → Platform.runLater(() → 遍历 listeners.onMessage(cmd, args))
```

---

### 3.4 `NetworkListener` — 消息监听接口

**文件**：[NetworkListener.java](../client/src/main/java/com/gomatch/client/network/NetworkListener.java)

```java
public interface NetworkListener {
    void onMessage(String command, String[] args);
}
```

所有需要处理服务器消息的 Controller 均实现此接口，并在初始化时注册到 `ServerConnector`。

---

### 3.5 `ClientConfig` — 本地与包内配置管理

**文件**：[ClientConfig.java](../client/src/main/java/com/gomatch/client/network/ClientConfig.java)  
**类型**：静态配置读写类

负责加载和保存客户端的服务器 IP 及端口配置，避免不同启动路径对配置文件的依赖。

#### 双向存储路由机制
1. **默认包内配置**：从类路径下加载只读默认配置文件 `client-config.json`（采用 Java Properties 属性名键值对格式保存）。
2. **用户本地配置**：当用户在设置页面中更改服务器地址并应用时，配置将被自动写入用户主目录的隐藏文件夹：`${user.home}/.gomatch/config.json`（亦为 Properties 格式）。
3. **加载优先级**：启动时优先加载本地的 `config.json`，若文件不存在则降级加载包内默认的 `client-config.json`。

---

### 3.6 `ScreenManager` — 场景管理器

**文件**：[ScreenManager.java](../client/src/main/java/com/gomatch/client/ui/ScreenManager.java)  
**类型**：静态工具类（全局唯一 `Stage`）

#### 窗口配置

| 属性 | 值 |
|------|----|
| 默认宽度 | `900px` |
| 默认高度 | `640px` |
| 最小宽度 | `720px` |
| 最小高度 | `540px` |
| 窗口样式 | `StageStyle.UNDECORATED`（无系统边框，自定义标题栏） |

#### 主要方法

| 方法 | 说明 |
|------|------|
| `init(Stage)` | 初始化主窗口，设置无边框样式和最小尺寸 |
| `switchTo(fxmlPath)` | 加载 FXML，切换主场景；**切换前自动调用 `clearListeners()`** |
| `showDialog(fxmlPath)` | 创建模态弹窗（如 GameOver），不清理监听器 |
| `getStage()` | 获取主窗口 Stage |

#### 自定义标题栏特性

- **无边框沉浸式**：`StageStyle.UNDECORATED`，避免系统原生标题栏
- **最大化/还原**：自定义实现，贴合 `Screen.getPrimary().getVisualBounds()`（不遮挡任务栏）
- **拖拽移动**：标题栏区域拖拽支持最大化状态下自动还原
- **边缘拉伸调整**：6px 边缘热区，支持 8 个方向 `ResizeMode`
- **Windows Aero Snap 模拟**：
  - 拖到顶部 → 最大化
  - 拖到左边 → 左半屏
  - 拖到右边 → 右半屏

#### 层级结构（每次切换）

```
StackPane (wrapper)
├── ShapeGridBackground   ← 动态粒子点阵背景（最底层）
└── VBox (mainLayout)
    ├── HBox (customTitleBar)  ← 自定义标题栏
    └── Parent (FXML root)     ← 业务 UI
```

---

### 3.7 `ShapeGridBackground` — 动态背景

**文件**：[ShapeGridBackground.java](../client/src/main/java/com/gomatch/client/ui/ShapeGridBackground.java)

使用 JavaFX Canvas 绘制动态粒子点阵，粒子会响应鼠标位置产生排斥效果，营造沉浸式视觉体验。

---

## 4. 界面路由

```
LoginUI（默认首页）
  ├─ 登录成功   → LobbyUI
  ├─ 注册       → RegisterUI
  └─ 浏览模式   → LobbyUI（previewMode=true）

LobbyUI（主大厅）
  ├─ 对战       → CasualLobbyUI
  ├─ 排位       → RankedLobbyUI
  ├─ 个人中心   → ProfileUI
  ├─ 左上头像   → ProfileUI
  ├─ 排行榜按钮 → 左侧抽屉展开（StackPane 浮层）
  └─ 关于       → AboutUI

CasualLobbyUI（休闲大厅）
  ├─ 创建房间   → CreateRoomUI → RoomWaitingUI
  ├─ 快速匹配   → RoomWaitingUI / BoardUI
  ├─ 加入房间   → RoomWaitingUI
  └─ 观战       → BoardUI（只读）

RankedLobbyUI（排位大厅）
  └─ 匹配       → BoardUI

RoomWaitingUI（等待界面）
  └─ 游戏开始   → BoardUI

BoardUI（棋盘对战）
  └─ 对局结束   → GameOverUI（模态弹窗） → LobbyUI

ProfileUI（个人中心）
  └─ 返回       → LobbyUI
```

---

## 5. Controller 一览

### 5.1 `LoginController`

**FXML**：`/fxml/LoginUI.fxml`

| 关键方法 | 说明 |
|----------|------|
| `handleLogin()` | 连接服务器 → 发送 `LOGIN` → 等待 `LOGIN_OK`/`LOGIN_FAIL` |
| `handleBrowse()` | 设置 `previewMode=true`，跳至 LobbyUI |
| `handleRegister()` | 跳至 RegisterUI |

### 5.2 `RegisterController`

**FXML**：`/fxml/RegisterUI.fxml`

| 关键方法 | 说明 |
|----------|------|
| `handleRegister()` | 发送 `REGISTER` → 收到 `REGISTER_OK` 后自动登录 |

### 5.3 `LobbyController`

**FXML**：`/fxml/LobbyUI.fxml`

- 展示当前用户头像、昵称、积分
- 排行榜抽屉：以 `StackPane` 浮层展开，不挤压主内容
- 监听 `RANKING_LIST` 填充排行榜数据

### 5.4 `CasualLobbyController`

**FXML**：`/fxml/CasualLobbyUI.fxml`

- 请求 `ROOM_LIST` 拉取房间列表，显示于 TableView
- 支持创建/加入/快速匹配
- 房间行双击或选择后点击观战进入观战模式

### 5.5 `RankedLobbyController`

**FXML**：`/fxml/RankedLobbyUI.fxml`

- 展示当前段位和积分
- 发送 `RANKED_MATCH` 进入排位匹配队列

### 5.6 `RoomWaitingController`

**FXML**：`/fxml/RoomWaitingUI.fxml`

- 等待对手加入，展示房间 ID
- 收到 `GAME_START` 后跳转至 BoardUI 并传递颜色信息

### 5.7 `BoardController`

**FXML**：`/fxml/BoardUI.fxml`  
**实现**：`NetworkListener`

| 关键特性 | 说明 |
|----------|------|
| 动态棋盘 | Canvas 随容器尺寸缩放，`cellSize` 自动计算 |
| 落子 | 点击 Canvas → 发送 `MOVE|row|col` |
| 同步 | 收到 `MOVE_SYNC` → 更新 board 数组 → 重绘 |
| 聊天 | TextArea + TextField，收到 `CHAT` 追加并自动滚底 |
| 游戏结束 | 收到 `GAME_OVER` → `ScreenManager.showDialog(GameOverUI)` |
| 操作按钮 | 悔棋（`REQUEST_UNDO`）、求和（`REQUEST_DRAW`）、认输（`SURRENDER`） |

### 5.8 `ProfileController`

**FXML**：`/fxml/ProfileUI.fxml`

- TableView 展示对战记录（绑定 `GameRecord` 模型）
- 支持修改密码（发送 `CHANGE_PWD`）

### 5.9 `GameOverController`

**FXML**：`/fxml/GameOverUI.fxml`  
**显示方式**：模态弹窗（`ScreenManager.showDialog`）

- 显示胜负结果
- 返回大厅按钮 → 关闭弹窗 → 跳至 LobbyUI

### 5.10 `RankingController` / `RecordController` / `AboutController`

纯展示界面，从服务端或 AppConfig 获取数据后填充 UI 控件。

---

## 6. 数据模型

### `User`

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `long` | 用户唯一 ID |
| `username` | `String` | 用户名 |
| `score` | `int` | ELO 积分 |
| `winCount` | `int` | 胜场数 |
| `loseCount` | `int` | 败场数 |
| `drawCount` | `int` | 平局数 |
| `rank` | `int` | 排行榜名次 |
| `winRate` | `String` | 胜率字符串（如 `"75.0%"`） |

### `RoomInfo`

| 字段 | 类型 | 说明 |
|------|------|------|
| `roomId` | `String` | 房间唯一 ID（8位UUID前缀） |
| `roomName` | `String` | 房间名称 |
| `playerCount` | `int` | 当前玩家数 |
| `status` | `String` | 状态：`WAITING`/`PLAYING` |
| `hasPassword` | `boolean` | 是否设置密码 |

### `GameRecord`

| 字段 | 类型 | 说明 |
|------|------|------|
| `recordId` | `long` | 记录 ID |
| `opponentName` | `String` | 对手用户名 |
| `result` | `String` | 对局结果：`WIN`/`LOSE`/`DRAW` |
| `myColor` | `String` | 执子颜色：`BLACK`/`WHITE` |
| `createdAt` | `String` | 对局时间 |

---

## 7. 本地 AI (`GomokuAI`)

**文件**：[GomokuAI.java](../client/src/main/java/com/gomatch/client/ai/GomokuAI.java)

> [!NOTE]
> 客户端侧 AI 用于**预览模式**下的本地体验。服务端侧 `AIPlayer` 才是真正联网人机对战的 AI。

### 算法设计

- **评估范围**：只评估距现有棋子 2 格以内的空位（剪枝优化）
- **启发式评分**：对每个候选位置分别计算己方攻击得分和对方防守得分
- **优先级**：
  1. 自己能五连 → 立即落子（`attackScore >= 100000`）
  2. 对手能五连 → 必须防守
  3. 综合得分最高位置

| 方法 | 说明 |
|------|------|
| `findBestMove(board, aiColor)` | 返回 `{x, y}` 最优落子坐标；棋盘为空时返回天元 `{7,7}` |
| `evaluatePosition(board, x, y, color)` | 计算指定位置的启发式分数 |
| `hasNeighbor(board, x, y, radius)` | 判断候选位置周围是否有棋子 |

---

## 8. UI 风格规范

本项目采用**白色极简磨砂玻璃**设计语言，全局 CSS 文件路径：`/css/style.css`。

| 规范 | 说明 |
|------|------|
| 卡片样式 | `.glass-card`：乳白色透明磨砂，圆角 12px |
| 按钮样式 | 底线描边 + 悬浮遮罩，圆角 8px，无四面外边框 |
| Hover 反馈 | 背景填充板岩灰 `rgba(148,163,184,0.35)` |
| 按下反馈 | 高亮白 `rgba(255,255,255,0.95)` |
| 颜色体系 | 中性无色系（板岩灰、木炭黑），禁用鲜活天蓝色 |
| 字体 | Maple Mono NF CN（内置），回退到系统等宽字体 |
| 动态背景 | `ShapeGridBackground` 粒子点阵，响应鼠标位置 |

> [!IMPORTANT]
> FXML 命名空间**不加版本号**：使用 `xmlns="http://javafx.com/javafx"`（写 `/21` 会导致 `FXMLLoadException`）。

---

## 9. 预览模式

无服务器时，在登录页面点击"浏览模式"可启用预览模式：

- `AppConfig.setPreviewMode(true)`
- 各 Controller 的 `setPreviewMode(true)` 绕过网络调用
- 使用硬编码假数据填充 TableView 等控件

---

## 10. 启动命令

```powershell
# 方式一：使用本机脚本（推荐）
.\scripts\ssc_client.ps1

# 方式二：Maven 直接运行（需确保 JDK 21 已配置）
mvn -f client/pom.xml javafx:run
```
