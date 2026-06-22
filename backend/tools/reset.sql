-- 将 root 密码重置为 attend123（与 backend/app/config.py 默认值一致）
ALTER USER 'root'@'localhost' IDENTIFIED BY 'attend123';
-- 确保 TCP（127.0.0.1）连接也能用 root 登录，后端正是通过 127.0.0.1 访问
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY 'attend123';
ALTER USER 'root'@'127.0.0.1' IDENTIFIED BY 'attend123';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;
FLUSH PRIVILEGES;
