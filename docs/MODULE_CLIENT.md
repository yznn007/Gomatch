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
│   ├── audio/
│   │   └── BackgroundMusicManager.java # 客户端背景音乐循环播放管理器
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
│       ├── AiLobbyController.java      # 人机对战难度选择界面
│       ├── AsciiTextCanvas.java        # 登录页 ASCII 品牌动效画布
│       ├── AutoCleanupController.java  # 控制器基类（自动清理监听器）
│       ├── BoardController.java        # 棋盘对战界面
│       ├── CasualLobbyController.java  # 休闲大厅（房间列表+观战）
│       ├── ConfirmExitController.java  # 退出确认弹窗
│       ├── CreateRoomController.java   # 创建房间界面
│       ├── ElasticVolumeSlider.java    # 设置页弹性音量滑杆
│       ├── FriendsController.java      # 好友列表/请求/添加界面
│       ├── GameOverController.java     # 对局结束弹窗
│       ├── LobbyController.java        # 主大厅（匹配入口+排行榜抽屉）
│       ├── LoginController.java        # 登录界面
│       ├── ManagementController.java   # 存档管理与管理员控制台
│       ├── MessageDialogController.java # 项目定制消息弹窗
│       ├── PixelCard.java              # 主题卡片/粒子/悬浮交互组件
│       ├── ProfileController.java      # 个人中心（对战记录+改密码）
│       ├── RankedLobbyController.java  # 排位大厅（段位+匹配按钮）
│       ├── RankingController.java      # 排行榜界面
│       ├── RecordController.java       # 对战记录详情
│       ├── RegisterController.java     # 注册界面
│       ├── RoomWaitingController.java  # 房间等待界面
│       ├── ScreenManager.java          # 全局场景/窗口管理器
│       ├── ServerConfigDialogController.java # 服务器地址配置弹窗
│       ├── SettingsController.java     # 设置界面
│       └── ShapeGridBackground.java    # 动态粒子点阵背景
└── resources/
    ├── audio/
    │   └── lobby-theme.mp3             # 大厅/客户端背景音乐资源
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
       ├─ BackgroundMusicManager.playLobbyTheme()
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
| `start(Stage)` | 方法 | JavaFX 应用启动入口，初始化字体、连接器、ScreenManager，并启动循环背景音乐 |
| `stop()` | 方法 | 应用退出时关闭 Socket 连接并释放背景音乐播放器 |
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

### 3.3 `BackgroundMusicManager` — 背景音乐管理器

**文件**：[BackgroundMusicManager.java](../client/src/main/java/com/gomatch/client/audio/BackgroundMusicManager.java)

负责加载 `/audio/lobby-theme.mp3` 并通过 JavaFX `MediaPlayer` 循环播放。全局音乐与背景音乐的开关、音量值暴露为 JavaFX Property，设置页可直接同步到实际播放状态。若当前运行环境不支持该音频格式，管理器会释放播放器并保持客户端正常启动。

---

### 3.4 `ServerConnector` — 网络层

**文件**：[ServerConnector.java](../client/src/main/java/com/gomatch/client/network/ServerConnector.java)

#### 主要字段

| 字段 | 说明 |
|------|------|
| `socket` | TCP Socket 连接 |
| `reader/writer` | 行读/行写流 |
| `listeners` | `CopyOnWriteArrayList<NetworkListener>` — 线程安全的监听器列表 |
| `lastHost/lastPort` | 记录连接地址，用于断线重连 |

#### 关键参数

| 常量 | 值 | 说明 |
|------|----|------|
| `MAX_RECONNECT_ATTEMPTS` | `3` | 最大重连尝试次数 |
| `RECONNECT_DELAY_SEC` | `5` | 重连间隔（秒） |

#### 主要方法

| 方法 | 说明 |
|------|------|
| `connect(host, port)` | 建立 TCP 连接，启动接收线程 |
| `send(Message msg)` | 发送 JSON 消息对象 |
| `send(String cmd)` | 发送仅含命令的消息（如认领 `"SURRENDER"`） |
| `addListener(listener)` | 注册消息监听器（去重）|
| `removeListener(listener)` | 移除指定监听器 |
| `clearListeners()` | **切换页面前调用**，清除所有旧监听，防止内存泄漏 |
| `disconnect()` | 关闭连接，停止接收线程 |
| `isConnected()` | 检查当前连接状态 |

#### 消息分发流程

```
接收线程读到一行
  → Message.fromJson(line) 解析为 Message 对象
  → Platform.runLater(() → 遍历 listeners.onMessage(msg))
```

---

### 3.5 `NetworkListener` — 消息监听接口

