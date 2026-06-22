"""UC0301 发起考勤、UC0302 学生定位签到、UC0303 记录查询、考勤监控。

核心：所有时间/距离判定都在服务端用服务器时间完成，客户端不做判断。
"""
from datetime import datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import (
    ATT_ABSENT, ATT_LATE, ATT_LEAVE, ATT_LOCATION_ERROR, ATT_NORMAL,
    ROLE_ADMIN, ROLE_STUDENT, ROLE_TEACHER, ST_APPROVED,
    AttendanceRecord, AttendanceTask, Clazz, Course, LeaveRequest,
    Notification, Schedule, User,
)
from ..schemas import (
    CheckinIn, CheckinOut, LaunchTaskIn, MonitorOut, RecordOut, TaskOut,
)
from ..security import get_current_user, require_role
from ..utils import gen_checkin_code, haversine

router = APIRouter(prefix="/api/attendance", tags=["课堂考勤"])


def _task_out(db: Session, t: AttendanceTask) -> TaskOut:
    course = db.query(Course).get(t.course_id)
    clazz = db.query(Clazz).get(t.class_id)
    return TaskOut(
        id=t.id, course_id=t.course_id, course_name=course.name if course else None,
        class_id=t.class_id, class_name=clazz.name if clazz else None,
        code=t.code, start_time=t.start_time, end_time=t.end_time,
        late_offset_min=t.late_offset_min, latitude=t.latitude, longitude=t.longitude,
        radius_m=t.radius_m, status=t.status,
    )


def _record_out(db: Session, r: AttendanceRecord) -> RecordOut:
    stu = db.query(User).get(r.student_id)
    task = db.query(AttendanceTask).get(r.task_id)
    course = db.query(Course).get(task.course_id) if task else None
    clazz = db.query(Clazz).get(task.class_id) if task else None
    return RecordOut(
        id=r.id, task_id=r.task_id, student_id=r.student_id,
        student_name=stu.name if stu else None,
        student_username=stu.username if stu else None,
        course_name=course.name if course else None,
        class_name=clazz.name if clazz else None,
        checkin_time=r.checkin_time, distance_m=r.distance_m,
        status=r.status, remark=r.remark,
        task_time=task.start_time if task else None,
    )


@router.post("/tasks", response_model=TaskOut, summary="发起课堂考勤")
def launch_task(data: LaunchTaskIn, db: Session = Depends(get_db),
                teacher: User = Depends(require_role(ROLE_TEACHER, ROLE_ADMIN))):
    course_id, class_id = data.course_id, data.class_id
    lat, lng = data.latitude, data.longitude

    # 从课程表带出课程、班级、默认坐标
    if data.schedule_id:
        sch = db.query(Schedule).get(data.schedule_id)
        if sch is None:
            raise HTTPException(status_code=404, detail="课程表不存在")
        course_id = course_id or sch.course_id
        class_id = class_id or sch.class_id
        if lat is None or lng is None:
            lat, lng = sch.latitude, sch.longitude
    if not course_id or not class_id:
        raise HTTPException(status_code=400, detail="缺少课程或班级信息")
    if lat is None or lng is None:
        raise HTTPException(status_code=400, detail="请补充考勤地点坐标")

    # 班级名单
    students = db.query(User).filter(
        User.class_id == class_id, User.role == ROLE_STUDENT, User.status == 1
    ).all()
    if not students:
        raise HTTPException(status_code=400, detail="该班级名单为空，请先维护学生名单")

    # 生成不与现存进行中任务冲突的签到码
    active_codes = {t.code for t in db.query(AttendanceTask).filter(
        AttendanceTask.status == "active").all()}
    code = gen_checkin_code()
    while code in active_codes:
        code = gen_checkin_code()

    now = datetime.now()
    task = AttendanceTask(
        schedule_id=data.schedule_id, course_id=course_id, class_id=class_id,
        teacher_id=teacher.id, code=code, start_time=now,
        end_time=now + timedelta(minutes=data.duration_min),
        late_offset_min=data.late_offset_min, latitude=lat, longitude=lng,
        radius_m=data.radius_m, status="active",
    )
    db.add(task)
    db.flush()   # 拿到 task.id

    course = db.query(Course).get(course_id)
    course_name = course.name if course else "课程"

    # 按名单预插记录：已通过且覆盖当前时段的请假 → leave，否则 absent
    for stu in students:
        on_leave = db.query(LeaveRequest).filter(
            LeaveRequest.student_id == stu.id,
            LeaveRequest.status == ST_APPROVED,
            LeaveRequest.start_time <= now,
            LeaveRequest.end_time >= now,
        ).first()
        if on_leave and (on_leave.course_id is None or on_leave.course_id == course_id):
            status, remark = ATT_LEAVE, "请假审批通过"
        else:
            status, remark = ATT_ABSENT, None
        db.add(AttendanceRecord(task_id=task.id, student_id=stu.id, status=status, remark=remark))
        db.add(Notification(
            user_id=stu.id, title=f"《{course_name}》开始签到",
            content=f"签到码 {code}，有效至 {task.end_time.strftime('%H:%M')}",
            type="attendance",
        ))

    db.commit()
    db.refresh(task)
    return _task_out(db, task)


