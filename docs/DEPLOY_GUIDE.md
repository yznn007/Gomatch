# Gomatch 服务端部署指南

本指南详细介绍了如何将 `Gomatch` 游戏服务端打包，并在 Linux 服务器上运行、配置以及通过守护进程进行管理的完整流程。

---

## 1. 本地打包（Jar 包构建）

Gomatch 项目使用 Maven 多模块架构。在进行打包前，请确保本地配置了 JDK 21，并在项目根目录下执行以下命令：

```bash
# 1. 编译并安装公共 common 模块（如果 common 有修改）
mvn clean install -pl common -am -DskipTests

# 2. 打包 server 服务端模块（-am 会自动打包依赖的 common 和 database 模块）
mvn clean package -pl server -am -DskipTests
```

构建成功后，在 `server/target/` 目录下会生成以下文件：
- **`gomatch-server-1.0.0.jar`**：包含所有依赖的 shaded JAR（Fat JAR），**这是我们需要传输到服务器上运行的唯一包**。

---

## 2. 服务器环境准备

在服务器上运行服务端，需要满足以下运行环境：

### 2.1 安装 JDK 21
服务端基于 JDK 21 编写，服务器上必须安装 JDK 21 或更高版本的 JRE/JDK。

以 Ubuntu/Debian 为例安装 OpenJDK 21：
```bash
sudo apt update
sudo apt install openjdk-21-jre-headless -y
```
验证安装：
```bash
java -version
```

### 2.2 数据库与 Redis 准备
1. **MySQL 8.x**：
   - 创建数据库 `gomatch`，字符集推荐 `utf8mb4`。
   - 导入初始 SQL 脚本：将本地项目的 `sql/init.sql` 以及 `sql/migrations/` 下的迁移脚本（如有）传输到服务器，并在数据库中执行。
   - 创建专用的数据库用户（如 `gomatch`），并赋予对 `gomatch` 数据库的全部权限。
2. **Redis**：
   - 安装并启动 Redis 服务。
   - 如果 Redis 与游戏服务器在同一台机器上运行，可限制 Redis 仅监听 `127.0.0.1` 且免密启动（或者配置密码）。如果外网访问，**必须**配置强密码。

### 2.3 防火墙与网络端口
- 服务端 TCP 默认监听端口为 **`8888`**。
- 请在云服务器控制台安全组以及服务器防火墙（如 `ufw` 或 `firewalld`）中，放行 TCP `8888` 端口。

---

## 3. 配置管理与环境变量

为保证安全性，**切勿**直接修改 Jar 包内打包好的 `database.properties`，也不要把生产数据库密码提交到代码仓库。

