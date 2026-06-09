# Agent 学习路线设计文档

> **设计日期**: 2026-06-08
> **目标用户**: 编程入门者，掌握 Java/Python 基础语法，想学后端，想要效率高、能落地、能写进简历
> **总周期**: 约 9 周

## 设计原则

1. **先做出东西，再深挖原理** — 每个阶段产出可运行的代码
2. **每个项目可写进简历** — 有具体技术栈、业务价值、量化指标
3. **混合路线** — Agent 层用 Python（生态最成熟），业务后端用 Java Spring Boot（企业级标准）
4. **学完能回答"Agent 到底是什么"** — 不仅仅是调 API

---

## 总体架构

```
Phase 1（2周）      Phase 2（3周）          Phase 3（2周）      Phase 4（2周）
后端基座           Agent 应用层            Agent 底层原理      终极实战
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Spring Boot  ──→  项目1: AI 代码审查    ──→  手搓 LLM 层    ──→  AI 开发助理
FastAPI      ──→  项目2: 智能客服      ──→  手搓 Tool 系统 ──→  全栈联调
              ──→  项目3: AI 第二大脑  ──→  手搓 Agent 循环──→  部署上线
              ──→  项目4: 工作流平台   ──→  手搓 Memory    ──→  简历作品集
```

**产出**: 6 个可运行项目 + 1 个自研框架 + 1 个旗舰全栈项目

---

## Phase 1: 后端基座（2 周）

### 目标
Spring Boot 和 FastAPI 都能写出 CRUD API，为后续 Agent 项目提供后端支撑。

### 前置认知（3 个概念）
1. **什么是后端**: 一个持续运行的程序，监听端口，等待 HTTP 请求，处理完返回数据
2. **什么是 API**: 函数调用，但是通过网络。本地 `taskService.create(task)` 对应远程 `POST /api/tasks` + JSON
3. **为什么两个框架都学**: Spring Boot = 编译型/强类型/企业级；FastAPI = 解释型/类型提示/轻量快速。Agent 层用 FastAPI，业务层用 Spring Boot

### 第 1 周: Spring Boot 速通

| 天 | 主题 | 关键产出 |
|---|------|---------|
| 1 | 项目结构、依赖注入、@RestController | 跑起 Hello World API |
| 2 | RESTful CRUD API 设计 | Task Manager 完整 CRUD（内存存储） |
| 3 | @Valid 校验 + @ControllerAdvice 全局异常处理 | 统一错误响应格式 |
| 4 | JPA + H2 数据库持久化 | 数据落库，H2 Console 可视化 |
| 5 | MockMvc 集成测试 + mvn package 打包 | 可执行 jar 包 |

### 第 2 周: FastAPI 速通

| 天 | 主题 | 关键产出 |
|---|------|---------|
| 1 | 项目结构、路径参数、uvicorn 启动 | Hello World + 自动 Swagger 文档 |
| 2 | Pydantic 模型、请求体、响应模型 | 用 Pydantic 重写 Task Manager |
| 3 | async/await 异步编程基础 | 理解什么时候用 async（Agent 应用大量需要） |
| 4 | SQLAlchemy + SQLite + Alembic 迁移 | 数据持久化 |
| 5 | pytest 测试 + Dockerfile 部署 | Docker 镜像 |

### Phase 1 结业检查
- [ ] GET/POST/PUT/DELETE API 完整实现
- [ ] 输入校验 + 统一错误响应
- [ ] 数据持久化（重启不丢失）
- [ ] 自动 API 文档（Swagger）
- [ ] 集成测试覆盖
- [ ] 单命令启动 / Docker 部署

### 核心概念对比

| 概念 | Spring Boot | FastAPI |
|------|-------------|---------|
| 启动方式 | @SpringBootApplication main | uvicorn main:app --reload |
| 路由定义 | @GetMapping("/tasks") | @app.get("/tasks") |
| 请求体验证 | @Valid + Bean Validation | Pydantic BaseModel |
| 依赖注入 | @Autowired / 构造器注入 | Depends() |
| ORM | JPA / Hibernate | SQLAlchemy |
| 自动文档 | 需 SpringDoc | 自动 /docs Swagger |
| 异步 | Servlet 3.0+ | Python async/await 原生 |

---

## Phase 2: Agent 应用层（3 周）

### 前置认知（5 个概念）

