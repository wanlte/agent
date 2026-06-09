# Phase 3: Agent 底层原理 实施计划

> **Goal:** 手搓一个能跑的 Agent 框架，彻底理解 LLM 调用、Tool 系统、ReAct 循环、Memory 管理的底层实现

**Tech Stack:** Python 3.11+, OpenAI SDK, tiktoken, pytest

**核心理念**: 你已经用了 3 周 LangChain/CrewAI，现在忘掉框架，用最原始的方式实现 Agent。框架隐藏了太多细节——你以为懂了 Tool Calling，其实只是懂了 `@tool` 装饰器。手搓一遍，你会看到每一层在做什么。

---

## 你手写的框架结构

```
mini-agent-framework/
├── core/
│   ├── __init__.py
│   ├── types.py           # 核心类型（Message, ToolCall, AgentState...）
│   ├── llm.py             # LLM 调用抽象层（统一 OpenAI/DeepSeek 接口）
│   ├── tools.py            # 工具系统（注册、Schema 生成、执行）
│   ├── agent.py            # Agent 核心循环（ReAct）
│   ├── memory.py           # 记忆系统（Token 管理 + 长期摘要）
│   └── planner.py          # 规划器（Plan & Execute）
├── examples/
│   ├── 01_basic_chat.py    # 纯对话
│   ├── 02_single_tool.py   # 单工具调用
│   ├── 03_multi_tools.py   # 多工具自主选择
│   ├── 04_memory.py         # 带记忆多轮对话
│   ├── 05_plan_execute.py  # 规划+执行
│   └── 06_error_recovery.py # 错误恢复
└── tests/
```

---

## Week 1: 拆解 Agent 核心机制

### Day 1: LLM 调用抽象层

**原理**: 无论调用 OpenAI、DeepSeek 还是其他模型，底层都是 HTTP POST 请求。你的抽象层隐藏差异，对外暴露统一接口。

**核心**: LLM API 的标准请求/响应格式:

```
POST /v1/chat/completions
Body: {"model":"...", "messages":[...], "tools":[...], "temperature":0.7}

Response: {
  "choices": [{
    "message": {
      "content": "思考内容...",        // 或 null（如果要调工具）
      "tool_calls": [{                 // LLM 要求执行工具时出现
        "id": "call_abc",
        "function": {"name": "search", "arguments": "{\"query\":\"...\"}"}
      }]
    },
    "finish_reason": "tool_calls"      // "stop" = 完成 / "tool_calls" = 要调工具
  }]
}
```

**任务**: 创建 `core/types.py` 和 `core/llm.py`

**types.py** — 统一类型定义：

```python
from dataclasses import dataclass, field
from typing import Literal, Optional, Any
from enum import Enum

class MessageRole(str, Enum):
    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"
    TOOL = "tool"

@dataclass
class ToolCall:
    id: str
    name: str
    arguments: dict[str, Any]

@dataclass
class Message:
    role: MessageRole
    content: Optional[str] = None
    tool_calls: Optional[list[ToolCall]] = None
    tool_call_id: Optional[str] = None
    name: Optional[str] = None

@dataclass
class LLMResponse:
    content: Optional[str] = None
    tool_calls: Optional[list[ToolCall]] = None
    finish_reason: Literal["stop", "tool_calls", "length"] = "stop"
    usage: dict[str, int] = field(default_factory=dict)
```

**llm.py** — LLM 客户端：

```python
from openai import OpenAI
import json
from .types import Message, LLMResponse, ToolCall

class LLMClient:
    def __init__(self, api_key: str, base_url: str = "https://api.openai.com/v1",
                 default_model: str = "gpt-4o-mini"):
        self.client = OpenAI(api_key=api_key, base_url=base_url)
        self.default_model = default_model

    def chat(self, messages: list[Message], tools_schema: list[dict] | None = None,
             model: str | None = None, temperature: float = 0.7) -> LLMResponse:
        api_messages = [self._to_openai_msg(m) for m in messages]
        kwargs = {"model": model or self.default_model, "messages": api_messages,
                  "temperature": temperature}
        if tools_schema:
            kwargs["tools"] = tools_schema
            kwargs["tool_choice"] = "auto"

        response = self.client.chat.completions.create(**kwargs)
        choice = response.choices[0]

        tool_calls = None
        if choice.message.tool_calls:
            tool_calls = [ToolCall(id=tc.id, name=tc.function.name,
                          arguments=json.loads(tc.function.arguments))
                          for tc in choice.message.tool_calls]

        return LLMResponse(
            content=choice.message.content,
            tool_calls=tool_calls,
            finish_reason=choice.finish_reason,
            usage={"prompt": response.usage.prompt_tokens,
                   "completion": response.usage.completion_tokens,
                   "total": response.usage.total_tokens}
        )

    def _to_openai_msg(self, msg: Message) -> dict:
        result = {"role": msg.role.value}
        if msg.content is not None:
            result["content"] = msg.content
        if msg.tool_calls:
            result["tool_calls"] = [{"id": tc.id, "type": "function",
                "function": {"name": tc.name, "arguments": json.dumps(tc.arguments)}}
                for tc in msg.tool_calls]
        if msg.tool_call_id:
            result["tool_call_id"] = msg.tool_call_id
        return result
```

