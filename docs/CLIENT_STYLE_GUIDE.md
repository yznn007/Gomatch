# Gomatch 客户端前端设计规范 (Client Style Guide)

为了保证 Gomatch 联机五子棋对战平台客户端界面的美观性、一致性与高端质感，特制定本前端设计规范。所有参与 JavaFX UI、FXML 布局以及 CSS 样式调整的开发人员（包括 AI 助手）必须严格遵守本规范，禁止使用偏离本设计语言的 ad-hoc 样式。

---

## 1. 核心设计理念：双主题磨砂 (Dual Frosted Minimalist)

Gomatch 客户端保留两套可切换视觉基调：**清透白磨砂**作为经典原色，**暗夜黑磨砂**作为沉浸模式。浅色规则作为基础 CSS，暗色规则必须统一收敛在 `.theme-dark` 覆盖层下，由 `ScreenManager.applyUserTheme()` 在 Scene 根节点切换。

### 配色原则 (Neutral Palette)
*   **杜绝鲜活彩色**：界面中一律**杜绝使用鲜活天蓝色、亮绿色**等饱和度过高的颜色，防止视觉廉价感。
*   **采用中性色系**：颜色一律归一化为**石墨黑 (Graphite Black)**、**板岩灰 (Slate Gray)**、**冷白色 (Cool White)** 以及半透明叠加色。
*   **状态色规范**：
    *   常规状态：冷白色（主文本）、板岩灰（分割线/边框/提示）。
    *   辅助状态：透明板岩灰 cover 覆盖层（悬浮）。
    *   警示/删除状态：柔和红色 (e.g. `#dc2626` / `rgba(220, 38, 38, 0.2)` 等红色系下划线描边)。

---

## 2. 视觉层级与玻璃磨砂卡片 (Glassmorphism Hierarchy)

界面元素不使用硬性的实色底色，而是采用透明磨砂质感卡片建立清晰的视觉纵深。浅色主题使用乳白半透明卡片，暗色主题使用近黑/石墨半透明卡片。登录、注册、个人信息等重点表单可以使用 `PixelCard` 作为外层容器，内部再放置 `.glass-card` 内容层。

```
┌────────────────────────────────────────────────────────┐
│  背景层：ShapeGridBackground 点阵粒子粒子背景（动态微交互）  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  中景层：PixelCard / .card / .glass-card 卡片层    │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │  前景层：极简底划线输入框 / 8px 圆角按钮     │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### 磨砂卡片 CSS 规范
*   **卡片底色**：浅色主题使用乳白半透明；暗色主题只在 `.theme-dark` 中使用近黑/石墨磨砂半透（如 `rgba(15, 23, 42, 0.66)` 或 `rgba(10, 15, 28, 0.72)`）。
*   **卡片阴影**：常规表单卡片保留低透明暗影与细冷灰边框；交互式入口卡片悬浮时只允许轻微亮边或低饱和状态色，拒绝粗糙重阴影和霓虹光效。
*   **PixelCard 用法**：登录、注册等表单页使用 `variant="blue"`、`showParticles="false"` 的静态 `PixelCard` 包裹内容，避免粒子动画干扰输入；大厅和 AI 对战入口可保留粒子、悬浮和吊绳动效。
*   **分割线设计**：分割不同区域时，必须采用 `Separator` 配合轻量分割线样式 `.section-divider`（如底色 `rgba(0, 0, 0, 0.1)` 的极细虚化线），禁用加粗或实色粗条。

---

## 3. 控件边框与底划线规范 (Bottom-Line Controls)

为了维持极简高级感，界面中所有文本输入框与选项控件统一采用**极简底划线描边**，去除传统的四面封闭外边框。

*   **去除外边框**：所有 `TextField`、`PasswordField`、`ComboBox`、`Button` 必须移除左、上、右三面的边框描边，禁用默认的四面焦点框。
*   **下划线参数**：仅留**底部 1.5px 极简下划线描边**。
    *   *常规状态*：`transparent transparent rgba(203, 213, 225, 0.24) transparent`。
    *   *聚焦状态 (Focused)*：`transparent transparent #e2e8f0 transparent`。
*   **尺寸对齐**：文本框、下拉选择框与按钮控件的高度必须保持高度统一（如默认 `36px` 或 `40px` 档位），避免高矮参差不齐。

---

## 4. 按钮圆角与交互反馈 (Button Feedbacks)

按钮不仅是静态的文字，其交互反馈必须让界面“活”起来。

