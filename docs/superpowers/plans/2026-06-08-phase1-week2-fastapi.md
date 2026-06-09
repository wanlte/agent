# Phase 1 Week 2: FastAPI 速通 实施计划

> **Goal:** 用 FastAPI 实现同样的 Task Manager API，理解 Python 后端与 Java 后端的本质异同

**Tech Stack:** Python 3.11+, FastAPI, Pydantic, SQLAlchemy, SQLite, pytest, Docker

**Architecture:** 和 Spring Boot 周同样的三层架构（Router → Service → Repository），但用 Python 实现

---

## FastAPI vs Spring Boot 核心差异（先读这个）

| 概念 | Spring Boot | FastAPI |
|------|-------------|---------|
| **语言哲学** | 编译型、强类型、运行时反射 | 解释型、类型提示（Type Hints）、运行时验证 |
| **启动方式** | `@SpringBootApplication` + main | `uvicorn main:app --reload` |
| **路由定义** | `@GetMapping("/tasks")` 注解 | `@app.get("/tasks")` 装饰器 |
| **请求体验证** | `@Valid` + Bean Validation | **Pydantic `BaseModel`**（自动校验，无需额外注解） |
| **依赖注入** | `@Autowired` / 构造器注入 | **`Depends()` 函数**（更轻量） |
| **ORM** | JPA / Hibernate | SQLAlchemy |
| **异步支持** | Servlet 3.0+ / WebFlux | **Python `async/await` 原生** |
| **API 文档** | 需要 SpringDoc | **自动生成 `/docs` Swagger** |
| **热重载** | spring-devtools | `--reload` 默认支持 |
| **JSON 序列化** | Jackson（基于反射） | Pydantic（基于类型提示，更快） |

---

## Day 6: FastAPI 从零到 Hello World

### 讲原理

**FastAPI 的本质**：一个基于 Python 类型提示的异步 Web 框架。你的函数写什么类型，FastAPI 就自动做校验、序列化、生成文档。

```python
# 你写：
@app.get("/hello")
def hello(name: str = "World") -> dict:
    return {"message": f"Hello {name}"}

# FastAPI 自动：
# 1. 校验 name 必须是 str
# 2. 把 dict 转成 JSON
# 3. 生成 Swagger 文档（/docs）
# 4. 生成 OpenAPI Schema
```

**uvicorn**：Python 的异步 Web 服务器（类似 Tomcat 的角色）。FastAPI 是框架，uvicorn 是运行它的服务器。

**Starlette**：FastAPI 底层使用的 ASGI 框架（类似 Java 的 Servlet 规范）。

### 任务

**Step 1**: 安装依赖

```bash
pip install fastapi uvicorn[standard]
```

**Step 2**: 创建项目结构

```bash
mkdir -p F:/java.work/learning.txt/agent/task-manager-fastapi/app
```

**Step 3**: 创建 `app/main.py`

```python
from fastapi import FastAPI

app = FastAPI(title="Task Manager", version="1.0.0")


@app.get("/hello")
def hello():
    return {"message": "Hello Agent World"}


@app.get("/hello/{name}")
def hello_name(name: str):
    return {"message": f"Hello {name}"}
```

**Step 4**: 运行

```bash
cd F:/java.work/learning.txt/agent/task-manager-fastapi
uvicorn app.main:app --reload
```

浏览器访问：
- `http://localhost:8000/hello` → JSON 响应
- `http://localhost:8000/docs` → 自动生成的 Swagger 文档（可以直接在网页上测试 API）
- `http://localhost:8000/redoc` → 另一种风格的文档

---

## Day 7: Pydantic 模型 + CRUD API

### 讲原理

**Pydantic** 之于 FastAPI = Bean Validation + Jackson 之于 Spring Boot。

```python
from pydantic import BaseModel, Field
from enum import Enum

class Priority(str, Enum):
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"

class TaskCreate(BaseModel):
    title: str = Field(..., min_length=2, max_length=100, description="任务标题")
    description: str | None = Field(None, max_length=500)
    priority: Priority = Field(..., description="优先级")

# FastAPI 路由中：
@app.post("/api/tasks")
def create_task(task: TaskCreate):  # FastAPI 自动从请求体 JSON 解析 + 校验
    ...
```

