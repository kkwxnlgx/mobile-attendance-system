"""极简 token 认证。

课程项目阶段不引入 JWT：登录成功后生成随机 token 存内存字典，
客户端携带 Authorization: Bearer <token>。服务重启后重新登录即可。
"""
import secrets

from fastapi import Depends, Header, HTTPException
from sqlalchemy.orm import Session

from .database import get_db
from .models import User

# token -> user_id
_TOKENS: dict[str, int] = {}


def issue_token(user_id: int) -> str:
    token = secrets.token_hex(16)
    _TOKENS[token] = user_id
    return token


def revoke_token(token: str) -> None:
    _TOKENS.pop(token, None)


def get_current_user(
    authorization: str = Header(default=""),
    db: Session = Depends(get_db),
) -> User:
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="未登录或登录已失效")
    token = authorization[7:]
    user_id = _TOKENS.get(token)
    if user_id is None:
        raise HTTPException(status_code=401, detail="未登录或登录已失效")
    user = db.query(User).get(user_id)
    if user is None or user.status != 1:
        raise HTTPException(status_code=401, detail="账号不存在或已被禁用")
    return user


def require_role(*roles: str):
    """生成一个依赖：要求当前用户角色在 roles 内。"""

    def checker(user: User = Depends(get_current_user)) -> User:
        if user.role not in roles:
            raise HTTPException(status_code=403, detail="没有权限执行该操作")
        return user

    return checker
