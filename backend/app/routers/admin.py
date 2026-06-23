"""系统管理员：用户管理（权限与数据维护）。"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import (
    ROLE_ADMIN, ROLE_STUDENT, ROLE_TEACHER,
    AttendanceRecord, AttendanceTask, Clazz, Course,
    FixRequest, LeaveRequest, Notification, User,
)
from ..schemas import AdminUserIn, AdminUserUpdateIn, MessageOut, UserOut
from ..security import require_role
from ..utils import hash_password

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


@router.post("/users", response_model=UserOut, summary="新增用户（学生/教师）")
def create_user(data: AdminUserIn, db: Session = Depends(get_db), _=Depends(admin_only)):
    if data.role not in (ROLE_STUDENT, ROLE_TEACHER):
        raise HTTPException(status_code=400, detail="只能新增学生或教师")
    if db.query(User).filter(User.username == data.username).first():
        raise HTTPException(status_code=400, detail="该账号已存在")
    class_id = None
    if data.role == ROLE_STUDENT and data.class_id:
        if db.query(Clazz).get(data.class_id) is None:
            raise HTTPException(status_code=400, detail="班级不存在")
        class_id = data.class_id
    u = User(
        username=data.username,
        password_hash=hash_password(data.password),
        name=data.name,
        role=data.role,
        phone=data.phone,
        class_id=class_id,
        status=1,
    )
    db.add(u)
    db.commit()
    db.refresh(u)
    return u


@router.put("/users/{user_id}", response_model=UserOut, summary="编辑用户")
def update_user(user_id: int, data: AdminUserUpdateIn,
                db: Session = Depends(get_db), _=Depends(admin_only)):
    u = db.query(User).get(user_id)
    if u is None:
        raise HTTPException(status_code=404, detail="用户不存在")
    if data.name is not None and data.name.strip():
        u.name = data.name.strip()
    if data.phone is not None:
        u.phone = data.phone or None
    if data.password:
        u.password_hash = hash_password(data.password)
    db.commit()
    db.refresh(u)
    return u


@router.delete("/users/{user_id}", response_model=MessageOut, summary="删除用户")
def delete_user(user_id: int, db: Session = Depends(get_db), me: User = Depends(admin_only)):
    u = db.query(User).get(user_id)
    if u is None:
        raise HTTPException(status_code=404, detail="用户不存在")
    if u.id == me.id:
        raise HTTPException(status_code=400, detail="不能删除自己的账号")
    if u.role == ROLE_ADMIN:
        raise HTTPException(status_code=400, detail="不能删除管理员账号")
    # 关联保护：被业务数据引用时拒绝硬删，引导改用禁用
    refs = 0
    if u.role == ROLE_TEACHER:
        refs += db.query(Course).filter(Course.teacher_id == u.id).count()
        refs += db.query(AttendanceTask).filter(AttendanceTask.teacher_id == u.id).count()
    if u.role == ROLE_STUDENT:
        refs += db.query(AttendanceRecord).filter(AttendanceRecord.student_id == u.id).count()
        refs += db.query(LeaveRequest).filter(LeaveRequest.student_id == u.id).count()
        refs += db.query(FixRequest).filter(FixRequest.student_id == u.id).count()
    if refs:
        raise HTTPException(status_code=400, detail="该用户已有关联数据（课程/考勤/审批），无法删除，请改用禁用")
    # 仅清理其私有的通知，再删账号
    db.query(Notification).filter(Notification.user_id == u.id).delete()
    db.delete(u)
    db.commit()
    return MessageOut(message="已删除")


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
