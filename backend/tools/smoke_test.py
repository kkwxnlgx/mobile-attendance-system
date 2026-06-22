"""端到端冒烟测试：用 FastAPI TestClient 走通核心业务流，无需起端口。

运行：
    cd backend
    set DB_BACKEND=sqlite & set SQLITE_PATH=...\smoke.db & python -m tools.smoke_test
"""
import os
import sys

# 强制 SQLite，独立测试库
os.environ.setdefault("DB_BACKEND", "sqlite")
HERE = os.path.dirname(os.path.abspath(__file__))
os.environ.setdefault("SQLITE_PATH", os.path.join(HERE, "smoke.db"))

if os.path.exists(os.environ["SQLITE_PATH"]):
    os.remove(os.environ["SQLITE_PATH"])

from fastapi.testclient import TestClient  # noqa: E402

from app import init_db  # noqa: E402
from app.main import app  # noqa: E402

init_db.main()
client = TestClient(app)

passed = 0
failed = 0


def check(name, cond, extra=""):
    global passed, failed
    if cond:
        passed += 1
        print(f"  [OK] {name}")
    else:
        failed += 1
        print(f"  [FAIL] {name} {extra}")


def login(username, password):
    r = client.post("/api/auth/login", json={"username": username, "password": password})
    assert r.status_code == 200, r.text
    return r.json()["token"]


def auth(token):
    return {"Authorization": f"Bearer {token}"}


print("\n== 1. 登录 ==")
admin = login("admin", "admin123")
t1 = login("t1001", "123456")
s1 = login("s2023001", "123456")
s2 = login("s2023002", "123456")
check("管理员/教师/学生均可登录", all([admin, t1, t2 := login("t1002", "123456"), s1, s2]))

bad = client.post("/api/auth/login", json={"username": "s2023001", "password": "wrong"})
check("错误密码被拒", bad.status_code == 400)

print("\n== 2. 注册 ==")
reg = client.post("/api/auth/register", json={"username": "s2023099", "name": "新同学", "password": "123456"})
check("新学号注册成功", reg.status_code == 200)
dup = client.post("/api/auth/register", json={"username": "s2023099", "name": "重复", "password": "123456"})
check("重复学号被拒", dup.status_code == 400)

print("\n== 3. 今日课程 / 课程表 ==")
today_t = client.get("/api/schedules/today", headers=auth(t1))
check("教师今日课程接口可用", today_t.status_code == 200)
schedules = client.get("/api/schedules", headers=auth(t1)).json()
check("教师可见课程表", len(schedules) > 0)

# 找一个张老师班级的课程表条目用于发起考勤
sch_id = schedules[0]["id"]
class_id = schedules[0]["class_id"]

print("\n== 4. 发起考勤 ==")
launch = client.post("/api/attendance/tasks", headers=auth(t1), json={
    "schedule_id": sch_id, "duration_min": 30, "late_offset_min": 10, "radius_m": 300})
check("发起考勤成功", launch.status_code == 200, launch.text)
task = launch.json()
code = task["code"]
task_id = task["id"]
check("返回4位签到码", len(code) == 4)

monitor = client.get(f"/api/attendance/tasks/{task_id}/monitor", headers=auth(t1)).json()
check("监控：应到=8（含历史新生不在本班）", monitor["total"] == 8, f"total={monitor['total']}")
check("监控：初始全部缺勤", monitor["absent"] == 8, f"absent={monitor['absent']}")

print("\n== 5. 学生签到 ==")
active = client.get("/api/attendance/tasks/active", headers=auth(s1)).json()
check("学生看到进行中考勤", len(active) == 1)

# 正常签到（坐标与考勤点一致）
ck = client.post("/api/attendance/checkin", headers=auth(s1),
                 json={"code": code, "latitude": 23.1291, "longitude": 113.2644})
check("正常签到返回 normal", ck.status_code == 200 and ck.json()["status"] == "normal", ck.text)

# 重复签到
ck2 = client.post("/api/attendance/checkin", headers=auth(s1),
                  json={"code": code, "latitude": 23.1291, "longitude": 113.2644})
check("重复签到被拒", ck2.status_code == 400)

# 位置异常签到（坐标拉远 ~ 数公里）
ck3 = client.post("/api/attendance/checkin", headers=auth(s2),
                  json={"code": code, "latitude": 23.2000, "longitude": 113.4000})
check("超距签到返回 location_error",
      ck3.status_code == 200 and ck3.json()["status"] == "location_error", ck3.text)

# 错误签到码
ck4 = client.post("/api/attendance/checkin", headers=auth(s1),
                  json={"code": "ZZZZ", "latitude": 23.1291, "longitude": 113.2644})
check("错误签到码被拒", ck4.status_code == 400)

