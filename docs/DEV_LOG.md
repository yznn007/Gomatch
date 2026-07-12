# 开发过程记录

## 项目信息

- **项目名称**：Gomatch
- **开始日期**：2026-07-04
- **技术栈**：JavaFX (FXML + CSS) + Socket + JDBC + MySQL + Redis

---

## 日志

### 前端（Frontend）

> FXML 布局、CSS 样式、动画特效、视觉组件

| 时间 | 作者 | 工作内容 | Problem/备注 |
| 2026-07-12 17:02 | Contributor| fix(ci): 修复 build-client 在预装新版 WiX 时失败 (PR #57) | 将 `choco install wixtoolset --version=3.14.1` 替换为 `choco upgrade wixtoolset`，避免 runner 已预装新版 WiX 时 Chocolatey 退出非零导致构建失败。 | 验证通过 |
| 2026-07-12 17:30 | Contributor| fix(ci): 修复版本正则表达式不支持 alpha/beta 等预发布标识 | 将 `client-release.yml` 中的版本校验正则从 `^\d+\.\d+\.\d+(?:[-.]\d+)?$` 改为 `^\d+\.\d+\.\d+(-[\w.]+)?$`，支持 `2.0.0-alpha`、`2.0.0-beta.1` 等符合 semver 规范的预发布标签。 | 验证通过 |
| 2026-07-12 02:24 | Contributor| fix(client): 连接成功提示隐藏服务器 IP 和端口以防泄漏 | 在设置页和服务器配置弹窗中，连接成功后提示统一改为使用无参的已连接词条，不再显式渲染服务器的具体 IP 和端口，并清理了 messages 配置文件中多余的带参数词条。 | 验证通过 |
| 2026-07-12 00:05 | Contributor| fix(client): 修复本地凭据存储逻辑以通过单元测试并防范明文密码残留 | 优化了 `CredentialStore.java`：1. 仅在传入密码非空时写入 `password` 属性；2. 增加 `loadUsername` 时的旧明文密码判定逻辑，当解密失败时自动剥离明文密码并更新存储文件，修复了 `CredentialStoreTest` 两个单元测试 100% 失败的缺陷。 | 编译与测试成功 |
| 2026-07-11 23:05 | Contributor| fix(client): 修复好友请求列表无法渲染及好友头像缺失的 Bug (Issue #17) | 在 FriendsController 中重构 FRIEND_LIST 和 FRIEND_REQUEST_LIST 消息解析逻辑，改用 opt/optString 等防御性 API 读取字段，避免 requestId 作为数字类型传回时触发 `r.getString()` 产生的 JSONException 导致中断；并将头像字段映射修改为与服务端一致的 avatarUrl。 | 编译与测试成功 |
| 2026-07-11 22:45 | Contributor| fix(client): 修复应用在后台时静音失效与内存泄漏的 Bug | 将失焦静音监听器重构为 ScreenManager 下的全局 Application 级事件，通过 BackgroundMusicManager.setMutedByBackground 属性独立控制静音状态；删除 SettingsController 中的旧局部监听器及 checkbox 状态强改逻辑，彻底杜绝 SettingsController 实例多次被 Stage 持有造成的内存泄漏与状态覆盖行为。 | 验证通过 |
| 2026-07-11 22:00 | Contributor| fix(server): PR code review 修复——消除 OSS 重复代码、修复 leaveRoom 通知顺序 Bug、消除 TOCTOU 竞态 | 1. **ClientHandler**：提取 `generateAvatarUrl(String)` 统一处理 OSS 判断与 try-catch，消除 4 处内联三元重复；2. **RoomManager.leaveRoom**：修复先 `room.leave()` 再判断通知名单的顺序 Bug，补全观战者通知与引用清理，移除危险的 `.clear()` 调用；3. **Room / ClientHandler.cleanup()**：新增 `cleanupDisconnect` 同步方法，将 WAITING / 非 WAITING 状态判断原子化到 Room 锁内，消除 TOCTOU；4. **init.sql**：补充生产环境安全警告注释；5. **AsciiTextCanvas**：补充 D3D 管线 NPE 修复说明注释 |
| 2026-07-11 | Contributor| fix(auth): 修复旧密码改密兼容 | 旧明文密码账户改密时使用兼容校验，成功后统一写入 BCrypt 哈希；新增旧密码与 BCrypt 密码匹配回归测试。 |
| 2026-07-11 | Contributor| fix(build): 取消 Maven forked javac 编译 | Windows 下 forked javac 无法访问 common 模块的 classpath，导致客户端误报 `com.gomatch.common` 缺失；统一改用 Maven 进程内编译。 |
| 2026-07-11 | Contributor| fix(scripts): 客户端启动不再强制清理构建目录 | 移除四份客户端脚本中的 `clean`，避免运行中的 JavaFX MediaPlayer 锁定音频资源时导致启动失败。 |
| 2026-07-11 | Contributor| fix(client): 空服务器地址回退至发布服务器 | 客户端默认连接改为 `${GOMATCH_SERVER_HOST}:8888`；登录弹窗与设置页的 IP 输入框留空时使用该默认地址，填写 IP 才保存为本地覆盖；连接状态不显示服务器地址。 |
| 2026-07-11 | Contributor| fix(config): 允许无认证 Redis 使用 `.env` 配置 | Redis 密码保持环境变量来源；未启用 Redis 认证时允许其为空，避免旧本地配置无法启动。 |
| 2026-07-11 | Contributor| docs(config): 补充运行凭据优先级 | 明确系统环境变量、`.env` 与非敏感资源默认值的优先级，以及 `GOMATCH_ENV_FILE` 的定位规则。 |
| 2026-07-11 | Contributor| feat(config): 统一运行时凭据为 `.env` 与环境变量 | MySQL、Redis 与 OSS 凭据不再提交到资源配置；系统环境变量优先于 `.env`，服务端启动校验必填 MySQL/Redis 配置。 |
|------|------|----------|-----------|
| 2026-07-11 14:36 | Contributor| ci(client): 新增 MSI 自动发布流程 | 新增本地/CI 共用 MSI 打包脚本、标签 Release 工作流与 `docs/releases/` 版本说明约定；标签对应说明缺失时自动生成 GitHub 变更摘要。 |
| 2026-07-11 13:42 | Contributor| refactor(database): 移除未接入的用户设置与音乐表业务 | 删除 `t_user_settings`、`t_user_music`、未接入设置/媒体协议及其 database 实体/仓储；云端配置继续使用 `t_user_profile.config_json`，头像 OSS 与审计日志保持不变。 |
| 2026-07-11 03:54 | Contributor| style(client): 将登录和注册界面的手机号码前缀与输入框合并到同一行，只留前缀代码并实现登录切换栏 | 1. **LoginUI/RegisterUI.fxml**：合并国家区号 ComboBox 和输入框至同一行，并缩窄宽度至 65px；2. **Login/RegisterController**：区号项移除中文仅留 +86 等代码；3. **LoginUI.fxml / LoginController**：在登录标题下加入“账号登录/手机号登录”的极简 HBox 切换栏，切至手机号时展开区号下拉框并改变占位符为“手机号”，切至账号时隐藏区号，并支持在记住密码时根据账号类型自动切换 Tab 加载；4. **style.css**：为登录切换栏定制极简双主题底划线样式；5. **RegisterUI.fxml**：手机号默认文字简化为“手机号”，并移动行位置。 |
| 2026-07-11 03:20 | Contributor| fix: 修复大厅音乐选择与播放顺序在下次登录时无法同步的 bug | 1. **UserSettings**：新增 `selectedSongPath` 和 `playOrder` 字段，通过 GSON 自动持久化；2. **BackgroundMusicManager**：将 `currentPlayOrder` 升级为 `ObjectProperty<PlayOrder>` 以支持响应式 UI，增加 `selectAndPlaySong` 以区分用户主动选歌与自动切歌，并实现 `applySettings` 用于应用/同步本地及云端偏好；3. **SettingsController**：将选歌下拉框改为调用新 API，并对播放顺序属性添加 Listener，在云端同步播放顺序时自动刷新按钮文本；4. **ClientMain / ConfigManager**：分别在本地加载和云端配置同步完后自动触发 `applySettings` 平滑过渡应用。 |
| 2026-07-11 00:57 | Contributor| physics: 减小 3D 移动卡片 (PixelCard) 的 Z 轴摆动幅度 | 进一步抑制摆动的响应振幅：1. 鼠标滑入瞬间的脉冲推力因子从 0.25 降至 0.06，防止晃动起点过大；2. 鼠标平移速度带来的摆动角加速度传导系数从 0.000025 降低至 0.000006；3. 单帧最大推力限制从 0.15 限制到 0.04，使得晃动幅度和摆角被严格约束在极小且微弱的精致感官范围内。 |
| 2026-07-11 00:49 | Contributor| physics: 优化 3D 移动卡片 (PixelCard) 挂绳悬挂动力学，引入偏移越远阻力越大机制 | 优化卡片物理 Z 轴摆动（挂绳特性）的重力恢复力与摆动阻尼公式。通过引入基于摆动角度 angle 绝对值和平方的非线性刚度调节与大摆角阻尼增强算法，使卡片在大角度晃动时承受成倍的阻力和回弹恢复力，从而在小角度下保持轻盈飘逸，大角度下能迅速平抑和阻尼，使动态吊挂交互更加质感与沉稳。 |
| 2026-07-11 00:27 | Contributor| fix: 第二轮前端 UI 合规性审查与全面修复 | 1. **style.css**：定义 .success-text, .rank-text, .win-rate-text, .expiry-text 样式类及其暗色 theme-dark 覆盖。2. **ProfileUI.fxml / ForgotPasswordUI.fxml**：移除 rankLabel, winRateLabel, codeExpiryLabel 的硬编码内联样式，改用相应的主题类。3. **BoardController**：剩余时间少于10s时的倒计时警告色换用 themeColor()。4. **RankingController**：无数据标签设为 themeColor；当前用户 "(我)" 高亮从违禁天蓝色 #0ea5e9 改为雅致青色 (Teal) 主题色。5. **FriendsController**：修复在线状态 activeLabel 多余分号的语法错误；删除/拒绝按钮移除内联红色直接改用 btn-danger 样式类；空提示标签换用 themeColor；申请头像从硬编码橘色改为 themeColor 橘色；errorLabel 移除原生颜色 red/green。6. **ProfileController**：密码错误/成功提示与对战记录分值全部改用 themeColor；showUploadStatus 将硬编码颜色名称转换为主题自适应 Hex。7. **ForgotPasswordController**：所有动态状态消息 setStyle 色值全部替换为 themeColor。 |
| 2026-07-11 00:16 | Contributor| fix: 弹窗 UI 合规性修复 — 主题注入缺失、硬编码颜色、语义错误 | 1. **GameOverController**：积分变化标签 +/- 颜色换用 themeColor()（浅色/暗色分别使用深绿深红/浅绿浅红）。2. **QuickMatchReadyUI.fxml + QuickMatchReadyController**：准备按钮从 btn-danger（红色）改为 btn-primary（语义正确）；所有 matchStatus/countdownLabel 的硬编码颜色全部换用 themeColor()，并将 cancelMatch 状态从鲜蓝 #3b82f6 改为中性板岩灰。3. **BoardController.showGameOverDialog**：从手动注入 CSS 改为调用 applyStylesheetPublic + applyUserTheme + applyUserFont，修复暗色主题下结算弹窗背景和颜色异常。4. **BoardController.showPendingRequestDialog**：同上，补充完整主题注入。5. **ScreenManager**：新增 applyStylesheetPublic() 公共方法，供外部类使用。 |
| 2026-07-11 00:08 | Contributor| fix: UI 合规性全面修复 — 17 处违反设计约定的样式问题 | CSS: btn-danger/success 改极简底线风格+8px圆角，player-info-card-self 鲜蓝→板岩灰，arena-match hover 天蓝→中性，nav-btn/mode-btn-primary 圆角统一8px。FXML: AvatarPreviewUI 默认头像背景去天蓝；CropAvatarUI 整体背景改为半透磨砂、按钮改用CSS类；ForgotPasswordUI 验证码Label去鲜蓝。Java: BoardController 回放分数/roleLabel白方/弹窗background/ProgressIndicator全部换themeColor；selfTag去鲜蓝；CasualLobbyController 空状态提示+观战头像换themeColor/板岩灰。 |
| 2026-07-05 01:24 | Contributor| style: 创建全局 white-minimalist CSS：卡片/按钮/表格/输入框圆角、页面透明背景+Separator 分割 | |
| 2026-07-05 01:24 | Contributor| refactor: Lobby 重构：上传头像+昵称、匹配/排位入口、排行榜侧边抽屉、好友/设置 | |
| 2026-07-05 01:24 | Contributor| feat: 新增 CasualLobbyUI（房间列表+观战+创建/加入/快速匹配）和 RankedLobbyUI（段位+匹配按钮） | |
| 2026-07-05 01:24 | Contributor| feat: 对战记录整合进个人中心 ProfileUI | |
| 2026-07-05 01:24 | Contributor| style: 匹配/排位按钮改为切角长方形（-fx-shape SVG path）+ -fx-background-insets 伪描边 | hover 仅变背景色，无特效 |
| 2026-07-05 18:35 | Contributor| feat: 实现本地字体内置及个人中心重构，全面优化白色极简 UI | |
| 2026-07-05 18:37 | Contributor| style: UI 进一步深度美化：(1) 排行榜抽屉改为 StackPane 浮层，展开时完全不挤压主大厅内容；(2) 原生 Decorated 窗口改为 Undecorated 沉浸式自定义标题栏，支持 Windows Aero Snap 贴边分屏、双击最大化、拖拽还原定位、控制区避让鼠标等；(3) 按钮及卡片改为 8px 圆角，摒弃切角样式；(4) 实现 CodingNepal 风格磨砂玻璃卡片与无 Label 底部下单线下划线输入框，支持鼠标 Hover/Focus 时按钮式 Cover 底色高光反馈，外加 38px 阴影显著显化卡片深度；(5) 客户端内置集成并动态注册 Maple-Mono-NF-CN 字体文件 | mvn clean compile 通过 |
| 2026-07-05 18:37 | Contributor| style: 白色极简风格深度规范化：回退微灰卡片底色还原纯白磨砂；将登录/注册卡片白度密度（卡片 0.40 透，按钮 0.85 实）；将登录/注册卡片主按钮、主大厅按钮元素及二级界面全部按钮元素统一重构为与输入框相同的"底线描边+悬浮遮罩"极简样式；彻底清理战绩、排行榜、房间列表在点击行时的蓝色残留，修复行选中时文字变白的 JavaFX 渲染缺陷。 | mvn clean compile 通过，Git 提交规范代码 |
| 2026-07-06 16:43 | Contributor| 重构设置界面为与个人中心一致的左导航右内容大卡片布局（SettingsUI.fxml），将设置细分为"基础设置"、"图像设置"、"声音设置"、"UI风格"等六个子面板；同时将退出游戏二级确认升级为粒子无框风格自定义弹窗（ConfirmExitUI.fxml）；在 style.css 中对 ComboBox 和 CheckBox 重写了磨砂白极简底线风格与动画反馈样式 | 编译验证通过，完美匹配白色磨砂极简风格 |
| 2026-07-06 16:43 | Contributor| 优化页面顶栏边距：修改 style.css 中 .page-root 的默认 padding，将顶部内边距（top-padding）由原来的 30px 调整为 10px，从而使个人中心、系统设置、匹配大厅、排位大厅等全部二级页面的顶栏向上移动并与大厅首部对齐，消除上方多余空行。 | 页面顶栏对齐与排版优化通过 |
| 2026-07-06 16:43 | Contributor| 1. 重构排行榜界面（RankingUI.fxml）与控制器（RankingController.java），采用与个人中心完全一致的左侧磨砂微透卡片导航栏 + 右侧列表展示的布局，并修复了跳转时丢失当前用户信息状态的 bug；2. 新建好友大厅界面（FriendsUI.fxml）与控制器（FriendsController.java），实现了"我的好友"（支持删除）、"好友请求"（支持接受/拒绝）以及"添加好友"（发送申请）的选项卡动态切换布局；3. 在 LobbyController.java 中将好友入口由原本的弹出提示升级为跳转至新版好友大厅。 | 排行榜与好友页面磨砂侧栏化重构完成 |
| 2026-07-06 16:43 | Contributor| 重构对战大厅/房间大厅界面（CasualLobbyUI.fxml）与控制器（CasualLobbyController.java），采用与个人中心完全一致的左侧磨砂微透卡片导航栏（将"房间列表"与"观战对局"分为两个独立的侧栏按钮）+ 右侧切换面板布局，并在房间列表中提供美观的控制按钮，补全了原本缺少的 handleRefresh 刷新控制方法。 | 房间大厅磨砂侧栏化重构完成 |
| 2026-07-06 23:52 | Contributor| 1. 还原排位大厅段位页面（RankedLobbyUI.fxml）和控制器为经典的居中排位匹配布局，移除不必要的侧栏；2. 重构房间大厅等待界面（RoomWaitingUI.fxml）与控制器（RoomWaitingController.java），采用与个人中心完全一致的左侧磨砂玻璃侧栏布局，将"房间玩家"和"房间聊天"拆分为两个独立的导航选项卡进行右侧面板切换展示。同时把原先底部的"离开房间"大按钮升级为顶部左侧的极简 « 离开房间 统一返回按钮。 | 房间等待大厅磨砂侧栏化重构完成，排位段位大厅还原成功 |
| 2026-07-06 23:52 | Contributor| 1. 完全还原段位大厅界面（RankedLobbyUI.fxml）为独立的、原汁原味的居中简洁段位匹配页；2. 还原并对齐排位匹配的房间进入规则：在 RankedLobbyController.java 中恢复了 JOIN_OK 消息跳转逻辑，无论是排位匹配还是常规匹配，在匹配成功后都会正常进入侧栏化房间等待大厅（RoomWaitingUI.fxml），确保段位大厅依然是独立的，且排位赛具有与普通匹配完全相同的局前等待流。 | 段位大厅完全还原，排位前置房间流对齐完毕 |
| 2026-07-06 23:52 | Contributor| 对战棋盘界面（BoardUI.fxml）右侧面板添加伸缩/折叠侧边栏功能。通过引入 HBox 与磨砂材质的折叠控制按钮，点击可一键收起/展开聊天与玩家控制侧栏；当折叠后，棋盘 Canvas 容器将自适应占满整个屏幕并自动平滑缩放重绘。在 BoardController.java 中注入了 toggleSidebar 控制及界面重新计算绑定。 | 棋盘侧栏折叠缩放功能开发完成 |
| 2026-07-06 23:52 | Contributor| 1. 彻底移除侧边栏顶部新增的"局内工具栏"多余文本与多余的"✕"按钮，使棋盘界面复原至极简结构；2. 采用精炼的单按钮双态控制方案：通过同一个 toggleSidebarBtn 控制折叠与展开，点击 » 隐藏侧栏并切换为 «，点击 « 重新拉回侧栏，确保收起时按钮依然醒目且极简；3. 在折叠按钮右侧同步集成了 dragHandle 隐式拖拽区域，在侧栏展开时允许玩家使用鼠标拖拽手柄左边缘直接双向拉伸调节侧栏宽度（限制在 150px ~ 400px 范围），棋盘区域在拉伸中可自适应完美重绘尺寸。 | 侧边栏折叠按钮精炼与拖拽拉伸功能完全落地 |
| 2026-07-06 23:52 | Contributor| 修复棋盘缩放时 UI 异常的缺陷：在 BoardController.java 中重写了 Canvas 类的 isResizable() 及 min/pref 测算尺寸方法，从而解耦了 Canvas固定宽高大小对父层容器收缩时造成的死锁限制，并结合每次缩放重绘时的 gc.clearRect() 逻辑，彻底解决了窗口在快速缩放拉伸或折叠侧边栏时，棋盘无法正常等比收缩以及留下旧渲染残影的布局异常。 | 棋盘拉伸与折叠自适应自收缩逻辑修复完成 |
| 2026-07-06 23:52 | Contributor| 1. 优化侧边栏拖拽边界数学限制：设定侧栏最大拉伸宽度受限于棋盘正方形特性（左侧需留出至少一个棋盘边长高度 boardContainer.getHeight()），防止棋盘被横向挤压变形，最小侧栏宽度设为 150px；2. 引入窗口缩放侧栏宽度等比例自适应变化：引入 sidebarRatio，窗口大小改变时自动按先前比例缩放侧栏并应用边界限制；3. 彻底禁用聊天区域水平滚动条：在 BoardUI.fxml 的 chatArea 中加入 wrapText="true"（自动换行），文本过长时自动换行，免除不必要左右移动条的视觉干扰。 | 侧栏动态数学限制、等比自适应及聊天换行优化完成 |
| 2026-07-06 23:52 | Contributor| 修复侧边栏缩小时控制按钮文字被挤压显示为"..."的缺陷：在 BoardUI.fxml 中，将原本水平并排在 HBox 中的"悔棋"和"求和"按钮拆出，与"认输"按钮一致改为垂直平铺堆叠布局（各按钮均设为 maxWidth="Infinity" 以充满侧栏横向空间），确保在侧栏拉伸收缩至最小的 150px 状态下，按钮也拥有极佳的水平空间，完全规避字符被挤压并显示为省略号的异常。 | 侧栏按钮防裁剪垂直堆叠优化完成 |
| 2026-07-07 20:43 | Contributor| fix: 棋盘标签改为当前落子方加粗 | BoardUI.fxml 中 blackLabel 默认使用 section-label 导致黑方永远加粗；改为 subtitle，BoardController.updateTurnLabel() 根据 currentTurn 动态设置黑方/白方 font-weight，实现落子方加粗高亮 |
| 2026-07-09 23:44 | Contributor| feat: 集成 React Bits 的 PixelCard 粒子像素化悬浮效果 | 1. 将 React Bits 的 PixelCard 移植为自定义 JavaFX 布局控件；2. 采用 Canvas 与 AnimationTimer 实现 60FPS 像素出现、波动延迟、shimmer 闪烁和自然缩回消失物理算法；3. 修改 LobbyUI.fxml 分别为匹配、AI 对战和排位卡片应用 blue、yellow 和 pink 变体；4. 优化样式，对 canvas 开启 mouse-transparent 使得鼠标事件完全穿透。 |
| 2026-07-09 23:44 | Contributor| feat: 优化 PixelCard 粒子悬浮效果（速度、尺寸与渐变度） | 1. 提升粒子扩散速度，将延迟基准降至 30%；2. 增加粒子基础尺寸上限（1.2px - 4.0px），使其在视网膜屏上更加醒目；3. 引入中心距离归一化比率，实时调配像素点最大大小和不透明度，完美重塑"中心淡、四周浓"的渐变呼吸微粒景观。 |
| 2026-07-09 23:44 | Contributor| feat: 扩大大厅卡片并支持窗口自适应覆盖 | 1. 修改 LobbyUI.fxml，移除原本固定的 260x160 尺寸限制，采用 HBox.hgrow 与 VBox.vgrow 布局自适应填充整个中心窗口；2. 同步微调 style.css 中的图标、标题和子标题字体大小，使其在卡片扩大展示时保持极佳的高级极简视觉比例。 |
| 2026-07-09 23:44 | Contributor| refactor: 微调 PixelCard 参数以优化颜色、密度与动效强度 | 1. 蓝色变体减小 gap 至 7 并混入白色 (#ffffff)，改善淡色并增添星光感；2. 黄色变体增大 gap 至 8，解决粒子过于稠密拥挤的问题；3. 全局增大粒子大小 (1.8px - 5.5px) 并提升出现步长，将速度倍率增加至 3.5 倍；4. 提高中心最小比率限制至 0.35，增强整体动效可视度。 |
| 2026-07-09 23:44 | Contributor| refactor: 匹配卡片配色升级为星光色 | 1. 修改 BLUE_CONFIG 颜色序列，额外引入淡褐色/淡棕色 (#d2b48c 和 #b45309) 以及高概率纯白，调配出天蓝、深蓝、淡棕与白光交汇的宇宙星河"星光色"。 |
| 2026-07-09 23:44 | Contributor| refactor: 微调 PixelCard 粒子扩散速度 | 1. 将扩散延迟系数从 0.3 适度放宽至 0.65，使微粒向四周平铺扩散的速度节奏处于舒适、优雅的涟漪波动区，避免过度局促与突兀感。 |
| 2026-07-09 23:44 | Contributor| refactor: 缩减 PixelCard 粒子尺寸大小 | 1. 将粒子基础尺寸区间由 1.8px - 5.5px 微调至 0.8px - 3.0px，在维持星光配色可见度的同时，恢复精细雅致的超细微"星尘"粒子感，消除像素颗粒偏大显粗糙的观感。 |
| 2026-07-09 23:44 | Contributor| refactor: 重构 PixelCard 颜色表，实现主色调为主的五彩斑斓星光效果 | 1. 匹配卡片（蓝色）彻底移除较重的深棕，加入高纯白及少量浅金、粉红与淡紫点缀；2. AI 卡片（黄色）融入少量蜜桔橙、薄荷绿与晴空蓝；3. 排位卡片（粉色）融合了蓝、紫、黄、橙，调配出清新舒适的五彩斑斓悬停特效。 |
| 2026-07-09 23:44 | Contributor| refactor: 稍微调大 PixelCard 粒子并减缓动效节奏 | 1. 将粒子尺寸范围由 0.8px - 3.0px 适度放大至 1.2px - 3.8px，提高星尘可见性；2. 将基础流速倍率由 0.0035 减缓至 0.002，并降低 sizeStep 至 0.45 及呼吸闪烁随机速度，营造静谧雅致的流光闪动效果。 |
| 2026-07-09 23:44 | Contributor| refactor: 再次增大 PixelCard 粒子尺寸上限 | 1. 将粒子基础尺寸区间由 1.2px - 3.8px 进一步扩充至 1.6px - 4.8px，使得像素颗粒在拉伸铺开后具有更饱满的外观与出色的显示可见度。 |
| 2026-07-09 23:44 | Contributor| refactor: 净化卡片五彩色彩表，消除相悖互补色 | 1. 微调颜色数组，确保各变体微粒完全限制在主色调同色系或邻近色范围内；2. 匹配卡片（蓝色）使用靛蓝与淡紫；3. AI卡片（黄色）使用暖橙与杏色；4. 排位卡片（粉色）使用霓虹粉与香芋紫，从而在保留多彩层次的同时实现极佳的视觉协调性。 |
| 2026-07-09 23:44 | Contributor| feat: 结合 React Bits 集成 TiltedCard 3D 悬停倾斜效果 | 1. 在 ScreenManager.java 中为场景及弹窗配置 PerspectiveCamera(false)，引入不影响 2D 绝对定位的透视投影；2. 在 PixelCard 中注册 Rotate 变换并监听鼠标移动和拖拽，依据鼠标距卡片中心偏移量，在 AnimationTimer 中使用指数衰减算法实现 12 度 3D 偏转和 1.05 倍缩放 Spring 回弹过渡；3. 利用 setViewOrder 属性实现悬停卡片置顶，避免相邻遮挡。 |
| 2026-07-09 23:44 | Contributor| refactor: 增大 PixelCard 3D 旋转偏角振幅 | 1. 将 3D 旋转的倾斜角振幅由 12 度调高至 18 度，以提供更加敏感、显著和动感的空间倾斜效果。 |
| 2026-07-09 23:44 | Contributor| refactor: 悬停底色适配卡片主色调，调和为淡色透明状 | 1. 在 LobbyUI.fxml 中为匹配和 AI 卡片标签分别增加样式名 arena-match 和 arena-ai；2. 在 style.css 中为匹配（蓝色）、AI对战（黄金）、排位（粉色）重写独立的 hover 样式，使用 12% 不透明度的专属主色背景和 35% 不透明度的底边框，代替原本统一的板岩灰，达成融洽自然的淡色透明悬浮体验。 |
| 2026-07-09 23:44 | Contributor| feat: 集成 Lanyard 原生单摆动力学悬挂吊绳效果 | 1. 在 PixelCard.java 中加入 Region cardBody，隔离背景与边框渲染，在 layoutChildren 中限制卡片身体和外部子内容仅占用 Y=45px 偏下的卡片本体区；2. 增加 Z 轴旋转及单摆力学公式，根据鼠标滑过/拖拽速度对卡片施加角加速度，驱动 Z 轴偏转摆动；3. 在 Canvas 顶部使用二次贝塞尔曲线和金属扣动态绘制跟随速度摆动弯折的主题色吊带环圈；4. 适配 CSS 完成半透磨砂背景的隔离转移。 |
| 2026-07-09 23:44 | Contributor| refactor: 调小 Lanyard 吊绳晃动幅度并增加阻尼阻力系数 | 1. 降低 setOnMouseEntered 鼠标滑入随机推力至 0.25；2. 在单摆公式中将重力恢复系数由 4.5 提升至 6.5 增强回正力，将阻尼由 2.0 提升至 3.5 快速吸能结算；3. 将鼠标划过速度对角加速度的推力比重由 0.00008 大幅缩减至 0.000025，并收窄瞬时推力最大值为 0.15，彻底解决原本剧烈摆荡的问题，提供优雅静谧的微小回弹。 |
| 2026-07-09 23:44 | Contributor| refactor: 还原粒子流动波速逻辑，并新增粒子尺寸随卡片大小自适应缩放 | 1. 回退还原 PixelCard 粒子流动速率的 widthPlusHeight 绑定参数；2. 在 initPixels 中计算基于 300px 卡片宽度的 scaleFactor，传入 Pixel 构造函数对粒子尺寸 minSize/maxSize、绘制居中偏置 centerOffset、以及出现 sizeStep 与消失收缩速率进行等比缩放，确保粒子在大/小窗口下视觉比例恒定。 |
| 2026-07-09 23:44 | Contributor| feat: 重构 AI 对战选择界面为三卡片布局 | 1. 修改 AiLobbyUI.fxml，导入 PixelCard 并移除原来的普通列表按钮，改为与大厅主页一致的自适应横向 HBox 卡片结构；2. 将简单、中等、困难三个难度映射到 blue（🌱）、yellow（⚡）和 pink（🔥）三种主色调变体与专属徽章图标，添加细小的说明介绍文字；3. 在 style.css 中为新增的描述类 .arena-desc 设定排版字号和悬浮文字色。 |
| 2026-07-07 17:36 | Contributor| feat: 实现业务 UI 随窗口拉伸整体等比自适应缩放 | 1. 在 ScreenManager.java 的 switchTo 中，将主业务 VBox 容器 mainLayout 锁定为 1920x1080 参考尺寸（标准 16:9）；2. 对其直接挂载 Scale 缩放变换，并移除了额外的 centerWrapper 层；3. 监听顶层 wrapper 的实际窗口宽高变化，取最窄边比例平滑更新 Scale 缩放系数，彻底消除了标题栏与内容区缩放不一致而产生的多余黑边。 |
| 2026-07-07 17:36 | Contributor| refactor: 调整默认窗口尺寸与设计缩放基准为 1280*720 黄金比例 | 1. 将 ScreenManager.java 中的默认 WIDTH 和 HEIGHT 常量修改为 1280 和 720；2. 业务 UI 在此基准尺寸上自适应等比缩放，窗口支持自由拉伸，使得在任何尺寸拉伸时内部都完美按 16:9 相对大小进行矢量对齐，既适合本地测试又实现了标准宽屏。 |
| 2026-07-07 17:36 | Contributor| fix: 引入事件过滤器拦截边框拖动，解决拉伸交互冲突及状态重置 Bug | 1. 在 ScreenManager.java 中引入 MouseEvent 事件过滤器，在捕获阶段优先拦截边框拉伸事件，并通过 e.consume() 阻断事件向下传递至子组件，规避拉伸窗口时引发的子组件物理拖拽冲突；2. 增加 MOUSE_RELEASED 阶段的监听器，在鼠标松开时将 resizeMode 强制重置为 NONE，修复了释放鼠标后依然会异常缩放窗口的遗留缺陷。 |
| 2026-07-07 17:36 | Contributor| fix: 优化大厅个人头像信息芯片宽度与点击域 | 1. 移除 LobbyUI.fxml 中 playerInfoBox 头像芯片的 HBox.hgrow="ALWAYS" 强制横向填充，将其还原为只包裹头像和昵称的紧凑胶囊组件；2. 增设隐藏 Region 填充组件，将点击操作范围完全限制在头像可见的芯片范围内，断绝了在顶部栏空白处误触进入个人中心的操作体验 Bug。 |
| 2026-07-07 17:36 | Contributor| fix: 引入边缘阻尼渐变并结合静态投影检测，彻底根除 3D 卡片四周及下边缘抖动 Bug | 1. 在 handleMouseMoved 中计算鼠标距离卡片身体边缘的最短像素距离，在 25px 边缘过渡区内计算 0.0~1.0 的阻尼因子，使得鼠标跨越卡片边界时 3D 偏转角平滑衰减至 0；2. 结合 setOnMouseExited 中的静态无偏转包围盒屏幕投影校验，拦截旋转位移产生的假滑出信号，彻底治愈了下边缘在鼠标滑出时的反复触发与抖动。 |
| 2026-07-07 17:36 | Contributor| fix: 清除顶部栏分割线内边距并开启鼠标穿透，解决按钮下边缘点击遮挡 Bug | 1. 在 style.css 中将 .section-divider 的 -fx-padding 设为 0，彻底移除 Modena 默认主题中分割线上下各 10px 的不可见外边距死区；2. 在 LobbyUI.fxml 和 AiLobbyUI.fxml 中为 Separator 加上 mouseTransparent="true" 物理穿透，彻底根治了顶栏按钮下方边缘点击被分割线 hitbox 拦截截断的问题。 |
| 2026-07-07 21:34 | Contributor| refactor: 将点阵背景动画速度重构为基于 Delta Time 的时间步长计算 | 1. 在 ShapeGridBackground.java 中，用 SPEED_X_PER_SEC/SPEED_Y_PER_SEC 代替每帧固定位移，引入基于时间步长 dt 的无缝包裹位移；2. 增加对最大帧时间 dt 的阈值限制 (0.1s)，避免应用挂起恢复或帧率突变时粒子产生位置瞬移，确保在不同刷新率显示器下流动速度一致恒定。 |
| 2026-07-07 21:53 | Contributor| style: 为大厅卡片与 AI 对战卡片设计并添加柔和的悬浮及按下状态阴影特效 | 1. 在 style.css 中为 .pixel-card-body 引入基础极简软投影，提供细腻的纸张悬浮深度感；2. 为匹配（蓝色）、AI对战（黄色）、排位（粉色）三个主题色分别定制专属的 hover 投影（24px gaussian blur, 18% 不透明度），增强 3D 偏转侧倾时的空间光感表现；3. 增设 pressed 状态阴影收紧收敛（12px gaussian blur），建立直观敏捷的按下触觉交互反馈。 |
| 2026-07-07 21:53 | Contributor| fix: 修正 PixelCard 粒子在快速划入/划出及闪烁时可能产生的渲染残留缺陷 | 1. 在 Pixel.draw() 中对 size<=0 进行拦截，在 shimmer/disappear 中进行 Clamp 边界夹紧，阻断异常渲染；2. 增加 Canvas 动态可见性管理（在开始 appear 时可见，在 disappear 结束时设为不可见），彻底打断 JavaFX 3D 渲染管线及脏区缓存失效引发的粒子显示残留。 |
| 2026-07-08 12:41 | Contributor| feat: 排行榜拆分为全服排行与好友排行分榜（前端部分） | 改造 RankingUI.fxml 左侧导航栏增加"全服排行"与"好友排行"两个选项按钮，为标题分配 fx:id；重构 RankingController.java 实现 Tab 切换逻辑、多类型 Mock 数据展现与网络消息类型过滤，防范并发乱序渲染。 |
| 2026-07-08 14:22 | Contributor| feat: 登录界面标题整合纯 JavaFX 自定义 ASCIIText 特效 | 1. 采用 3D 正向光栅化高精密度采样（将采样量暴增至 480x120，共 5.7 万个点），并在 X, Y, Z 轴上大幅降慢且收窄 3D 几何形变波动以减少扭动；2. 将 ASCII 单字字号收缩为极精密的 5px（asciiFontSize=5），网格分辨率自动扩充至 174x76（共 1.3 万个字符），使字符点阵凝聚感和高密度精细度翻倍；3. 将字身黑色判定阈值门槛压低至 45，使核心黑色字符占比高达 95% 以上，并对红色、蓝色色散通道实施大幅亮度衰减融入暗色系；4. 布局拓宽至 480，销毁 AnimationTimer 防止泄漏。 | 编译与渲染测试成功 |
| 2026-07-08 23:00 | Contributor| feat: 断线状态可视化 — 玩家卡片显示"断线中"角标 | BoardController：在玩家卡片右下角添加红色"断线中"角标（默认隐藏）；新增 setPlayerOnline(isBlack, online) 方法切换断线状态：断线时卡片透明度 0.55 + 名字变红 + 显示角标；监听 PLAYER_DISCONNECTED/PLAYER_RECONNECTED/RECONNECT_ATTEMPT/RECONNECT_FAILED 消息并在聊天区打印系统提示；新增 blackId/whiteId 字段用于按 userId 匹配玩家卡片 |
| 2026-07-08 23:00 | Contributor| feat: 悔棋/求和按钮仅当前落子方可操作（前端部分） | CSS：新增 .btn-secondary:disabled 样式（灰底 + 低透明 + 光标 default） |
| 2026-07-08 01:19 | Contributor| feat: 对战界面玩家信息卡片重构 + GAME_START 协议增强传递用户名 | |
| 2026-07-08 00:13 | Contributor| refactor: TableView→卡片式列表UI升级，磨砂玻璃视觉语言统一 | |
| 2026-07-08 19:03 | Contributor| feat(server,client): 重构排位匹配系统，修复并优化匹配逻辑 | 匹配大厅界面优化 |
| 2026-07-08 23:00 | Contributor| feat: 实现再来一局（Rematch）功能（客户端部分） | BoardController 新增 handleRequestRematch/handleReplyRematch 方法及 rematchRequested 状态；对局结束弹窗 GameOverController 新增"再来一局"按钮；BoardController 监听 REMATCH_REQUESTED/REMATCH_ACCEPTED/REMATCH_REJECTED 消息，同意后自动重置棋盘开始新对局 |
| 2026-07-08 23:00 | Contributor| feat: GAME_START 协议增强 — 显示对手用户名和积分（客户端部分） | BoardController.onMessage(GAME_START) 解析新增的 blackUsername/blackScore/whiteUsername/whiteScore 字段；新增带分数的 setGameInfo 重载，对手卡片显示真实用户名和积分而非 ID 和问号 |
| 2026-07-11 21:42 | Contributor| fix(client): 修复登录界面 ASCII 粒子标题在 D3D 管线下发生渲染空指针报错的 Bug | 重构 AsciiTextCanvas.java，将原先基于 JavaFX Canvas.snapshot() 转换为在 CPU 堆内存上运行的 java.awt.image.BufferedImage，彻底避开 JavaFX 渲染引擎 (Prism) 与 Direct3D/VRAM 资源回收触发的 `NullPointerException`，保障高频采样效率与零崩溃稳定性。 | 编译与渲染测试成功 |
| 2026-07-09 23:44 | Contributor| fix: 修复黑色主题下登录/注册卡片内部泛白问题 + 全盘黑白样式同步 + 登录标题暗色色调适配 | 1. 将 .theme-dark .glass-card 背景不透明度从 0.48 提升至 0.60；新增 .theme-dark .auth-pixel-card .pixel-card-body 专属深色磨砂背景（0.55 不透明度）并改用深色投影；保留 .theme-dark .auth-pixel-card 外层白色高光投影。2. 将浅色/暗色模式下的 .custom-title-bar 和 .top-bar 背景统一为透明并去除投影。3. 统一侧栏菜单 active/导航按钮/标题栏按钮 hover/active 背景色。4. 补齐暗色模式下缺失的 pressed 状态。5. AsciiTextCanvas 支持暗色模式：根据 ScreenManager.isDarkTheme() 动态切换采样贴图（白底黑字）和输出色调，核心字符使用 #f8fafc，边缘使用柔和浅色偏色，使登录页 Gomatch 标题在暗色背景下清晰可见且风格统一。 | mvn -f client/pom.xml compile 通过 |
| 2026-07-10 12:01 | Contributor| style: 修复头像预览弹窗背景色与顶栏小头像不一致问题 | AvatarPreviewUI.fxml：Circle 填充改为 StackPane 渐变背景 + 150px 圆角，保留 Circle 透明兜底。AvatarPreviewController：pickColor 8 色纯色调色板替换为 gradientFor 7 色渐变调色板（与 LobbyController/RankingController 一致），保证同一用户在小头像、排行榜、大头像预览中颜色完全一致。 |

### 服务端（Server）

> Socket 多线程、游戏逻辑、匹配算法、ELO 排名

| 时间 | 作者 | 工作内容 | 问题/备注 |
|------|------|----------|-----------|
| 2026-07-12 00:09 | Contributor| fix(database): 修复 init.sql 约束与种子数据不匹配导致初始化失败的 Bug | 将 t_user 表的 phone 字段 NOT NULL 约束移除（改为允许为 NULL），并为初始种子用户数据补全符合 E.164 规范的唯一测试手机号，解决初始化数据冲突问题。 | 验证通过 |
| 2026-07-11 23:05 | Contributor| fix(server): 允许手机号为选填进行注册 (Issue #16) | 修复 ClientHandler.java 与 UserDAO.java 中注册校验逻辑对空手机号的不当检验；若手机号为空则不再进行 E.164 规范化和唯一性校验，直接允许空值插入，打通了“手机号选填”的注册通路。 | 编译与测试成功 |
| 2026-07-11 21:42 | Contributor| fix(storage): 修复未配置 OSS 对象存储时，读取头像触发 NullPointerException 的 Bug | 在 ClientHandler.java 与 RankDAO.java 中补齐对 StorageManager.getInstance().isEnabled() 的前置防御检查，防止本地开发或未启用 OSS 服务时触发 `getOssService().generatePresignedUrl(...)` 空指针异常崩溃。 | 验证通过 |
| 2026-07-11 21:35 | Contributor| fix(auth): 修复使用非数字标识符（用户名/邮箱/手机号）登录时匹配到 AI 的 Bug | 1. 在 UserDAO.login 中，若 accountId 解析为 -1，则对 PreparedStatement 的 user_id 参数设为 NULL，避免匹配到 user_id = -1 的 AI 占位记录；2. 在 ClientHandler.cleanup() 及 RoomManager.leaveRoom() 中增强对 WAITING 房间的清理逻辑，防止房主断线或离开时残留幽灵房间导致持久化警告。 | 验证通过 |
| 2026-07-11 21:20 | Contributor| feat(database): 修改 init.sql 中的数据库账号创建与授权配置 | 新增 'gomatch'@'%' 用户的创建与授权规则，保留 'gomatch'@'localhost' 规则，以便支持远程数据库客户端（如 DataGrip 等）及跨主机远程连接。 | 验证通过 |
| 2026-07-11 01:00 | Contributor| fix: 修复 Redis 背景保存失败导致写入命令被禁用的问题 | 1. 通过 redis-cli 将 stop-writes-on-bgsave-error 设置为 no 恢复本地运行；2. 在 RedisManager.init() 中添加了程序内配置，确保在其他环境初始化时同样禁用此选项以提高健壮性。 | 验证通过 |
| 2026-07-11 03:05 | Contributor| docs: 编写服务端部署指南 docs/DEPLOY_GUIDE.md | 整理并编写了服务端的部署文档，包含本地 shaded Maven 打包、JDK 21 与 MySQL/Redis 环境准备、基于环境变量的配置覆盖机制，以及终端直接启动、Systemd 服务化、Docker/Docker Compose 容器化部署这三种运行与守护形式。 | 验证通过 |
| 2026-07-04 13:20 | Contributor| 搭建 Maven 多模块项目骨架，创建 pom.xml 及目录结构 | 使用 JDK 21 |
| 2026-07-06 14:57 | Contributor| 服务端核心功能实现：注册、登录、房间管理、落子同步、五连判胜、认输、断线处理、战绩入库、房间聊天、观战功能、悔棋/求和、断线重连、人机对战 | 基于 Socket 多线程架构 |
| 2026-07-06 14:57 | Contributor| 服务端进阶功能：密码房间、排位匹配（ELO算法）、好友系统（请求/接受/删除）、棋局保存/加载、聊天消息持久化、Socket会话持久化、观战记录、游戏操作记录 | 定时任务自动刷新 |
| 2026-07-06 14:57 | Contributor| 实现退出登录功能：在 Protocol.java 中添加 LOGOUT 和 LOGOUT_OK 协议常量，在 ClientHandler.java 中添加 handleLogout() 方法，处理离开房间、清除会话、标记离线等逻辑 | 编译验证通过 |
| 2026-07-06 16:43 | Contributor| 审查全部模块源码，在 docs/ 下新建三份模块级详细文档：MODULE_COMMON.md（协议常量全览）、MODULE_CLIENT.md（JavaFX 架构/Controller/网络层/AI）、MODULE_SERVER.md（服务器架构/DAO/游戏流程/ELO/Redis/并发设计）；同步更新 README.md 文档索引 | 共覆盖 25+ 个 Java 类 |
| 2026-07-06 16:43 | Contributor| 整理文档体系，在 docs/ 下新建文档中心索引 docs/README.md 分类整合归纳全部 15 个技术及实训辅助文档，并在根目录 README.md 中进行引用 | 提升团队协作可读性与文档查找效率 |
| 2026-07-06 23:09 | Contributor| fix: 修复人机对战4个Bug：AI占位用户外键约束、NPE、gameOver阻断、再来一局状态传递 | 编译验证通过 |
| 2026-07-07 14:11 | Contributor| chore: 分支整理 — server 合并到 dev 并推送到 GitHub，删除 server 和 feature/server-dev 分支，从 dev 切出 fix 分支 | |
| 2026-07-07 14:47 | Contributor| fix: 修复快速匹配有对手仍卡在匹配页面的问题 | RoomManager.quickMatch() 原逻辑先将自己入队再 poll，导致每个请求都先取出自己然后返回等待；改为先尝试从队列取对手，无可用对手再入队，确保第二位玩家点击时能正确配对并通知双方 |
| 2026-07-07 14:47 | Contributor| fix: 普通匹配不再增减积分 | Room.endGame() 中非排位赛普通匹配取消 winner +10 / loser -5 的积分调整，仅记录胜负场次；排位赛仍按原逻辑更新积分 |
| 2026-07-07 20:43 | Contributor| fix: 修复进入观战后无法看到棋盘的问题（服务端部分） | 服务端 handleSpectate() 原只发送 JOIN_OK 导致客户端进入 RoomWaitingUI 卡死；改为发送 GAME_START 触发 BoardUI 切换，并回放所有已有落子使观战者看到实时棋盘 |
| 2026-07-07 20:43 | Contributor| fix: 修复观战列表为空的问题（服务端部分） | 服务端 Room.getRoomListInfo() 返回的 status 为大写 PLAYING |
| 2026-07-07 20:43 | Contributor| fix: AI 简单/中等/困难三个难度实际完全一致的问题（服务端部分） | AIPlayer 新增 Difficulty 枚举并实现差异化策略：EASY 40%随机落子+弱攻击评分（攻3防7）、MEDIUM 沿用原攻防均衡评分、HARD alpha-beta剪枝的深度2 MiniMax前瞻+即时获胜/堵杀检测；Room/RoomManager/ClientHandler 传递 difficulty |
| 2026-07-08 11:51 | Contributor| refactor: 客户端/服务端通信协议统一为 JSON 格式（服务端部分） | 服务端 ClientHandler.dispatch() 改为 Message 分发，所有 send() 调用改为 Message.of()/builder()；handleRoomList/handleRanking/handleQueryRecord 改用 Message.builder() + JSONArray 替代手动 StringBuilder 拼 JSON；Room 广播方法全部改为 Message 参数 |
| 2026-07-08 19:43 | Contributor| fix: 修复对局结束后对战记录缺失的问题 | 1. Room.endGame() 中拆分 MySQL 和 Redis 错误处理，Redis 缓存失败不再掩盖 MySQL 成功保存；2. RecordDAO.saveRecord() 对 startTime 做防御性 null 检查，避免 NPE 导致整条记录保存失败 |
| 2026-07-08 12:41 | Contributor| feat: 排行榜拆分为全服排行与好友排行分榜（服务端部分） | 在 ClientHandler.java 中支持 Protocol.RANKING 携带 type 参数，并在返回数据包时回传分榜类型 |
| 2026-07-08 12:51 | Contributor| chore: 全面排查并修复空指针风险（服务端部分） | ClientHandler.java: handleRegister/handleLogin/handleChangePassword/handleCreateRoom/handleCreateAiRoom 增加 String 入参 null 检查；handleReconnect: token 日志行 NPE 保护；Room.java: getRankByUserId TOCTOU 重复调用风险修复 |
| 2026-07-08 14:18 | Contributor| chore: 移除心跳机制并清理文档（服务端部分） | SessionManager 中 updateHeartbeat 蜕化为会话查询时的活跃时间维护，保留；清理 MODULE_SERVER.md 中的过时心跳描述 |
| 2026-07-08 17:36 | Contributor| feat: 实现心跳机制 + 对局断线重连 + 三次失败后判负（服务端部分） | 服务端 ClientHandler：处理 HEARTBEAT 命令 + 后台线程检测 90s 无消息则关闭连接；Room：对局中玩家断线启动 60s 重连计时窗口，最多 3 次重连机会，全部失败则判对方获胜（endGame reason=disconnect），重连成功后自动回放已有落子 |
| 2026-07-07 19:04 | Contributor| feat: 添加回合计时与超时判负功能 | |
| 2026-07-07 18:48 | Contributor| fix: 修复重复登录问题并完善对局退出逻辑 | |
| 2026-07-07 17:12 | Contributor| fix: 修复空指针异常并处理悔棋求和请求 | |
| 2026-07-07 15:08 | Contributor| fix: 修复对局结果判断和好友请求重复问题 | |
| 2026-07-08 15:40 | Contributor| feat: 实现Token重连机制（服务端部分） | |
| 2026-07-08 18:45 | Contributor| feat: 添加管理员功能（踢人/封禁/强制结束） | |
| 2026-07-08 19:03 | Contributor| feat(server,client): 重构排位匹配系统，修复并优化匹配逻辑 | |
| 2026-07-08 23:00 | Contributor| feat: 实现再来一局（Rematch）功能（服务端部分） | Protocol 新增 REQUEST_REMATCH/REPLY_REMATCH/REMATCH_ACCEPTED/REMATCH_REJECTED 常量；ClientHandler 新增 handleRequestRematch/handleReplyRematch 方法；Room 新增 rematchRequesterId 字段 + requestRematch/replyRematch/resetAndRestartGame 方法，支持双方同意后自动重置棋盘开始新对局 |
| 2026-07-08 23:00 | Contributor| feat: GAME_START 协议增强 — 传递用户名和积分（服务端部分） | Room 新增 blackUsername/whiteUsername/blackScore/whiteScore 字段 + getter；join() 缓存用户名；startGame()/resetAndRestartGame() 查询并刷新双方积分；新增 buildGameStartMessage() 统一构建含用户名/积分的 GAME_START 消息；ClientHandler 全部 7 处 GAME_START 广播改用 buildGameStartMessage() |
| 2026-07-09 14:02 | Contributor| fix: 修复注册时用户名并发重复问题 | 1. UserDAO.register() 新增捕获 SQLIntegrityConstraintViolationException，防止 existsByUsername 和 INSERT 之间的并发竞态绕过唯一性检查；2. UserDAO.existsByUsername() 在 SQL 异常时改为返回 true（保守拒绝注册），而非 false（放行），确保数据库故障时不被绕过 | 编译通过 |

### 客户端（Client）

> Controller 业务逻辑、网络通信、数据管理、AI 对弈、游戏流程

| 时间 | 作者 | 工作内容 | 问题/备注 |
|------|------|----------|-----------|
| 2026-07-11 02:42 | Contributor| fix(ui): 解决弹窗出现方形直角边底色（假圆角）的渲染缺陷 | 1. 在 style.css 的 .dialog-pane 中重写 -fx-background-insets: 0 并配合 -fx-background-radius 消除多层内底色直角；2. 为 Dialog 的 Scene fill 添加 fillProperty 变更监听，防止 JavaFX 在运行时重置透明度，彻底实现完美圆角与毛玻璃边框融合。同时修复 RegisterController 的语法错误。 | 验证通过 |
| 2026-07-11 02:18 | Contributor| feat(ui): 全局启用无边框毛玻璃/磨砂质感自定义弹窗/警示框样式 | 重构 ScreenManager 的 showStyledAlert 并在其基础上封装通用的 showStyledDialog。将全系统 Alert 与 TextInputDialog 全部升级为无边框模式（StageStyle.TRANSPARENT）并配置鼠标拖拽移动逻辑，同时实现圆角外框背景完全透明化。 | 验证通过 |
| 2026-07-11 02:09 | Contributor| fix(ui): 统一排位与快速匹配成功后的“未准备”按钮样式为 btn-primary | 解决排位匹配成功后“未准备”按钮错误被渲染为红色（btn-danger）的体验冲突，使其与快速匹配一致呈现为蓝色主按钮样式（btn-primary）。并避免清除默认 button class。 | 验证通过 |
| 2026-07-11 01:44 | Contributor| docs: 补充安全加固测试用例并根据 Git 提交历史归档历史缺陷记录 | 1. 在 TEST.md 中补全 IP速率限制、超大包保护、恶意重连校验等 8 项安全边界与越权用例；2. 根据 Git 历史整理并补全了 D01~D06 历史缺陷的日志。 | 验证通过 |
| 2026-07-11 01:15 | Contributor| docs: 将 init.sql 的测试账号与排行榜样例数据补充至 README.md | 提升快速启动时的本地调试便利度，方便开发者获取默认的管理员与玩家测试凭据。 | 验证通过 |
| 2026-07-05 01:24 | Contributor| refactor: Swing → JavaFX 迁移：添加 javafx 依赖和 maven plugin，重写 ClientMain 为 Application | FXML 命名空间不能加版本号 |
| 2026-07-05 01:24 | Contributor| feat: 实现 ScreenManager（场景导航/弹窗）、ServerConnector（接收线程+消息分发）、NetworkListener | 首次居中，之后保留窗口位置 |
| 2026-07-05 01:24 | Contributor| feat: 完成 11 个 FXML + 对应 Controller：Login/Register/Lobby/CreateRoom/RoomWaiting/Board/GameOver/Profile/Ranking/Record/About | 预览模式支持无服务器浏览 |
| 2026-07-05 01:24 | Contributor| 客户端代码全面优化：修复 NetworkListener 内存泄漏（ScreenManager.switchTo 前清空监听器）；修复所有 handleBack 丢失 currentUser；修复 ProfileController TableView 改用 GameRecord 并绑定列；修复 BoardController 棋盘随窗口缩放（动态 cellSize）；修复预览模式 NPE（myColor 默认值）；聊天区自动滚底；新增 AppConfig（服务器地址集中配置+Gson 单例）；新增 GameRecord model；Protocol 补充 QUICK_MATCH/RANKED_MATCH/SPECTATE/REGISTER_OK 常量，消除魔法字符串；RegisterController 区分 REGISTER_OK 和 LOGIN_OK；RoomWaitingController 修复 setPreviewMode 时序依赖 | mvn compile 通过 |
| 2026-07-06 13:04 | Contributor| feat: 补全 Protocol 协议常量：HEARTBEAT、SPECTATE_OK/FAIL、RANKED_MATCH 等 | |
| 2026-07-06 13:04 | Contributor| chore: 服务器地址配置化：ClientConfig + client-config.properties | |
| 2026-07-06 13:04 | Contributor| feat: NetworkListener 自动注销：AutoCleanupController 基类 | |
| 2026-07-06 13:04 | Contributor| feat: 注册/登录/改密码/退出响应对接 | |
| 2026-07-06 13:04 | Contributor| feat: 房间创建/列表/匹配流程对接 | |
| 2026-07-06 13:04 | Contributor| feat: 聊天消息时间戳 [HH:mm] | |
| 2026-07-06 13:04 | Contributor| feat: 对战记录+回放：RecordController + BoardController.replayMoves | |
| 2026-07-06 13:04 | Contributor| feat: 观战模式：spectateMode 禁用落子 | |
| 2026-07-06 13:04 | Contributor| feat: 断线检测+全局弹窗 | |
| 2026-07-06 13:04 | Contributor| feat: 心跳机制：30s 发送 / 90s 超时 | |
| 2026-07-06 13:04 | Contributor| feat: 断线重连：3次重试 / 5s 间隔 | |
| 2026-07-06 13:04 | Contributor| feat: AI 对弈：GomokuAI 启发式评分 | |
| 2026-07-06 13:21 | Contributor| fix: 补回 Protocol 遗漏常量，补全 replayMoves 方法 | |
| 2026-07-07 00:18 | Contributor| feat: 设置页面支持用户选择与输入自定义服务器 IP，本地持久化为 config.json 并优化为即时网络连通刷新 | 采用可编辑的 ComboBox，解耦本地存储路径，并适配项目磨砂极简风格 MessageDialogUI 弹窗 |
| 2026-07-06 16:42 | Contributor| 创建 UI 开发分支 `feat/client-business`，并在新建的 scripts/ 目录下放置通用启动脚本 ssc_client.ps1 和 ssc_server.ps1，配置好基于 $HOME 的通用 JDK 21 和 Maven 路径，使用 $PSScriptRoot 进行相对定位 | 脚本已加入 Git 追踪供团队成员使用 |
| 2026-07-06 23:52 | Contributor| 修复预览模式下的匹配卡死缺陷：修改 RoomWaitingController.java 中的 setRoomInfo()，在预览模式下强制使 startBtn（开始游戏按钮）可见，确保玩家能够顺利启动人机对战，打通浏览模式闭环。 | 预览模式匹配正常启动人机对局 |
| 2026-07-06 23:52 | Contributor| 1. 保持排位对局的房间等待交互逻辑与普通匹配完全相同，继续通过 RoomWaitingUI.fxml 统一管理等待与准备流程，维持联机体验一致性；2. 修复 JOIN_OK 协议解析容灾：在 CasualLobbyController.java 和 RankedLobbyController.java 中重写了 JOIN_OK 的命令解析器，自动识别并兼容 JSON 格式以及原生以竖线分隔的文本格式（JOIN_OK\|roomId\|roomName\|playerCount），从根本上避免了联机配对时客户端崩溃黑屏的问题。 | 联机协议兼容与对齐逻辑升级通过 |
| 2026-07-06 23:52 | Contributor| 修正排位段位大厅匹配按钮的流转方向：修改了 RankedLobbyController.java 中的 handleMatch() 方法，当玩家在独立的段位匹配页面中点击"开始排位匹配"按钮时，它会首先切换进入对战大厅（CasualLobbyUI.fxml，即房间大厅），而不是跨越匹配阶段直接生成房间。 | 排位至大厅流转流程修正完成 |
| 2026-07-07 14:01 | Contributor| refactor: 优化登录及设置页IP配置交互，修复假死与连接数泄漏缺陷：1. 登录页新增服务器配置独立二级弹窗(ServerConfigDialogUI)；2. 将连接逻辑迁移至后台守护线程，去除嵌套弹窗，彻底解决 JavaFX 主线程卡死；3. 修复服务端断开连接后 connectionCount 不递减导致达 100 限制后拒绝服务的 Bug | 编译验证通过 |
| 2026-07-07 01:06 | Contributor| fix: 修复悔棋后双方棋盘不一致的 bug | 客户端在本地落子时提前写入 moveHistory，导致自己收到的 MOVE_SYNC 又被追加一次；移除本地写入，使悔棋弹出的两步与服务器保持一致，双方棋盘同步 |
| 2026-07-07 14:01 | Contributor| fix: 修复记住密码不生效的 bug | 1. LoginUI.fxml 中 CheckBox 缺 fx:id 导致 Controller 无法绑定；2. LoginController 新增凭据保存/加载逻辑，存入 ~/.gomatch/credentials.json |
| 2026-07-07 20:43 | Contributor| fix: 修复对局结束不显示黑胜/白胜/平局的问题 | GameOverController.setResult() 只处理数字参数，BoardController 传的是中文文字；default 分支改为直接显示传入的 result 字符串，使胜负结果正确展示 |
| 2026-07-07 20:43 | Contributor| fix: 修复游戏结束结算弹窗内容空白的问题 | ScreenManager.showDialog 内部使用 showAndWait，原调用在 showAndWait 返回后才 setResult，导致弹窗已关闭才设置文字；新增带 setupCallback 的 showDialog 重载，在 showAndWait 前完成控制器设置，BoardController 所有 GameOver 弹窗改为通过回调提前设置结果文本 |
| 2026-07-07 20:43 | Contributor| fix: 修复匹配胜负不更新客户端积分的问题 | 服务端 Room.endGame() 积分更新后新增 USER_SCORE_UPDATE 协议推送最新积分到玩家客户端；BoardController 接收后更新 AppConfig.currentUser 的 score/胜负平场次，使大厅和个人中心实时显示正确积分 |
| 2026-07-07 20:43 | Contributor| fix: 修复快速匹配点击无反应的问题 | CasualLobbyController.onMessage() 缺少 ERROR 协议处理，服务端返回"等待对手…"时被静默丢弃；新增 ERROR/USER_SCORE_UPDATE 协议处理，FXML 增加 statusLabel 状态提示，handleQuickMatch/onMessage 中实时更新匹配状态；LobbyController 同步添加 USER_SCORE_UPDATE 处理确保积分标签即时刷新 |
| 2026-07-07 20:43 | Contributor| fix: 修复观战列表为空的问题（客户端部分） | CasualLobbyController 中过滤观战房间时使用了小写 "playing"，导致条件永远不成立；改为 "PLAYING".equalsIgnoreCase() 比较 |
| 2026-07-07 20:43 | Contributor| fix: 修复进入观战后无法看到棋盘的问题（客户端部分） | 客户端 BoardController 新增 spectatorMode，观战模式自动禁用落子点击和操作按钮 |
| 2026-07-07 23:38 | Contributor| refactor: 清理未使用的类/方法/变量/导入 | 消除 14 个文件的 lint 警告：移除 28 个未使用 import、删除 4 个未使用方法、移除 7 个未使用字段/变量；修复 DatabaseBootstrap Scanner 资源泄漏 |
| 2026-07-07 20:43 | Contributor| fix: 修复棋盘开局倒计时不动与 AI 落子无标记的问题 | 1. 在 BoardController.setGameInfo() 中主动启动倒计时，解决从 AiLobby/RoomWaiting 切换后新 BoardController 因 CopyOnWriteArrayList 快照机制收不到同一条 GAME_START 导致倒计时卡死；2. 新增 lastMoveX/Y 记录最后落子，redrawAll 中绘制红色加粗圆环标记，覆盖本地落子、MOVE_SYNC、悔棋、回放等场景；3. 回放模式启动时停止倒计时，避免残留 |
| 2026-07-07 23:38 | Contributor| fix: AI 简单/中等/困难三个难度实际完全一致的问题（客户端部分） | AIPlayer 新增 Difficulty 枚举并实现差异化策略 |
| 2026-07-07 23:38 | Contributor| fix: 消除编译警告 | 删除 database/src/main/resources/logback.xml（库模块不应包含导致运行时冲突）；RecordController 添加 @SuppressWarnings("unchecked") 消除 JavaFX 泛型 unchecked 警告 |
| 2026-07-08 11:51 | Contributor| refactor: 客户端/服务端通信协议统一为 JSON 格式（客户端部分） | 1. 新增 common/Message 类（fromJson/toJson/builder/of/cmdOnly），添加 org.json 依赖；2. NetworkListener 接口改为 onMessage(Message msg)；3. ServerConnector.send() 改为 send(Message) + send(String cmd) 重载；4. 所有 14 个客户端 Controller 的 onMessage 和 send() 全部迁移到 Message 模式，列表类响应（房间/排行榜/好友/对战记录）统一解析 JSONArray 替代竖线分割数组；5. 修复 handleRoomList 错误使用 RANKING_LIST→ROOM_LIST 及 FriendsController.handleRejectRequest 遗漏的旧格式调用 | 全项目零残留旧 pipe 协议代码，编译通过 |
| 2026-07-08 12:51 | Contributor| fix: 修复排行榜页面空白的问题 | 1. RankingController 初始化时主动渲染占位，处理 ERROR 消息，使用 optInt/optString 解析数值字段，预览模式加载本地假数据；2. LobbyController 抽屉排行榜同步修复解析方式 |
| 2026-07-08 19:38 | Contributor| fix: 回放从头开始 | BoardController.replayMoves 默认 replayIndex=0（空棋盘），而非从最后一步开始 |
| 2026-07-08 19:38 | Contributor| fix: 快速匹配计时从1开始 + 取消匹配按钮变红 | 1. CasualLobbyController 匹配计数器从 0 开始正向递增（首秒后变1），不限循环次数；2. 匹配中 quickMatchBtn 添加 btn-danger 红色样式，取消匹配时移除 |
| 2026-07-08 19:43 | Contributor| fix: 修复个人中心点击回放错误提示"暂无棋谱数据"的 Bug | 1. 在 ProfileController.java 增加 extractBoardData 辅助方法，兼容 JSON 数组、双重 JSON 编码字符串、老格式分号/管道分隔等多种 boardData 存储形式；2. 在回放按钮事件中改用 isReplayDataValid 判定，区分空数据 (`[]`) 与有效 JSON 数组，避免误报"暂无棋谱数据"；3. 在 Room.java 的 cacheGameRecordToRedis 中将 boardData 字符串解析为 org.json.JSONArray 再 put 进去，避免 org.json 在嵌套 put(String, String) 时对引号转义造成的双重 JSON 编码，从根上解决 Redis 缓存中棋谱数据被破坏的问题。 | 编译通过，预期修复 AI 对局棋谱回放 |
| 2026-07-08 12:51 | Contributor| chore: 全面排查并修复空指针风险（客户端部分） | ProfileController.java: msg.getData() 为 null 保护 + record.getResult() 空字符串 substring 越界修复 |
| 2026-07-08 14:18 | Contributor| chore: 移除心跳机制并清理文档（客户端部分） | 客户端 ServerConnector.java 不存在心跳定时器（已在前续版本移除）；Protocol.java 无 HEARTBEAT 常量；清理 MODULE_CLIENT.md 中的过时心跳描述 |
| 2026-07-08 17:36 | Contributor| feat: 实现心跳机制 + 对局断线重连 + 三次失败后判负（客户端部分） | 客户端 ServerConnector：添加定时心跳（30s/次）+ 响应超时检测（90s）+ 自动重连（5s间隔, 3次） |
| 2026-07-08 23:00 | Contributor| feat: 悔棋/求和按钮仅当前落子方可操作（客户端部分） | BoardController：新增 updateActionButtons() 仅在 myTurn && !gameOver && !spectatorMode && !replayMode 时启用 undoBtn 和 drawBtn，非己方回合时灰色禁用；在 setGameInfo / handleMouseClick / MOVE_SYNC / GAME_OVER / UNDO_ACCEPTED / TURN_TIMEOUT 等所有 myTurn/gameOver 变更点调用 |
| 2026-07-08 15:40 | Contributor| feat: 实现Token重连机制（客户端部分） | |
| 2026-07-07 16:20 | Contributor| feat(client): 添加棋谱回放与房间密码验证功能 | |
| 2026-07-07 01:06 | Contributor| fix(network): 修复更换IP后旧接收线程触发级联重连导致界面卡死 | |
| 2026-07-07 14:02 | Contributor| fix: 修复客户端断线后服务端连接数不释放 | |
| 2026-07-07 00:40 | Contributor| feat(client): 本地配置重命名为 config.json 并优化资源读取路径路由 | |
| 2026-07-08 12:51 | Contributor| chore: 全面排查并修复空指针风险（服务端部分） | ClientHandler.java: handleRegister/handleLogin/handleChangePassword/handleCreateRoom/handleCreateAiRoom 增加 String 入参 null 检查，handleReconnect token 日志行 NPE 保护；Room.java: getRankByUserId TOCTOU 重复调用风险修复 | 编译通过 |
| 2026-07-08 14:18 | Contributor| chore: 移除心跳机制并清理文档（服务端部分） | Protocol.java 无 HEARTBEAT 常量残留清理；SessionManager 中 updateHeartbeat 蜕化为会话查询时的活跃时间维护，保留；清理 MODULE_SERVER.md / MODULE_COMMON.md / MODULE_CLIENT.md / PRD.md 中的过时心跳描述 | 文档已同步 |
| 2026-07-08 17:36 | Contributor| feat: 实现心跳机制 + 对局断线重连 + 三次失败后判负（服务端部分） | 服务端 ClientHandler：处理 HEARTBEAT 命令 + 后台线程检测 90s 无消息则关闭连接；Room：对局中玩家断线启动 60s 重连计时窗口，最多 3 次重连机会，全部失败则判对方获胜（endGame reason=disconnect），重连成功后自动回放已有落子 | lint 零告警 |
| 2026-07-08 23:00 | Contributor| feat: 断线状态可视化 — 玩家卡片显示"断线中"角标 | BoardController：在玩家卡片右下角添加红色"断线中"角标（默认隐藏）；新增 setPlayerOnline(isBlack, online) 方法切换断线状态：断线时卡片透明度 0.55 + 名字变红 + 显示角标；监听 PLAYER_DISCONNECTED/PLAYER_RECONNECTED/RECONNECT_ATTEMPT/RECONNECT_FAILED 消息并在聊天区打印系统提示；新增 blackId/whiteId 字段用于按 userId 匹配玩家卡片 | lint 零告警 |
| 2026-07-08 23:00 | Contributor| feat: 悔棋/求和按钮仅当前落子方可操作 | BoardController：新增 updateActionButtons() 仅在 myTurn && !gameOver && !spectatorMode && !replayMode 时启用 undoBtn 和 drawBtn，非己方回合时灰色禁用；在 setGameInfo / handleMouseClick / MOVE_SYNC / GAME_OVER / UNDO_ACCEPTED / TURN_TIMEOUT 等所有 myTurn/gameOver 变更点调用；CSS：新增 .btn-secondary:disabled 样式（灰底 + 低透明 + 光标 default） | lint 零告警 |
| 2026-07-08 20:25 | Contributor| docs: 同步协作文档中的分支与 Redis 说明 | 1. 从 AGENTS.md 移除固定当前分支描述，避免多人协作时误导；2. 将项目说明补充 Redis；3. 更新 README，将 Redis 标记为必需组件，并改正启动脚本示例与目录说明，避免引用不存在的 run-client/run-server 脚本。 | 文档已同步 |
| 2026-07-08 20:24 | Contributor| fix: 全项目审查问题修复 | Maven 编译插件统一 3.13.0 + release 21 + fork；database 测试改为结构测试；心跳改为服务端返回 HEARTBEAT_OK 并刷新 Redis 会话活跃时间；Redis 改为服务端必需组件，匹配候选从 Redis 队列读取，锁失败不再兜底成功；会话 Redis 数据补充 role；新增管理中心页面 + 存档功能；协议常量集中化；清理 cp.txt 与 client-config.properties | 编译验证通过 |
| 2026-07-09 01:16 | Contributor| refactor(client-ui): 响应式布局与表单卡片体验整理 | 1. ScreenManager 移除整页 Scale 缩放，改由 FXML 布局约束自适应；2. 登录/注册页改用静态 PixelCard 外壳，列表页和个人中心释放固定宽度；3. 非 ADMIN 用户隐藏管理入口；4. BoardController 与 ShapeGridBackground 增加 0 尺寸和重复尺寸重绘保护；5. 同步更新 UI_GUIDELINE.md 与 CLIENT.md。 |
| 2026-07-09 11:52 | Contributor| feat(client-ui): 设置页音量滑杆改为弹性 JavaFX 控件 | 1. 新增 ElasticVolumeSlider 自定义控件，用 JavaFX 属性、鼠标事件和 Timeline 实现 React Bits ElasticSlider 风格的悬浮放大、轨道变粗、越界衰减与松手回弹；2. SettingsUI.fxml 将全局音量原生 Slider 替换为 0-100、步长 5 的弹性滑杆；3. style.css 补充中性板岩灰轨道、图标和数值提示样式；4. 新增 PRODUCT.md 记录产品设计上下文。 | 编译通过 |
| 2026-07-09 11:52 | Contributor| feat(client-ui): 扩展声音设置为多通道音量控制 | 1. 全局音乐新增独立开关与弹性音量条；2. 背景音乐、音效保留无文字开关并新增 ElasticVolumeSlider；3. 将"大厅音乐"改名为"大厅歌曲"，仅保留歌曲下拉选择（默认主题曲、清晨棋局、静夜对弈、赛场低语）；4. SettingsController 绑定开关与对应音量条的禁用和透明度状态。 | 编译通过 |
| 2026-07-09 13:58 | Contributor| feat(client): 添加客户端循环背景音乐 | 1. 新增由用户指定 D:\CloudMusic 原曲转码得到的 audio/lobby-theme.mp3；2. 新增 BackgroundMusicManager，通过 JavaFX MediaPlayer 循环播放并在退出时释放；3. ClientMain 启动时播放背景音乐，SettingsController 将全局音乐/背景音乐开关与音量条同步到实际播放状态；4. client/pom.xml 补充 javafx-media 依赖。 | 编译通过 |
| 2026-07-09 14:02 | Contributor| fix: 修复快速匹配 GAME_START 后双方看不到姓名和积分的 Bug | **原因**：CasualLobbyController.onMessage(GAME_START) 只调用了 3 参数版 setGameInfo(bId, wId, username)，丢弃了服务端传来的 blackUsername/blackScore/whiteUsername/whiteScore 全部数据；同时 JOIN_OK 在 GAME_START 后又被发送，导致已切入 BoardUI 的客户端又被切回 RoomWaitingUI。**修复**：1. GAME_START 处理改为解析全部 6 个字段（blackUsername/blackScore/whiteUsername/whiteScore），传递到 BoardController.setGameInfo() 的完整参数重载；2. 新增 joinedGame 标记位，在已进入 GAME_START 对局后忽略后续 JOIN_OK 消息，防止界面回切到等待室 | 编译通过 |
| 2026-07-10 12:01 | Contributor| fix: 修复头像显示三个一致性问题：排行榜透明、对战回放缺头像、头像预览颜色不一致 | **根因 1 - 排行榜透明**：RankingController 只对 rank 1/2/3 通过 setStyle 给金/银/铜纯色背景，rank 4+ 匿名用户无兜底背景色；修复：rank 4+ 按用户名 hash 给渐变底色。**根因 2 - 对战回放缺头像**：GameRecord 模型缺头像字段；修复：新增 player1Avatar/player2Avatar 字段。**根因 3 - 头像预览颜色不一致**：改用与 LobbyController 一致的 7 色渐变调色板。 | 编译通过 |
| 2026-07-09 14:53 | Contributor| fix(client): 背景音乐改用 MP3 资源播放 | 1. 定位大厅音乐未播放原因为 JavaFX Media 对 FLAC 运行时支持不稳定且原逻辑静默降级；2. 使用 ShareX ffmpeg 将原曲转为纯音频 lobby-theme.mp3；3. BackgroundMusicManager 改为加载 MP3，移除未使用 FLAC 资源；4. CLIENT.md 同步资源路径。 | 媒体探针 READY；编译通过 |
| 2026-07-09 15:17 | Contributor| fix(client): 降低背景音乐默认音量 | 将 BackgroundMusicManager 的默认全局音乐音量从 80 调整为 45、背景音乐音量从 70 调整为 35，初始实际播放音量由 56% 降至约 16%；SettingsUI.fxml 同步默认滑杆显示值。 | 编译通过 |
| 2026-07-09 19:02 | Contributor| fix(server): 修复快速匹配重复加入房间导致匹配失败 | RoomManager.quickMatch() 中 createRoom() 已将 player 加入房间，但后续又调用 room.join(player) 被重复检查拒绝；删除重复调用 | 编译通过 |
| 2026-07-09 19:02 | Contributor| fix(server): 修复排位匹配重复加入房间导致匹配失败 | RoomManager.rankedMatch() 中 createRankedRoom() 已将 player 加入房间，但后续又调用 room.join(player) 被重复检查拒绝；删除重复调用 | 编译通过 |
| 2026-07-09 19:02 | Contributor| fix(server): 修复AI模式下人类玩家获胜时winnerId错误 | Room.makeAiMove() 无论谁获胜都使用 whiteUserId(-1) 作为 winnerId；改为根据 winnerColor 正确判断获胜者 | 编译通过 |
| 2026-07-09 19:02 | Contributor| fix(server): 修复对战记录查询winner_id NULL值处理 | RecordDAO.getHistory() 中使用 rs.wasNull() 检查平局时的 NULL 值，确保正确处理 | 编译通过 |
| 2026-07-09 19:02 | Contributor| fix(server): 修复好友请求列表手动拼接JSON安全隐患 | ClientHandler.handleFriendRequests() 改用 JSONObject/JSONArray 构建安全的 JSON 响应，避免特殊字符导致解析错误 | 编译通过 |
| 2026-07-09 14:31 | Contributor| feat(client): 实现设置页面全部交互功能 | 1. 全屏模式：stage.setFullScreen() 即时生效；2. 分辨率切换：动态调整 stage 宽高并居中；3. 动态背景：ShapeGridBackground.setEnabled() 控制粒子网格显隐；4. 帧率限制：ShapeGridBackground 跳帧逻辑实现 60/120/无限制；5. 字体切换：修改 scene root 的 -fx-font-family 属性；6. 开机自启：VBS 脚本写入 Windows %APPDATA%\Startup；7. 记住密码：LoginController 读取 UserSettings 控制默认勾选；8. 后台静音：窗口失焦暂停音乐、获焦恢复；9. 音频控制：ElasticVolumeSlider 双向绑定 BackgroundMusicManager | 编译通过 |
| 2026-07-09 14:31 | Contributor| feat(client): 新增UserSettings用户偏好持久化 | 新增 UserSettings 类，将全屏/分辨率/帧率/音量/背景/字体/主题/语言/开机自启/记住密码等 15 项偏好保存到 ~/.gomatch/user-settings.json，启动时自动加载 | 编译通过 |
| 2026-07-09 16:09 | Contributor| feat(client): 实现国际化中英文切换 | 1. 新增 I18N 工具类，支持 ResourceBundle 动态切换；2. 新增 messages_zh.properties 和 messages_en.properties 资源文件（240+ key）；3. 所有 21 个 FXML 中文文本替换为 %key 占位符；4. ScreenManager 加载 FXML 时传入 ResourceBundle；5. ClientMain 启动时初始化语言；6. SettingsController 语言切换后重载场景 | 编译通过 |
| 2026-07-09 16:09 | Contributor| fix(client): 修复字体切换时背景变蓝 | root.setStyle() 会覆盖所有内联样式导致 -fx-background-color 丢失；改为 appendFontFamily() 追加/替换 -fx-font-family 属性，保留其他样式 | 编译通过 |
| 2026-07-09 16:35 | Contributor| fix(client): 修复BoardController多余花括号 | updateTurnLabel() 中 if-else 块后多了一个 }，导致 updatePlayerCardHighlight() 脱离方法体 | 编译通过 |
| 2026-07-09 19:02 | Contributor| fix(client): 修复AI对战结果不显示黑胜/白胜/平局 | 1. GameOverController.setResult() 支持中文描述（黑方胜、白方胜、平局、对手胜）；2. BoardController GAME_OVER 处理增加 AI 模式特殊判断（winnerId="-1"时判定为对手胜） | 编译通过 |
| 2026-07-09 19:02 | Contributor| fix(server): 修复删除好友后无法重新添加的问题 | 1. FriendDAO.sendFriendRequest() 发送新请求前先删除双方所有旧请求记录（PENDING/ACCEPTED/REJECTED）；2. FriendDAO.deleteFriend() 删除好友关系时同时清理双方的请求记录，解决唯一索引冲突 | 编译通过 |
| 2026-07-09 19:02 | Contributor| fix(server): 修复对局记录缺失boardData导致无法回放 | 1. RecordDAO.getHistory() 添加返回 board_data 字段；2. ClientHandler.handleQueryRecord() 在 RECORD_LIST 响应中添加 boardData，转义 `\|` 防止协议解析错误 | 编译通过 |
| 2026-07-09 19:23 | Contributor| fix(client): 修复对战记录回放功能 | 1. RecordController.onMessage() 更新解析步长从10改为11，添加 boardData 处理和解转义；2. BoardController.replayMoves() 新增 JSON 格式棋谱支持（检测 `[` 开头自动切换解析方式），兼容旧格式分号分隔 | 编译通过 |
| 2026-07-09 19:02 | Contributor| fix(client): 修复棋谱回放按钮文字显示为省略号 | BoardUI.fxml 将回放按钮容器从 HBox（水平排列）改为 VBox（垂直排列），按钮 maxWidth="Infinity" 充分利用侧边栏宽度 | 编译通过 |
| 2026-07-09 19:02 | Contributor| fix(client): 修复好友请求操作按钮文字显示为省略号 | FriendsUI.fxml 将操作列宽度从 100px 增加到 140px，确保"接受"和"拒绝"按钮完整显示 | 编译通过 |
| 2026-07-09 14:31 | Contributor| fix(client): 修复客户端配置文件服务器地址错误 | client-config.properties 中 server.host 从错误值改为 127.0.0.1 | 编译通过 |
| 2026-07-10 00:00 | Contributor| feat(client): 完善悔棋/求和请求等待机制 | 1. 发送悔棋/求和请求后显示等待弹窗（带加载动画），禁止落子和重复请求；2. 收到回复后关闭弹窗，恢复正常游戏；3. 游戏结束时自动关闭等待弹窗 |
| 2026-07-10 00:00 | Contributor| feat(client): 悔棋/求和时双方倒计时暂停与恢复 | 1. 发送请求时停止倒计时；2. 收到请求时停止倒计时；3. 收到回复后恢复倒计时；4. 悔棋同意时重置为60秒，拒绝时继续当前剩余时间；5. 求和拒绝时继续当前剩余时间 |
| 2026-07-10 00:00 | Contributor| feat(client): 观战模式游戏结束弹窗 | 观战者在对局结束时弹出游戏结束对话框，显示获胜者信息（黑方/白方 + 用户名）和"返回大厅"按钮 |
| 2026-07-09 23:44 | Contributor| feat(client-ui): 开发暗夜黑磨砂 UI 风格 | 1. style.css 追加暗夜磨砂主题覆盖层，将全局背景、卡片、输入框、按钮、表格、列表、弹窗、滚动条和音量滑杆统一为近黑玻璃与冷灰描边；2. 清理多处 FXML 浅色内联背景，避免覆盖全局主题；3. 设置页暗夜主题选项从"开发中"改为可用并持久化；4. 同步 UI_GUIDELINE.md 与 AGENTS.md 的 UI 约定。 | mvn -f client/pom.xml compile -e 通过 |
| 2026-07-09 23:44 | Contributor| feat(client-ui): 暗夜黑磨砂改为可切换双主题 | 1. ScreenManager 新增 theme-light/theme-dark 根类切换与 themeColor(light,dark) 动态色辅助；2. 设置页恢复"清透白磨砂 (经典)"与"暗夜黑磨砂"两个选项，切换后即时应用当前 Scene；3. 暗色 CSS 统一限定在 `.theme-dark`，动态列表/棋盘玩家卡/个人资料头部跟随主题；4. FXML 清理残留暗色内联样式并同步设计规范。 | 编译通过，FXML XML 解析通过 |
| 2026-07-09 23:44 | Contributor| fix(client-ui): 修复暗夜主题背景仍显示白色 | 根因是最外层 StackPane 背景链路在浅色/暗色间没有统一归口；改为由 ScreenManager.applyUserTheme() 按主题写入 wrapper 背景，浅色透明、暗色近黑渐变，并补齐暗色 editable ComboBox 的 text-field/arrow-button 子节点样式。 | 编译通过 |
| 2026-07-09 23:44 | Contributor| fix(client-ui): 移除基础 root 蓝白渐变 | 将 style.css 的 `.root` 背景从蓝白渐变改为中性雾白渐变，避免浅色或背景链路兜底时出现泛蓝底色。 | CSS 调整 |
| 2026-07-09 23:44 | Contributor| polish(client-ui): 加深暗夜主题并补齐漏白控件 | 1. 暗色根背景、顶栏、玻璃卡片、列表卡、PixelCard 统一压深到黑曜石色阶；2. 登录/注册页移除 `.glass-card` 的白色内联背景，交由主题 CSS 接管；3. 暗色头像/头像徽章补稳定圆角与透明子标签，避免白色方框露底。 | 编译通过，FXML XML 解析通过 |
| 2026-07-09 23:44 | Contributor| fix(client-ui): 修复登录/注册卡片双层边框不重合 | **真正根因**：PixelCard.layoutChildren() 把内部 cardBody 和 FXML 内层 glass-card 都放在同一卡片身体区域内，VBox 的 max 限制使其只按内容 preferred size 居中、不被拉满，导致内层比外层小、形成双线。**修复**：1. PixelCard.java layoutChildren 对 Region 先 setMaxWidth/Height(Double.POSITIVE_INFINITY) 再 layoutInArea；2. .glass-card 的 background-radius/border-radius 统一为 8、border-width 改 1.2；3. 移除 LoginUI.fxml、RegisterUI.fxml 内联 -fx-background-radius:16。 | mvn compile 通过 |
| 2026-07-10 01:16 | Contributor| docs(client-ui): 补充可复用前端组件模板 | 在 UI_GUIDELINE.md 新增按钮、卡片、布局、输入、表格列表、弹窗、暗色覆盖和禁止写法模板，提供可照抄的 FXML/CSS/Controller 片段，重点约束结构与样式而非另起颜色体系。 |
| 2026-07-10 11:19 | Contributor| docs: 整理文档中心与 common 协议说明 | 1. 重写 docs/README.md 为按场景阅读的文档中心，补充需求/架构/模块/UI/实训资料/启动联调/维护规则；2. 将 COMMON.md 从旧竖线协议说明更新为当前 Message JSON 协议模型；3. 同步根 README 的文档索引、common 文件结构和项目持续迭代说明。 | 文档整理完成 |
| 2026-07-10 11:19 | Contributor| docs: 去重整理 docs 文档职责边界 | 1. PRD.md 收敛为产品需求唯一权威文档；2. TSD.md 保留跨模块架构并引用协议/数据库专题文档；3. CLIENT.md、SERVER.md 启动段改为引用文档中心；4. DEV_GUIDELINE.md 的 UI 细节改为引用 UI_GUIDELINE.md。 | 文档结构检查通过 |
| 2026-07-10 11:19 | Contributor| docs: 归档学校原始资料 | 新建 docs/school_materials/，移动学校讲义、实操手册、动员会、项目详解和报告模板，并更新 docs/README.md 中的资料链接。 | 文档归档完成 |
| 2026-07-09 20:06 | Contributor| feat: 添加OSS存储、用户设置/音乐/审计日志数据库设计及文档更新 | 新增 t_user_settings、t_user_music、t_audit_log 表；更新 DB_DESIGN.md 文档 |
| 2026-07-10 21:35 | Contributor| docs: 全文档审查与更新 | 对照 init.sql/Protocol.java/DEV_LOG 全面更新：README 修死链补资料；PRD 补 FR-021~025 与 UI-016~019；TSD 架构图补 storage/OSS；COMMON 补 QUICK_READY/QUICK_MATCH_FOUND/RANKING 与重连字面量说明；SERVER 补命令分发表与 storage 包、修 Redis Key 矛盾与 DBUtil 密码；DB_DESIGN 加权威来源说明、修 t_room/t_chat_message/t_room_watcher 的 room_id 类型与密码；CLIENT 落子协议改 JSON；DEV_GUIDELINE 日志加作者列；TEST 修越权文案并补 S21~S26；AGENTS 补服务端命令与页面路由 | 文档更新完成 |

### 2026-07-11 — 标题栏窗口控制按钮右对齐

- 将主窗口业务布局固定为外层容器的全尺寸左上角子节点。
- 确保窗口缩放、最大化/还原和页面切换后，右上角控制按钮保持贴边对齐。

### 2026-07-11 — 重构区号不透明下拉框与登录 Tab 对称及脚本缓存清理

- 废弃 FXML 的 `countryCodeBox` (ComboBox)，改用 `btnCountryCode` (Button) 和 `inlineCodeMenu` (VBox) 内联 3D 悬浮菜单。
- 修复 FXML 里多类名空格连写错误，改在 `initialize()` 中动态为“账号登录” Tab 按钮绑定 `login-tab-btn-active` 样式，解决 Tab 切换按钮退化原生灰框的 Bug。
- 在切换登录方式（账号/手机号 Tab）的方法中增加 `hideInlineMenu()` 调用，彻底修复打开下拉框时切换模式导致的悬浮框残留 Bug。
- 统一登录 Tab 切换按钮宽度为 `120px` 且文字水平居中，使下划线和鼠标热区在视觉上绝对对称对齐。
- 在 `style.css` 结尾处提升类选择器特异性为 `.glass-card .inline-popup-menu`，强制实色（清透白/暗夜深色）不透明背景，彻底压制父卡片的透明度影响。
- 解决 visible/managed 为 false 时 JavaFX 首次渲染跳过 CSS apply 的怪癖 Bug，在 `toggleInlineMenu()` 展开时强制调用 `inlineCodeMenu.applyCss()` 重新刷入 CSS。
- 修改 `scripts/` 下全部 4 个成员的客户端启动脚本（`hln/jzx/ssc/zpw-client.ps1`），在运行命令中补充 `clean` 编译阶段，彻底根治 Maven 增量构建跳过 CSS 资源文件拷贝的旧缓存问题。

### 2026-07-11 — 规范 Git 提交与分支合并流程

- 修改 `docs/DEV_GUIDELINE.md` 中的分支与提交规范，明确所有日常开发工作必须在个人功能/修复分支进行，开发完毕并测试通过后，通过在远程仓库提交 Pull Request (PR) 的形式进行合并，严禁开发人员在本地将个人分支直接合并或直接推送至开发主分支（如 `dev`）。
- 同步修改根目录 `AGENTS.md` 中的 Git 提交规范，在其中增加 **提交与合并要求** 章节，明确“禁止直接推送”原则。

### 2026-07-12 — 修复好友页暗色主题可读性

- 好友页动态节点已从 Java 内联主题颜色迁移到 CSS 语义类，修复 PR #8 遗留暗色可读性回退。

### 2026-07-12 — 悔棋/求和时间同步与服务端统一管控 (PR #65)

- 新增 `TIMER_PAUSE` 协议常量，服务端在 `requestUndo()`/`requestDraw()` 中广播给所有客户端（含观战者），统一暂停倒计时并显示"等待回复..."状态。
- 移除 `cancelTurnTimer()` 中原有的 `TIME_SYNC` 广播（该广播导致客户端重启倒计时而非暂停），改由请求方法显式发送暂停指令。
- 客户端新增 `TIMER_PAUSE` 消息处理器，同步停止倒计时并更新标签文字；发起方本地 `handleUndo()`/`handleDraw()` 也补全等待状态显示。

### 2026-07-12 — 注册页面手机号从选填改为必填 (PR #71)

- `RegisterController`: 手机号为空时提示必填，格式校验始终执行，`REGISTER` 消息始终携带 `phone` 字段。
- i18n: `register.phone` 去掉"（选填）"标注，`register.allRequired` 增加手机号提示。
- 与服务端 `ProtocolRequestGuard` 的 phone length 校验对齐。

### 2026-07-12 — 好友申请页面补齐请求方头像 (PR #72)

- 补齐 FriendDAO → ClientHandler → FriendsController 4 层头像数据链路。
- `FriendDAO.getPendingRequests()`: SQL 增加 `u.avatar` 列，`FriendRequestInfo` 新增 `avatar` 字段并生成 OSS 预签名 URL。
- `FriendsController`: `FriendRequestModel` 加 `avatar` 字段；`renderRequestCards` 头像从纯文字改为异步图片加载 + 文字兜底。

### 2026-07-12 — 悔棋/求和次数上限弹窗透明 (PR #73)

- `showLimitAlert` 添加 `glass-card` CSS 类为 `TRANSPARENT` 舞台提供磨砂背景，背景不透明度从 0.96 提升至 0.98。

### 2026-07-12 — D3D 管线 NPE 规避 (PR #74)

- `jpackage` MSI 打包命令添加 `--java-options "-Dprism.dirtyopts=false"`，禁用 D3D 脏区域优化，规避 GPU 资源回收后 `D3DSwapChain.createGraphics()` 空指针崩溃。

### 2026-07-12 — CI deploy-server 修复 (PR #75)

- `deploy-server` job 添加 `environment: 1` 以读取环境级 SSH secrets。
- 新增 SSH 凭据预检步骤，缺少 secrets 时跳过部署步骤并输出 warning 而非硬失败。
- 后续下载/上传/重启步骤均添加 `if: steps.deploy-ready.outputs.enabled == 'true'` 门控。

### 2026-07-12 — 版本发布与文档 (v2.0.2)

- 新增 `docs/release/RELEASE_GUIDE.md` 发布流程文档。
- 新增 `docs/releases/v2.0.2.md` 版本文档。
- pom.xml 版本号同步至 2.0.2。
