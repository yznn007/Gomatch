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

# Set console code page to UTF-8
chcp 65001 | Out-Null

# Compile common module first
Write-Host "[BUILD] Installing common module..." -ForegroundColor Yellow
mvn install -pl common -am -DskipTests -q

# Start client
Write-Host "[START] Starting client..." -ForegroundColor Green
mvn -f "$PSScriptRoot/../client/pom.xml" javafx:run "-Dfile.encoding=UTF-8"
