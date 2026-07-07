package com.gomatch.server.room;

import com.gomatch.common.Protocol;
import com.gomatch.server.game.AIPlayer;
import com.gomatch.server.game.Game;
import com.gomatch.server.redis.RedisManager;
import com.gomatch.server.handler.ClientHandler;
import com.gomatch.server.dao.GameActionDAO;
import com.gomatch.server.dao.RecordDAO;
import com.gomatch.server.dao.RoomWatcherDAO;
import com.gomatch.server.dao.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private boolean gameStarted;
    private Instant gameStartTime;

    private Long undoRequesterId;
    private Long drawRequesterId;
    private long currentRecordId = -1;
    private Long pendingUndoActionId;
    private Long pendingDrawActionId;

    private boolean aiMode;
    private AIPlayer aiPlayer;
    private GameType gameType = GameType.NORMAL;

    private static final RecordDAO recordDAO = new RecordDAO();
    private static final UserDAO userDAO = new UserDAO();

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

    public int getPlayerCount() { return players.size(); }
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
            log.info("Player {} assigned as BLACK in room {}", handler.getUserId(), roomId);
        } else if (players.size() == 2) {
            whiteUserId = handler.getUserId();
            log.info("Player {} assigned as WHITE in room {}", handler.getUserId(), roomId);
        }

        return true;
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

        if (aiMode) {
            whiteUserId = -1;
            aiPlayer = new AIPlayer(Game.WHITE);
            log.info("AI game started in room {}: BLACK={}, AI=WHITE, startTime={}",
                    roomId, blackUserId, gameStartTime);
        } else {
            if (whiteUserId == 0) {
                log.error("startGame rejected: whiteUserId={} is not set", whiteUserId);
                return false;
            }
            log.info("Game started in room {}: BLACK={}, WHITE={}, startTime={}",
                    roomId, blackUserId, whiteUserId, gameStartTime);
        }

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
            broadcastAll(
                    Protocol.MOVE_SYNC,
                    String.valueOf(x),
                    String.valueOf(y),
                    String.valueOf(Game.WHITE)
            );

            if (game.isFinished()) {
                long winnerId = whiteUserId;
                endGame(winnerId, "five_in_a_row");
                broadcastAll(
                        Protocol.GAME_OVER,
                        String.valueOf(winnerId),
                        "AI 获胜!"
                );
                log.info("AI won in room {}: winner={}", roomId, winnerId);
            }
        }
    }

    public synchronized void endGame(long winnerId, String reason) {
        log.info("endGame called for room {}: winnerId={}, reason={}, currentStatus={}",
                roomId, winnerId, reason, status);

        status = Status.FINISHED;
        gameStarted = false;

        long durationSeconds = 0;
        if (gameStartTime != null) {
            durationSeconds = Duration.between(gameStartTime, Instant.now()).getSeconds();
            log.info("Game duration for room {}: {} seconds", roomId, durationSeconds);
        }

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
                    aiMode ? "AI" : "PVP");
            log.info("Game record saved successfully for room {}: recordId={}, {} moves recorded", roomId, currentRecordId, moves.size());
        } catch (Exception e) {
            log.error("Failed to save game record for room {}: {}", roomId, e.getMessage(), e);
        }

        // AI 模式下不更新积分和胜负场次
        if (!aiMode) {
            try {
                if (winnerId != 0) {
                    log.debug("Updating scores for win: winnerId={}, blackUserId={}, whiteUserId={}, gameType={}",
                            winnerId, blackUserId, whiteUserId, gameType);

                    long loserId = (winnerId == blackUserId) ? whiteUserId : blackUserId;

                    if (gameType == GameType.RANKED) {
                        updateRankedScore(winnerId, loserId, true);
                    } else {
                        boolean winnerScoreUpdated = userDAO.updateScore(winnerId, 10);
                        userDAO.updateWinCount(winnerId);
                        boolean loserScoreUpdated = userDAO.updateScore(loserId, -5);
                        userDAO.updateLoseCount(loserId);

                        if (winnerScoreUpdated && loserScoreUpdated) {
                            log.info("Score updated successfully: winner={}(+10), loser={}(-5)", winnerId, loserId);
                        } else {
                            log.warn("Score update partially failed: winner={}(success={}), loser={}(success={})",
                                    winnerId, winnerScoreUpdated, loserId, loserScoreUpdated);
                        }
                    }
                } else {
                    log.debug("Updating scores for draw: blackUserId={}, whiteUserId={}, gameType={}",
                            blackUserId, whiteUserId, gameType);

                    userDAO.updateDrawCount(blackUserId);
                    userDAO.updateDrawCount(whiteUserId);

                    if (gameType == GameType.RANKED) {
                        updateRankedScore(blackUserId, whiteUserId, false);
                    }

                    log.info("Draw count updated successfully for both players: black={}, white={}",
                            blackUserId, whiteUserId);
                }
            } catch (Exception e) {
                log.error("Failed to update user stats for room {}: {}", roomId, e.getMessage(), e);
            }
        } else {
            log.info("AI mode: skipping score/win-loss update for room {}", roomId);
        }

        try {
            RedisManager.getInstance().deleteGameState(roomId);
            log.debug("Redis game state deleted for room {}", roomId);
        } catch (Exception e) {
            log.error("Failed to delete Redis game state for room {}: {}", roomId, e.getMessage(), e);
        }

        log.info("Game ended in room {}: winner={}, reason={}, duration={}s",
                roomId, winnerId, reason, durationSeconds);
    }

    private void updateRankedScore(long winnerId, long loserId, boolean hasWinner) {
        UserDAO.UserInfo winnerInfo = userDAO.getUserById(winnerId);
        UserDAO.UserInfo loserInfo = userDAO.getUserById(loserId);

        if (winnerInfo == null || loserInfo == null) {
            log.warn("updateRankedScore failed: user info not found, winnerId={}, loserId={}", winnerId, loserId);
            return;
        }

        int winnerScore = winnerInfo.getScore();
        int loserScore = loserInfo.getScore();
        int K = 20;

        double expectedWinner = 1.0 / (1.0 + Math.pow(10, (loserScore - winnerScore) / 400.0));
        double expectedLoser = 1.0 / (1.0 + Math.pow(10, (winnerScore - loserScore) / 400.0));

        if (hasWinner) {
            int winnerDelta = (int) Math.round(K * (1 - expectedWinner));
            int loserDelta = (int) Math.round(K * (0 - expectedLoser));

            userDAO.updateScore(winnerId, winnerDelta);
            userDAO.updateWinCount(winnerId);
            userDAO.updateScore(loserId, loserDelta);
            userDAO.updateLoseCount(loserId);

            log.info("Ranked score updated: winner={}(+{}), loser={}({}), K={}",
                    winnerId, winnerDelta, loserId, loserDelta, K);
        } else {
            int winnerDelta = (int) Math.round(K * (0.5 - expectedWinner));
            int loserDelta = (int) Math.round(K * (0.5 - expectedLoser));

            userDAO.updateScore(winnerId, winnerDelta);
            userDAO.updateScore(loserId, loserDelta);

            log.info("Ranked draw score updated: player1={}(+{}), player2={}(+{})",
                    winnerId, winnerDelta, loserId, loserDelta);
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
            boolean success = game.undo();
            if (success) {
                log.info("Undo accepted by {} in room {}", replierId, roomId);
                if (pendingUndoActionId != null) {
                    gameActionDAO.updateActionStatus(pendingUndoActionId, "accepted");
                }
            }
            undoRequesterId = null;
            pendingUndoActionId = null;
            return success;
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
            endGame(0, "draw");
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
        endGame(0, "draw");
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
        log.info("Player {} reconnected to room {}", userId, roomId);
        return true;
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

    public synchronized void broadcastToPlayers(String... msg) {
        String message = com.gomatch.common.Protocol.build(msg);
        log.debug("broadcastToPlayers room {}: message={}, playerCount={}",
                roomId, message, players.size());

        int sentCount = 0;
        for (ClientHandler player : players) {
            if (player != null) {
                player.send(message);
                sentCount++;
            }
        }
        log.debug("broadcastToPlayers completed: sent to {} of {} players", sentCount, players.size());
    }

    public synchronized void broadcastToObservers(String... msg) {
        String message = com.gomatch.common.Protocol.build(msg);
        log.debug("broadcastToObservers room {}: message={}, observerCount={}",
                roomId, message, observers.size());

        int sentCount = 0;
        for (ClientHandler observer : observers) {
            if (observer != null) {
                observer.send(message);
                sentCount++;
            }
        }
        log.debug("broadcastToObservers completed: sent to {} of {} observers", sentCount, observers.size());
    }

    public synchronized void broadcastAll(String... msg) {
        log.debug("broadcastAll room {}: message parts={}", roomId, msg.length);
        broadcastToPlayers(msg);
        broadcastToObservers(msg);
        log.debug("broadcastAll completed for room {}", roomId);
    }

    public synchronized void sendToPlayer(ClientHandler handler, String... msg) {
        log.debug("sendToPlayer room {}: handler={}, message parts={}",
                roomId, handler != null ? handler.getUserId() : "null", msg.length);

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

    public static List<String[]> getRoomListInfo(Map<String, Room> rooms) {
        log.debug("getRoomListInfo called: room count={}", rooms.size());
        List<String[]> list = new ArrayList<>();
        for (Room room : rooms.values()) {
            // 不对外暴露已结束的房间
            if (room.getStatus() == Status.FINISHED) continue;
            list.add(new String[]{
                room.getRoomId(),
                room.getRoomName(),
                room.getStatus().toString(),
                String.valueOf(room.getPlayerCount()),
                "2",
                room.hasPassword() ? "1" : "0"
            });
        }
        log.debug("getRoomListInfo completed: {} rooms listed", list.size());
        return list;
    }

    private void saveStateToRedis() {
        log.debug("saveStateToRedis called for room {}", roomId);
        try {
            StringBuilder json = new StringBuilder("{");
            json.append('"').append("board").append('"').append(":");
            json.append("[");
            int[][] board = game.getBoard();
            for (int i = 0; i < Game.SIZE; i++) {
                json.append("[");
                for (int j = 0; j < Game.SIZE; j++) {
                    if (j > 0) json.append(",");
                    json.append(board[i][j]);
                }
                json.append("]");
            }
            json.append("],");
            json.append('"').append("currentPlayer").append('"').append(":");
            json.append(game.getCurrentPlayer());
            json.append(",");
            json.append('"').append("moveCount").append('"').append(":");
            json.append(game.getMoveCount());
            json.append("}");

            RedisManager.getInstance().saveGameState(roomId, json.toString());
            log.debug("saveStateToRedis completed for room {}", roomId);
        } catch (Exception e) {
            log.error("Failed to save game state to Redis for room {}: {}",
                    roomId, e.getMessage(), e);
        }
    }
}