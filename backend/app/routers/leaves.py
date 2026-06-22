"""UC0401 请假申请、UC0402 请假审批。

审批通过后，自动把请假时段内、关联课程匹配的考勤记录覆盖为"请假"。
"""
from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import (
    ATT_ABSENT, ATT_LATE, ATT_LEAVE, ATT_LOCATION_ERROR,
    ROLE_ADMIN, ROLE_STUDENT, ROLE_TEACHER, ST_APPROVED, ST_PENDING, ST_REJECTED,
    AttendanceRecord, AttendanceTask, Course, LeaveRequest, Notification, User,
)
from ..schemas import ApproveIn, LeaveIn, LeaveOut
from ..security import require_role

router = APIRouter(prefix="/api/leaves", tags=["请假"])


def _leave_out(db: Session, lv: LeaveRequest) -> LeaveOut:
    stu = db.query(User).get(lv.student_id)
    course = db.query(Course).get(lv.course_id) if lv.course_id else None
    return LeaveOut(
        id=lv.id, student_id=lv.student_id, student_name=stu.name if stu else None,
        course_id=lv.course_id, course_name=course.name if course else None,
        type=lv.type, start_time=lv.start_time, end_time=lv.end_time, reason=lv.reason,
        status=lv.status, approve_comment=lv.approve_comment, approve_time=lv.approve_time,
    )


@router.post("", response_model=LeaveOut, summary="提交请假申请")
def create_leave(data: LeaveIn, db: Session = Depends(get_db),
                 student: User = Depends(require_role(ROLE_STUDENT))):
    if data.end_time < data.start_time:
        raise HTTPException(status_code=400, detail="结束时间不能早于开始时间")
    dup = db.query(LeaveRequest).filter(
        LeaveRequest.student_id == student.id,
        LeaveRequest.status == ST_PENDING,
        LeaveRequest.start_time == data.start_time,
        LeaveRequest.end_time == data.end_time,
    ).first()
    if dup:
        raise HTTPException(status_code=400, detail="已有相同的待审批请假，请勿重复提交")
    lv = LeaveRequest(
        student_id=student.id, course_id=data.course_id, type=data.type,
        start_time=data.start_time, end_time=data.end_time, reason=data.reason,
        status=ST_PENDING,
    )
    db.add(lv)
    db.commit()
    db.refresh(lv)
    return _leave_out(db, lv)


@router.get("/my", response_model=list[LeaveOut], summary="我的请假")
def my_leaves(db: Session = Depends(get_db), student: User = Depends(require_role(ROLE_STUDENT))):
    rows = db.query(LeaveRequest).filter(
        LeaveRequest.student_id == student.id).order_by(LeaveRequest.id.desc()).all()
    return [_leave_out(db, lv) for lv in rows]


@router.get("/pending", response_model=list[LeaveOut], summary="待审批请假")
def pending(db: Session = Depends(get_db), _=Depends(require_role(ROLE_TEACHER, ROLE_ADMIN))):
    rows = db.query(LeaveRequest).filter(
        LeaveRequest.status == ST_PENDING).order_by(LeaveRequest.id.desc()).all()
    return [_leave_out(db, lv) for lv in rows]


@router.post("/{leave_id}/approve", response_model=LeaveOut, summary="审批请假")
def approve(leave_id: int, data: ApproveIn, db: Session = Depends(get_db),
            teacher: User = Depends(require_role(ROLE_TEACHER, ROLE_ADMIN))):
    lv = db.query(LeaveRequest).get(leave_id)
    if lv is None:
        raise HTTPException(status_code=404, detail="请假申请不存在")
    if lv.status != ST_PENDING:
        raise HTTPException(status_code=400, detail="该申请已被处理")

    lv.status = ST_APPROVED if data.approve else ST_REJECTED
    lv.approver_id = teacher.id
    lv.approve_time = datetime.now()
    lv.approve_comment = data.comment

    if data.approve:
        # 覆盖请假时段内、课程匹配的考勤记录为 leave
        affected = db.query(AttendanceRecord).join(
            AttendanceTask, AttendanceRecord.task_id == AttendanceTask.id
        ).filter(
            AttendanceRecord.student_id == lv.student_id,
            AttendanceTask.start_time >= lv.start_time,
            AttendanceTask.start_time <= lv.end_time,
            AttendanceRecord.status.in_([ATT_ABSENT, ATT_LATE, ATT_LOCATION_ERROR]),
        )
        if lv.course_id:
            affected = affected.filter(AttendanceTask.course_id == lv.course_id)
        for r in affected.all():
            r.status = ATT_LEAVE
            r.remark = "请假审批通过"

    result = "通过" if data.approve else "驳回"
    db.add(Notification(
        user_id=lv.student_id, title=f"请假申请已{result}",
        content=data.comment or f"你的请假申请已{result}", type="leave",
    ))
    db.commit()
    db.refresh(lv)
    return _leave_out(db, lv)
