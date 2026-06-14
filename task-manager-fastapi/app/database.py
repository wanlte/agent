from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from .models import Base
import os

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./taskdb.sqlite")

engine = create_engine(DATABASE_URL, echo=False)
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