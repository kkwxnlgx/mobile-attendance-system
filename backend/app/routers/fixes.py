"""UC0304 补卡与异常申诉。"""
from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import (
    ATT_NORMAL, ROLE_ADMIN, ROLE_STUDENT, ROLE_TEACHER,
    ST_APPROVED, ST_PENDING, ST_REJECTED,
    AttendanceRecord, AttendanceTask, Course, FixRequest, Notification, User,
)
from ..schemas import ApproveIn, FixIn, FixOut
from ..security import require_role

router = APIRouter(prefix="/api/fixes", tags=["补卡申诉"])


def _fix_out(db: Session, f: FixRequest) -> FixOut:
    stu = db.query(User).get(f.student_id)
    rec = db.query(AttendanceRecord).get(f.record_id)
    task = db.query(AttendanceTask).get(rec.task_id) if rec else None
    course = db.query(Course).get(task.course_id) if task else None
    return FixOut(
        id=f.id, record_id=f.record_id, student_id=f.student_id,
        student_name=stu.name if stu else None,
        course_name=course.name if course else None,
        reason_type=f.reason_type, description=f.description, status=f.status,
        approve_comment=f.approve_comment,
        task_time=task.start_time if task else None,
    )


@router.post("", response_model=FixOut, summary="提交补卡申请")
def create_fix(data: FixIn, db: Session = Depends(get_db),
               student: User = Depends(require_role(ROLE_STUDENT))):
    rec = db.query(AttendanceRecord).get(data.record_id)
    if rec is None or rec.student_id != student.id:
        raise HTTPException(status_code=404, detail="考勤记录不存在")
    dup = db.query(FixRequest).filter(
        FixRequest.record_id == data.record_id, FixRequest.status == ST_PENDING).first()
    if dup:
        raise HTTPException(status_code=400, detail="该记录已有待审批补卡申请")
    f = FixRequest(
        record_id=data.record_id, student_id=student.id,
        reason_type=data.reason_type, description=data.description, status=ST_PENDING,
    )
    db.add(f)
    db.commit()
    db.refresh(f)
    return _fix_out(db, f)


@router.get("/my", response_model=list[FixOut], summary="我的补卡")
def my_fixes(db: Session = Depends(get_db), student: User = Depends(require_role(ROLE_STUDENT))):
    rows = db.query(FixRequest).filter(
        FixRequest.student_id == student.id).order_by(FixRequest.id.desc()).all()
    return [_fix_out(db, f) for f in rows]


@router.get("/pending", response_model=list[FixOut], summary="待审批补卡")
def pending(db: Session = Depends(get_db), _=Depends(require_role(ROLE_TEACHER, ROLE_ADMIN))):
    rows = db.query(FixRequest).filter(
        FixRequest.status == ST_PENDING).order_by(FixRequest.id.desc()).all()
    return [_fix_out(db, f) for f in rows]


@router.post("/{fix_id}/approve", response_model=FixOut, summary="审批补卡")
def approve(fix_id: int, data: ApproveIn, db: Session = Depends(get_db),
            teacher: User = Depends(require_role(ROLE_TEACHER, ROLE_ADMIN))):
    f = db.query(FixRequest).get(fix_id)
    if f is None:
        raise HTTPException(status_code=404, detail="补卡申请不存在")
    if f.status != ST_PENDING:
        raise HTTPException(status_code=400, detail="该申请已被处理")
    f.status = ST_APPROVED if data.approve else ST_REJECTED
    f.approver_id = teacher.id
    f.approve_time = datetime.now()
    f.approve_comment = data.comment

    if data.approve:
        rec = db.query(AttendanceRecord).get(f.record_id)
        if rec:
            rec.status = ATT_NORMAL
            rec.remark = "补卡通过：" + (data.comment or "")

    result = "通过" if data.approve else "驳回"
    db.add(Notification(
        user_id=f.student_id, title=f"补卡申请已{result}",
        content=data.comment or f"你的补卡申请已{result}", type="fix",
    ))
    db.commit()
    db.refresh(f)
    return _fix_out(db, f)
