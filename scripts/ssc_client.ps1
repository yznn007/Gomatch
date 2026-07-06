$ScoopJdk = "$HOME\scoop\apps\openjdk21\current"
if (Test-Path $ScoopJdk) {
    $env:JAVA_HOME = $ScoopJdk
    $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
}
mvn -f "$PSScriptRoot/../client/pom.xml" javafx:run "-Dmaven.repo.local=$HOME\.m2\repository"
