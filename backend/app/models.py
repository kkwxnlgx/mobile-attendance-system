"""数据库 ORM 模型，对应需求文档第 5 章领域模型的 9 张表。

设计要点：
- 状态字段统一用字符串常量（见各类 STATUS_*），不使用数据库 ENUM，
  方便课程阶段随时调整取值。
- 发起考勤时按班级名单批量预插考勤记录（初始 absent），签到只做 UPDATE，
  这样"已到/应到""提前结束自动缺勤""统计分组计数"都能自然成立。
"""
from datetime import datetime

from sqlalchemy import (
    BigInteger,
    Column,
    DateTime,
    Double,
    ForeignKey,
    Integer,
    SmallInteger,
    String,
    Time,
    UniqueConstraint,
)
from sqlalchemy.orm import relationship

from .database import Base

# MySQL 用 BIGINT 自增；SQLite 只对 INTEGER 主键自增，故用方言变体兼容两者。
BigId = BigInteger().with_variant(Integer, "sqlite")

# ---- 角色 ----
ROLE_STUDENT = "student"
ROLE_TEACHER = "teacher"
ROLE_ADMIN = "admin"

# ---- 考勤记录状态 ----
ATT_NORMAL = "normal"          # 正常
ATT_LATE = "late"              # 迟到
ATT_ABSENT = "absent"          # 缺勤
ATT_LEAVE = "leave"            # 请假
ATT_LOCATION_ERROR = "location_error"  # 位置异常

# ---- 审批状态 ----
ST_PENDING = "pending"
ST_APPROVED = "approved"
ST_REJECTED = "rejected"


class User(Base):
    __tablename__ = "users"

    id = Column(BigId, primary_key=True, autoincrement=True)
    username = Column(String(32), unique=True, nullable=False, index=True)  # 学号/工号/admin
    password_hash = Column(String(128), nullable=False)                     # salt$sha256
    name = Column(String(32), nullable=False)
    role = Column(String(10), nullable=False)                               # student/teacher/admin
    phone = Column(String(20), nullable=True)
    class_id = Column(BigId, ForeignKey("classes.id"), nullable=True)  # 学生归属班级
    status = Column(SmallInteger, default=1)                                # 1 启用 0 禁用
    created_at = Column(DateTime, default=datetime.now)

    clazz = relationship("Clazz", back_populates="students", foreign_keys=[class_id])


class Clazz(Base):
    __tablename__ = "classes"

    id = Column(BigId, primary_key=True, autoincrement=True)
    name = Column(String(64), unique=True, nullable=False)
    grade = Column(String(16), nullable=True)
    major = Column(String(64), nullable=True)
    remark = Column(String(255), nullable=True)
    status = Column(SmallInteger, default=1)        # 1 正常 0 停用（删除即停用）
    created_at = Column(DateTime, default=datetime.now)

    students = relationship("User", back_populates="clazz", foreign_keys="User.class_id")


class Course(Base):
    __tablename__ = "courses"

    id = Column(BigId, primary_key=True, autoincrement=True)
    name = Column(String(64), nullable=False)
    teacher_id = Column(BigId, ForeignKey("users.id"), nullable=False)
    semester = Column(String(32), nullable=True)
    created_at = Column(DateTime, default=datetime.now)

    teacher = relationship("User", foreign_keys=[teacher_id])


class Schedule(Base):
    __tablename__ = "schedules"

    id = Column(BigId, primary_key=True, autoincrement=True)
    course_id = Column(BigId, ForeignKey("courses.id"), nullable=False)
    class_id = Column(BigId, ForeignKey("classes.id"), nullable=False)
    weekday = Column(SmallInteger, nullable=False)          # 1-7（周一到周日）
    start_section = Column(SmallInteger, nullable=True)     # 起始节次
    end_section = Column(SmallInteger, nullable=True)       # 结束节次
    start_time = Column(Time, nullable=True)
    end_time = Column(Time, nullable=True)
    weeks = Column(String(64), nullable=True)              # 如 "1-16"
    location = Column(String(64), nullable=True)
    latitude = Column(Double, nullable=True)               # 默认考勤坐标
    longitude = Column(Double, nullable=True)
    created_at = Column(DateTime, default=datetime.now)

    course = relationship("Course", foreign_keys=[course_id])
    clazz = relationship("Clazz", foreign_keys=[class_id])


