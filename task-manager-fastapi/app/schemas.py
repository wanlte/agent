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