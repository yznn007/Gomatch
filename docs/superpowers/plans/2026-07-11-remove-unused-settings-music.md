# 移除未接入用户设置与音乐表 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 删除未接入的 `t_user_settings` 与 `t_user_music` 设计和代码，保留 `t_user_profile` 配置同步与头像 OSS。

**Architecture:** `UserSettings` 继续是客户端模型，`ConfigManager` 通过 Socket 与 `t_user_profile.config_json` 同步。移除的两张表没有运行时调用者，因此删除 `database` 模块孤立仓储及陈旧架构文档即可。

**Tech Stack:** Java 21、Maven 多模块、MySQL 初始化 SQL、Markdown/Mermaid。

---

### Task 1: 删除孤立的数据库仓储层

**Files:**
- Delete: `database/src/main/java/com/gomatch/database/entity/UserSettings.java`
- Delete: `database/src/main/java/com/gomatch/database/entity/UserMusic.java`
- Delete: `database/src/main/java/com/gomatch/database/repository/UserSettingsRepository.java`
- Delete: `database/src/main/java/com/gomatch/database/repository/UserMusicRepository.java`
- Delete: `database/src/main/java/com/gomatch/database/repository/impl/UserSettingsRepositoryImpl.java`
- Delete: `database/src/main/java/com/gomatch/database/repository/impl/UserMusicRepositoryImpl.java`

- [ ] **Step 1: 运行引用检查，确认仓储层没有生产调用者。**

Run: `rg -n "UserSettingsRepository|UserMusicRepository|database\.entity\.UserSettings|database\.entity\.UserMusic" client server common database -g "*.java"`

Expected: 仅命中这六个待删除文件。

- [ ] **Step 2: 删除六个孤立的实体与 Repository 文件。**

Remove exactly the six files listed above. Do not remove `client/src/main/java/com/gomatch/client/UserSettings.java`.

- [ ] **Step 3: 重新运行引用检查。**

Run: `rg -n "UserSettingsRepository|UserMusicRepository|database\.entity\.UserSettings|database\.entity\.UserMusic" client server common database -g "*.java"`

Expected: no matches (exit code 1).

- [ ] **Step 4: 编译 database 模块。**

Run: `mvn -pl database -am compile`

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: 提交仓储层删除。**

```powershell
git add database
git commit -m "refactor(database): 移除未接入设置音乐仓储"
```

### Task 2: 从初始化 schema 移除两张表

**Files:**
- Modify: `sql/init.sql:209-250`

- [ ] **Step 1: 写 schema 守卫检查。**

Run: `rg -n "CREATE TABLE IF NOT EXISTS `t_user_(settings|music)`" sql/init.sql`

Expected before change: two matches.

- [ ] **Step 2: 删除两段完整建表语句。**

Remove the `CREATE TABLE IF NOT EXISTS` blocks for `t_user_settings` and `t_user_music`, including their indexes and foreign keys. Preserve the following `t_audit_log` block unchanged.

- [ ] **Step 3: 验证 schema 不再声明已删除表。**

Run: `rg -n "t_user_settings|t_user_music" sql/init.sql`

Expected: no matches (exit code 1).

- [ ] **Step 4: 提交 schema 变更。**

```powershell
git add sql/init.sql
git commit -m "refactor(sql): 移除未接入设置音乐表"
```

### Task 3: 收敛数据库设计文档

**Files:**
- Modify: `docs/DB_DESIGN.md:6, 90-230, 400-450, 753-848, 1698-1755, 2240-2475, 3065-3125`
- Modify: `docs/DEV_LOG.md:after line 20`

- [ ] **Step 1: 先将头像流程改为真实实现。**

Replace `t_user_settings` avatar reads/writes in Mermaid and flow tables with `t_user.avatar`; keep the OSS upload and presigned URL portions. Replace the fictional settings-login flow with the real `t_user_profile.config_json` Socket sync flow.

- [ ] **Step 2: 删除未实现的音乐业务叙述。**

Remove background-music upload/list/default flows, the `t_user_music` ER node, its detailed table section, its SQL copy, and section `13.8 用户音乐存储方案` in full. Retain the client bundled-music behavior only where unrelated to this database design.

- [ ] **Step 3: 删除未接入设置表叙述。**

Remove the `t_user_settings` ER node, section `2.2.12`, its index-row, and every `t_user_settings` reference. Renumber following subsections when necessary.

- [ ] **Step 4: 追加开发日志。**

Add a dated entry stating that unused `t_user_settings`/`t_user_music` schema and `database` repositories were removed; state that `t_user_profile.config_json` remains the authoritative cloud configuration path.

- [ ] **Step 5: 验证活动架构文档无残留。**

Run: `rg -n "t_user_settings|t_user_music|UserSettingsRepository|UserMusicRepository" docs/DB_DESIGN.md`

Expected: no matches (exit code 1).

- [ ] **Step 6: 提交文档收敛。**

```powershell
git add docs/DB_DESIGN.md docs/DEV_LOG.md
git commit -m "docs(database): 收敛用户配置存储说明"
```

### Task 4: 全量验证

**Files:**
- Verify: `client/src/main/java/com/gomatch/client/UserSettings.java`
- Verify: `client/src/main/java/com/gomatch/client/ConfigManager.java`
- Verify: `server/src/main/java/com/gomatch/server/dao/ConfigDAO.java`
- Verify: `sql/init.sql`

- [ ] **Step 1: 运行残留扫描。**

Run: `rg -n "t_user_settings|t_user_music|UserSettingsRepository|UserMusicRepository" client server common database sql docs/DB_DESIGN.md -g "*.java" -g "*.sql" -g "*.md"`

Expected: no matches (exit code 1).

- [ ] **Step 2: 核对保留链路。**

Run: `rg -n "config_json|REQ_UPDATE_CONFIG|REQ_GET_CONFIG" client/src/main server/src/main sql/init.sql -g "*.java" -g "*.sql"; rg -n "uploadAvatar|generatePresignedUrl|deleteAvatar" server/src/main -g "*.java"`

Expected: `t_user_profile` cloud-config and avatar OSS handlers remain matched.

- [ ] **Step 3: 编译所有受影响模块。**

Run: `mvn -pl database,server,client -am compile`

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: 运行格式检查并审阅变更。**

Run: `git diff --check; git diff --stat; git status --short`

Expected: no whitespace errors; only planned source, SQL, and documentation changes.

- [ ] **Step 5: 提交验证后的收尾变更（若前述按文件分提交已完成则无需额外提交）。**

```powershell
git status --short
```

Expected: clean working tree.