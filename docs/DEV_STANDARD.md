# 开发规范

## 一、Commit 提交规范

采用 Conventional Commits 格式：

```
<type>: <subject>
```

### type 类型

| type | 说明 |
|------|------|
| feat | 新功能 |
| fix | 修复 Bug |
| refactor | 重构代码 |
| docs | 文档变更 |
| chore | 杂项（构建、配置等） |
| style | 代码格式调整 |
| test | 测试相关 |
| perf | 性能优化 |

### 示例

```
feat: 实现五连判定算法
fix: 修复房间解散后玩家未正确退出
docs: 更新需求文档技术栈章节
chore: 配置 Maven Shade 插件
```

### 规则

- subject 用中文描述做了什么
- 不写句号
- 不超过 50 字

---

## 二、分支规范

> **禁止直接推送 `main` 分支。所有开发在 `dev` 上进行，阶段稳定后由 `dev` 合并到 `main`。**

| 分支 | 命名格式 | 说明 |
|------|----------|------|
| main | `main` | 主分支，保持稳定可运行，仅从 dev 合并 |
| dev | `dev` | 开发主分支，所有 feature/fix 在此集散 |
| feature | `feature/<功能名>` | 新功能分支，从 dev 切出，完成后合并回 dev |
| fix | `fix/<问题简述>` | Bug 修复分支，从 dev 切出 |
| refactor | `refactor/<模块>` | 重构分支，不改变功能只改代码结构 |
| docs | `docs/<内容>` | 文档分支，更新需求/规范/README 等 |
| chore | `chore/<任务>` | 杂项分支，构建配置、依赖升级、CI 等 |
| test | `test/<模块>` | 测试分支，补充或重构测试 |
| perf | `perf/<模块>` | 性能优化分支 |
| release | `release/v<版本>` | 发版分支，从 dev 切出，合并到 main 并打 tag |
| hotfix | `hotfix/<问题>` | 紧急修复分支，从 main 切出，修复后合并回 main 和 dev |

### 工作流

```
main ─── 仅合并 dev，不直接开发
  ↑
dev ──── 日常开发集散地
  │
  ├── feature/xxx  ── 开发 → 合并回 dev → 删除
  ├── feature/xxx  ── 开发 → 合并回 dev → 删除
  └── fix/xxx      ── 修复 → 合并回 dev → 删除
```

#### 场景一：单人串行开发

```
(1) 从 dev 切 feature/A
(2) 在 feature/A 上完成开发、提交
(3) 切回 dev，合并 feature/A
(4) 删除 feature/A
(5) 从 dev 切 feature/B
    ...
```

适合独立模块，一次只做一个功能。

#### 场景二：多人并行开发

```
                  feature/大厅列表（开发者甲）
                 /
dev ──────── 合并 ──────── 合并 ──────── dev
                 \
                  feature/棋盘UI（开发者乙）

两人从同一 dev 切出，各自开发，依次合并回 dev。
```

适合 client 和 server 分离开发等场景。

#### 场景三：开发中修复 Bug

```
(1) dev 上正在开发 feature/聊天模块
(2) 发现紧急 Bug（如房间无法解散）
(3) 从 dev 切 fix/房间解散
(4) 修复后合并回 dev
(5) feature/聊天模块 合并回 dev（拉取修复后的代码）
```

修复分支直接从 dev 切出，优先合并，确保其他功能分支能拉取到修复。

#### 场景四：多人并行 + 冲突处理

```
甲: feature/落子同步      乙: feature/聊天模块
         │                        │
         └──────┬─────────────────┘
                ▼
        dev (可能冲突)
                │
        解决冲突后提交
                │
          继续开发
```

两人同时修改同一文件时，后合并的人负责解决冲突。

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