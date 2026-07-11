# 开发规范

## 一、Commit 提交规范

项目提倡采用**细粒度提交**，并结合 Conventional Commits 规范。

### 格式约定
提交的 Header 前缀使用标准的英文类型与范围，而**提交消息的主题（Subject）和主体（Body）必须使用中文描述**。

基本格式：
```
<type>(<scope>): <中文简短描述>

<中文详细描述（可选，描述具体修改背景或原因）>
```

### type 类型

| type | 说明 | 示例 |
|------|------|------|
| feat | 新增功能 | `feat(security): 实现客户端和服务端加密与安全工具类` |
| fix | 修复 Bug | `fix(redis): 在启动时程序化配置禁用 stop-writes-on-bgsave-error` |
| refactor | 重构代码 | `refactor(client): 解耦提取客户端消息解析器` |
| docs | 文档变更 | `docs: 新增 Git 提交规范说明` |
| style | 代码格式与样式调整 | `style(ui): 统一全局 CSS 控件按钮、文本框和主题类` |
| build | 构建工具或外部依赖修改 | `build(client): 新增 JUnit 测试依赖与 surefire 插件` |
| chore | 其他辅助/日常杂项 | `chore: 清理冗余配置与旧运行脚本` |
| test | 测试用例编写/重构 | `test(session): 编写 SessionPolicy 规则判定测试` |

### 提交粒度与规则

- **细粒度原则**：逻辑独立的修改必须单独提交。禁止将不相关的多个模块修改（如安全逻辑、UI 样式、持久化配置等）打包进一个庞大粗糙的 Commit，以便于回滚、审查和追溯历史。
- **描述清晰**：subject（中文简短描述）说明具体“做了什么”，不写句号，字数不超过 50 字。
- **详述背景**：对于复杂的逻辑变更，应在 Body（中文详细描述）中说明修改背景、原因及核心影响范围。
- **PR 合并原则**：个人分支开发完成后，必须推送到远程仓库并通过提交 **Pull Request (PR)** 进行合并，由项目审查人员审核通过后合并入开发分支（如 `dev`），禁止直接推送或在本地合并后直接上传。

---

## 二、分支规范

> **禁止直接推送 `main` 和 `dev` 分支。所有日常开发在个人分支上进行，完成后提交 Pull Request (PR) 申请合并至 `dev`。**

| 分支 | 命名格式 | 说明 |
|------|----------|------|
| main | `main` | 主分支，保持稳定可运行，仅从 dev 合并（通常以 PR 或 Release 合并形式） |
| dev | `dev` | 开发主分支，作为所有个人分支的目标合并分支。**禁止直接 Push**，仅能通过 PR 合并 |
| feature | `feature/<功能名>` | 新功能分支，从 dev 切出，完成后通过 PR 合并回 dev |
| fix | `fix/<问题简述>` | Bug 修复分支，从 dev 切出，完成后通过 PR 合并回 dev |
| refactor | `refactor/<模块>` | 重构分支，不改变功能只改代码结构，完成后通过 PR 合并回 dev |
| docs | `docs/<内容>` | 文档分支，更新需求/规范/README 等，完成后通过 PR 合并回 dev |
| chore | `chore/<任务>` | 杂项分支，构建配置、依赖升级、CI 等，完成后通过 PR 合并回 dev |
| test | `test/<模块>` | 测试分支，补充或重构测试，完成后通过 PR 合并回 dev |
| perf | `perf/<模块>` | 性能优化分支，完成后通过 PR 合并回 dev |
| release | `release/v<版本>` | 发版分支，从 dev 切出，合并到 main 并打 tag |
| hotfix | `hotfix/<问题>` | 紧急修复分支，从 main 切出，修复后通过 PR 合并回 main 和 dev |

### 工作流

