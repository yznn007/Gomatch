# Gomatch 服务端部署指南

## 构建

需确保 JDK 21 已配置。在项目根目录执行：

```bash
mvn clean install -pl common -am -DskipTests
mvn clean package -pl server -am -DskipTests
```

将 `server/target/gomatch-server-2.0.2.jar` 部署到服务端，并准备 MySQL 8.x 与 Redis 7.x。执行 `sql/init.sql` 及需要的迁移脚本；SQL 初始化用户维持脚本原有定义。

## 运行凭据

应用不在资源文件中保存数据库、Redis 或 OSS 凭据。将项目的 `.env.example` 复制为部署目录中的 `.env`，在该文件中填写：

- `GOMATCH_DB_URL`、`GOMATCH_DB_USERNAME`、`GOMATCH_DB_PASSWORD`
- `GOMATCH_REDIS_HOST`、`GOMATCH_REDIS_PORT`、`GOMATCH_REDIS_DATABASE`，以及 Redis 启用认证时的 `GOMATCH_REDIS_PASSWORD`
- 启用 OSS 存储时填写 `OSS_ENDPOINT`、`OSS_BUCKET`、`OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`

进程环境变量优先于 `.env`，因此 Docker、CI 和 systemd 可以通过其各自的安全凭据机制注入同名变量。不要提交 `.env`，也不要在 systemd 单元、Compose 文件或 Markdown 中写入实际密码。

配置优先级依次为：系统环境变量、`.env`、资源文件中的非敏感默认值。空值不会覆盖较低层值；必填的 MySQL 与 Redis 主机/端口变量缺失时服务端会停止启动；未启用 Redis 认证时可将 `GOMATCH_REDIS_PASSWORD` 留空。默认 `.env` 路径为工作目录下的 `.env`；系统环境变量 `GOMATCH_ENV_FILE` 可指定其他文件路径。

## 启动

确保运行目录含有 `.env`，然后执行：

```bash
java -Dfile.encoding=UTF-8 -jar gomatch-server-2.0.2.jar
```

服务端启动时会校验 MySQL 与 Redis 的必填变量；缺失时会报告变量名而不会打印凭据。开放游戏 TCP 端口 `8888`，并限制 MySQL 与 Redis 的网络访问范围。

## 容器与守护进程

容器编排使用 `.env` 或平台 secret 注入变量；systemd 使用 `EnvironmentFile=` 指向权限受限的文件。两种方式都应避免将凭据写入版本库。