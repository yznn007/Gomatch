CREATE DATABASE IF NOT EXISTS gomatch CHARACTER SET utf8mb4;
USE gomatch;

CREATE TABLE IF NOT EXISTS t_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(32) UNIQUE NOT NULL,
    password VARCHAR(64) NOT NULL,
    nickname VARCHAR(32),
    score INT DEFAULT 1000,
    win_count INT DEFAULT 0,
    lose_count INT DEFAULT 0,
    draw_count INT DEFAULT 0,
    role TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_game_record (
    record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id VARCHAR(32),
    black_user_id BIGINT,
    white_user_id BIGINT,
    winner_id BIGINT,
    result TINYINT,
    steps TEXT,
    start_time DATETIME,
    end_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
