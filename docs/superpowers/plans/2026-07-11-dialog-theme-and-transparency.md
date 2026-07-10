# 弹窗主题与透明窗口修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 所有客户端弹窗在浅色和暗色主题中保持可读，且无边框圆角外不再露出矩形窗口底色。

**Architecture:** `ScreenManager` 提供透明弹窗主题路径：保留主题类与用户字体，但不向透明 Scene 根节点写入矩形渐变背景。FXML、原生 JavaFX Dialog 和 `BoardController` 的手动 Stage 都通过该路径；CSS 负责卡片与按钮的主题表面颜色。

**Tech Stack:** JDK 21、JavaFX FXML/CSS、Maven。

---

## 文件职责

- `client/src/main/java/com/gomatch/client/ui/ScreenManager.java`：透明 Scene、主题、字体和原生 Dialog 注入。
- `client/src/main/java/com/gomatch/client/ui/BoardController.java`：非阻塞结算和等待请求 Stage。
- `client/src/main/resources/fxml/AvatarPreviewUI.fxml`、`CropAvatarUI.fxml`：移除仅深色内联外观。
- `client/src/main/resources/css/style.css`：浅色基础与 `.theme-dark` 覆盖。
- `docs/DEV_LOG.md`：修复与验证记录。

### Task 1: 建立统一透明弹窗主题入口

**Files:**
- Modify: `client/src/main/java/com/gomatch/client/ui/ScreenManager.java:176-220, 400-449, 555-610`

- [ ] **Step 1: 记录当前回归信号**

Run: `rg -n "scene\.setFill\(.*TRANSPARENT|applyUserTheme\(scene\)|applyUserFont\(scene\)" client/src/main/java/com/gomatch/client/ui/ScreenManager.java`

Expected: `showDialog` 尚未设置透明 Scene，且只有普通主题调用。

- [ ] **Step 2: 增加不写矩形背景的主题方法**

Add next to `applyUserTheme`:

```java
public static void applyDialogTheme(Scene scene) {
    if (scene == null || scene.getRoot() == null) return;
    scene.getRoot().getStyleClass().removeAll(THEME_LIGHT, THEME_DARK);
    scene.getRoot().getStyleClass().add(isDarkTheme() ? THEME_DARK : THEME_LIGHT);
    scene.getRoot().setStyle(appendBackgroundColor(scene.getRoot().getStyle(), "transparent"));
    applyUserFont(scene);
}
```

Keep `applyUserTheme(Scene)` unchanged for the full main window: it must retain the dark gradient.

- [ ] **Step 3: Correct the FXML dialog path**

In `showDialog`, use:

```java
dialog.initStyle(StageStyle.TRANSPARENT);
Scene scene = new Scene(dialogWrapper);
scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
scene.setCamera(new javafx.scene.PerspectiveCamera(false));
applyStylesheet(scene);
applyDialogTheme(scene);
```

Keep owner, modality, title-bar width binding, `setResizable(false)` and `showAndWait()` unchanged. Do not call `applyUserTheme(scene)` in this method.

- [ ] **Step 4: Correct the native Dialog path**

In `showStyledDialog`, when a `DialogPane` Scene is available (both listener and immediate branch), run:

```java
scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
applyDialogTheme(scene);
```

Retain the fill-property listener, stylesheet injection, owner and drag handling. This makes `TextInputDialog` and all `Alert` instances inherit `theme-light` or `theme-dark`.

- [ ] **Step 5: Verify the infrastructure change**

Run: `rg -n -A 45 "public static <T> T showDialog" client/src/main/java/com/gomatch/client/ui/ScreenManager.java; rg -n -A 62 "showStyledDialog" client/src/main/java/com/gomatch/client/ui/ScreenManager.java`

Expected: Both entry points set `Color.TRANSPARENT` and call `applyDialogTheme`; neither applies the full-window gradient to a transparent dialog.

- [ ] **Step 6: Commit**

```powershell
git add client/src/main/java/com/gomatch/client/ui/ScreenManager.java
git commit -m "fix(ui): 统一透明弹窗主题注入"
```

### Task 2: 修复棋局页手动创建的 Stage

**Files:**
- Modify: `client/src/main/java/com/gomatch/client/ui/BoardController.java:1159-1225`

- [ ] **Step 1: 记录当前回归信号**

Run: `rg -n -A 75 "showGameOverDialog|showPendingRequestDialog" client/src/main/java/com/gomatch/client/ui/BoardController.java`

Expected: 两条路径都使用 `UNDECORATED`、未设置透明填充并调用 `applyUserTheme`。

- [ ] **Step 2: 对齐结算和等待请求 Stage**

For both `showGameOverDialog` and `showPendingRequestDialog`, replace their visual initialization with:

```java
dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
javafx.scene.Scene scene = new javafx.scene.Scene(layout);
scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
ScreenManager.applyStylesheetPublic(scene);
ScreenManager.applyDialogTheme(scene);
```

Use `pendingRequestStage.initStyle(...)` in the pending method. Keep `show()` (not `showAndWait()`), callback, owner, modality, `pendingRequest` lifecycle and network behavior unchanged.

- [ ] **Step 3: Verify the manual paths**

