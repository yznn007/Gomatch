# Gomatch — AGENTS.md

## 项目本质

学生实训项目，JDK 21 + Swing + Socket 多线程服务端 + JDBC + MySQL。Maven 多模块。

## 架构要点

- **三模块**：`client/`（Swing 界面）、`server/`（多线程 Socket）、`common/`（协议常量），common 被打进 client 和 server 的胖 JAR 中
- **通信**：自定义文本协议 `COMMAND|param1|param2`，TCP 长连接
- **并发**：每连接一个 `ClientHandler` 线程，`Room` 对象内 synchronized 隔离
- **五连判定**：四方向（横/竖/左斜/右斜）从落子点向两侧计数

## 文档维护

每次完成开发任务后，**必须主动维护以下文档**：

| 文档 | 维护时机 | 维护内容 |
|------|----------|----------|
| `docs/DEV_LOG.md` | 每完成一个任务 | 更新任务清单状态、追加日志行、填过程记录 |
| `docs/requirements.md` | 需求/设计发生变更时 | 同步更新功能描述、协议、数据库设计等 |
| `README.md` | 项目信息变化时 | 保持简介和文档索引准确 |
| `AGENTS.md` | 命令/架构/流程变化时 | 确保新 Agent 能正确上手 |

维护完成后提交并推送至 GitHub。

## 分支工作流

- 日常开发在 `dev` 分支上进行
- `main` 分支保持稳定，仅阶段完成时合并
- 详细规范见 `docs/DEV_STANDARD.md`
