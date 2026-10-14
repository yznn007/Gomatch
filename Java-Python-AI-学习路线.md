# Java + Python AI 开发学习流程

> **核心逻辑**：Java 打工程底座，Python 做 AI 能力，Vibe Coding 加速，最终在 Java 项目里集成 AI。

---

## 总览

| 阶段 | 主题 | 周期 | Python 侧同步内容 |
|------|------|------|------------------|
| 第一阶段 | 语言与工具地基 | 1-2 个月 | 基础语法（1-2 周） |
| 第二阶段 | Web 与数据层 | 1-2 个月 | NumPy / Pandas |
| 第三阶段 | 并发与底层 | 1-2 个月 | scikit-learn 入门 |
| 第四阶段 | 前端与交互 | 2-3 周 | — |
| 第五阶段 | 部署与中间件 | 1-2 个月 | — |
| 第六阶段 | 微服务与实时通信 | 1-2 个月 | — |
| 第七阶段 | AI 集成（核心汇合点） | 1-2 个月 | LangChain / LangGraph |

---

## 第一阶段：语言与工具地基（1-2 个月）

**技术栈线路**：JavaSE → Maven → Git → MySQL

- **JavaSE**：语法、OOP、集合、异常、IO、泛型、反射。**集合框架必须吃透。**
- **MySQL**：索引、事务、Explain 调优。
- **Maven / Git**：边用边学，熟练常用命令。
- **Python 基础（同步，1-2 周）**：列表、字典、函数、类。

### 项目

| 项目 | 技术 | 训练目标 |
|------|------|---------|
| 控制台版图书管理系统 | 纯 JavaSE，集合存储数据 | 练 OOP 和集合操作 |
| 命令行 TODO 工具 | Python | 练基础语法 |

---

## 第二阶段：Web 与数据层（1-2 个月）

**技术栈线路**：JavaWeb → MyBatis → Spring MVC → Spring → Spring Boot

- **JavaWeb**：理解 Servlet / Tomcat，**跳过 JSP**。
- **Spring**：IOC 和 AOP 必须能讲清原理。
- **Spring Boot**：自动配置、REST API、整合 MyBatis。
- **Python 侧**：NumPy、Pandas 基础。

### 项目

| 项目 | 技术 | 训练目标 |
|------|------|---------|
| 个人博客系统（后端） | Spring Boot + MyBatis + MySQL | 文章 CRUD、分类、标签；REST API 形式，用 Postman 测试 |
| 数据清洗小脚本 | Python + Pandas | 处理 CSV 数据，做统计和可视化 |

---

## 第三阶段：并发与底层（1-2 个月）

**技术栈线路**：JUC → JVM

- **JUC**：线程池参数设置、锁、并发容器。
- **JVM**：内存结构、GC、调优。
- **Python 侧**：scikit-learn 入门，理解分类、回归、聚类。

### 项目

| 项目 | 技术 | 训练目标 |
|------|------|---------|
| 高并发秒杀系统（简化版） | Spring Boot + Redis + JUC | 练线程池、锁、缓存 |
| 鸢尾花分类 Demo | Python + scikit-learn | 跑通机器学习完整流程 |

---

## 第四阶段：前端与交互（2-3 周）

**技术栈线路**：HTML → CSS → JavaScript → Ajax → Vue → TypeScript

- **HTML / CSS / JS**：能看懂、能改样式、能写交互。
- **Vue**：组合式 API、Vue Router、Pinia、Element Plus。
- **TypeScript**：能看懂，会用基本类型。

### 项目

| 项目 | 技术 | 训练目标 |
|------|------|---------|
| 博客系统前端 | Vue 3 + Element Plus + Axios | 对接第二阶段的博客后端，实现完整前后端联调 |

---

## 第五阶段：部署与中间件（1-2 个月）

**技术栈线路**：Linux → Docker → Nginx → Redis → MQ → ES

- **Linux**：文件操作、文本处理、进程管理、网络排查。
- **Docker**：镜像、容器、Dockerfile、Docker Compose。
- **Nginx**：反向代理、负载均衡、动静分离。
- **Redis**：缓存三大问题、分布式锁。
- **MQ**：RabbitMQ 或 RocketMQ 入门。
- **ES**：倒排索引、DSL 查询、聚合分析。

### 项目

| 项目 | 技术 | 训练目标 |
|------|------|---------|
| 博客系统完整部署 | Dockerfile + Docker Compose 编排 Spring Boot + MySQL + Redis + Nginx | 部署到云服务器 |
| 商品搜索功能 | ES | MySQL 同步数据到 ES，实现全文检索 + 聚合统计 |
| 异步通知系统 | MQ | 用 MQ 实现评论后异步发邮件 / 站内信 |

---

## 第六阶段：微服务与实时通信（1-2 个月）

**技术栈线路**：Spring Cloud → SSE / WebSocket

- **Spring Cloud**：Nacos + OpenFeign + Gateway 三件套，再补 Sentinel、配置中心。
- **SSE**：优先学，Spring Boot `SseEmitter` 半天跑通。
- **WebSocket**：双向场景才上。

### 项目

| 项目 | 技术 | 训练目标 |
|------|------|---------|
| 微服务版博客系统 | Nacos + OpenFeign + Gateway | 拆成用户服务、文章服务、评论服务，注册发现 + 统一入口 |
| 实时消息推送 | SSE / WebSocket | 用 SSE 实现"新评论通知"，或用 WebSocket 实现简单聊天室 |

---

## 第七阶段：AI 集成（核心汇合点，1-2 个月）

**技术栈线路**：LangChain4j / Spring AI Alibaba → Python LangChain 参考 → Neo4j → GraphRAG

- **Java 侧**：用 LangChain4j 或 Spring AI Alibaba 在 Spring Boot 中调用大模型。
- **Python 侧**：看懂 LangChain 架构，LangGraph 当设计参考。
- **Neo4j**：Cypher 基础和图建模。

### 项目

| 项目 | 技术 | 训练目标 |
|------|------|---------|
| 博客 AI 问答助手 | RAG 模块 | 文档加载 → 文本分割 → 向量检索 → 生成回答 |
| 知识图谱问答 | Neo4j + 向量库 + GraphRAG | 存储博客文章关系，实现多跳推理问答 |
| 智能代码审查助手 | Python LangChain Agent | 调用大模型审查 Java 代码，输出改进建议 |

---

## 贯穿始终的两条线

### 四大件（持续学习）

| 科目 | 优先级 | 说明 |
|------|--------|------|
| 数据结构与算法 | 最高 | 刷题贯穿全程 |
| 计算机网络 | 很高 | HTTP / TCP 天天用 |
| 操作系统 | 高 | 重点并发和 IO |
| 计算机组成原理 | 了解即可 | — |
| 软件工程 | 实践中补 | 设计模式、UML、单元测试、CI/CD |

### Vibe Coding（全程加速）

- 用 Cursor / Claude Code 生成脚手架和样板代码。
- **核心原则：生成后必读 diff，看不懂就不用。**
- 把省下的时间花在**理解原理和架构设计**上。

---

## 项目演进主线

```
图书管理系统
   → 个人博客系统
   → 博客完整部署
   → 微服务版博客
   → 博客 AI 问答助手
```

**这条主线的好处**：每个阶段都在同一个项目上叠加新能力，而不是每次从零开始。你能清楚看到项目如何从**单体到微服务**、从**无缓存到有缓存**、从**纯后端到 AI 集成**。最终这个项目就是你面试时最有说服力的作品。