**Pydantic 自动做的事**：
1. JSON → Python 对象（反序列化）
2. 类型校验（str/int/float）
3. 长度/范围校验（min_length/max_length）
4. 自动生成 JSON Schema（用于 Swagger 文档）
5. 友好的错误信息

### 任务

创建以下文件：

**文件 1**: `app/schemas.py`

```python
from pydantic import BaseModel, Field
from enum import Enum
from datetime import datetime


class Priority(str, Enum):
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"


class TaskStatus(str, Enum):
    TODO = "TODO"
    IN_PROGRESS = "IN_PROGRESS"
    DONE = "DONE"


class TaskCreate(BaseModel):
    title: str = Field(..., min_length=2, max_length=100, description="任务标题")
    description: str | None = Field(None, max_length=500, description="任务描述")
    priority: Priority = Field(..., description="优先级")


class TaskUpdate(BaseModel):
    title: str | None = Field(None, min_length=2, max_length=100)
    description: str | None = Field(None, max_length=500)
    status: TaskStatus | None = None
    priority: Priority | None = None


class TaskResponse(BaseModel):
    id: int
    title: str
    description: str | None
    status: TaskStatus
    priority: Priority
    created_at: datetime

    # Pydantic V2 的 ORM 模式配置
    model_config = {"from_attributes": True}
```

**文件 2**: `app/models.py`

```python
from sqlalchemy import Column, Integer, String, DateTime, Enum as SQLEnum
from sqlalchemy.orm import DeclarativeBase
from datetime import datetime, timezone
import enum


class Base(DeclarativeBase):
    pass


class Priority(str, enum.Enum):
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"


class TaskStatus(str, enum.Enum):
    TODO = "TODO"
    IN_PROGRESS = "IN_PROGRESS"
    DONE = "DONE"


class Task(Base):
    __tablename__ = "tasks"

    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String(100), nullable=False)
    description = Column(String(500), nullable=True)
    status = Column(String(20), nullable=False, default=TaskStatus.TODO.value)
    priority = Column(String(10), nullable=False, default=Priority.MEDIUM.value)
    created_at = Column(DateTime(timezone=True), nullable=False,
                        default=lambda: datetime.now(timezone.utc))
```

**文件 3**: `app/database.py`

```python
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from .models import Base
import os

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./taskdb.sqlite")

engine = create_engine(DATABASE_URL, echo=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def init_db():
    """创建所有表（相当于 JPA 的 ddl-auto: update）"""
    Base.metadata.create_all(bind=engine)


def get_db():
    """依赖注入：每个请求获得一个数据库 session"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
```

**文件 4**: `app/router.py`

```python
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from . import schemas, models, database

router = APIRouter(prefix="/api/tasks", tags=["tasks"])


@router.get("", response_model=list[schemas.TaskResponse])
def list_tasks(status: str | None = None, db: Session = Depends(database.get_db)):
    query = db.query(models.Task)
    if status:
        query = query.filter(models.Task.status == status)
    return query.order_by(models.Task.created_at.desc()).all()


@router.get("/{task_id}", response_model=schemas.TaskResponse)
def get_task(task_id: int, db: Session = Depends(database.get_db)):
    task = db.query(models.Task).filter(models.Task.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail=f"任务不存在: {task_id}")
    return task


@router.post("", response_model=schemas.TaskResponse, status_code=201)
def create_task(task_data: schemas.TaskCreate, db: Session = Depends(database.get_db)):
    task = models.Task(
        title=task_data.title,
        description=task_data.description,
        priority=task_data.priority.value,
        status=models.TaskStatus.TODO.value,
    )
    db.add(task)
    db.commit()
    db.refresh(task)
    return task


@router.put("/{task_id}", response_model=schemas.TaskResponse)
def update_task(task_id: int, task_data: schemas.TaskUpdate,
                db: Session = Depends(database.get_db)):
    task = db.query(models.Task).filter(models.Task.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail=f"任务不存在: {task_id}")

    update_dict = task_data.model_dump(exclude_unset=True)  # 只取用户传了的字段
    if "priority" in update_dict and update_dict["priority"]:
        update_dict["priority"] = update_dict["priority"].value
    if "status" in update_dict and update_dict["status"]:
        update_dict["status"] = update_dict["status"].value

    for key, value in update_dict.items():
        setattr(task, key, value)

    db.commit()
    db.refresh(task)
    return task


@router.delete("/{task_id}", status_code=204)
def delete_task(task_id: int, db: Session = Depends(database.get_db)):
    task = db.query(models.Task).filter(models.Task.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail=f"任务不存在: {task_id}")
    db.delete(task)
    db.commit()
```

