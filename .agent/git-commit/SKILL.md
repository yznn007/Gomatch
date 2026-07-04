---
name: git-commit
description: 规范 Git 提交信息，使用 Conventional Commits 标准格式
---

## 提交信息格式

使用 Conventional Commits 格式：

```
<type>(<scope>): <subject>

[可选的 body]

[可选的 footer]
```

## Type 类型

| Type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 bug |
| `docs` | 文档变更 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 重构（不是修复也不是新功能） |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `build` | 构建系统或依赖变更 |
| `ci` | CI 配置变更 |
| `chore` | 其他不修改 src/test 的变更 |

## Rules

- 使用中文描述 `subject`
- `scope` 为可选，用于标识影响范围
- `subject` 不超过 50 字符
- `subject` 用动词开头，使用第一人称现在时
- `subject` 结尾不要使用句号
- Breaking Changes 在 footer 使用 `BREAKING CHANGE: ` 标注

## 示例

**功能提交：**
```
feat(auth): 添加第三方登录支持

新增 GitHub 和 Google OAuth 登录选项
```

**修复提交：**
```
fix(api): 修复用户查询接口超时问题

当查询参数包含特殊字符时会导致响应超时
```

**破坏性变更：**
```
feat(db)!: 更换 ORM 框架为 Prisma

BREAKING CHANGE: 数据库迁移脚本需要重新编写
所有现有的 seed 脚本需要更新
```

**多行格式：**
```
refactor(core): 重构缓存逻辑

- 移除内存缓存，改为使用 Redis
- 添加缓存失效机制
- 保持 API 接口向后兼容
```