# Gomatch

网络五子棋联机对战平台。基于 **JavaFX + Socket 多线程服务端 + JDBC** 架构，支持玩家实时对弈、房间管理、聊天、观战、战绩排行等功能。

## 快速启动

```powershell
# 启动客户端
.\run-client.ps1
```

## 🎨 极简美学设计

本项目客户端深度践行 **白色极简主义（White Minimalist Glassmorphism）** 视觉方案：
- **下划线底线控件**：全界面按钮（含主按钮、次按钮、排行榜等控制键）与文本框高度统一样式，去除四面外框与卡片阴影，仅留底部 1.5px 极简下单线描边。
- **圆角透明卡片**：采用 `8px` 圆角乳白色透明磨砂卡片，去除粗糙渐变，在轻量的暗灰色柔和投影衬托下，呈现完美的悬浮冰面质感。
- **Maple Mono CN 字体**：客户端内置集成并动态装载枫叶等宽字体（`Maple Mono NF CN`），消除系统字体依赖，实现跨设备的高清符号对齐与渲染。
- **中性板岩灰交互**：鼠标悬浮触发统一的板岩灰 Cover 遮罩高光（`rgba(148, 163, 184, 0.35)`），点击按下（armed）高亮为聚焦实白。全界面无杂色天蓝污染。

## 文档

| 文件 | 说明 |
|------|------|
| [docs/requirements.md](docs/requirements.md) | 需求文档 |
| [docs/DEV_LOG.md](docs/DEV_LOG.md) | 开发过程记录 |
| [docs/DEV_STANDARD.md](docs/DEV_STANDARD.md) | 开发规范（Commit/分支/版本） |
| [AGENTS.md](AGENTS.md) | AI Agent 上手指引 |

