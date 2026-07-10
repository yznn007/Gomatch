ALTER TABLE t_user ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像URL';
CREATE INDEX idx_user_avatar_url ON t_user(avatar_url);