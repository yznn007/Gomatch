$env:JAVA_HOME = "D:\develop\Java\jdk21"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
mvn -f server/pom.xml exec:java -Dexec.mainClass="com.gomatch.server.ServerMain" "-Dmaven.repo.local=C:\Users\23364\.m2\repository"