### 圆角与形状
*   **统一 8px 圆角**：所有按钮元素（包含主次按钮、控制按钮、二级页面功能入口）的背景与描边圆角均统一使用 **8px 圆角结构**。
*   **禁用切角形状**：放弃早期的 SVG 路径切角形状（`-fx-shape`），改用更稳重的圆角矩形，以与玻璃大卡片圆角风格完美融合。

### 三态伪类反馈
所有交互按钮必须在 CSS 中显式且精细地定义以下三种状态的背景反馈：

1.  **Hover 状态（鼠标悬浮）**：
    *   背景切换为冷灰 Cover 遮罩色（推荐 `rgba(71, 85, 105, 0.72)`），文字保持冷白。
2.  **Pressed 状态（点击按下）**：
    *   背景切换为低透明冷白高光（推荐 `rgba(226, 232, 240, 0.18)`），提升触感物理反馈。
3.  **Focused 状态（获取焦点）**：
    *   底部下划线高亮显示，或者周围显示淡淡的发光过渡圈，杜绝系统默认的粗糙蓝色虚线框。

---

## 5. 自适应布局与棋盘自收缩机制 (Adaptive Layouts)

客户端窗口支持自由拉伸、最大化以及分屏贴边。业务界面不再依赖 `ScreenManager` 对整页做 `Scale` 变换，而是由 FXML 自身的 `GridPane`、`HBox`、`VBox`、`Region`、`hgrow/vgrow`、`min/maxWidth` 等约束完成响应式排版，避免整页缩放导致文字模糊、点击区域偏移或宽高被锁死。

### 页面响应式规则
*   **禁止无必要固定宽度**：列表页、个人中心、好友页、排行榜页等内容面板不要同时设置 `prefWidth` 与 `maxWidth` 锁死横向空间；仅输入框、按钮、棋盘等固定格式控件保留合理 `maxWidth`。
*   **登录/注册布局**：登录页采用左右两列 `GridPane`，品牌区与表单卡片按比例分配；注册页居中放置静态 `PixelCard`，表单控件通过 `maxWidth` 控制可读宽度。
*   **最小窗口基准**：主窗口当前以 `1280 x 720` 作为默认与最小尺寸基准，页面应在该尺寸下完整可用，并在最大化时自然扩展。

### 棋盘侧边栏拉伸数学限制 (Sidebar Clamping)
局内对战棋盘是正方形，其最大宽度受限于棋盘容器的物理高度。
*   **物理拉手**：侧边栏左边缘集成隐式 `dragHandle`（`6px` 宽）。
*   **宽度约束范围**：拖动范围被锁定在 `150px ~ 400px` 之间。
*   **棋盘正方形保护**：侧栏最大拉伸宽度不能超过 `当前窗口总宽 - 30px (按钮与拉手) - 棋盘容器高度`。这能保证左侧棋盘拥有至少 1 倍的正方形物理边长，永远不会被挤压变形。
*   **比例自适应**：引入比例因子 `sidebarRatio`。窗口整体缩放时，侧栏基于比例自动等比缩放并应用边界限制。

### Canvas 防死锁尺寸机制 (Canvas Resizing)
*   **尺寸解耦**：自定义棋盘 `Canvas` 类必须重写 `isResizable()` 返回 `true`，且将 `minWidth`、`minHeight` 等重写返回小值（如 `10`）。
*   **自适应重绘**：避免 Canvas 将父层容器尺寸撑死导致无法缩小的 JavaFX 缺陷。容器尺寸改变时，监听器重算 `cellSize`，清空旧画布 (`gc.clearRect`) 并平滑重绘。
*   **重复重绘保护**：棋盘 Canvas 与动态背景 Canvas 在宽高变化小于 1px 时跳过 `setWidth/setHeight` 与重绘，防止 JavaFX 布局监听器形成重复触发布局抖动。

### 聊天与文本换行
*   **聊天记录区** (`chatArea`) 必须配置 `wrapText="true"`。
*   文字超长时在单元格内自动分行，**全界面聊天展示区域杜绝出现横向滚动条**。

---

## 6. 弹窗与对话框规范 (Immersive Dialogs)

客户端采用无边框沉浸式窗口，原生操作系统的 `Alert` 弹窗（Windows 默认装饰框与灰白按钮）会极大地破坏视觉一致性。

