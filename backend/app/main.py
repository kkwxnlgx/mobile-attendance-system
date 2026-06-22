"""FastAPI 应用入口。

启动：
    cd backend
    uvicorn app.main:app --port 8000
然后打开 http://127.0.0.1:8000/docs 查看接口文档。
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .database import Base, engine
from .routers import (
    admin, attendance, auth, classes, courses, fixes, leaves, notifications, stats,
)

app = FastAPI(title="移动考勤系统 API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def on_startup():
    # 确保表存在（库需提前由 init_db 创建）
    Base.metadata.create_all(bind=engine)


@app.get("/", tags=["健康检查"])
def root():
    return {"app": "移动考勤系统", "status": "ok", "docs": "/docs"}


for r in (auth, classes, courses, attendance, leaves, fixes, stats, notifications, admin):
    app.include_router(r.router)
