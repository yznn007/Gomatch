# 客户端 MSI 自动发布 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 推送 `v*` 标签时构建 Gomatch MSI，读取同名中文版本说明并创建 GitHub Release。

**Architecture:** `scripts/package_client_msi.ps1` 是本地与 CI 唯一的 MSI 打包入口。`.github/workflows/client-release.yml` 在 Windows Runner 调用该脚本，读取 `docs/releases/<tag>.md`；缺失文件时回退为 GitHub 自动变更摘要。

**Tech Stack:** JDK 21、Maven、JavaFX 21、WiX Toolset 3、jpackage、GitHub Actions。

---

### Task 1: 发布说明约定

**Files:**
- Create: `docs/releases/README.md`
- Create: `docs/releases/v1.0.0.md`
- Modify: `docs/README.md`

- [ ] **Step 1: 创建目录说明。**

```md
# 客户端版本说明

每个正式 Git 标签必须有同名说明文件：`vX.Y.Z` 对应 `vX.Y.Z.md`。

## 模板

## 本次更新

- 新增：
- 修复：
- 优化：

## 安装与升级

运行 Release 附带的 MSI；安装程序会覆盖旧版本。

## 已知问题

- 无。
```

- [ ] **Step 2: 创建 `docs/releases/v1.0.0.md`，使用相同模板。**
- [ ] **Step 3: 在 `docs/README.md` 的开发规范表加入：**

```md
| [releases/README.md](releases/README.md) | 客户端版本说明：标签同名 Markdown、Release 编写模板 |
```

- [ ] **Step 4: 验证。**

Run: `Test-Path docs/releases/README.md; Test-Path docs/releases/v1.0.0.md; rg -n "releases/README" docs/README.md`

Expected: all checks succeed.

- [ ] **Step 5: Commit.**

```powershell
git add docs/releases docs/README.md
git commit -m "docs(release): 新增客户端版本说明约定"
```

### Task 2: MSI 打包脚本

**Files:**
- Create: `scripts/package_client_msi.ps1`

- [ ] **Step 1: 添加参数与前置检查。**

```powershell
param(
    [Parameter(Mandatory)] [string] $Version,
    [string] $OutputDirectory = 'client/target/installer'
)
foreach ($command in 'jpackage.exe', 'candle.exe', 'light.exe') {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "缺少 $command；请安装 JDK 21 与 WiX Toolset 3。"
    }
}
```

- [ ] **Step 2: 调用 `mvn -pl client -am package`，再用 `jpackage --type msi` 打包 `client/target/gomatch-client-1.0.0.jar`。**

The launcher uses `com.gomatch.client.ClientMain`, creates Start Menu and desktop shortcuts, and holds a fixed `--win-upgrade-uuid` constant.

- [ ] **Step 3: 验证帮助和出包。**

Run: `powershell -ExecutionPolicy Bypass -File scripts/package_client_msi.ps1 -Version 1.0.0-test`

Expected: exactly one MSI under `client/target/installer`.

- [ ] **Step 4: Commit.**

```powershell
git add scripts/package_client_msi.ps1
git commit -m "build(client): 新增 MSI 打包脚本"
```

### Task 3: GitHub Release 工作流

**Files:**
- Create: `.github/workflows/client-release.yml`

- [ ] **Step 1: 使用受控触发和权限。**

```yaml
on:
  push:
    tags: ['v*']
  workflow_dispatch:
permissions:
  contents: write
```

- [ ] **Step 2: 使用 `windows-2022`、JDK 21、Maven 缓存和 WiX 3；调用 MSI 脚本。**
- [ ] **Step 3: 版本说明选择逻辑仅查找 `docs/releases/$version.md`；存在时传给 Release 的 `body_path`，否则启用自动生成变更摘要。**
- [ ] **Step 4: 每次运行上传 MSI Artifact；仅标签运行创建 Release 并附加 MSI。**
- [ ] **Step 5: 静态验证。**

Run: `rg -n "tags:|windows-2022|contents: write|docs/releases|body_path|\.msi" .github/workflows/client-release.yml`

Expected: all release controls are present.

- [ ] **Step 6: Commit.**

```powershell
git add .github/workflows/client-release.yml
git commit -m "ci(client): 自动发布 MSI 安装包"
```

### Task 4: 最终验证

- [ ] **Step 1: 重新运行本地 MSI 打包命令。**
- [ ] **Step 2: 运行 `git diff --check; git status --short`，确认不暂存 `RuntimeConfig.java`、其测试目录及现有运行时配置计划。**
