"""SQLAlchemy 引擎、会话与基类。"""
from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

from . import config

# SQLite 需要 check_same_thread=False 才能在多线程的 FastAPI 下使用
connect_args = {"check_same_thread": False} if config.DB_BACKEND == "sqlite" else {}

engine = create_engine(
    config.database_url(),
    pool_pre_ping=True,
    echo=False,
    connect_args=connect_args,
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()


def get_db():
    """FastAPI 依赖：每个请求一个会话，结束自动关闭。"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
