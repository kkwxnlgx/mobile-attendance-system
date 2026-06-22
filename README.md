# 移动考勤系统

课程大作业：Android（Java）+ FastAPI + 数据库 的移动课堂考勤系统。
覆盖学生、教师、系统管理员三类角色，实现注册登录、班级/课程表/名单管理、
发起课堂考勤、定位+签到码签到、考勤记录、补卡申诉、请假申请/审批、考勤统计与通知。

## 目录结构

```
finalwork/
├── backend/                 # FastAPI 后端
│   ├── requirements.txt
│   ├── app/
│   │   ├── config.py        # 数据库与业务配置（可切换 MySQL / SQLite）
│   │   ├── database.py models.py schemas.py security.py utils.py
│   │   ├── init_db.py       # 建库 + 种子数据（幂等）
│   │   ├── main.py          # 应用入口
│   │   └── routers/         # 9 个业务路由模块
│   └── tools/
│       └── smoke_test.py    # 端到端接口冒烟测试（35 项）
└── android/                 # Android 工程（Java）
    └── app/src/main/...     # 学生 / 教师 / 管理员三端界面
```

## 一、启动后端

### 1. 安装依赖（仅首次）
```
cd backend
pip install -r requirements.txt
```

### 2. 选择数据库并初始化

**方案 A：MySQL（课程推荐）**
```
# 设置数据库连接（按本机实际情况）
set DB_BACKEND=mysql
set DB_HOST=127.0.0.1
set DB_USER=root
set DB_PASSWORD=你的root密码
set DB_NAME=attendance_db
python -m app.init_db        # 自动建库、建表、灌入演示数据
```

**方案 B：SQLite（零配置，便于快速演示）**
```
set DB_BACKEND=sqlite
python -m app.init_db        # 在 backend/ 下生成 attendance.db
```

> 业务代码与数据库无关，两种方案功能完全一致，仅连接配置不同。

### 3. 启动服务
```
# 模拟器演示用 127.0.0.1 即可（模拟器通过 10.0.2.2 访问宿主机）
uvicorn app.main:app --host 127.0.0.1 --port 8000
```
打开 http://127.0.0.1:8000/docs 可查看并调试全部接口（答辩可直接演示）。

> 若用真机调试，改为 `--host 0.0.0.0`，并把 Android 端 `ApiClient.BASE_URL`
> 改成电脑局域网 IP，同时在防火墙放行 8000 端口。

## 二、运行 Android 端

1. 用 Android Studio 打开 `android/` 目录（已锁定 Gradle 9.3.1 + AGP 9.1.0，
   compileSdk 36，JDK 17，无需联网升级，遇到升级提示选择保持不变）。
2. 启动一个 Android 模拟器（API 24+）。
3. 运行 app，安装到模拟器。

后端地址固定为 `http://10.0.2.2:8000/`（见 `net/ApiClient.java`），
这是 Android 模拟器访问宿主机回环地址的标准方式，无需改动。

### 模拟器定位设置（定位签到必需）
- 模拟器右侧「⋮」→ Location → 输入经度 `113.2644`、纬度 `23.1291` → SET LOCATION；
- 或命令行：`adb emu geo fix 113.2644 23.1291`（注意经度在前）。
- 演示「位置异常」时把坐标改远，例如 `23.2000 / 113.4000` 后再签到。

## 三、演示账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 | admin | admin123 |
| 教师 | t1001（张老师）、t1002（李老师） | 123456 |
| 学生 | s2023001 ~ s2023008 | 123456 |

种子数据含 1 个班级、3 门课、覆盖每个工作日的课程表、过去两周的历史考勤记录
（含迟到/缺勤/请假/异常，便于统计页展示），以及待审批的请假和补卡各 1 条。

## 四、答辩演示流程（单模拟器，登出切换角色）

1. 学生注册新账号 →（错误密码/正确）演示登录。
2. 登录 t1001 → 今日授课 → **发起考勤**（30 分钟 / 半径 300m）→ 监控页显示大字签到码、已到 0 / 应到 8。
3. 登录 s2023001 → 首页「进行中」→ **立即签到** → 输入签到码、显示定位坐标 → 提交 → 绿色「签到成功（正常）」。
4. 把模拟器坐标改远 → 登录 s2023002 签到 → 红色「位置异常」。
5. s2023002 提交请假申请 → 登出。
6. 登录 t1001 → 监控页计数已刷新 → **结束考勤**（未签到自动缺勤）→ 审批页同意请假、演示补卡审批。
7. 统计页选班级 → 出勤率、迟到/缺勤/请假/异常计数与学生明细。
8. 登录 admin → 用户管理禁用某账号 → 该账号登录提示「已禁用」。

## 五、后端自测

```
cd backend
set DB_BACKEND=sqlite
python -m tools.smoke_test     # 走通登录→发起→签到→请假→审批→统计，35 项断言
```
