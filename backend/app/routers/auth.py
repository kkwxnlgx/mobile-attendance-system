"""UC0101 学生注册、UC0102 用户登录。"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import ROLE_STUDENT, User
from ..schemas import LoginIn, LoginOut, RegisterIn, UserOut
from ..security import get_current_user, issue_token
from ..utils import hash_password, verify_password

router = APIRouter(prefix="/api/auth", tags=["认证"])


@router.post("/register", response_model=UserOut, summary="学生注册")
def register(data: RegisterIn, db: Session = Depends(get_db)):
    if db.query(User).filter(User.username == data.username).first():
        raise HTTPException(status_code=400, detail="该学号已注册")
    user = User(
        username=data.username,
        password_hash=hash_password(data.password),
        name=data.name,
        role=ROLE_STUDENT,
        phone=data.phone,
        status=1,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


@router.post("/login", response_model=LoginOut, summary="登录")
def login(data: LoginIn, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.username == data.username).first()
    if user is None or not verify_password(data.password, user.password_hash):
        raise HTTPException(status_code=400, detail="账号或密码错误")
    if user.status != 1:
        raise HTTPException(status_code=403, detail="账号已被禁用，请联系管理员")
    token = issue_token(user.id)
    return LoginOut(
        token=token,
        user_id=user.id,
        username=user.username,
        name=user.name,
        role=user.role,
        class_id=user.class_id,
    )


@router.get("/me", response_model=UserOut, summary="当前用户信息")
def me(user: User = Depends(get_current_user)):
    return user
