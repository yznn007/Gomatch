package com.gomatch.server.handler;

import com.gomatch.common.Protocol;
import com.gomatch.server.dao.ChatDAO;
import com.gomatch.server.dao.FriendDAO;
import com.gomatch.server.dao.GameSaveDAO;
import com.gomatch.server.dao.RankDAO;
import com.gomatch.server.dao.RoomWatcherDAO;
import com.gomatch.server.dao.RecordDAO;
import com.gomatch.server.dao.UserDAO;
import com.gomatch.server.room.Room;
import com.gomatch.server.room.RoomManager;
import com.gomatch.server.session.SessionManager;
import com.gomatch.server.redis.RedisManager;
import com.gomatch.server.game.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

/**
 * 客户端处理器
 * 每个客户端连接分配一个独立的 ClientHandler 线程
 * 负责：协议解析、命令分发、消息发送、资源清理
 */
public class ClientHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private static final UserDAO userDAO = new UserDAO();
    private static final RecordDAO recordDAO = new RecordDAO();
    private static final ChatDAO chatDAO = new ChatDAO();
    private static final FriendDAO friendDAO = new FriendDAO();
    private static final RoomWatcherDAO roomWatcherDAO = new RoomWatcherDAO();
    private static final GameSaveDAO gameSaveDAO = new GameSaveDAO();
    private static final RankDAO rankDAO = new RankDAO();
    private static final RoomManager roomManager = RoomManager.getInstance();
    private static final SessionManager sessionManager = SessionManager.getInstance();

    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    
    // Q2-A: 新增会话字段
    private long userId;
    private String username;
    private boolean authenticated;
    
    // 当前所在房间
    Room currentRoom;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            log.info("Client connected: {} from {}",
                    socket.getLocalPort(), socket.getInetAddress());

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] parts = Protocol.parse(line);
                log.debug("Received from {}: {}", socket.getPort(), line);
                dispatch(parts);
            }
        } catch (IOException e) {
            log.warn("Client read error: {}", socket.getInetAddress());
        } finally {
            cleanup();
        }
    }

    private void dispatch(String[] parts) {
        if (parts.length == 0) return;

        String cmd = parts[0];
        switch (cmd) {
            case Protocol.LOGIN        -> handleLogin(parts);
            case Protocol.REGISTER     -> handleRegister(parts);
            case Protocol.CREATE_ROOM   -> handleCreateRoom(parts);
            case "CREATE_AI_ROOM"     -> handleCreateAiRoom(parts);
            case Protocol.JOIN_ROOM   -> handleJoinRoom(parts);
            case Protocol.LEAVE_ROOM   -> handleLeaveRoom();
            case Protocol.MOVE         -> handleMove(parts);
            case Protocol.CHAT         -> handleChat(parts);
            case Protocol.SURRENDER    -> handleSurrender();
            case Protocol.SPECTATE     -> handleSpectate(parts);
            case Protocol.ROOM_LIST    -> handleRoomList();
            case Protocol.QUICK_MATCH  -> handleQuickMatch();
            case "RANKED_MATCH"      -> handleRankedMatch();
            case Protocol.RANKING      -> handleRanking();
            case Protocol.QUERY_RECORD     -> handleQueryRecord(parts);
            case Protocol.REQUEST_UNDO -> handleRequestUndo();
            case Protocol.REPLY_UNDO   -> handleReplyUndo(parts);
            case Protocol.REQUEST_DRAW -> handleRequestDraw();
            case Protocol.REPLY_DRAW   -> handleReplyDraw(parts);
            case "RECONNECT"         -> handleReconnect(parts);
            case Protocol.CHANGE_PWD -> handleChangePassword(parts);
            case Protocol.ADD_FRIEND -> handleAddFriend(parts);
            case Protocol.ACCEPT_FRIEND -> handleAcceptFriend(parts);
            case Protocol.REJECT_FRIEND -> handleRejectFriend(parts);
            case Protocol.DELETE_FRIEND -> handleDeleteFriend(parts);
            case Protocol.FRIEND_LIST -> handleFriendList();
            case Protocol.FRIEND_REQUESTS -> handleFriendRequests();
            case Protocol.SAVE_GAME -> handleSaveGame(parts);
            case Protocol.LOAD_GAME -> handleLoadGame(parts);
            case Protocol.DELETE_SAVE -> handleDeleteSave(parts);
            case Protocol.SAVE_LIST -> handleSaveList(parts);
            case Protocol.CHAT_HISTORY -> handleChatHistory(parts);
            case Protocol.WATCHER_HISTORY -> handleWatcherHistory(parts);
            case Protocol.LOGOUT -> handleLogout();
            case Protocol.HEARTBEAT -> handleHeartbeat();
            case "AI_MATCH" -> handleAiMatch(parts);

            default -> send(Protocol.ERROR, "Unknown command: " + cmd);
        }
    }

    public void send(String... parts) {
        if (writer != null) {
            writer.println(Protocol.build(parts));
            log.debug("Sent to {}: {}", socket.getPort(), Protocol.build(parts));
        }
    }

    public void send(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    // ==================== 用户注册 ====================

    private void handleRegister(String[] parts) {
        if (parts.length < 3) {
            send("REGISTER_FAIL", "缺少参数");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        log.info("注册请求: username={}", username);

        if (username.trim().isEmpty()) {
            send("REGISTER_FAIL", "用户名不能为空");
            return;
        }
        if (username.length() > 16) {
            send("REGISTER_FAIL", "用户名最多16个字符");
            return;
        }
        if (password.length() < 6) {
            send("REGISTER_FAIL", "密码至少6位");  
            return;
        }

        boolean success = userDAO.register(username, password);
        if (success) {
            send("REGISTER_OK");
            log.info("用户注册成功: {}", username);
        } else {
            send("REGISTER_FAIL", "用户名已存在");
            log.warn("Register failed (duplicate): {}", username);
        }
    }

    // ==================== 修改密码 ====================

    private void handleChangePassword(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (parts.length < 3) {
            send(Protocol.ERROR, "缺少参数");
            return;
        }

        String oldPassword = parts[1];
        String newPassword = parts[2];

        if (newPassword.length() < 6) {
            send(Protocol.ERROR, "新密码至少6位");
            return;
        }

        boolean success = userDAO.changePassword(userId, oldPassword, newPassword);
        if (success) {
            send(Protocol.PWD_CHANGED);
            log.info("用户 {} 修改密码成功", userId);
        } else {
            send(Protocol.ERROR, "旧密码错误");
            log.warn("用户 {} 修改密码失败: 旧密码错误", userId);
        }
    }

    // ==================== 心跳 ====================

    private void handleHeartbeat() {
        send(Protocol.HEARTBEAT_OK);
    }

    // ==================== 用户登录 ====================

    private void handleLogin(String[] parts) {
        if (parts.length < 3) {
            send(Protocol.LOGIN_FAIL, "缺少参数");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        log.info("登录请求: username={}", username);

        UserDAO.UserInfo userInfo = userDAO.login(username, password);
        if (userInfo != null) {
            SessionManager.Session existingSession = sessionManager.getSessionByUserId(userInfo.getUserId());
            if (existingSession != null && existingSession.handler != null) {
                send(Protocol.LOGIN_FAIL, "该账号已在其他设备登录");
                log.warn("登录失败: 用户 {} 已在线", username);
                return;
            }

            this.userId = userInfo.getUserId();
            this.username = userInfo.getUsername();
            this.authenticated = true;

            SessionManager.Session session = sessionManager.createSession(
                    userInfo.getUserId(), userInfo.getUsername(),
                    userInfo.getScore(), userInfo.getWinCount(),
                    userInfo.getLoseCount(), userInfo.getDrawCount());
            sessionManager.updateSessionHandler(userInfo.getUserId(), this);

            send(Protocol.LOGIN_OK,
                    String.valueOf(userInfo.getUserId()),
                    userInfo.getUsername(),
                    String.valueOf(userInfo.getScore()),
                    String.valueOf(userInfo.getWinCount()),
                    String.valueOf(userInfo.getLoseCount()),
                    String.valueOf(userInfo.getDrawCount()),
                    session.token);
            
            log.info("User logged in: {} (id={}, score={}, token={})", 
                    userInfo.getUsername(), userInfo.getUserId(), userInfo.getScore(),
                    session.token.substring(0, 8));
        } else {
            send(Protocol.LOGIN_FAIL, "用户名或密码错误");
            log.warn("登录失败: {}", username);
        }
    }

    // ==================== 房间管理 ====================

    /**
     * 创建房间
     * 协议: CREATE_ROOM|roomName
     * 响应: JOIN_OK|roomId|roomName|1/2
     */
    private void handleCreateRoom(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (parts.length < 2) {
            send(Protocol.ERROR, "缺少房间名称");
            return;
        }

        String roomName = parts[1];
        if (roomName.trim().isEmpty()) {
            send(Protocol.ERROR, "房间名不能为空");
            return;
        }

        String password = null;
        if (parts.length >= 3 && !parts[2].trim().isEmpty()) {
            password = parts[2];
        }

        // 如果已在房间，先离开
        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }

        Room room = roomManager.createRoom(roomName, this, password);
        this.currentRoom = room;

        // 响应: JOIN_OK|roomId|roomName|playerCount/max|hasPassword
        send("JOIN_OK",
                room.getRoomId(),
                room.getRoomName(),
                room.getPlayerCount() + "/" + 2,
                room.hasPassword() ? "1" : "0");

        log.info("Player {} created room: {} ({}) with password: {}", userId, room.getRoomId(), roomName, 
                password != null && !password.isEmpty());
    }

    /**
     * 创建人机对战房间
     * 协议: CREATE_AI_ROOM|roomName
     * 响应: JOIN_OK|roomId|roomName|1/1|AI
     */
    private void handleCreateAiRoom(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (parts.length < 2) {
            send(Protocol.ERROR, "缺少房间名称");
            return;
        }

        String roomName = parts[1];
        if (roomName.trim().isEmpty()) {
            send(Protocol.ERROR, "房间名不能为空");
            return;
        }

        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }

        Room room = roomManager.createAiRoom(roomName, this);
        this.currentRoom = room;

        send("JOIN_OK",
                room.getRoomId(),
                room.getRoomName(),
                room.getPlayerCount() + "/" + 1,
                "AI");

        room.startGame();
        room.broadcastAll(
                Protocol.GAME_START,
                String.valueOf(room.getBlackUserId()),
                String.valueOf(room.getWhiteUserId())
        );

        log.info("Player {} created AI room: {} ({}), game started", userId, room.getRoomId(), roomName);
    }

    /**
     * AI 对战
     * 协议: AI_MATCH|difficulty
     * 响应: JOIN_OK|roomId|roomName|playerCount/max
     */
    private void handleAiMatch(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        String difficulty = "EASY";
        if (parts.length >= 2) {
            difficulty = parts[1];
        }

        log.info("AI对战请求: userId={}, difficulty={}", userId, difficulty);

        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }

        String roomName = "AI对战 - " + difficulty;
        Room room = roomManager.createAiRoom(roomName, this, difficulty);
        this.currentRoom = room;

        send(Protocol.JOIN_OK,
                room.getRoomId(),
                roomName,
                "1/2",
                "0"
        );

        room.startGame();
        room.broadcastAll(
                Protocol.GAME_START,
                String.valueOf(userId),
                "AI"
        );

        log.info("AI对战房间创建成功: userId={}, roomId={}, difficulty={}", userId, room.getRoomId(), difficulty);
    }

    /**
     * 加入房间
     * 协议: JOIN_ROOM|roomId
     * 响应: JOIN_OK|roomId|roomName|playerCount/max
     *       JOIN_FAIL|reason
     */
    private void handleJoinRoom(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (parts.length < 2) {
            send(Protocol.JOIN_FAIL, "缺少房间ID");
            return;
        }

        String roomId = parts[1];
        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            send(Protocol.JOIN_FAIL, "房间不存在");
            return;
        }

        String password = null;
        if (parts.length >= 3) {
            password = parts[2];
        }

        // 如果已在其他房间，先离开
        if (currentRoom != null && currentRoom != room) {
            roomManager.leaveRoom(this);
        }

        if (room.hasPassword() && !room.checkPassword(password)) {
            send(Protocol.JOIN_FAIL, "密码错误");
            return;
        }

        boolean success = roomManager.joinExistingRoom(this, roomId, password);
        if (success) {
            this.currentRoom = room;

            // 广播: 新玩家进入
            room.broadcastToPlayers(
                Protocol.PLAYER_ENTER,
                String.valueOf(getUserId()),
                getUsername()
            );

            // 响应
            send("JOIN_OK",
                    room.getRoomId(),
                    room.getRoomName(),
                    room.getPlayerCount() + "/" + 2);

            // 房间满2人时自动开始游戏
            if (room.getPlayerCount() == 2) {
                room.startGame();
                room.broadcastAll(
                    Protocol.GAME_START,
                    String.valueOf(room.getBlackUserId()),
                    String.valueOf(room.getWhiteUserId())
                );
                log.info("房间 {} 已满，游戏开始: BLACK={}, WHITE={}",
                        roomId, room.getBlackUserId(), room.getWhiteUserId());
            }

            log.info("玩家 {} 加入房间 {}", userId, roomId);
        } else {
            send(Protocol.JOIN_FAIL, "房间已满或游戏进行中");
        }
    }

    /**
     * 离开房间
     * 协议: LEAVE_ROOM
     * 响应: 无（服务端主动广播 PLAYER_LEAVE）
     */
    private void handleLeaveRoom() {
        if (currentRoom == null) {
            send(Protocol.ERROR, "不在房间");
            return;
        }

        Room room = currentRoom;
        String roomId = room.getRoomId();

        room.broadcastToPlayers(
            Protocol.PLAYER_LEAVE,
            String.valueOf(userId),
            getUsername()
        );

        roomManager.leaveRoom(this);
        log.info("玩家 {} 离开房间 {}", userId, roomId);
    }

    /**
     * 获取房间列表
     * 协议: ROOM_LIST
     * 响应: RANKING_LIST|roomId|roomName|status|players|max|...
     */
    private void handleRoomList() {
        List<String[]> roomList = Room.getRoomListInfo(roomManager.getAllRooms());
        
        StringBuilder sb = new StringBuilder();
        sb.append(Protocol.RANKING_LIST); // 复用 RANKING_LIST 作为列表响应头
        for (String[] info : roomList) {
            sb.append("|").append(info[0])   // roomId
              .append("|").append(info[1])   // roomName
              .append("|").append(info[2])   // status
              .append("|").append(info[3])   // players
              .append("|").append(info[4])   // max
              .append("|").append(info[5]);  // hasPassword
        }
        send(sb.toString());
        log.info("Sent room list to {}: {} rooms", socket.getPort(), roomList.size());
    }

    /**
     * 快速匹配
     * 协议: QUICK_MATCH
     * 响应: JOIN_OK|roomId|roomName|2/2 (配对成功)
     *       ERROR|reason (队列中)
     */
    private void handleQuickMatch() {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        // 如果已在房间，先离开
        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }

        Room room = roomManager.quickMatch(this);
        if (room != null) {
            this.currentRoom = room;

            // 通知双方游戏开始
            room.broadcastAll(
                Protocol.GAME_START,
                String.valueOf(room.getBlackUserId()),
                String.valueOf(room.getWhiteUserId())
            );

            send(Protocol.JOIN_OK,
                    room.getRoomId(),
                    room.getRoomName(),
                    room.getPlayerCount() + "/" + 2);

            log.info("玩家 {} 配对成功，房间: {}", userId, room.getRoomId());
        } else {
            send(Protocol.ERROR, "等待对手... (" + roomManager.getMatchQueueSize() + "/2)");
        }
    }

    /**
     * 排位匹配
     * 协议: RANKED_MATCH
     * 响应: JOIN_OK|roomId|roomName|2/2|RANKED (配对成功)
     *       ERROR|reason (队列中)
     */
    private void handleRankedMatch() {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }

        UserDAO.UserInfo userInfo = userDAO.getUserById(userId);
        if (userInfo == null) {
            send(Protocol.ERROR, "用户信息不存在");
            return;
        }

        Room room = roomManager.rankedMatch(this, userInfo.getScore());
        if (room != null) {
            this.currentRoom = room;

            room.broadcastAll(
                Protocol.GAME_START,
                String.valueOf(room.getBlackUserId()),
                String.valueOf(room.getWhiteUserId())
            );

            send(Protocol.JOIN_OK,
                    room.getRoomId(),
                    room.getRoomName(),
                    room.getPlayerCount() + "/" + 2,
                    "RANKED");

            log.info("玩家 {} 排位匹配成功，房间: {}", userId, room.getRoomId());
        } else {
            send("RANKED_MATCH_WAIT", "等待排位对手... (" + roomManager.getRankedMatchQueueSize() + "/2)");
        }
    }

    // ==================== 游戏操作 ====================

    private void handleMove(String[] parts) {
        log.info("handleMove called: parts={}, currentRoom={}, status={}, aiMode={}",
                java.util.Arrays.toString(parts), currentRoom, 
                currentRoom != null ? currentRoom.getStatus() : "null",
                currentRoom != null ? currentRoom.isAiMode() : "null");

        if (currentRoom == null || currentRoom.getStatus() != Room.Status.PLAYING) {
            send(Protocol.ERROR, "不在游戏中");
            return;
        }

        if (parts.length < 3) {
            send(Protocol.ERROR, "缺少 x,y 坐标");
            return;
        }

        int x, y;
        try {
            x = Integer.parseInt(parts[1]);
            y = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            send(Protocol.ERROR, "坐标无效，请输入整数坐标");   
            return;
        }

        int color = currentRoom.getGame().getCurrentPlayer();
        log.info("Player move: x={}, y={}, color={}, aiMode={}", x, y, 
                color == Game.BLACK ? "BLACK" : "WHITE", currentRoom.isAiMode());
        
        boolean success = currentRoom.makeMove(x, y, this);
        if (!success) {
            send(Protocol.ERROR, "无效的移动子操作");       
            return;
        }

        currentRoom.broadcastAll(
            Protocol.MOVE_SYNC,
            String.valueOf(x),
            String.valueOf(y),
            String.valueOf(color)
        );

        if (currentRoom.isGameOver()) {
            long winnerId = currentRoom.getWinnerId();
            boolean isDraw = winnerId == 0;
            String reason = isDraw ? "board_full" : "five_in_a_row";
            String msg = isDraw ? "棋盘已满，平局" : "五子连珠!";
            currentRoom.endGame(winnerId, reason);

            currentRoom.broadcastAll(
                Protocol.GAME_OVER,
                String.valueOf(winnerId),
                msg
            );

            log.info("Game over in room {}: winner={}, reason={}", currentRoom.getRoomId(), winnerId, reason);
        } else if (currentRoom.isAiMode()) {
            log.info("Calling makeAiMove() for room {}", currentRoom.getRoomId());
            currentRoom.makeAiMove();
            log.info("makeAiMove() completed for room {}", currentRoom.getRoomId());
        }
    }

    private void handleChat(String[] parts) {
        if (currentRoom == null) {
            send(Protocol.ERROR, "不在房间中");
            return;
        }

        if (parts.length < 2) {
            send(Protocol.ERROR, "缺少聊天消息");           
            return;
        }

        String message = parts[1];
        currentRoom.broadcastAll(
            Protocol.CHAT_MSG,
            String.valueOf(userId),
            getUsername(),
            message
        );
        // 同时发布到 Redis Pub/Sub
        RedisManager.getInstance().publish(currentRoom.getRoomId(),
            Protocol.build(Protocol.CHAT_MSG, String.valueOf(userId), getUsername(), message));

        // 保存聊天消息到数据库
        chatDAO.saveMessage(currentRoom.getRoomId(), userId, message, "text");

        log.info("玩家 {} 在房间 {} 发送聊天消息: [{}]", getUsername(), currentRoom.getRoomId(), message);
    }

    private void handleSurrender() {
        if (currentRoom == null || currentRoom.getStatus() != Room.Status.PLAYING) {
            send(Protocol.ERROR, "不在游戏中");
            return;
        }

        if (currentRoom.isObserver(this)) {
            send(Protocol.ERROR, "观战者不能认输");
            return;
        }

        long winnerId = (userId == currentRoom.getBlackUserId()) 
                ? currentRoom.getWhiteUserId() 
                : currentRoom.getBlackUserId();

        currentRoom.endGame(winnerId, "surrender");

        // 广播游戏结束
        currentRoom.broadcastAll(
            Protocol.GAME_OVER,
            String.valueOf(winnerId),
            getUsername() + " 认输"
        );
        // 同时发布到 Redis Pub/Sub
        RedisManager.getInstance().publish(currentRoom.getRoomId(),
            Protocol.build(Protocol.GAME_OVER, String.valueOf(winnerId), getUsername() + " 认输"));

        log.info("玩家 {} 在房间 {} 认输，获胜方: {}", 
                userId, currentRoom.getRoomId(), winnerId);
    }

    /**
     * 观战加入
     * 协议: SPECTATE|roomId
     * 响应: GAME_START|blackUserId|whiteUserId + MOVE_SYNC 回放已有落子
     */
    private void handleSpectate(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (parts.length < 2) {
            send("JOIN_FAIL", "缺少房间ID");
            return;
        }

        String roomId = parts[1];
        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            send("JOIN_FAIL", "房间不存在");
            return;
        }

        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }

        room.spectate(this);
        this.currentRoom = room;

        room.broadcastToPlayers(
            Protocol.PLAYER_ENTER,
            String.valueOf(userId),
            getUsername(),
            "SPECTATOR"
        );

        // 发送 GAME_START 让客户端切换到棋盘界面
        send(Protocol.GAME_START,
            String.valueOf(room.getBlackUserId()),
            String.valueOf(room.getWhiteUserId()));

        // 回放所有已有落子，让观战者看到当前棋盘
        for (Game.MoveRecord mr : room.getGame().getMoveHistory()) {
            send(Protocol.MOVE_SYNC,
                String.valueOf(mr.x),
                String.valueOf(mr.y),
                String.valueOf(mr.color));
        }

        log.info("玩家 {} 开始观战房间 {} (已回放 {} 步)", 
                userId, roomId, room.getGame().getMoveHistory().size());
    }

    private void handleRanking() {
        var ranking = rankDAO.getRanking(20);
        if (ranking.isEmpty()) {
            send(Protocol.ERROR, "暂无用户");   
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(Protocol.RANKING_LIST);
        for (int i = 0; i < ranking.size(); i++) {
            var u = ranking.get(i);
            sb.append("|").append(u.getRank())
              .append("|").append(u.getUsername())
              .append("|").append(u.getScore())
              .append("|").append(u.getWinRate());
        }
        send(sb.toString());
        log.info("Sent ranking to {}: {} users", socket.getPort(), ranking.size());
    }

    /**
     * 查询战绩
     * 协议: QUERY_RECORD|page|size
     * 响应: RECORD_LIST|recordId|player1Id|player1Name|player2Id|player2Name|winnerId|result|moveCount|duration|createTime|...
     */
    private void handleQueryRecord(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        int page = 0;
        int size = 10;
        try {
            if (parts.length >= 2) page = Integer.parseInt(parts[1]);
            if (parts.length >= 3) size = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            send(Protocol.ERROR, "参数无效");
            return;
        }

        List<String[]> records = recordDAO.getHistory(userId, page, size);
        
        StringBuilder sb = new StringBuilder();
        sb.append("RECORD_LIST");
        for (String[] record : records) {
            long player1Id = Long.parseLong(record[1]);
            long player2Id = Long.parseLong(record[2]);
            long winnerId = record[3].equals("0") ? 0 : Long.parseLong(record[3]);
            
            UserDAO.UserInfo player1 = userDAO.getUserById(player1Id);
            UserDAO.UserInfo player2 = userDAO.getUserById(player2Id);
            
            String player1Name = player1 != null ? player1.getUsername() : "未知";
            String player2Name = player2 != null ? player2.getUsername() : (player2Id == -1 ? "AI" : "未知");
            
            String boardData = record.length > 8 ? record[8] : "";
            boardData = boardData.replace("|", "\\|");
            
            sb.append("|").append(record[0])      // recordId
              .append("|").append(player1Id)     // player1Id
              .append("|").append(player1Name)   // player1Name
              .append("|").append(player2Id)     // player2Id
              .append("|").append(player2Name)   // player2Name
              .append("|").append(winnerId)      // winnerId
              .append("|").append(record[4])     // result
              .append("|").append(record[5])     // moveCount
              .append("|").append(record[6])     // duration
              .append("|").append(record[7])     // createTime
              .append("|").append(boardData);    // boardData
        }
        send(sb.toString());
        log.info("Sent record list to user {} ({} records)", userId, records.size());
    }

    /**
     * 请求悔棋
     * 协议: REQUEST_UNDO
     * 响应: UNDO_REQUESTED|requesterId|requesterName
     */
    private void handleRequestUndo() {
        if (currentRoom == null || currentRoom.getStatus() != Room.Status.PLAYING) {
            send(Protocol.ERROR, "不在游戏中");
            return;
        }

        if (currentRoom.isObserver(this)) {
            send(Protocol.ERROR, "观战者不能请求悔棋");
            return;
        }

        // AI 模式下直接悔棋，不发送请求
        if (currentRoom.isAiMode()) {
            boolean undone = currentRoom.directUndo(userId);
            if (undone) {
                currentRoom.broadcastAll("UNDO_ACCEPTED");
                log.info("Player {} undid move in AI mode", userId);
            } else {
                send(Protocol.ERROR, "无法悔棋");
            }
            return;
        }

        boolean success = currentRoom.requestUndo(userId);
        if (success) {
            // 只通知对手，不通知请求者本人
            currentRoom.broadcastToPlayersExcept(userId,
                "UNDO_REQUESTED",
                String.valueOf(userId),
                getUsername()
            );
            log.info("玩家 {} 请求悔棋", userId);
        } else {
            send(Protocol.ERROR, "无法请求悔棋");
        }
    }

    /**
     * 回复悔棋请求
     * 协议: REPLY_UNDO|accept(1/0)
     * 响应: UNDO_ACCEPTED|x|y (撤销成功)
     *       UNDO_REJECTED (拒绝)
     */
    private void handleReplyUndo(String[] parts) {
        if (currentRoom == null || currentRoom.getStatus() != Room.Status.PLAYING) {
            send(Protocol.ERROR, "不在游戏中");
            return;
        }

        if (currentRoom.isObserver(this)) {
            send(Protocol.ERROR, "观战者不能回复悔棋");
            return;
        }

        if (parts.length < 2) {
            send(Protocol.ERROR, "缺少参数");
            return;
        }

        boolean accept = "1".equals(parts[1]);
        boolean success = currentRoom.replyUndo(userId, accept);
        if (success) {
            if (accept) {
                currentRoom.broadcastAll("UNDO_ACCEPTED");
                log.info("玩家 {} 同意悔棋", userId);
            } else {
                currentRoom.broadcastAll("UNDO_REJECTED");
                log.info("玩家 {} 拒绝悔棋", userId);
            }
        } else {
            send(Protocol.ERROR, "无法回复悔棋请求");
        }
    }

    /**
     * 请求求和
     * 协议: REQUEST_DRAW
     * 响应: DRAW_REQUESTED|requesterId|requesterName
     */
    private void handleRequestDraw() {
        if (currentRoom == null || currentRoom.getStatus() != Room.Status.PLAYING) {
            send(Protocol.ERROR, "不在游戏中");
            return;
        }

        if (currentRoom.isObserver(this)) {
            send(Protocol.ERROR, "观战者不能请求求和");
            return;
        }

        // AI 模式下直接判平局，不发送请求
        if (currentRoom.isAiMode()) {
            currentRoom.directDraw(userId);
            currentRoom.broadcastAll(Protocol.GAME_OVER, "0", "双方平局(求和)");
            log.info("Player {} drew game in AI mode", userId);
            return;
        }

        boolean success = currentRoom.requestDraw(userId);
        if (success) {
            // 只通知对手，不通知请求者本人
            currentRoom.broadcastToPlayersExcept(userId,
                "DRAW_REQUESTED",
                String.valueOf(userId),
                getUsername()
            );
            log.info("玩家 {} 请求求和", userId);
        } else {
            send(Protocol.ERROR, "无法请求求和");
        }
    }

    /**
     * 回复求和请求
     * 协议: REPLY_DRAW|accept(1/0)
     * 响应: GAME_OVER|0|draw (同意求和)
     *       DRAW_REJECTED (拒绝)
     */
    private void handleReplyDraw(String[] parts) {
        if (currentRoom == null || currentRoom.getStatus() != Room.Status.PLAYING) {
            send(Protocol.ERROR, "不在游戏中");
            return;
        }

        if (currentRoom.isObserver(this)) {
            send(Protocol.ERROR, "观战者不能回复求和");
            return;
        }

        if (parts.length < 2) {
            send(Protocol.ERROR, "缺少参数");
            return;
        }

        boolean accept = "1".equals(parts[1]);
        boolean success = currentRoom.replyDraw(userId, accept);
        if (success) {
            if (accept) {
                currentRoom.broadcastAll(Protocol.GAME_OVER, "0", "双方平局(求和)");
                log.info("玩家 {} 同意求和，游戏平局结束", userId);
            } else {
                currentRoom.broadcastAll("DRAW_REJECTED");
                log.info("玩家 {} 拒绝求和", userId);
            }
        } else {
            send(Protocol.ERROR, "无法回复求和请求");
        }
    }

    /**
     * 断线重连
     * 协议: RECONNECT|token
     * 响应: RECONNECT_OK|userId|username|score|winCount|loseCount|drawCount|roomId|roomName|playerCount/max
     *       RECONNECT_FAIL|reason
     */
    private void handleReconnect(String[] parts) {
        if (parts.length < 2) {
            send("RECONNECT_FAIL", "缺少参数");
            return;
        }

        String token = parts[1];
        SessionManager.Session session = sessionManager.getSessionByToken(token);

        if (session == null) {
            send("RECONNECT_FAIL", "会话不存在或已过期");
            log.warn("Reconnect failed: session not found for token {}", token.substring(0, 8));
            return;
        }

        if (session.isExpired()) {
            sessionManager.removeSession(token);
            send("RECONNECT_FAIL", "会话已过期");
            log.warn("Reconnect failed: session expired for userId {}", session.userId);
            return;
        }

        this.userId = session.userId;
        this.username = session.username;
        this.authenticated = true;

        sessionManager.updateSessionHandler(session.userId, this);

        Room room = roomManager.findRoomByUserId(session.userId);
        if (room != null) {
            boolean reconnected = room.reconnect(this);
            if (reconnected) {
                this.currentRoom = room;
                sessionManager.updateSessionRoom(session.userId, room.getRoomId());

                send("RECONNECT_OK",
                        String.valueOf(session.userId),
                        session.username,
                        String.valueOf(session.score),
                        String.valueOf(session.winCount),
                        String.valueOf(session.loseCount),
                        String.valueOf(session.drawCount),
                        room.getRoomId(),
                        room.getRoomName(),
                        room.getPlayerCount() + "/" + 2);

                room.broadcastToPlayers(
                        Protocol.PLAYER_ENTER,
                        String.valueOf(session.userId),
                        session.username);

                log.info("玩家 {} 断线重连成功，进入房间 {}", session.userId, room.getRoomId());
            } else {
                send("RECONNECT_FAIL", "无法重新加入房间");
                log.warn("Reconnect failed: cannot rejoin room {} for userId {}", room.getRoomId(), session.userId);
            }
        } else {
            send("RECONNECT_OK",
                    String.valueOf(session.userId),
                    session.username,
                    String.valueOf(session.score),
                    String.valueOf(session.winCount),
                    String.valueOf(session.loseCount),
                    String.valueOf(session.drawCount),
                    "", "", "");

            log.info("玩家 {} 断线重连成功，不在房间中", session.userId);
        }
    }

    // ==================== 好友系统 ====================

    private void handleAddFriend(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (parts.length < 2) {
            send(Protocol.ERROR, "缺少好友ID");
            return;
        }

        long friendUserId;
        try {
            friendUserId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            send(Protocol.ERROR, "好友ID无效");
            return;
        }

        String message = parts.length >= 3 ? parts[2] : "";

        if (friendUserId == userId) {
            send(Protocol.ERROR, "不能添加自己为好友");
            return;
        }

        boolean success = friendDAO.sendFriendRequest(userId, friendUserId, message);
        if (success) {
            send("ADD_FRIEND_OK");
            log.info("玩家 {} 发送好友请求给 {}", userId, friendUserId);
        } else {
            send("ADD_FRIEND_FAIL", "发送失败（可能已为好友或请求已存在）");
            log.warn("玩家 {} 发送好友请求失败: {}", userId, friendUserId);
        }
    }

    private void handleAcceptFriend(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (parts.length < 2) {
            send(Protocol.ERROR, "缺少请求ID");
            return;
        }

        long requestId;
        try {
            requestId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            send(Protocol.ERROR, "请求ID无效");
            return;
        }

        boolean success = friendDAO.acceptFriendRequest(requestId, userId);
        if (success) {
            send("ACCEPT_FRIEND_OK");
            log.info("玩家 {} 接受好友请求: {}", userId, requestId);
        } else {
            send("ACCEPT_FRIEND_FAIL", "接受失败");
            log.warn("玩家 {} 接受好友请求失败: {}", userId, requestId);
        }
    }

    private void handleRejectFriend(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (parts.length < 2) {
            send(Protocol.ERROR, "缺少请求ID");
            return;
        }

        long requestId;
        try {
            requestId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            send(Protocol.ERROR, "请求ID无效");
            return;
        }

        boolean success = friendDAO.rejectFriendRequest(requestId, userId);
        if (success) {
            send("REJECT_FRIEND_OK");
            log.info("玩家 {} 拒绝好友请求: {}", userId, requestId);
        } else {
            send("REJECT_FRIEND_FAIL", "拒绝失败");
            log.warn("玩家 {} 拒绝好友请求失败: {}", userId, requestId);
        }
    }

    private void handleDeleteFriend(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (parts.length < 2) {
            send(Protocol.ERROR, "缺少好友ID");
            return;
        }

        long friendUserId;
        try {
            friendUserId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            send(Protocol.ERROR, "好友ID无效");
            return;
        }

        boolean success = friendDAO.deleteFriend(userId, friendUserId);
        if (success) {
            send("DELETE_FRIEND_OK");
            log.info("玩家 {} 删除好友: {}", userId, friendUserId);
        } else {
            send("DELETE_FRIEND_FAIL", "删除失败");
            log.warn("玩家 {} 删除好友失败: {}", userId, friendUserId);
        }
    }

    private void handleFriendList() {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        List<FriendDAO.FriendInfo> friends = friendDAO.getFriendList(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("FRIEND_LIST");
        for (FriendDAO.FriendInfo friend : friends) {
            sb.append("|").append(friend.getFriendUserId())
              .append("|").append(friend.getFriendUsername())
              .append("|").append(friend.getFriendScore());
        }
        send(sb.toString());
        log.info("发送好友列表给玩家 {}: {} 个好友", userId, friends.size());
    }

    private void handleFriendRequests() {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        List<FriendDAO.FriendRequestInfo> requests = friendDAO.getPendingRequests(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("FRIEND_REQUEST_LIST");
        for (FriendDAO.FriendRequestInfo request : requests) {
            sb.append("|").append(request.getRequestId())
              .append("|").append(request.getFromUserId())
              .append("|").append(request.getFromUsername())
              .append("|").append(request.getFromScore())
              .append("|").append(request.getMessage() != null ? request.getMessage() : "");
        }
        send(sb.toString());
        log.info("发送好友请求列表给玩家 {}: {} 个请求", userId, requests.size());
    }

    // ==================== 棋局保存 ====================

    private void handleSaveGame(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (currentRoom == null || !currentRoom.isGameStarted()) {
            send("SAVE_GAME_FAIL", "不在游戏房间或游戏未开始");
            return;
        }

        String saveName = parts.length >= 2 ? parts[1] : "未命名棋局";
        if (saveName.length() > 100) {
            saveName = saveName.substring(0, 100);
        }

        long saveId = gameSaveDAO.saveGame(userId, null, saveName, currentRoom.getGame());
        if (saveId > 0) {
            send("SAVE_GAME_OK", String.valueOf(saveId), saveName);
            log.info("玩家 {} 保存棋局成功: saveId={}", userId, saveId);
        } else {
            send("SAVE_GAME_FAIL", "保存失败");
            log.warn("玩家 {} 保存棋局失败", userId);
        }
    }

    private void handleLoadGame(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (parts.length < 2) {
            send(Protocol.ERROR, "缺少存档ID");
            return;
        }

        long saveId;
        try {
            saveId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            send(Protocol.ERROR, "存档ID无效");
            return;
        }

        GameSaveDAO.GameSaveInfo saveInfo = gameSaveDAO.getGameSave(saveId);
        if (saveInfo == null) {
            send("LOAD_GAME_FAIL", "存档不存在");
            return;
        }

        if (saveInfo.getUserId() != userId) {
            send("LOAD_GAME_FAIL", "无权加载此存档");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("LOAD_GAME_OK");
        sb.append("|").append(saveInfo.getSaveId());
        sb.append("|").append(saveInfo.getSaveName());
        sb.append("|").append(saveInfo.getBoardData());
        sb.append("|").append(saveInfo.getCurrentPlayer());
        sb.append("|").append(saveInfo.getGameStatus());
        send(sb.toString());
        log.info("玩家 {} 加载棋局成功: saveId={}", userId, saveId);
    }

    private void handleDeleteSave(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (parts.length < 2) {
            send(Protocol.ERROR, "缺少存档ID");
            return;
        }

        long saveId;
        try {
            saveId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            send(Protocol.ERROR, "存档ID无效");
            return;
        }

        boolean success = gameSaveDAO.deleteGameSave(saveId, userId);
        if (success) {
            send("DELETE_SAVE_OK");
            log.info("玩家 {} 删除棋局存档成功: saveId={}", userId, saveId);
        } else {
            send("DELETE_SAVE_FAIL", "删除失败");
            log.warn("玩家 {} 删除棋局存档失败: saveId={}", userId, saveId);
        }
    }

    private void handleSaveList(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        int page = 0;
        int size = 10;
        if (parts.length >= 2) {
            try {
                page = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }
        if (parts.length >= 3) {
            try {
                size = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {}
        }

        List<GameSaveDAO.GameSaveInfo> saves = gameSaveDAO.getGameSavesByUserId(userId, page, size);
        StringBuilder sb = new StringBuilder();
        sb.append("SAVE_LIST");
        for (GameSaveDAO.GameSaveInfo save : saves) {
            sb.append("|").append(save.getSaveId())
              .append("|").append(save.getSaveName())
              .append("|").append(save.getGameStatus())
              .append("|").append(save.getCreateTime());
        }
        send(sb.toString());
        log.info("发送棋局存档列表给玩家 {}: {} 个存档", userId, saves.size());
    }

    // ==================== 聊天历史 ====================

    private void handleChatHistory(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (currentRoom == null) {
            send(Protocol.ERROR, "不在房间中");
            return;
        }

        int page = 0;
        int size = 20;
        if (parts.length >= 2) {
            try {
                page = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }
        if (parts.length >= 3) {
            try {
                size = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {}
        }

        List<ChatDAO.ChatMessage> messages = chatDAO.getMessagesByRoomId(currentRoom.getRoomId(), page, size);
        StringBuilder sb = new StringBuilder();
        sb.append("CHAT_HISTORY");
        for (ChatDAO.ChatMessage msg : messages) {
            sb.append("|").append(msg.getMessageId())
              .append("|").append(msg.getUserId())
              .append("|").append(msg.getUsername())
              .append("|").append(msg.getContent())
              .append("|").append(msg.getCreateTime());
        }
        send(sb.toString());
        log.info("发送聊天历史给玩家 {}: {} 条消息", userId, messages.size());
    }

    // ==================== 观战记录 ====================

    private void handleWatcherHistory(String[] parts) {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        int page = 0;
        int size = 10;
        if (parts.length >= 2) {
            try {
                page = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }
        if (parts.length >= 3) {
            try {
                size = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {}
        }

        List<RoomWatcherDAO.RoomWatcher> watchers = roomWatcherDAO.getWatcherHistory(userId, page, size);
        StringBuilder sb = new StringBuilder();
        sb.append("WATCHER_HISTORY");
        for (RoomWatcherDAO.RoomWatcher w : watchers) {
            sb.append("|").append(w.getRoomId())
              .append("|").append(w.getJoinTime())
              .append("|").append(w.getLeaveTime() != null ? w.getLeaveTime() : "");
        }
        send(sb.toString());
        log.info("发送观战记录给玩家 {}: {} 条", userId, watchers.size());
    }

    // ==================== 退出登录 ====================

    private void handleLogout() {
        if (!authenticated) {
            send(Protocol.ERROR, "未登录");
            return;
        }

        if (currentRoom != null) {
            roomManager.leaveRoom(this);
            log.info("玩家 {} 退出登录时离开房间 {}", userId, currentRoom.getRoomId());
        }

        sessionManager.removeSessionByUserId(userId);

        send(Protocol.LOGOUT_OK);
        log.info("玩家 {} 退出登录成功", userId);

        this.userId = 0;
        this.username = null;
        this.authenticated = false;
        this.currentRoom = null;
    }

    // ==================== 资源清理 ====================

    private void cleanup() {
        log.info("Client disconnecting: {} (auth={}, room={})", 
                socket.getInetAddress(), authenticated, 
                currentRoom != null ? currentRoom.getRoomId() : "none");

        if (currentRoom != null) {
            currentRoom.disconnect(this);
            log.info("玩家 {} 从房间 {} 断开连接", userId, currentRoom.getRoomId());
        }

        if (authenticated && userId > 0) {
            sessionManager.updateSessionHandler(userId, null);
            log.info("用户 {} 断开连接，保留会话等待重连", userId);
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.error("Error closing socket", e);
        }

        this.reader = null;
        this.writer = null;
        this.userId = 0;
        this.username = null;
        this.currentRoom = null;
        this.authenticated = false;

        // 递减服务器端全局连接计数
        com.gomatch.server.ServerMain.decrementConnections();
        log.info("Active connections decremented. Current: {}", com.gomatch.server.ServerMain.getActiveConnections());
    }

    // ==================== Getter ====================

    public Socket getSocket() { return socket; }
    public long getUserId() { return userId; }
    public String getUsername() { return username != null ? username : "unknown"; }
    public boolean isAuthenticated() { return authenticated; }
    public Room getCurrentRoom() { return currentRoom; }

    public void setUserId(long userId) {
        this.userId = userId;
        this.authenticated = true;
    }

    public void setCurrentRoom(Room currentRoom) {
        this.currentRoom = currentRoom;
    }
}

