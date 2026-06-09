# Phase 4: 终极实战 —— AI 开发助理 实施计划

> **Goal:** 把所有学到的串联成一个完整项目：用户提需求 → Agent 分析 → 设计 → 生成代码 → 创建 GitHub PR

**Tech Stack:** React, Spring Boot, FastAPI, LangGraph, SSE, GitHub API, Docker Compose

**Architecture:**

```
用户浏览器 (React)
    ├── REST ──→ Spring Boot (业务层) ──→ MySQL
    └── SSE ───→ FastAPI (Agent层)
                    ├── LangGraph (3 Agent 编排)
                    ├── GitHub API (分支/提交/PR)
                    └── LLM (OpenAI/DeepSeek)
```

---

## 项目结构

```
ai-dev-assistant/
├── frontend/                    # React + React Flow
│   └── src/
│       ├── pages/Dashboard.tsx, ProjectDetail.tsx
│       ├── components/RequireChat.tsx, PlanTimeline.tsx, CodePreview.tsx
│       └── api/client.ts       # SSE 流式客户端
├── agent-service/               # FastAPI
│   └── app/
│       ├── agents/requirement_agent.py, architect_agent.py, coder_agent.py
│       ├── workflow/dev_workflow.py  # LangGraph 编排
│       ├── tools/github_tools.py    # GitHub Git Data API
│       └── sse/stream.py           # SSE 流式推送
├── business-service/            # Spring Boot
│   └── src/main/java/com/you/
│       ├── controller/ProjectController, RequirementController, GenerationController
│       ├── model/Project, Requirement, GenerationRecord
│       └── service/...
├── docker-compose.yml           # 前端 + 两个后端 + MySQL
└── README.md
```

---

## Day 1-2: Spring Boot 业务层

**数据库设计**:

```sql
-- 项目表
projects(id, name, description, github_repo, tech_stack JSON, created_at)

-- 需求表
requirements(id, project_id FK, title, description, status ENUM(PENDING,ANALYZING,DESIGNING,CODING,DONE,FAILED), priority ENUM(LOW,MEDIUM,HIGH), created_at)

-- 生成记录表
generation_records(id, requirement_id FK, type ENUM(ANALYSIS,ARCHITECTURE,CODE,PR), content LONGTEXT, metadata JSON, status ENUM(RUNNING,COMPLETED,FAILED), created_at)
```

**API**:
```
POST /api/projects                     创建项目
GET  /api/projects                     项目列表
GET  /api/projects/{id}                项目详情

POST /api/projects/{id}/requirements   提交需求
GET  /api/requirements/{id}            需求详情（含生成记录）
POST /api/requirements/{id}/execute    触发 Agent 执行（调用 FastAPI）
```

---

## Day 3-4: FastAPI Agent 核心

**三个 Agent 角色**:

```python
# 需求分析 Agent — 产品经理视角
requirement_agent_prompt = """你是资深产品经理。分析需求：
1. 核心功能点  2. 用户故事  3. 验收标准  4. 技术风险"""

# 架构设计 Agent — 架构师视角
architect_agent_prompt = """你是资深架构师。设计方案：
1. 技术选型  2. 模块划分  3. API 设计（OpenAPI）  4. 数据库设计  5. Mermaid 类图"""

# 代码生成 Agent — 开发视角
coder_agent_prompt = """你是资深开发工程师。生成代码：
1. 可运行的完整代码  2. 单元测试  3. 遵循 SOLID 原则"""
```

**LangGraph 工作流**:

```
[需求分析] → 成功？→ [架构设计] → 成功？→ [代码生成] → 成功？→ [创建 PR]
    ↓ 失败           ↓ 失败          ↓ 失败
[错误处理]        [错误处理]       [错误处理]
```

**GitHub 操作工具链**（不 clone，直接用 API）:

```
POST /repos/{repo}/git/blobs     → 创建文件 blob
POST /repos/{repo}/git/trees      → 创建目录树
POST /repos/{repo}/git/commits    → 创建 commit
PATCH /repos/{repo}/git/refs/...  → 更新分支
POST /repos/{repo}/pulls          → 创建 PR
```

---

## Day 5-6: GitHub 集成

**完整流程**:

```python
async def create_pr_from_code(repo, code_files, requirement):
    # 1. 创建分支
    branch = f"ai/req-{requirement['id']}"
    sha = get_main_sha(repo)
    create_branch(repo, branch, sha)

    # 2. 逐个提交文件
    for file in code_files:
        blob_sha = create_blob(repo, file.content)
        tree_sha = create_tree(repo, base_tree_sha, file.path, blob_sha)

    # 3. 创建 commit
    commit_sha = create_commit(repo, tree_sha, f"AI: {requirement['title']}")

    # 4. 更新分支引用
    update_ref(repo, f"heads/{branch}", commit_sha)

    # 5. 创建 PR
    pr = create_pr(repo, requirement['title'], analysis_summary, branch)
    return pr['html_url']
```

