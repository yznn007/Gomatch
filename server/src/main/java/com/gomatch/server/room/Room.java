package com.gomatch.server.room;

import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import com.gomatch.server.game.AIPlayer;
import com.gomatch.server.game.Game;
import com.gomatch.server.redis.RedisManager;
import com.gomatch.server.handler.ClientHandler;
import com.gomatch.server.dao.GameActionDAO;
import com.gomatch.server.dao.RankDAO;
import com.gomatch.server.dao.RecordDAO;
import com.gomatch.server.dao.RoomWatcherDAO;
import com.gomatch.server.dao.UserDAO;
import com.gomatch.server.storage.StorageManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Room {
    private static final Logger log = LoggerFactory.getLogger(Room.class);
    private static final RoomWatcherDAO roomWatcherDAO = new RoomWatcherDAO();
    private static final GameActionDAO gameActionDAO = new GameActionDAO();

    public enum Status { WAITING, PLAYING, FINISHED }
    public enum GameType { NORMAL, RANKED }

    private final String roomId;
    private final String roomName;
    private final ClientHandler owner;
    private final String password;
    private Status status = Status.WAITING;
    private final Game game = new Game();
    private final List<ClientHandler> players = new CopyOnWriteArrayList<>();
    private final List<ClientHandler> observers = new CopyOnWriteArrayList<>();

    private long blackUserId;
    private long whiteUserId;
    private String blackUsername;
    private String whiteUsername;
    private int blackScore;
    private int whiteScore;
    private String blackAvatarUrl;
    private String whiteAvatarUrl;
    private boolean gameStarted;
    private Instant gameStartTime;
    private boolean blackReady = false;
    private boolean whiteReady = false;

    private Long undoRequesterId;
    private Long drawRequesterId;
    private Long rematchRequesterId;
    private long currentRecordId = -1;
    private Long pendingUndoActionId;
    private Long pendingDrawActionId;

    private boolean aiMode;
    private AIPlayer aiPlayer;
    private AIPlayer.Difficulty aiDifficulty = AIPlayer.Difficulty.MEDIUM;
    private GameType gameType = GameType.NORMAL;

    // 断线重连管理
    private static final int MAX_DISCONNECT_COUNT = 3;   // 累计断线次数上限（超过判负）
    private static final long RECONNECT_TIMEOUT_SEC = 60;
    private final Set<Long> disconnectedPlayerIds = new HashSet<>();  // 当前断线的玩家ID集合
    private final Map<Long, Integer> playerDisconnectCounts = new java.util.HashMap<>(); // 每局玩家断线次数
    private final Map<Long, ScheduledFuture<?>> reconnectTimerFutures = new HashMap<>(); // 每个玩家的断线倒计时

    // 回合计时器
    private static final int TURN_TIMEOUT_SECONDS = 60;
    private static final ScheduledExecutorService turnTimerPool =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "turn-timer");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> turnTimerFuture;

    private static final RecordDAO recordDAO = new RecordDAO();
    private static final UserDAO userDAO = new UserDAO();
    private static final RankDAO rankDAO = new RankDAO();

    public Room(String roomId, String roomName, ClientHandler owner) {
        this(roomId, roomName, owner, null);
    }

    public Room(String roomId, String roomName, ClientHandler owner, String password) {
        this(roomId, roomName, owner, password, false);
    }

    public Room(String roomId, String roomName, ClientHandler owner, String password, boolean aiMode) {
        this(roomId, roomName, owner, password, aiMode, GameType.NORMAL);
    }

    public Room(String roomId, String roomName, ClientHandler owner, String password, boolean aiMode, GameType gameType) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.owner = owner;
        this.password = password;
        this.aiMode = aiMode;
        this.gameType = gameType;
        log.info("Room created: roomId={}, roomName={}, owner={}, hasPassword={}, aiMode={}, gameType={}", roomId, roomName,
                owner != null ? owner.getUserId() : "null", password != null && !password.isEmpty(), aiMode, gameType);
    }

    public void setAiDifficulty(AIPlayer.Difficulty difficulty) {
        this.aiDifficulty = difficulty;
    }

    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public Status getStatus() { return status; }
    public boolean hasPassword() { return password != null && !password.isEmpty(); }
    public boolean checkPassword(String inputPassword) {
        if (!hasPassword()) return true;
        return password.equals(inputPassword);
    }
    public void setStatus(Status status) { this.status = status; }
    public boolean isAiMode() { return aiMode; }
    public GameType getGameType() { return gameType; }
    public ClientHandler getOwner() { return owner; }
    public Game getGame() { return game; }
    public List<ClientHandler> getPlayers() { return players; }
    public List<ClientHandler> getObservers() { return observers; }
    public Instant getGameStartTime() { return gameStartTime; }
    public boolean isGameStarted() { return gameStarted; }
    public long getBlackUserId() { return blackUserId; }
    public long getWhiteUserId() { return whiteUserId; }
    public String getBlackUsername() { return blackUsername; }
    public String getWhiteUsername() { return whiteUsername; }
    public int getBlackScore() { return blackScore; }
    public int getWhiteScore() { return whiteScore; }

    public int getPlayerCount() { return players.size(); }
    public Set<Long> getDisconnectedPlayerIds() { return disconnectedPlayerIds; }
    /** 判断指定玩家当前是否断线 */
    public boolean isPlayerDisconnected(long userId) { return disconnectedPlayerIds.contains(userId); }
    public String getPlayerUsername(long userId) {
        for (ClientHandler p : players) {
            if (p.getUserId() == userId) return p.getUsername();
        }
        return String.valueOf(userId);
    }
    /** 根据 userId 查找玩家 ClientHandler（用于定向发送消息） */
    public synchronized ClientHandler findPlayerById(long userId) {
        for (ClientHandler p : players) {
            if (p.getUserId() == userId) return p;
        }
        return null;
    }
    public int getObserverCount() { return observers.size(); }

    public synchronized boolean isObserver(ClientHandler handler) {
        boolean result = observers.contains(handler);
        log.debug("isObserver check: handler={}, isObserver={}, observers_size={}",
                handler != null ? handler.getUserId() : "null", result, observers.size());
        return result;
    }

    public synchronized boolean join(ClientHandler handler) {
        log.debug("Attempting join room {}: handler={}, currentStatus={}, playerCount={}",
                roomId, handler != null ? handler.getUserId() : "null", status, players.size());

        if (status != Status.WAITING) {
            log.warn("Join rejected: room {} status is {} (expected WAITING)", roomId, status);
            return false;
        }

        if (players.size() >= 2) {
            log.warn("Join rejected: room {} is full (2/2 players)", roomId);
            return false;
        }

        if (players.contains(handler)) {
            log.warn("Join rejected: player {} already in room {}", handler.getUserId(), roomId);
            return false;
        }

        if (observers.contains(handler)) {
            log.warn("Join rejected: handler {} is currently an observer, must leave observer first",
                    handler.getUserId());
            return false;
        }

        players.add(handler);
        log.info("Player {} joined room {}: current players {}/2",
                handler.getUserId(), roomId, players.size());

        if (players.size() == 1) {
            blackUserId = handler.getUserId();
            blackUsername = handler.getUsername();
            log.info("Player {} assigned as BLACK in room {}", handler.getUserId(), roomId);
        } else if (players.size() == 2) {
            whiteUserId = handler.getUserId();
            whiteUsername = handler.getUsername();
            log.info("Player {} assigned as WHITE in room {}", handler.getUserId(), roomId);
        }

        return true;
    }

    public synchronized boolean setReady(long userId, boolean ready) {
        if (userId == blackUserId) {
            blackReady = ready;
            log.info("Player {} (BLACK) ready status changed to {} in room {}", userId, ready, roomId);
        } else if (userId == whiteUserId) {
            whiteReady = ready;
            log.info("Player {} (WHITE) ready status changed to {} in room {}", userId, ready, roomId);
        } else {
            log.warn("setReady rejected: player {} is not in room {}", userId, roomId);
            return false;
        }
        return true;
    }

    public synchronized boolean isBlackReady() { return blackReady; }
    public synchronized boolean isWhiteReady() { return whiteReady; }
    public synchronized boolean bothReady() { return blackReady && whiteReady; }

    public synchronized void resetReady() {
        blackReady = false;
        whiteReady = false;
    }

    public synchronized void leave(ClientHandler handler) {
        leave(handler, true);
    }

    public synchronized void leave(ClientHandler handler, boolean resetRoom) {
        log.debug("Attempting leave room {}: handler={}, resetRoom={}", roomId,
                handler != null ? handler.getUserId() : "null", resetRoom);

        boolean removedFromPlayers = players.remove(handler);
        boolean removedFromObservers = observers.remove(handler);

        if (removedFromPlayers) {
            log.info("Player {} left room {}: remaining players {}/2",
                    handler.getUserId(), roomId, players.size());
            // 游戏已结束的房间不再重置，等待清理
            if (status == Status.FINISHED) {
                log.info("Room {} is finished, skip reset", roomId);
            } else if (resetRoom && players.size() < 2) {
                log.info("Room {} reset: less than 2 players, status changed to WAITING", roomId);
                cancelTurnTimer();
                status = Status.WAITING;
                gameStarted = false;
                game.reset();
                blackUserId = 0;
                whiteUserId = 0;
                gameStartTime = null;
            }
        } else if (removedFromObservers) {
            log.info("Observer {} left room {}: remaining observers {}",
                    handler.getUserId(), roomId, observers.size());
        } else {
            log.warn("Leave ignored: handler {} is not in room {} (neither player nor observer)",
                    handler != null ? handler.getUserId() : "null", roomId);
        }
    }

    public synchronized void disconnect(ClientHandler handler) {
        log.debug("Attempting disconnect from room {}: handler={}", roomId,
                handler != null ? handler.getUserId() : "null");

        boolean removedFromPlayers = players.remove(handler);
        if (removedFromPlayers) {
            log.info("Player {} disconnected from room {}: remaining players {}/2",
                    handler.getUserId(), roomId, players.size());

            // 对局进行中玩家断线
            if (status == Status.PLAYING
                    && (handler.getUserId() == blackUserId || handler.getUserId() == whiteUserId)) {
                long uid = handler.getUserId();

                // AI 对战中玩家断线：直接判 AI 胜（玩家负），无重连机会
                if (aiMode) {
                    long aiWinnerId = (uid == blackUserId) ? whiteUserId : blackUserId;
                    String loserName = getPlayerUsername(uid);
                    log.info("AI mode: player {} disconnected from room {}, AI (id={}) wins by disconnect",
                            uid, roomId, aiWinnerId);
                    endGame(aiWinnerId, "ai_disconnect",
                            loserName + " 断线，AI 获胜");
                    broadcastAll(Message.of("AI_PLAYER_DISCONNECTED",
                            "userId", uid,
                            "msg", loserName + " 断线，AI 获胜"));
                    return;
                }

                int count = playerDisconnectCounts.getOrDefault(uid, 0) + 1;
                playerDisconnectCounts.put(uid, count);

                if (count >= MAX_DISCONNECT_COUNT) {
                    // 累计断线3次，立即判负
                    long winnerId = (uid == blackUserId) ? whiteUserId : blackUserId;
                    String loserName = getPlayerUsername(uid);
                    log.info("Player {} disconnected {} times in room {}, declaring {} as winner",
                            uid, count, roomId, winnerId);
                    disconnectedPlayerIds.remove(uid);
                    cancelReconnectTimer(uid);
                    endGame(winnerId, "disconnect_limit",
                            loserName + " 累计断线" + count + "次，自动判负");
                    broadcastAll(Message.of("PLAYER_DISCONNECT_LIMIT",
                            "userId", uid,
                            "disconnectCount", count,
                            "msg", loserName + " 累计断线" + count + "次，自动判负"));
                    return;
                }

                disconnectedPlayerIds.add(uid);
                startReconnectTimer(uid);
                broadcastToPlayers(Message.of("PLAYER_DISCONNECTED",
                        "userId", uid,
                        "disconnectCount", count));
                log.info("Player {} disconnected during game in room {} (disconnect #{}/{}), reconnect timer started ({}s)",
                        uid, roomId, count, MAX_DISCONNECT_COUNT, RECONNECT_TIMEOUT_SEC);
            }
        } else {
            boolean removedFromObservers = observers.remove(handler);
            if (removedFromObservers) {
                roomWatcherDAO.stopWatching(roomId, handler.getUserId());
                log.info("Observer {} disconnected from room {}", handler.getUserId(), roomId);
            }
        }
    }

    public synchronized void spectate(ClientHandler handler) {
        log.debug("Attempting spectate room {}: handler={}", roomId,
                handler != null ? handler.getUserId() : "null");

        if (players.contains(handler)) {
            log.warn("Spectate rejected: player {} cannot be an observer in room {}",
                    handler.getUserId(), roomId);
            return;
        }

        if (observers.contains(handler)) {
            log.debug("Spectate ignored: handler {} is already an observer in room {}",
                    handler.getUserId(), roomId);
            return;
        }

        observers.add(handler);
        roomWatcherDAO.startWatching(roomId, handler.getUserId());
        log.info("Observer {} joined room {}: total observers {}",
                handler.getUserId(), roomId, observers.size());
    }

    public synchronized void leaveObserver(ClientHandler handler) {
        boolean removed = observers.remove(handler);
        if (removed) {
            roomWatcherDAO.stopWatching(roomId, handler.getUserId());
            log.info("Observer {} left room {}: remaining observers {}",
                    handler.getUserId(), roomId, observers.size());
        } else {
            log.warn("leaveObserver ignored: handler {} is not an observer in room {}",
                    handler != null ? handler.getUserId() : "null", roomId);
        }
    }

    public synchronized boolean startGame() {
        log.debug("Attempting startGame room {}: playerCount={}, status={}, gameStarted={}, aiMode={}",
                roomId, players.size(), status, gameStarted, aiMode);

        if (!aiMode && players.size() != 2) {
            log.warn("startGame rejected: room {} needs 2 players, got {}", roomId, players.size());
            return false;
        }

        if (aiMode && players.size() != 1) {
            log.warn("startGame rejected: AI room {} needs exactly 1 player, got {}", roomId, players.size());
            return false;
        }

        if (status != Status.WAITING) {
            log.warn("startGame rejected: room {} status is {} (expected WAITING)", roomId, status);
            return false;
        }

        if (blackUserId == 0) {
            log.error("startGame rejected: blackUserId={} is not set", blackUserId);
            return false;
        }

        status = Status.PLAYING;
        gameStarted = true;
        game.reset();
        gameStartTime = Instant.now();
        playerDisconnectCounts.clear();

        if (aiMode) {
            whiteUserId = -1;
            whiteUsername = "AI";
            whiteScore = 0;
            whiteAvatarUrl = "";
            // 查询黑方积分和头像
            UserDAO.UserInfo blackInfo = userDAO.getUserById(blackUserId);
            blackScore = blackInfo != null ? blackInfo.getScore() : 0;
            blackAvatarUrl = resolvePlayerAvatar(blackUserId);
            aiPlayer = new AIPlayer(Game.WHITE, aiDifficulty);
            log.info("AI game started in room {}: BLACK={}, AI=WHITE, difficulty={}, startTime={}",
                    roomId, blackUserId, aiDifficulty, gameStartTime);
        } else {
            if (whiteUserId == 0) {
                log.error("startGame rejected: whiteUserId={} is not set", whiteUserId);
                return false;
            }
            // 查询双方积分和头像
            UserDAO.UserInfo blackInfo = userDAO.getUserById(blackUserId);
            UserDAO.UserInfo whiteInfo = userDAO.getUserById(whiteUserId);
            blackScore = blackInfo != null ? blackInfo.getScore() : 0;
            whiteScore = whiteInfo != null ? whiteInfo.getScore() : 0;
            blackAvatarUrl = resolvePlayerAvatar(blackUserId);
            whiteAvatarUrl = resolvePlayerAvatar(whiteUserId);
            log.info("Game started in room {}: BLACK={}({}分), WHITE={}({}分), startTime={}",
                    roomId, blackUserId, blackScore, whiteUserId, whiteScore, gameStartTime);
        }

        startTurnTimer();
        return true;
    }

    public synchronized void makeAiMove() {
        if (!aiMode || !gameStarted || status != Status.PLAYING || aiPlayer == null) {
            log.debug("makeAiMove skipped: aiMode={}, gameStarted={}, status={}", aiMode, gameStarted, status);
            return;
        }

        if (game.getCurrentPlayer() != Game.WHITE) {
            log.debug("makeAiMove skipped: not AI's turn, currentPlayer={}", game.getCurrentPlayer());
            return;
        }

        int[] move = aiPlayer.getMove(game.getBoard());
        int x = move[0];
        int y = move[1];

        log.info("AI making move in room {}: ({},{})", roomId, x, y);

        boolean success = game.move(x, y, -1);
        if (success) {
            broadcastAll(Message.of(Protocol.MOVE_SYNC,
                    "x", x,
                    "y", y,
                    "color", Game.WHITE));

            if (game.isFinished()) {
            int winnerColor = game.getWinner();
            long winnerId = winnerColor == Game.EMPTY ? 0 : 
                    (winnerColor == Game.BLACK ? blackUserId : whiteUserId);
            String reason = winnerColor == Game.EMPTY ? "draw" : "five_in_a_row";
            String msg = winnerColor == Game.EMPTY ? "棋盘已满，平局" : 
                    (winnerColor == Game.BLACK ? "黑方获胜!" : "AI 获胜!");
            endGame(winnerId, reason, msg);
                log.info("AI game ended in room {}: winnerColor={}, winnerId={}, reason={}",
                        roomId, winnerColor == Game.BLACK ? "BLACK" : winnerColor == Game.WHITE ? "WHITE" : "DRAW", winnerId, reason);
            } else {
                startTurnTimer(); // AI 走完后轮到人类，重新计时
            }
        }
    }

    public static JSONObject buildScoreChangesJson(Map<Long, Integer> scoreChangeMap) {
        JSONObject obj = new JSONObject();
        for (Map.Entry<Long, Integer> e : scoreChangeMap.entrySet()) {
            obj.put(String.valueOf(e.getKey()), e.getValue());
        }
        return obj;
    }

    public synchronized void endGame(long winnerId, String reason, String message) {
        log.info("endGame called for room {}: winnerId={}, reason={}, currentStatus={}",
                roomId, winnerId, reason, status);

        cancelTurnTimer();
        // 清除所有断线状态（对局结束，不再需要重连计时）
        disconnectedPlayerIds.clear();
        cancelAllReconnectTimers();
        status = Status.FINISHED;
        gameStarted = false;

        long durationSeconds = 0;
        if (gameStartTime != null) {
            durationSeconds = Duration.between(gameStartTime, Instant.now()).getSeconds();
            log.info("Game duration for room {}: {} seconds", roomId, durationSeconds);
        }

        // 计算双方积分变化（所有模式统一计算，AI 模式变化为 0）
        Map<Long, Integer> scoreChangeMap = new HashMap<>();
        int p1ScoreChange = 0;
        int p2ScoreChange = 0;

        if (aiMode) {
            scoreChangeMap.put(blackUserId, 0);
            scoreChangeMap.put(whiteUserId, 0);
        } else {
            UserDAO.UserInfo blackInfo = userDAO.getUserById(blackUserId);
            UserDAO.UserInfo whiteInfo = userDAO.getUserById(whiteUserId);

            if (winnerId != 0) {
                long loserId = (winnerId == blackUserId) ? whiteUserId : blackUserId;
                UserDAO.UserInfo winnerInfo = winnerId == blackUserId ? blackInfo : whiteInfo;
                UserDAO.UserInfo loserInfo = loserId == blackUserId ? blackInfo : whiteInfo;

                if (gameType == GameType.RANKED) {
                    int[] deltas = updateRankedScore(winnerInfo, loserInfo, true, game.getMoveCount());
                    p1ScoreChange = winnerId == blackUserId ? deltas[0] : deltas[1];
                    p2ScoreChange = winnerId == whiteUserId ? deltas[0] : deltas[1];
                } else {
                    userDAO.updateWinCount(winnerId);
                    userDAO.updateLoseCount(loserId);
                    log.info("Normal match result recorded: winner={}, loser={}", winnerId, loserId);
                }
            } else {
                userDAO.updateDrawCount(blackUserId);
                userDAO.updateDrawCount(whiteUserId);

                if (gameType == GameType.RANKED) {
                    int[] deltas = updateRankedScore(blackInfo, whiteInfo, false, game.getMoveCount());
                    p1ScoreChange = deltas[0];
                    p2ScoreChange = deltas[1];
                }

                log.info("Draw count updated successfully for both players: black={}, white={}",
                        blackUserId, whiteUserId);
            }

            scoreChangeMap.put(blackUserId, p1ScoreChange);
            scoreChangeMap.put(whiteUserId, p2ScoreChange);
        }

        // 保存对战记录到 MySQL
        try {
            List<RecordDAO.MoveRecord> moves = new ArrayList<>();
            for (Game.MoveRecord mr : game.getMoveHistory()) {
                moves.add(new RecordDAO.MoveRecord(mr.x, mr.y, mr.color, mr.userId));
            }

            log.debug("Saving game record: roomId={}, blackUserId={}, whiteUserId={}, winnerId={}, " +
                    "reason={}, moveCount={}, startTime={}",
                    roomId, blackUserId, whiteUserId, winnerId, reason, moves.size(), gameStartTime);

            currentRecordId = recordDAO.saveRecord(roomId, blackUserId, whiteUserId,
                    winnerId == 0 ? null : winnerId, reason, moves, gameStartTime,
                    aiMode ? "AI" : "PVP",
                    p1ScoreChange, p2ScoreChange);

            if (currentRecordId > 0) {
                log.info("Game record saved successfully for room {}: recordId={}, {} moves recorded",
                        roomId, currentRecordId, moves.size());
            } else {
                log.error("Game record save returned invalid id for room {}: saveRecord returned {}",
                        roomId, currentRecordId);
            }

            // 缓存对战记录到 Redis（3天TTL，失败不影响 MySQL 记录）
            try {
                String boardData = new com.google.gson.Gson().toJson(moves);
                cacheGameRecordToRedis(blackUserId, whiteUserId, winnerId, reason,
                        moves.size(), durationSeconds, p1ScoreChange, p2ScoreChange, boardData);
            } catch (Exception redisEx) {
                log.warn("Failed to cache game record to Redis for room {} (MySQL save ok): {}",
                        roomId, redisEx.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to save game record to MySQL for room {}: {}", roomId, e.getMessage(), e);
        }

        // 对局结束后主动推送双方最新积分与排名到客户端
        try {
            UserDAO.UserInfo blackInfo = userDAO.getUserById(blackUserId);
            UserDAO.UserInfo whiteInfo = userDAO.getUserById(whiteUserId);
            for (ClientHandler handler : players) {
                long id = handler.getUserId();
                UserDAO.UserInfo info = id == blackUserId ? blackInfo : (id == whiteUserId ? whiteInfo : null);
                if (info != null) {
                    RankDAO.RankInfo rankInfo = rankDAO.getRankByUserId(id);
                    int rank = rankInfo != null ? rankInfo.getRank() : 0;
                    handler.send(Message.of(Protocol.USER_SCORE_UPDATE,
                        "score", info.getScore(),
                        "winCount", info.getWinCount(),
                        "loseCount", info.getLoseCount(),
                        "drawCount", info.getDrawCount(),
                        "rank", rank));
                }
            }
            log.info("Score update pushed to players in room {}: black={}(score={}), white={}(score={})",
                    roomId, blackUserId, blackInfo != null ? blackInfo.getScore() : "N/A",
                    whiteUserId, whiteInfo != null ? whiteInfo.getScore() : "N/A");
        } catch (Exception e) {
            log.error("Failed to push score update in room {}: {}", roomId, e.getMessage(), e);
        }

        // 广播游戏结束消息（附带每位玩家的积分变化）
        broadcastAll(Message.of(Protocol.GAME_OVER,
                "winnerId", winnerId,
                "msg", message != null ? message : "游戏结束",
                "scoreChanges", buildScoreChangesJson(scoreChangeMap)));

        try {
            RedisManager.getInstance().deleteGameState(roomId);
            log.debug("Redis game state deleted for room {}", roomId);
        } catch (Exception e) {
            log.error("Failed to delete Redis game state for room {}: {}", roomId, e.getMessage(), e);
        }

        log.info("Game ended in room {}: winner={}, reason={}, duration={}s",
                roomId, winnerId, reason, durationSeconds);
    }

    /**
     * 将对战记录缓存到 Redis，双方玩家各存一份，3天后自动过期
     */
    private void cacheGameRecordToRedis(long player1Id, long player2Id, long winnerId,
                                        String result, int moveCount, long duration,
                                        int p1ScoreChange, int p2ScoreChange, String boardData) {
        try {
            UserDAO.UserInfo p1Info = userDAO.getUserById(player1Id);
            UserDAO.UserInfo p2Info = userDAO.getUserById(player2Id);
            String p1Name = p1Info != null ? p1Info.getUsername() : "未知";
            String p2Name = p2Info != null ? p2Info.getUsername() : (aiMode ? "AI" : "未知");
            // 解析双方头像 URL：优先用本房间已解析过的 blackAvatarUrl/whiteAvatarUrl，回退到重新查询
            // 这样回放/记录列表页面就能直接显示双方头像图片
            String p1Avatar = "";
            String p2Avatar = "";
            if (player1Id == blackUserId) {
                p1Avatar = blackAvatarUrl != null ? blackAvatarUrl : resolvePlayerAvatar(blackUserId);
            } else if (player1Id == whiteUserId) {
                p1Avatar = whiteAvatarUrl != null ? whiteAvatarUrl : resolvePlayerAvatar(whiteUserId);
            }
            if (player2Id == blackUserId) {
                p2Avatar = blackAvatarUrl != null ? blackAvatarUrl : resolvePlayerAvatar(blackUserId);
            } else if (player2Id == whiteUserId) {
                p2Avatar = whiteAvatarUrl != null ? whiteAvatarUrl : resolvePlayerAvatar(whiteUserId);
            }
            if (p1Avatar == null) p1Avatar = "";
            if (p2Avatar == null) p2Avatar = "";
            long now = System.currentTimeMillis();
            String createTime = java.time.Instant.now().toString().replace("T", " ").substring(0, 19);

            // 将 boardData 字符串解析为 JSONArray 直接存入，避免双重 JSON 编码
            // (org.json 在 put(String, String) 时会对引号转义，导致反序列化时拿到的是
            //  "转义后字符串"，optString 后再喂给 Gson 解析会失败)
            org.json.JSONArray movesArray = new org.json.JSONArray();
            if (boardData != null && !boardData.isEmpty()) {
                try {
                    org.json.JSONArray parsed = new org.json.JSONArray(boardData);
                    movesArray = parsed;
                } catch (Exception parseEx) {
                    log.warn("boardData not valid JSON array, store as empty: {}", parseEx.getMessage());
                }
            }

            // 为 player1 构建记录
            JSONObject recForP1 = new JSONObject();
            recForP1.put("recordId", currentRecordId);
            recForP1.put("player1Id", player1Id);
            recForP1.put("player1Name", p1Name);
            recForP1.put("player1Score", blackScore);
            recForP1.put("player1Avatar", p1Avatar);
            recForP1.put("player2Id", player2Id);
            recForP1.put("player2Name", p2Name);
            recForP1.put("player2Score", whiteScore);
            recForP1.put("player2Avatar", p2Avatar);
            recForP1.put("winnerId", winnerId);
            recForP1.put("result", result);
            recForP1.put("moveCount", moveCount);
            recForP1.put("duration", duration);
            recForP1.put("createTime", createTime);
            recForP1.put("scoreChange", p1ScoreChange);
            recForP1.put("boardData", movesArray);

            // 为 player2 构建记录
            JSONObject recForP2 = new JSONObject();
            recForP2.put("recordId", currentRecordId);
            recForP2.put("player1Id", player1Id);
            recForP2.put("player1Name", p1Name);
            recForP2.put("player1Score", blackScore);
            recForP2.put("player1Avatar", p1Avatar);
            recForP2.put("player2Id", player2Id);
            recForP2.put("player2Name", p2Name);
            recForP2.put("player2Score", whiteScore);
            recForP2.put("player2Avatar", p2Avatar);
            recForP2.put("winnerId", winnerId);
            recForP2.put("result", result);
            recForP2.put("moveCount", moveCount);
            recForP2.put("duration", duration);
            recForP2.put("createTime", createTime);
            recForP2.put("scoreChange", p2ScoreChange);
            recForP2.put("boardData", movesArray);

            RedisManager redis = RedisManager.getInstance();
            redis.saveGameRecord(player1Id, recForP1.toString(), now);
            redis.saveGameRecord(player2Id, recForP2.toString(), now);
            log.debug("Game record cached in Redis for room {}: p1={}, p2={}", roomId, player1Id, player2Id);
        } catch (Exception e) {
            log.warn("Failed to cache game record in Redis for room {}: {}", roomId, e.getMessage());
        }
    }

    private int[] updateRankedScore(UserDAO.UserInfo winnerInfo, UserDAO.UserInfo loserInfo, boolean hasWinner, int moveCount) {
        if (winnerInfo == null || loserInfo == null) {
            log.warn("updateRankedScore failed: user info not found");
            return new int[]{0, 0};
        }

        long winnerId = winnerInfo.getUserId();
        long loserId = loserInfo.getUserId();

        if (hasWinner) {
            int minSteps = 5;
            int maxSteps = 225;
            int minScore = 25;
            int maxScore = 50;

            int clampedSteps = Math.max(minSteps, Math.min(moveCount, maxSteps));
            int scoreDelta = minScore + (int) ((clampedSteps - minSteps) * (maxScore - minScore) / (maxSteps - minSteps));

            userDAO.updateScore(winnerId, scoreDelta);
            userDAO.updateWinCount(winnerId);
            userDAO.updateScore(loserId, -scoreDelta);
            userDAO.updateLoseCount(loserId);

            log.info("Ranked score updated: winner={}(+{}), loser=({}), moveCount={}",
                    winnerId, scoreDelta, loserId, -scoreDelta, moveCount);
            return new int[]{scoreDelta, -scoreDelta};
        } else {
            userDAO.updateScore(winnerId, 0);
            userDAO.updateScore(loserId, 0);

            log.info("Ranked draw score updated: no score change");
            return new int[]{0, 0};
        }
    }

    public synchronized boolean makeMove(int x, int y, ClientHandler handler) {
        log.debug("makeMove called for room {}: x={}, y={}, handler={}",
                roomId, x, y, handler != null ? handler.getUserId() : "null");

        if (status != Status.PLAYING) {
            log.warn("makeMove rejected: room {} status is {} (expected PLAYING)", roomId, status);
            return false;
        }

        if (handler == null) {
            log.warn("makeMove rejected: handler is null");
            return false;
        }

        if (observers.contains(handler)) {
            log.warn("makeMove rejected: observer {} cannot make moves in room {}",
                    handler.getUserId(), roomId);
            return false;
        }

        if (!players.contains(handler)) {
            log.warn("makeMove rejected: handler {} is not a player in room {}",
                    handler.getUserId(), roomId);
            return false;
        }

        int currentPlayer = game.getCurrentPlayer();
        long expectedUserId = (currentPlayer == Game.BLACK) ? blackUserId : whiteUserId;

        log.debug("makeMove validation: currentPlayer={}, expectedUserId={}, handlerUserId={}",
                currentPlayer == Game.BLACK ? "BLACK" : "WHITE", expectedUserId, handler.getUserId());

        if (handler.getUserId() != expectedUserId) {
            log.warn("makeMove rejected: player {} is not current turn (expected {})",
                    handler.getUserId(), expectedUserId);
            return false;
        }

        boolean success = game.move(x, y, handler.getUserId());
        if (success) {
            log.info("Move successful in room {}: ({},{}) by player {} (userId={})",
                    roomId, x, y, currentPlayer == Game.BLACK ? "BLACK" : "WHITE", handler.getUserId());
            undoRequesterId = null;
            drawRequesterId = null;
            pendingUndoActionId = null;
            pendingDrawActionId = null;
            startTurnTimer();

            if (aiMode && !game.isFinished()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        synchronized (this) {
                            makeAiMove();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, "ai-move-" + roomId).start();
            }
        } else {
            log.warn("Move failed in room {}: ({},{}) by player {}",
                    roomId, x, y, handler.getUserId());
        }
        return success;
    }

    // ==================== 回合计时器 ====================

    private synchronized void startTurnTimer() {
        cancelTurnTimer();
        if (status != Status.PLAYING) return;

        // AI 模式下只对人类玩家（黑方）计时，AI 几乎立即落子不需要计时
        if (aiMode && game.getCurrentPlayer() != Game.BLACK) return;

        long currentPlayerId = (game.getCurrentPlayer() == Game.BLACK) ? blackUserId : whiteUserId;
        log.debug("Starting turn timer for room {}: currentPlayer={}, timeout={}s", roomId, currentPlayerId, TURN_TIMEOUT_SECONDS);

        turnTimerFuture = turnTimerPool.schedule(() -> {
            synchronized (Room.this) {
                if (status != Status.PLAYING) return;
                long timedOutPlayerId = (game.getCurrentPlayer() == Game.BLACK) ? blackUserId : whiteUserId;
                long winnerId = (timedOutPlayerId == blackUserId) ? whiteUserId : blackUserId;
                log.info("Turn timeout in room {}: player {} ran out of time, winner={}", roomId, timedOutPlayerId, winnerId);
                endGame(winnerId, "timeout", "回合超时，对方获胜");
                // GAME_OVER 已在 endGame() 中广播，不再重复发送 TURN_TIMEOUT 避免客户端弹两次框
            }
        }, TURN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private synchronized void cancelTurnTimer() {
        if (turnTimerFuture != null && !turnTimerFuture.isDone()) {
            turnTimerFuture.cancel(false);
            turnTimerFuture = null;
            log.debug("Turn timer cancelled for room {}", roomId);
        }
    }

    public synchronized boolean requestUndo(long requesterId) {
        log.debug("requestUndo called for room {}: requesterId={}", roomId, requesterId);

        if (status != Status.PLAYING) {
            log.warn("requestUndo rejected: room {} status is {}", roomId, status);
            return false;
        }

        if (game.getMoveCount() < 2) {
            log.warn("requestUndo rejected: not enough moves (need at least 2)");
            return false;
        }

        if (undoRequesterId != null) {
            log.warn("requestUndo rejected: undo request already pending from {}", undoRequesterId);
            return false;
        }

        if (!isValidPlayer(requesterId)) {
            log.warn("requestUndo rejected: {} is not a valid player", requesterId);
            return false;
        }

        // 只有当前回合方（尚未落子的一方）才能请求悔棋
        int requesterColor = (requesterId == blackUserId) ? Game.BLACK : Game.WHITE;
        if (game.getCurrentPlayer() != requesterColor) {
            log.warn("requestUndo rejected: requester {} is not the current player", requesterId);
            return false;
        }

        undoRequesterId = requesterId;
        pendingUndoActionId = gameActionDAO.recordAction(currentRecordId > 0 ? currentRecordId : null, requesterId, "undo", game.getMoveCount());
        log.info("Undo requested by {} in room {}, actionId={}", requesterId, roomId, pendingUndoActionId);
        return true;
    }

    public synchronized boolean replyUndo(long replierId, boolean accept) {
        log.debug("replyUndo called for room {}: replierId={}, accept={}", roomId, replierId, accept);

        if (undoRequesterId == null) {
            log.warn("replyUndo rejected: no pending undo request");
            return false;
        }

        if (replierId == undoRequesterId) {
            log.warn("replyUndo rejected: requester cannot reply to own request");
            return false;
        }

        if (!isValidPlayer(replierId)) {
            log.warn("replyUndo rejected: {} is not a valid player", replierId);
            return false;
        }

        if (accept) {
            // 撤销两步：对手最后一步 + 请求者上一步
            int movesBefore = game.getMoveCount();
            game.undo(); // 撤销对手的最后一步
            game.undo(); // 撤销请求者的上一步
            log.info("Undo accepted by {} in room {}: reverted {} moves",
                    replierId, roomId, movesBefore - game.getMoveCount());
            if (pendingUndoActionId != null) {
                gameActionDAO.updateActionStatus(pendingUndoActionId, "accepted");
            }
            undoRequesterId = null;
            pendingUndoActionId = null;
            startTurnTimer(); // 悔棋后回合变更，重新计时
            return true;
        } else {
            log.info("Undo rejected by {} in room {}", replierId, roomId);
            if (pendingUndoActionId != null) {
                gameActionDAO.updateActionStatusWithReason(pendingUndoActionId, "rejected", "opponent rejected");
            }
            undoRequesterId = null;
            pendingUndoActionId = null;
            return true;
        }
    }

    public synchronized boolean requestDraw(long requesterId) {
        log.debug("requestDraw called for room {}: requesterId={}", roomId, requesterId);

        if (status != Status.PLAYING) {
            log.warn("requestDraw rejected: room {} status is {}", roomId, status);
            return false;
        }

        if (drawRequesterId != null) {
            log.warn("requestDraw rejected: draw request already pending from {}", drawRequesterId);
            return false;
        }

        if (!isValidPlayer(requesterId)) {
            log.warn("requestDraw rejected: {} is not a valid player", requesterId);
            return false;
        }

        drawRequesterId = requesterId;
        pendingDrawActionId = gameActionDAO.recordAction(currentRecordId > 0 ? currentRecordId : null, requesterId, "draw", game.getMoveCount());
        log.info("Draw requested by {} in room {}, actionId={}", requesterId, roomId, pendingDrawActionId);
        return true;
    }

    public synchronized boolean replyDraw(long replierId, boolean accept) {
        log.debug("replyDraw called for room {}: replierId={}, accept={}", roomId, replierId, accept);

        if (drawRequesterId == null) {
            log.warn("replyDraw rejected: no pending draw request");
            return false;
        }

        if (replierId == drawRequesterId) {
            log.warn("replyDraw rejected: requester cannot reply to own request");
            return false;
        }

        if (!isValidPlayer(replierId)) {
            log.warn("replyDraw rejected: {} is not a valid player", replierId);
            return false;
        }

        if (accept) {
            log.info("Draw accepted by {} in room {}", replierId, roomId);
            endGame(0, "draw", "双方平局(求和)");
            if (pendingDrawActionId != null) {
                gameActionDAO.updateActionStatus(pendingDrawActionId, "accepted");
            }
            drawRequesterId = null;
            pendingDrawActionId = null;
            return true;
        } else {
            log.info("Draw rejected by {} in room {}", replierId, roomId);
            if (pendingDrawActionId != null) {
                gameActionDAO.updateActionStatusWithReason(pendingDrawActionId, "rejected", "opponent rejected");
            }
            drawRequesterId = null;
            pendingDrawActionId = null;
            return true;
        }
    }

    // ==================== 再来一局 ====================

    /**
     * 发起再来一局请求（游戏已结束状态）
     */
    public synchronized boolean requestRematch(long requesterId) {
        log.debug("requestRematch called for room {}: requesterId={}", roomId, requesterId);

        if (status != Status.FINISHED) {
            log.warn("requestRematch rejected: room {} status is {} (expected FINISHED)", roomId, status);
            return false;
        }

        if (rematchRequesterId != null) {
            log.warn("requestRematch rejected: rematch request already pending from {}", rematchRequesterId);
            return false;
        }

        if (!isValidPlayer(requesterId)) {
            log.warn("requestRematch rejected: {} is not a valid player", requesterId);
            return false;
        }

        rematchRequesterId = requesterId;
        log.info("Rematch requested by {} in room {}", requesterId, roomId);
        return true;
    }

    /**
     * 回复再来一局请求。如果双方同意，重置房间并重新开始对局。
     * 返回请求方 userId（accept 或 reject 时都需要，供 ClientHandler 定向通知），失败返回 -1。
     */
    public synchronized long replyRematch(long replierId, boolean accept) {
        log.debug("replyRematch called for room {}: replierId={}, accept={}", roomId, replierId, accept);

        if (rematchRequesterId == null) {
            log.warn("replyRematch rejected: no pending rematch request");
            return -1;
        }

        if (replierId == rematchRequesterId) {
            log.warn("replyRematch rejected: requester cannot reply to own request");
            return -1;
        }

        if (!isValidPlayer(replierId)) {
            log.warn("replyRematch rejected: {} is not a valid player", replierId);
            return -1;
        }

        long requesterId = rematchRequesterId;
        rematchRequesterId = null;

        if (accept) {
            log.info("Rematch accepted in room {}: both players agreed", roomId);
            resetAndRestartGame();
            return requesterId;
        } else {
            log.info("Rematch rejected by {} in room {}", replierId, roomId);
            return requesterId;
        }
    }

    /**
     * 重置房间状态并重新开始对局
     */
    private synchronized void resetAndRestartGame() {
        log.info("Resetting room {} for rematch: black={}, white={}", roomId, blackUserId, whiteUserId);

        // 重置游戏状态
        game.reset();
        gameStarted = true;
        gameStartTime = Instant.now();
        status = Status.PLAYING;

        // 清除所有请求状态
        undoRequesterId = null;
        drawRequesterId = null;
        rematchRequesterId = null;
        pendingUndoActionId = null;
        pendingDrawActionId = null;

        // 清除断线计数
        playerDisconnectCounts.clear();
        disconnectedPlayerIds.clear();
        cancelAllReconnectTimers();

        // 刷新双方积分（可能因上一局变化）
        if (!aiMode) {
            UserDAO.UserInfo blackInfo = userDAO.getUserById(blackUserId);
            UserDAO.UserInfo whiteInfo = userDAO.getUserById(whiteUserId);
            blackScore = blackInfo != null ? blackInfo.getScore() : 0;
            whiteScore = whiteInfo != null ? whiteInfo.getScore() : 0;
            blackAvatarUrl = resolvePlayerAvatar(blackUserId);
            whiteAvatarUrl = resolvePlayerAvatar(whiteUserId);
        } else {
            blackAvatarUrl = resolvePlayerAvatar(blackUserId);
        }

        // 广播新游戏开始（附带用户名和积分）
        broadcastAll(buildGameStartMessage());

        startTurnTimer();
        log.info("Room {} rematch started: black={}({}分), white={}({}分), gameType={}",
                roomId, blackUserId, blackScore, whiteUserId, whiteScore, gameType);
    }

    public synchronized Long getRematchRequesterId() { return rematchRequesterId; }

    /**
     * AI 模式下直接悔棋（不经过请求/确认流程）
     */
    public synchronized boolean directUndo(long userId) {
        log.debug("directUndo called for room {}: userId={}", roomId, userId);

        if (status != Status.PLAYING) {
            log.warn("directUndo rejected: room {} status is {}", roomId, status);
            return false;
        }

        if (game.getMoveCount() < 2) {
            log.warn("directUndo rejected: not enough moves (need at least 2)");
            return false;
        }

        if (!isValidPlayer(userId)) {
            log.warn("directUndo rejected: {} is not a valid player", userId);
            return false;
        }

        // 清空可能存在的待处理请求，避免冲突
        undoRequesterId = null;
        drawRequesterId = null;
        pendingUndoActionId = null;
        pendingDrawActionId = null;

        // AI 模式下需要撤销两步：AI的最后一步 + 玩家上一步
        int moveNumberBeforeUndo = game.getMoveCount();
        game.undo(); // 撤销 AI 的下子
        game.undo(); // 撤销玩家的下子
        log.info("Direct undo executed by {} in room {}: reverted {} moves", userId, roomId,
                moveNumberBeforeUndo - game.getMoveCount());
        try {
            long actionId = gameActionDAO.recordAction(
                    currentRecordId > 0 ? currentRecordId : null,
                    userId, "undo", moveNumberBeforeUndo);
            gameActionDAO.updateActionStatus(actionId, "accepted");
        } catch (Exception e) {
            log.error("Failed to record AI undo action in room {}: {}", roomId, e.getMessage(), e);
        }
        return true;
    }

    /**
     * AI 模式下直接判平局（不经过请求/确认流程）
     */
    public synchronized boolean directDraw(long userId) {
        log.debug("directDraw called for room {}: userId={}", roomId, userId);

        if (status != Status.PLAYING) {
            log.warn("directDraw rejected: room {} status is {}", roomId, status);
            return false;
        }

        if (!isValidPlayer(userId)) {
            log.warn("directDraw rejected: {} is not a valid player", userId);
            return false;
        }

        // 清空可能存在的待处理请求，避免冲突
        undoRequesterId = null;
        drawRequesterId = null;
        pendingUndoActionId = null;
        pendingDrawActionId = null;

        try {
            long actionId = gameActionDAO.recordAction(
                    currentRecordId > 0 ? currentRecordId : null,
                    userId, "draw", game.getMoveCount());
            gameActionDAO.updateActionStatus(actionId, "accepted");
        } catch (Exception e) {
            log.error("Failed to record AI draw action in room {}: {}", roomId, e.getMessage(), e);
        }

        log.info("Direct draw executed by {} in room {}", userId, roomId);
        endGame(0, "draw", "双方平局(求和)");
        return true;
    }

    private synchronized boolean isValidPlayer(long userId) {
        return userId == blackUserId || userId == whiteUserId;
    }

    public synchronized Long getUndoRequesterId() { return undoRequesterId; }
    public synchronized Long getDrawRequesterId() { return drawRequesterId; }

    public synchronized boolean reconnect(ClientHandler handler) {
        log.debug("Attempting reconnect to room {}: handler={}", roomId,
                handler != null ? handler.getUserId() : "null");

        if (handler == null) {
            log.warn("Reconnect rejected: handler is null");
            return false;
        }

        long userId = handler.getUserId();
        if (userId != blackUserId && userId != whiteUserId) {
            log.warn("Reconnect rejected: {} is not a player in room {}", userId, roomId);
            return false;
        }

        if (players.contains(handler)) {
            log.warn("Reconnect rejected: player {} is already in room {}", userId, roomId);
            return false;
        }

        players.add(handler);
        log.info("Player {} reconnected to room {} (disconnect #{}/{})", userId, roomId,
                playerDisconnectCounts.getOrDefault(userId, 0), MAX_DISCONNECT_COUNT);

        // 重连成功：清除断线状态，停止倒计时
        if (disconnectedPlayerIds.remove(userId)) {
            cancelReconnectTimer(userId);
            broadcastToPlayers(Message.of("PLAYER_RECONNECTED",
                    "userId", userId));
        }

        // 回放已有落子（通过 replayMovesTo 显式调用，便于调用方控制 LOGIN_OK/MOVE_SYNC 顺序）
        return true;
    }

    /**
     * 向指定 handler 发送该房间的所有历史落子（用于重连时回放）。
     * 注意：调用方应先发送 LOGIN_OK/RECONNECT_OK 再调本方法，保证客户端
     * 先建立 BoardController 再收到落子。
     */
    public synchronized void replayMovesTo(ClientHandler handler) {
        for (Game.MoveRecord mr : game.getMoveHistory()) {
            handler.send(Message.of(Protocol.MOVE_SYNC,
                    "x", mr.x,
                    "y", mr.y,
                    "color", mr.color));
        }
    }

    // ==================== 断线重连计时器 ====================

    /** 为指定玩家启动断线重连倒计时 */
    private synchronized void startReconnectTimer(long userId) {
        // 先取消该玩家已有的计时器
        cancelReconnectTimer(userId);
        reconnectTimerFutures.put(userId, turnTimerPool.schedule(() -> {
            synchronized (Room.this) {
                if (status != Status.PLAYING || !isPlayerDisconnected(userId)) return;
                // 断线超时（60秒内未重连），判负
                long winnerId = (userId == blackUserId) ? whiteUserId : blackUserId;
                String loserName = getPlayerUsername(userId);
                log.info("Player {} reconnect timed out in room {}, declaring {} as winner",
                        userId, roomId, winnerId);
                disconnectedPlayerIds.remove(userId);
                reconnectTimerFutures.remove(userId);
                endGame(winnerId, "disconnect_timeout",
                        loserName + " 断线超时未重连，自动判负");
                broadcastAll(Message.of("RECONNECT_TIMEOUT",
                        "userId", String.valueOf(userId),
                        "msg", "断线超时，对方获胜"));
            }
        }, RECONNECT_TIMEOUT_SEC, TimeUnit.SECONDS));
    }

    /** 取消指定玩家的断线倒计时 */
    private synchronized void cancelReconnectTimer(long userId) {
        ScheduledFuture<?> f = reconnectTimerFutures.remove(userId);
        if (f != null && !f.isDone()) {
            f.cancel(false);
            log.debug("Reconnect timer cancelled for player {} in room {}", userId, roomId);
        }
    }

    /** 取消所有断线倒计时 */
    private synchronized void cancelAllReconnectTimers() {
        for (ScheduledFuture<?> f : reconnectTimerFutures.values()) {
            if (f != null && !f.isDone()) f.cancel(false);
        }
        reconnectTimerFutures.clear();
        log.debug("All reconnect timers cancelled for room {}", roomId);
    }

    public synchronized boolean isGameOver() {
        boolean result = game.isFinished();
        log.debug("isGameOver called for room {}: result={}", roomId, result);
        return result;
    }

    public synchronized long getWinnerId() {
        int winnerColor = game.getWinner();
        long winnerId = 0;
        if (winnerColor == Game.BLACK) winnerId = blackUserId;
        else if (winnerColor == Game.WHITE) winnerId = whiteUserId;

        log.debug("getWinnerId called for room {}: winnerColor={}, winnerId={}",
                roomId, winnerColor == Game.BLACK ? "BLACK" : winnerColor == Game.WHITE ? "WHITE" : "NONE",
                winnerId);
        return winnerId;
    }

    public synchronized void broadcastToPlayers(Message msg) {
        log.debug("broadcastToPlayers room {}: message={}, playerCount={}",
                roomId, msg.getCmd(), players.size());

        int sentCount = 0;
        for (ClientHandler player : players) {
            if (player != null) {
                player.send(msg);
                sentCount++;
            }
        }
        log.debug("broadcastToPlayers completed: sent to {} of {} players", sentCount, players.size());
    }

    public synchronized void broadcastToObservers(Message msg) {
        log.debug("broadcastToObservers room {}: cmd={}, observerCount={}",
                roomId, msg.getCmd(), observers.size());

        int sentCount = 0;
        for (ClientHandler observer : observers) {
            if (observer != null) {
                observer.send(msg);
                sentCount++;
            }
        }
        log.debug("broadcastToObservers completed: sent to {} of {} observers", sentCount, observers.size());
    }

    public synchronized void broadcastAll(Message msg) {
        log.debug("broadcastAll room {}: cmd={}", roomId, msg.getCmd());
        broadcastToPlayers(msg);
        broadcastToObservers(msg);
        log.debug("broadcastAll completed for room {}", roomId);
    }

    /**
     * 向除指定玩家外的所有人广播（用于悔棋/求和请求等场景，避免请求者自己也收到弹窗）
     */
    public synchronized void broadcastToPlayersExcept(long excludeUserId, Message msg) {
        log.debug("broadcastToPlayersExcept room {}: exclude={}, cmd={}", roomId, excludeUserId, msg.getCmd());

        for (ClientHandler player : players) {
            if (player != null && player.getUserId() != excludeUserId) {
                player.send(msg);
            }
        }
        // 观战者也应该看到请求通知
        broadcastToObservers(msg);
    }

    public synchronized void sendToPlayer(ClientHandler handler, Message msg) {
        log.debug("sendToPlayer room {}: handler={}, cmd={}",
                roomId, handler != null ? handler.getUserId() : "null", msg.getCmd());

        if (handler == null) {
            log.warn("sendToPlayer rejected: handler is null");
            return;
        }

        if (!players.contains(handler)) {
            log.warn("sendToPlayer rejected: handler {} is not a player in room {}",
                    handler.getUserId(), roomId);
            return;
        }

        handler.send(msg);
        log.debug("sendToPlayer completed: sent to player {} in room {}", handler.getUserId(), roomId);
    }

    public String getInfo() {
        return String.format("Room[%s|%s|%s|%d/%d]",
                roomId, roomName, status, players.size(), 2);
    }

    /** 构建包含用户名、积分和头像的 GAME_START 消息 */
    public Message buildGameStartMessage() {
        return Message.of(Protocol.GAME_START,
                "blackUserId", blackUserId,
                "blackUsername", blackUsername != null ? blackUsername : String.valueOf(blackUserId),
                "blackScore", blackScore,
                "blackAvatarUrl", blackAvatarUrl != null ? blackAvatarUrl : "",
                "whiteUserId", whiteUserId,
                "whiteUsername", whiteUsername != null ? whiteUsername : String.valueOf(whiteUserId),
                "whiteScore", whiteScore,
                "whiteAvatarUrl", whiteAvatarUrl != null ? whiteAvatarUrl : "");
    }

    public static List<String[]> getRoomListInfo(Map<String, Room> rooms) {
        log.debug("getRoomListInfo called: room count={}", rooms.size());
        List<String[]> list = new ArrayList<>();
        for (Room room : rooms.values()) {
            // 不对外暴露已结束的房间
            if (room.getStatus() == Status.FINISHED) continue;
            String ownerName = "未知";
            if (room.aiMode) {
                ownerName = "AI";
            } else if (room.owner != null) {
                ownerName = room.owner.getUsername();
            }
            list.add(new String[]{
                    room.getRoomId(),
                    room.getRoomName(),
                    room.getStatus().toString(),
                    String.valueOf(room.getPlayerCount()),
                    "2",
                    room.hasPassword() ? "true" : "false",
                    ownerName
            });
        }
        log.debug("getRoomListInfo completed: {} rooms listed", list.size());
        return list;
    }

    /**
     * 查询玩家的头像 URL（OSS 预签名 30 天有效），失败则返回空字符串。
     */
    private String resolvePlayerAvatar(long userId) {
        try {
            String avatarPath = userDAO.getAvatar(userId);
            if (avatarPath == null || avatarPath.isEmpty()) {
                log.debug("resolvePlayerAvatar: userId={} has no avatar in DB", userId);
                return "";
            }
            String lower = avatarPath.toLowerCase();
            // 已带签名参数（如 ?Expires=...）的 URL 直接返回，不需要重新签名
            if (avatarPath.contains("?") && (lower.startsWith("http://") || lower.startsWith("https://"))) {
                log.info("resolvePlayerAvatar: userId={} already has presigned URL, length={}", userId, avatarPath.length());
                return avatarPath;
            }
            // 纯 OSS URL 无签名参数（历史数据 bug）→ 提取 object key 重新签名
            if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file://")) {
                try {
                    java.net.URI uri = java.net.URI.create(avatarPath);
                    String urlPath = uri.getPath(); // e.g. /avatar/74_xxx.png
                    if (urlPath != null && !urlPath.isEmpty()) {
                        String key = urlPath.startsWith("/") ? urlPath.substring(1) : urlPath;
                        if (!StorageManager.getInstance().isEnabled()) {
                            log.warn("resolvePlayerAvatar: userId={} OSS not enabled, skip presign for key={}", userId, key);
                            return avatarPath; // 降级返回原 URL
                        }
                        String presigned = StorageManager.getInstance().getOssService().generatePresignedUrl(key, 86400 * 30);
                        log.info("resolvePlayerAvatar: userId={} plain URL -> regenerated presigned, length={}", userId,
                                presigned != null ? presigned.length() : 0);
                        return presigned != null ? presigned : avatarPath;
                    }
                } catch (Exception e) {
                    log.warn("resolvePlayerAvatar: userId={} failed to parse URL: {}", userId, avatarPath, e);
                }
                return avatarPath; // 降级返回原 URL
            }
            // 防御：OSS 未启用时避免 NPE
            if (!StorageManager.getInstance().isEnabled()) {
                log.warn("resolvePlayerAvatar: userId={} OSS not enabled, skip presign for path={}", userId, avatarPath);
                return "";
            }
            String url = StorageManager.getInstance().getOssService().generatePresignedUrl(avatarPath, 86400 * 30);
            log.info("resolvePlayerAvatar: userId={} path={} -> url length={}", userId, avatarPath, url != null ? url.length() : 0);
            return url;
        } catch (Exception e) {
            log.debug("Failed to resolve avatar for userId={}: {}", userId, e.getMessage());
            return "";
        }
    }
}