*   **禁用原生 Alert**：禁止使用 JDK 原生 `new Alert(...)` 展示提示信息。
*   **使用沉浸式模态弹窗**：一律使用 `ScreenManager.showDialog("/fxml/MessageDialogUI.fxml")` 或自定义对话框 FXML。
*   **弹窗外壳特性**：通过 `ScreenManager` 弹出的模态窗自动采用 `StageStyle.UNDECORATED`（无系统窗口栏），背景集成点阵网格粒子特效，并包含项目定制的极简关闭/关闭标题栏，整体风格完美统一。

---

## 7. 可复用前端组件模板 (Reusable UI Templates)

本章提供给 FXML 与 Controller 直接复用的组件样式模板。模板强调**结构、间距、圆角、边框、状态反馈、布局约束**，颜色只沿用 `style.css` 已有双主题规则；新增暗色样式时必须写在 `.theme-dark` 作用域内，避免破坏主题切换。

### 7.1 使用原则

*   **优先使用样式类**：FXML 中使用 `styleClass="btn-primary"`、`styleClass="card-padded"` 等已有类；Controller 动态创建节点时通过 `getStyleClass().add(...)` 添加类。
*   **减少内联样式**：内联样式只允许写尺寸、间距、对齐等一次性布局参数；颜色、边框、背景、阴影、hover/pressed/focused 状态必须沉淀到 CSS 类。
*   **动态文字色跟随主题**：Controller 中确实需要设置文字颜色时，必须使用 `ScreenManager.themeColor(light, dark)`。
*   **组件 class 命名**：页面通用组件使用语义名，如 `.list-item-card`、`.form-section`；页面私有组件使用页面前缀，如 `.profile-header-card`、`.board-sidebar`。

### 7.2 按钮模板

#### 主操作按钮

适用于登录、保存、开始匹配、确认等页面主动作。默认使用已有 `.btn-primary`。

```xml
<Button text="确认" styleClass="btn-primary" prefWidth="180"/>
```

```css
.btn-primary {
    -fx-background-radius: 8;
    -fx-border-radius: 8;
    -fx-border-width: 0 0 1.5 0;
    -fx-padding: 9 24;
    -fx-font-weight: bold;
    -fx-cursor: hand;
    -fx-effect: none;
}
.btn-primary:hover,
.btn-primary:armed,
.btn-primary:focused {
    -fx-border-width: 0 0 1.5 0;
}
```

#### 次操作按钮

适用于返回、取消、刷新、查看详情等动作。默认使用已有 `.btn-secondary`。

```xml
<Button text="返回" styleClass="btn-secondary" prefWidth="140"/>
```

```css
.btn-secondary {
    -fx-background-radius: 8;
    -fx-border-radius: 8;
    -fx-border-width: 0 0 1.5 0;
    -fx-padding: 9 24;
    -fx-font-weight: bold;
    -fx-cursor: hand;
    -fx-effect: none;
}
.btn-secondary:disabled {
    -fx-cursor: default;
    -fx-opacity: 0.55;
}
```

#### 危险操作按钮

适用于退出、删除、投降、取消匹配等不可逆或负向动作。危险按钮允许使用柔和红色状态，但形状仍保持 8px 圆角与底划线语言。

```xml
<Button text="退出" styleClass="btn-danger" prefWidth="140"/>
```

```css
.btn-danger {
    -fx-background-radius: 8;
    -fx-border-radius: 8;
    -fx-border-width: 0 0 1.5 0;
    -fx-padding: 9 24;
    -fx-font-weight: bold;
    -fx-cursor: hand;
}
.btn-danger:hover,
.btn-danger:armed {
    -fx-border-width: 0 0 1.5 0;
}
```

#### 顶部栏与导航按钮

适用于大厅顶部、底部导航、设置页侧栏菜单。按钮应轻量、透明、无投影。

```xml
<Button text="个人中心" styleClass="top-btn"/>
<Button text="设置" styleClass="nav-btn"/>
```

```css
.top-btn,
.nav-btn,
.sidebar-menu-btn {
    -fx-background-color: transparent;
    -fx-background-radius: 8;
    -fx-border-width: 0;
    -fx-padding: 8 14;
    -fx-cursor: hand;
    -fx-effect: none;
}
.top-btn:hover,
.nav-btn:hover,
.sidebar-menu-btn:hover,
.nav-btn-active,
.sidebar-menu-btn.active {
    -fx-background-radius: 8;
}
.nav-btn-active,
.sidebar-menu-btn.active {
    -fx-font-weight: bold;
}
```

#### 列表内小按钮

