"""建库 + 建表 + 种子数据（幂等，可重复运行）。

运行：
    cd backend
    python -m app.init_db

行为：
1. 若用 MySQL，先用裸连接 CREATE DATABASE IF NOT EXISTS（utf8mb4）。
2. create_all 建表。
3. users 表已有数据则跳过种子；否则灌入一套可直接答辩演示的数据。
"""
from datetime import datetime, time, timedelta

from . import config
from .database import Base, SessionLocal, engine
from .models import (
    ATT_ABSENT, ATT_LATE, ATT_LEAVE, ATT_LOCATION_ERROR, ATT_NORMAL,
    ROLE_ADMIN, ROLE_STUDENT, ROLE_TEACHER, ST_APPROVED, ST_PENDING,
    AttendanceRecord, AttendanceTask, Clazz, Course, FixRequest,
    LeaveRequest, Notification, Schedule, User,
)
from .utils import hash_password


def ensure_database():
    """MySQL：确保目标库存在。SQLite 无需处理。"""
    if config.DB_BACKEND == "sqlite":
        return
    import pymysql

    conn = pymysql.connect(
        host=config.MYSQL_HOST, port=config.MYSQL_PORT,
        user=config.MYSQL_USER, password=config.MYSQL_PASSWORD,
    )
    try:
        with conn.cursor() as cur:
            cur.execute(
                f"CREATE DATABASE IF NOT EXISTS {config.MYSQL_DB} "
                f"CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            )
        conn.commit()
    finally:
        conn.close()


def seed(db):
    if db.query(User).first():
        print("已存在用户数据，跳过种子。")
        return

    pwd = hash_password("123456")
    admin_pwd = hash_password("admin123")

    # ---- 用户：管理员、教师 ----
    admin = User(username="admin", password_hash=admin_pwd, name="系统管理员", role=ROLE_ADMIN)
    t1 = User(username="t1001", password_hash=pwd, name="张老师", role=ROLE_TEACHER, phone="13800000001")
    t2 = User(username="t1002", password_hash=pwd, name="李老师", role=ROLE_TEACHER, phone="13800000002")
    db.add_all([admin, t1, t2])
    db.flush()

    # ---- 班级 ----
    clazz = Clazz(name="软件工程2023-1班", grade="2023级", major="软件工程", remark="移动开发大作业演示班级")
    db.add(clazz)
    db.flush()

    # ---- 学生（绑定班级） ----
    names = ["王小明", "李华", "张伟", "刘洋", "陈静", "赵磊", "孙悦", "周杰"]
    students = []
    for i, nm in enumerate(names, start=1):
        s = User(username=f"s2023{i:03d}", password_hash=pwd, name=nm,
                 role=ROLE_STUDENT, class_id=clazz.id, phone=f"139000000{i:02d}")
        students.append(s)
    db.add_all(students)
    db.flush()

    # ---- 课程 ----
    c1 = Course(name="移动应用开发", teacher_id=t1.id, semester="2025-2026-2")
    c2 = Course(name="数据库原理", teacher_id=t1.id, semester="2025-2026-2")
    c3 = Course(name="Web程序设计", teacher_id=t2.id, semester="2025-2026-2")
    db.add_all([c1, c2, c3])
    db.flush()

    # ---- 课程表：覆盖周一到周五，保证答辩当天"今日课程"非空 ----
    lat, lng = config.DEMO_LAT, config.DEMO_LNG
    schedule_plan = [
        (c1, 1, 1, 2, time(8, 0), time(9, 40), "实训楼A301"),
        (c2, 2, 3, 4, time(10, 0), time(11, 40), "教学楼B202"),
        (c3, 3, 1, 2, time(8, 0), time(9, 40), "实训楼A305"),
        (c1, 4, 3, 4, time(10, 0), time(11, 40), "实训楼A301"),
        (c2, 5, 5, 6, time(14, 0), time(15, 40), "教学楼B202"),
        (c1, 6, 1, 2, time(8, 0), time(9, 40), "实训楼A301"),
        (c3, 7, 3, 4, time(10, 0), time(11, 40), "实训楼A305"),
    ]
    schedules = []
    for course, wd, s1, s2, st, et, loc in schedule_plan:
        sch = Schedule(course_id=course.id, class_id=clazz.id, weekday=wd,
                       start_section=s1, end_section=s2, start_time=st, end_time=et,
                       weeks="1-16", location=loc, latitude=lat, longitude=lng)
        schedules.append(sch)
    db.add_all(schedules)
    db.flush()

    # ---- 历史考勤任务 + 记录（过去两周，混合状态，让统计页饱满） ----
    now = datetime.now()
    # 每个元组：天数前, 课程, 各状态学生索引分配（其余为 normal）
    history = [
        (12, c1, {"late": [1], "absent": [6], "location_error": [4]}),
        (10, c2, {"late": [2, 5], "absent": [7]}),
        (8, c1, {"absent": [3], "leave": [0]}),
        (5, c3, {"late": [4], "location_error": [2], "absent": [6]}),
        (3, c1, {"late": [1, 3], "absent": [7]}),
        (1, c2, {"leave": [5], "late": [0]}),
    ]
    for days_ago, course, dist in history:
        task_start = now - timedelta(days=days_ago, hours=2)
        task = AttendanceTask(
            schedule_id=None, course_id=course.id, class_id=clazz.id, teacher_id=course.teacher_id,
            code="HIST", start_time=task_start, end_time=task_start + timedelta(minutes=30),
            late_offset_min=10, latitude=lat, longitude=lng, radius_m=300, status="finished",
        )
        db.add(task)
        db.flush()
        # 反向索引：学生索引 -> 状态
        idx_status = {}
        for st_name, key in [(ATT_LATE, "late"), (ATT_ABSENT, "absent"),
                             (ATT_LOCATION_ERROR, "location_error"), (ATT_LEAVE, "leave")]:
            for idx in dist.get(key, []):
                idx_status[idx] = st_name
        for i, stu in enumerate(students):
            status = idx_status.get(i, ATT_NORMAL)
            checkin = None if status in (ATT_ABSENT, ATT_LEAVE) else task_start + timedelta(
                minutes=3 if status == ATT_NORMAL else 15)
            db.add(AttendanceRecord(
                task_id=task.id, student_id=stu.id, status=status, checkin_time=checkin,
                latitude=lat if checkin else None, longitude=lng if checkin else None,
                distance_m=20.0 if status == ATT_NORMAL else (
                    520.0 if status == ATT_LOCATION_ERROR else (30.0 if checkin else None)),
                remark="历史请假" if status == ATT_LEAVE else None,
            ))

    # ---- 演示用审批数据 ----
    # 1) 一条已通过的历史请假
    db.add(LeaveRequest(
        student_id=students[0].id, course_id=c1.id, type="sick",
        start_time=now - timedelta(days=8, hours=3), end_time=now - timedelta(days=8),
        reason="感冒发烧，已就医", status=ST_APPROVED, approver_id=t1.id,
        approve_time=now - timedelta(days=8, hours=1), approve_comment="同意，注意休息",
    ))
    # 2) 一条待审批请假（答辩可直接演示同意）
    db.add(LeaveRequest(
        student_id=students[2].id, course_id=c2.id, type="personal",
        start_time=now + timedelta(days=1, hours=2), end_time=now + timedelta(days=1, hours=4),
        reason="家中有事需请假半天", status=ST_PENDING,
    ))
    # 3) 一条待审批补卡（挂到一条历史缺勤/异常记录上）
    fix_rec = db.query(AttendanceRecord).filter(
        AttendanceRecord.student_id == students[6].id,
        AttendanceRecord.status == ATT_ABSENT,
    ).first()
    if fix_rec:
        db.add(FixRequest(
            record_id=fix_rec.id, student_id=students[6].id, reason_type="forgot",
            description="当时在教室但忘记打卡，申请补卡", status=ST_PENDING,
        ))

    # ---- 给学生一条欢迎通知 ----
    for stu in students:
        db.add(Notification(user_id=stu.id, title="欢迎使用移动考勤系统",
                            content="请在上课时及时完成签到。", type="attendance", is_read=0))

    db.commit()
    print("种子数据写入完成。")
    print("演示账号：admin/admin123；t1001、t1002 / 123456；s2023001~s2023008 / 123456")


def main():
    ensure_database()
    Base.metadata.create_all(bind=engine)
    print(f"数据库已就绪：{config.database_url()}")
    db = SessionLocal()
    try:
        seed(db)
    finally:
        db.close()


if __name__ == "__main__":
    main()
