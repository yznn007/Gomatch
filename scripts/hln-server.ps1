# Auto-detect JDK 21+
$JDKCandidates = @(
    "$env:JAVA_HOME",
    "$HOME\scoop\apps\openjdk21\current"
)
$jdkFound = $false
foreach ($jdk in $JDKCandidates) {
    if ($jdk -and (Test-Path "$jdk\bin\java.exe")) {
        $env:JAVA_HOME = $jdk
        $env:Path = "$env:JAVA_HOME\bin;$env:Path"
        $jdkFound = $true
        break
    }
}
if (-not $jdkFound) {
    Write-Host "[WARN] Could not auto-detect JDK 21. Assuming java is on PATH." -ForegroundColor Yellow
}

# Auto-detect Maven
$MvnCandidates = @(
    "C:\codetool\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin",
    "$HOME\scoop\apps\maven\current\bin"
)
$mvnFound = $false
foreach ($mvnDir in $MvnCandidates) {
    if ($mvnDir -and (Test-Path "$mvnDir\mvn.cmd")) {
        $env:Path = "$mvnDir;$env:Path"
        $mvnFound = $true
        break
    }
}
if (-not $mvnFound) {
    Write-Host "[WARN] Could not auto-detect Maven. Assuming mvn is on PATH." -ForegroundColor Yellow
}

# Set console code page to UTF-8
chcp 65001 | Out-Null

# Compile common module first
Write-Host "[BUILD] Installing common module..." -ForegroundColor Yellow
mvn install -pl common -am -DskipTests -q

# Start server
Write-Host "[START] Starting server..." -ForegroundColor Green
mvn -f "$PSScriptRoot/../server/pom.xml" compile exec:java "-Dexec.mainClass=com.gomatch.server.ServerMain" "-Dfile.encoding=UTF-8"
