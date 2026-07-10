# 标题栏窗口控制按钮右对齐 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让主窗口右上角的最小化、最大化和关闭按钮在初始显示及窗口尺寸变化后持续贴合右边缘。

**Architecture:** 保持标题栏的 `HBox` 和可增长空白 `Region` 不变，将承载它的 `VBox mainLayout` 设为外层 `StackPane` 的全尺寸子节点。标题栏绑定到 `mainLayout` 的实际宽度，使其及按钮组跟随场景尺寸而非业务页面的首选宽度。

**Tech Stack:** JDK 21、JavaFX 21、Maven、JUnit 5。

---

## File Structure

- Modify: `client/src/main/java/com/gomatch/client/ui/ScreenManager.java` — 主窗口根布局与标题栏的尺寸约束。
- Modify: `docs/DEV_LOG.md` — 记录此次 UI 布局修复。
- No test file: 当前标题栏由私有静态工厂方法创建并依赖运行中的 `Stage`，项目没有 JavaFX/TestFX 的 UI 测试启动器；用 Maven 编译和运行时手工验收覆盖真实窗口布局行为。

### Task 1: 建立构建基线

**Files:**
- Modify: none
- Test: Maven 客户端测试套件

- [ ] **Step 1: 运行现有客户端测试**

Run: `mvn -pl client test`

Expected: 构建成功，现有 JUnit 测试全部通过；若失败，记录失败输出并在本次布局修复之外单独处理。

### Task 2: 固定主标题栏到可用宽度

**Files:**
- Modify: `client/src/main/java/com/gomatch/client/ui/ScreenManager.java:94-151`
- Test: `mvn -pl client test`

- [ ] **Step 1: 让主布局填满 `StackPane` 的可用区域**

在 `wrapper.getChildren().addAll(bg, mainLayout);` 后加入以下约束：

```java
StackPane.setAlignment(mainLayout, javafx.geometry.Pos.TOP_LEFT);
mainLayout.maxWidthProperty().bind(wrapper.widthProperty());
mainLayout.maxHeightProperty().bind(wrapper.heightProperty());
```

这使 `mainLayout` 的可用宽高随 `wrapper` 更新，并将其从居中定位改为左上角定位。

- [ ] **Step 2: 将标题栏宽度绑定到主布局的实际宽度**

保留现有绑定：

```java
customTitleBar.prefWidthProperty().bind(mainLayout.widthProperty());
```

它在步骤 1 后会跟随 `wrapper` 的实际宽度；不要改为手动计算 `translateX` 或为按钮设置固定 X 坐标。

- [ ] **Step 3: 编译并运行单元测试**

Run: `mvn -pl client test`

Expected: `BUILD SUCCESS`；该改动不影响网络解析和凭据存储测试。

- [ ] **Step 4: 启动客户端进行真实窗口验收**

Run: `./scripts/ssc_client.ps1`

Expected: 客户端启动后，右上角三个窗口控制按钮贴合右边缘。依次自由拖动窗口四条边、点击最大化后还原，并在登录页与大厅间切换；每个状态中按钮组均保持右上角对齐且可点击。

### Task 3: 更新开发日志并提交

**Files:**
- Modify: `docs/DEV_LOG.md`

- [ ] **Step 1: 追加开发日志**

在 `docs/DEV_LOG.md` 文件末尾追加：

```markdown
### 2026-07-11 — 标题栏窗口控制按钮右对齐

- 将主窗口业务布局固定为外层容器的全尺寸左上角子节点。
- 确保窗口缩放、最大化/还原和页面切换后，右上角控制按钮保持贴边对齐。
```

- [ ] **Step 2: 检查变更与构建结果**

Run: `git diff --check; mvn -pl client test`

Expected: 无空白错误且 `BUILD SUCCESS`。

- [ ] **Step 3: 提交实现**

Run:

```powershell
git add -- client/src/main/java/com/gomatch/client/ui/ScreenManager.java docs/DEV_LOG.md
git commit -m "fix(ui): 修复标题栏控制按钮右对齐"
```

Expected: 创建只包含布局修复与开发日志的独立提交。
