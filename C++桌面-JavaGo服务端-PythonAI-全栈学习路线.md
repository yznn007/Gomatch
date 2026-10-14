# C++ 桌面 + Java/Go 服务端 + Python AI 全栈学习路线（C++ 零基础版）

> **核心定位**：C++ 做桌面 C/S 客户端，Java/Go 做 B/S 服务端，Python 做 AI 能力层。从零开始，四语言协作，最终形成"桌面端 + Web 端 + AI 能力"的完整技术画像。

---

## 零、先认清全局

### 四语言分工

| 语言 | 定位 | 核心产出 |
|------|------|---------|
| **C++** | 桌面 C/S 客户端 | 高性能本地应用、Qt 界面 |
| **Java** | B/S 服务端主力 | 企业级业务、微服务 |
| **Go** | 高并发 / 云原生补位 | 网关、基础设施、CLI |
| **Python** | AI 能力层 | 模型实验、推理服务 |

> **学习原则**：先精通一门，再横向扩展。**不要四语言同时从零学，会崩。**

**推荐顺序**：Java（主语言，先建立工程思维）→ C++（桌面客户端）→ Python（AI）→ Go（补充）。

---

## 第一阶段：编程入门与工具地基（2-3 个月）

**技术栈线路**：JavaSE → Maven → Git → MySQL → Python 基础

- **JavaSE**：语法、OOP、集合、异常、IO、泛型、反射。**集合框架必须吃透。**
- **MySQL**：索引、事务、Explain 调优。
- **Maven / Git**：边用边学，熟练常用命令。
- **Python 基础（同步，1-2 周）**：列表、字典、函数、类。

> **为什么先学 Java 而不是 C++**：Java 语法更友好、生态更成熟、报错更清晰，适合零基础建立编程思维。C++ 的指针和内存管理留到后面。

### 项目

- 控制台版图书管理系统（JavaSE，集合存储）
- 命令行 TODO 工具（Python，练基础语法）

---

## 第二阶段：Web 与数据层（2 个月）

**技术栈线路**：JavaWeb → MyBatis → Spring MVC → Spring → Spring Boot

- **JavaWeb**：理解 Servlet / Tomcat，**跳过 JSP**。从"单机"到"服务端"的关键转折。
- **Spring**：IOC 和 AOP 必须能讲清原理。
- **Spring Boot**：自动配置、REST API、整合 MyBatis。
- **Python 侧**：NumPy、Pandas 基础。

### 项目

- 个人博客系统（后端）：Spring Boot + MyBatis + MySQL，REST API 形式，Postman 测试。
- 数据清洗脚本：Python + Pandas，处理 CSV 数据。

---

## 第三阶段：并发与底层（2 个月）

**技术栈线路**：JUC → JVM → Go 基础

- **JUC**：线程池参数设置、锁、并发容器。
- **JVM**：内存结构、GC、调优。
- **Go**：基础语法 + goroutine / channel / select，和 Java 并发对比着学。
- **Python 侧**：scikit-learn 入门，理解分类、回归、聚类。

### 项目

- 高并发秒杀系统（简化版）：Spring Boot + Redis + JUC。
- Go 版 HTTP 服务：对比 Spring Boot 实现差异。
- 鸢尾花分类 Demo：Python + scikit-learn。

---

## 第四阶段：前端与交互（2-3 周）

**技术栈线路**：HTML → CSS → JavaScript → Ajax → Vue → TypeScript

- **HTML / CSS / JS**：能看懂、能改样式、能写交互。
- **Vue**：组合式 API、Vue Router、Pinia、Element Plus。
- **TypeScript**：能看懂，会用基本类型。

### 项目

- 博客系统前端：Vue 3 + Element Plus + Axios，完整前后端联调。

---

## 第五阶段：C++ 与桌面开发（2-3 个月，零基础重点）

**技术栈线路**：C++ 基础 → Qt → C/S 通信

### 5.1 C++ 基础（1 个月）

- 语法、指针、引用、内存管理（`new` / `delete`）、RAII
- 面向对象：类、继承、多态、虚函数
- STL：`vector`、`map`、`string`、智能指针
- 和 Java 对比学：Java 有 GC，C++ 手动管理；Java 接口显式实现，C++ 多继承

### 5.2 Qt 框架（1 个月）

- **Qt Widgets**：窗口、布局、控件、信号与槽
- **Qt Designer**：可视化拖拽 UI
- **QNetworkAccessManager**：HTTP 请求，对接服务端
- **QJsonDocument**：JSON 解析

### 5.3 C/S 架构通信（2-3 周）

- **HTTP / REST**：C++ 客户端调用 Java 服务端 API
- **WebSocket**：实时双向通信
- **gRPC（选学）**：高性能 RPC

### 项目

- Qt 桌面版图书管理：纯本地，练 C++ 和 UI
- Qt 桌面 + Java 服务端（C/S 架构）：客户端走 REST API 读写数据，练 C/S 通信

---

## 第六阶段：部署与中间件（2 个月）

**技术栈线路**：Linux → Docker → Nginx → Redis → MQ → ES