1. **LLM 调用 ≠ Agent**: Agent 有自主决策循环（观察→思考→行动→观察），LLM 调用只是单次问答
2. **Tool Calling = Agent 的手**: LLM 输出特殊 JSON（tool_calls），程序解析后真正执行函数，结果回传给 LLM
3. **RAG = 给 LLM 外挂硬盘**: 文档→分块→向量嵌入→存向量库→用户提问时语义搜索→相关文档+问题一起发给 LLM
4. **多 Agent = 分工协作**: 复杂任务拆成不同角色，每个 Agent 只干一件事，通过消息传递协作
5. **LangGraph = 工作流编排引擎**: StateGraph 建模 Agent 执行过程为有向图（节点=执行步骤，边=流转规则）

### 项目清单

| 项目 | 天数 | 简历标题 | 核心技术栈 |
|------|------|---------|-----------|
| AI 代码审查 Agent | 5天 | 基于 LangChain 的自动化代码审查系统 | FastAPI, LangChain, GitHub Webhook, Docker |
| 多 Agent 智能客服 | 5天 | 基于 CrewAI 的多 Agent 协作智能客服 | CrewAI, RAG, ChromaDB, Spring Boot, WebSocket |
| AI 第二大脑 | 5天 | 融合 RAG 与知识图谱的个人知识管理 | RAG, Neo4j, 混合检索, Cross-Encoder, Gradio |
| Agent 工作流平台 | 5天 | Agent 工作流编排平台 | LangGraph, Spring Boot, FastAPI, React Flow, SSE |

### Phase 2 结业检查
- [ ] Agent 和普通 LLM 调用的本质区别（ReAct 循环）
- [ ] Tool Calling 完整流程：Schema 生成 → LLM 决策 → 执行 → 结果回传
- [ ] RAG 三个核心步骤：文档加载分块 → 向量嵌入 → 相似度检索
- [ ] 混合检索为什么比纯向量检索好（RRF 融合原理）
- [ ] 知识图谱和向量数据库各自适用场景
- [ ] CrewAI Process.sequential vs Process.hierarchical
- [ ] LangGraph StateGraph 如何建模 Agent 工作流
- [ ] SSE 和 WebSocket 的区别及适用场景
- [ ] LLM Provider 多模型适配器设计
- [ ] Docker Compose 多服务编排

---

## Phase 3: Agent 底层原理（2 周）

### 目标
手搓一个能跑的 Agent 框架，彻底理解每一行代码为什么这么写。

### Week 1: 拆解 Agent 核心机制

| 天 | 模块 | 核心实现 |
|---|------|---------|
| 1 | LLM 调用抽象层 | 封装 OpenAI/DeepSeek API，统一 Message/ToolCall/LLMResponse 类型 |
| 2 | 工具系统 | @tool 装饰器、ToolRegistry、自动 JSON Schema 生成（get_type_hints + inspect.signature） |
| 3 | Agent 核心循环 | ReAct 模式：while 循环 + LLM 调用 + 工具执行 + 结果回传 + 终止条件判断 |
| 4 | Memory 系统 | 短期记忆（TokenCounter + MemoryManager 智能裁剪）、长期记忆（SummaryMemory 摘要存储） |
| 5 | 规划器 | Plan & Execute 模式：先让 LLM 生成计划 → 逐步执行 → 每步验证 |

### Week 2: 高级特性 + 对比反思

| 天 | 模块 | 核心实现 |
|---|------|---------|
| 6 | 错误恢复 | 工具参数错误自动重试、执行超时降级、无结果换策略 |
| 7 | 多 Agent 路由 | 极简 Router：关键词匹配 + LLM 语义匹配两层路由 |
| 8 | 对比分析 | 用自研框架 vs LangChain 实现同一任务，写对比笔记 |
| 9-10 | 框架完善 + 文档 | pytest 测试、支持 Anthropic Claude、README + API 文档 |

### Phase 3 产出: mini-agent-framework

```
core/
├── llm.py         # LLM 抽象层
├── agent.py       # Agent 核心循环（ReAct）
├── tools.py       # 工具系统（注册、Schema、执行）
├── memory.py      # 记忆系统（短期 + 长期）
├── planner.py     # 规划器（Plan & Execute）
└── types.py       # 核心类型定义
examples/
├── 01_basic_chat.py      # 纯对话
├── 02_single_tool.py     # 单工具调用
├── 03_multi_tools.py     # 多工具自主选择
├── 04_memory.py          # 带记忆多轮对话
├── 05_plan_execute.py    # 规划+执行
└── 06_error_recovery.py  # 错误恢复
tests/
└── ...
```