**文件**：[NetworkListener.java](../client/src/main/java/com/gomatch/client/network/NetworkListener.java)

```java
import com.gomatch.common.Message;

public interface NetworkListener {
    void onMessage(Message msg);
}
```

所有需要处理服务器消息的 Controller 均实现此接口，并在初始化时注册到 `ServerConnector`。消息通过 `msg.getCmd()` 获取命令、`msg.getData()` 获取 `JSONObject` 数据体。

---

### 3.6 `ClientConfig` — 本地与包内配置管理

**文件**：[ClientConfig.java](../client/src/main/java/com/gomatch/client/network/ClientConfig.java)  
**类型**：静态配置读写类

负责加载和保存客户端的服务器 IP 及端口配置，避免不同启动路径对配置文件的依赖。

#### 双向存储路由机制
1. **默认包内配置**：从类路径下加载只读默认配置文件 `client-config.json`（采用 Java Properties 属性名键值对格式保存）。
2. **用户本地配置**：当用户在设置页面中更改服务器地址并应用时，配置将被自动写入用户主目录的隐藏文件夹：`${user.home}/.gomatch/config.json`（亦为 Properties 格式）。
3. **加载优先级**：启动时优先加载本地的 `config.json`，若文件不存在则降级加载包内默认的 `client-config.json`。

---

### 3.7 `ScreenManager` — 场景管理器

**文件**：[ScreenManager.java](../client/src/main/java/com/gomatch/client/ui/ScreenManager.java)  
**类型**：静态工具类（全局唯一 `Stage`）

#### 窗口配置

| 属性 | 值 |
|------|----|
| 默认宽度 | `1280px` |
| 默认高度 | `720px` |
| 最小宽度 | `1280px` |
| 最小高度 | `720px` |
| 窗口样式 | `StageStyle.UNDECORATED`（无系统边框，自定义标题栏） |

#### 主要方法

| 方法 | 说明 |
|------|------|
| `init(Stage)` | 初始化主窗口，设置无边框样式和 1280 x 720 最小尺寸 |
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

#### 响应式布局策略

`ScreenManager` 只负责窗口、标题栏、背景层和场景切换，不再对 `mainLayout` 挂载整页 `Scale` 变换。各 FXML 页面通过自身的布局约束响应窗口尺寸变化，避免固定参考尺寸带来的文字模糊、点击区域偏移和内容被锁死。

#### 层级结构（每次切换）

```
StackPane (wrapper)
├── ShapeGridBackground   ← 动态粒子点阵背景（最底层）
└── VBox (mainLayout)
    ├── HBox (customTitleBar)  ← 自定义标题栏
    └── Parent (FXML root)     ← 业务 UI
```

---

### 3.8 `ShapeGridBackground` — 动态背景

**文件**：[ShapeGridBackground.java](../client/src/main/java/com/gomatch/client/ui/ShapeGridBackground.java)

使用 JavaFX Canvas 绘制动态粒子点阵，粒子会响应鼠标位置产生排斥效果，营造沉浸式视觉体验。背景 Canvas 在尺寸变化小于 1px 时跳过重新设置宽高，避免窗口拉伸期间重复触发布局和绘制。

---

## 4. 界面路由

