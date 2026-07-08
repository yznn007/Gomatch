# Gomatch 服务端启动指南

## 1. 环境要求

### 1.1 必备软件

| 软件 | 版本要求 | 用途 |
|------|----------|------|
| JDK | 21 LTS | 运行环境，JavaFX 21 依赖 |
| Maven | 3.9+ | 项目构建工具 |
| MySQL | 8.x | 数据库存储 |
| Redis | 7.x（必需） | 缓存、会话持久化、排行榜 |

### 1.2 环境变量配置

确保 `JAVA_HOME` 已指向 JDK 21，`mvn` 可通过命令行执行。验证配置：

```powershell
java -version
# 输出示例：openjdk version "21" 2023-09-19

mvn -v
# 输出示例：Apache Maven 3.9.15
```

---

## 2. 数据库配置

### 2.1 初始化数据库

执行 SQL 脚本创建数据库和表：

```powershell
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source sql/init.sql
```

### 2.2 配置数据库连接

修改 `database/src/main/resources/database.properties` 文件：

```properties
# MySQL 连接配置
db.url=jdbc:mysql://localhost:3306/gomatch?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
db.username=${GOMATCH_DB_USERNAME}
db.password=${GOMATCH_DB_PASSWORD}
db.driver=com.mysql.cj.jdbc.Driver

# HikariCP 连接池配置
db.pool.maxSize=20
db.pool.minIdle=5
db.pool.connectionTimeout=30000
db.pool.idleTimeout=600000
db.pool.maxLifetime=1800000

# Redis 配置（必需，服务端启动前必须可连接）
redis.host=localhost
redis.port=6379
redis.password=${GOMATCH_REDIS_PASSWORD}
redis.timeout=3000
redis.database=0
```

---

## 3. 编译项目

### 3.1 安装公共模块

首次启动或 common 模块变更后，需要安装到本地 Maven 仓库：

```powershell
mvn install -pl common -am
```

### 3.2 编译服务端

```powershell
mvn clean compile -pl server -am
```

---

## 4. 启动服务端

### 4.1 使用启动脚本（推荐）

```powershell
# 方式一：直接运行脚本
.\\scripts\\ssc_server.ps1

# 方式二：PowerShell 命令行
powershell -ExecutionPolicy Bypass -File scripts/ssc_server.ps1
```

### 4.2 手动启动

```powershell
# 启动服务端
mvn -f server/pom.xml exec:java -Dexec.mainClass="com.gomatch.server.ServerMain"
```

### 4.3 启动成功标志

服务端启动成功后，控制台会输出以下信息：

```
[main] INFO com.gomatch.server.ServerMain - Gomatch Server starting...
[main] INFO com.gomatch.server.ServerMain - Server listening on port 8888
[main] INFO com.gomatch.server.ServerMain - Database connected successfully
[main] INFO com.gomatch.server.redis.RedisManager - Redis connected: localhost:6379
[main] INFO com.gomatch.server.ServerMain - Gomatch Server started successfully!
```

---

## 5. 启动客户端

### 5.1 使用启动脚本（推荐）

```powershell
.\\scripts\\ssc_client.ps1
```

### 5.2 手动启动

```powershell
mvn -f client/pom.xml javafx:run
```

---

## 6. 常用命令

```powershell
# 编译整个项目
mvn clean compile

# 安装所有模块到本地仓库
mvn install -DskipTests

# 运行服务端测试
mvn test -pl server

# 打包服务端（生成 jar）
mvn package -pl server -am -DskipTests

# 清理
mvn clean
```

---

## 7. 常见问题

### 7.1 Java 版本问题

**错误信息**：
```
Error: JavaFX runtime components are missing, and are required to run this application.
```

**解决方案**：确保系统 `JAVA_HOME` 已指向 JDK 21，或直接使用 `scripts/scripts/ssc_client.ps1` 启动脚本（已内置自动查找逻辑）。

### 7.2 数据库连接失败

**错误信息**：
```
java.sql.SQLException: Access denied for user 'root'@'localhost'
```

**解决方案**：
1. 确认 MySQL 服务已启动
2. 检查 `database.properties` 中的用户名和密码
3. 确认数据库 `gomatch` 已创建

### 7.3 Redis 连接失败

**错误信息**：
```
Failed to initialize Redis: Connection refused
```

**解决方案**：
1. 启动 Redis 服务：`redis-server`
2. 启动 Redis 后重新启动服务端

### 7.4 端口占用

**错误信息**：
```
java.net.BindException: Address already in use
```

**解决方案**：
```powershell
# 查找占用端口的进程
netstat -ano | findstr :8888

# 终止进程（替换 PID）
taskkill /PID <PID> /F
```

### 7.5 Maven 依赖下载慢

**解决方案**：
```powershell
# 使用阿里云镜像（修改 ~/.m2/settings.xml）
<mirrors>
    <mirror>
        <id>aliyunmaven</id>
        <mirrorOf>central</mirrorOf>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
</mirrors>
```

---

## 8. 服务端配置说明

### 8.1 默认端口

| 服务 | 端口 | 配置位置 |
|------|------|----------|
| 游戏服务端 | 8888 | ServerMain.java |
| MySQL | 3306 | database.properties |
| Redis | 6379 | database.properties |

### 8.2 定时任务

| 任务 | 间隔 | 说明 |
|------|------|------|
| 会话清理 | 1 分钟 | 清理过期会话 |
| 房间持久化 | 1 分钟 | 同步内存房间到数据库 |
| 排行榜刷新 | 5 分钟 | 刷新物化表和 Redis |
| 过期房间清理 | 1 分钟 | 删除 30 分钟前结束的房间 |

---

## 9. 调试模式

### 9.1 设置日志级别

修改 `server/src/main/resources/logback.xml`：

```xml
<root level="DEBUG">
    <appender-ref ref="STDOUT"/>
</root>
```

### 9.2 远程调试

```powershell
mvn -f server/pom.xml exec:java \
    -Dexec.mainClass="com.gomatch.server.ServerMain" \
    -Dexec.args="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

---

## 10. 测试账号

| 用户名 | 密码 | 角色 | 积分 |
|--------|------|------|------|
| admin | password | ADMIN | 9999 |
| player1 | password | PLAYER | 1000 |
| player2 | password | PLAYER | 1000 |

---

**文档版本**：v1.0
**最后更新**：2026-07-06

