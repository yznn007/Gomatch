$env:JAVA_HOME = "D:\C_Work\Java"
$env:MAVEN_HOME = "D:\Tools\maven\apache-maven-3.9.6"
$env:Path = "$env:MAVEN_HOME\bin;$env:JAVA_HOME\bin;$env:Path"

$projectRoot = Split-Path $PSScriptRoot -Parent

chcp 65001 | Out-Null

Write-Host "[BUILD] Installing common module..." -ForegroundColor Yellow
Push-Location $projectRoot
mvn install -pl common -am -DskipTests
Pop-Location

Write-Host "[START] Starting server..." -ForegroundColor Green
Push-Location $projectRoot
mvn -f server/pom.xml exec:java "-Dexec.mainClass=com.gomatch.server.ServerMain" "-Dfile.encoding=UTF-8"
Pop-Location