Run: `rg -n "StageStyle\.(UNDECORATED|TRANSPARENT)|setFill\(.*TRANSPARENT|apply(DialogTheme|UserTheme)" client/src/main/java/com/gomatch/client/ui/BoardController.java`

Expected: Both paths use `TRANSPARENT`, set transparent fill and call `applyDialogTheme` only.

- [ ] **Step 4: Commit**

```powershell
git add client/src/main/java/com/gomatch/client/ui/BoardController.java
git commit -m "fix(ui): 修复棋局弹窗透明底色"
```

### Task 3: 主题化固定色的弹窗资源

**Files:**
- Modify: `client/src/main/resources/fxml/AvatarPreviewUI.fxml:8-35`
- Modify: `client/src/main/resources/fxml/CropAvatarUI.fxml:11-64`
- Modify: `client/src/main/resources/css/style.css:1299-1518`

- [ ] **Step 1: 记录固定色证据**

Run: `rg -n "rgba\(0,0,0,0\.85\)|rgba\(10,15,25,0\.96\)|#e2e8f0" client/src/main/resources/fxml/AvatarPreviewUI.fxml client/src/main/resources/fxml/CropAvatarUI.fxml`

Expected: 两个弹窗含有无法响应 `theme-light` 的固定深色外观。

- [ ] **Step 2: 用样式类替换 FXML 固定外观**

Use `avatar-preview-dialog` on AvatarPreview's outer StackPane and `avatar-preview-name` on its user label. Use `crop-avatar-dialog`, `crop-avatar-title`, `crop-avatar-title-text` and `crop-avatar-actions` for CropAvatar's root, title HBox, title Label and bottom HBox. Remove only the fixed outer surface/text colors; retain dimensions, paddings, crop canvas, click behavior and existing button classes.

- [ ] **Step 3: Add the base and dark styles**

Add near the dialog rules:

```css
.avatar-preview-dialog { -fx-background-color: rgba(226, 232, 240, 0.94); }
.avatar-preview-name, .crop-avatar-title-text { -fx-text-fill: #1e293b; }
.crop-avatar-dialog, .crop-avatar-actions { -fx-background-color: rgba(248, 250, 252, 0.96); }
.theme-dark .avatar-preview-dialog { -fx-background-color: rgba(0, 0, 0, 0.85); }
.theme-dark .avatar-preview-name, .theme-dark .crop-avatar-title-text { -fx-text-fill: #e2e8f0; }
.theme-dark .crop-avatar-dialog, .theme-dark .crop-avatar-actions { -fx-background-color: rgba(10, 15, 25, 0.96); }
```

Do not add unscoped dark overrides. Existing `btn-*` selectors already supply hover, armed and disabled states.

- [ ] **Step 4: Verify resource audit**

Run: `rg -n "<Button" client/src/main/resources/fxml/*DialogUI.fxml client/src/main/resources/fxml/AvatarPreviewUI.fxml client/src/main/resources/fxml/CropAvatarUI.fxml client/src/main/resources/fxml/GameOverUI.fxml client/src/main/resources/fxml/CreateRoomUI.fxml client/src/main/resources/fxml/ConfirmExitUI.fxml; rg -n "rgba\(0,0,0,0\.85\)|rgba\(10,15,25,0\.96\)|#e2e8f0" client/src/main/resources/fxml/AvatarPreviewUI.fxml client/src/main/resources/fxml/CropAvatarUI.fxml`

Expected: Every popup button uses a standard button class; checked fixed colors occur only in scoped CSS rules.

- [ ] **Step 5: Commit**

```powershell
git add client/src/main/resources/fxml/AvatarPreviewUI.fxml client/src/main/resources/fxml/CropAvatarUI.fxml client/src/main/resources/css/style.css
git commit -m "fix(ui): 适配弹窗浅暗主题样式"
```

### Task 4: 完整验证和开发日志

**Files:**
- Modify: `docs/DEV_LOG.md`

- [ ] **Step 1: Compile client**

Run: `mvn -pl client -am test`

Expected: Maven exits with code `0`; no Java compilation, dependency or FXML loading error.

- [ ] **Step 2: Audit every production dialog creation path**

Run: `rg -n "new (Alert|TextInputDialog|Dialog)|new javafx\.stage\.Stage\(|new Stage\(" client/src/main/java; rg -n "showStyled(Alert|Dialog)\(" client/src/main/java`

Expected: Native dialogs use `ScreenManager.showStyledAlert` or `showStyledDialog`; the two manual Stages are the reviewed Board paths.

- [ ] **Step 3: Manual UI verification**

With JDK 21 and required services available, run `./scripts/ssc_client.ps1`. In both themes open server configuration, room password, avatar preview/crop, exit confirmation, game over, pending undo/draw and confirmation Alert. Confirm text/button readability, hover/pressed feedback, and no rectangle outside rounded content.

- [ ] **Step 4: Append DEV_LOG and commit**

Record the opaque Scene plus full-window-gradient root cause, unified paths and Maven result. Then run `git diff --check; git diff --stat; git status --short --branch` and commit only the log:

```powershell
git add docs/DEV_LOG.md
git commit -m "docs(devlog): 记录弹窗主题修复"
```
