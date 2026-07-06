$ScoopJdk = "$HOME\scoop\apps\openjdk21\current"
if (Test-Path $ScoopJdk) {
    $env:JAVA_HOME = $ScoopJdk
    $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
}
mvn -f "$PSScriptRoot/../server/pom.xml" exec:java -Dexec.mainClass="com.gomatch.server.ServerMain" "-Dmaven.repo.local=$HOME\.m2\repository"