适用于房间卡片的“加入”、好友请求的“接受/拒绝”、记录行的“回放”。它继承主/次按钮，但尺寸更紧凑。

```java
Button actionBtn = new Button("加入");
actionBtn.getStyleClass().addAll("btn-primary", "btn-compact");
```

```css
.btn-compact {
    -fx-padding: 4 14;
    -fx-font-size: 12px;
    -fx-min-height: 28;
    -fx-pref-height: 28;
    -fx-background-radius: 8;
    -fx-border-radius: 8;
}
```

### 7.3 卡片模板

#### 普通内容卡片

适用于页面主体区域、个人信息区、设置分组。优先使用 `.card-padded`。

```xml
<VBox styleClass="card-padded" spacing="16" maxWidth="Infinity">
    <Label text="标题" styleClass="section-label"/>
</VBox>
```

```css
.card-padded {
    -fx-background-radius: 16;
    -fx-border-radius: 16;
    -fx-border-width: 1.2;
    -fx-padding: 28 32;
}
```

#### 表单玻璃卡片

适用于登录、注册、服务器配置等聚焦输入页面。外层可用 `PixelCard`，内层使用 `.glass-card`。

```xml
<VBox styleClass="glass-card" spacing="18" maxWidth="Infinity" maxHeight="Infinity">
    <Label text="登录" styleClass="title"/>
    <VBox styleClass="input-field-group">
        <TextField promptText="账号" styleClass="glass-input"/>
    </VBox>
    <Button text="登录" styleClass="btn-primary" maxWidth="Infinity"/>
</VBox>
```

```css
.glass-card {
    -fx-background-radius: 8;
    -fx-border-radius: 8;
    -fx-border-width: 1.2;
    -fx-padding: 40;
    -fx-effect: none;
}
.glass-card .input-field-group {
    -fx-background-radius: 8;
    -fx-border-radius: 8;
    -fx-border-width: 0 0 1.5 0;
    -fx-padding: 6 14;
    -fx-spacing: 2;
}
.glass-card .input-field-group:hover,
.glass-card .input-field-group:focused-within {
    -fx-border-width: 0 0 1.5 0;
}
.glass-card .glass-input {
    -fx-background-color: transparent;
    -fx-border-width: 0;
    -fx-padding: 4 0;
}
```

#### 入口功能卡片

适用于大厅模式入口、AI/匹配/排位入口。结构必须可点击，文字区垂直排列，卡片本身承担 hover/pressed。

```xml
<VBox styleClass="feature-card" spacing="6" prefWidth="240">
    <Label text="休闲对战" styleClass="feature-card-title"/>
    <Label text="创建房间或快速加入" styleClass="feature-card-desc" wrapText="true"/>
</VBox>
```

```css
.feature-card {
    -fx-background-radius: 14;
    -fx-border-radius: 14;
    -fx-border-width: 1;
    -fx-padding: 16 18;
    -fx-spacing: 6;
    -fx-cursor: hand;
}
.feature-card:hover,
.feature-card:pressed {
    -fx-background-radius: 14;
}
.feature-card-title {
    -fx-font-size: 18px;
    -fx-font-weight: bold;
}
.feature-card-desc {
    -fx-font-size: 13px;
}
```

#### 列表项卡片

适用于房间列表、观战列表、好友列表、排行榜行。建议 Controller 动态创建时统一用 `HBox` 根节点。

```java
HBox card = new HBox(14);
card.getStyleClass().add("list-item-card");
card.setAlignment(Pos.CENTER_LEFT);
HBox.setHgrow(card, Priority.ALWAYS);

VBox infoBox = new VBox(4);
HBox.setHgrow(infoBox, Priority.ALWAYS);

Button actionBtn = new Button("加入");
actionBtn.getStyleClass().addAll("btn-primary", "btn-compact");
card.getChildren().addAll(avatar, infoBox, actionBtn);
```

```css
.list-item-card {
    -fx-background-radius: 8;
    -fx-border-radius: 8;
    -fx-border-width: 0 0 1.5 0;
    -fx-padding: 12 14;
    -fx-spacing: 14;
    -fx-alignment: center-left;
    -fx-cursor: hand;
}
.list-item-card:hover,
.list-item-card-highlight {
    -fx-background-radius: 8;
    -fx-border-radius: 8;
}
.list-item-card-highlight {
    -fx-border-width: 0 0 1.5 0;
}
```

#### 玩家信息卡片

适用于棋盘侧栏、对局状态、回合提示。外层只负责占位，内层负责视觉。

