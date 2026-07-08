package com.gomatch.server.room;

import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import com.gomatch.server.game.AIPlayer;
import com.gomatch.server.handler.ClientHandler;
import com.gomatch.server.redis.RedisManager;
import com.gomatch.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 房间管理器（单例）
 * 管理所有房间的创建、查询、销毁
 * 提供快速匹配功能（带超时）
 */
public class RoomManager {
    private static final Logger log = LoggerFactory.getLogger(RoomManager.class);
    private static final RoomManager instance = new RoomManager();

    /** 快速匹配超时时间（秒） */
    private static final int MATCH_TIMEOUT_SECONDS = 60;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    /** 快速匹配等待队列 */
    private final Queue<MatchEntry> matchQueue = new ConcurrentLinkedQueue<>();
    private final Map<Long, MatchEntry> matchEntries = new ConcurrentHashMap<>();

    /** 排位匹配等待队列（内存实现，作为Redis备选） */
    private final Queue<MatchEntry> rankedMatchQueue = new ConcurrentLinkedQueue<>();
    private final Map<Long, MatchEntry> rankedMatchEntries = new ConcurrentHashMap<>();

    private final ScheduledExecutorService matchScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "match-timeout");
        t.setDaemon(true);
        return t;
    });

    /** 匹配队列条目 */
    private static class MatchEntry {
        final long userId;
        final int score;
        final ClientHandler handler;
        final ScheduledFuture<?> timeoutTask;

        MatchEntry(long userId, int score, ClientHandler handler, ScheduledFuture<?> timeoutTask) {
            this.userId = userId;
            this.score = score;
            this.handler = handler;
            this.timeoutTask = timeoutTask;
        }

        void cancelTimeout() {
            if (timeoutTask != null && !timeoutTask.isDone()) {
                timeoutTask.cancel(false);
            }
        }
    }

    private RoomManager() {}

    public static RoomManager getInstance() { return instance; }

    // ==================== 房间 CRUD ====================

    /**
     * 创建房间
     * @param name 房间名称
     * @param owner 房主
     * @return 创建的 Room 对象
     */
    public Room createRoom(String name, ClientHandler owner) {
        return createRoom(name, owner, null);
    }

    public Room createRoom(String name, ClientHandler owner, String password) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Room room = new Room(id, name, owner, password);
        rooms.put(id, room);
        
        // 房主加入房间
        room.join(owner);
        
        log.info("房间创建成功: {} 由用户 {} 创建，是否有密码: {}", 
                id, owner.getUserId(), password != null && !password.isEmpty());
        return room;
    }

    /**
     * 创建人机对战房间
     * @param name 房间名称
     * @param owner 房主（唯一玩家）
     * @param difficulty AI 难度
     * @return 创建的 Room 对象
     */
    public Room createAiRoom(String name, ClientHandler owner, String difficulty) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Room room = new Room(id, name, owner, null, true);
        rooms.put(id, room);
        
        // 设置 AI 难度
        try {
            room.setAiDifficulty(AIPlayer.Difficulty.valueOf(difficulty.toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown AI difficulty '{}', fallback to MEDIUM", difficulty);
            room.setAiDifficulty(AIPlayer.Difficulty.MEDIUM);
        }
        
        room.join(owner);
        
        log.info("人机对战房间创建成功: {} 由用户 {} 创建, 难度={}", id, owner.getUserId(), difficulty);
        return room;
    }

    /**
     * 创建人机对战房间（默认中等难度）
     */
    public Room createAiRoom(String name, ClientHandler owner) {
        return createAiRoom(name, owner, "MEDIUM");
    }

    public Room createRankedRoom(String name, ClientHandler owner) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Room room = new Room(id, name, owner, null, false, Room.GameType.RANKED);
        rooms.put(id, room);
        
        room.join(owner);
        
        log.info("排位赛房间创建成功: {} 由用户 {} 创建", id, owner.getUserId());
        return room;
    }

    /**
     * 根据房间ID获取房间
     */
    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    /**
     * 获取所有房间
     */
    public Map<String, Room> getAllRooms() {
        return Collections.unmodifiableMap(rooms);
    }

    /**
     * 通过用户ID查找房间（用于断线重连）
     * @param userId 用户ID
     * @return 用户所在的房间，未找到返回null
     */
    public Room findRoomByUserId(long userId) {
        for (Room room : rooms.values()) {
            if (room.getBlackUserId() == userId || room.getWhiteUserId() == userId) {
                return room;
            }
        }
        return null;
    }

    /**
     * 获取活跃房间数量
     */
    public int getActiveRoomCount() {
        return rooms.size();
    }

    /**
     * 删除房间
     */
    public void removeRoom(String roomId) {
        Room room = rooms.remove(roomId);
        if (room != null) {
            log.info("房间已删除: {}", roomId);
        }
    }

    // ==================== 快速匹配（内存队列 + 超时）====================

    /**
     * 快速匹配：优先配对等待中的玩家，否则进入等待队列，超时自动取消
     */
    public Room quickMatch(ClientHandler player) {
        long userId = player.getUserId();
        log.info("玩家 {} 请求快速匹配", userId);

        // 如果已在等待队列中，先移除旧的
        cancelMatch(userId);

        SessionManager.Session session = SessionManager.getInstance().getSessionByUserId(userId);
        int score = session != null ? session.score : 1000;

        // 1. 从 Redis 等待队列中取候选人配对
        String queuedOpponent;
        while ((queuedOpponent = RedisManager.getInstance().pollMatchQueue()) != null) {
            String[] parts = queuedOpponent.split(":");
            if (parts.length < 1) {
                continue;
            }

            long opponentId;
            int opponentScore = 1000;
            try {
                opponentId = Long.parseLong(parts[0]);
                if (parts.length > 1) {
                    opponentScore = Integer.parseInt(parts[1]);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid quick match queue entry: {}", queuedOpponent);
                continue;
            }

            if (opponentId == userId) {
                continue;
            }

            MatchEntry opponentEntry = matchEntries.remove(opponentId);
            ClientHandler opponentHandler = opponentEntry != null ? opponentEntry.handler : findClientHandler(opponentId);
            if (opponentHandler == null || opponentHandler.getCurrentRoom() != null) {
                log.info("对手 {} 已离线或已在房间，跳过", opponentId);
                continue;
            }

            if (opponentEntry != null) {
                opponentEntry.cancelTimeout();
                matchQueue.remove(opponentEntry);
            }

            String roomName = "快速匹配 #" + (getActiveRoomCount() + 1);
            Room room = createRoom(roomName, player);

            room.join(opponentHandler);
            player.setCurrentRoom(room);
            opponentHandler.setCurrentRoom(room);
            room.startGame();

            log.info("快速匹配成功: 玩家 {}(score={}) vs 玩家 {}(score={})",
                    userId, score, opponentId, opponentScore);
            return room;
        }

        // 2. 没有对手，加入 Redis 等待队列，启动超时定时器
        ScheduledFuture<?> timeoutTask = matchScheduler.schedule(() -> {
            MatchEntry entry = matchEntries.remove(userId);
            if (entry != null) {
                matchQueue.remove(entry);
                RedisManager.getInstance().removeFromMatchQueue(userId);
                // 如果玩家还没被匹配，通知超时
                if (entry.handler.getCurrentRoom() == null) {
                    entry.handler.send(Message.of(Protocol.MATCH_TIMEOUT,
                            "msg", "匹配超时，请重试"));
                    log.info("玩家 {} 快速匹配超时，已移出队列", userId);
                }
            }
        }, MATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        MatchEntry entry = new MatchEntry(userId, score, player, timeoutTask);
        matchQueue.offer(entry);
        matchEntries.put(userId, entry);

        // 写入 Redis 匹配队列
        RedisManager.getInstance().addMatchQueue(userId, score);

        log.info("玩家 {} 进入快速匹配队列等待（{}秒超时），当前Redis队列长度: {}",
                userId, MATCH_TIMEOUT_SECONDS, RedisManager.getInstance().getMatchQueueSize());
        return null;
    }

    /**
     * 取消匹配
     */
    public void cancelMatch(long userId) {
        MatchEntry entry = matchEntries.remove(userId);
        if (entry != null) {
            entry.cancelTimeout();
            matchQueue.remove(entry);
            RedisManager.getInstance().removeFromMatchQueue(userId);
            log.info("玩家 {} 取消快速匹配", userId);
        }
    }

    public int getMatchQueueSize() {
        return RedisManager.getInstance().getMatchQueueSize();
    }

    public Room rankedMatch(ClientHandler player, int playerScore) {
        long userId = player.getUserId();
        log.info("玩家 {} (积分:{}) 加入排位匹配队列", userId, playerScore);

        cancelRankedMatch(userId);

        // 1. 从 Redis 排位队列中取积分接近的候选人配对
        String rankedOpponent = RedisManager.getInstance().pollRankedMatchQueue(playerScore, userId);
        while (rankedOpponent != null) {
            String[] parts = rankedOpponent.split(":");
            if (parts.length < 1) {
                rankedOpponent = RedisManager.getInstance().pollRankedMatchQueue(playerScore, userId);
                continue;
            }

            long opponentId;
            int opponentScore = playerScore;
            try {
                opponentId = Long.parseLong(parts[0]);
                if (parts.length > 1) {
                    opponentScore = Integer.parseInt(parts[1]);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid ranked match queue entry: {}", rankedOpponent);
                rankedOpponent = RedisManager.getInstance().pollRankedMatchQueue(playerScore, userId);
                continue;
            }

            MatchEntry opponentEntry = rankedMatchEntries.remove(opponentId);
            ClientHandler opponentHandler = opponentEntry != null ? opponentEntry.handler : findClientHandler(opponentId);
            if (opponentHandler == null || opponentHandler.getCurrentRoom() != null) {
                log.info("对手 {} 已离线或已在房间，跳过", opponentId);
                rankedOpponent = RedisManager.getInstance().pollRankedMatchQueue(playerScore, userId);
                continue;
            }

            if (opponentEntry != null) {
                opponentEntry.cancelTimeout();
                rankedMatchQueue.remove(opponentEntry);
            }

            String roomName = "排位赛 #" + (getActiveRoomCount() + 1);
            Room room = createRankedRoom(roomName, player);

            room.join(opponentHandler);
            player.setCurrentRoom(room);
            opponentHandler.setCurrentRoom(room);
            room.startGame();

            log.info("排位匹配成功: 玩家 {}({}) vs 玩家 {}({})",
                    userId, playerScore, opponentId, opponentScore);
            return room;
        }

        // 2. 没有对手，加入 Redis 等待队列，启动超时定时器
        ScheduledFuture<?> timeoutTask = matchScheduler.schedule(() -> {
            MatchEntry entry = rankedMatchEntries.remove(userId);
            if (entry != null) {
                rankedMatchQueue.remove(entry);
                RedisManager.getInstance().removeRankedMatchQueue(userId);
                if (entry.handler.getCurrentRoom() == null) {
                    entry.handler.send(Message.of(Protocol.MATCH_TIMEOUT,
                            "msg", "排位匹配超时，请重试"));
                    log.info("玩家 {} 排位匹配超时，已移出队列", userId);
                }
            }
        }, MATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        MatchEntry entry = new MatchEntry(userId, playerScore, player, timeoutTask);
        rankedMatchQueue.offer(entry);
        rankedMatchEntries.put(userId, entry);

        RedisManager.getInstance().addRankedMatchQueue(userId, playerScore);

        log.info("玩家 {} 进入排位匹配队列等待（{}秒超时），当前Redis队列长度: {}",
                userId, MATCH_TIMEOUT_SECONDS, RedisManager.getInstance().getRankedMatchQueueSize());
        return null;
    }

    public int getRankedMatchQueueSize() {
        return RedisManager.getInstance().getRankedMatchQueueSize();
    }

    public void cancelRankedMatch(long userId) {
        MatchEntry entry = rankedMatchEntries.remove(userId);
        if (entry != null) {
            entry.cancelTimeout();
            rankedMatchQueue.remove(entry);
            RedisManager.getInstance().removeRankedMatchQueue(userId);
            log.info("玩家 {} 取消排位匹配", userId);
        }
    }

    private ClientHandler findClientHandler(long userId) {
        SessionManager.Session session = SessionManager.getInstance().getSessionByUserId(userId);
        if (session != null && session.handler != null) {
            return session.handler;
        }
        return null;
    }

    // ==================== 玩家加入已有房间 ====================

    /**
     * 玩家加入指定房间
     * @return true=成功 false=失败（房间不存在/已满/游戏中）
     */
    public boolean joinExistingRoom(ClientHandler player, String roomId) {
        return joinExistingRoom(player, roomId, null);
    }

    public boolean joinExistingRoom(ClientHandler player, String roomId, String password) {
        Room room = getRoom(roomId);
        if (room == null) {
            log.warn("房间不存在: {}", roomId);
            return false;
        }
        if (room.getPlayerCount() >= 2) {
            log.warn("房间 {} 已满", roomId);
            return false;
        }
        if (room.getStatus() == Room.Status.PLAYING) {
            log.warn("房间 {} 游戏中，无法加入", roomId);
            return false;
        }
        
        if (!room.checkPassword(password)) {
            log.warn("房间 {} 密码错误", roomId);
            return false;
        }

        boolean success = room.join(player);
        if (success) {
            log.info("玩家 {} 加入房间 {}", player.getUserId(), roomId);
        }
        return success;
    }

    /**
     * 玩家离开当前房间
     */
    public void leaveRoom(ClientHandler player) {
        Room room = player.getCurrentRoom();
        if (room != null) {
            String roomId = room.getRoomId();
            room.leave(player);
            log.info("玩家 {} 离开房间 {}", player.getUserId(), roomId);
            player.setCurrentRoom(null);
            // 游戏已结束且无玩家时，自动清理房间
            if (room.getStatus() == Room.Status.FINISHED && room.getPlayerCount() == 0) {
                removeRoom(roomId);
                log.info("房间 {} 已结束且无玩家，自动清理", roomId);
            }
        }
    }
}


