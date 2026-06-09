# Phase 2: Agent 应用层 实施计划

> **Goal:** 用 LangChain/LangGraph/CrewAI 构建 4 个可写进简历的 Agent 应用

**Tech Stack:** Python 3.11+, FastAPI, LangChain, LangGraph, CrewAI, ChromaDB, Neo4j, React Flow, Docker

---

## 进入 Phase 2 之前：5 个核心认知

### 认知 1：LLM 调用 ≠ Agent

```
普通 LLM 调用：
  你 → "写一个排序函数" → LLM → 返回代码

Agent 调用：
  你 → "帮我管理日程" → Agent → 想了想 → 调用了 add_event 工具
                                → 想了想 → 调用了 search_events 工具
                                → 想了想 → 组织语言回答你
```

区别在于 Agent 有一个**自主决策循环**：接收任务 → 推理下一步做什么 → 调用工具 → 观察结果 → 再推理 → ... → 最终输出。

### 认知 2：Tool Calling = Agent 的手

LLM 只会产出文本，不能真的"做"任何事。Tool Calling 就是给它装上手：

```
LLM 输出不是代码，是一种特殊 JSON：
{
  "tool_calls": [{
    "id": "call_abc",
    "function": {
      "name": "search_calendar",
      "arguments": "{\"date\":\"2026-06-09\"}"
    }
  }]
}

你的程序解析 JSON → 真的调用 search_calendar("2026-06-09") → 结果发给 LLM
```

### 认知 3：RAG = 给 LLM 外挂硬盘

```
所有文档 → 切成小块 → 每块转成向量 → 存进向量数据库

用户提问时：
  问题 → 转成向量 → 在数据库里找最相似的文档块 → 相关文档 + 问题 → 发给 LLM
```

本质是**搜索 + 喂给 LLM**。"搜索"用向量相似度（语义），不是关键词匹配。

### 认知 4：多 Agent = 分工协作

```
老板 Agent："分析这个需求"
  → 研究员 Agent：查资料
  → 工程师 Agent：写代码
  → 审查员 Agent：检查代码
  → 老板 Agent：汇总输出
```

### 认知 5：LangGraph = Agent 工作流编排引擎

```
[用户输入] → [LLM 推理节点] → 条件边 →
  ├─ 需要调工具 → [工具执行节点] → 回到 [LLM 推理节点]
  └─ 不需要 → [输出节点] → 返回结果
```

---

---

## 项目 1: AI 代码审查 Agent（5 天）

**简历标题**: 基于 LangChain 的自动化代码审查系统 — GitHub Webhook 触发，自动检测安全漏洞与代码质量

**项目结构**:

```
code-review-agent/
├── app/
│   ├── main.py              # FastAPI 入口
│   ├── config.py            # 配置（GitHub Token、LLM Key）
│   ├── github/
│   │   ├── webhook.py       # 接收 GitHub Webhook 事件
│   │   └── client.py        # GitHub API 客户端
│   ├── agent/
│   │   ├── reviewer.py      # 审查 Agent（LangChain）
│   │   ├── tools.py         # 工具：读 diff、读文件、搜索代码
│   │   └── prompts.py       # 审查规则 Prompt
│   └── rules/
│       ├── sql_injection.py # SQL 注入检测
│       ├── null_pointer.py  # 空指针风险
│       ├── exception.py     # 异常处理缺失
│       └── security.py      # 硬编码密钥检测
└── tests/
```

### Day 1: GitHub Webhook + FastAPI 骨架

**原理**: GitHub 有 PR 事件时 → 发 HTTP POST 到你的服务器 → 你的服务器解析事件 → 异步启动审查流程

**任务**:

1. 创建 FastAPI 项目结构
2. 实现 `webhook.py`：

```python
import hmac
import hashlib
import json
from fastapi import APIRouter, Request, Header, BackgroundTasks

router = APIRouter()

def verify_signature(body: bytes, signature: str, secret: str) -> bool:
    """验证 GitHub Webhook 签名，防止伪造请求"""
    expected = "sha256=" + hmac.new(
        secret.encode(), body, hashlib.sha256
    ).hexdigest()
    return hmac.compare_digest(expected, signature)

@router.post("/webhook/github")
async def github_webhook(
    request: Request,
    background_tasks: BackgroundTasks,
    x_hub_signature_256: str = Header(None, alias="X-Hub-Signature-256")
):
    body = await request.body()
    if not verify_signature(body, x_hub_signature_256, GITHUB_SECRET):
        return {"error": "Invalid signature"}, 403

    event_type = request.headers.get("X-GitHub-Event")
    if event_type == "pull_request":
        payload = json.loads(body)
        if payload["action"] in ["opened", "synchronize"]:
            background_tasks.add_task(review_pr, payload)

    return {"status": "ok"}
```