```xml
<VBox styleClass="player-info-card">
    <HBox styleClass="player-info-card-inner" spacing="10" alignment="CENTER_LEFT">
        <StackPane styleClass="avatar-badge"/>
        <VBox spacing="2" HBox.hgrow="ALWAYS"/>
    </HBox>
</VBox>
```

```css
.player-info-card {
    -fx-padding: 0;
    -fx-min-height: 0;
}
.player-info-card-inner {
    -fx-background-radius: 8;
    -fx-border-radius: 8;
    -fx-border-width: 1;
    -fx-padding: 8 12;
    -fx-alignment: center-left;
}
.player-info-card-active {
    -fx-border-width: 1;
}
```

### 7.4 布局模板

#### 页面根布局

所有业务页根节点使用透明背景，由 `ScreenManager` 负责最外层主题背景和动态背景。

```xml
<BorderPane styleClass="page-root" xmlns="http://javafx.com/javafx"
            xmlns:fx="http://javafx.com/fxml">
</BorderPane>
```

```css
.page-root {
    -fx-background-color: transparent;
    -fx-padding: 10 30 30 30;
}
```

#### 顶部栏

顶部栏只承担导航与信息密度，不做厚重卡片。

```xml
<HBox styleClass="top-bar" alignment="CENTER_LEFT" spacing="12">
    <Label text="Gomatch" styleClass="section-label"/>
    <Region HBox.hgrow="ALWAYS"/>
    <Button text="设置" styleClass="top-btn"/>
</HBox>
```

```css
.top-bar {
    -fx-background-color: transparent;
    -fx-border-width: 0;
    -fx-effect: none;
    -fx-padding: 0 0 12 0;
}
```

#### 双栏页面

适用于登录页、设置页、列表+详情页。使用 `GridPane` 或 `HBox`，通过 `hgrow/vgrow` 分配空间，不用整页缩放。

```xml
<GridPane styleClass="page-root" hgap="24" vgap="0">
    <columnConstraints>
        <ColumnConstraints percentWidth="42"/>
        <ColumnConstraints percentWidth="58"/>
    </columnConstraints>
    <VBox GridPane.columnIndex="0" styleClass="brand-panel"/>
    <VBox GridPane.columnIndex="1" styleClass="glass-card" maxWidth="520"/>
</GridPane>
```

```css
.layout-two-column {
    -fx-background-color: transparent;
    -fx-padding: 10 30 30 30;
    -fx-hgap: 24;
}
.layout-sidebar {
    -fx-background-color: transparent;
    -fx-spacing: 8;
    -fx-padding: 0 18 0 0;
}
.layout-content {
    -fx-background-color: transparent;
    -fx-spacing: 16;
}
```

#### 列表页工具栏

适用于房间大厅、好友、记录、排行榜等页面。

```xml
<VBox styleClass="card-padded" spacing="14">
    <HBox styleClass="list-toolbar" alignment="CENTER_LEFT" spacing="10">
        <Label text="房间列表" styleClass="section-label"/>
        <Region HBox.hgrow="ALWAYS"/>
        <Button text="刷新" styleClass="btn-secondary"/>
        <Button text="创建" styleClass="btn-primary"/>
    </HBox>
    <VBox fx:id="roomsContainer" styleClass="list-stack" spacing="10"/>
</VBox>
```

```css
.list-toolbar {
    -fx-background-color: transparent;
    -fx-padding: 0 0 8 0;
    -fx-spacing: 10;
    -fx-alignment: center-left;
}
.list-stack {
    -fx-background-color: transparent;
    -fx-spacing: 10;
}
```

### 7.5 输入与表单模板

#### 普通输入框

```xml
<TextField promptText="请输入昵称" styleClass="field" prefWidth="240"/>
<PasswordField promptText="请输入密码" styleClass="field" prefWidth="240"/>
```

```css
.field {
    -fx-background-radius: 8;
    -fx-border-radius: 8;
    -fx-border-width: 0 0 1.5 0;
    -fx-padding: 8 14;
    -fx-font-size: 14px;
}
.field:hover,
.field:focused {
    -fx-border-width: 0 0 1.5 0;
}
```

#### 表单行

```xml
<GridPane styleClass="form-grid" hgap="24" vgap="16">
    <Label text="服务器" styleClass="muted" GridPane.rowIndex="0" GridPane.columnIndex="0"/>
    <TextField styleClass="field" GridPane.rowIndex="0" GridPane.columnIndex="1"/>
</GridPane>
```

