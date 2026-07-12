# 发布流程

## 1. 同步 pom.xml 版本号

```powershell
mvn versions:set "-DnewVersion=X.Y.Z" -DgenerateBackupPoms=false
```

该命令会同时更新根 pom 和 `common/`、`server/`、`client/` 三个子模块的版本号。

## 2. 编写版本说明

在 `docs/releases/vX.Y.Z.md` 按已有格式编写版本说明。

## 3. 提交并合并到 main

```powershell
git add -A
git commit -m "chore(release): bump version to X.Y.Z"
git checkout main
git merge dev --no-edit
```

## 4. 打 tag

```powershell
git tag -a vX.Y.Z -m "vX.Y.Z"
```

## 5. 推送

```powershell
git push origin main --tags
git push origin dev
```

## 6. 构建 MSI（可选）

```powershell
.\scripts\package_client_msi.ps1 -Version X.Y.Z
```