服务端已集成环境变量自动覆盖机制（参见 [EnvironmentConfig](file:///server/src/main/java/com/gomatch/server/config/EnvironmentConfig.java)）。你可以在启动服务端时，通过设置以下环境变量来重写默认配置：

| 对应配置项 | 环境变量名 | 说明 |
| :--- | :--- | :--- |
| `mysql.url` | `GOMATCH_DB_URL` | MySQL 连接串，例：`jdbc:mysql://<IP>:<PORT>/gomatch?...` |
| `mysql.username` | `GOMATCH_DB_USERNAME` | MySQL 用户名 |
| `mysql.password` | `GOMATCH_DB_PASSWORD` | MySQL 密码 |
| `redis.host` | `GOMATCH_REDIS_HOST` | Redis 服务器地址，例如 `127.0.0.1` |
| `redis.port` | `GOMATCH_REDIS_PORT` | Redis 端口，默认 `6379` |
| `redis.password` | `GOMATCH_REDIS_PASSWORD`| Redis 密码 |
| `oss.access-key-id` | `OSS_ACCESS_KEY_ID` | 阿里云 OSS AccessKey ID（如需启用头像/音乐等存储） |
| `oss.access-key-secret`| `OSS_ACCESS_KEY_SECRET` | 阿里云 OSS AccessKey Secret |

---

## 4. 部署与运行方式

### 方式一：终端直接启动（仅用于临时测试）

将 Jar 包上传到服务器（例如目录 `/opt/gomatch-server`），然后在该目录下运行：

```bash
# 设置环境变量并运行 Jar 包（Linux 示例）
export GOMATCH_DB_URL="jdbc:mysql://127.0.0.1:3306/gomatch?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false"
export GOMATCH_DB_USERNAME="gomatch"
export GOMATCH_DB_PASSWORD="your_secure_password"
export GOMATCH_REDIS_HOST="127.0.0.1"
export GOMATCH_REDIS_PORT="6379"
export GOMATCH_REDIS_PASSWORD="your_redis_password"

java -Dfile.encoding=UTF-8 -jar gomatch-server-1.0.0.jar
```

---

### 方式二：使用 Systemd 守护进程运行（推荐生产部署）

为了使服务端在后台持久运行、开机自启、崩溃自动重启，并由系统统一管理日志，可以使用 `systemd`。

1. **创建服务配置文件**：
   在服务器上创建并编辑 `/etc/systemd/system/gomatch-server.service` 文件：
   ```bash
   sudo nano /etc/systemd/system/gomatch-server.service
   ```

2. **写入以下配置**（请根据实际路径和密码进行修改）：
   ```ini
   [Unit]
   Description=Gomatch Game Server
   After=network.target mysql.service redis.service

   [Service]
   Type=simple
   User=root
   WorkingDirectory=/opt/gomatch-server
   ExecStart=/usr/bin/java -Dfile.encoding=UTF-8 -jar /opt/gomatch-server/gomatch-server-1.0.0.jar
   Restart=always
   RestartSec=5

   # 环境变量配置，用以覆盖数据库与 Redis 默认连接参数
   Environment="GOMATCH_DB_URL=jdbc:mysql://127.0.0.1:3306/gomatch?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false"
   Environment="GOMATCH_DB_USERNAME=gomatch"
   Environment="GOMATCH_DB_PASSWORD=your_secure_password"
   Environment="GOMATCH_REDIS_HOST=127.0.0.1"
   Environment="GOMATCH_REDIS_PORT=6379"
   Environment="GOMATCH_REDIS_PASSWORD=your_redis_password"
   
   # 如需 OSS 存储，请补充以下环境变量
   # Environment="OSS_ACCESS_KEY_ID=your_oss_id"
   # Environment="OSS_ACCESS_KEY_SECRET=your_oss_secret"

   [Install]
   WantedBy=multi-user.target
   ```

3. **管理系统服务**：
   ```bash
   # 重新加载 Systemd 配置
   sudo systemctl daemon-reload

   # 启用开机自启并立即启动服务
   sudo systemctl enable --now gomatch-server

   # 查看服务状态
   sudo systemctl status gomatch-server

   # 停止服务
   sudo systemctl stop gomatch-server

   # 重启服务
   sudo systemctl restart gomatch-server
   ```

4. **查看运行日志**：
   Systemd 会自动收集输出到 Standard Out/Error 的日志，可使用 `journalctl` 查看：
   ```bash
   # 实时查看最后 100 行日志
   sudo journalctl -u gomatch-server -n 100 -f
   ```

---

### 方式三：使用 Docker 容器化部署（优雅独立）

如果你喜欢容器化，可以使用 Docker 极速部署并统一管理。

1. **编写 `Dockerfile`**：
   在 `/opt/gomatch-server` 或项目根目录创建 `Dockerfile`：
   ```dockerfile
   FROM openjdk:21-slim
   WORKDIR /app
   COPY gomatch-server-1.0.0.jar app.jar
   EXPOSE 8888
   ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
   ```

2. **构建镜像**：
   ```bash
   docker build -t gomatch-server:1.0.0 .
   ```

3. **使用 `docker-compose.yml` 联合启动 MySQL + Redis + GameServer**：
   可以在服务器上编写 `docker-compose.yml`：
   ```yaml
   version: '3.8'

   services:
     mysql:
       image: mysql:8.0
       container_name: gomatch-mysql
       environment:
         MYSQL_DATABASE: gomatch
         MYSQL_USER: gomatch
         MYSQL_PASSWORD: your_secure_db_password
         MYSQL_ROOT_PASSWORD: your_root_password
       ports:
         - "3306:3306"
       volumes:
         - mysql-data:/var/lib/mysql
         # 如果把 init.sql 放在当前目录下的 init-sql/ 目录中，可以挂载自动初始化建表
         - ./init-sql:/docker-entrypoint-initdb.d
       restart: always

     redis:
       image: redis:7.0-alpine
       container_name: gomatch-redis
       command: redis-server --requirepass your_secure_redis_password
       ports:
         - "6379:6379"
       volumes:
         - redis-data:/data
       restart: always

     server:
       image: gomatch-server:1.0.0
       container_name: gomatch-game-server
       ports:
         - "8888:8888"
       depends_on:
         - mysql
         - redis
       environment:
         - GOMATCH_DB_URL=jdbc:mysql://mysql:3306/gomatch?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
         - GOMATCH_DB_USERNAME=gomatch
         - GOMATCH_DB_PASSWORD=your_secure_db_password
         - GOMATCH_REDIS_HOST=redis
         - GOMATCH_REDIS_PORT=6379
         - GOMATCH_REDIS_PASSWORD=your_secure_redis_password
       restart: always

   volumes:
     mysql-data:
     redis-data:
   ```

4. **一键启动**：
   ```bash
   docker compose up -d
   ```
