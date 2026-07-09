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
| 50 | 游戏大厅卡片集成 PixelCard 粒子悬浮效果 | 已完成 |
| 51 | 客户端/服务端 JSON 协议统一 | 已完成 |
| 52 | 排行榜分榜逻辑 | 已完成 |
| 53 | 登录界面 ASCII 动态渐变标题 | 已完成 |
| 54 | 心跳机制 + 对局断线重连 + 三次失败判负 | 已完成 |
| 55 | 断线状态可视化 — 玩家卡片"断线中"角标 | 已完成 |
| 56 | 回合计时与超时判负 | 已完成 |
| 57 | 悔棋/求和按钮仅当前落子方可操作 | 已完成 |
| 58 | 管理员功能（踢人/封禁/强制结束） | 已完成 |
| 59 | Token 重连机制 | 已完成 |
| 60 | 再来一局（Rematch）功能 | 已完成 |
| 61 | GAME_START 协议增强（传递用户名/积分） | 已完成 |
| 62 | 修复快速匹配 GAME_START 数据丢失 + 注册用户名并发重复 | 已完成 |
| 63 | 设置页面交互功能实现 | 已完成 |
| 64 | UserSettings 用户偏好持久化 | 已完成 |
| 65 | 国际化中英文切换 | 已完成 |

### 状态说明

- 已完成 — 开发完毕且验证通过
- 正在进行 — 正在开发中
- 待测试 — 开发完成，待验证
- 未开工 — 尚未开始

---

## 日志

### 前端（Frontend）

> FXML 布局、CSS 样式、动画特效、视觉组件