- **Linux**：文件操作、文本处理、进程管理、网络排查。**零基础必须补的短板。**
- **Docker**：镜像、容器、Dockerfile、Docker Compose。
- **Nginx**：反向代理、负载均衡、动静分离。
- **Redis**：缓存三大问题、分布式锁。
- **MQ**：RabbitMQ 或 RocketMQ 入门。
- **ES**：倒排索引、DSL 查询、聚合分析。

### 项目

- 博客系统完整部署：Docker Compose 编排 Spring Boot + MySQL + Redis + Nginx。
- 商品搜索功能：ES 全文检索 + 聚合统计。
- 异步通知系统：MQ 实现评论后异步发邮件 / 站内信。
- Go 写 CLI 工具：日志分析、健康检查。

---

## 第七阶段：微服务与实时通信（2 个月）

**技术栈线路**：Spring Cloud → SSE / WebSocket

- **Spring Cloud**：Nacos + OpenFeign + Gateway 三件套，再补 Sentinel、配置中心。
- **SSE**：优先学，Spring Boot `SseEmitter` 半天跑通。
- **WebSocket**：双向场景才上。

### 项目

- 微服务版博客系统：用户 / 文章 / 评论服务，Nacos 注册发现，OpenFeign 调用，Gateway 统一入口。
- 实时消息推送：SSE 实现"新评论通知"，或 WebSocket 实现聊天室。

---

## 第八阶段：AI 集成（2 个月，核心汇合点）

**技术栈线路**：LangChain4j / Spring AI Alibaba → Python LangChain 参考 → Neo4j → GraphRAG

- **Java 侧**：用 LangChain4j 或 Spring AI Alibaba 在 Spring Boot 中调用大模型。
- **Python 侧**：看懂 LangChain 架构，LangGraph 当设计参考。
- **Go 侧**：LangChain Go / Ollama / 高性能向量检索服务。
- **Neo4j**：Cypher 基础和图建模。

### 项目

- 博客 AI 问答助手：RAG 模块，文档加载 → 文本分割 → 向量检索 → 生成回答。
- 知识图谱问答：Neo4j + 向量库做 GraphRAG，多跳推理问答。
- Go 版高并发 AI API 层。

---

## 第九阶段：C++ 桌面 AI 客户端（1-2 个月，收官）

**技术栈线路**：Qt + AI 能力集成

- **桌面 AI 客户端**：Qt 客户端集成 AI 问答，调用 Java/Go 服务端 AI API。
- **C/S + B/S 混合**：C++ 桌面做高性能本地处理，Java/Go 做云端服务，Python 做 AI 分析。
- **完整闭环**：C++ 桌面 + Java/Go 服务端 + Python AI。

### 项目

- 桌面 AI 助手：Qt 客户端 + Python AI 服务，本地推理或远程调用。
- C/S + B/S 混合系统：桌面端高性能处理 + Web 端管理 + AI 分析。

---

## 项目开发路线（一条主线演进）

### 阶段一：单机工具

- 控制台图书管理（Java）→ Qt 桌面版图书管理（C++）

### 阶段二：C/S 架构

- Qt 客户端 + Spring Boot 后端，走 REST API

### 阶段三：加入 Web 端（B/S）

- Vue 前端 + Spring Boot 后端，和 C++ 客户端共用同一套 API

### 阶段四：完整部署

- Docker Compose 编排 Spring Boot + MySQL + Redis + Nginx + ES + MQ

### 阶段五：微服务化

- 拆成用户 / 文章 / 评论服务，Nacos + Gateway
- C++ 客户端和 Vue 前端都通过 Gateway 访问

### 阶段六：AI 集成

- RAG 问答模块（Java 侧 LangChain4j）
- Python 做模型实验，Go 做高并发 AI API 层
- Neo4j 做知识图谱增强

### 阶段七：桌面 AI 客户端

- Qt 客户端集成 AI 问答
- 形成"C++ 桌面 + Java/Go 服务端 + Python AI"完整闭环

---

## 最终技能画像

| 层次 | 技术 | 定位 |
|------|------|------|
| 桌面端 | C++ / Qt | 高性能本地客户端、C/S 架构 |
| Web 服务端 | Java / Spring 全家桶 | 企业级业务、微服务主力 |
| 高并发服务 | Go | 云原生、网关、基础设施 |
| AI 能力 | Python | 模型实验、数据处理、推理服务 |
| AI 集成 | LangChain4j / Spring AI Alibaba | Java 项目内嵌 AI |
| 前端 | Vue + TypeScript | B/S 客户端 |
| 数据层 | MySQL / Redis / ES / Neo4j | 关系、缓存、搜索、图 |
| 部署 | Linux / Docker | 服务端运行环境 |

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

## 给 C++ 零基础的三条建议

1. **先学 Java，别先啃 C++**：Java 语法友好、生态成熟，适合零基础建立编程思维。C++ 的指针和内存管理留到第五阶段。
2. **C++ 和 Java 对比学**：Java 有 GC，C++ 手动管理；Java 接口显式实现，C++ 多继承。对比着学，理解更深。
3. **别四语言同时学**：Java 主攻 → C++ 桌面 → Python AI → Go 补位。一门一门来，每门都要有项目产出。

---

> **一句话节奏**：Java 打工程底座，C++ 做桌面客户端，Python 做 AI，Go 做高并发补位，Vue 做 Web 端。四语言协作，项目一条主线从单机工具演进到全栈 AI 系统。
