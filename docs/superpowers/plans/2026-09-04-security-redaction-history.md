# Security Redaction and History Rewrite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不修改 `docs/school_materials/**` 内容的前提下，将 Gomatch 当前文件和全部可达 Git 历史中的个人姓名、个人邮箱、硬编码服务凭据统一脱敏，并让运行时凭据通过 `.env` 注入。

**Architecture:** 当前运行时配置继续由项目根目录 `.env` 提供，`.env.example` 只保留占位符；SQL 初始化脚本使用 `.env` 中的种子密码动态替换标记，并将种子用户邮箱置空。历史清理由 `git-filter-repo` 一次性处理全部本地 refs：提交作者/提交者统一为非个人身份，提交消息和非教学文本中的敏感信息替换为中性文本，教学资料 blob 保持原字节不变。

**Tech Stack:** Git 2.x、git-filter-repo、Bash、PowerShell、Java 21/Maven 多模块项目、MySQL、Redis。

---

### Task 1: 建立历史重写前检查点

**Files:**
- Create outside repository: a permission-restricted Git bundle backup
- Inspect: all local branches, remote-tracking refs, tags, Codex internal refs, reflogs, and worktree status

- [ ] **Step 1: Confirm scope and record refs**

Run:

```bash
git status --short --branch
git show-ref
git reflog --all --date=iso
git ls-remote --heads --tags origin
```

Expected: the current checkout and known refs are recorded; no teaching-material file is modified by this task.

- [ ] **Step 2: Create a recoverable pre-rewrite bundle outside the repository**

Run with an explicit temporary directory outside `/Users/Apple/Projects/Gomatch`:

```bash
BACKUP_DIR="$(mktemp -d)"
chmod 700 "$BACKUP_DIR"
git bundle create "$BACKUP_DIR/Gomatch-pre-rewrite.bundle" --all
chmod 600 "$BACKUP_DIR/Gomatch-pre-rewrite.bundle"
```

Expected: the bundle contains every ref that will be rewritten and is not tracked by the repository.

### Task 2: Complete runtime secret migration

**Files:**
- Modify: `.env.example`
- Modify: `sql/init.sql`
- Modify: `scripts/init-db.sh`
- Modify: `scripts/init-db.ps1`
- Modify: `docs/DB_DESIGN.md`
- Modify: `docs/COMMON.md`
- Modify: `common/src/main/java/com/gomatch/common/Message.java`

- [ ] **Step 1: Add the seed-password contract**

Add `GOMATCH_SEED_PASSWORD=your_seed_password` to `.env.example`. Keep the real value only in the ignored local `.env`; never print it or stage it.

- [ ] **Step 2: Remove seed credentials from tracked SQL**

In `sql/init.sql`, make the seed-user `email` column nullable, replace every seed password literal with the exact marker `${GOMATCH_SEED_PASSWORD}`, replace seed email values with SQL `NULL`, and document that the application stores a password hash after normal registration/migration.

- [ ] **Step 3: Inject the seed password only while initializing**

Make both initialization scripts load `GOMATCH_SEED_PASSWORD`, reject an empty value, escape it as a SQL literal, and replace `${GOMATCH_SEED_PASSWORD}` in memory/streamed SQL before invoking `mysql`. The tracked scripts must not contain a real password.

- [ ] **Step 4: Remove credential-looking examples from live documentation and examples**

Use neutral placeholders such as `<user-password>` in protocol examples. Replace the JDBC example in `docs/DB_DESIGN.md` with values read from `RuntimeConfig.loadRuntimeProperties("database.properties")`; do not leave any usable credential in documentation.

- [ ] **Step 5: Validate the runtime migration statically**

Run:

```bash
bash -n scripts/init-db.sh
pwsh -NoLogo -NoProfile -Command "[System.Management.Automation.Language.Parser]::ParseFile((Resolve-Path 'scripts/init-db.ps1'), [ref]\$null, [ref]\$null) | Out-Null"
rg -n --hidden --glob '!.git/**' --glob '!docs/school_materials/**' "<known-credential>|<personal-email>" .
```

Expected: syntax checks pass and the scan returns no tracked runtime/documentation result.

### Task 3: Redact current non-teaching text

**Files:**
- Modify: `README.md`
- Modify: `docs/DEV_LOG.md`
- Modify: other non-teaching text files only when the scan identifies a person name, personal email, or hard-coded service credential

- [ ] **Step 1: Remove the member roster and test-account disclosure from README**

Keep project usage and architecture information, but replace the credential table with a short statement that seed credentials are supplied by `GOMATCH_SEED_PASSWORD` in local `.env`. Remove real names, personal addresses, and personal account handles from the current README.

- [ ] **Step 2: Normalize current development-log author fields**

