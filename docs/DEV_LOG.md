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