**验证**: `examples/01_basic_chat.py` — 发一条消息给 LLM，打印响应

---

### Day 2: 工具系统

**原理**: 工具系统的三个步骤：
1. **注册**：Python 函数 → JSON Schema（LLM 能理解的格式）
2. **匹配**：LLM 返回 tool_calls → 根据名称找到 Python 函数
3. **执行**：调用 Python 函数 → 结果包装成 Message → 发回给 LLM

**自动生成 JSON Schema 的关键**：`inspect.signature` + `get_type_hints` 读取 Python 函数的参数类型信息

```python
def search_knowledge(query: str, max_results: int = 5) -> str:
    """搜索知识库。query: 搜索关键词  max_results: 最大返回数"""
    ...

# 自动生成：
# {
#   "type": "function",
#   "function": {
#     "name": "search_knowledge",
#     "description": "搜索知识库。",
#     "parameters": {
#       "type": "object",
#       "properties": {
#         "query": {"type": "string", "description": "搜索关键词"},
#         "max_results": {"type": "integer", "description": "最大返回数"}
#       },
#       "required": ["query"]
#     }
#   }
# }
```

**任务**: 创建 `core/tools.py`

```python
import json
import inspect
from typing import Callable, Any, get_type_hints

class Tool:
    def __init__(self, func: Callable, name: str, description: str):
        self.func = func
        self.name = name
        self.description = description

    def to_openai_schema(self) -> dict:
        hints = get_type_hints(self.func)
        sig = inspect.signature(self.func)
        properties = {}
        required = []
        for param_name, param in sig.parameters.items():
            if param_name in ("self", "cls"):
                continue
            json_type = {str: "string", int: "integer", float: "number",
                        bool: "boolean", list: "array", dict: "object"}.get(hints.get(param_name, str), "string")
            properties[param_name] = {"type": json_type, "description": self._extract_param_desc(param_name)}
            if param.default is inspect.Parameter.empty:
                required.append(param_name)
        return {"type": "function", "function": {"name": self.name, "description": self.description,
                "parameters": {"type": "object", "properties": properties, "required": required}}}

    def execute(self, **kwargs) -> str:
        try:
            return str(self.func(**kwargs))
        except Exception as e:
            return f"工具执行错误: {e}"

    def _extract_param_desc(self, param_name: str) -> str:
        if not self.func.__doc__:
            return ""
        for line in self.func.__doc__.split("\n"):
            line = line.strip()
            if line.startswith(f"{param_name}:"):
                return line.split(":", 1)[1].strip()
        return ""


class ToolRegistry:
    def __init__(self):
        self._tools: dict[str, Tool] = {}

    def register(self, func=None, *, name=None, description=None):
        def decorator(fn):
            tool_name = name or fn.__name__
            tool_desc = description or (fn.__doc__ or "").split("\n")[0].strip()
            self._tools[tool_name] = Tool(fn, tool_name, tool_desc)
            return fn
        return decorator(func) if func else decorator

    def get_schemas(self) -> list[dict]:
        return [t.to_openai_schema() for t in self._tools.values()]

    def execute(self, name: str, arguments: dict) -> str:
        tool = self._tools.get(name)
        if not tool:
            return f"错误：未知工具 '{name}'"
        return tool.execute(**arguments)
```

**验证**: `examples/02_single_tool.py` — 注册一个天气查询工具，Agent 调用它

---

### Day 3: Agent 核心循环 —— ReAct（最重要的一天）

**原理**: Agent 的灵魂是一个 while 循环：

```
while 任务未完成:
    1. 调 LLM，传入当前消息 + 可用工具列表
    2. LLM 返回：
       - 文本 → 任务完成，返回文本
       - tool_calls → 执行工具 → 结果加入消息 → 回到第 1 步
```

