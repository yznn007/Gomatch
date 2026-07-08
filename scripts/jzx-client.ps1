$env:JAVA_HOME = "D:\develop\Java\jdk21"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
# Set console code page to UTF-8 to avoid garbled Chinese output
chcp 65001 | Out-Null
mvn -f "$PSScriptRoot/../common/pom.xml" install -q "-Dmaven.repo.local=$HOME\.m2\repository"
mvn -f "$PSScriptRoot/../client/pom.xml" javafx:run "-Dmaven.repo.local=$HOME\.m2\repository" "-Dfile.encoding=UTF-8"