# 好友页 CSS 主题迁移设计

## 目标

修复 PR #8 冲突解决将好友页动态卡片文字色回退为固定浅色值的问题。好友列表和好友申请列表的动态节点不再通过 Java 内联颜色参与主题切换，统一由 `style.css` 的基础规则和 `.theme-dark` 覆盖规则控制。

## 范围

- 仅修改 `FriendsController` 动态创建的好友卡、好友申请卡和状态提示的样式挂接方式。
- 在 `style.css` 增加好友页语义样式类及对应暗色主题规则。
- 保持现有国际化键、好友协议、页面布局、卡片交互和按钮行为不变。

## 设计

`FriendsController` 为动态节点设置语义类：

- `friend-card-name`：姓名文字。
- `friend-card-meta`：用户 ID、积分等辅助文字。
- `friend-card-message`：好友申请留言。
- `friend-card-status-online` 与 `friend-card-status-offline`：在线状态文字。
- `friend-card-action`：动态按钮的通用尺寸排版。
- `friend-request-avatar`：好友申请的橙色头像底色。
- `friend-status-success` 与 `friend-status-error`：提交好友请求的反馈文字。

浅色主题在基础选择器中定义；暗色主题只通过 `.theme-dark` 覆盖颜色。删除和拒绝按钮继续复用既有 `btn-danger`，不再以 Java 内联样式固定文字色。

## 验收与验证

- `FriendsController` 不再调用 `ScreenManager.themeColor(...)`，也不再为好友卡文字、状态文字或按钮设置内联文字色。
- `style.css` 为每个新增语义类提供浅色规则和 `.theme-dark` 覆盖规则。
- `git diff --check` 无空白错误。
- 运行客户端模块编译；若基线阻塞，明确记录阻塞输出并补充静态检查证据。
