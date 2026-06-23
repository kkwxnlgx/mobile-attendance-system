"""UC0202 课程与课程表设置。"""
from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import (
    ROLE_ADMIN, ROLE_STUDENT, ROLE_TEACHER,
    AttendanceTask, Course, Schedule, User,
)
from ..schemas import CourseIn, CourseOut, MessageOut, ScheduleIn, ScheduleOut
from ..security import get_current_user, require_role

router = APIRouter(prefix="/api", tags=["课程与课程表"])

manage = require_role(ROLE_TEACHER, ROLE_ADMIN)   # 读取/排课：教师与管理员
admin_only = require_role(ROLE_ADMIN)              # 课程基础数据增删改：仅管理员


def _course_out(db: Session, c: Course) -> CourseOut:
    t = db.query(User).get(c.teacher_id)
    return CourseOut(id=c.id, name=c.name, teacher_id=c.teacher_id,
                     teacher_name=t.name if t else None, semester=c.semester)


def _schedule_out(db: Session, s: Schedule) -> ScheduleOut:
    course = db.query(Course).get(s.course_id)
    from ..models import Clazz
    clazz = db.query(Clazz).get(s.class_id)
    return ScheduleOut(
        id=s.id, course_id=s.course_id, course_name=course.name if course else None,
        class_id=s.class_id, class_name=clazz.name if clazz else None,
        weekday=s.weekday, start_section=s.start_section, end_section=s.end_section,
        start_time=s.start_time, end_time=s.end_time, weeks=s.weeks,
        location=s.location, latitude=s.latitude, longitude=s.longitude,
    )


# ---- 课程 ----
@router.get("/courses", response_model=list[CourseOut], summary="课程列表")
def list_courses(db: Session = Depends(get_db), user: User = Depends(manage)):
    q = db.query(Course)
    if user.role == ROLE_TEACHER:
        q = q.filter(Course.teacher_id == user.id)
    return [_course_out(db, c) for c in q.order_by(Course.id).all()]


@router.post("/courses", response_model=CourseOut, summary="新增课程")
def create_course(data: CourseIn, db: Session = Depends(get_db), _=Depends(admin_only)):
    if not data.teacher_id:
        raise HTTPException(status_code=400, detail="请指定授课教师")
    teacher = db.query(User).filter(User.id == data.teacher_id, User.role == ROLE_TEACHER).first()
    if teacher is None:
        raise HTTPException(status_code=400, detail="授课教师不存在")
    c = Course(name=data.name, teacher_id=data.teacher_id, semester=data.semester)
    db.add(c)
    db.commit()
    db.refresh(c)
    return _course_out(db, c)


@router.put("/courses/{cid}", response_model=CourseOut, summary="编辑课程")
def update_course(cid: int, data: CourseIn, db: Session = Depends(get_db), _=Depends(admin_only)):
    c = db.query(Course).get(cid)
    if c is None:
        raise HTTPException(status_code=404, detail="课程不存在")
    c.name = data.name
    c.semester = data.semester
    if data.teacher_id:
        teacher = db.query(User).filter(User.id == data.teacher_id, User.role == ROLE_TEACHER).first()
        if teacher is None:
            raise HTTPException(status_code=400, detail="授课教师不存在")
        c.teacher_id = data.teacher_id
    db.commit()
    db.refresh(c)
    return _course_out(db, c)


@router.delete("/courses/{cid}", response_model=MessageOut, summary="删除课程")
def delete_course(cid: int, db: Session = Depends(get_db), _=Depends(admin_only)):
    c = db.query(Course).get(cid)
    if c is None:
        raise HTTPException(status_code=404, detail="课程不存在")
    # 关联保护：已有排课或考勤时拒绝硬删
    refs = db.query(Schedule).filter(Schedule.course_id == cid).count()
    refs += db.query(AttendanceTask).filter(AttendanceTask.course_id == cid).count()
    if refs:
        raise HTTPException(status_code=400, detail="该课程已有排课或考勤记录，无法删除")
    db.delete(c)
    db.commit()
    return MessageOut(message="已删除")


# ---- 课程表 ----
@router.get("/schedules", response_model=list[ScheduleOut], summary="课程表列表")
def list_schedules(class_id: int = None, db: Session = Depends(get_db), user: User = Depends(manage)):
    q = db.query(Schedule)
    if class_id:
        q = q.filter(Schedule.class_id == class_id)
    if user.role == ROLE_TEACHER:
        course_ids = [c.id for c in db.query(Course).filter(Course.teacher_id == user.id).all()]
        q = q.filter(Schedule.course_id.in_(course_ids or [-1]))
    return [_schedule_out(db, s) for s in q.order_by(Schedule.weekday, Schedule.start_section).all()]


@router.post("/schedules", response_model=ScheduleOut, summary="新增课程表")
def create_schedule(data: ScheduleIn, db: Session = Depends(get_db), _=Depends(manage)):
    s = Schedule(**data.model_dump())
    db.add(s)
    db.commit()
    db.refresh(s)
    return _schedule_out(db, s)


@router.put("/schedules/{sid}", response_model=ScheduleOut, summary="编辑课程表")
def update_schedule(sid: int, data: ScheduleIn, db: Session = Depends(get_db), _=Depends(manage)):
    s = db.query(Schedule).get(sid)
    if s is None:
        raise HTTPException(status_code=404, detail="课程表不存在")
    for k, v in data.model_dump().items():
        setattr(s, k, v)
    db.commit()
    db.refresh(s)
    return _schedule_out(db, s)


@router.get("/schedules/today", response_model=list[ScheduleOut], summary="今日课程")
def today(db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    weekday = datetime.now().isoweekday()   # 1-7
    q = db.query(Schedule).filter(Schedule.weekday == weekday)
    if user.role == ROLE_STUDENT:
        if not user.class_id:
            return []
        q = q.filter(Schedule.class_id == user.class_id)
    elif user.role == ROLE_TEACHER:
        course_ids = [c.id for c in db.query(Course).filter(Course.teacher_id == user.id).all()]
        q = q.filter(Schedule.course_id.in_(course_ids or [-1]))
    return [_schedule_out(db, s) for s in q.order_by(Schedule.start_section).all()]
