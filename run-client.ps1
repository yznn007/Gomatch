<#
.SYNOPSIS
    Start Gomatch Client
.PARAMETER SkipBuild
    Skip compiling and installing dependency modules
.EXAMPLE
    .\run-client.ps1
    .\run-client.ps1 -SkipBuild
#>
param([switch]$SkipBuild)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# ===== detect JDK 21 =====
$jdkPaths = @(
    "D:\develop\Java\jdk21",
    "$env:JAVA_HOME",
    "C:\Users\$env:USERNAME\scoop\apps\openjdk21\current",
    "C:\Program Files\Java\jdk-21"
)
$jdkHome = $null
foreach ($p in $jdkPaths) {
    if ($p -and (Test-Path "$p\bin\java.exe")) { $jdkHome = $p; break }
}
if (-not $jdkHome) {
    Write-Host "[ERROR] JDK 21 not found" -ForegroundColor Red
    exit 1
}
$env:JAVA_HOME = $jdkHome
$env:Path = "$jdkHome\bin;$env:Path"
Write-Host "[OK] JAVA_HOME = $jdkHome" -ForegroundColor Green

# ===== build =====
if (-not $SkipBuild) {
    Write-Host "[BUILD] Installing common module..." -ForegroundColor Yellow
    Push-Location $scriptDir
    try {
        mvn install -pl common -am -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "Build failed" }
    } finally { Pop-Location }
}

# ===== run =====
Write-Host "[START] Starting client (localhost:8888)..." -ForegroundColor Green
Push-Location $scriptDir
mvn -f client/pom.xml javafx:run
Pop-Location