### Phase 3 结业检查
- [ ] 能画出 Agent ReAct 循环流程图
- [ ] 理解 tool_calls 在 LLM API 响应中的原始 JSON 格式
- [ ] 理解 JSON Schema 如何从 Python 函数自动生成
- [ ] 理解 token 计数原理和记忆裁剪策略
- [ ] 能说出 Plan & Execute 和 ReAct 各自适用场景
- [ ] 能说出自研框架和 LangChain 的 3 个关键区别

---

## Phase 4: 终极实战 —— AI 开发助理（2 周）

### 目标
把所有学到的东西串联成一个完整项目：用户提需求 → Agent 自动分析 → 设计方案 → 生成代码 → 创建 GitHub PR

### 项目结构

```
ai-dev-assistant/
├── frontend/              # React 前端（Dashboard, RequireChat, CodePreview）
├── agent-service/         # FastAPI Agent 核心（3 个 Agent + LangGraph 编排）
├── business-service/      # Spring Boot 业务层（项目/需求/记录 CRUD）
└── docker-compose.yml
```

### 日程

| 天 | 模块 | 关键产出 |
|---|------|---------|
| 1-2 | Spring Boot 项目管理后端 | 项目/需求/生成记录 3 张表，完整 CRUD API |
| 3-4 | FastAPI Agent 核心服务 | 需求分析/架构设计/代码生成 3 个 Agent + LangGraph 工作流 |
| 5-6 | GitHub 集成 | Git Data API 操作（创建分支、提交文件、创建 PR） |
| 7-8 | React 前端 | SSE 流式展示、PlanTimeline、CodePreview |
| 9-10 | 联调 + 部署 + README | Docker Compose 一键部署，端到端测试，架构图 |

### 工作流

```
[用户需求] → [需求分析 Agent] → [架构设计 Agent] → [代码生成 Agent] → [GitHub PR]
                  ↓ 失败              ↓ 失败              ↓ 失败
              [错误处理]           [错误处理]           [错误处理]
```

### Phase 4 结业检查
- [ ] 前端提交需求 → 后端记录 → Agent 执行 → SSE 流式展示 → PR 自动创建
- [ ] 异常流程：需求描述不清晰、Agent 超时、GitHub API 失败
- [ ] 日志追踪：每个请求一个 trace_id 贯穿所有服务
- [ ] Docker Compose 一键启动所有服务
- [ ] README 包含架构图、本地运行步骤、API 文档

---

## 技能覆盖矩阵

| 能力 | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
|------|---------|---------|---------|---------|
| Spring Boot | ✓ | ✓ | | ✓ |
| FastAPI | ✓ | ✓ | | ✓ |
| LangChain | | ✓ | | ✓ |
| Tool Calling | | ✓ | ✓ | ✓ |
| RAG | | ✓ | | |
| ChromaDB | | ✓ | | |
| CrewAI | | ✓ | | |
| LangGraph | | ✓ | | ✓ |
| 知识图谱 (Neo4j) | | ✓ | | |
| 混合检索 | | ✓ | | |
| React | | ✓ | | ✓ |
| SSE/WebSocket | | ✓ | | ✓ |
| Docker | ✓ | ✓ | | ✓ |
| LLM 底层原理 | | | ✓ | |
| Token 管理 | | | ✓ | |
| Agent 循环实现 | | | ✓ | |
| Plan & Execute | | | ✓ | |
| GitHub API | | ✓ | | ✓ |

## 简历输出建议

### 项目经历
1. **AI 开发助理平台** - 全栈，LangGraph 编排多 Agent，需求→代码自动化。`Spring Boot` `FastAPI` `LangGraph` `React` `Docker`
2. **多 Agent 智能客服系统** - 3-Agent 协作，RAG + 工单升级。`CrewAI` `ChromaDB` `WebSocket` `Spring Boot`
3. **个人 AI 第二大脑** - 向量检索 + 知识图谱混合检索。`RAG` `Neo4j` `混合检索` `Cross-Encoder`
4. **Mini-Agent 框架** - 从零实现 ReAct/Tool/Memory。`Python` `OpenAI API` `tiktoken`
5. **AI 代码审查 Agent** - GitHub Webhook 自动审查。`LangChain` `FastAPI` `Docker`

### 技能标签
`LangChain` `LangGraph` `CrewAI` `RAG` `Agent 开发` `Tool Calling` `Spring Boot` `FastAPI` `React` `Docker` `Neo4j` `ChromaDB` `SSE/WebSocket`