```
main ─── 仅从 dev 合并，不直接开发
  ↑
dev ──── 开发主分支（受保护，仅接受 PR 合并，禁止直接推送）
  ▲
  │ (通过 Pull Request 合并)
  ├────── feature/xxx  ── 开发 → 推送远程 → 提交 PR ── (审查通过) ── 合并 ── 删除分支
  ├────── feature/xxx  ── 开发 → 推送远程 → 提交 PR ── (审查通过) ── 合并 ── 删除分支
  └────── fix/xxx      ── 修复 → 推送远程 → 提交 PR ── (审查通过) ── 合并 ── 删除分支
```

#### 场景一：单人串行开发

```
(1) 从 dev 切出个人分支 feature/A
(2) 在 feature/A 上完成开发、提交并 Push 到远程
(3) 在远程仓库发起 Pull Request (PR) 申请合并至 dev
(4) 由团队成员/项目负责人 Review 并合并 PR，删除远程/本地分支 feature/A
(5) 本地拉取最新的 dev，切出 feature/B
    ...
```

适合独立模块，一次只做一个功能。

#### 场景二：多人并行开发

```
                  feature/大厅列表（开发者甲）── Push ── 提交 PR ── 合并 ──┐
                 /                                                     ▼
dev ────────────┬──────────────────────────────────────────────────── dev
                 \                                                     ▲
                  feature/棋盘UI（开发者乙）── Push ── 提交 PR ── 合并 ───┘

两人从同一 dev 切出，各自在个人分支开发，完成后分别推送至远程并提交 PR，由项目负责人审核并合并入 dev。
```

适合 client 和 server 分离开发等场景。

#### 场景三：开发中修复 Bug

```
(1) 开发人员甲在 feature/聊天模块 分支开发
(2) 发现紧急 Bug（如房间无法解散）
(3) 开发人员乙从 dev 切出 fix/房间解散 分支，修复后推送远程并提交 PR
(4) PR 快速审核合并回 dev
(5) 正在开发的甲从远程 pull 最新的 dev 并 merge 到 feature/聊天模块 中（获取已修复的代码），继续开发
```

修复分支直接从 dev 切出，通过 PR 优先合并，确保其他功能分支能及时拉取到修复。

#### 场景四：冲突处理与解决

当多人并行修改相同文件时，在 PR 阶段可能出现冲突：
```
(1) 甲的 feature/落子同步 PR 已被合并回 dev
(2) 乙的 feature/聊天模块 PR 提示存在冲突
(3) 乙在本地切换回 feature/聊天模块，执行以下操作：
    git checkout dev
    git pull origin dev
    git checkout feature/聊天模块
    git merge dev (或者 git rebase dev)
(4) 在本地解决冲突并保存，重新提交并 Push
(5) PR 冲突解决，等待审核通过后自动合并
```

## 三、版本规范

格式：`v<major>.<minor>.<patch>`

| 版本位 | 触发条件 | 示例 |
|--------|----------|------|
| major | 重大重构或架构变更 | v2.0.0 |
| minor | 新增功能/模块 | v1.1.0 |
| patch | Bug 修复或小优化 | v1.0.1 |

### 版本标记方式

```bash
# 最终发版
git checkout main
git merge dev
git tag v1.0.0
git push origin main --tags
```

### 本项目的版本规划

| 版本 | 内容 |
|------|------|
| v1.0.0 | 全部开发完成，首次发布 |

> **所有功能全部开发完成并通过测试后，dev 一次性合并到 main 并打 tag，严禁在开发过程中合并main。**

---

## 四、代码规范

### 4.1 命名规范

| 类别 | 规范 | 示例 |
|------|------|------|
| Java 类名 | 大驼峰 | `BoardController`, `GameRecord` |
| Java 方法/变量 | 小驼峰 | `handleReplay()`, `myUsername` |
| Java 常量 | 全大写下划线 | `QUICK_MATCH`, `GAME_OVER` |
| FXML 文件 | 大驼峰 + UI 后缀 | `LoginUI.fxml`, `LobbyUI.fxml` |
| Controller 类 | 大驼峰 + Controller 后缀 | `LoginController`, `BoardController` |
| 包名 | 全小写 | `com.gomatch.client.ui` |

### 4.2 FXML ↔ Controller 映射

