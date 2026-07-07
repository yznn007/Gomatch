# 开发过程记录

## 项目信息

- **项目名称**：Gomatch
- **开始日期**：2026-07-04
- **技术栈**：JavaFX (FXML + CSS) + Socket + JDBC + MySQL

---

## 任务清单

| # | 任务 | 状态 |
|---|------|------|
| 1 | 项目骨架搭建 | 已完成 |
| 2 | 数据库建表 | 已完成 |
| 3 | 通信协议定义 | 已完成 |
| 4 | 服务端主框架 | 已完成 |
| 5 | 注册 | 已完成 |
| 6 | 登录 | 已完成 |
| 7 | 客户端 UI 框架（JavaFX 迁移 + ScreenManager） | 已完成 |
| 8 | 登录/注册界面 | 已完成 |
| 9 | 游戏大厅界面（匹配/排位入口 + 排行榜抽屉） | 已完成 |
| 10 | 匹配大厅（房间列表 + 观战） | 已完成 |
| 11 | 排位大厅（段位 + 匹配按钮） | 已完成 |
| 12 | 创建房间 | 已完成 |
| 13 | 房间等待界面 | 已完成 |
| 14 | 棋盘 UI + 聊天 | 已完成 |
| 15 | 对局结束弹窗 | 已完成 |
| 16 | 个人中心（含对战记录+改密码） | 已完成 |
| 17 | 排行榜界面 | 已完成 |
| 18 | 关于界面 | 已完成 |
| 19 | 全局 CSS 白色极简主题 | 已完成 |
| 20 | 预览模式（无服务器浏览全部界面） | 已完成 |
| 21 | 落子同步 | 已完成 |
| 22 | 五连判胜 | 已完成 |
| 23 | 断线判负 + 认输 | 已完成 |
| 24 | 战绩入库 | 已完成 |
| 25 | 定时心跳发送，检测半开连接 | 已完成 |
| 26 | 房间聊天 | 已完成 |
| 27 | 观战功能 | 已完成 |
| 28 | 悔棋/求和 | 已完成 |
| 29 | 断线重连 | 已完成 |
| 30 | 简单人机对战 | 已完成 |
| 31 | 密码房间 | 已完成 |
| 32 | 排位匹配（ELO算法） | 已完成 |
| 33 | 好友系统 | 已完成 |
| 34 | 棋局保存 | 已完成 |
| 35 | 聊天消息持久化 | 已完成 |
| 36 | Socket会话持久化 | 已完成 |
| 37 | 观战记录 | 已完成 |
| 38 | 游戏操作记录 | 已完成 |
| 39 | 排行榜物化表 | 已完成 |
| 40 | Redis优化（会话/排行榜/匹配/在线状态） | 已完成 |
| 41 | 房间定时持久化 | 已完成 |
| 42 | 退出登录 | 已完成 |
| 43 | 服务器地址配置化 | 已完成 |
| 44 | NetworkListener 自动注销 | 已完成 |
| 45 | 注册/登录/改密码对接 | 已完成 |
| 46 | 房间创建/列表/观战流程对接 | 已完成 |
| 47 | 快速/排位匹配流程对接 | 已完成 |
| 48 | 对战记录页面 + 棋局回放 | 已完成 |
| 49 | 排行榜页面路由 | 已完成 |

### 状态说明

- 已完成 — 开发完毕且验证通过
- 正在进行 — 正在开发中
- 待测试 — 开发完成，待验证
- 未开工 — 尚未开始

---

## 日志

### Updated upstream（服务端 + 数据库）

