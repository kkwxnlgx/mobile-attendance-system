"""通知提醒。"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import Notification, User
from ..schemas import MessageOut, NotificationOut
from ..security import get_current_user

router = APIRouter(prefix="/api/notifications", tags=["通知"])


@router.get("", response_model=list[NotificationOut], summary="我的通知")
def list_notifications(db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    return db.query(Notification).filter(
        Notification.user_id == user.id).order_by(Notification.id.desc()).all()


@router.post("/{nid}/read", response_model=MessageOut, summary="标记已读")
def mark_read(nid: int, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    n = db.query(Notification).filter(Notification.id == nid, Notification.user_id == user.id).first()
    if n is None:
        raise HTTPException(status_code=404, detail="通知不存在")
    n.is_read = 1
    db.commit()
    return MessageOut(message="已标记为已读")