Keep technical change descriptions, but set the author field of each current log entry to `Contributor`; remove personal emails and names from the non-teaching log.

- [ ] **Step 3: Verify the teaching-material boundary before staging**

Compare the SHA-256 digest of every tracked file under `docs/school_materials/**` before and after current-tree edits. Every digest must remain identical.

### Task 4: Record the sanitized current tree

**Files:**
- Modify: `docs/DEV_LOG.md`
- Add: `docs/superpowers/plans/2026-09-04-security-redaction-history.md`

- [ ] **Step 1: Add the security-work log entry**

Append a Chinese entry describing `.env` injection, current-text redaction, and the fact that teaching materials were intentionally excluded. Do not include a person name, email, IP, password, token, or private key in the entry.

- [ ] **Step 2: Stage only intended files and check the staged diff**

Run:

```bash
git add -A
git diff --cached --check
git diff --cached --stat
```

Expected: `.env` is not staged; `docs/school_materials/**` has no staged changes.

- [ ] **Step 3: Commit with a neutral identity**

Run:

```bash
git -c user.name='Gomatch Contributor' -c user.email='contributor.invalid' commit -m 'chore(security): 清理敏感配置与个人信息'
```

### Task 5: Rewrite every reachable local history

**Files:**
- Rewrite: all refs returned by `git show-ref`, including `main`, remote-tracking refs, tags, and local Codex refs
- Preserve byte-for-byte: every blob reachable at a path below `docs/school_materials/`

- [ ] **Step 1: Install git-filter-repo into a temporary tool directory**

Run:

```bash
FILTER_REPO_DIR="$(mktemp -d)"
python3 -m pip install --quiet --target "$FILTER_REPO_DIR" git-filter-repo
PATH="$FILTER_REPO_DIR/bin:$PATH" git filter-repo --version
```

- [ ] **Step 2: Run a path-aware content and metadata callback**

Use `git filter-repo --force --replace-refs update-no-add` over all existing refs. The callback must:

1. leave every `docs/school_materials/**` file-info/blob unchanged;
2. replace email-shaped personal addresses and known member names in other Markdown/source/config text;
3. replace historical SQL seed passwords/emails and historical `database.properties` credential values with `.env` markers or neutral values;
4. replace personal names/handles in commit messages while preserving repository infrastructure URLs such as the GitHub remote owner;
5. set every author and committer to `Gomatch Contributor <contributor.invalid>`;
6. preserve file modes, tags, parent relationships, and technical source behavior apart from the requested redaction.

- [ ] **Step 3: Re-add the recorded origin URL if the rewrite removes it**

Run:

```bash
git remote get-url origin || git remote add origin "<重写前记录的 origin URL>"
```

The repository owner in this remote URL is infrastructure identity, not a secret; changing it would disconnect the project from its GitHub repository.

### Task 6: Verify the rewritten repository

**Files:**
- Inspect: all reachable commits, blobs, refs, tags, and teaching-material digests

- [ ] **Step 1: Verify identities and commit messages**

Run scans over `git log --all` for personal names, personal email addresses, old handles, and known credential literals. Every reachable commit must use the neutral author/committer identity.

- [ ] **Step 2: Verify reachable file content**

Use `git rev-list --objects --all` plus `git cat-file` to scan every reachable text blob, excluding paths under `docs/school_materials/**` from content assertions. Check current-tree files separately with `rg`.

- [ ] **Step 3: Verify the teaching-material invariant**

Compare the pre-rewrite and post-rewrite blob IDs for each teaching-material path at every ref where the path existed. All IDs must match exactly.

- [ ] **Step 4: Run repository checks available on this machine**

Run:

```bash
git fsck --full --no-reflogs
git diff --check
bash -n scripts/init-db.sh
```

Run Maven compile/tests if `mvn` is available; otherwise record that Maven is unavailable and do not claim a Maven build passed.

### Task 7: Publish the rewritten refs and verify the remote

**Files:**
- Update remote: `origin/main` and all existing version tags

- [ ] **Step 1: Save the post-rewrite local refs**

Record the rewritten `main` and tag SHAs, then compare them with `git ls-remote --heads --tags origin` before publishing.

- [ ] **Step 2: Force-update only the authorized rewritten refs**

Use `--force-with-lease` for `main` against the recorded old remote SHA and force-update the rewritten version tags. Do not create, merge, or delete unrelated branches.

- [ ] **Step 3: Verify the remote and report limitations**

Run `git ls-remote --heads --tags origin` again and repeat the reachable-history scans against the rewritten local refs. Report any GitHub branch-protection rejection immediately. State that old objects can remain in forks, PR refs, caches, backups, or existing clones and that all historical service credentials must be rotated/revoked.