class AttendanceTask(Base):
    __tablename__ = "attendance_tasks"

    id = Column(BigId, primary_key=True, autoincrement=True)
    schedule_id = Column(BigId, ForeignKey("schedules.id"), nullable=True)
    course_id = Column(BigId, ForeignKey("courses.id"), nullable=False)
    class_id = Column(BigId, ForeignKey("classes.id"), nullable=False)
    teacher_id = Column(BigId, ForeignKey("users.id"), nullable=False)
    code = Column(String(8), nullable=False)               # 签到码
    start_time = Column(DateTime, nullable=False)
    end_time = Column(DateTime, nullable=False)
    late_offset_min = Column(Integer, default=10)          # 开始后 N 分钟内算正常
    latitude = Column(Double, nullable=True)
    longitude = Column(Double, nullable=True)
    radius_m = Column(Integer, default=300)
    status = Column(String(10), default="active")          # active / finished
    created_at = Column(DateTime, default=datetime.now)

    course = relationship("Course", foreign_keys=[course_id])
    clazz = relationship("Clazz", foreign_keys=[class_id])
    teacher = relationship("User", foreign_keys=[teacher_id])
    records = relationship("AttendanceRecord", back_populates="task")


class AttendanceRecord(Base):
    __tablename__ = "attendance_records"
    __table_args__ = (UniqueConstraint("task_id", "student_id", name="uq_task_student"),)

    id = Column(BigId, primary_key=True, autoincrement=True)
    task_id = Column(BigId, ForeignKey("attendance_tasks.id"), nullable=False)
    student_id = Column(BigId, ForeignKey("users.id"), nullable=False)
    checkin_time = Column(DateTime, nullable=True)
    latitude = Column(Double, nullable=True)
    longitude = Column(Double, nullable=True)
    distance_m = Column(Double, nullable=True)
    status = Column(String(20), default=ATT_ABSENT)
    remark = Column(String(255), nullable=True)            # 补卡/请假覆盖说明
    updated_at = Column(DateTime, default=datetime.now, onupdate=datetime.now)

    task = relationship("AttendanceTask", back_populates="records")
    student = relationship("User", foreign_keys=[student_id])


class LeaveRequest(Base):
    __tablename__ = "leave_requests"

    id = Column(BigId, primary_key=True, autoincrement=True)
    student_id = Column(BigId, ForeignKey("users.id"), nullable=False)
    course_id = Column(BigId, ForeignKey("courses.id"), nullable=True)  # 可选关联课程
    type = Column(String(10), nullable=False)              # sick/personal/other
    start_time = Column(DateTime, nullable=False)
    end_time = Column(DateTime, nullable=False)
    reason = Column(String(500), nullable=True)
    status = Column(String(10), default=ST_PENDING)
    approver_id = Column(BigId, ForeignKey("users.id"), nullable=True)
    approve_time = Column(DateTime, nullable=True)
    approve_comment = Column(String(255), nullable=True)
    created_at = Column(DateTime, default=datetime.now)

    student = relationship("User", foreign_keys=[student_id])
    course = relationship("Course", foreign_keys=[course_id])


class FixRequest(Base):
    __tablename__ = "fix_requests"

    id = Column(BigId, primary_key=True, autoincrement=True)
    record_id = Column(BigId, ForeignKey("attendance_records.id"), nullable=False)
    student_id = Column(BigId, ForeignKey("users.id"), nullable=False)
    reason_type = Column(String(10), nullable=False)       # forgot/gps_fail/other
    description = Column(String(500), nullable=True)
    status = Column(String(10), default=ST_PENDING)
    approver_id = Column(BigId, ForeignKey("users.id"), nullable=True)
    approve_time = Column(DateTime, nullable=True)
    approve_comment = Column(String(255), nullable=True)
    created_at = Column(DateTime, default=datetime.now)

    record = relationship("AttendanceRecord", foreign_keys=[record_id])
    student = relationship("User", foreign_keys=[student_id])


class Notification(Base):
    __tablename__ = "notifications"

    id = Column(BigId, primary_key=True, autoincrement=True)
    user_id = Column(BigId, ForeignKey("users.id"), nullable=False)   # 接收人
    title = Column(String(64), nullable=False)
    content = Column(String(255), nullable=True)
    type = Column(String(20), nullable=True)               # attendance/leave/fix
    is_read = Column(SmallInteger, default=0)
    created_at = Column(DateTime, default=datetime.now)
