"""UC0501 考勤统计与筛选。"""
from datetime import datetime

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import (
    ATT_ABSENT, ATT_LATE, ATT_LEAVE, ATT_LOCATION_ERROR, ATT_NORMAL,
    ROLE_ADMIN, ROLE_TEACHER,
    AttendanceRecord, AttendanceTask, User,
)
from ..schemas import StatStudentRow, StatSummaryOut
from ..security import require_role

router = APIRouter(prefix="/api/stats", tags=["统计"])


def _rate(normal: int, late: int, total: int) -> float:
    return round((normal + late) / total * 100, 1) if total else 0.0


@router.get("/summary", response_model=StatSummaryOut, summary="考勤统计汇总")
def summary(class_id: int = None, course_id: int = None,
            start_date: str = None, end_date: str = None,
            db: Session = Depends(get_db),
            _=Depends(require_role(ROLE_TEACHER, ROLE_ADMIN))):
    q = db.query(AttendanceRecord).join(
        AttendanceTask, AttendanceRecord.task_id == AttendanceTask.id)
    if class_id:
        q = q.filter(AttendanceTask.class_id == class_id)
    if course_id:
        q = q.filter(AttendanceTask.course_id == course_id)
    if start_date:
        q = q.filter(AttendanceTask.start_time >= datetime.fromisoformat(start_date + "T00:00:00"))
    if end_date:
        q = q.filter(AttendanceTask.start_time <= datetime.fromisoformat(end_date + "T23:59:59"))
    records = q.all()

    total = len(records)
    agg = {ATT_NORMAL: 0, ATT_LATE: 0, ATT_ABSENT: 0, ATT_LEAVE: 0, ATT_LOCATION_ERROR: 0}
    per_student: dict[int, dict] = {}
    for r in records:
        agg[r.status] = agg.get(r.status, 0) + 1
        row = per_student.setdefault(r.student_id, {
            ATT_NORMAL: 0, ATT_LATE: 0, ATT_ABSENT: 0, ATT_LEAVE: 0, ATT_LOCATION_ERROR: 0})
        row[r.status] = row.get(r.status, 0) + 1

    students = []
    for sid, row in per_student.items():
        stu = db.query(User).get(sid)
        stu_total = sum(row.values())
        students.append(StatStudentRow(
            student_id=sid, username=stu.username if stu else str(sid),
            name=stu.name if stu else "", normal=row[ATT_NORMAL], late=row[ATT_LATE],
            absent=row[ATT_ABSENT], leave=row[ATT_LEAVE],
            location_error=row[ATT_LOCATION_ERROR], total=stu_total,
            attend_rate=_rate(row[ATT_NORMAL], row[ATT_LATE], stu_total),
        ))
    students.sort(key=lambda s: s.username)

    return StatSummaryOut(
        total=total, normal=agg[ATT_NORMAL], late=agg[ATT_LATE], absent=agg[ATT_ABSENT],
        leave=agg[ATT_LEAVE], location_error=agg[ATT_LOCATION_ERROR],
        attend_rate=_rate(agg[ATT_NORMAL], agg[ATT_LATE], total), students=students,
    )
