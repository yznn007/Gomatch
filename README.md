# Gomatch

网络五子棋联机对战平台。基于 **JavaFX + Socket 多线程服务端 + JDBC** 架构，支持玩家实时对弈、房间管理、聊天、观战、战绩排行等功能。

## 快速启动

```powershell
# 启动客户端
.\run-client.ps1
```

## 项目文件与文档索引

| 文件/目录 | 说明 |
|------|------|
| [docs/requirements.md](docs/requirements.md) | 需求与系统设计文档 |
| [docs/DEV_LOG.md](docs/DEV_LOG.md) | 详细开发过程与更新记录 |
| [docs/DEV_STANDARD.md](docs/DEV_STANDARD.md) | 项目团队开发规范 (Git Commit/分支标准) |
| [AGENTS.md](AGENTS.md) | AI Agent 自动化开发上手指引与约定 |
| [sql/init.sql](sql/init.sql) | MySQL 数据库初始化建表脚本 |
| [run-client.ps1](run-client.ps1) | 客户端一键启动脚本 (自动指定 JDK 21 运行) |


