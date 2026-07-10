# 弹窗主题与透明窗口修复设计

## 目标

让所有 Gomatch 客户端弹窗在浅色和暗色主题下均符合 UI 规范，并消除无边框窗口圆角外露出的不透明矩形底色。

## 范围

- FXML 弹窗：通过 `ScreenManager.showDialog` 展示的所有页面。
- 原生 JavaFX `Dialog`：目前的 `TextInputDialog` 与 `Alert`，以及后续通过 `ScreenManager.showStyledDialog` 展示的 Dialog。
- 对局页直接创建的结算和等待请求弹窗。
- 弹窗内容中硬编码为仅深色的背景或文字颜色。

不改变弹窗业务流程、尺寸、模态关系或按钮行为。

## 根因

1. 无边框弹窗使用 `StageStyle.UNDECORATED`，但其 `Scene` 没有使用透明填充；当根节点的圆角内容未填满窗口时，JavaFX 默认的不透明 Scene 背景会显示为矩形。
2. 自定义 FXML 弹窗会挂载主题类，但原生 `DialogPane` 未统一挂载主题和字体；此外少数 FXML 使用固定深色内联颜色，无法在浅色主题下正确呈现。
3. 对局页存在绕过通用弹窗入口创建 Stage 的路径，容易漏掉透明、主题和样式注入。

## 方案

以 `ScreenManager` 作为唯一的弹窗视觉基础设施。

### 自定义 FXML 弹窗

- 保持无系统标题栏和现有自定义标题栏、拖拽行为。
- 在创建 Scene 后将填充设为 `Color.TRANSPARENT`，并使用透明 Stage 样式，使卡片圆角外部真正透明。
- 继续注入全局 CSS、用户主题和用户字体；动态背景与内容卡片保持现有层级。

### 原生 Dialog

- `showStyledDialog` 在显示前为 `DialogPane` 的 Scene 设置透明填充。
- 将当前主题类和字体应用到 `DialogPane` 的 Scene 根节点，使其与 FXML 弹窗使用同一套 `.theme-dark` 覆盖样式。
- 保留既有的 owner、拖拽和圆角样式逻辑。

### 对局页动态弹窗

- 将 `BoardController` 手动创建的弹窗对齐到上述透明 Scene、样式表、主题和字体约定。
- 不改变其非阻塞 `show()` 语义，确保再来一局、悔棋与求和消息处理不受影响。

### FXML 与 CSS 审查

- 审查所有弹窗 FXML 中按钮、标签与容器的 `styleClass` 和内联颜色。
- 将仅服务于暗色的固定背景/文字色替换为现有样式类或在 CSS 中补充 `.theme-dark` 覆盖，保证浅色主题同样可读。
- 不新增未以 `.theme-dark` 限定的全局暗色覆盖；按钮继续使用现有 8px 圆角、下划线边框与 hover/pressed/focused 反馈。

## 验证

1. 编译 client 模块，确认 FXML、控制器和 JavaFX API 调用有效。
2. 静态检查所有 Stage/Dialog 创建路径：透明 Scene、全局样式、用户主题和字体均已覆盖。
3. 审查全部弹窗 FXML，确认按钮使用标准样式类，未保留仅深色主题的固定色彩。
4. 在可运行的 JavaFX 环境中手动切换浅色/暗色主题，打开每类弹窗，确认圆角外部透明且文字、按钮可读。

## 风险与回退

透明 Scene 依赖平台窗口合成；如特定平台出现阴影或点击区域异常，回退只需恢复对应弹窗 Scene 的默认填充，不影响业务逻辑。所有改动集中在视觉基础设施和样式，便于按文件回退。
