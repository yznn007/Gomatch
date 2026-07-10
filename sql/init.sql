CREATE DATABASE IF NOT EXISTS `gomatch` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


USE `gomatch`;

CREATE TABLE IF NOT EXISTS `t_user` (
    `user_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码(明文)',
    `email` VARCHAR(100) UNIQUE COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'PLAYER' COMMENT '角色: PLAYER/ADMIN',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/BANNED/INACTIVE',
    `score` INT DEFAULT 1000 COMMENT '积分',
    `win_count` INT DEFAULT 0 COMMENT '胜场数',
    `lose_count` INT DEFAULT 0 COMMENT '负场数',
    `draw_count` INT DEFAULT 0 COMMENT '平局数',
    `ai_win_count` INT DEFAULT 0 COMMENT 'AI对战胜场数',
    `ai_lose_count` INT DEFAULT 0 COMMENT 'AI对战负场数',
    `version` INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_username` (`username`),
    INDEX `idx_role` (`role`),
    INDEX `idx_status` (`status`),
    INDEX `idx_role_status` (`role`, `status`),
    INDEX `idx_score` (`score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
CREATE TABLE IF NOT EXISTS `t_user_profile` (
    `user_id` BIGINT PRIMARY KEY COMMENT '用户ID',
    `config_json` JSON NOT NULL COMMENT '客户端配置(JSON)',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后同步时间',
    CONSTRAINT `fk_user_profile_user` FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户云端配置表';

CREATE TABLE IF NOT EXISTS `t_room` (
    `room_id` VARCHAR(50) PRIMARY KEY COMMENT '房间ID(UUID)',
    `room_name` VARCHAR(100) NOT NULL COMMENT '房间名称',
    `password` VARCHAR(100) COMMENT '房间密码(可选)',
    `max_players` INT DEFAULT 2 COMMENT '最大人数',
    `current_players` INT DEFAULT 0 COMMENT '当前人数',
    `watcher_count` INT DEFAULT 0 COMMENT '观战人数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'waiting' COMMENT '房间状态: waiting/playing/finished',
    `host_id` BIGINT NOT NULL COMMENT '房主ID',
    `allow_watch` BOOLEAN DEFAULT TRUE COMMENT '是否允许观战',
    `version` INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_status` (`status`),
    INDEX `idx_host_id` (`host_id`),
    INDEX `idx_allow_watch` (`allow_watch`),
    INDEX `idx_status_players` (`status`, `current_players`),
    INDEX `idx_watch_status` (`allow_watch`, `status`),
    FOREIGN KEY (`host_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间表';

CREATE TABLE IF NOT EXISTS `t_game_record` (
    `record_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `player1_id` BIGINT NOT NULL COMMENT '玩家1ID',
    `player2_id` BIGINT NOT NULL COMMENT '玩家2ID',
    `game_type` VARCHAR(20) NOT NULL DEFAULT 'PVP' COMMENT '游戏类型: PVP/PVE/RANKED',
    `winner_id` BIGINT COMMENT '胜者ID(平局为NULL)',
    `result` VARCHAR(30) NOT NULL COMMENT '结果: five_in_a_row/surrender/draw',
    `board_data` TEXT NOT NULL COMMENT '棋谱数据(JSON格式)',
    `duration` INT COMMENT '对局时长(秒)',
    `move_count` INT DEFAULT 0 COMMENT '落子总数',
    `player1_score_change` INT DEFAULT 0 COMMENT '玩家1积分变化',
    `player2_score_change` INT DEFAULT 0 COMMENT '玩家2积分变化',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_player1` (`player1_id`),
    INDEX `idx_player2` (`player2_id`),
    INDEX `idx_game_type` (`game_type`),
    INDEX `idx_create_time` (`create_time`),
    FOREIGN KEY (`player1_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`player2_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`winner_id`) REFERENCES `t_user`(`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏记录表';

CREATE TABLE IF NOT EXISTS `t_rank` (
    `rank_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '排名ID',
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    `rank` INT DEFAULT 0 COMMENT '排名',
    `score` INT DEFAULT 0 COMMENT '积分',
    `win_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT '胜率',
    `ai_win_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT 'AI对战胜率',
    `version` INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_rank` (`rank`),
    INDEX `idx_score` (`score`),
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜表';

CREATE TABLE IF NOT EXISTS `t_socket_session` (
    `session_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `session_key` VARCHAR(64) NOT NULL UNIQUE COMMENT '会话密钥(UUID)',
    `client_ip` VARCHAR(50) COMMENT '客户端IP',
    `connection_id` VARCHAR(64) COMMENT '连接ID',
    `room_id` VARCHAR(50) COMMENT '当前所在房间ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'connected' COMMENT '连接状态: connected/disconnected/reconnecting',
    `last_heartbeat` DATETIME COMMENT '最后心跳时间',
    `disconnect_time` DATETIME COMMENT '断开连接时间',
    `disconnect_reason` VARCHAR(50) COMMENT '断开原因: timeout/network/normal',
    `reconnect_attempts` INT DEFAULT 0 COMMENT '重连尝试次数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_last_heartbeat` (`last_heartbeat`),
    INDEX `idx_room_id` (`room_id`),
    INDEX `idx_room_status` (`room_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Socket会话表';

CREATE TABLE IF NOT EXISTS `t_chat_message` (
    `message_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    `room_id` VARCHAR(50) NOT NULL COMMENT '房间ID',
    `user_id` BIGINT NOT NULL COMMENT '发送者ID',
    `content` VARCHAR(500) NOT NULL COMMENT '消息内容',
    `message_type` VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '消息类型: text/system/game',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_room_id` (`room_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

CREATE TABLE IF NOT EXISTS `t_room_watcher` (
    `watcher_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '观战记录ID',
    `room_id` VARCHAR(50) NOT NULL COMMENT '房间ID',
    `user_id` BIGINT NOT NULL COMMENT '观战者ID',
    `join_time` DATETIME NOT NULL COMMENT '加入观战时间',
    `leave_time` DATETIME COMMENT '离开观战时间',
    INDEX `idx_room_id` (`room_id`),
    INDEX `idx_user_id` (`user_id`),
    UNIQUE INDEX `uk_room_user` (`room_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间观战表';

CREATE TABLE IF NOT EXISTS `t_game_action` (
    `action_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '操作记录ID',
    `record_id` BIGINT COMMENT '关联对局ID(游戏进行中为NULL)',
    `user_id` BIGINT NOT NULL COMMENT '操作用户ID',
    `action_type` VARCHAR(20) NOT NULL COMMENT '操作类型: UNDO_REQUEST/DRAW_REQUEST',
    `action_status` VARCHAR(20) NOT NULL COMMENT '操作状态: PENDING/ACCEPTED/REJECTED',
    `move_number` INT DEFAULT 0 COMMENT '操作时落子数',
    `reject_reason` VARCHAR(100) COMMENT '拒绝原因',
    `opponent_response` DATETIME COMMENT '对手响应时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_record_id` (`record_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_action_type` (`action_type`),
    INDEX `idx_action_status` (`action_status`),
    INDEX `idx_record_status` (`record_id`, `action_status`),
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏操作表';

CREATE TABLE IF NOT EXISTS `t_game_save` (
    `save_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '保存ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `record_id` BIGINT COMMENT '关联对局ID(正在进行的对局为NULL)',
    `file_uri` VARCHAR(500) NOT NULL UNIQUE COMMENT '棋局文件URI',
    `save_name` VARCHAR(100) COMMENT '保存名称',
    `board_data` TEXT NOT NULL COMMENT '棋盘状态(JSON)',
    `current_player` INT NOT NULL COMMENT '当前执子方: 1(黑棋)/2(白棋)',
    `game_status` VARCHAR(20) NOT NULL COMMENT '游戏状态: playing/paused/finished',
    `checksum` VARCHAR(64) COMMENT '文件校验和(SHA-256)',
    `file_size` INT DEFAULT 0 COMMENT '文件大小(字节)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_record_id` (`record_id`),
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='棋局保存表';

CREATE TABLE IF NOT EXISTS `t_friend` (
    `friend_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友关系ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_user_id` BIGINT NOT NULL COMMENT '好友ID',
    `relation_type` VARCHAR(20) NOT NULL DEFAULT 'FRIEND' COMMENT '关系类型',
    `remark` VARCHAR(50) COMMENT '好友备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_friend_user_id` (`friend_user_id`),
    UNIQUE INDEX `uk_user_friend` (`user_id`, `friend_user_id`),
    INDEX `idx_relation_type` (`relation_type`),
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`friend_user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

CREATE TABLE IF NOT EXISTS `t_friend_request` (
    `request_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '请求ID',
    `from_user_id` BIGINT NOT NULL COMMENT '发起者ID',
    `to_user_id` BIGINT NOT NULL COMMENT '接收者ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '请求状态: PENDING/ACCEPTED/REJECTED',
    `message` VARCHAR(200) COMMENT '请求附言',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `respond_time` DATETIME COMMENT '响应时间',
    INDEX `idx_from_user` (`from_user_id`),
    INDEX `idx_to_user` (`to_user_id`),
    UNIQUE INDEX `uk_from_to` (`from_user_id`, `to_user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_to_status` (`to_user_id`, `status`),
    FOREIGN KEY (`from_user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`to_user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友请求表';

CREATE TABLE IF NOT EXISTS `t_user_settings` (
    `settings_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设置ID',
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    `avatar_path` VARCHAR(500) COMMENT '头像文件路径(阿里云OSS)',
    `language` VARCHAR(20) DEFAULT 'zh_CN' COMMENT '语言: zh_CN/en_US',
    `font_family` VARCHAR(50) DEFAULT 'SimSun' COMMENT '字体',
    `font_size` INT DEFAULT 12 COMMENT '字体大小(px)',
    `frame_rate` INT DEFAULT 60 COMMENT '帧率(30/60/120)',
    `window_width` INT DEFAULT 900 COMMENT '窗口宽度',
    `window_height` INT DEFAULT 640 COMMENT '窗口高度',
    `background_music_enabled` BOOLEAN DEFAULT TRUE COMMENT '背景音乐开关',
    `background_music_volume` DECIMAL(3,2) DEFAULT 0.5 COMMENT '背景音乐音量(0-1)',
    `sound_effects_enabled` BOOLEAN DEFAULT TRUE COMMENT '音效开关',
    `sound_effects_volume` DECIMAL(3,2) DEFAULT 0.7 COMMENT '音效音量(0-1)',
    `lobby_music_id` BIGINT COMMENT '当前大厅背景音乐ID',
    `theme` VARCHAR(50) DEFAULT 'light' COMMENT '主题: light/dark',
    `version` INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_lobby_music_id` (`lobby_music_id`),
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表';

CREATE TABLE IF NOT EXISTS `t_user_music` (
    `music_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '音乐ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `music_name` VARCHAR(100) NOT NULL COMMENT '音乐名称',
    `music_path` VARCHAR(500) NOT NULL COMMENT '音乐文件路径(阿里云OSS)',
    `file_name` VARCHAR(200) NOT NULL COMMENT '原始文件名',
    `file_size` INT DEFAULT 0 COMMENT '文件大小(字节)',
    `duration` INT COMMENT '音乐时长(秒)',
    `music_type` VARCHAR(20) NOT NULL DEFAULT 'LOBBY' COMMENT '音乐类型: LOBBY/BGM',
    `is_default` BOOLEAN DEFAULT FALSE COMMENT '是否设为默认音乐',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_music_type` (`music_type`),
    UNIQUE INDEX `uk_user_filename` (`user_id`, `file_name`),
    FOREIGN KEY (`user_id`) REFERENCES `t_user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户音乐表';

CREATE TABLE IF NOT EXISTS `t_audit_log` (
    `log_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `action_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `action_desc` VARCHAR(200) COMMENT '操作描述',
    `target_type` VARCHAR(50) COMMENT '目标类型',
    `target_id` BIGINT COMMENT '目标ID',
    `before_data` TEXT COMMENT '修改前数据(JSON)',
    `after_data` TEXT COMMENT '修改后数据(JSON)',
    `ip_address` VARCHAR(50) COMMENT '操作IP地址',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_action_type` (`action_type`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

INSERT INTO `t_user` (`username`, `password`, `email`, `role`, `status`, `score`, `win_count`, `lose_count`, `draw_count`) VALUES
('admin',   '${GOMATCH_SEED_PASSWORD}', '[redacted-email]',   'ADMIN',  'ACTIVE', 9999, 0, 0, 0),
('player1', '${GOMATCH_SEED_PASSWORD}', '[redacted-email]', 'PLAYER', 'ACTIVE', 1300, 8, 4, 1),
('player2', '${GOMATCH_SEED_PASSWORD}', '[redacted-email]', 'PLAYER', 'ACTIVE', 1200, 5, 3, 0),
('player3', '${GOMATCH_SEED_PASSWORD}', '[redacted-email]', 'PLAYER', 'ACTIVE', 1150, 4, 2, 1),
('player4', '${GOMATCH_SEED_PASSWORD}', '[redacted-email]', 'PLAYER', 'ACTIVE', 1100, 3, 1, 0),
('player5', '${GOMATCH_SEED_PASSWORD}', '[redacted-email]', 'PLAYER', 'ACTIVE', 1080, 3, 0, 2),
('player6', '${GOMATCH_SEED_PASSWORD}', '[redacted-email]', 'PLAYER', 'ACTIVE', 1060, 2, 1, 1),
('player7', '${GOMATCH_SEED_PASSWORD}', '[redacted-email]', 'PLAYER', 'ACTIVE', 1050, 2, 2, 0),
('player8', '${GOMATCH_SEED_PASSWORD}', '[redacted-email]', 'PLAYER', 'ACTIVE', 1020, 1, 1, 0),
('player9', '${GOMATCH_SEED_PASSWORD}', '[redacted-email]', 'PLAYER', 'ACTIVE', 1000, 0, 0, 0),
('player10','${GOMATCH_SEED_PASSWORD}', '[redacted-email]','PLAYER', 'ACTIVE',  990, 0, 0, 0),
('player11','${GOMATCH_SEED_PASSWORD}', '[redacted-email]','PLAYER', 'ACTIVE',  980, 1, 3, 0),
('player12','${GOMATCH_SEED_PASSWORD}', '[redacted-email]','PLAYER', 'ACTIVE',  950, 0, 2, 0),
('player13','${GOMATCH_SEED_PASSWORD}', '[redacted-email]','PLAYER', 'ACTIVE', 1180, 5, 5, 0),
('player14','${GOMATCH_SEED_PASSWORD}', '[redacted-email]','PLAYER', 'ACTIVE', 1000, 0, 0, 0);

-- AI 占位用户，用于满足对战记录表的外键约束
INSERT INTO `t_user` (`user_id`, `username`, `password`, `email`, `role`, `status`, `score`) VALUES
(-1, 'AI', '${GOMATCH_SEED_PASSWORD}', '[redacted-email]', 'PLAYER', 'ACTIVE', 0);

INSERT INTO `t_rank` (`user_id`, `rank`, `score`, `win_rate`, `ai_win_rate`) VALUES
(1,  1,  9999, 0.00, 0.00),
(2,  2,  1300, 0.67, 0.00),
(3,  3,  1200, 0.63, 0.00),
(4,  4,  1150, 0.57, 0.00),
(5,  5,  1100, 0.75, 0.00),
(6,  6,  1080, 0.60, 0.00),
(7,  7,  1060, 0.67, 0.00),
(8,  8,  1050, 0.50, 0.00),
(9,  9,  1020, 0.50, 0.00),
(10, 10, 1000, 0.00, 0.00),
(11, 11,  990, 0.00, 0.00),
(12, 12,  980, 0.25, 0.00),
(13, 13,  950, 0.00, 0.00),
(14, 14, 1180, 0.50, 0.00),
(15, 15, 1000, 0.00, 0.00);