```
fx:id="xxx"    ↔  @FXML private Type xxx;
onAction="#handleXxx"  ↔  @FXML private void handleXxx()
fx:controller="com.gomatch.client.ui.XxxController"
```

FXML 命名空间**不带版本号**：`xmlns="http://javafx.com/javafx"`（写 `/21` 会导致 FXML LoadException）。

### 4.3 NPE 防御

- 所有从外部获取的对象（网络消息、用户输入、回调参数）使用前必须做 null 检查
- `@FXML` 注入的控件在 `initialize()` 中确保引用非空
- 网络回调中 `currentUser` / `myUsername` 等状态变量先判空再使用

```java
// ✅ 正确示例
String username = (currentUser != null) ? currentUser.getUsername() : "player1";
```

### 4.4 状态管理

- 跨 Controller 传递状态：使用 `AppConfig` 全局静态变量，避免依赖时序
- `ScreenManager.showDialog()` 是阻塞调用（`dialog.showAndWait()`），对话框关闭后才执行后续代码，**不要**在 `showDialog()` 之后通过 setter 传递参数给即将显示的 Controller

```java
// ❌ 错误：showDialog 阻塞，setter 来不及时执行
ctrl = ScreenManager.showDialog("GameOverUI.fxml");
ctrl.setAiMode(aiMode);  // 此时对话框已关闭，handleReplay 已执行完毕

// ✅ 正确：提前设置全局状态
AppConfig.setAiMode(this.aiMode);
ScreenManager.showDialog("GameOverUI.fxml");
// GameOverController 内部读取 AppConfig.isAiMode()
```

### 4.5 UI 操作线程安全

- 所有 JavaFX UI 更新必须在 JavaFX Application Thread 中执行
- 网络回调使用 `Platform.runLater()` 包裹 UI 更新
- 避免在非 UI 线程直接操作 `@FXML` 控件

### 4.6 协议通信

- 服务端客户端通信使用JSON 消息协议：`{"cmd":"COMMAND","data":{"key":"value"}}`
- 协议常量统一在 `common` 模块的 `Protocol.java` 中定义
- 禁止在 Controller 或 Handler 中硬编码协议字符串（魔法字符串）

### 4.7 预览模式

每个 Controller 需支持 `setPreviewMode(true)` 绕过网络调用，用假数据填充：

```java
public void setPreviewMode(boolean preview) {
    this.isPreview = preview;
    if (preview) {
        // 填充假数据
    }
}
```

---

## 五、UI 样式规范

UI 详细规则以 [UI_GUIDELINE.md](UI_GUIDELINE.md) 为准，本文件只保留协作时必须记住的硬约束：

- 新增或修改 FXML/CSS 时优先复用 `style.css` 中已有样式类，避免在 FXML 写大段内联视觉样式。
- 暗色主题差异必须限定在 `.theme-dark` 作用域下，禁止新增未加作用域的暗色全局覆盖。
- Controller 动态生成文字颜色时使用 `ScreenManager.themeColor(light, dark)`，保证主题切换后可读。
- FXML 命名空间不带版本号：`xmlns="http://javafx.com/javafx"`。
- 按钮、输入框、卡片、弹窗、列表等组件模板参考 `UI_GUIDELINE.md` 第 7 节。

---

## 六、文档维护

| 文档 | 触发时机 |
|------|----------|
| `docs/DEV_LOG.md` | 每次完成任务后追加日志 |
| `docs/PRD.md` | 需求范围、功能清单、验收标准变化时同步 |
| `docs/DEV_GUIDELINE.md` | Git、分支、代码协作规范变化时 |
| `AGENTS.md` | 命令/架构/约定变化时 |
| `README.md` / `docs/README.md` | 项目入口、启动方式、文档索引变化时 |

### 日志格式

```
| YYYY-MM-DD HH:mm | 作者 | 工作内容简述 | 问题/备注 |
```

- 时间精确到分钟，格式 `YYYY-MM-DD HH:mm`
- 作者填写本人标识（Git 用户名或姓名缩写）
- 工作内容简洁描述做了什么
- 备注记录遇到的问题或注意事项
