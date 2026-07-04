$env:JAVA_HOME = "C:\Users\31816\scoop\apps\openjdk21\current"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
mvn -f client/pom.xml javafx:run