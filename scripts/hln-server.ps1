$env:JAVA_HOME = "C:\codetool\java"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
mvn -f server/pom.xml compile exec:java "-Dexec.mainClass=com.gomatch.server.ServerMain"
