$env:JAVA_HOME = "D:\C_Work\Java"
$env:MAVEN_HOME = "D:\Tools\maven\apache-maven-3.9.6"
$env:Path = "$env:MAVEN_HOME\bin;$env:JAVA_HOME\bin;$env:Path"

Write-Host "[INFO] Starting Gomatch Server..." -ForegroundColor Green
Write-Host "[INFO] JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Yellow
Write-Host "[INFO] MAVEN_HOME = $env:MAVEN_HOME" -ForegroundColor Yellow

# Set console code page to UTF-8 to avoid garbled Chinese output
chcp 65001 | Out-Null
mvn -f "$PSScriptRoot/../server/pom.xml" exec:java "-Dexec.mainClass=com.gomatch.server.ServerMain" "-Dfile.encoding=UTF-8"