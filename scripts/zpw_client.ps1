$env:JAVA_HOME = "D:\C_Work\Java"
$env:MAVEN_HOME = "D:\Tools\maven\apache-maven-3.9.6"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

$projectRoot = Split-Path $PSScriptRoot -Parent

# Set console code page to UTF-8 to avoid garbled Chinese output
chcp 65001 | Out-Null

Write-Host "[BUILD] Installing common module..." -ForegroundColor Yellow
Push-Location $projectRoot
mvn install -pl common -am -DskipTests
Pop-Location

Write-Host "[START] Starting client..." -ForegroundColor Green
Push-Location $projectRoot
mvn -f client/pom.xml javafx:run "-Dfile.encoding=UTF-8"
Pop-Location