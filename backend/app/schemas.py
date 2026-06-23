"""Pydantic 请求/响应模型。字段命名与 Android 端 POJO 保持一致。"""
from datetime import datetime, time
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field

ORM = ConfigDict(from_attributes=True)


# ---------------------------------------------------------------------------
# 认证
# ---------------------------------------------------------------------------
class RegisterIn(BaseModel):
    username: str = Field(min_length=2, max_length=32)   # 学号
    name: str = Field(min_length=1, max_length=32)
    password: str = Field(min_length=6, max_length=64)
    phone: Optional[str] = None


class LoginIn(BaseModel):
    username: str
    password: str


class ChangePasswordIn(BaseModel):
    """用户自助改密：校验原密码后更新。"""
    old_password: str
    new_password: str = Field(min_length=6, max_length=64)


class LoginOut(BaseModel):
    token: str
    user_id: int
    username: str
    name: str
    role: str
    class_id: Optional[int] = None


class UserOut(BaseModel):
    model_config = ORM
    id: int
    username: str
    name: str
    role: str
    phone: Optional[str] = None
    class_id: Optional[int] = None
    status: int


class AdminUserIn(BaseModel):
    """管理员新增用户：仅限学生/教师。"""
    username: str = Field(min_length=2, max_length=32)   # 学号/工号
    name: str = Field(min_length=1, max_length=32)
    password: str = Field(min_length=6, max_length=64)
    role: str                                            # student / teacher
    phone: Optional[str] = None
    class_id: Optional[int] = None                       # 学生可选归属班级


class AdminUserUpdateIn(BaseModel):
    """管理员编辑用户：账号与角色不可改；password 留空表示不修改。"""
    name: Optional[str] = Field(default=None, max_length=32)
    phone: Optional[str] = None
    password: Optional[str] = Field(default=None, min_length=6, max_length=64)


# ---------------------------------------------------------------------------
# 班级 / 课程 / 课程表 / 名单
# ---------------------------------------------------------------------------
class ClassIn(BaseModel):
    name: str
    grade: Optional[str] = None
    major: Optional[str] = None
    remark: Optional[str] = None


class ClassOut(BaseModel):
    model_config = ORM
    id: int
    name: str
    grade: Optional[str] = None
    major: Optional[str] = None
    remark: Optional[str] = None
    status: int
    student_count: int = 0


class CourseIn(BaseModel):
    name: str
    semester: Optional[str] = None
    teacher_id: Optional[int] = None   # 管理员可指定，教师默认自己


class CourseOut(BaseModel):
    model_config = ORM
    id: int
    name: str
    teacher_id: int
    teacher_name: Optional[str] = None
    semester: Optional[str] = None


class ScheduleIn(BaseModel):
    course_id: int
    class_id: int
    weekday: int = Field(ge=1, le=7)
    start_section: Optional[int] = None
    end_section: Optional[int] = None
    start_time: Optional[time] = None
    end_time: Optional[time] = None
    weeks: Optional[str] = None
    location: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None


class ScheduleOut(BaseModel):
    model_config = ORM
    id: int
    course_id: int
    course_name: Optional[str] = None
    class_id: int
    class_name: Optional[str] = None
    weekday: int
    start_section: Optional[int] = None
    end_section: Optional[int] = None
    start_time: Optional[time] = None
    end_time: Optional[time] = None
    weeks: Optional[str] = None
    location: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None


class AddStudentIn(BaseModel):
    username: str   # 按学号添加


# ---------------------------------------------------------------------------
# 考勤
# ---------------------------------------------------------------------------
class LaunchTaskIn(BaseModel):
    schedule_id: Optional[int] = None
    course_id: Optional[int] = None
    class_id: Optional[int] = None
    duration_min: int = 30
    late_offset_min: int = 10
    radius_m: int = 300
    latitude: Optional[float] = None
    longitude: Optional[float] = None


class TaskOut(BaseModel):
    model_config = ORM
    id: int
    course_id: int
    course_name: Optional[str] = None
    class_id: int
    class_name: Optional[str] = None
    code: str
    start_time: datetime
    end_time: datetime
    late_offset_min: int
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    radius_m: int
    status: str


class CheckinIn(BaseModel):
    code: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None


class CheckinOut(BaseModel):
    status: str               # normal/late/location_error
    distance_m: Optional[float] = None
    radius_m: Optional[int] = None
    message: str


class RecordOut(BaseModel):
    model_config = ORM
    id: int
    task_id: int
    student_id: int
    student_name: Optional[str] = None
    student_username: Optional[str] = None
    course_name: Optional[str] = None
    class_name: Optional[str] = None
    checkin_time: Optional[datetime] = None
    distance_m: Optional[float] = None
    status: str
    remark: Optional[str] = None
    task_time: Optional[datetime] = None   # 该次考勤的开始时间，便于按日期展示


class MonitorOut(BaseModel):
    task: TaskOut
    total: int
    normal: int
    late: int
    absent: int
    leave: int
    location_error: int
    records: list[RecordOut]


# ---------------------------------------------------------------------------
# 请假 / 补卡
# ---------------------------------------------------------------------------
class LeaveIn(BaseModel):
    type: str                      # sick/personal/other
    start_time: datetime
    end_time: datetime
    reason: Optional[str] = None
    course_id: Optional[int] = None


class LeaveOut(BaseModel):
    model_config = ORM
    id: int
    student_id: int
    student_name: Optional[str] = None
    course_id: Optional[int] = None
    course_name: Optional[str] = None
    type: str
    start_time: datetime
    end_time: datetime
    reason: Optional[str] = None
    status: str
    approve_comment: Optional[str] = None
    approve_time: Optional[datetime] = None


class FixIn(BaseModel):
    record_id: int
    reason_type: str               # forgot/gps_fail/other
    description: Optional[str] = None


class FixOut(BaseModel):
    model_config = ORM
    id: int
    record_id: int
    student_id: int
    student_name: Optional[str] = None
    course_name: Optional[str] = None
    reason_type: str
    description: Optional[str] = None
    status: str
    approve_comment: Optional[str] = None
    task_time: Optional[datetime] = None


class ApproveIn(BaseModel):
    approve: bool
    comment: Optional[str] = None


# ---------------------------------------------------------------------------
# 统计 / 通知
# ---------------------------------------------------------------------------
class StatStudentRow(BaseModel):
    student_id: int
    username: str
    name: str
    normal: int
    late: int
    absent: int
    leave: int
    location_error: int
    total: int
    attend_rate: float


class StatSummaryOut(BaseModel):
    total: int
    normal: int
    late: int
    absent: int
    leave: int
    location_error: int
    attend_rate: float             # (normal+late)/total
    students: list[StatStudentRow]


class NotificationOut(BaseModel):
    model_config = ORM
    id: int
    title: str
    content: Optional[str] = None
    type: Optional[str] = None
    is_read: int
    created_at: datetime


class MessageOut(BaseModel):
    message: str