3. 用 ngrok 暴露本地端口：`ngrok http 8000`，配置到 GitHub Webhook 设置

### Day 2: LangChain Agent + 审查 Prompt

**原理**: System Prompt 定义 Agent 的"人设"——资深代码审查专家，遵守特定规则。

**任务**: 创建 `reviewer.py`

```python
from langchain_openai import ChatOpenAI
from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain_core.prompts import ChatPromptTemplate

llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.1)

system_prompt = """你是资深代码审查专家。审查规则：
1. 安全：SQL 注入、XSS、硬编码密钥、路径遍历
2. 空值：可能为 null 未判空
3. 异常：catch 块为空、无 finally 释放资源
4. 质量：过长函数（>50 行）、过深嵌套（>3 层）、魔法数字

严重程度：🔴 Critical / 🟡 Warning / 🔵 Suggestion
输出格式：## 审查总结 → ## 发现的问题 → ### 🔴/🟡/🔵 问题N
"""

prompt = ChatPromptTemplate.from_messages([
    ("system", system_prompt),
    ("human", "{input}"),
    ("placeholder", "{agent_scratchpad}"),
])

agent = create_tool_calling_agent(llm, tools, prompt)
executor = AgentExecutor(agent=agent, tools=tools, verbose=True)
```

**关键**: `temperature=0.1` — 代码审查要稳定确定性，不是创意。

### Day 3: 审查工具实现

**原理**: 给 Agent 工具让它主动获取信息，而不仅仅分析你喂给它的 diff。

**任务**: 创建 `tools.py`

```python
from langchain.tools import tool
import requests

@tool
def get_pr_diff(repo: str, pr_number: int) -> str:
    """获取 PR 的文件变更。repo 格式 'owner/repo'"""
    url = f"https://api.github.com/repos/{repo}/pulls/{pr_number}/files"
    headers = {"Authorization": f"Bearer {GITHUB_TOKEN}"}
    resp = requests.get(url, headers=headers)
    files = resp.json()
    return "\n\n".join([f"--- {f['filename']} ---\n{f.get('patch', '')}" for f in files])

@tool
def get_file_content(repo: str, path: str, ref: str) -> str:
    """获取仓库中某文件的完整内容"""
    url = f"https://api.github.com/repos/{repo}/contents/{path}"
    headers = {"Authorization": f"Bearer {GITHUB_TOKEN}"}
    resp = requests.get(url, headers=headers, params={"ref": ref})
    import base64
    return base64.b64decode(resp.json()["content"]).decode()

@tool
def search_similar_code(repo: str, pattern: str) -> str:
    """在仓库中搜索相似代码，检查同类问题是否多处存在"""
    # GitHub Code Search API
    ...
```

**理解 JSON Schema 自动生成**: LangChain 读函数的类型提示和 docstring，自动生成 `{"name":"get_pr_diff","parameters":{...}}`，塞进 LLM 的 system prompt。

### Day 4: 审查报告 + 自动评论

**原理**: Agent 输出结构化 Markdown 报告 → 调用 GitHub API 发布 PR Review。

**任务**:

```python
async def review_pr(payload: dict):
    repo = payload["repository"]["full_name"]
    pr_number = payload["pull_request"]["number"]
    
    result = executor.invoke({
        "input": f"审查 {repo} PR #{pr_number}"
    })
    
    # 发布 Comment 到 PR
    github_client.create_review(
        repo=repo, pr_number=pr_number,
        body=result["output"], event="COMMENT"
    )
```

### Day 5: Docker 部署 + README

**任务**:
1. 写 Dockerfile
2. 写 docker-compose.yml
3. README：Mermaid 架构图 + 本地运行步骤 + 审查效果截图

---

## 项目 2: 多 Agent 智能客服系统（5 天）

**简历标题**: 基于 CrewAI 的 3-Agent 协作智能客服 — RAG 知识库 + 自动升级工单

**项目结构**:

```
multi-agent-customer/
├── agent-service/app/
│   ├── main.py
│   ├── agents/
│   │   ├── triage.py         # 分流 Agent
│   │   ├── answer.py         # 解答 Agent（RAG）
│   │   └── escalation.py     # 升级 Agent
│   ├── crew/
│   │   └── customer_crew.py  # CrewAI 编排
│   ├── knowledge/
│   │   ├── loader.py         # 知识库加载
│   │   └── vector_store.py   # ChromaDB
│   └── websocket/
│       └── handler.py
├── business-service/         # Spring Boot 工单系统
└── docker-compose.yml
```

### Day 1: 三个 Agent 角色设计

**原理**: 每个 Agent 的 system prompt 定义它的职责边界。CrewAI 的 `Agent` 类封装了 role/goal/backstory/tools。

**任务**: 创建三个 Agent