**ReAct** = Reasoning（推理） + Acting（行动）

**任务**: 创建 `core/agent.py`

```python
from .types import Message, MessageRole, AgentState
from .llm import LLMClient
from .tools import ToolRegistry

SYSTEM_PROMPT = """你是智能助手，能使用工具完成任务。
规则：
1. 分析问题，判断是否需要工具
2. 需要工具→调用→基于结果继续思考
3. 不需要→直接回答
4. 连续 3 次工具调用无进展→给出最佳答案"""

class Agent:
    def __init__(self, llm: LLMClient, tools: ToolRegistry,
                 max_iterations: int = 15, system_prompt: str = SYSTEM_PROMPT):
        self.llm = llm
        self.tools = tools
        self.max_iterations = max_iterations
        self.system_prompt = system_prompt

    def run(self, user_input: str) -> str:
        state = AgentState(max_iterations=self.max_iterations)
        state.messages.append(Message(role=MessageRole.SYSTEM, content=self.system_prompt))
        state.messages.append(Message(role=MessageRole.USER, content=user_input))

        # === Agent 核心循环 ===
        while not state.is_done and state.iteration_count < state.max_iterations:
            state.iteration_count += 1

            # 1. 调用 LLM
            response = self.llm.chat(
                messages=state.messages,
                tools_schema=self.tools.get_schemas() if self.tools.list_tools() else None,
            )

            # 2. 判断 LLM 想做什么
            if response.tool_calls:
                self._handle_tool_calls(state, response)
            else:
                state.final_answer = response.content
                state.is_done = True

        return state.final_answer or "超过最大轮数限制"

    def _handle_tool_calls(self, state, response):
        # 1. 把 LLM 的 tool_calls 作为 assistant 消息加入历史
        state.messages.append(Message(role=MessageRole.ASSISTANT,
                              tool_calls=response.tool_calls,
                              content=response.content))
        # 2. 执行每个工具，结果作为 tool 消息加入历史
        for tc in response.tool_calls:
            result = self.tools.execute(tc.name, tc.arguments)
            state.messages.append(Message(role=MessageRole.TOOL,
                                  content=result, tool_call_id=tc.id, name=tc.name))
```

**为什么这 50 行是理解 Agent 的关键**：LangChain 的 `AgentExecutor` 点进去看源码，核心就是类似循环。区别只有 Callbacks、中间件、错误策略这些边角料。

**验证**: `examples/03_multi_tools.py` — 注册天气+计算器，测试"北京今天天气？"、"计算 123+456"、"北京和上海哪个更热？"

---

### Day 4: Memory 系统

**原理**: 
- **短期记忆** = 消息数组 + Token 计数 + 智能裁剪
- **长期记忆** = 旧对话自动摘要 + 向量存储

**Token 裁剪策略**：始终保留 system prompt（Agent 的"人设"），从旧到新裁剪对话历史

**任务**: 创建 `core/memory.py`

```python
import tiktoken

class TokenCounter:
    def __init__(self, model: str = "gpt-4o-mini"):
        try:
            self.encoder = tiktoken.encoding_for_model(model)
        except KeyError:
            self.encoder = tiktoken.get_encoding("cl100k_base")

    def count(self, text: str) -> int:
        return len(self.encoder.encode(text))

    def count_messages(self, messages: list[Message]) -> int:
        total = 0
        for msg in messages:
            total += 4  # 每条消息格式固定开销
            if msg.content:
                total += self.count(msg.content)
            if msg.tool_calls:
                for tc in msg.tool_calls:
                    total += self.count(tc.name) + self.count(str(tc.arguments))
        return total


class MemoryManager:
    def __init__(self, model: str = "gpt-4o-mini", max_tokens: int = 8000):
        self.counter = TokenCounter(model)
        self.max_tokens = max_tokens

    def trim(self, messages: list[Message]) -> list[Message]:
        """保留 system prompt，从旧消息开始裁剪"""
        total = self.counter.count_messages(messages)
        if total <= self.max_tokens:
            return messages

        system_idx = 0 if messages and messages[0].role.value == "system" else None
        start = 1 if system_idx is not None else 0
        trimmed, remaining = messages[:start], messages[start:]
        current = self.counter.count_messages(trimmed)

        for msg in remaining:
            mt = self.counter.count_messages([msg])
            if current + mt <= self.max_tokens:
                trimmed.append(msg)
                current += mt
            else:
                break
        return trimmed


class SummaryMemory:
    def __init__(self, llm_client):
        self.llm = llm_client
        self.summaries: list[str] = []

    def summarize(self, messages: list[Message]) -> str:
        conversation = "\n".join([f"{m.role}: {m.content or '(tools)'}" for m in messages])
        response = self.llm.chat([Message(role="system",
            content="将对话总结为一句话摘要"), Message(role="user", content=conversation)])
        self.summaries.append(response.content)
        return response.content

    def get_context(self) -> str:
        return "对话历史摘要：\n" + "\n---\n".join(self.summaries) if self.summaries else ""
```

