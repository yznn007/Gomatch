[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^\d+\.\d+\.\d+(-[\w.]+)?$')]
    [string]$Version
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $repoRoot

Write-Host ">>> 更新 pom.xml 版本到 $Version ..." -ForegroundColor Yellow
mvn versions:set "-DnewVersion=$Version" "-DgenerateBackupPoms=false" -q
if ($LASTEXITCODE -ne 0) { throw "versions:set 失败" }

Write-Host ">>> 提交版本更新 ..." -ForegroundColor Yellow
git add -A
git commit -m "chore(release): bump version to $Version" --allow-empty 2>$null

Write-Host ">>> 合并到 main 并打 tag v$Version ..." -ForegroundColor Yellow
$currentBranch = git branch --show-current
git checkout main
git merge $currentBranch --no-edit
git tag -a "v$Version" -m "v$Version"

git checkout $currentBranch
git merge main --no-edit

git push origin main --tags
git push origin $currentBranch

Write-Host ">>> v$Version 发布完成" -ForegroundColor Green
Write-Host ">>> 若需构建 MSI，执行: .\scripts\package_client_msi.ps1 -Version $Version" -ForegroundColor Cyan