```css
.form-grid {
    -fx-background-color: transparent;
    -fx-padding: 10 0;
    -fx-hgap: 24;
    -fx-vgap: 16;
}
.form-section {
    -fx-background-color: transparent;
    -fx-spacing: 16;
    -fx-alignment: top-center;
}
.form-actions {
    -fx-background-color: transparent;
    -fx-spacing: 12;
    -fx-alignment: center-right;
    -fx-padding: 10 0 0 0;
}
```

### 7.6 表格、列表与滚动容器模板

#### TableView 容器

```xml
<VBox styleClass="table-card" VBox.vgrow="ALWAYS">
    <TableView fx:id="recordTable" VBox.vgrow="ALWAYS"/>
</VBox>
```

```css
.table-card {
    -fx-background-radius: 14;
    -fx-border-radius: 14;
    -fx-border-width: 1;
    -fx-padding: 0;
}
.table-view {
    -fx-background-color: transparent;
    -fx-background-radius: 14;
    -fx-border-width: 0;
    -fx-padding: 0;
}
.table-view .table-row-cell {
    -fx-background-radius: 8;
    -fx-border-width: 0;
    -fx-padding: 4 8;
}
.table-view .table-cell {
    -fx-border-width: 0;
    -fx-padding: 6 12;
    -fx-alignment: center;
}
```

#### ListView 容器

```xml
<VBox styleClass="list-card" VBox.vgrow="ALWAYS">
    <ListView fx:id="messageList" VBox.vgrow="ALWAYS"/>
</VBox>
```

```css
.list-card {
    -fx-background-radius: 12;
    -fx-border-radius: 12;
    -fx-border-width: 1;
    -fx-padding: 0;
}
.list-view {
    -fx-background-color: transparent;
    -fx-background-radius: 10;
    -fx-border-width: 0;
    -fx-padding: 0;
}
.list-view .list-cell {
    -fx-background-radius: 8;
    -fx-padding: 10 14;
    -fx-border-width: 0;
}
```

### 7.7 弹窗模板

所有确认、提示、输入类弹窗应使用 FXML 自定义弹窗，避免原生系统窗口破坏风格。

```xml
<VBox styleClass="dialog-pane" spacing="14" xmlns="http://javafx.com/javafx"
      xmlns:fx="http://javafx.com/fxml">
    <Label text="提示" styleClass="section-label"/>
    <Label text="确认执行该操作？" styleClass="muted" wrapText="true"/>
    <HBox styleClass="form-actions">
        <Button text="取消" styleClass="btn-secondary"/>
        <Button text="确认" styleClass="btn-primary"/>
    </HBox>
</VBox>
```

```css
.dialog-pane {
    -fx-background-radius: 12;
    -fx-border-radius: 12;
    -fx-border-width: 1;
    -fx-padding: 16;
}
.dialog-pane .button {
    -fx-background-radius: 8;
    -fx-border-radius: 8;
    -fx-border-width: 0 0 1.5 0;
    -fx-padding: 6 16;
    -fx-font-weight: bold;
    -fx-cursor: hand;
}
```

### 7.8 暗色主题覆盖模板

新增组件时，浅色样式写在基础 class，暗色差异只写 `.theme-dark`。不要在基础 class 中写黑色背景来“兼容暗色”。

```css
.new-component {
    -fx-background-radius: 8;
    -fx-border-radius: 8;
    -fx-border-width: 0 0 1.5 0;
    -fx-padding: 12 14;
}
.new-component:hover {
    -fx-background-radius: 8;
}

.theme-dark .new-component {
    -fx-border-width: 0 0 1.5 0;
}
.theme-dark .new-component:hover {
    -fx-border-width: 0 0 1.5 0;
}
```

### 7.9 禁止写法

以下写法会破坏双主题、响应式或统一组件语言，除临时调试外禁止提交。

```java
// 禁止：动态控件写死浅色或暗色，切换主题后不可读。
label.setStyle("-fx-text-fill: #000000;");

// 允许：确实需要动态色时使用主题辅助。
label.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#1e293b", "#f8fafc") + ";");
```

```xml
<!-- 禁止：FXML 中直接写背景、边框、阴影等视觉样式。 -->
<VBox style="-fx-background-color: white; -fx-border-color: #ddd; -fx-effect: dropshadow(...);"/>

<!-- 允许：使用样式类承接视觉，只在 FXML 中保留布局参数。 -->
<VBox styleClass="card-padded" spacing="16" maxWidth="Infinity"/>
```
