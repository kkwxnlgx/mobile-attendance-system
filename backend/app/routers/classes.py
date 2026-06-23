"""UC0201 班级管理、UC0203 学生名单管理。"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import (
    ROLE_ADMIN, ROLE_STUDENT, ROLE_TEACHER,
    AttendanceTask, Clazz, Schedule, User,
)
from ..schemas import AddStudentIn, ClassIn, ClassOut, MessageOut, UserOut
from ..security import require_role

router = APIRouter(prefix="/api/classes", tags=["班级与名单"])

manage = require_role(ROLE_TEACHER, ROLE_ADMIN)


def _to_out(db: Session, c: Clazz) -> ClassOut:
    count = db.query(User).filter(User.class_id == c.id).count()
    return ClassOut(
        id=c.id, name=c.name, grade=c.grade, major=c.major,
        remark=c.remark, status=c.status, student_count=count,
    )


@router.get("", response_model=list[ClassOut], summary="班级列表")
def list_classes(db: Session = Depends(get_db), _=Depends(manage)):
    return [_to_out(db, c) for c in db.query(Clazz).order_by(Clazz.id).all()]


@router.post("", response_model=ClassOut, summary="新增班级")
def create_class(data: ClassIn, db: Session = Depends(get_db), _=Depends(manage)):
    if db.query(Clazz).filter(Clazz.name == data.name).first():
        raise HTTPException(status_code=400, detail="班级名称已存在")
    c = Clazz(name=data.name, grade=data.grade, major=data.major, remark=data.remark, status=1)
    db.add(c)
    db.commit()
    db.refresh(c)
    return _to_out(db, c)


@router.put("/{class_id}", response_model=ClassOut, summary="编辑班级")
def update_class(class_id: int, data: ClassIn, db: Session = Depends(get_db), _=Depends(manage)):
    c = db.query(Clazz).get(class_id)
    if c is None:
        raise HTTPException(status_code=404, detail="班级不存在")
    dup = db.query(Clazz).filter(Clazz.name == data.name, Clazz.id != class_id).first()
    if dup:
        raise HTTPException(status_code=400, detail="班级名称已存在")
    c.name, c.grade, c.major, c.remark = data.name, data.grade, data.major, data.remark
    db.commit()
    db.refresh(c)
    return _to_out(db, c)


@router.put("/{class_id}/status", response_model=ClassOut, summary="停用/启用班级")
def toggle_class(class_id: int, db: Session = Depends(get_db), _=Depends(manage)):
    c = db.query(Clazz).get(class_id)
    if c is None:
        raise HTTPException(status_code=404, detail="班级不存在")
    c.status = 0 if c.status == 1 else 1
    db.commit()
    db.refresh(c)
    return _to_out(db, c)


@router.delete("/{class_id}", response_model=MessageOut, summary="删除班级")
def delete_class(class_id: int, db: Session = Depends(get_db), _=Depends(manage)):
    c = db.query(Clazz).get(class_id)
    if c is None:
        raise HTTPException(status_code=404, detail="班级不存在")
    # 关联保护：仍有学生、排课或考勤时拒绝硬删
    refs = db.query(User).filter(User.class_id == class_id).count()
    refs += db.query(Schedule).filter(Schedule.class_id == class_id).count()
    refs += db.query(AttendanceTask).filter(AttendanceTask.class_id == class_id).count()
    if refs:
        raise HTTPException(status_code=400, detail="该班级下还有学生或排课/考勤，无法删除，请先移出学生或改用停用")
    db.delete(c)
    db.commit()
    return MessageOut(message="已删除")


@router.get("/{class_id}/students", response_model=list[UserOut], summary="班级名单")
def roster(class_id: int, keyword: str = "", db: Session = Depends(get_db), _=Depends(manage)):
    q = db.query(User).filter(User.class_id == class_id, User.role == ROLE_STUDENT)
    if keyword:
        like = f"%{keyword}%"
        q = q.filter((User.username.like(like)) | (User.name.like(like)))
    return q.order_by(User.username).all()


@router.post("/{class_id}/students", response_model=UserOut, summary="添加学生到名单")
def add_student(class_id: int, data: AddStudentIn, db: Session = Depends(get_db), _=Depends(manage)):
    c = db.query(Clazz).get(class_id)
    if c is None:
        raise HTTPException(status_code=404, detail="班级不存在")
    stu = db.query(User).filter(User.username == data.username, User.role == ROLE_STUDENT).first()
    if stu is None:
        raise HTTPException(status_code=404, detail="该学号学生不存在，请先让学生注册")
    if stu.class_id == class_id:
        raise HTTPException(status_code=400, detail="该学生已在本班名单中")
    stu.class_id = class_id
    db.commit()
    db.refresh(stu)
    return stu


@router.delete("/{class_id}/students/{student_id}", response_model=MessageOut, summary="移出名单")
def remove_student(class_id: int, student_id: int, db: Session = Depends(get_db), _=Depends(manage)):
    stu = db.query(User).filter(User.id == student_id, User.class_id == class_id).first()
    if stu is None:
        raise HTTPException(status_code=404, detail="该学生不在本班名单中")
    stu.class_id = None
    db.commit()
    return MessageOut(message="已移出班级名单")
