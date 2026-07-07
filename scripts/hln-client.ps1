$env:JAVA_HOME = "C:\codetool\java"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
mvn -f client/pom.xml javafx:run