| 日期 | 工作内容 | 问题/备注 |
|------|----------|-----------|
| 2026-07-05 | Contributor| |
| 2026-07-05 | Contributor| |
| 2026-07-05 | Contributor| |
| 2026-07-05 | Contributor| |
| 2026-07-05 | Contributor| hover 仅变背景色，无特效 |
| 2026-07-05 | Contributor| |
| 2026-07-05 | Contributor| mvn clean compile 通过 |
| 2026-07-05 | Contributor| mvn clean compile 通过，Git 提交规范代码 |
| 2026-07-06 | Contributor| 编译验证通过，完美匹配白色磨砂极简风格 |
| 2026-07-06 | Contributor| 页面顶栏对齐与排版优化通过 |
| 2026-07-06 | Contributor| 排行榜与好友页面磨砂侧栏化重构完成 |
| 2026-07-06 | Contributor| 房间大厅磨砂侧栏化重构完成 |
| 2026-07-06 | Contributor| 房间等待大厅磨砂侧栏化重构完成，排位段位大厅还原成功 |
| 2026-07-06 | Contributor| 段位大厅完全还原，排位前置房间流对齐完毕 |
| 2026-07-06 | Contributor| 棋盘侧栏折叠缩放功能开发完成 |
| 2026-07-06 | Contributor| 侧边栏折叠按钮精炼与拖拽拉伸功能完全落地 |
| 2026-07-06 | Contributor| 棋盘拉伸与折叠自适应自收缩逻辑修复完成 |
| 2026-07-06 | Contributor| 侧栏动态数学限制、等比自适应及聊天换行优化完成 |
| 2026-07-06 | Contributor| 侧栏按钮防裁剪垂直堆叠优化完成 |
| 2026-07-07 | Contributor| BoardUI.fxml 中 blackLabel 默认使用 section-label 导致黑方永远加粗；改为 subtitle，BoardController.updateTurnLabel() 根据 currentTurn 动态设置黑方/白方 font-weight，实现落子方加粗高亮 |
| 2026-07-07 | Contributor| 1. 将 React Bits 的 PixelCard 移植为自定义 JavaFX 布局控件；2. 采用 Canvas 与 AnimationTimer 实现 60FPS 像素出现、波动延迟、shimmer 闪烁和自然缩回消失物理算法；3. 修改 LobbyUI.fxml 分别为匹配、AI 对战和排位卡片应用 blue、yellow 和 pink 变体；4. 优化样式，对 canvas 开启 mouse-transparent 使得鼠标事件完全穿透。 |
| 2026-07-07 | Contributor| 1. 提升粒子扩散速度，将延迟基准降至 30%；2. 增加粒子基础尺寸上限（1.2px - 4.0px），使其在视网膜屏上更加醒目；3. 引入中心距离归一化比率，实时调配像素点最大大小和不透明度，完美重塑"中心淡、四周浓"的渐变呼吸微粒景观。 |
| 2026-07-07 | Contributor| 1. 修改 LobbyUI.fxml，移除原本固定的 260x160 尺寸限制，采用 HBox.hgrow 与 VBox.vgrow 布局自适应填充整个中心窗口；2. 同步微调 style.css 中的图标、标题和子标题字体大小，使其在卡片扩大展示时保持极佳的高级极简视觉比例。 |
| 2026-07-07 | Contributor| 1. 蓝色变体减小 gap 至 7 并混入白色 (#ffffff)，改善淡色并增添星光感；2. 黄色变体增大 gap 至 8，解决粒子过于稠密拥挤的问题；3. 全局增大粒子大小 (1.8px - 5.5px) 并提升出现步长，将速度倍率增加至 3.5 倍；4. 提高中心最小比率限制至 0.35，增强整体动效可视度。 |
| 2026-07-07 | Contributor| 1. 修改 BLUE_CONFIG 颜色序列，额外引入淡褐色/淡棕色 (#d2b48c 和 #b45309) 以及高概率纯白，调配出天蓝、深蓝、淡棕与白光交汇的宇宙星河"星光色"。 |
| 2026-07-07 | Contributor| 1. 将扩散延迟系数从 0.3 适度放宽至 0.65，使微粒向四周平铺扩散的速度节奏处于舒适、优雅的涟漪波动区，避免过度局促与突兀感。 |
| 2026-07-07 | Contributor| 1. 将粒子基础尺寸区间由 1.8px - 5.5px 微调至 0.8px - 3.0px，在维持星光配色可见度的同时，恢复精细雅致的超细微"星尘"粒子感，消除像素颗粒偏大显粗糙的观感。 |
| 2026-07-07 | Contributor| 1. 匹配卡片（蓝色）彻底移除较重的深棕，加入高纯白及少量浅金、粉红与淡紫点缀；2. AI 卡片（黄色）融入少量蜜桔橙、薄荷绿与晴空蓝；3. 排位卡片（粉色）融合了蓝、紫、黄、橙，调配出清新舒适的五彩斑斓悬停特效。 |
| 2026-07-07 | Contributor| 1. 将粒子尺寸范围由 0.8px - 3.0px 适度放大至 1.2px - 3.8px，提高星尘可见性；2. 将基础流速倍率由 0.0035 减缓至 0.002，并降低 sizeStep 至 0.45 及呼吸闪烁随机速度，营造静谧雅致的流光闪动效果。 |
| 2026-07-07 | Contributor| 1. 将粒子基础尺寸区间由 1.2px - 3.8px 进一步扩充至 1.6px - 4.8px，使得像素颗粒在拉伸铺开后具有更饱满的外观与出色的显示可见度。 |
| 2026-07-07 | Contributor| 1. 微调颜色数组，确保各变体微粒完全限制在主色调同色系或邻近色范围内；2. 匹配卡片（蓝色）使用靛蓝与淡紫；3. AI卡片（黄色）使用暖橙与杏色；4. 排位卡片（粉色）使用霓虹粉与香芋紫，从而在保留多彩层次的同时实现极佳的视觉协调性。 |
| 2026-07-07 | Contributor| 1. 在 ScreenManager.java 中为场景及弹窗配置 PerspectiveCamera(false)，引入不影响 2D 绝对定位的透视投影；2. 在 PixelCard 中注册 Rotate 变换并监听鼠标移动和拖拽，依据鼠标距卡片中心偏移量，在 AnimationTimer 中使用指数衰减算法实现 12 度 3D 偏转和 1.05 倍缩放 Spring 回弹过渡；3. 利用 setViewOrder 属性实现悬停卡片置顶，避免相邻遮挡。 |
| 2026-07-07 | Contributor| 1. 将 3D 旋转的倾斜角振幅由 12 度调高至 18 度，以提供更加敏感、显著和动感的空间倾斜效果。 |
| 2026-07-07 | Contributor| 1. 在 LobbyUI.fxml 中为匹配和 AI 卡片标签分别增加样式名 arena-match 和 arena-ai；2. 在 style.css 中为匹配（蓝色）、AI对战（黄金）、排位（粉色）重写独立的 hover 样式，使用 12% 不透明度的专属主色背景和 35% 不透明度的底边框，代替原本统一的板岩灰，达成融洽自然的淡色透明悬浮体验。 |
| 2026-07-07 | Contributor| 1. 在 PixelCard.java 中加入 Region cardBody，隔离背景与边框渲染，在 layoutChildren 中限制卡片身体和外部子内容仅占用 Y=45px 偏下的卡片本体区；2. 增加 Z 轴旋转及单摆力学公式，根据鼠标滑过/拖拽速度对卡片施加角加速度，驱动 Z 轴偏转摆动；3. 在 Canvas 顶部使用二次贝塞尔曲线和金属扣动态绘制跟随速度摆动弯折的主题色吊带环圈；4. 适配 CSS 完成半透磨砂背景的隔离转移。 |
| 2026-07-07 | Contributor| 1. 降低 setOnMouseEntered 鼠标滑入随机推力至 0.25；2. 在单摆公式中将重力恢复系数由 4.5 提升至 6.5 增强回正力，将阻尼由 2.0 提升至 3.5 快速吸能结算；3. 将鼠标划过速度对角加速度的推力比重由 0.00008 大幅缩减至 0.000025，并收窄瞬时推力最大值为 0.15，彻底解决原本剧烈摆荡的问题，提供优雅静谧的微小回弹。 |
| 2026-07-07 | Contributor| 1. 回退还原 PixelCard 粒子流动速率的 widthPlusHeight 绑定参数；2. 在 initPixels 中计算基于 300px 卡片宽度的 scaleFactor，传入 Pixel 构造函数对粒子尺寸 minSize/maxSize、绘制居中偏置 centerOffset、以及出现 sizeStep 与消失收缩速率进行等比缩放，确保粒子在大/小窗口下视觉比例恒定。 |
| 2026-07-07 | Contributor| 1. 修改 AiLobbyUI.fxml，导入 PixelCard 并移除原来的普通列表按钮，改为与大厅主页一致的自适应横向 HBox 卡片结构；2. 将简单、中等、困难三个难度映射到 blue（🌱）、yellow（⚡）和 pink（🔥）三种主色调变体与专属徽章图标，添加细小的说明介绍文字；3. 在 style.css 中为新增的描述类 .arena-desc 设定排版字号和悬浮文字色。 |
| 2026-07-07 | Contributor| 1. 在 ScreenManager.java 的 switchTo 中，将主业务 VBox 容器 mainLayout 锁定为 1920x1080 参考尺寸（标准 16:9）；2. 对其直接挂载 Scale 缩放变换，并移除了额外的 centerWrapper 层；3. 监听顶层 wrapper 的实际窗口宽高变化，取最窄边比例平滑更新 Scale 缩放系数，彻底消除了标题栏与内容区缩放不一致而产生的多余黑边。 |
| 2026-07-07 | Contributor| 1. 将 ScreenManager.java 中的默认 WIDTH 和 HEIGHT 常量修改为 1280 和 720；2. 业务 UI 在此基准尺寸上自适应等比缩放，窗口支持自由拉伸，使得在任何尺寸拉伸时内部都完美按 16:9 相对大小进行矢量对齐，既适合本地测试又实现了标准宽屏。 |
| 2026-07-07 | Contributor| 1. 在 ScreenManager.java 中引入 MouseEvent 事件过滤器，在捕获阶段优先拦截边框拉伸事件，并通过 e.consume() 阻断事件向下传递至子组件，规避拉伸窗口时引发的子组件物理拖拽冲突；2. 增加 MOUSE_RELEASED 阶段的监听器，在鼠标松开时将 resizeMode 强制重置为 NONE，修复了释放鼠标后依然会异常缩放窗口的遗留缺陷。 |
| 2026-07-07 | Contributor| 1. 移除 LobbyUI.fxml 中 playerInfoBox 头像芯片的 HBox.hgrow="ALWAYS" 强制横向填充，将其还原为只包裹头像和昵称的紧凑胶囊组件；2. 增设隐藏 Region 填充组件，将点击操作范围完全限制在头像可见的芯片范围内，断绝了在顶部栏空白处误触进入个人中心的操作体验 Bug。 |
| 2026-07-07 | Contributor| 1. 在 handleMouseMoved 中计算鼠标距离卡片身体边缘的最短像素距离，在 25px 边缘过渡区内计算 0.0~1.0 的阻尼因子，使得鼠标跨越卡片边界时 3D 偏转角平滑衰减至 0；2. 结合 setOnMouseExited 中的静态无偏转包围盒屏幕投影校验，拦截旋转位移产生的假滑出信号，彻底治愈了下边缘在鼠标滑出时的反复触发与抖动。 |
| 2026-07-07 | Contributor| 1. 在 style.css 中将 .section-divider 的 -fx-padding 设为 0，彻底移除 Modena 默认主题中分割线上下各 10px 的不可见外边距死区；2. 在 LobbyUI.fxml 和 AiLobbyUI.fxml 中为 Separator 加上 mouseTransparent="true" 物理穿透，彻底根治了顶栏按钮下方边缘点击被分割线 hitbox 拦截截断的问题。 |
| 2026-07-07 | Contributor| 1. 在 ShapeGridBackground.java 中，用 SPEED_X_PER_SEC/SPEED_Y_PER_SEC 代替每帧固定位移，引入基于时间步长 dt 的无缝包裹位移；2. 增加对最大帧时间 dt 的阈值限制 (0.1s)，避免应用挂起恢复或帧率突变时粒子产生位置瞬移，确保在不同刷新率显示器下流动速度一致恒定。 |
| 2026-07-07 | Contributor| 1. 在 style.css 中为 .pixel-card-body 引入基础极简软投影，提供细腻的纸张悬浮深度感；2. 为匹配（蓝色）、AI对战（黄色）、排位（粉色）三个主题色分别定制专属的 hover 投影（24px gaussian blur, 18% 不透明度），增强 3D 偏转侧倾时的空间光感表现；3. 增设 pressed 状态阴影收紧收敛（12px gaussian blur），建立直观敏捷的按下触觉交互反馈。 |
| 2026-07-07 | Contributor| 1. 在 Pixel.draw() 中对 size<=0 进行拦截，在 shimmer/disappear 中进行 Clamp 边界夹紧，阻断异常渲染；2. 增加 Canvas 动态可见性管理（在开始 appear 时可见，在 disappear 结束时设为不可见），彻底打断 JavaFX 3D 渲染管线及脏区缓存失效引发的粒子显示残留。 |
| 2026-07-08 | Contributor| 改造 RankingUI.fxml 左侧导航栏增加"全服排行"与"好友排行"两个选项按钮，为标题分配 fx:id；重构 RankingController.java 实现 Tab 切换逻辑、多类型 Mock 数据展现与网络消息类型过滤，防范并发乱序渲染。 |
| 2026-07-08 | Contributor| 1. 采用 3D 正向光栅化高精密度采样（将采样量暴增至 480x120，共 5.7 万个点），并在 X, Y, Z 轴上大幅降慢且收窄 3D 几何形变波动以减少扭动；2. 将 ASCII 单字字号收缩为极精密的 5px（asciiFontSize=5），网格分辨率自动扩充至 174x76（共 1.3 万个字符），使字符点阵凝聚感和高密度精细度翻倍；3. 将字身黑色判定阈值门槛压低至 45，使核心黑色字符占比高达 95% 以上，并对红色、蓝色色散通道实施大幅亮度衰减融入暗色系；4. 布局拓宽至 480，销毁 AnimationTimer 防止泄漏。 | 编译与渲染测试成功，完美呈现超高密度且沉稳黑色的高精细偏色特效 |
| 2026-07-08 | Contributor| BoardController：在玩家卡片右下角添加红色"断线中"角标（默认隐藏）；新增 setPlayerOnline(isBlack, online) 方法切换断线状态：断线时卡片透明度 0.55 + 名字变红 + 显示角标；监听 PLAYER_DISCONNECTED/PLAYER_RECONNECTED/RECONNECT_ATTEMPT/RECONNECT_FAILED 消息并在聊天区打印系统提示；新增 blackId/whiteId 字段用于按 userId 匹配玩家卡片 |
| 2026-07-08 | Contributor| CSS：新增 .btn-secondary:disabled 样式（灰底 + 低透明 + 光标 default） |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| 匹配大厅界面优化 |
| 2026-07-08 | Contributor| BoardController 新增 handleRequestRematch/handleReplyRematch 方法及 rematchRequested 状态；对局结束弹窗 GameOverController 新增"再来一局"按钮；BoardController 监听 REMATCH_REQUESTED/REMATCH_ACCEPTED/REMATCH_REJECTED 消息，同意后自动重置棋盘开始新对局 |
| 2026-07-08 | Contributor| BoardController.onMessage(GAME_START) 解析新增的 blackUsername/blackScore/whiteUsername/whiteScore 字段；新增带分数的 setGameInfo 重载，对手卡片显示真实用户名和积分而非 ID 和问号 |
| 2026-07-09 | Contributor| 1. 将 .theme-dark .glass-card 背景不透明度从 0.48 提升至 0.60；新增 .theme-dark .auth-pixel-card .pixel-card-body 专属深色磨砂背景（0.55 不透明度）并改用深色投影；保留 .theme-dark .auth-pixel-card 外层白色高光投影。2. 将浅色/暗色模式下的 .custom-title-bar 和 .top-bar 背景统一为透明并去除投影。3. 统一侧栏菜单 active/导航按钮/标题栏按钮 hover/active 背景色。4. 补齐暗色模式下缺失的 pressed 状态。5. AsciiTextCanvas 支持暗色模式：根据 ScreenManager.isDarkTheme() 动态切换采样贴图（白底黑字）和输出色调，核心字符使用 #f8fafc，边缘使用柔和浅色偏色，使登录页 Gomatch 标题在暗色背景下清晰可见且风格统一。 | mvn -f client/pom.xml compile 通过 |

### 服务端（Server）

> Socket 多线程、游戏逻辑、匹配算法、ELO 排名

| 日期 | 工作内容 | 问题/备注 |
|------|----------|-----------|
| 2026-07-04 | Contributor| 使用 JDK 21 |
| 2026-07-06 | Contributor| 基于 Socket 多线程架构 |
| 2026-07-06 | Contributor| 定时任务自动刷新 |
| 2026-07-06 | Contributor| 编译验证通过 |
| 2026-07-06 | Contributor| 共覆盖 25+ 个 Java 类 |
| 2026-07-06 | Contributor| 提升团队协作可读性与文档查找效率 |
| 2026-07-06 | Contributor| 编译验证通过 |
| 2026-07-07 | Contributor| |
| 2026-07-07 | Contributor| RoomManager.quickMatch() 原逻辑先将自己入队再 poll，导致每个请求都先取出自己然后返回等待；改为先尝试从队列取对手，无可用对手再入队，确保第二位玩家点击时能正确配对并通知双方 |
| 2026-07-07 | Contributor| Room.endGame() 中非排位赛普通匹配取消 winner +10 / loser -5 的积分调整，仅记录胜负场次；排位赛仍按原逻辑更新积分 |
| 2026-07-07 | Contributor| 服务端 handleSpectate() 原只发送 JOIN_OK 导致客户端进入 RoomWaitingUI 卡死；改为发送 GAME_START 触发 BoardUI 切换，并回放所有已有落子使观战者看到实时棋盘 |
| 2026-07-07 | Contributor| 服务端 Room.getRoomListInfo() 返回的 status 为大写 PLAYING |
| 2026-07-07 | Contributor| AIPlayer 新增 Difficulty 枚举并实现差异化策略：EASY 40%随机落子+弱攻击评分（攻3防7）、MEDIUM 沿用原攻防均衡评分、HARD alpha-beta剪枝的深度2 MiniMax前瞻+即时获胜/堵杀检测；Room/RoomManager/ClientHandler 传递 difficulty |
| 2026-07-08 | Contributor| 服务端 ClientHandler.dispatch() 改为 Message 分发，所有 send() 调用改为 Message.of()/builder()；handleRoomList/handleRanking/handleQueryRecord 改用 Message.builder() + JSONArray 替代手动 StringBuilder 拼 JSON；Room 广播方法全部改为 Message 参数 |
| 2026-07-08 | Contributor| 1. Room.endGame() 中拆分 MySQL 和 Redis 错误处理，Redis 缓存失败不再掩盖 MySQL 成功保存；2. RecordDAO.saveRecord() 对 startTime 做防御性 null 检查，避免 NPE 导致整条记录保存失败 |
| 2026-07-08 | Contributor| 在 ClientHandler.java 中支持 Protocol.RANKING 携带 type 参数，并在返回数据包时回传分榜类型 |
| 2026-07-08 | Contributor| ClientHandler.java: handleRegister/handleLogin/handleChangePassword/handleCreateRoom/handleCreateAiRoom 增加 String 入参 null 检查；handleReconnect: token 日志行 NPE 保护；Room.java: getRankByUserId TOCTOU 重复调用风险修复 |
| 2026-07-08 | Contributor| SessionManager 中 updateHeartbeat 蜕化为会话查询时的活跃时间维护，保留；清理 MODULE_SERVER.md 中的过时心跳描述 |
| 2026-07-08 | Contributor| 服务端 ClientHandler：处理 HEARTBEAT 命令 + 后台线程检测 90s 无消息则关闭连接；Room：对局中玩家断线启动 60s 重连计时窗口，最多 3 次重连机会，全部失败则判对方获胜（endGame reason=disconnect），重连成功后自动回放已有落子 |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| Protocol 新增 REQUEST_REMATCH/REPLY_REMATCH/REMATCH_ACCEPTED/REMATCH_REJECTED 常量；ClientHandler 新增 handleRequestRematch/handleReplyRematch 方法；Room 新增 rematchRequesterId 字段 + requestRematch/replyRematch/resetAndRestartGame 方法，支持双方同意后自动重置棋盘开始新对局 |
| 2026-07-08 | Contributor| Room 新增 blackUsername/whiteUsername/blackScore/whiteScore 字段 + getter；join() 缓存用户名；startGame()/resetAndRestartGame() 查询并刷新双方积分；新增 buildGameStartMessage() 统一构建含用户名/积分的 GAME_START 消息；ClientHandler 全部 7 处 GAME_START 广播改用 buildGameStartMessage() |
| 2026-07-09 | Contributor| 1. UserDAO.register() 新增捕获 SQLIntegrityConstraintViolationException，防止 existsByUsername 和 INSERT 之间的并发竞态绕过唯一性检查；2. UserDAO.existsByUsername() 在 SQL 异常时改为返回 true（保守拒绝注册），而非 false（放行），确保数据库故障时不被绕过 | 编译通过 |

### 客户端（Client）

> Controller 业务逻辑、网络通信、数据管理、AI 对弈、游戏流程

| 日期 | 工作内容 | 问题/备注 |
|------|----------|-----------|
| 2026-07-05 | Contributor| FXML 命名空间不能加版本号 |
| 2026-07-05 | Contributor| 首次居中，之后保留窗口位置 |
| 2026-07-05 | Contributor| 预览模式支持无服务器浏览 |
| 2026-07-05 | Contributor| mvn compile 通过 |
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
| 2026-07-06 | Contributor| 采用可编辑的 ComboBox，解耦本地存储路径，并适配项目磨砂极简风格 MessageDialogUI 弹窗 |
| 2026-07-06 | Contributor| 脚本已加入 Git 追踪供团队成员使用 |
| 2026-07-06 | Contributor| 预览模式匹配正常启动人机对局 |
| 2026-07-06 | Contributor|roomId\|roomName\|playerCount），从根本上避免了联机配对时客户端崩溃黑屏的问题。 | 联机协议兼容与对齐逻辑升级通过 |
| 2026-07-06 | Contributor| 排位至大厅流转流程修正完成 |
| 2026-07-07 | Contributor| 编译验证通过 |
| 2026-07-07 | Contributor| 客户端在本地落子时提前写入 moveHistory，导致自己收到的 MOVE_SYNC 又被追加一次；移除本地写入，使悔棋弹出的两步与服务器保持一致，双方棋盘同步 |
| 2026-07-07 | Contributor| 1. LoginUI.fxml 中 CheckBox 缺 fx:id 导致 Controller 无法绑定；2. LoginController 新增凭据保存/加载逻辑，存入 ~/.gomatch/credentials.json |
| 2026-07-07 | Contributor| GameOverController.setResult() 只处理数字参数，BoardController 传的是中文文字；default 分支改为直接显示传入的 result 字符串，使胜负结果正确展示 |
| 2026-07-07 | Contributor| ScreenManager.showDialog 内部使用 showAndWait，原调用在 showAndWait 返回后才 setResult，导致弹窗已关闭才设置文字；新增带 setupCallback 的 showDialog 重载，在 showAndWait 前完成控制器设置，BoardController 所有 GameOver 弹窗改为通过回调提前设置结果文本 |
| 2026-07-07 | Contributor| 服务端 Room.endGame() 积分更新后新增 USER_SCORE_UPDATE 协议推送最新积分到玩家客户端；BoardController 接收后更新 AppConfig.currentUser 的 score/胜负平场次，使大厅和个人中心实时显示正确积分 |
| 2026-07-07 | Contributor| CasualLobbyController.onMessage() 缺少 ERROR 协议处理，服务端返回"等待对手…"时被静默丢弃；新增 ERROR/USER_SCORE_UPDATE 协议处理，FXML 增加 statusLabel 状态提示，handleQuickMatch/onMessage 中实时更新匹配状态；LobbyController 同步添加 USER_SCORE_UPDATE 处理确保积分标签即时刷新 |
| 2026-07-07 | Contributor| CasualLobbyController 中过滤观战房间时使用了小写 "playing"，导致条件永远不成立；改为 "PLAYING".equalsIgnoreCase() 比较 |
| 2026-07-07 | Contributor| 客户端 BoardController 新增 spectatorMode，观战模式自动禁用落子点击和操作按钮 |
| 2026-07-07 | Contributor| 消除 14 个文件的 lint 警告：移除 28 个未使用 import、删除 4 个未使用方法、移除 7 个未使用字段/变量；修复 DatabaseBootstrap Scanner 资源泄漏 |
| 2026-07-07 | Contributor| 1. 在 BoardController.setGameInfo() 中主动启动倒计时，解决从 AiLobby/RoomWaiting 切换后新 BoardController 因 CopyOnWriteArrayList 快照机制收不到同一条 GAME_START 导致倒计时卡死；2. 新增 lastMoveX/Y 记录最后落子，redrawAll 中绘制红色加粗圆环标记，覆盖本地落子、MOVE_SYNC、悔棋、回放等场景；3. 回放模式启动时停止倒计时，避免残留 |
| 2026-07-07 | Contributor| AIPlayer 新增 Difficulty 枚举并实现差异化策略 |
| 2026-07-07 | Contributor| 删除 database/src/main/resources/logback.xml（库模块不应包含导致运行时冲突）；RecordController 添加 @SuppressWarnings("unchecked") 消除 JavaFX 泛型 unchecked 警告 |
| 2026-07-08 | Contributor| 1. 新增 common/Message 类（fromJson/toJson/builder/of/cmdOnly），添加 org.json 依赖；<br>2. NetworkListener 接口改为 onMessage(Message msg)；<br>3. ServerConnector.send() 改为 send(Message) + send(String cmd) 重载；<br>4. 所有 14 个客户端 Controller 的 onMessage 和 send() 全部迁移到 Message 模式，列表类响应（房间/排行榜/好友/对战记录）统一解析 JSONArray 替代竖线分割数组；<br>5. 修复 handleRoomList 错误使用 RANKING_LIST→ROOM_LIST 及 FriendsController.handleRejectRequest 遗漏的旧格式调用 | 全项目零残留旧 pipe 协议代码，编译通过 |
| 2026-07-08 | Contributor| 1. RankingController 初始化时主动渲染占位，处理 ERROR 消息，使用 optInt/optString 解析数值字段，预览模式加载本地假数据；2. LobbyController 抽屉排行榜同步修复解析方式 |
| 2026-07-08 | Contributor| BoardController.replayMoves 默认 replayIndex=0（空棋盘），而非从最后一步开始 |
| 2026-07-08 | Contributor| 1. CasualLobbyController 匹配计数器从 0 开始正向递增（首秒后变1），不限循环次数；2. 匹配中 quickMatchBtn 添加 btn-danger 红色样式，取消匹配时移除 |
| 2026-07-08 | Contributor| 1. 在 ProfileController.java 增加 extractBoardData 辅助方法，兼容 JSON 数组、双重 JSON 编码字符串、老格式分号/管道分隔等多种 boardData 存储形式；2. 在回放按钮事件中改用 isReplayDataValid 判定，区分空数据 (`[]`) 与有效 JSON 数组，避免误报"暂无棋谱数据"；3. 在 Room.java 的 cacheGameRecordToRedis 中将 boardData 字符串解析为 org.json.JSONArray 再 put 进去，避免 org.json 在嵌套 put(String, String) 时对引号转义造成的双重 JSON 编码，从根上解决 Redis 缓存中棋谱数据被破坏的问题。 | 编译通过，预期修复 AI 对局棋谱回放 |
| 2026-07-08 | Contributor| ProfileController.java: msg.getData() 为 null 保护 + record.getResult() 空字符串 substring 越界修复 |
| 2026-07-08 | Contributor| 客户端 ServerConnector.java 不存在心跳定时器（已在前续版本移除）；Protocol.java 无 HEARTBEAT 常量；清理 MODULE_CLIENT.md 中的过时心跳描述 |
| 2026-07-08 | Contributor| 客户端 ServerConnector：添加定时心跳（30s/次）+ 响应超时检测（90s）+ 自动重连（5s间隔, 3次） |
| 2026-07-08 | Contributor| BoardController：新增 updateActionButtons() 仅在 myTurn && !gameOver && !spectatorMode && !replayMode 时启用 undoBtn 和 drawBtn，非己方回合时灰色禁用；在 setGameInfo / handleMouseClick / MOVE_SYNC / GAME_OVER / UNDO_ACCEPTED / TURN_TIMEOUT 等所有 myTurn/gameOver 变更点调用 |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| |
| 2026-07-08 | Contributor| ClientHandler.java: handleRegister/handleLogin/handleChangePassword/handleCreateRoom/handleCreateAiRoom 增加 String 入参 null 检查，handleReconnect token 日志行 NPE 保护；Room.java: getRankByUserId TOCTOU 重复调用风险修复 | 编译通过 |
| 2026-07-08 | Contributor| Protocol.java 无 HEARTBEAT 常量残留清理；SessionManager 中 updateHeartbeat 蜕化为会话查询时的活跃时间维护，保留；清理 MODULE_SERVER.md / MODULE_COMMON.md / MODULE_CLIENT.md / PRD.md 中的过时心跳描述 | 文档已同步 |
| 2026-07-08 | Contributor| 服务端 ClientHandler：处理 HEARTBEAT 命令 + 后台线程检测 90s 无消息则关闭连接；Room：对局中玩家断线启动 60s 重连计时窗口，最多 3 次重连机会，全部失败则判对方获胜（endGame reason=disconnect），重连成功后自动回放已有落子 | lint 零告警 |
| 2026-07-08 | Contributor| BoardController：在玩家卡片右下角添加红色"断线中"角标（默认隐藏）；新增 setPlayerOnline(isBlack, online) 方法切换断线状态：断线时卡片透明度 0.55 + 名字变红 + 显示角标；监听 PLAYER_DISCONNECTED/PLAYER_RECONNECTED/RECONNECT_ATTEMPT/RECONNECT_FAILED 消息并在聊天区打印系统提示；新增 blackId/whiteId 字段用于按 userId 匹配玩家卡片 | lint 零告警 |
| 2026-07-08 | Contributor| BoardController：新增 updateActionButtons() 仅在 myTurn && !gameOver && !spectatorMode && !replayMode 时启用 undoBtn 和 drawBtn，非己方回合时灰色禁用；在 setGameInfo / handleMouseClick / MOVE_SYNC / GAME_OVER / UNDO_ACCEPTED / TURN_TIMEOUT 等所有 myTurn/gameOver 变更点调用；CSS：新增 .btn-secondary:disabled 样式（灰底 + 低透明 + 光标 default） | lint 零告警 |
| 2026-07-08 | Contributor| 1. 从 AGENTS.md 移除固定当前分支描述，避免多人协作时误导；2. 将项目说明补充 Redis；3. 更新 README，将 Redis 标记为必需组件，并改正启动脚本示例与目录说明，避免引用不存在的 run-client/run-server 脚本。 | 文档已同步 |
| 2026-07-08 | Contributor| Maven 编译插件统一 3.13.0 + release 21 + fork；database 测试改为结构测试；心跳改为服务端返回 HEARTBEAT_OK 并刷新 Redis 会话活跃时间；Redis 改为服务端必需组件，匹配候选从 Redis 队列读取，锁失败不再兜底成功；会话 Redis 数据补充 role；新增管理中心页面 + 存档功能；协议常量集中化；清理 cp.txt 与 client-config.properties | 编译验证通过 |
| 2026-07-09 | Contributor| 1. ScreenManager 移除整页 Scale 缩放，改由 FXML 布局约束自适应；2. 登录/注册页改用静态 PixelCard 外壳，列表页和个人中心释放固定宽度；3. 非 ADMIN 用户隐藏管理入口；4. BoardController 与 ShapeGridBackground 增加 0 尺寸和重复尺寸重绘保护；5. 同步更新 CLIENT_STYLE_GUIDE.md 与 MODULE_CLIENT.md。 |
| 2026-07-09 | Contributor| 1. 新增 ElasticVolumeSlider 自定义控件，用 JavaFX 属性、鼠标事件和 Timeline 实现 React Bits ElasticSlider 风格的悬浮放大、轨道变粗、越界衰减与松手回弹；2. SettingsUI.fxml 将全局音量原生 Slider 替换为 0-100、步长 5 的弹性滑杆；3. style.css 补充中性板岩灰轨道、图标和数值提示样式；4. 新增 PRODUCT.md 记录产品设计上下文。 | 编译通过 |
| 2026-07-09 | Contributor| 1. 全局音乐新增独立开关与弹性音量条；2. 背景音乐、音效保留无文字开关并新增 ElasticVolumeSlider；3. 将"大厅音乐"改名为"大厅歌曲"，仅保留歌曲下拉选择（默认主题曲、清晨棋局、静夜对弈、赛场低语）；4. SettingsController 绑定开关与对应音量条的禁用和透明度状态。 | 编译通过 |
| 2026-07-09 | Contributor| 1. 新增由用户指定 D:\CloudMusic 原曲转码得到的 audio/lobby-theme.mp3；2. 新增 BackgroundMusicManager，通过 JavaFX MediaPlayer 循环播放并在退出时释放；3. ClientMain 启动时播放背景音乐，SettingsController 将全局音乐/背景音乐开关与音量条同步到实际播放状态；4. client/pom.xml 补充 javafx-media 依赖。 | 编译通过 |
| 2026-07-09 | Contributor| **原因**：CasualLobbyController.onMessage(GAME_START) 只调用了 3 参数版 setGameInfo(bId, wId, username)，丢弃了服务端传来的 blackUsername/blackScore/whiteUsername/whiteScore 全部数据；同时 JOIN_OK 在 GAME_START 后又被发送，导致已切入 BoardUI 的客户端又被切回 RoomWaitingUI。**修复**：1. GAME_START 处理改为解析全部 6 个字段（blackUsername/blackScore/whiteUsername/whiteScore），传递到 BoardController.setGameInfo() 的完整参数重载；2. 新增 joinedGame 标记位，在已进入 GAME_START 对局后忽略后续 JOIN_OK 消息，防止界面回切到等待室 | 编译通过 |
| 2026-07-09 | Contributor| 1. 定位大厅音乐未播放原因为 JavaFX Media 对 FLAC 运行时支持不稳定且原逻辑静默降级；2. 使用 ShareX ffmpeg 将原曲转为纯音频 lobby-theme.mp3；3. BackgroundMusicManager 改为加载 MP3，移除未使用 FLAC 资源；4. MODULE_CLIENT.md 同步资源路径。 | 媒体探针 READY；编译通过 |
| 2026-07-09 | Contributor| 将 BackgroundMusicManager 的默认全局音乐音量从 80 调整为 45、背景音乐音量从 70 调整为 35，初始实际播放音量由 56% 降至约 16%；SettingsUI.fxml 同步默认滑杆显示值。 | 编译通过 |
| 2026-07-09 | Contributor| RoomManager.quickMatch() 中 createRoom() 已将 player 加入房间，但后续又调用 room.join(player) 被重复检查拒绝；删除重复调用 | 编译通过 |
| 2026-07-09 | Contributor| RoomManager.rankedMatch() 中 createRankedRoom() 已将 player 加入房间，但后续又调用 room.join(player) 被重复检查拒绝；删除重复调用 | 编译通过 |
| 2026-07-09 | Contributor| Room.makeAiMove() 无论谁获胜都使用 whiteUserId(-1) 作为 winnerId；改为根据 winnerColor 正确判断获胜者 | 编译通过 |
| 2026-07-09 | Contributor| RecordDAO.getHistory() 中使用 rs.wasNull() 检查平局时的 NULL 值，确保正确处理 | 编译通过 |
| 2026-07-09 | Contributor| ClientHandler.handleFriendRequests() 改用 JSONObject/JSONArray 构建安全的 JSON 响应，避免特殊字符导致解析错误 | 编译通过 |
| 2026-07-09 | Contributor| 1. 全屏模式：stage.setFullScreen() 即时生效；2. 分辨率切换：动态调整 stage 宽高并居中；3. 动态背景：ShapeGridBackground.setEnabled() 控制粒子网格显隐；4. 帧率限制：ShapeGridBackground 跳帧逻辑实现 60/120/无限制；5. 字体切换：修改 scene root 的 -fx-font-family 属性；6. 开机自启：VBS 脚本写入 Windows %APPDATA%\Startup；7. 记住密码：LoginController 读取 UserSettings 控制默认勾选；8. 后台静音：窗口失焦暂停音乐、获焦恢复；9. 音频控制：ElasticVolumeSlider 双向绑定 BackgroundMusicManager | 编译通过 |
| 2026-07-09 | Contributor| 新增 UserSettings 类，将全屏/分辨率/帧率/音量/背景/字体/主题/语言/开机自启/记住密码等 15 项偏好保存到 ~/.gomatch/user-settings.json，启动时自动加载 | 编译通过 |
| 2026-07-09 | Contributor| 1. 新增 I18N 工具类，支持 ResourceBundle 动态切换；2. 新增 messages_zh.properties 和 messages_en.properties 资源文件（240+ key）；3. 所有 21 个 FXML 中文文本替换为 %key 占位符；4. ScreenManager 加载 FXML 时传入 ResourceBundle；5. ClientMain 启动时初始化语言；6. SettingsController 语言切换后重载场景 | 编译通过 |
| 2026-07-09 | Contributor| root.setStyle() 会覆盖所有内联样式导致 -fx-background-color 丢失；改为 appendFontFamily() 追加/替换 -fx-font-family 属性，保留其他样式 | 编译通过 |
| 2026-07-09 | Contributor| updateTurnLabel() 中 if-else 块后多了一个 }，导致 updatePlayerCardHighlight() 脱离方法体 | 编译通过 |
| 2026-07-09 | Contributor| 1. GameOverController.setResult() 支持中文描述（黑方胜、白方胜、平局、对手胜）；2. BoardController GAME_OVER 处理增加 AI 模式特殊判断（winnerId="-1"时判定为对手胜） | 编译通过 |
| 2026-07-09 | Contributor| 1. FriendDAO.sendFriendRequest() 发送新请求前先删除双方所有旧请求记录（PENDING/ACCEPTED/REJECTED）；2. FriendDAO.deleteFriend() 删除好友关系时同时清理双方的请求记录，解决唯一索引冲突 | 编译通过 |
| 2026-07-09 | Contributor| 1. RecordDAO.getHistory() 添加返回 board_data 字段；2. ClientHandler.handleQueryRecord() 在 RECORD_LIST 响应中添加 boardData，转义 `\|` 防止协议解析错误 | 编译通过 |
| 2026-07-09 | Contributor| 1. RecordController.onMessage() 更新解析步长从10改为11，添加 boardData 处理和解转义；2. BoardController.replayMoves() 新增 JSON 格式棋谱支持（检测 `[` 开头自动切换解析方式），兼容旧格式分号分隔 | 编译通过 |
| 2026-07-09 | Contributor| BoardUI.fxml 将回放按钮容器从 HBox（水平排列）改为 VBox（垂直排列），按钮 maxWidth="Infinity" 充分利用侧边栏宽度 | 编译通过 |
| 2026-07-09 | Contributor| FriendsUI.fxml 将操作列宽度从 100px 增加到 140px，确保"接受"和"拒绝"按钮完整显示 | 编译通过 |
| 2026-07-09 | Contributor| client-config.properties 中 server.host 从错误值改为 127.0.0.1 | 编译通过 |
| 2026-07-09 | Contributor| 1. 发送悔棋/求和请求后显示等待弹窗（带加载动画），禁止落子和重复请求；2. 收到回复后关闭弹窗，恢复正常游戏；3. 游戏结束时自动关闭等待弹窗 |
| 2026-07-09 | Contributor| 1. 发送请求时停止倒计时；2. 收到请求时停止倒计时；3. 收到回复后恢复倒计时；4. 悔棋同意时重置为60秒，拒绝时继续当前剩余时间；5. 求和拒绝时继续当前剩余时间 |
| 2026-07-09 | Contributor| 观战者在对局结束时弹出游戏结束对话框，显示获胜者信息（黑方/白方 + 用户名）和"返回大厅"按钮 |
| 2026-07-09 | Contributor| 1. style.css 追加暗夜磨砂主题覆盖层，将全局背景、卡片、输入框、按钮、表格、列表、弹窗、滚动条和音量滑杆统一为近黑玻璃与冷灰描边；2. 清理多处 FXML 浅色内联背景，避免覆盖全局主题；3. 设置页暗夜主题选项从"开发中"改为可用并持久化；4. 同步 CLIENT_STYLE_GUIDE.md、requirements.md 与 AGENTS.md 的 UI 约定。 | mvn -f client/pom.xml compile -e 通过 |
| 2026-07-09 | Contributor| 1. ScreenManager 新增 theme-light/theme-dark 根类切换与 themeColor(light,dark) 动态色辅助；2. 设置页恢复"清透白磨砂 (经典)"与"暗夜黑磨砂"两个选项，切换后即时应用当前 Scene；3. 暗色 CSS 统一限定在 `.theme-dark`，动态列表/棋盘玩家卡/个人资料头部跟随主题；4. FXML 清理残留暗色内联样式并同步设计规范。 | 编译通过，FXML XML 解析通过 |
| 2026-07-09 | Contributor| 根因是最外层 StackPane 背景链路在浅色/暗色间没有统一归口：固定透明会让暗色露出 Scene 白底，删除透明又会露出 `.root` 浅色蓝白渐变；改为由 ScreenManager.applyUserTheme() 按主题写入 wrapper 背景，浅色透明、暗色近黑渐变，并补齐暗色 editable ComboBox 的 text-field/arrow-button 子节点样式。 | 编译通过 |
| 2026-07-09 | Contributor| 将 style.css 的 `.root` 背景从蓝白渐变改为中性雾白渐变，避免浅色或背景链路兜底时出现泛蓝底色。 | CSS 调整 |
| 2026-07-09 | Contributor| 1. 暗色根背景、顶栏、玻璃卡片、列表卡、PixelCard 统一压深到黑曜石色阶；2. 登录/注册页移除 `.glass-card` 的白色内联背景，交由主题 CSS 接管；3. 暗色头像/头像徽章补稳定圆角与透明子标签，避免白色方框露底。 | 编译通过，FXML XML 解析通过 |
| 2026-07-09 | Contributor| **真正根因**：`PixelCard.layoutChildren()` 把内部 `cardBody`（Region，默认 maxWidth=MAX_VALUE）和 FXML 内层 `glass-card`（VBox，默认 maxWidth=USE_COMPUTED_SIZE=preferred）都放在同一卡片身体区域内，虽都传 `fillWidth=true/fillHeight=true`，但 VBox 的 max 限制使其只按内容 preferred size 居中、不被拉满，导致内层比外层小、形成 2-3px 间隙的双线。次要原因：两层圆角/线宽参数原本也不一致（pixel-card-body 8/1.2 vs glass-card 12/1.5，且 FXML 内联 `-fx-background-radius:16` 又把背景圆角顶成 16）。**修复**：1. `PixelCard.java` layoutChildren 遍历 FXML 子节点时，对 Region 先 setMaxWidth/Height(Double.POSITIVE_INFINITY) 再 layoutInArea，使内层被拉到与 cardBody 完全一致的尺寸；2. `.glass-card` 的 background-radius/border-radius 统一为 8、border-width 改 1.2，同步外层规格；3. 移除 LoginUI.fxml、RegisterUI.fxml 内联 `-fx-background-radius:16`，并加 maxWidth/maxHeight=Infinity 兜底。 | mvn compile 通过；PrintWindow 截图经多模态确认已由双线变为单条干净描边 |
