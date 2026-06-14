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