```python
# 分流 Agent
triage_agent = Agent(
    role="客服分流专员",
    goal="快速准确分类问题：产品使用 → 解答Agent / Bug → 收集信息后转解答 / 投诉 → 直接升级",
    backstory="经验丰富的客服分流专员...",
    llm=llm, verbose=True
)

# 解答 Agent（RAG 加持）
answer_agent = Agent(
    role="产品专家",
    goal="基于知识库提供准确解答。找不到答案时明确告知并建议升级。",
    tools=[search_knowledge_base],
    llm=llm, verbose=True
)

# 升级 Agent
escalation_agent = Agent(
    role="升级处理专员",
    goal="总结问题 + 整理已尝试方案 + 判断紧急度 + 生成工单",
    tools=[create_ticket],
    llm=llm, verbose=True
)
```

### Day 2: ChromaDB 向量知识库（RAG 核心）

**原理**: 文档 → 分块 → 嵌入向量 → 存 ChromaDB → 用户提问 → 语义搜索 → 相关文档喂给 LLM

**任务**: 创建 `loader.py` 和 `vector_store.py`

```python
from langchain_community.document_loaders import TextLoader, UnstructuredMarkdownLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_openai import OpenAIEmbeddings
from langchain_chroma import Chroma

# 1. 加载文档
loaders = [TextLoader("knowledge_docs/faq.txt"), UnstructuredMarkdownLoader("knowledge_docs/product.md")]
docs = []
for loader in loaders:
    docs.extend(loader.load())

# 2. 分块（chunk）
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=500, chunk_overlap=50,
    separators=["\n\n", "\n", "。", ".", " ", ""]
)
chunks = text_splitter.split_documents(docs)

# 3. 向量化 + 存库
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
vector_store = Chroma.from_documents(
    documents=chunks, embedding=embeddings,
    persist_directory="./chroma_db"
)

# 4. 搜索工具
@tool
def search_knowledge_base(query: str) -> str:
    """在知识库中语义搜索。"""
    results = vector_store.similarity_search(query, k=3)
    return "\n\n".join([r.page_content for r in results])
```

**关键参数**:
- `chunk_size=500`：太大语义不聚焦，太小信息不完整
- `chunk_overlap=50`：防止关键信息在分块边界被截断
- `text-embedding-3-small`：1536 维，性价比最高

### Day 3: CrewAI 编排

**原理**: `Crew` 把多个 Agent 和 Task 组织成有序流程。`Process.sequential` 顺序执行——上一个的输出是下一个的输入。

**任务**: 创建 `customer_crew.py`

```python
from crewai import Crew, Process

customer_crew = Crew(
    agents=[triage_agent, answer_agent, escalation_agent],
    tasks=[],
    process=Process.sequential,  # 顺序执行
    verbose=True
)

result = customer_crew.kickoff(inputs={
    "user_message": "登录一直提示密码错误",
    "user_history": "老用户"
})
```

**两种模式对比**:
- `Process.sequential`：Agent 按顺序执行（客服流程适合）
- `Process.hierarchical`：有"领导 Agent"分配任务（复杂项目适合）

### Day 4: Spring Boot 工单系统

**数据库表**: tickets（id, user_id, title, description, priority, status, source, ai_summary, ai_attempts, created_at, updated_at）

**API**: `POST /api/tickets`, `GET /api/tickets`, `GET /api/tickets/{id}`

### Day 5: WebSocket 实时对话 + Docker Compose

**原理**: HTTP 请求-响应后断开；WebSocket 持久连接，适合 Agent 流式输出。

**Docker Compose**: agent-service + business-service + mysql + chroma

---

## 项目 3: AI 第二大脑（5 天）

**简历标题**: 融合 RAG 与 Neo4j 知识图谱的混合检索知识管理系统

**项目结构**:

```
second-brain/
├── app/
│   ├── main.py
│   ├── ingestion/
│   │   ├── loader.py          # 多格式加载器（MD/PDF/代码/网页）
│   │   ├── chunker.py         # 智能分块（不同格式不同策略）
│   │   └── embedder.py        # 嵌入管道
│   ├── retrieval/
│   │   ├── vector_search.py   # ChromaDB 语义搜索
│   │   ├── keyword_search.py  # BM25 关键词搜索
│   │   ├── hybrid.py          # RRF 混合检索
│   │   └── reranker.py        # Cross-Encoder 精排
│   ├── knowledge_graph/
│   │   ├── extractor.py       # 实体关系抽取
│   │   ├── neo4j_client.py    # Neo4j 封装
│   │   └── graph_builder.py   # 图谱构建
│   ├── agent/
│   │   └── brain_agent.py     # 第二大脑 Agent
│   └── ui/
│       └── app.py             # Gradio 界面
└── docker-compose.yml
```

### Day 1: 多源数据接入管线