**文件 5**: 更新 `app/main.py`

```python
from fastapi import FastAPI
from .database import init_db
from .router import router

app = FastAPI(title="Task Manager API", version="1.0.0")

@app.on_event("startup")
def on_startup():
    init_db()

app.include_router(router)


@app.get("/hello")
def hello():
    return {"message": "Hello Agent World"}
```

### 验证

```powershell
# 启动
uvicorn app.main:app --reload

# 测试（用 curl.exe 或浏览器打开 /docs 直接测试）
curl.exe -X POST http://localhost:8000/api/tasks -H "Content-Type: application/json" -d "{\"title\":\"学习FastAPI\",\"priority\":\"HIGH\"}"
curl.exe http://localhost:8000/api/tasks
```

---

## Day 8: async/await 异步编程本质

### 讲原理

**同步 vs 异步**：

```
同步（def）：
请求1 → [等数据库 100ms] → 返回 ──── 请求2 → [等数据库 100ms] → 返回
                                   ↑ 阻塞在这里什么也做不了

异步（async def）：
请求1 → [发起数据库查询] → 挂起 ──→ 请求2 → [发起查询] → 挂起
       ← [结果返回] ← 继续 ←──          ← [结果返回] ← 继续
```

**`async/await` 关键规则**：
1. `async def` 定义的函数返回一个 Coroutine，不会立即执行
2. `await` 挂起当前函数，把 CPU 让给其他协程
3. 只有 I/O 密集操作（数据库查询、HTTP 请求）能从异步中获益
4. CPU 密集计算用 async 反而更慢

**FastAPI 的两种路由**：

```python
# 同步路由：FastAPI 在线程池中执行（不会阻塞事件循环）
@app.get("/sync")
def sync_endpoint():
    time.sleep(2)  # FastAPI 自动放线程池
    return {"ok": True}

# 异步路由：必须用 async 库（如 httpx、SQLAlchemy async），否则无意义
@app.get("/async")
async def async_endpoint():
    await asyncio.sleep(2)  # 真正非阻塞
    return {"ok": True}
```

**Agent 应用为什么需要 async**：
- Agent 调用 LLM API 需要等待 2-10 秒
- 如果同步处理，一个用户等 10 秒，其他用户全阻塞
- 异步处理：10 个用户同时请求，一个等 LLM 时，事件循环处理下一个

---

## Day 9: pytest 测试 + Alembic 数据库迁移

### 讲原理

**pytest vs JUnit**：
- pytest 用函数式风格，不需要类
- `assert result == expected` 直接用 Python 自带断言
- `conftest.py` 定义全局 fixtures（类似 `@BeforeEach`）

**Alembic**：Python 版的 Flyway/Liquibase。数据库版本管理工具——每次表结构变更生成一个迁移文件，可以回滚。

### 任务

**Step 1**: 安装测试依赖

```bash
pip install pytest httpx
```

**Step 2**: 创建 `tests/conftest.py`

```python
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.database import init_db, engine
from app.models import Base

@pytest.fixture
def client():
    # 测试用内存数据库
    Base.metadata.create_all(bind=engine)
    with TestClient(app) as c:
        yield c
    Base.metadata.drop_all(bind=engine)
```