---

### Day 5: 规划器（Plan & Execute）

**原理**: ReAct 是"走一步看一步"。Plan & Execute 是"先列计划，再逐步执行"。

**任务**: 创建 `core/planner.py`

```python
class Planner:
    def __init__(self, llm_client):
        self.llm = llm_client

    def create_plan(self, task: str) -> list[dict]:
        response = self.llm.chat([Message(role="user",
            content=f"为此任务制定步骤计划，格式：1. 标题：描述\\n{task}")])
        return self._parse_plan(response.content)

    def _parse_plan(self, text: str) -> list[dict]:
        import re
        steps = []
        for m in re.finditer(r"(\d+)[.、]\s*(.+?)[：:]\s*(.+)", text):
            steps.append({"step": int(m.group(1)), "title": m.group(2).strip(),
                          "description": m.group(3).strip(), "status": "pending"})
        return steps


class PlanAndExecuteAgent:
    def __init__(self, agent, planner):
        self.agent = agent
        self.planner = planner

    def run(self, task: str) -> dict:
        plan = self.planner.create_plan(task)
        results = []
        for step in plan:
            step["status"] = "executing"
            context = "\n".join([f"步骤{r['step']}: {r.get('result','')[:200]}" for r in results])
            result = self.agent.run(
                f"执行步骤{step['step']}: {step['title']}\n已完成：{context}")
            step["status"] = "completed"
            step["result"] = result
            results.append(step)
        return {"plan": plan, "results": results}
```

---

## Week 2: 高级特性 + 对比反思

### Day 6: 错误恢复

**三类错误 + 策略**:

| 错误 | 示例 | 策略 |
|------|------|------|
| 参数错误 | LLM 传 `city=123` | 错误反馈给 LLM，让它修正 |
| 执行超时 | API > 30s | 重试 1 次，失败降级 |
| 无结果 | 搜索无结果 | 自动换词重搜索 |

### Day 7: 多 Agent 路由

**原理**: 分发器 + 多个专用 Agent。两层路由：关键词匹配（快）+ LLM 语义匹配（准）

```python
class MiniRouter:
    def __init__(self, llm_client):
        self.agents: list[AgentCard] = []
        self.llm = llm_client

    def route(self, user_input: str) -> AgentCard:
        # 第一层：关键词匹配
        for card in self.agents:
            if any(kw in user_input.lower() for kw in card.keywords):
                return card
        # 第二层：LLM 语义匹配
        # 第三层：默认 Agent
        return self.default_agent
```

### Day 8: 与 LangChain 对比分析

用自研框架 vs LangChain 实现同一任务，写对比笔记：

| 维度 | 自研框架 | LangChain |
|------|---------|-----------|
| 代码行数 | ~200 行（全懂） | ~3 行（隐藏数千行） |
| Agent 循环 | 你写的 while 循环 | AgentExecutor 内部 |
| 工具注册 | 你的 ToolRegistry | @tool 装饰器 |
| Schema 生成 | 你的 get_type_hints | Pydantic |
| 可调试性 | print 就能看 | 需要 verbose=True |

**核心结论**: LangChain 没有魔法。核心就是你第 3 天写的 50 行 while 循环，外面包了 20 层抽象。

### Day 9-10: 完善 + 文档

- 加 pytest 单元测试
- 扩展支持 Anthropic Claude（验证抽象层是否真正解耦）
- 写 README：架构图、快速开始、API 文档、对比 LangChain 的差异表

---

## Phase 3 结业检查

- [ ] 能画出 ReAct 循环流程图
- [ ] 理解 tool_calls 在 LLM API 响应中的原始 JSON
- [ ] 理解 JSON Schema 从 Python 函数自动生成
- [ ] 理解 token 计数和记忆裁剪策略
- [ ] 能说出 Plan & Execute 和 ReAct 各自适用场景
- [ ] 能说出自研框架和 LangChain 的 3 个关键区别