**任务**: 创建统一加载器，支持 MD/PDF/代码/JSON，自动识别格式

### Day 2: 嵌入管道 + ChromaDB

**任务**: OpenAI Embeddings → 向量化 → 增量更新 ChromaDB

### Day 3: 混合检索策略（核心亮点）

**原理**:
- **语义检索**：擅长"怎么提高性能"→找到"性能优化"（字面不同语义相关 ✓）
- **关键词检索（BM25）**：擅长 `UserService.java`（精确匹配 ✓）
- **RRF 融合**：两个排序结果加权融合后重新排序
- **Cross-Encoder 精排**：初筛 20 条 → 精排到 3 条（query+document 一起编码，精准但慢）

**任务**: 实现 `hybrid.py`（RRF 融合算法）和 `reranker.py`

```python
def _rrf_fusion(self, semantic, keyword, alpha, top_k):
    k = 60  # RRF 常数
    scores = {}
    for rank, (doc, _) in enumerate(semantic, 1):
        scores[doc_id] = {"doc": doc, "score": alpha * (1/(k+rank))}
    for rank, (doc, _) in enumerate(keyword, 1):
        scores[doc_id]["score"] += (1-alpha) * (1/(k+rank))
    return sorted(scores.values(), key=lambda x: x["score"], reverse=True)[:top_k]
```

### Day 4: Neo4j 知识图谱

**原理**: 向量检索擅长"找到相关内容"，知识图谱擅长"回答关系类问题"（"谁依赖谁"、"属于哪个模块"）。

**任务**: LLM 抽取实体关系 → Neo4j 存储 → Cypher 查询

```python
# 实体关系抽取 Prompt
extract_prompt = """提取实体（Person/Technology/Concept/File）和关系（USES/DEPENDS_ON/PART_OF）。
输出严格 JSON：{"entities":[{"name":"Spring Boot","type":"Technology"}],
              "relations":[{"source":"Spring Boot","relation":"USES","target":"JPA"}]}"""
```

### Day 5: 整合 + Gradio 界面

**任务**: Agent 整合向量搜索 + 关键词搜索 + 图谱查询 → Gradio ChatInterface

---

## 项目 4: Agent 工作流平台（5 天）

**简历标题**: AI Agent 工作流编排平台 — 可视化构建、多模型接入、插件扩展

**项目结构**:

```
agent-flow-platform/
├── frontend/                 # React + React Flow
├── agent-executor/           # FastAPI + LangGraph
├── business-service/         # Spring Boot
└── docker-compose.yml
```

### Day 1: LangGraph 工作流引擎

**原理**: `StateGraph` 建模 Agent 执行过程为有向图。Node = 执行步骤，Edge = 流转规则。

**任务**: 实现 `WorkflowRunner.build_graph()`——根据工作流 JSON 定义动态构建 LangGraph

```python
def build_graph(self, definition: WorkflowDefinition) -> StateGraph:
    graph = StateGraph(WorkflowState)
    for node in definition.nodes:
        graph.add_node(node.id, self._get_executor(node))
    for edge in definition.edges:
        if edge.condition:
            graph.add_conditional_edges(...)
        else:
            graph.add_edge(edge.source, edge.target)
    return graph.compile()
```

### Day 2: Spring Boot 业务层

**数据库**: workflows（JSON 存储工作流定义）、executions（执行记录）、execution_steps（步骤明细）

### Day 3: FastAPI + SSE 流式

**原理**: SSE（Server-Sent Events）单向流——服务器不断推送数据给前端。适合展示工作流执行过程。

**任务**: SSE 端点 + 前端 EventSource 客户端

### Day 4: 多模型适配器

**原理**: 适配器模式——统一接口，适配 OpenAI / DeepSeek 等不同 LLM 提供商的 API 差异。

**任务**: `LLMProvider` 接口 + `OpenAIProvider` + `DeepSeekProvider` + `PluginRegistry`

### Day 5: React Flow 工作流编辑器 + 联调

**任务**: 可拖拽节点（LLM/Tool/Condition/Input/Output）、连线、属性面板 + Docker Compose 一键部署

---

## Phase 2 结业检查

- [ ] Tool Calling 完整流程：Schema 生成 → LLM 决策 → 执行 → 结果回传
- [ ] RAG 三个核心步骤：文档加载分块 → 向量嵌入 → 相似度检索
- [ ] 混合检索为什么比纯向量检索好（RRF 融合原理）
- [ ] 知识图谱和向量数据库各自适用场景
- [ ] CrewAI Process.sequential vs Process.hierarchical
- [ ] LangGraph StateGraph 如何建模工作流
- [ ] SSE vs WebSocket 各自适用场景
- [ ] LLM Provider 多模型适配器设计
- [ ] Docker Compose 多服务编排