| 日期 | 工作内容 | 问题/备注 |
|------|----------|-----------|
| 2026-07-04 | Contributor| 使用 JDK 21 |
| 2026-07-05 | Contributor| FXML 命名空间不能加版本号 |
| 2026-07-05 | Contributor| 首次居中，之后保留窗口位置 |
| 2026-07-05 | Contributor| 预览模式支持无服务器浏览 |
| 2026-07-05 | Contributor| |
| 2026-07-05 | Contributor|  |
| 2026-07-05 | Contributor| |
| 2026-07-05 | Contributor| |
| 2026-07-05 | Contributor| hover 仅变背景色，无特效 |
| 2026-07-05 | Contributor| mvn compile 通过 |
| 2026-07-05 | Contributor| mvn clean compile 通过 |
| 2026-07-05 | Contributor| mvn clean compile 通过，Git 提交规范代码 |
| 2026-07-06 | Contributor| 基于 Socket 多线程架构 |
| 2026-07-06 | Contributor| 定时任务自动刷新 |
| 2026-07-06 | Contributor| 使用 Lettuce Redis 客户端 |
| 2026-07-06 | Contributor| 修改 t_room.room_id 为 VARCHAR(50) |
| 2026-07-06 | Contributor| 编译验证通过 |
| 2026-07-06 | Contributor| 编译验证通过 |
| 2026-07-06 | Contributor| 脚本已加入 Git 追踪供团队成员使用 |
| 2026-07-06 | Contributor| 共覆盖 25+ 个 Java 类 |
| 2026-07-06 | Contributor| 提升团队协作可读性与文档查找效率 |
| 2026-07-06 | Contributor| 编译验证通过，完美匹配白色磨砂极简风格 |
| 2026-07-06 | Contributor| 本地数据库初始化及编译验证测试通过 |
| 2026-07-06 | Contributor| 页面顶栏对齐与排版优化通过 |
| 2026-07-06 | Contributor| 排行榜与好友页面磨砂侧栏化重构完成 |
| 2026-07-06 | Contributor| 预览模式匹配正常启动人机对局 |
| 2026-07-06 | Contributor| 房间大厅磨砂侧栏化重构完成 |
| 2026-07-06 | Contributor|roomId|roomName|playerCount`），从根本上避免了联机配对时客户端崩溃黑屏的问题。 | 联机协议兼容与对齐逻辑升级通过 |
| 2026-07-06 | Contributor| 房间等待大厅磨砂侧栏化重构完成，排位段位大厅还原成功 |
| 2026-07-06 | Contributor| 段位大厅完全还原，排位前置房间流对齐完毕 |
| 2026-07-06 | Contributor| 排位至大厅流转流程修正完成 |
| 2026-07-06 | Contributor| 棋盘侧栏折叠缩放功能开发完成 |
| 2026-07-06 | Contributor| 侧边栏折叠按钮精炼与拖拽拉伸功能完全落地 |
| 2026-07-06 | Contributor| 棋盘拉伸与折叠自适应自收缩逻辑修复完成 |
| 2026-07-06 | Contributor| 侧栏动态数学限制、等比自适应及聊天换行优化完成 |
| 2026-07-06 | Contributor| 侧栏按钮防裁剪垂直堆叠优化完成 |
| 2026-07-05 | Contributor| |
| 2026-07-05 | Contributor| 修复编译失败 |
| 2026-07-05 | Contributor| |
| 2026-07-06 | Contributor| 基于 Socket 多线程架构 |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| 定时任务自动刷新 |
| 2026-07-06 | Contributor| 使用 Lettuce Redis 客户端 |
| 2026-07-06 | Contributor| RoomDAO 每1分钟同步，修改 t_room.room_id 为 VARCHAR(50) |
| 2026-07-06 | Contributor| 减少约190行，编译验证通过 |
| 2026-07-06 | Contributor| 编译验证通过 |
| 2026-07-06 | Contributor| AI占位用户外键约束、NPE、gameOver阻断、再来一局状态传递 |
| 2026-07-06 | Contributor| |
| 2026-07-07 | Contributor| |

---

### Stashed changes（客户端）

| 日期 | 工作内容 | 问题/备注 |
|------|----------|-----------|
| 2026-07-04 | Contributor| 使用 JDK 21 |
| 2026-07-05 | Contributor| FXML 命名空间不能加版本号 |
| 2026-07-05 | Contributor| 首次居中，之后保留窗口位置 |
| 2026-07-05 | Contributor| |
| 2026-07-05 | Contributor| |
| 2026-07-05 | Contributor| 预览模式支持无服务器浏览 |
| 2026-07-05 | Contributor| 卡片/按钮/表格/输入框圆角 |
| 2026-07-05 | Contributor| 上传头像+昵称、匹配/排位入口、排行榜侧边抽屉 |
| 2026-07-05 | Contributor| 房间列表+观战+创建/加入/快速匹配 |
| 2026-07-05 | Contributor| 段位+匹配按钮 |
| 2026-07-05 | Contributor| |
| 2026-07-05 | Contributor| -fx-shape SVG path，hover 仅变背景色 |
| 2026-07-05 | Contributor| 全面优化白色极简UI |
| 2026-07-05 | Contributor| StackPane 浮层+Undecorated 标题栏+8px 圆角+磨砂玻璃卡片+动态注册字体 |
| 2026-07-05 | Contributor| 纯白磨砂卡片+底线描边+悬浮遮罩极简按钮 |
| 2026-07-06 | Contributor| HEARTBEAT、SPECTATE_OK/FAIL、RANKED_MATCH 等 |
| 2026-07-06 | Contributor| ClientConfig + client-config.properties |
| 2026-07-06 | Contributor| AutoCleanupController 基类 |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| RecordController + BoardController.replayMoves |
| 2026-07-06 | Contributor| spectateMode 禁用落子 |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| 30s 发送 / 90s 超时 |
| 2026-07-06 | Contributor| 3次重试 / 5s 间隔 |
| 2026-07-06 | Contributor| GomokuAI 启发式评分 |
| 2026-07-06 | Contributor| |
| 2026-07-06 | Contributor| 采用可编辑的 ComboBox，解耦本地存储路径，并适配项目磨砂极简风格的 MessageDialogUI 弹窗 |
| 2026-07-07 | Contributor| 1. 登录页新增服务器配置独立二级弹窗(ServerConfigDialogUI)；2. 将连接逻辑迁移至后台守护线程，去除嵌套弹窗，彻底解决 JavaFX 主线程卡死；3. 修复服务端断开连接后 connectionCount 不递减导致达 100 限制后拒绝服务的 Bug |
| 2026-07-07 | Contributor| 客户端在本地落子时提前写入 moveHistory，导致自己收到的 MOVE_SYNC 又被追加一次；移除本地写入，使悔棋弹出的两步与服务器保持一致，双方棋盘同步 |
| 2026-07-07 | Contributor| 1. LoginUI.fxml 中 CheckBox 缺 fx:id 导致 Controller 无法绑定；2. LoginController 新增凭据保存/加载逻辑，存入 ~/.gomatch/credentials.json |
| 2026-07-07 | Contributor| GameOverController.setResult() 只处理数字参数，BoardController 传的是中文文字；default 分支改为直接显示传入的 result 字符串，使胜负结果正确展示 |
| 2026-07-07 | Contributor| ScreenManager.showDialog 内部使用 showAndWait，原调用在 showAndWait 返回后才 setResult，导致弹窗已关闭才设置文字；新增带 setupCallback 的 showDialog 重载，在 showAndWait 前完成控制器设置，BoardController 所有 GameOver 弹窗改为通过回调提前设置结果文本 |
| 2026-07-07 | Contributor| 服务端 Room.endGame() 积分更新后新增 USER_SCORE_UPDATE 协议推送最新积分到玩家客户端；BoardController 接收后更新 AppConfig.currentUser 的 score/胜负平场次，使大厅和个人中心实时显示正确积分 |
| 2026-07-07 | Contributor| CasualLobbyController.onMessage() 缺少 ERROR 协议处理，服务端返回"等待对手…"时被静默丢弃；新增 ERROR/USER_SCORE_UPDATE 协议处理，FXML 增加 statusLabel 状态提示，handleQuickMatch/onMessage 中实时更新匹配状态；LobbyController 同步添加 USER_SCORE_UPDATE 处理确保积分标签即时刷新 |
| 2026-07-07 | Contributor| RoomManager.quickMatch() 原逻辑先将自己入队再 poll，导致每个请求都先取出自己然后返回等待；改为先尝试从队列取对手，无可用对手再入队，确保第二位玩家点击时能正确配对并通知双方 |
| 2026-07-07 | Contributor| Room.endGame() 中非排位赛普通匹配取消 winner +10 / loser -5 的积分调整，仅记录胜负场次；排位赛仍按原逻辑更新积分 |
| 2026-07-07 | Contributor| BoardUI.fxml 中 blackLabel 默认使用 section-label 导致黑方永远加粗；改为 subtitle，BoardController.updateTurnLabel() 根据 currentTurn 动态设置黑方/白方 font-weight，实现落子方高亮 |
| 2026-07-07 | Contributor| 服务端 Room.getRoomListInfo() 返回的 status 为大写 PLAYING，CasualLobbyController 中过滤观战房间时使用了小写 "playing"，导致条件永远不成立；改为 "PLAYING".equalsIgnoreCase() 比较，使进行中的对局正确出现在观战列表 |
| 2026-07-07 | Contributor| 服务端 handleSpectate() 原只发送 JOIN_OK 导致客户端进入 RoomWaitingUI 卡死；改为发送 GAME_START 触发 BoardUI 切换，并回放所有已有落子使观战者看到实时棋盘；客户端 BoardController 新增 spectatorMode，观战模式自动禁用落子点击和操作按钮 |
| 2026-07-07 | Contributor| 消除 14 个文件的 lint 警告：移除 28 个未使用 import、删除 4 个未使用方法、移除 7 个未使用字段/变量；修复 DatabaseBootstrap Scanner 资源泄漏 |
| 2026-07-07 | Contributor| 1. 在 BoardController.setGameInfo() 中主动启动倒计时，解决从 AiLobby/RoomWaiting 切换后新 BoardController 因 CopyOnWriteArrayList 快照机制收不到同一条 GAME_START 导致倒计时卡死；2. 新增 lastMoveX/Y 记录最后落子，redrawAll 中绘制红色加粗圆环标记，覆盖本地落子、MOVE_SYNC、悔棋、回放等场景；3. 回放模式启动时停止倒计时，避免残留
| 2026-07-07 | Contributor| AIPlayer 新增 Difficulty 枚举并实现差异化策略：EASY 40%随机落子+弱攻击评分（攻3防7）、MEDIUM 沿用原攻防均衡评分、HARD alpha-beta剪枝的深度2 MiniMax前瞻+即时获胜/堵杀检测；Room/RoomManager/ClientHandler 传递 difficulty
| 2026-07-07 | Contributor| 删除 database/src/main/resources/logback.xml（库模块不应包含导致运行时冲突）；RecordController 添加 @SuppressWarnings("unchecked") 消除 JavaFX 泛型 unchecked 警告













