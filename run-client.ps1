$env:JAVA_HOME = "D:\develop\Java\jdk21"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
mvn -f client/pom.xml javafx:run "-Dmaven.repo.local=C:\Users\23364\.m2\repository"