monitor2 = client.get(f"/api/attendance/tasks/{task_id}/monitor", headers=auth(t1)).json()
check("监控刷新：1正常", monitor2["normal"] == 1, f"normal={monitor2['normal']}")
check("监控刷新：1位置异常", monitor2["location_error"] == 1, f"loc_err={monitor2['location_error']}")

print("\n== 6. 学生记录 / 通知 ==")
my_rec = client.get("/api/attendance/records/my", headers=auth(s1)).json()
check("学生可查个人记录", len(my_rec) > 0)
notis = client.get("/api/notifications", headers=auth(s1)).json()
check("学生收到考勤通知", any(n["type"] == "attendance" for n in notis))

print("\n== 7. 请假申请 + 审批 ==")
leave = client.post("/api/leaves", headers=auth(s1), json={
    "type": "personal", "start_time": "2026-06-12T08:00:00", "end_time": "2026-06-12T12:00:00",
    "reason": "测试请假"})
check("提交请假成功", leave.status_code == 200, leave.text)
pending_lv = client.get("/api/leaves/pending", headers=auth(t1)).json()
check("教师看到待审批请假", len(pending_lv) >= 1)
lv_id = pending_lv[0]["id"]
appr = client.post(f"/api/leaves/{lv_id}/approve", headers=auth(t1), json={"approve": True, "comment": "同意"})
check("审批通过成功", appr.status_code == 200)

print("\n== 8. 补卡申请 + 审批 ==")
# s2 的位置异常记录可申请补卡
s2_rec = client.get("/api/attendance/records/my", headers=auth(s2)).json()
loc_err_rec = next((r for r in s2_rec if r["status"] == "location_error"), None)
check("找到 s2 位置异常记录", loc_err_rec is not None)
if loc_err_rec:
    fix = client.post("/api/fixes", headers=auth(s2), json={
        "record_id": loc_err_rec["id"], "reason_type": "gps_fail", "description": "定位漂移"})
    check("提交补卡成功", fix.status_code == 200, fix.text)
    dup_fix = client.post("/api/fixes", headers=auth(s2), json={
        "record_id": loc_err_rec["id"], "reason_type": "gps_fail", "description": "重复"})
    check("重复补卡被拒", dup_fix.status_code == 400)
    pending_fix = client.get("/api/fixes/pending", headers=auth(t1)).json()
    fix_id = next(f["id"] for f in pending_fix if f["record_id"] == loc_err_rec["id"])
    appr_fix = client.post(f"/api/fixes/{fix_id}/approve", headers=auth(t1), json={"approve": True, "comment": "核实通过"})
    check("补卡审批通过", appr_fix.status_code == 200)
    # 验证记录状态被改为 normal
    s2_rec2 = client.get("/api/attendance/records/my", headers=auth(s2)).json()
    fixed = next((r for r in s2_rec2 if r["id"] == loc_err_rec["id"]), None)
    check("补卡后记录变 normal", fixed and fixed["status"] == "normal", f"status={fixed['status'] if fixed else '?'}")

print("\n== 9. 统计 ==")
stats = client.get(f"/api/stats/summary?class_id={class_id}", headers=auth(t1)).json()
check("统计返回汇总", "attend_rate" in stats and stats["total"] > 0,
      f"total={stats.get('total')}, rate={stats.get('attend_rate')}")
check("统计含学生明细", len(stats["students"]) > 0)

print("\n== 10. 班级 / 名单管理 ==")
classes = client.get("/api/classes", headers=auth(t1)).json()
check("教师可见班级列表", len(classes) >= 1)
roster = client.get(f"/api/classes/{class_id}/students", headers=auth(t1)).json()
check("名单含8名学生", len(roster) == 8, f"count={len(roster)}")
# 把新注册的 s2023099 加入班级
add = client.post(f"/api/classes/{class_id}/students", headers=auth(t1), json={"username": "s2023099"})
check("按学号添加学生成功", add.status_code == 200, add.text)

print("\n== 11. 管理员用户管理 ==")
users = client.get("/api/admin/users", headers=auth(admin)).json()
check("管理员可列出全部用户", len(users) > 10)
teacher_view = client.get("/api/admin/users", headers=auth(t1))
check("教师无权访问用户管理", teacher_view.status_code == 403)
# 禁用一个学生，然后该学生登录应失败
target = next(u for u in users if u["username"] == "s2023099")
client.put(f"/api/admin/users/{target['id']}/status", headers=auth(admin))
disabled_login = client.post("/api/auth/login", json={"username": "s2023099", "password": "123456"})
check("被禁用账号登录被拒", disabled_login.status_code == 403)

print(f"\n=== 结果：{passed} 通过 / {failed} 失败 ===")
sys.exit(1 if failed else 0)
