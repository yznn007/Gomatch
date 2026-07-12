[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^\d+\.\d+\.\d+(-[\w.]+)?$')]
    [string]$Version,
    [string]$OutputDirectory = 'client/target/installer'
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $repoRoot

# MSI ProductVersion 只接受纯数字 A.B.C，提取前三位数字部分
$msiVersion = if ($Version -match '^(\d+\.\d+\.\d+)') { $Matches[1] } else { $Version }

foreach ($command in 'mvn.cmd', 'jpackage.exe', 'candle.exe', 'light.exe') {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "缺少 $command；请安装 Maven、JDK 21 与 WiX Toolset 3。"
    }
}

# 从 Maven 本地仓库定位 JavaFX 模块 JAR
$m2Repo = Join-Path $env:USERPROFILE '.m2\repository'
if (-not (Test-Path (Join-Path $m2Repo 'org\openjfx'))) {
    $raw = & mvn.cmd help:evaluate -Dexpression=settings.localRepository -q -DforceStdout 2>&1
    $candidate = (@($raw) -split '\r?\n' | Where-Object { $_ -match '^[A-Za-z]:[\\/]' -and $_ -notmatch '[<>]' } | Select-Object -First 1)
    if ($candidate) { $m2Repo = $candidate.Trim() }
}
$m2Repo = $m2Repo.Trim()
$fxPath = Join-Path $m2Repo 'org\openjfx'
$fxJars = Get-ChildItem -Path $fxPath -Recurse -Filter "javafx-*-21-win.jar" | Where-Object {
    $_.Name -match '^javafx-(base|controls|fxml|graphics|media)-21-win\.jar$'
} | ForEach-Object { $_.FullName }
$modulePath = ($fxJars -join ';')

$inputDirectory = Join-Path $repoRoot 'client/target'
$mainJarFile = Get-ChildItem -LiteralPath $inputDirectory -Filter "gomatch-client-*.jar" |
    Where-Object { $_.Name -notmatch 'original' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $mainJarFile) {
    throw "未在 $inputDirectory 找到客户端 JAR，请检查 Maven 构建结果。"
}
$mainJar = $mainJarFile.Name
$mainJarPath = $mainJarFile.FullName

if (-not $fxJars -or $fxJars.Count -lt 5) {
    throw "未找到全部 JavaFX 平台 JAR，检查 Maven 本地仓库：$fxPath"
}

$destination = [IO.Path]::GetFullPath((Join-Path $repoRoot $OutputDirectory))
New-Item -ItemType Directory -Force -Path $destination | Out-Null

& jpackage.exe `
    --type msi `
    --name Gomatch `
    --app-version $msiVersion `
    --vendor Gomatch `
    --input $inputDirectory `
    --main-jar $mainJar `
    --main-class com.gomatch.client.ClientMain `
    --module-path $modulePath `
    --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.graphics,javafx.base,java.desktop,java.logging,java.naming,java.sql,java.xml `
    --dest $destination `
    --win-menu `
    --win-menu-group Gomatch `
    --win-shortcut `
    --win-dir-chooser `
    --win-upgrade-uuid 93c1f0fb-248a-4b8a-a1f1-c423e8039f03
if ($LASTEXITCODE -ne 0) {
    throw "MSI 打包失败，退出码：$LASTEXITCODE"
}

$msi = Get-ChildItem -LiteralPath $destination -Filter '*.msi' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($null -eq $msi) {
    throw "jpackage 未在 $destination 生成 MSI。"
}

Write-Output "MSI_PATH=$($msi.FullName)"