@router.get("/tasks/active", response_model=list[TaskOut], summary="学生获取进行中考勤")
def active_tasks(db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    if user.role != ROLE_STUDENT or not user.class_id:
        return []
    now = datetime.now()
    tasks = db.query(AttendanceTask).filter(
        AttendanceTask.class_id == user.class_id,
        AttendanceTask.status == "active",
        AttendanceTask.end_time >= now,
    ).order_by(AttendanceTask.id.desc()).all()
    return [_task_out(db, t) for t in tasks]


@router.get("/tasks/mine_active", response_model=list[TaskOut], summary="教师进行中的考勤")
def mine_active_tasks(db: Session = Depends(get_db),
                      teacher: User = Depends(require_role(ROLE_TEACHER, ROLE_ADMIN))):
    now = datetime.now()
    q = db.query(AttendanceTask).filter(
        AttendanceTask.status == "active", AttendanceTask.end_time >= now)
    if teacher.role == ROLE_TEACHER:
        q = q.filter(AttendanceTask.teacher_id == teacher.id)
    return [_task_out(db, t) for t in q.order_by(AttendanceTask.id.desc()).all()]


@router.get("/tasks/{task_id}/monitor", response_model=MonitorOut, summary="考勤监控")
def monitor(task_id: int, db: Session = Depends(get_db),
            _=Depends(require_role(ROLE_TEACHER, ROLE_ADMIN))):
    task = db.query(AttendanceTask).get(task_id)
    if task is None:
        raise HTTPException(status_code=404, detail="考勤任务不存在")
    records = db.query(AttendanceRecord).filter(AttendanceRecord.task_id == task_id).all()
    counts = {ATT_NORMAL: 0, ATT_LATE: 0, ATT_ABSENT: 0, ATT_LEAVE: 0, ATT_LOCATION_ERROR: 0}
    for r in records:
        counts[r.status] = counts.get(r.status, 0) + 1
    return MonitorOut(
        task=_task_out(db, task), total=len(records),
        normal=counts[ATT_NORMAL], late=counts[ATT_LATE], absent=counts[ATT_ABSENT],
        leave=counts[ATT_LEAVE], location_error=counts[ATT_LOCATION_ERROR],
        records=[_record_out(db, r) for r in records],
    )


@router.post("/tasks/{task_id}/finish", response_model=TaskOut, summary="结束考勤")
def finish_task(task_id: int, db: Session = Depends(get_db),
                _=Depends(require_role(ROLE_TEACHER, ROLE_ADMIN))):
    task = db.query(AttendanceTask).get(task_id)
    if task is None:
        raise HTTPException(status_code=404, detail="考勤任务不存在")
    task.status = "finished"
    # 未签到的记录保持 absent（预插时即为 absent），无需额外处理
    db.commit()
    db.refresh(task)
    return _task_out(db, task)


@router.post("/checkin", response_model=CheckinOut, summary="学生定位签到")
def checkin(data: CheckinIn, db: Session = Depends(get_db),
            student: User = Depends(require_role(ROLE_STUDENT))):
    now = datetime.now()
    task = db.query(AttendanceTask).filter(
        AttendanceTask.code == data.code.strip().upper(),
        AttendanceTask.status == "active",
    ).first()
    if task is None:
        raise HTTPException(status_code=400, detail="签到码错误或考勤未开始")
    if now > task.end_time:
        raise HTTPException(status_code=400, detail="签到已超过有效时间")
    if student.class_id != task.class_id:
        raise HTTPException(status_code=403, detail="你不在本次考勤名单中")

    record = db.query(AttendanceRecord).filter(
        AttendanceRecord.task_id == task.id,
        AttendanceRecord.student_id == student.id,
    ).first()
    if record is None:
        # 理论上发起时已预插；名单后补的学生兜底插入
        record = AttendanceRecord(task_id=task.id, student_id=student.id, status=ATT_ABSENT)
        db.add(record)
        db.flush()
    if record.status != ATT_ABSENT:
        if record.status == ATT_LEAVE:
            raise HTTPException(status_code=400, detail="你在本次考勤已请假，无需签到")
        raise HTTPException(status_code=400, detail="请勿重复签到")

    dist = haversine(data.latitude, data.longitude, task.latitude, task.longitude)
    if data.latitude is not None and dist > task.radius_m:
        status, msg = ATT_LOCATION_ERROR, f"位置异常：距考勤点约 {dist:.0f} 米，超出 {task.radius_m} 米范围"
    elif now <= task.start_time + timedelta(minutes=task.late_offset_min):
        status, msg = ATT_NORMAL, "签到成功"
    else:
        status, msg = ATT_LATE, "签到成功（迟到）"

    record.status = status
    record.checkin_time = now
    record.latitude = data.latitude
    record.longitude = data.longitude
    record.distance_m = round(dist, 1)
    db.commit()
    return CheckinOut(status=status, distance_m=round(dist, 1), radius_m=task.radius_m, message=msg)


@router.get("/records/my", response_model=list[RecordOut], summary="我的考勤记录")
def my_records(status: str = None, course_id: int = None,
               db: Session = Depends(get_db), student: User = Depends(require_role(ROLE_STUDENT))):
    q = db.query(AttendanceRecord).filter(AttendanceRecord.student_id == student.id)
    if status:
        q = q.filter(AttendanceRecord.status == status)
    records = q.all()
    out = [_record_out(db, r) for r in records]
    if course_id:
        # course_id 过滤在出参上做（task 关联课程）
        task_ids = {t.id for t in db.query(AttendanceTask).filter(
            AttendanceTask.course_id == course_id).all()}
        out = [o for o in out if o.task_id in task_ids]
    out.sort(key=lambda o: o.task_time or datetime.min, reverse=True)
    return out


@router.get("/records", response_model=list[RecordOut], summary="教师按条件查记录")
def query_records(class_id: int = None, course_id: int = None, status: str = None,
                  db: Session = Depends(get_db),
                  _=Depends(require_role(ROLE_TEACHER, ROLE_ADMIN))):
    q = db.query(AttendanceRecord).join(
        AttendanceTask, AttendanceRecord.task_id == AttendanceTask.id)
    if class_id:
        q = q.filter(AttendanceTask.class_id == class_id)
    if course_id:
        q = q.filter(AttendanceTask.course_id == course_id)
    if status:
        q = q.filter(AttendanceRecord.status == status)
    records = q.order_by(AttendanceTask.start_time.desc()).all()
    return [_record_out(db, r) for r in records]
