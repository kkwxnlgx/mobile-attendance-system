"""系统管理员：用户管理（权限与数据维护）。"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import ROLE_ADMIN, User
from ..schemas import UserOut
from ..security import require_role

router = APIRouter(prefix="/api/admin", tags=["系统管理"])

admin_only = require_role(ROLE_ADMIN)


@router.get("/users", response_model=list[UserOut], summary="用户列表")
def list_users(role: str = None, keyword: str = "",
               db: Session = Depends(get_db), _=Depends(admin_only)):
    q = db.query(User)
    if role:
        q = q.filter(User.role == role)
    if keyword:
        like = f"%{keyword}%"
        q = q.filter((User.username.like(like)) | (User.name.like(like)))
    return q.order_by(User.role, User.username).all()


@router.put("/users/{user_id}/status", response_model=UserOut, summary="启用/禁用账号")
def toggle_user(user_id: int, db: Session = Depends(get_db), me: User = Depends(admin_only)):
    u = db.query(User).get(user_id)
    if u is None:
        raise HTTPException(status_code=404, detail="用户不存在")
    if u.id == me.id:
        raise HTTPException(status_code=400, detail="不能禁用自己的账号")
    u.status = 0 if u.status == 1 else 1
    db.commit()
    db.refresh(u)
    return u
