"""集中管理后端配置。

数据库连接做成可切换：
- 默认使用 MySQL（课程要求）。如需临时改用零配置的 SQLite，
  把环境变量 DB_BACKEND 设为 "sqlite" 即可（业务代码无需改动）。
- MySQL 的账号、密码、库名也都可用环境变量覆盖，避免把密码写死。
"""
import os

# ---------------------------------------------------------------------------
# 数据库
# ---------------------------------------------------------------------------
DB_BACKEND = os.getenv("DB_BACKEND", "mysql").lower()

# MySQL 连接参数（本机 MySQL 8.4，服务名 MySQL84）
MYSQL_HOST = os.getenv("DB_HOST", "127.0.0.1")
MYSQL_PORT = int(os.getenv("DB_PORT", "3306"))
MYSQL_USER = os.getenv("DB_USER", "root")
MYSQL_PASSWORD = os.getenv("DB_PASSWORD", "")  # 真实密码走环境变量，勿写死（避免提交到仓库泄露）
MYSQL_DB = os.getenv("DB_NAME", "attendance_db")

# SQLite 兜底：数据库文件放在 backend/ 目录下
SQLITE_PATH = os.getenv(
    "SQLITE_PATH",
    os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "attendance.db"),
)


def server_url() -> str:
    """不带库名的连接串，用于 CREATE DATABASE。仅 MySQL 用得到。"""
    return (
        f"mysql+pymysql://{MYSQL_USER}:{MYSQL_PASSWORD}@{MYSQL_HOST}:{MYSQL_PORT}"
        f"/?charset=utf8mb4"
    )


def database_url() -> str:
    """SQLAlchemy 使用的完整连接串。"""
    if DB_BACKEND == "sqlite":
        return f"sqlite:///{SQLITE_PATH}"
    return (
        f"mysql+pymysql://{MYSQL_USER}:{MYSQL_PASSWORD}@{MYSQL_HOST}:{MYSQL_PORT}"
        f"/{MYSQL_DB}?charset=utf8mb4"
    )


# ---------------------------------------------------------------------------
# 业务默认值
# ---------------------------------------------------------------------------
DEFAULT_RADIUS_M = 300          # 默认打卡半径（米）
DEFAULT_LATE_OFFSET_MIN = 10    # 开始后多少分钟内算正常，超过算迟到
DEFAULT_DURATION_MIN = 30       # 考勤默认有效时长（分钟）

# 演示用默认坐标（广州天河，种子数据与课程表统一使用）
DEMO_LAT = 23.1291
DEMO_LNG = 113.2644