**Step 3**: 创建 `tests/test_tasks.py`

```python
def test_create_task(client):
    response = client.post("/api/tasks", json={
        "title": "测试任务",
        "priority": "HIGH"
    })
    assert response.status_code == 201
    data = response.json()
    assert data["title"] == "测试任务"
    assert data["status"] == "TODO"
    assert data["id"] is not None


def test_list_tasks(client):
    client.post("/api/tasks", json={"title": "任务A", "priority": "HIGH"})
    client.post("/api/tasks", json={"title": "任务B", "priority": "LOW"})

    response = client.get("/api/tasks")
    assert response.status_code == 200
    assert len(response.json()) == 2


def test_get_not_found(client):
    response = client.get("/api/tasks/99999")
    assert response.status_code == 404


def test_validation_error(client):
    response = client.post("/api/tasks", json={
        "title": "A",          # 太短（< 2）
        "priority": "INVALID"  # 无效枚举值
    })
    assert response.status_code == 422  # FastAPI 的校验错误码


def test_update_partial(client):
    resp = client.post("/api/tasks", json={"title": "原始", "priority": "LOW"})
    task_id = resp.json()["id"]

    resp = client.put(f"/api/tasks/{task_id}", json={"title": "新标题"})
    assert resp.status_code == 200
    assert resp.json()["title"] == "新标题"
    assert resp.json()["priority"] == "LOW"  # 未传的不变


def test_delete(client):
    resp = client.post("/api/tasks", json={"title": "删除", "priority": "MEDIUM"})
    task_id = resp.json()["id"]

    resp = client.delete(f"/api/tasks/{task_id}")
    assert resp.status_code == 204

    resp = client.get(f"/api/tasks/{task_id}")
    assert resp.status_code == 404
```

**Step 4**: 运行测试

```bash
pytest -v
```

---

## Day 10: Docker 部署

### 讲原理

**Docker 做的事情**：把你的代码 + Python 解释器 + 所有依赖打包成一个镜像。任何装了 Docker 的机器都能跑，不挑环境。

**Dockerfile 核心指令**：

```dockerfile
FROM python:3.11-slim          # 基础镜像
WORKDIR /app                    # 工作目录
COPY requirements.txt .         # 先复制依赖文件（利用缓存）
RUN pip install -r requirements.txt
COPY . .                        # 再复制代码
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### 任务

**Step 1**: 创建 `requirements.txt`

```
fastapi==0.109.0
uvicorn[standard]==0.25.0
sqlalchemy==2.0.25
pydantic==2.5.0
pytest==8.0.0
httpx==0.26.0
```

**Step 2**: 创建 `Dockerfile`

```dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

**Step 3**: 构建并运行

```bash
docker build -t task-manager-fastapi .
docker run -p 8000:8000 task-manager-fastapi
```

浏览器打开 `http://localhost:8000/docs` 验证。

---

## Week 2 总结

```
task-manager-fastapi/
├── app/
│   ├── __init__.py
│   ├── main.py          # FastAPI 入口 + 路由注册
│   ├── schemas.py        # Pydantic 请求/响应模型
│   ├── models.py         # SQLAlchemy ORM 模型
│   ├── database.py       # 数据库连接 + 依赖注入
│   └── router.py         # REST API 路由处理器
├── tests/
│   ├── conftest.py       # pytest fixtures
│   └── test_tasks.py     # API 测试（6 个用例）
├── requirements.txt
├── Dockerfile
└── .env
```

**Phase 1 结业检查**：

- [ ] Spring Boot 和 FastAPI 都能独立写出 CRUD API
- [ ] 理解 DTO/Model 分离的必要性
- [ ] 理解参数校验和全局异常处理
- [ ] 理解 ORM（JPA/SQLAlchemy）的作用
- [ ] 能写集成测试（MockMvc/pytest）
- [ ] 能用 Docker 部署应用
- [ ] 能说出 Spring Boot 和 FastAPI 的 5 个关键差异