---

## Day 7-8: React 前端

**三个核心页面**:

**Dashboard**: 项目卡片列表 + 新建项目弹窗

**ProjectDetail**: 需求列表 + "新建需求"按钮 → 跳转到需求详情

**RequirementDetail**（最关键的页面）:

```
┌──────────────────────────────────────────────────────┐
│  需求: 用户登录功能（手机号验证码）    状态: 🟢 已完成    │
├──────────────────────────────────────────────────────┤
│  ┌─ PlanTimeline（SSE 实时更新）─────────────────┐   │
│  │  ✅ 需求分析完成 (2.3s)  Token: 1,234          │   │
│  │  ✅ 架构设计完成 (3.1s)  3 API + 2 张表        │   │
│  │  ✅ 代码生成完成 (15.7s) 8 文件 1,247 行        │   │
│  │  ✅ PR 已创建: github.com/xxx/pull/42           │   │
│  └──────────────────────────────────────────────┘  │   │
│  ┌─ CodePreview（Tab 切换）───────────────────────┐   │
│  │  [AuthController] [AuthService] [更多...]       │   │
│  │  @RestController / @PostMapping("/send-code")  │   │
│  └──────────────────────────────────────────────┘  │   │
└──────────────────────────────────────────────────────┘
```

**SSE 流式客户端**:

```typescript
async function executeRequirement(projectId, requirementId, desc, onEvent) {
    const response = await fetch(`/api/requirements/${requirementId}/stream`, {
        method: "POST", headers: {"Content-Type": "application/json"},
        body: JSON.stringify({description: desc})
    });
    const reader = response.body!.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    while (true) {
        const {done, value} = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, {stream: true});
        const lines = buffer.split("\n");
        buffer = lines.pop() || "";
        for (const line of lines) {
            if (line.startsWith("data: ")) {
                onEvent(JSON.parse(line.slice(6)));
            }
        }
    }
}
```

---

## Day 9-10: 联调 + 部署 + README

### Day 9: 端到端联调
1. 前端提交需求 → Spring Boot 创建记录 → 调 FastAPI → SSE 流式推送 → 前端实时展示
2. 异常测试：需求不清晰、Agent 超时、GitHub API 失败
3. 日志：每个请求一个 trace_id，贯穿所有服务

### Day 10: Docker Compose + README

```yaml
services:
  frontend:       {build: ./frontend, ports: ["3000:3000"]}
  agent-service:  {build: ./agent-service, ports: ["8000:8000"], env_file: .env}
  business-service:
    build: ./business-service
    ports: ["8080:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/dev_assistant
    depends_on: [mysql]
  mysql:
    image: mysql:8.0
    environment: {MYSQL_ROOT_PASSWORD: ..., MYSQL_DATABASE: dev_assistant}
```

**README 架构图**:

```
用户 → React → REST → Spring Boot → MySQL
             ↘ SSE ↗
           FastAPI → LangGraph → LLM
                      └→ GitHub API → PR
```

---

## Phase 4 结业检查

- [ ] 前端提交需求 → Agent 执行 → SSE 流式展示 → PR 自动创建
- [ ] 异常流程正确处理
- [ ] 日志 trace_id 贯穿所有服务
- [ ] Docker Compose 一键启动
- [ ] README 包含架构图 + 本地运行步骤

---

## 全路线成果总结

```
Phase 1（2周）     Phase 2（3周）        Phase 3（2周）     Phase 4（2周）

Spring Boot ──→  项目1: 代码审查   ──→  手搓 LLM 层   ──→  AI 开发助理
FastAPI     ──→  项目2: 智能客服   ──→  手搓 Tool 系统──→  全栈联调
              ──→  项目3: 第二大脑 ──→  手搓 Agent 循环──→  部署上线
              ──→  项目4: 工作流平台──→  手搓 Memory   ──→  简历作品

产出: 6 个可运行项目 + 1 个自研框架 + 1 个旗舰全栈项目

简历可写:
- AI Dev Assistant: Spring Boot + FastAPI + LangGraph + React + Docker
- Multi-Agent Customer Service: CrewAI + ChromaDB + WebSocket + Spring Boot
- AI Second Brain: RAG + Neo4j + Hybrid Search + Cross-Encoder
- Agent Workflow Platform: LangGraph + React Flow + Multi-Model Adapter
- Code Review Agent: LangChain + GitHub Webhook + Docker
- Mini-Agent Framework: Python + OpenAI API + tiktoken

技能标签:
LangChain / LangGraph / CrewAI / RAG / Agent / Tool Calling /
Spring Boot / FastAPI / React / Docker / Neo4j / ChromaDB / SSE
