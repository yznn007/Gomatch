# Gomatch 版本更新

## v2.0.2 (2026-07-12)

### Bug 修复

- **注册页面手机号从选填改为必填** (#71)
  - 客户端校验与服务端 `ProtocolRequestGuard` 对齐，手机号为空时提示必填

- **好友申请页面补齐请求方头像显示** (#72)
  - 补齐 FriendDAO → ClientHandler → FriendsController 4 层头像数据链路
  - `renderRequestCards` 头像从纯文字改为异步图片加载+文字兜底

- **悔棋/求和次数上限弹窗透明** (#73)
  - `showLimitAlert` 添加 `glass-card` CSS 类为 TRANSPARENT 舞台提供磨砂背景
  - 背景不透明度从 0.96 提升至 0.98

- **JavaFX D3D 管线 NPE 崩溃** (#74)
  - `jpackage` MSI 打包添加 `--java-options "-Dprism.dirtyopts=false"`
  - 禁用 D3D 脏区域优化，规避 GPU 资源回收后空指针

---

## v2.0.1 (2026-07-12)

- 集成 ProGuard 混淆，修复 MSI 脚本重复构建
- 修复好友离线后仍显示在线
- 重构头像裁剪界面，修复拖拽/缩放/溢出/样式
- 悔棋/求和时服务端广播 TIMER_PAUSE 同步暂停所有客户端倒计时 (#65)
- 恢复对话框关闭按钮
