"""通用工具：密码哈希、签到码生成、经纬度距离计算。"""
import hashlib
import math
import secrets


def hash_password(plain: str) -> str:
    """返回 salt$sha256(salt+plain)。"""
    salt = secrets.token_hex(8)
    digest = hashlib.sha256((salt + plain).encode("utf-8")).hexdigest()
    return f"{salt}${digest}"


def verify_password(plain: str, stored: str) -> bool:
    try:
        salt, digest = stored.split("$", 1)
    except ValueError:
        return False
    return hashlib.sha256((salt + plain).encode("utf-8")).hexdigest() == digest


# 去掉容易混淆的 0 O 1 I
_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"


def gen_checkin_code(length: int = 4) -> str:
    return "".join(secrets.choice(_CODE_ALPHABET) for _ in range(length))


def haversine(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    """两点间地表距离（米）。任一坐标缺失时返回 0（视为不校验距离）。"""
    if None in (lat1, lng1, lat2, lng2):
        return 0.0
    r = 6371000.0
    p1 = math.radians(lat1)
    p2 = math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlmb = math.radians(lng2 - lng1)
    a = math.sin(dphi / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dlmb / 2) ** 2
    return r * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