```
LoginUI（默认首页）
  ├─ 登录成功   → LobbyUI
  ├─ 注册       → RegisterUI
  └─ 浏览模式   → LobbyUI（previewMode=true）

LobbyUI（主大厅）
  ├─ 对战       → CasualLobbyUI
  ├─ AI 对战    → AiLobbyUI
  ├─ 排位       → RankedLobbyUI
  ├─ 好友       → FriendsUI
  ├─ 管理       → ManagementUI（仅 ADMIN 角色显示入口）
  ├─ 设置       → SettingsUI
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
- 管理入口绑定 `managementBtn`，仅当 `User.role` 为 `ADMIN` 时显示并参与布局

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
| 动态棋盘 | Canvas 随容器尺寸缩放，`cellSize` 自动计算，并跳过 0 尺寸与重复尺寸重绘 |
| 落子 | 点击 Canvas → 发送 `MOVE|row|col` |
| 同步 | 收到 `MOVE_SYNC` → 更新 board 数组 → 重绘 |
| 聊天 | TextArea + TextField，收到 `CHAT` 追加并自动滚底 |
| 对手信息 | 收到 `GAME_START` → 解析 `blackUsername/whiteUsername/blackScore/whiteScore` 显示对手真实用户名和积分 |
| 积分变化 | 收到 `GAME_OVER` → 解析 `data.scoreChanges` 提取本局积分变化，传递给结算弹窗 |
| 游戏结束弹窗 | `showGameOverDialog()` 使用 `Stage.show()` 非阻塞模式，保持网络消息接收能力（支持再来一局） |
| 操作按钮 | 悔棋（`REQUEST_UNDO`）、求和（`REQUEST_DRAW`）、认输（`SURRENDER`） |
| 再来一局 | 发送 `REQUEST_REMATCH` 请求对手；监听 `REMATCH_REQUESTED/ACCEPTED/REJECTED` 处理对方响应 |
| AI 对战结果 | GAME_OVER 处理增加 AI 模式特殊判断（winnerId="-1"时判定为对手胜），正确显示黑方胜/白方胜/平局 |
| 棋谱回放 | `replayMoves()` 支持 JSON 格式棋谱（检测 `[` 开头自动切换解析方式），兼容旧格式分号分隔 |

#### 消息监听（Rematch 相关）

| 消息 | 处理逻辑 |
|------|----------|
| `GAME_START` | 解析 `blackUsername/blackScore/whiteUsername/whiteScore`，调用 `setGameInfo()` 显示双方用户名和积分 |
| `GAME_OVER` | 解析 `scoreChanges` 数组获取积分变化；调用 `showGameOverDialog()` 弹出非阻塞结算弹窗 |
| `REMATCH_REQUESTED` | 收到对手的再来一局请求 → `showConfirmDialog` → 发送 `REPLY_REMATCH|1` 或 `0` |
| `REMATCH_ACCEPTED` | 双方同意 → 关闭结算弹窗，等待新 `GAME_START` 重置棋盘 |
| `REMATCH_REJECTED` | 对手拒绝 → 关闭结算弹窗，返回大厅 |

#### 核心新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `gameOverDialogStage` | `Stage` | 持有结算弹窗引用，用于再来一局时关闭旧弹窗 |
| `blackPlayerScoreLabel` / `whitePlayerScoreLabel` | `Label` | 双方积分显示控件 |
| `rematchRequested` | `boolean` | 是否已发送过再来一局请求（防重复） |

### 5.8 `ProfileController`

**FXML**：`/fxml/ProfileUI.fxml`

- TableView 展示对战记录（绑定 `GameRecord` 模型）
- 支持修改密码（发送 `CHANGE_PWD`）

### 5.9 `GameOverController`

**FXML**：`/fxml/GameOverUI.fxml`  
**显示方式**：非阻塞弹窗（`Stage.show()`，由 `BoardController.showGameOverDialog()` 管理）

| 关键方法 | 说明 |
|----------|------|
| `setResult(result, scoreChange)` | 设置胜负结果（WIN/LOSE/DRAW），并显示积分变化：正数绿色 `+N 积分`、负数红色 `N 积分`、零灰色 `+0 积分` |
| `handleGoLobby()` | 关闭弹窗 → 返回 LobbyUI |
| `handleReplay()` | 再来一局：**AI 模式**发送 `AI_MATCH` 重新开始人机对战；**PVP 模式**发送 `REQUEST_REMATCH` 请求对手 |

#### 新增 FXML 控件

| fx:id | 类型 | 说明 |
|-------|------|------|
| `scoreChangeLabel` | `Label` | 显示本局积分变化（颜色区分正/负/零） |
| `replayButton` | `Button` | "再来一局"按钮，`onAction="#handleReplay"` |

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

本项目采用**双主题磨砂玻璃**设计语言：清透白磨砂为基础原色，暗夜黑磨砂通过 `.theme-dark` 覆盖层启用。全局 CSS 文件路径：`/css/style.css`。

| 规范 | 说明 |
|------|------|
| 卡片样式 | `PixelCard` + `.glass-card`：表单页可用静态 PixelCard 外壳；浅色为乳白透明磨砂，暗色在 `.theme-dark` 下切换为近黑石墨磨砂 |
| 按钮样式 | 底线描边 + 悬浮遮罩，圆角 8px，无四面外边框 |
| 音量滑杆 | `ElasticVolumeSlider`：设置页的全局音乐、背景音乐、音效使用 JavaFX 自定义弹性滑杆，开关以无文字勾选框放在音量条左侧；大厅歌曲仅保留歌曲下拉选择 |
| Hover 反馈 | 浅色为板岩灰轻遮罩，暗色为冷灰/石墨遮罩 |
| 按下反馈 | 浅色为高亮白，暗色为低透明冷白高光 |
| 颜色体系 | 中性无色系（乳白、板岩灰、木炭黑、冷白），禁用鲜活天蓝色 |
| 字体 | Maple Mono NF CN（内置），回退到系统等宽字体 |
| 动态背景 | `ShapeGridBackground` 粒子点阵，响应鼠标位置，并对重复尺寸变化做跳过保护 |

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
