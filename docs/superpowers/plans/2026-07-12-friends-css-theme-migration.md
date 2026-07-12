# 好友页 CSS 主题迁移 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让好友页动态卡片在浅色与暗色主题下均由 CSS 提供可读配色。

**Architecture:** `FriendsController` 只给动态 JavaFX 节点挂接语义样式类。`style.css` 先定义浅色规则，再由 `.theme-dark` 覆盖颜色；既有 `btn-primary`、`btn-danger` 继续负责按钮状态。

**Tech Stack:** Java 21、JavaFX、FXML、CSS、Maven。

---

### Task 1: 为动态好友节点挂接语义类

**Files:**

- Modify: `client/src/main/java/com/gomatch/client/ui/FriendsController.java:120-490`

- [ ] 将成功和失败提示改为 `friend-status-success`、`friend-status-error`。每次先移除两个类，避免状态残留：

```java
errorLabel.getStyleClass().removeAll("friend-status-success", "friend-status-error");
errorLabel.getStyleClass().add("friend-status-success");
```

- [ ] 为好友和申请卡的姓名、ID、积分、状态、留言、按钮、申请头像分别使用 `friend-card-name`、`friend-card-meta`、`friend-card-status-online`、`friend-card-status-offline`、`friend-card-message`、`friend-card-action`、`friend-request-avatar`，并删除对应内联样式。

- [ ] 运行 `rg -n "ScreenManager\.themeColor|setStyle\(\".*text-fill|setStyle\(\".*background-color" client/src/main/java/com/gomatch/client/ui/FriendsController.java`；预期不再存在主题颜色内联代码。

### Task 2: 添加双主题 CSS 语义规则

**Files:**

- Modify: `client/src/main/resources/css/style.css:1107-1137,1359-1728`

- [ ] 在 `.list-item-card` 相邻位置定义浅色规则：名称 `#1e293b`，辅助/留言/离线 `#64748b`，在线和成功 `#16a34a`，失败 `#dc2626`，申请头像 `#f59e0b`；`friend-card-action` 仅定义 `-fx-padding: 4 14` 与 `-fx-font-size: 12px`。

- [ ] 在 `.theme-dark .list-item-card` 相邻位置定义覆盖：名称 `#f8fafc`，辅助/留言/离线 `#a7b0c2`，在线和成功 `#86efac`，失败 `#fca5a5`，申请头像 `#b45309`。不为 `friend-card-action` 设置颜色，以保留 `btn-primary` 和 `btn-danger` 的已有主题交互。

- [ ] 运行 `rg -n "friend-(card|request|status)" client/src/main/resources/css/style.css client/src/main/java/com/gomatch/client/ui/FriendsController.java`；预期每个控制器样式类均有基础 CSS 定义和所需的暗色覆盖。

### Task 3: 验证并记录修复

**Files:**

- Modify: `docs/DEV_LOG.md`

- [ ] 在 `docs/DEV_LOG.md` 追加中文日志：好友页动态节点已从 Java 内联主题颜色迁移到 CSS 语义类，修复 PR #8 遗留暗色可读性回退。

- [ ] 运行 `git diff --check` 与 `mvn -pl client -am compile`。预期前者无输出、后者 `BUILD SUCCESS`；若 Maven 受既有基线阻断，记录首个非本次变更错误并保留静态检查结果。

- [ ] 运行 `git diff --stat` 和 `git diff -- client/src/main/java/com/gomatch/client/ui/FriendsController.java client/src/main/resources/css/style.css docs/DEV_LOG.md`；预期仅涉及已批准的控制器、样式、开发日志及规格/计划文档。

- [ ] 运行 `git add -- client/src/main/java/com/gomatch/client/ui/FriendsController.java client/src/main/resources/css/style.css docs/DEV_LOG.md`、`git diff --cached --check`、`git commit -m "fix(ui): 修复好友页暗色主题可读性"`。若 `.git/index.lock` 仍被占用，保留已验证变更并报告阻塞，不删除锁文件。
