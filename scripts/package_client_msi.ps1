[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^\d+\.\d+\.\d+(?:[-.]\d+)?$')]
    [string]$Version,
    [string]$OutputDirectory = 'client/target/installer'
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $repoRoot

foreach ($command in 'mvn.cmd', 'jpackage.exe', 'candle.exe', 'light.exe') {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "缺少 $command；请安装 Maven、JDK 21 与 WiX Toolset 3。"
    }
}

& mvn.cmd -pl client -am package
if ($LASTEXITCODE -ne 0) {
    throw "客户端 Maven 打包失败，退出码：$LASTEXITCODE"
}

$inputDirectory = Join-Path $repoRoot 'client/target'
$mainJar = 'gomatch-client-1.0.0.jar'
$mainJarPath = Join-Path $inputDirectory $mainJar
if (-not (Test-Path $mainJarPath)) {
    throw "未找到客户端 JAR：$mainJarPath"
}

$destination = [IO.Path]::GetFullPath((Join-Path $repoRoot $OutputDirectory))
New-Item -ItemType Directory -Force -Path $destination | Out-Null

& jpackage.exe `
    --type msi `
    --name Gomatch `
    --app-version $Version `
    --vendor Gomatch `
    --input $inputDirectory `
    --main-jar $mainJar `
    --main-class com.gomatch.client.ClientMain `
    --dest $destination `
    --add-modules java.desktop,java.logging,java.naming,java.sql,java.xml `
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