package com.gomatch.server.handler;

import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import com.gomatch.server.dao.*;
import com.gomatch.server.game.Game;
import com.gomatch.server.redis.RedisManager;
import com.gomatch.server.room.Room;
import com.gomatch.server.room.RoomManager;
import com.gomatch.server.session.SessionManager;
import com.gomatch.server.security.PhoneNumberNormalizer;
import com.gomatch.server.service.ConfigService;
import com.gomatch.server.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

/**
 * 客户端处理器（JSON 协议版本）
 * 每个客户端连接分配一个独立的 ClientHandler 线程
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
    private static final ConfigService configService = ConfigService.getInstance();

    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    
    private long userId;
    private String username;
    private String role = "PLAYER";
    private String sessionToken;
    private boolean authenticated;
    private volatile long lastMessageTime = System.currentTimeMillis();
    private volatile boolean heartbeatTimeout = false;
    
    Room currentRoom;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            log.info("Client connected: {} from {}", socket.getLocalPort(), socket.getInetAddress());

            // 启动心跳超时检测线程
            Thread timeoutChecker = new Thread(this::checkHeartbeatTimeout, "heartbeat-timeout-" + socket.getPort());
            timeoutChecker.setDaemon(true);
            timeoutChecker.start();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                lastMessageTime = System.currentTimeMillis();
                log.debug("Received from {}: {}", socket.getPort(), line);
                try {
                    Message msg = Message.fromJson(line);
                    dispatch(msg);
                } catch (Exception e) {
                    log.warn("Failed to parse message from {}: {}", socket.getInetAddress(), line);
                    send(Message.of(Protocol.ERROR, "msg", "消息格式错误"));
                }
            }
        } catch (IOException e) {
            log.warn("Client read error: {}", socket.getInetAddress());
        } finally {
            heartbeatTimeout = true; // 停止超时检测线程
            cleanup();
        }
    }

    /**
     * 心跳超时检测：每 30 秒检查一次，若 90 秒未收到任何消息则判定断线
     */
    private void checkHeartbeatTimeout() {
        while (!heartbeatTimeout) {
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                break;
            }
            if (heartbeatTimeout) break;
            long elapsed = System.currentTimeMillis() - lastMessageTime;
            if (elapsed > 90000) {
                log.warn("Heartbeat timeout: no message from {} for {}s, closing connection",
                        socket.getInetAddress(), elapsed / 1000);
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error("Error closing socket on timeout", e);
                }
                break;
            }
        }
    }

    // ==================== 消息分发 ====================

    private void dispatch(Message msg) {
        String cmd = msg.getCmd();
        switch (cmd) {
            case Protocol.LOGIN           -> handleLogin(msg);
            case Protocol.REGISTER        -> handleRegister(msg);
            case Protocol.CREATE_ROOM     -> handleCreateRoom(msg);
            case Protocol.CREATE_AI_ROOM  -> handleCreateAiRoom(msg);
            case Protocol.JOIN_ROOM       -> handleJoinRoom(msg);
            case Protocol.READY           -> handleReady(msg);
            case Protocol.LEAVE_ROOM      -> handleLeaveRoom();
            case Protocol.MOVE            -> handleMove(msg);
            case Protocol.CHAT            -> handleChat(msg);
            case Protocol.SURRENDER       -> handleSurrender();
            case Protocol.SPECTATE        -> handleSpectate(msg);
            case Protocol.LEAVE_SPECTATE -> handleLeaveSpectate();
            case Protocol.ROOM_LIST       -> handleRoomList();
            case Protocol.QUICK_MATCH     -> handleQuickMatch();
            case Protocol.CANCEL_MATCH    -> handleCancelMatch();
            case Protocol.RANKED_MATCH    -> handleRankedMatch();
            case Protocol.RANKED_READY    -> handleRankedReady(msg);
            case Protocol.QUICK_READY    -> handleQuickReady(msg);
            case Protocol.RANKING         -> handleRanking(msg);
            case Protocol.QUERY_RECORD    -> handleQueryRecord(msg);
            case Protocol.REQUEST_UNDO    -> handleRequestUndo();
            case Protocol.REPLY_UNDO      -> handleReplyUndo(msg);
            case Protocol.REQUEST_DRAW    -> handleRequestDraw();
            case Protocol.REPLY_DRAW      -> handleReplyDraw(msg);
            case Protocol.REQUEST_REMATCH -> handleRequestRematch();
            case Protocol.REPLY_REMATCH   -> handleReplyRematch(msg);
            case Protocol.RECONNECT       -> handleReconnect(msg);
            case Protocol.CHANGE_PWD      -> handleChangePassword(msg);
            case Protocol.ADD_FRIEND      -> handleAddFriend(msg);
            case Protocol.ACCEPT_FRIEND   -> handleAcceptFriend(msg);
            case Protocol.REJECT_FRIEND   -> handleRejectFriend(msg);
            case Protocol.DELETE_FRIEND   -> handleDeleteFriend(msg);
            case Protocol.FRIEND_LIST     -> handleFriendList();
            case Protocol.FRIEND_REQUESTS -> handleFriendRequests();
            case Protocol.SAVE_GAME       -> handleSaveGame(msg);
            case Protocol.LOAD_GAME       -> handleLoadGame(msg);
            case Protocol.DELETE_SAVE     -> handleDeleteSave(msg);
            case Protocol.SAVE_LIST       -> handleSaveList(msg);
            case Protocol.CHAT_HISTORY    -> handleChatHistory(msg);
            case Protocol.WATCHER_HISTORY -> handleWatcherHistory(msg);
            case Protocol.LOGOUT          -> handleLogout();
            case Protocol.HEARTBEAT       -> handleHeartbeat();
            case Protocol.REQ_GET_CONFIG  -> handleGetConfig();
            case Protocol.REQ_UPDATE_CONFIG -> handleUpdateConfig(msg);
            case Protocol.AI_MATCH        -> handleAiMatch(msg);
            case Protocol.UPLOAD_AVATAR   -> handleUploadAvatar(msg);
            case Protocol.GET_AVATAR      -> handleGetAvatar(msg);
            case Protocol.DELETE_AVATAR   -> handleDeleteAvatar();
            // Admin commands
            case Protocol.BAN_USER         -> handleBanUser(msg);
            case Protocol.UNBAN_USER       -> handleUnbanUser(msg);
            case Protocol.FORCE_CLOSE_ROOM -> handleForceCloseRoom(msg);
            case Protocol.USER_LIST        -> handleUserList(msg);
            case Protocol.ADMIN_STATS      -> handleAdminStats();
            case Protocol.UPDATE_USER_ROLE -> handleUpdateUserRole(msg);
            // Forgot password
            case Protocol.FORGOT_PASSWORD    -> handleForgotPassword(msg);
            case Protocol.VERIFY_FORGOT_CODE -> handleVerifyForgotCode(msg);
            case Protocol.FORGOT_PWD_RESET   -> handleForgotPwdReset(msg);
            default -> send(Message.of(Protocol.ERROR, "msg", "Unknown command: " + cmd));
        }
    }

    // ==================== 发送方法 ====================

    /** 发送 JSON 消息 */
    public void send(Message msg) {
        if (writer != null) {
            writer.println(msg.toJson());
            log.debug("Sent to {}: {}", socket.getPort(), msg.toJson());
        }
    }

    public void sendAndFlush(Message msg) {
        if (writer != null) {
            writer.println(msg.toJson());
            writer.flush();
            log.debug("Sent and flushed to {}: {}", socket.getPort(), msg.toJson());
        }
    }

    /** 发送仅含命令的消息（兼容旧调用风格） */
    public void send(String text) {
        if (writer != null) {
            writer.println(text);
        }
    }

    // ==================== 注册 ====================

    private void handleRegister(Message msg) {
        String username = msg.getString("username");
        String password = msg.getString("password");
        String email = msg.has("email") ? msg.getString("email") : "";
        String phone = msg.has("phone") ? msg.getString("phone") : "";

        log.info("注册请求: username={}, email={}, phone={}", username, email, phone);

        if (username == null || username.trim().isEmpty()) {
            send(Message.of("REGISTER_FAIL", "msg", "用户名不能为空"));
            return;
        }
        if (username.length() > 16) {
            send(Message.of("REGISTER_FAIL", "msg", "用户名最多16个字符"));
            return;
        }
        if (password == null || password.length() < 6) {
            send(Message.of("REGISTER_FAIL", "msg", "密码至少6位"));
            return;
        }
        if (email.isEmpty()) {
            send(Message.of("REGISTER_FAIL", "msg", "邮箱不能为空"));
            return;
        }
        // 简单邮箱格式校验
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            send(Message.of("REGISTER_FAIL", "msg", "邮箱格式不正确"));
            return;
        }

        String normalizedPhone = null;
        if (phone != null && !phone.trim().isEmpty()) {
            normalizedPhone = PhoneNumberNormalizer.normalize(phone);
            if (normalizedPhone == null) {
                send(Message.of("REGISTER_FAIL", "msg", "手机号必须是有效的国际号码（E.164，含国家区号）"));
                return;
            }
        }

        boolean success = userDAO.register(username, password, email, normalizedPhone);
        if (success) {
            send(Message.cmdOnly("REGISTER_OK"));
            log.info("用户注册成功: {} (email={})", username, email);
        } else {
            // 检查是用户名重复还是邮箱重复
            if (userDAO.existsByUsername(username)) {
                send(Message.of("REGISTER_FAIL", "msg", "用户名已存在"));
            } else if (userDAO.emailExists(email)) {
                send(Message.of("REGISTER_FAIL", "msg", "该邮箱已被注册"));
            } else if (userDAO.phoneExists(normalizedPhone)) {
                send(Message.of("REGISTER_FAIL", "msg", "手机号已被注册"));
            } else {
                send(Message.of("REGISTER_FAIL", "msg", "注册失败，请重试"));
            }
            log.warn("Register failed: username={}, email={}", username, email);
        }
    }

    // ==================== 登录 ====================

    private void handleLogin(Message msg) {
        String account = msg.getString("account");
        String password = msg.getString("password");
        String phone = msg.has("phone") ? msg.getString("phone") : null;

        if (account == null || account.trim().isEmpty() || password == null) {
            send(Message.of(Protocol.LOGIN_FAIL, "msg", "账号或密码不能为空"));
            return;
        }

        log.info("登录请求: account={}", account);

        UserDAO.UserInfo userInfo = userDAO.login(account, password, phone);
        if (userInfo != null) {
            SessionManager.Session session = null;
            synchronized (sessionManager) {
                SessionManager.Session existingSession = sessionManager.getSessionByUserId(userInfo.getUserId());
                if (existingSession != null && existingSession.handler != null) {
                    log.info("拒绝重复登录: 用户 {} 已在其他设备登录", account);
                    send(Message.of(Protocol.LOGIN_FAIL, "msg", "您的账号已在其他设备登录"));
                    return;
                }

                this.userId = userInfo.getUserId();
                this.username = userInfo.getUsername();
                this.role = userInfo.getRole();
                this.authenticated = true;

                session = sessionManager.createSession(
                        userInfo.getUserId(), userInfo.getUsername(),
                        userInfo.getScore(), userInfo.getWinCount(),
                        userInfo.getLoseCount(), userInfo.getDrawCount(),
                        userInfo.getRole());
                this.sessionToken = session.token;
                sessionManager.updateSessionHandler(userInfo.getUserId(), this);
            }

            int rank = 0;
            RankDAO.RankInfo rankInfo = rankDAO.getRankByUserId(userInfo.getUserId());
            if (rankInfo != null) {
                rank = rankInfo.getRank();
            }

            // 检查是否有进行中的对局（断线后重开客户端）
            Room activeRoom = roomManager.findRoomByUserId(userInfo.getUserId());
            if (activeRoom != null && activeRoom.getStatus() == Room.Status.PLAYING
                    && (userInfo.getUserId() == activeRoom.getBlackUserId()
                        || userInfo.getUserId() == activeRoom.getWhiteUserId())) {

                boolean reconnected = activeRoom.reconnect(this);
                if (reconnected) {
                    this.currentRoom = activeRoom;
                    sessionManager.updateSessionRoom(userInfo.getUserId(), activeRoom.getRoomId());

                    int currentColor = (userInfo.getUserId() == activeRoom.getBlackUserId()) ? 1 : 2;
                    long opponentId = (userInfo.getUserId() == activeRoom.getBlackUserId())
                            ? activeRoom.getWhiteUserId() : activeRoom.getBlackUserId();
                    long disconnectedId = activeRoom.isPlayerDisconnected(opponentId) ? opponentId : 0;

                    String avatarPath = userDAO.getAvatar(userInfo.getUserId());
                    String avatarUrl = generateAvatarUrl(avatarPath);

                    // 双方头像 URL（断线重连后客户端不会收到 GAME_START，必须在 LOGIN_OK 携带）
                    String blackAvatarUrl = generateAvatarUrl(userDAO.getAvatar(activeRoom.getBlackUserId()));
                    String whiteAvatarUrl = generateAvatarUrl(userDAO.getAvatar(activeRoom.getWhiteUserId()));

                    send(Message.builder(Protocol.LOGIN_OK)
                            .set("userId", String.valueOf(userInfo.getUserId()))
                            .set("username", userInfo.getUsername())
                            .set("score", String.valueOf(userInfo.getScore()))
                            .set("winCount", String.valueOf(userInfo.getWinCount()))
                            .set("loseCount", String.valueOf(userInfo.getLoseCount()))
                            .set("drawCount", String.valueOf(userInfo.getDrawCount()))
                            .set("rank", String.valueOf(rank))
                            .set("role", userInfo.getRole())
                            .set("avatar", avatarUrl)
                            .set("phone", userInfo.getPhone() != null ? userInfo.getPhone() : "")
                            .set("token", session.token)
                            .set("roomId", activeRoom.getRoomId())
                            .set("blackUserId", String.valueOf(activeRoom.getBlackUserId()))
                            .set("whiteUserId", String.valueOf(activeRoom.getWhiteUserId()))
                            .set("blackAvatarUrl", blackAvatarUrl)
                            .set("whiteAvatarUrl", whiteAvatarUrl)
                            .set("currentColor", String.valueOf(currentColor))
                            .set("disconnectedUserId", String.valueOf(disconnectedId))
                            .build());

                    sendConfig();

                    // 回放已有落子（在 LOGIN_OK 之后发送，确保客户端先初始化对局界面）
                    activeRoom.replayMovesTo(this);

                    // 通知对手该玩家已重连
                    activeRoom.broadcastToPlayers(Message.of(Protocol.PLAYER_ENTER,
                            "userId", String.valueOf(userInfo.getUserId()),
                            "username", userInfo.getUsername()));

                    log.info("User logged in and reconnected to room {}: {} (id={})",
                            activeRoom.getRoomId(), userInfo.getUsername(), userInfo.getUserId());
                    return;
                }
            }

            String avatarPath = userDAO.getAvatar(userInfo.getUserId());
            String avatarUrl = generateAvatarUrl(avatarPath);
            send(Message.builder(Protocol.LOGIN_OK)
                    .set("userId", String.valueOf(userInfo.getUserId()))
                    .set("username", userInfo.getUsername())
                    .set("score", String.valueOf(userInfo.getScore()))
                    .set("winCount", String.valueOf(userInfo.getWinCount()))
                    .set("loseCount", String.valueOf(userInfo.getLoseCount()))
                    .set("drawCount", String.valueOf(userInfo.getDrawCount()))
                    .set("rank", String.valueOf(rank))
                    .set("role", userInfo.getRole())
                    .set("avatar", avatarUrl)
                    .set("phone", userInfo.getPhone() != null ? userInfo.getPhone() : "")
                    .set("token", session.token)
                    .build());
            sendConfig();
            
            log.info("User logged in: {} (id={}, score={}, token={})", 
                    userInfo.getUsername(), userInfo.getUserId(), userInfo.getScore(),
                    session.token.substring(0, 8));
        } else {
            send(Message.of(Protocol.LOGIN_FAIL, "msg", "账号或密码错误"));
            log.warn("登录失败: {}", account);
        }
    }

    private void handleGetConfig() {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "请先登录"));
            return;
        }
        sendConfig();
    }

    private void handleUpdateConfig(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "请先登录"));
            return;
        }
        boolean updated = configService.updateConfig(userId, msg.getString("configJson"));
        send(Message.of(Protocol.RESP_UPDATE_CONFIG, "success", updated));
    }

    private void sendConfig() {
        String configJson = configService.getConfigJson(userId);
        if (configJson != null) {
            send(Message.of(Protocol.RESP_GET_CONFIG, "configJson", configJson));
        }
    }
    // ==================== 修改密码 ====================

    private void handleChangePassword(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        String oldPassword = msg.getString("oldPassword");
        String newPassword = msg.getString("newPassword");

        if (oldPassword == null || newPassword == null || newPassword.length() < 6) {
            send(Message.of(Protocol.ERROR, "msg", "新密码至少6位"));
            return;
        }
        if (newPassword.equals(oldPassword)) {
            send(Message.of(Protocol.ERROR, "msg", "新密码不能与旧密码相同"));
            return;
        }

        boolean success = userDAO.changePassword(userId, oldPassword, newPassword);
        if (success) {
            send(Message.cmdOnly(Protocol.PWD_CHANGED));
            log.info("用户 {} 修改密码成功", userId);
        } else {
            send(Message.of(Protocol.ERROR, "msg", "旧密码错误"));
            log.warn("用户 {} 修改密码失败: 旧密码错误", userId);
        }
    }

    // ==================== 忘记密码 ====================

    private void handleForgotPassword(Message msg) {
        String email = msg.getString("email");
        if (email == null || email.trim().isEmpty()) {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "邮箱不能为空"));
            return;
        }

        email = email.trim().toLowerCase();

        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "邮箱格式不正确"));
            return;
        }

        long userId = userDAO.findUserIdByEmail(email);
        if (userId == 0) {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "该邮箱未注册或账号异常"));
            log.warn("Forgot password: email not found - {}", email);
            return;
        }

        String code = VerificationCodeManager.getInstance().generateCode(email);
        send(Message.of(Protocol.FORGOT_PWD_CODE,
                "code", code,
                "email", email));
        log.info("Forgot password code sent to {} (userId={})", email, userId);
    }

    private void handleVerifyForgotCode(Message msg) {
        String email = msg.getString("email");
        String code = msg.getString("code");

        if (email == null || email.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "邮箱和验证码不能为空"));
            return;
        }

        email = email.trim().toLowerCase();

        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "邮箱格式不正确"));
            return;
        }

        // 先确认邮箱存在
        long userId = userDAO.findUserIdByEmail(email);
        if (userId == 0) {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "该邮箱未注册或账号异常"));
            return;
        }

        if (!VerificationCodeManager.getInstance().isCodeValid(email)) {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "验证码已过期，请重新获取"));
            return;
        }

        boolean verified = VerificationCodeManager.getInstance().verifyCode(email, code);
        if (verified) {
            send(Message.builder(Protocol.FORGOT_PWD_VERIFY_OK)
                    .set("email", email)
                    .build());
            log.info("Forgot password code verified for {}", email);
        } else {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "验证码错误"));
        }
    }

    private void handleForgotPwdReset(Message msg) {
        String email = msg.getString("email");
        String code = msg.getString("code");
        String newPassword = msg.getString("newPassword");

        if (email == null || email.trim().isEmpty()
                || code == null || code.trim().isEmpty()
                || newPassword == null || newPassword.length() < 6) {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "参数无效，新密码至少6位"));
            return;
        }

        email = email.trim().toLowerCase();

        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "邮箱格式不正确"));
            return;
        }

        // 再次校验验证码（验证码可能已被 consume，所以这里重新检查）
        // 实际上 verifyCode 成功后已清除，所以改用：先查用户，再重置
        long userId = userDAO.findUserIdByEmail(email);
        if (userId == 0) {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "该邮箱未注册或账号异常"));
            return;
        }

        // 安全校验：必须已通过 verify 步骤（通过则清除标记防止重复重置）
        VerificationCodeManager vcm = VerificationCodeManager.getInstance();
        if (!vcm.isVerified(email) && !vcm.verifyCode(email, code)) {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "验证码错误或已过期，请重新获取"));
            return;
        }

        boolean success = userDAO.resetPassword(userId, newPassword);
        if (success) {
            vcm.clearVerified(email);
            send(Message.cmdOnly(Protocol.FORGOT_PWD_OK));
            log.info("Password reset for userId={} (email={})", userId, email);
        } else {
            send(Message.of(Protocol.FORGOT_PWD_FAIL, "msg", "重置密码失败，请重试"));
        }
    }

    // ==================== 创建房间 ====================

    private void handleCreateRoom(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        String roomName = msg.getString("roomName");
        if (roomName == null || roomName.trim().isEmpty()) {
            send(Message.of(Protocol.ERROR, "msg", "房间名不能为空"));
            return;
        }

        String password = msg.has("password") ? msg.getString("password") : null;
        if (password != null && password.trim().isEmpty()) password = null;

        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }

        Room room = roomManager.createRoom(roomName, this, password);
        this.currentRoom = room;

        send(Message.builder(Protocol.JOIN_OK)
                .set("roomId", room.getRoomId())
                .set("roomName", room.getRoomName())
                .set("players", room.getPlayerCount() + "/" + 2)
                .set("hasPassword", room.hasPassword() ? "1" : "0")
                .build());

        log.info("Player {} created room: {} ({}) with password: {}", userId, room.getRoomId(), roomName, password != null);
    }

    // ==================== 创建 AI 房间 ====================

    private void handleCreateAiRoom(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        String roomName = msg.getString("roomName");
        if (roomName == null || roomName.trim().isEmpty()) {
            send(Message.of(Protocol.ERROR, "msg", "房间名不能为空"));
            return;
        }

        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }

        Room room = roomManager.createAiRoom(roomName, this);
        this.currentRoom = room;

        send(Message.builder(Protocol.JOIN_OK)
                .set("roomId", room.getRoomId())
                .set("roomName", room.getRoomName())
                .set("players", room.getPlayerCount() + "/" + 1)
                .set("type", "AI")
                .build());

        room.startGame();
        room.broadcastAll(room.buildGameStartMessage());

        log.info("Player {} created AI room: {} ({}), game started", userId, room.getRoomId(), roomName);
    }

    // ==================== AI 对战 ====================

    private void handleAiMatch(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        String difficulty = msg.has("difficulty") ? msg.getString("difficulty") : "EASY";

        log.info("AI对战请求: userId={}, difficulty={}", userId, difficulty);

        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }

        String roomName = "AI对战 - " + difficulty;
        Room room = roomManager.createAiRoom(roomName, this, difficulty);
        this.currentRoom = room;

        send(Message.builder(Protocol.JOIN_OK)
                .set("roomId", room.getRoomId())
                .set("roomName", roomName)
                .set("players", "1/2")
                .set("hasPassword", "0")
                .build());

        room.startGame();
        room.broadcastAll(room.buildGameStartMessage());

        log.info("AI对战房间创建成功: userId={}, roomId={}, difficulty={}", userId, room.getRoomId(), difficulty);
    }

    // ==================== 加入房间 ====================

    private void handleJoinRoom(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        String roomId = msg.getString("roomId");
        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            send(Message.of(Protocol.JOIN_FAIL, "msg", "房间不存在"));
            return;
        }

        String password = msg.has("password") ? msg.getString("password") : null;

        if (currentRoom != null && currentRoom != room) {
            roomManager.leaveRoom(this);
        }

        if (room.hasPassword() && !room.checkPassword(password)) {
            send(Message.of(Protocol.JOIN_FAIL, "msg", "密码错误"));
            return;
        }

        boolean success = roomManager.joinExistingRoom(this, roomId, password);
        if (success) {
            this.currentRoom = room;

            room.broadcastToPlayers(Message.of(Protocol.PLAYER_ENTER,
                    "userId", String.valueOf(getUserId()),
                    "username", getUsername()));

            Message.Builder joinOkBuilder = Message.builder(Protocol.JOIN_OK)
                    .set("roomId", room.getRoomId())
                    .set("roomName", room.getRoomName())
                    .set("players", room.getPlayerCount() + "/" + 2)
                    .set("blackUserId", String.valueOf(room.getBlackUserId()))
                    .set("blackUsername", room.getBlackUsername());

            if (room.getPlayerCount() == 2) {
                joinOkBuilder.set("whiteUserId", String.valueOf(room.getWhiteUserId()))
                        .set("whiteUsername", room.getWhiteUsername());
                log.info("房间 {} 已满，等待双方准备", roomId);
            }

            send(joinOkBuilder.build());

            log.info("玩家 {} 加入房间 {}", userId, roomId);
        } else {
            send(Message.of(Protocol.JOIN_FAIL, "msg", "房间已满或游戏进行中"));
        }
    }

    // ==================== 准备/取消准备 ====================

    private void handleReady(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        if (currentRoom == null) {
            send(Message.of(Protocol.ERROR, "msg", "不在房间"));
            return;
        }

        Room room = currentRoom;
        boolean ready = "true".equals(msg.getString("ready"));

        boolean success = room.setReady(userId, ready);
        if (success) {
            room.broadcastToPlayers(Message.of(Protocol.READY,
                    "blackReady", String.valueOf(room.isBlackReady()),
                    "whiteReady", String.valueOf(room.isWhiteReady())));

            if (room.getPlayerCount() == 2 && room.bothReady()) {
                room.startGame();
                room.broadcastAll(room.buildGameStartMessage());
                log.info("房间 {} 双方已准备，游戏开始: BLACK={}, WHITE={}", room.getRoomId(), room.getBlackUserId(), room.getWhiteUserId());
            }
        } else {
            send(Message.of(Protocol.ERROR, "msg", "设置准备状态失败"));
        }
    }

    // ==================== 离开房间 ====================

    private void handleLeaveRoom() {
        if (currentRoom == null) {
            send(Message.of(Protocol.ERROR, "msg", "不在房间"));
            return;
        }

        Room room = currentRoom;
        String roomId = room.getRoomId();

        room.broadcastToPlayers(Message.of(Protocol.PLAYER_LEAVE,
                "userId", String.valueOf(userId),
                "username", getUsername()));

        roomManager.leaveRoom(this);
        log.info("玩家 {} 离开房间 {}", userId, roomId);
    }

    // ==================== 房间列表 ====================

    private void handleRoomList() {
        List<String[]> roomList = Room.getRoomListInfo(roomManager.getAllRooms());

        JSONArray rooms = new JSONArray();
        for (String[] info : roomList) {
            JSONObject r = new JSONObject();
            r.put("roomId", info[0]);
            r.put("roomName", info[1]);
            r.put("status", info[2]);
            r.put("players", info[3]);
            r.put("max", info[4]);
            r.put("hasPassword", info[5]);
            r.put("ownerName", info[6]);
            rooms.put(r);
        }
        send(Message.builder(Protocol.ROOM_LIST).set("rooms", rooms).build());
        log.info("Sent room list to {}: {} rooms", socket.getPort(), roomList.size());
    }

    // ==================== 快速匹配 ====================

    private void handleQuickMatch() {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }

        Room room = roomManager.quickMatch(this);
        if (room != null) {
            this.currentRoom = room;

            log.info("玩家 {} 配对成功，房间: {}", userId, room.getRoomId());
        } else {
            send(Message.of(Protocol.ERROR, "msg", "等待对手... (" + roomManager.getMatchQueueSize() + "/2)"));
        }
    }

    // ==================== 取消匹配 ====================

    private void handleCancelMatch() {
        if (!authenticated) return;
        if (currentRoom != null && currentRoom.isGameStarted()) {
            send(Message.of(Protocol.ERROR, "msg", "对局已开始，无法取消"));
            return;
        }
        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }
        roomManager.cancelMatch(userId);
        roomManager.cancelRankedMatch(userId);
        send(Message.of(Protocol.CANCEL_MATCH, "msg", "已取消匹配"));
        log.info("玩家 {} 取消匹配", userId);
    }

    // ==================== 排位匹配 ====================

    private void handleRankedMatch() {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }

        UserDAO.UserInfo userInfo = userDAO.getUserById(userId);
        if (userInfo == null) {
            send(Message.of(Protocol.ERROR, "msg", "用户信息不存在"));
            return;
        }

        Room room = roomManager.rankedMatch(this, userInfo.getScore());
        if (room != null) {
            this.currentRoom = room;

            send(Message.builder(Protocol.JOIN_OK)
                    .set("roomId", room.getRoomId())
                    .set("roomName", room.getRoomName())
                    .set("players", room.getPlayerCount() + "/" + 2)
                    .set("type", "RANKED")
                    .build());

            log.info("玩家 {} 排位匹配成功，房间: {}", userId, room.getRoomId());
        } else {
            send(Message.of("RANKED_MATCH_WAIT", "msg", "等待排位对手... (" + roomManager.getRankedMatchQueueSize() + "/2)"));
        }
    }

    private void handleRankedReady(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        if (currentRoom == null) {
            send(Message.of(Protocol.ERROR, "msg", "不在房间中"));
            return;
        }

        if (currentRoom.getGameType() != Room.GameType.RANKED) {
            send(Message.of(Protocol.ERROR, "msg", "非排位模式"));
            return;
        }

        if (currentRoom.isGameStarted()) {
            send(Message.of(Protocol.ERROR, "msg", "游戏已开始"));
            return;
        }

        boolean ready = "true".equals(msg.getString("ready"));
        currentRoom.setReady(userId, ready);

        boolean blackReady = currentRoom.isBlackReady();
        boolean whiteReady = currentRoom.isWhiteReady();

        currentRoom.broadcastAll(Message.of(Protocol.RANKED_READY,
                "blackReady", String.valueOf(blackReady),
                "whiteReady", String.valueOf(whiteReady)));

        if (currentRoom.bothReady()) {
            log.info("双方玩家已准备，开始排位赛: room={}", currentRoom.getRoomId());
            currentRoom.startGame();
            currentRoom.broadcastAll(currentRoom.buildGameStartMessage());
        }
    }

    private void handleQuickReady(Message msg) {
        if (currentRoom == null) {
            log.warn("handleQuickReady: not in a room");
            return;
        }

        if (currentRoom.isGameStarted()) {
            send(Message.of(Protocol.ERROR, "msg", "游戏已开始"));
            return;
        }

        boolean ready = "true".equals(msg.getString("ready"));
        currentRoom.setReady(userId, ready);

        boolean blackReady = currentRoom.isBlackReady();
        boolean whiteReady = currentRoom.isWhiteReady();

        currentRoom.broadcastAll(Message.of(Protocol.QUICK_READY,
                "blackReady", String.valueOf(blackReady),
                "whiteReady", String.valueOf(whiteReady)));

        if (currentRoom.bothReady()) {
            log.info("双方玩家已准备，开始快速匹配对局: room={}", currentRoom.getRoomId());
            currentRoom.startGame();
            currentRoom.broadcastAll(currentRoom.buildGameStartMessage());
        }
    }

    // ==================== 游戏操作：落子 ====================

    private void handleMove(Message msg) {
        log.info("handleMove called: msg={}, currentRoom={}, status={}, aiMode={}",
                msg.toJson(), currentRoom, 
                currentRoom != null ? currentRoom.getStatus() : "null",
                currentRoom != null ? currentRoom.isAiMode() : "null");

        if (currentRoom == null || currentRoom.getStatus() != Room.Status.PLAYING) {
            send(Message.of(Protocol.ERROR, "msg", "不在游戏中"));
            return;
        }

        int x = msg.getInt("x");
        int y = msg.getInt("y");

        int color = currentRoom.getGame().getCurrentPlayer();
        log.info("Player move: x={}, y={}, color={}, aiMode={}", x, y, 
                color == Game.BLACK ? "BLACK" : "WHITE", currentRoom.isAiMode());
        
        boolean success = currentRoom.makeMove(x, y, this);
        if (!success) {
            send(Message.of(Protocol.ERROR, "msg", "无效的移动子操作"));
            return;
        }

        currentRoom.broadcastAll(Message.of(Protocol.MOVE_SYNC,
                "x", String.valueOf(x),
                "y", String.valueOf(y),
                "color", String.valueOf(color)));

        if (currentRoom.isGameOver()) {
            long winnerId = currentRoom.getWinnerId();
            boolean isDraw = winnerId == 0;
            String reason = isDraw ? "board_full" : "five_in_a_row";
            String resultMsg = isDraw ? "棋盘已满，平局" : "五子连珠!";
            currentRoom.endGame(winnerId, reason, resultMsg);

            log.info("Game over in room {}: winner={}, reason={}", currentRoom.getRoomId(), winnerId, reason);
        } else if (currentRoom.isAiMode()) {
            log.info("Calling makeAiMove() for room {}", currentRoom.getRoomId());
            currentRoom.makeAiMove();
            log.info("makeAiMove() completed for room {}", currentRoom.getRoomId());
        }
    }

    // ==================== 聊天 ====================

    private void handleChat(Message msg) {
        if (currentRoom == null) {
            send(Message.of(Protocol.ERROR, "msg", "不在房间中"));
            return;
        }

        String message = msg.getString("message");
        currentRoom.broadcastAll(Message.of(Protocol.CHAT_MSG,
                "userId", String.valueOf(userId),
                "username", getUsername(),
                "message", message));
        
        // 发布到 Redis Pub/Sub（保持兼容）
        RedisManager.getInstance().publish(currentRoom.getRoomId(),
            Message.of(Protocol.CHAT_MSG,
                "userId", String.valueOf(userId),
                "username", getUsername(),
                "message", message).toJson());

        // 保存聊天消息到数据库
        chatDAO.saveMessage(currentRoom.getRoomId(), userId, message, "text");

        log.info("玩家 {} 在房间 {} 发送聊天消息: [{}]", getUsername(), currentRoom.getRoomId(), message);
    }

    // ==================== 认输 ====================

    private void handleSurrender() {
        if (currentRoom == null || currentRoom.getStatus() != Room.Status.PLAYING) {
            send(Message.of(Protocol.ERROR, "msg", "不在游戏中"));
            return;
        }

        if (currentRoom.isObserver(this)) {
            send(Message.of(Protocol.ERROR, "msg", "观战者不能认输"));
            return;
        }

        long winnerId = (userId == currentRoom.getBlackUserId()) 
                ? currentRoom.getWhiteUserId() 
                : currentRoom.getBlackUserId();

        currentRoom.endGame(winnerId, "surrender", getUsername() + " 认输");

        log.info("玩家 {} 在房间 {} 认输，获胜方: {}", userId, currentRoom.getRoomId(), winnerId);
    }

    // ==================== 观战 ====================

    private void handleSpectate(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        String roomId = msg.getString("roomId");
        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            send(Message.of("JOIN_FAIL", "msg", "房间不存在"));
            return;
        }

        if (currentRoom != null) {
            roomManager.leaveRoom(this);
        }

        room.spectate(this);
        this.currentRoom = room;

        room.broadcastToPlayers(Message.of(Protocol.PLAYER_ENTER,
                "userId", String.valueOf(userId),
                "username", getUsername(),
                "type", "SPECTATOR"));

        send(room.buildGameStartMessage());

        // 回放已有落子
        for (Game.MoveRecord mr : room.getGame().getMoveHistory()) {
            send(Message.of(Protocol.MOVE_SYNC,
                    "x", String.valueOf(mr.x),
                    "y", String.valueOf(mr.y),
                    "color", String.valueOf(mr.color)));
        }

          send(Message.of(Protocol.TIME_SYNC,
                "remainingSeconds", room.getRemainingSeconds(),
                "currentPlayer", room.getGame().getCurrentPlayer()));

        log.info("玩家 {} 开始观战房间 {} (已回放 {} 步)", userId, roomId, room.getGame().getMoveHistory().size());
    }

    private void handleLeaveSpectate() {
        if (currentRoom == null) {
            send(Message.of(Protocol.ERROR, "msg", "不在房间中"));
            return;
        }

        if (!currentRoom.isObserver(this)) {
            send(Message.of(Protocol.ERROR, "msg", "不是观战者"));
            return;
        }

        roomManager.leaveRoom(this);
        log.info("玩家 {} 退出观战房间", userId);
    }

    // ==================== 排行榜 ====================

    private void handleRanking(Message msg) {
        String type = msg.getString("type");
        if (type == null || type.isEmpty()) {
            type = "GLOBAL";
        }

        List<RankDAO.RankInfo> ranking;
        if ("FRIENDS".equalsIgnoreCase(type)) {
            ranking = rankDAO.getFriendsRanking(userId);
        } else {
            ranking = rankDAO.getRanking(20);
        }

        JSONArray users = new JSONArray();
        for (var u : ranking) {
            JSONObject obj = new JSONObject();
            obj.put("rank", u.getRank());
            obj.put("userId", u.getUserId());
            obj.put("username", u.getUsername());
            obj.put("score", u.getScore());
            obj.put("winRate", u.getWinRate());
            obj.put("avatarUrl", u.getAvatar() != null ? u.getAvatar() : "");
            users.put(obj);
        }
        send(Message.builder(Protocol.RANKING_LIST)
                .set("type", type)
                .set("users", users)
                .build());
        log.info("Sent {} ranking to {}: {} users", type, socket.getPort(), ranking.size());
    }

    // ==================== 查询战绩 ====================

    private void handleQueryRecord(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        int page = msg.has("page") ? msg.getInt("page") : 0;
        int size = msg.has("size") ? msg.getInt("size") : 10;

        // 优先从 Redis 缓存读取对战记录（3天内的热数据）
        List<String> redisRecords = RedisManager.getInstance().getGameRecords(userId, page, size);
        if (!redisRecords.isEmpty()) {
            JSONArray recs = new JSONArray();
            for (String json : redisRecords) {
                JSONObject r = new JSONObject(json);
                // 旧 record（在 cacheGameRecordToRedis 加 avatar 字段前写入的）缺 player1Avatar/player2Avatar，
                // 这里实时从 MySQL 补全签名 URL，让回放页面能显示双方头像
                if (!r.has("player1Avatar") || r.optString("player1Avatar", "").isEmpty()) {
                    r.put("player1Avatar", resolveAvatarPresigned(r.optLong("player1Id", 0)));
                }
                if (!r.has("player2Avatar") || r.optString("player2Avatar", "").isEmpty()) {
                    r.put("player2Avatar", resolveAvatarPresigned(r.optLong("player2Id", 0)));
                }
                recs.put(r);
            }
            send(Message.builder("RECORD_LIST").set("records", recs).build());
            log.info("Sent record list from Redis to user {} ({} records)", userId, redisRecords.size());
            return;
        }

        // Redis 未命中则回退 MySQL 查询
        List<String[]> records = recordDAO.getHistory(userId, page, size);

        JSONArray recs = new JSONArray();
        for (String[] record : records) {
            long player1Id = Long.parseLong(record[1]);
            long player2Id = Long.parseLong(record[2]);
            long winnerId = record[3].equals("0") ? 0 : Long.parseLong(record[3]);

            UserDAO.UserInfo player1 = userDAO.getUserById(player1Id);
            UserDAO.UserInfo player2 = userDAO.getUserById(player2Id);

            String player1Name = player1 != null ? player1.getUsername() : "未知";
            String player2Name = player2 != null ? player2.getUsername() : (player2Id == -1 ? "AI" : "未知");

            // 查询双方头像 URL：MySQL 中 t_user.avatar_url 列保存的是 OSS 相对路径
            // （如 avatar/72_xxx.png），必须用 generatePresignedUrl 包成可访问的签名 URL。
            // 30 天有效期与 LOGIN_OK 保持一致，避免回放时头像过期。
            String player1AvatarPath = userDAO.getAvatar(player1Id);
            String player2AvatarPath = userDAO.getAvatar(player2Id);
            String player1Avatar = "";
            String player2Avatar = "";
            try {
                StorageManager sm = StorageManager.getInstance();
                if (sm != null && sm.getOssService() != null) {
                    if (player1AvatarPath != null && !player1AvatarPath.isEmpty()) {
                        player1Avatar = sm.getOssService().generatePresignedUrl(player1AvatarPath, 86400 * 30);
                    }
                    if (player2AvatarPath != null && !player2AvatarPath.isEmpty()) {
                        player2Avatar = sm.getOssService().generatePresignedUrl(player2AvatarPath, 86400 * 30);
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to generate presigned URL for record avatars (p1={}, p2={}): {}",
                        player1Id, player2Id, ex.getMessage());
            }

            String boardData = record.length > 8 ? record[8] : "";
            int p1ScoreChange = record.length > 9 ? Integer.parseInt(record[9]) : 0;
            int p2ScoreChange = record.length > 10 ? Integer.parseInt(record[10]) : 0;
            int userScoreChange = userId == player1Id ? p1ScoreChange : p2ScoreChange;

            int player1Score = player1 != null ? player1.getScore() : 0;
            int player2Score = player2 != null ? player2.getScore() : 0;

            JSONObject r = new JSONObject();
            r.put("recordId", record[0]);
            r.put("player1Id", player1Id);
            r.put("player1Name", player1Name);
            r.put("player1Score", player1Score);
            r.put("player1Avatar", player1Avatar != null ? player1Avatar : "");
            r.put("player2Id", player2Id);
            r.put("player2Name", player2Name);
            r.put("player2Score", player2Score);
            r.put("player2Avatar", player2Avatar != null ? player2Avatar : "");
            r.put("winnerId", winnerId);
            r.put("result", record[4]);
            r.put("moveCount", record[5]);
            r.put("duration", record[6]);
            r.put("createTime", record[7]);
            r.put("boardData", boardData);
            r.put("scoreChange", userScoreChange);
            recs.put(r);
        }
        send(Message.builder("RECORD_LIST").set("records", recs).build());
        log.info("Sent record list to user {} ({} records)", userId, records.size());
    }

    /**
     * 取用户头像的 30 天预签名 URL，userId 无效或 OSS 未启用时返回空串。
     * 用于从 Redis 老 record 补全 player1Avatar/player2Avatar 字段。
     */
    private String resolveAvatarPresigned(long userId) {
        if (userId <= 0) return "";
        try {
            String path = userDAO.getAvatar(userId);
            if (path == null || path.isEmpty()) return "";
            String lower = path.toLowerCase();
            if (path.contains("?") && (lower.startsWith("http://") || lower.startsWith("https://"))) {
                return path;
            }
            StorageManager sm = StorageManager.getInstance();
            if (sm == null || sm.getOssService() == null) return "";
            return sm.getOssService().generatePresignedUrl(path, 86400 * 30);
        } catch (Exception ex) {
            log.debug("resolveAvatarPresigned failed for userId={}: {}", userId, ex.getMessage());
            return "";
        }
    }

    /**
     * 根据 avatarPath（OSS 相对路径）生成 30 天预签名 URL。
     * OSS 未启用、路径为空或生成异常时均返回空串，调用方无需额外判断。
     */
    private String generateAvatarUrl(String avatarPath) {
        if (avatarPath == null || avatarPath.isEmpty()) return "";
        StorageManager sm = StorageManager.getInstance();
        if (sm == null || !sm.isEnabled() || sm.getOssService() == null) return "";
        try {
            return sm.getOssService().generatePresignedUrl(avatarPath, 86400 * 30);
        } catch (Exception e) {
            log.warn("Failed to generate avatar URL for {}: {}", avatarPath, e.getMessage());
            return "";
        }
    }

    // ==================== 悔棋请求 ====================

    private void handleRequestUndo() {
        if (currentRoom == null || currentRoom.getStatus() != Room.Status.PLAYING) {
            send(Message.of(Protocol.ERROR, "msg", "不在游戏中"));
            return;
        }

        if (currentRoom.isObserver(this)) {
            send(Message.of(Protocol.ERROR, "msg", "观战者不能请求悔棋"));
            return;
        }

        if (currentRoom.isAiMode()) {
            boolean undone = currentRoom.directUndo(userId);
            if (undone) {
                currentRoom.broadcastAll(Message.cmdOnly("UNDO_ACCEPTED"));
                log.info("Player {} undid move in AI mode", userId);
            } else {
                send(Message.of(Protocol.ERROR, "msg", "无法悔棋"));
            }
            return;
        }

        boolean success = currentRoom.requestUndo(userId);
        if (success) {
            currentRoom.broadcastToPlayersExcept(userId,
                Message.of("UNDO_REQUESTED",
                    "requesterId", String.valueOf(userId),
                    "requesterName", getUsername()));
            log.info("玩家 {} 请求悔棋", userId);
        } else {
            int undoCount = currentRoom.getUndoCount(userId);
            if (undoCount >= 3) {
                send(Message.of(Protocol.ERROR, "msg", "undo_limit_exceeded"));
            } else {
                send(Message.of(Protocol.ERROR, "msg", "无法请求悔棋"));
            }
        }
    }

    // ==================== 悔棋回复 ====================

    private void handleReplyUndo(Message msg) {
        if (currentRoom == null || currentRoom.getStatus() != Room.Status.PLAYING) {
            send(Message.of(Protocol.ERROR, "msg", "不在游戏中"));
            return;
        }

        if (currentRoom.isObserver(this)) {
            send(Message.of(Protocol.ERROR, "msg", "观战者不能回复悔棋"));
            return;
        }

        boolean accept = msg.getBoolean("accept");
        boolean success = currentRoom.replyUndo(userId, accept);
        if (success) {
            if (accept) {
                currentRoom.broadcastAll(Message.cmdOnly("UNDO_ACCEPTED"));
                log.info("玩家 {} 同意悔棋", userId);
            } else {
                currentRoom.broadcastAll(Message.cmdOnly("UNDO_REJECTED"));
                log.info("玩家 {} 拒绝悔棋", userId);
            }
        } else {
            send(Message.of(Protocol.ERROR, "msg", "无法回复悔棋请求"));
        }
    }

    // ==================== 求和请求 ====================

    private void handleRequestDraw() {
        if (currentRoom == null || currentRoom.getStatus() != Room.Status.PLAYING) {
            send(Message.of(Protocol.ERROR, "msg", "不在游戏中"));
            return;
        }

        if (currentRoom.isObserver(this)) {
            send(Message.of(Protocol.ERROR, "msg", "观战者不能请求求和"));
            return;
        }

        if (currentRoom.isAiMode()) {
            currentRoom.directDraw(userId);
            log.info("Player {} drew game in AI mode", userId);
            return;
        }

        boolean success = currentRoom.requestDraw(userId);
        if (success) {
            currentRoom.broadcastToPlayersExcept(userId,
                Message.of("DRAW_REQUESTED",
                    "requesterId", String.valueOf(userId),
                    "requesterName", getUsername()));
            log.info("玩家 {} 请求求和", userId);
        } else {
            int drawCount = currentRoom.getDrawCount(userId);
            if (drawCount >= 1) {
                send(Message.of(Protocol.ERROR, "msg", "draw_limit_exceeded"));
            } else {
                send(Message.of(Protocol.ERROR, "msg", "无法请求求和"));
            }
        }
    }

    // ==================== 求和回复 ====================

    private void handleReplyDraw(Message msg) {
        if (currentRoom == null || currentRoom.getStatus() != Room.Status.PLAYING) {
            send(Message.of(Protocol.ERROR, "msg", "不在游戏中"));
            return;
        }

        if (currentRoom.isObserver(this)) {
            send(Message.of(Protocol.ERROR, "msg", "观战者不能回复求和"));
            return;
        }

        boolean accept = msg.getBoolean("accept");
        boolean success = currentRoom.replyDraw(userId, accept);
        if (success) {
            if (accept) {
                log.info("玩家 {} 同意求和，游戏平局结束", userId);
            } else {
                currentRoom.broadcastAll(Message.cmdOnly("DRAW_REJECTED"));
                log.info("玩家 {} 拒绝求和", userId);
            }
        } else {
            send(Message.of(Protocol.ERROR, "msg", "无法回复求和请求"));
        }
    }

    // ==================== 再来一局请求 ====================

    private void handleRequestRematch() {
        if (currentRoom == null || currentRoom.getStatus() != Room.Status.FINISHED) {
            send(Message.of(Protocol.ERROR, "msg", "当前不在已结束的对局中"));
            return;
        }

        if (currentRoom.isObserver(this)) {
            send(Message.of(Protocol.ERROR, "msg", "观战者不能请求再来一局"));
            return;
        }

        boolean success = currentRoom.requestRematch(userId);
        if (success) {
            currentRoom.broadcastToPlayersExcept(userId,
                Message.of(Protocol.REMATCH_REQUESTED,
                    "requesterId", String.valueOf(userId),
                    "requesterName", getUsername()));
            log.info("玩家 {} 请求再来一局", userId);
        } else {
            send(Message.of(Protocol.ERROR, "msg", "无法请求再来一局"));
        }
    }

    // ==================== 再来一局回复 ====================

    private void handleReplyRematch(Message msg) {
        if (currentRoom == null || currentRoom.getStatus() != Room.Status.FINISHED) {
            send(Message.of(Protocol.ERROR, "msg", "当前不在已结束的对局中"));
            return;
        }

        if (currentRoom.isObserver(this)) {
            send(Message.of(Protocol.ERROR, "msg", "观战者不能回复再来一局"));
            return;
        }

        boolean accept = msg.getBoolean("accept");
        long result = currentRoom.replyRematch(userId, accept);
        if (result < 0) {
            send(Message.of(Protocol.ERROR, "msg", "无法回复再来一局请求"));
            log.warn("replyRematch failed: userId={}, accept={}", userId, accept);
            return;
        }

        if (accept) {
            // 同意：resetAndRestartGame 已广播 GAME_START，再补充 REMATCH_ACCEPTED 告知客户端
            currentRoom.broadcastAll(Message.cmdOnly(Protocol.REMATCH_ACCEPTED));
            log.info("玩家 {} 同意再来一局", userId);
        } else {
            // 拒绝：通知请求方
            ClientHandler requester = currentRoom.findPlayerById(result);
            if (requester != null) {
                requester.send(Message.of(Protocol.REMATCH_REJECTED, "replierName", getUsername()));
            }
            // 也通知自己（关闭弹窗并返回大厅）
            send(Message.of(Protocol.REMATCH_REJECTED, "replierName", getUsername()));
            log.info("玩家 {} 拒绝再来一局", userId);
        }
    }

    // ==================== 断线重连 ====================

    private void handleReconnect(Message msg) {
        String token = msg.getString("token");

        synchronized (sessionManager) {
            SessionManager.Session session = sessionManager.getSessionByToken(token);

            if (session == null) {
                send(Message.of("RECONNECT_FAIL", "msg", "会话不存在或已过期"));
                log.warn("Reconnect failed: session not found for token {}",
                        token != null ? token.substring(0, Math.min(8, token.length())) : "null");
                return;
            }

            SessionManager.Session currentSession = sessionManager.getSessionByUserId(session.userId);
            if (currentSession != null && !currentSession.token.equals(token)) {
                send(Message.of("RECONNECT_FAIL", "msg", "您的账号在其他设备登录"));
                log.warn("Reconnect failed: userId {} has new session, old token {} rejected",
                        session.userId, token.substring(0, 8));
                return;
            }

            if (session.isExpired()) {
                sessionManager.removeSession(token);
                send(Message.of("RECONNECT_FAIL", "msg", "会话已过期"));
                log.warn("Reconnect failed: session expired for userId {}", session.userId);
                return;
            }

            this.userId = session.userId;
            this.username = session.username;
            this.role = session.role;
            this.sessionToken = token;
            this.authenticated = true;

            sessionManager.updateSessionHandler(session.userId, this);
            RedisManager.getInstance().setOnline(session.userId, token);
        }

        Room room = roomManager.findRoomByUserId(userId);
        if (room != null) {
            boolean reconnected = room.reconnect(this);
            if (reconnected) {
                this.currentRoom = room;
                sessionManager.updateSessionRoom(userId, room.getRoomId());

                int rank = 0;
                RankDAO.RankInfo rankInfo = rankDAO.getRankByUserId(userId);
                if (rankInfo != null) {
                    rank = rankInfo.getRank();
                }

                send(Message.builder("RECONNECT_OK")
                        .set("userId", String.valueOf(userId))
                        .set("username", username)
                        .set("score", String.valueOf(sessionManager.getSessionByUserId(userId).score))
                        .set("winCount", String.valueOf(sessionManager.getSessionByUserId(userId).winCount))
                        .set("loseCount", String.valueOf(sessionManager.getSessionByUserId(userId).loseCount))
                        .set("drawCount", String.valueOf(sessionManager.getSessionByUserId(userId).drawCount))
                        .set("rank", String.valueOf(rank))
                        .set("role", role)
                        .set("roomId", room.getRoomId())
                        .set("roomName", room.getRoomName())
                        .set("players", room.getPlayerCount() + "/" + 2)
                        .build());

                room.broadcastToPlayers(Message.of(Protocol.PLAYER_ENTER,
                        "userId", String.valueOf(userId),
                        "username", username));

                log.info("玩家 {} 断线重连成功，进入房间 {}", userId, room.getRoomId());
            } else {
                send(Message.of("RECONNECT_FAIL", "msg", "无法重新加入房间"));
                log.warn("Reconnect failed: cannot rejoin room {} for userId {}", room.getRoomId(), userId);
            }
        } else {
            int rank = 0;
            RankDAO.RankInfo rankInfo = rankDAO.getRankByUserId(userId);
            if (rankInfo != null) {
                rank = rankInfo.getRank();
            }

            send(Message.builder("RECONNECT_OK")
                    .set("userId", String.valueOf(userId))
                    .set("username", username)
                    .set("score", String.valueOf(sessionManager.getSessionByUserId(userId).score))
                    .set("winCount", String.valueOf(sessionManager.getSessionByUserId(userId).winCount))
                    .set("loseCount", String.valueOf(sessionManager.getSessionByUserId(userId).loseCount))
                    .set("drawCount", String.valueOf(sessionManager.getSessionByUserId(userId).drawCount))
                    .set("rank", String.valueOf(rank))
                    .set("role", role)
                    .set("roomId", "")
                    .set("roomName", "")
                    .set("players", "")
                    .build());

            log.info("玩家 {} 断线重连成功，不在房间中", userId);
        }
    }

    // ==================== 好友系统 ====================

    private void handleAddFriend(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        long friendUserId = msg.getLong("friendUserId");
        if (friendUserId == 0) {
            send(Message.of("ADD_FRIEND_FAIL", "msg", "好友ID无效"));
            return;
        }
        if (friendUserId == userId) {
            send(Message.of("ADD_FRIEND_FAIL", "msg", "不能添加自己为好友"));
            return;
        }

        if (!userDAO.existsActiveUser(friendUserId)) {
            send(Message.of("ADD_FRIEND_FAIL", "msg", "目标用户不存在或账号异常"));
            log.warn("玩家 {} 发送好友请求失败: 目标用户 {} 不存在或状态异常", userId, friendUserId);
            return;
        }

        String message = msg.has("message") ? msg.getString("message") : "";

        if (friendDAO.isFriend(userId, friendUserId)) {
            send(Message.of("ADD_FRIEND_FAIL", "msg", "你们已经是好友了"));
            log.warn("玩家 {} 发送好友请求失败: 与 {} 已是好友", userId, friendUserId);
            return;
        }

        if (friendDAO.hasPendingRequest(userId, friendUserId)) {
            send(Message.of("ADD_FRIEND_FAIL", "msg", "已向对方发送过好友申请，请勿重复发送"));
            log.warn("玩家 {} 发送好友请求失败: 对 {} 的请求仍在pending", userId, friendUserId);
            return;
        }

        // 检查对方是否已向当前用户发送了好友请求
        if (friendDAO.hasPendingRequest(friendUserId, userId)) {
            send(Message.of("ADD_FRIEND_FAIL", "msg", "对方已向您发送了好友申请，请在好友请求中处理"));
            log.warn("玩家 {} 发送好友请求失败: 对方 {} 已有pending请求发向自己", userId, friendUserId);
            return;
        }

        boolean success = friendDAO.sendFriendRequest(userId, friendUserId, message);
        if (success) {
            send(Message.cmdOnly("ADD_FRIEND_OK"));
            log.info("玩家 {} 发送好友请求给 {}", userId, friendUserId);
        } else {
            send(Message.of("ADD_FRIEND_FAIL", "msg", "发送失败，请稍后重试"));
            log.warn("玩家 {} 发送好友请求失败: {}", userId, friendUserId);
        }
    }

    private void handleAcceptFriend(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        long requestId = msg.getLong("requestId");
        boolean success = friendDAO.acceptFriendRequest(requestId, userId);
        if (success) {
            send(Message.cmdOnly("ACCEPT_FRIEND_OK"));
            log.info("玩家 {} 接受好友请求: {}", userId, requestId);
        } else {
            send(Message.of("ACCEPT_FRIEND_FAIL", "msg", "接受失败"));
            log.warn("玩家 {} 接受好友请求失败: {}", userId, requestId);
        }
    }

    private void handleRejectFriend(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        long requestId = msg.getLong("requestId");
        boolean success = friendDAO.rejectFriendRequest(requestId, userId);
        if (success) {
            send(Message.cmdOnly("REJECT_FRIEND_OK"));
            log.info("玩家 {} 拒绝好友请求: {}", userId, requestId);
        } else {
            send(Message.of("REJECT_FRIEND_FAIL", "msg", "拒绝失败"));
            log.warn("玩家 {} 拒绝好友请求失败: {}", userId, requestId);
        }
    }

    private void handleDeleteFriend(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        long friendUserId = msg.getLong("friendUserId");
        boolean success = friendDAO.deleteFriend(userId, friendUserId);
        if (success) {
            send(Message.cmdOnly("DELETE_FRIEND_OK"));
            log.info("玩家 {} 删除好友: {}", userId, friendUserId);
        } else {
            send(Message.of("DELETE_FRIEND_FAIL", "msg", "删除失败"));
            log.warn("玩家 {} 删除好友失败: {}", userId, friendUserId);
        }
    }

    private void handleFriendList() {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        List<FriendDAO.FriendInfo> friends = friendDAO.getFriendList(userId);
        JSONArray arr = new JSONArray();
        for (FriendDAO.FriendInfo f : friends) {
            JSONObject obj = new JSONObject();
            obj.put("friendUserId", String.valueOf(f.getFriendUserId()));
            obj.put("friendUsername", f.getFriendUsername());
            obj.put("friendScore", String.valueOf(f.getFriendScore()));
            obj.put("online", f.isOnline());
            obj.put("avatarUrl", f.getAvatar() != null ? f.getAvatar() : "");
            arr.put(obj);
        }
        send(Message.builder("FRIEND_LIST").set("friends", arr).build());
        log.info("发送好友列表给玩家 {}: {} 个好友", userId, friends.size());
    }

    private void handleFriendRequests() {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        List<FriendDAO.FriendRequestInfo> requests = friendDAO.getPendingRequests(userId);
        JSONArray arr = new JSONArray();
        for (var r : requests) {
            JSONObject obj = new JSONObject();
            obj.put("requestId", r.getRequestId());
            obj.put("fromUserId", r.getFromUserId());
            obj.put("fromUsername", r.getFromUsername());
            obj.put("fromScore", r.getFromScore());
            obj.put("message", r.getMessage() != null ? r.getMessage() : "");
            arr.put(obj);
        }
        send(Message.builder("FRIEND_REQUEST_LIST").set("requests", arr).build());
        log.info("发送好友请求列表给玩家 {}: {} 个请求", userId, requests.size());
    }

    // ==================== 棋局保存 ====================

    private void handleSaveGame(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        if (currentRoom == null || !currentRoom.isGameStarted()) {
            send(Message.of("SAVE_GAME_FAIL", "msg", "不在游戏房间或游戏未开始"));
            return;
        }

        String saveName = msg.has("saveName") ? msg.getString("saveName") : "未命名棋局";
        if (saveName.length() > 100) {
            saveName = saveName.substring(0, 100);
        }

        long saveId = gameSaveDAO.saveGame(userId, null, saveName, currentRoom.getGame());
        if (saveId > 0) {
            send(Message.of("SAVE_GAME_OK", "saveId", String.valueOf(saveId), "saveName", saveName));
            log.info("玩家 {} 保存棋局成功: saveId={}", userId, saveId);
        } else {
            send(Message.of("SAVE_GAME_FAIL", "msg", "保存失败"));
            log.warn("玩家 {} 保存棋局失败", userId);
        }
    }

    private void handleLoadGame(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        long saveId = msg.getLong("saveId");
        GameSaveDAO.GameSaveInfo saveInfo = gameSaveDAO.getGameSave(saveId);
        if (saveInfo == null) {
            send(Message.of("LOAD_GAME_FAIL", "msg", "存档不存在"));
            return;
        }

        if (saveInfo.getUserId() != userId) {
            send(Message.of("LOAD_GAME_FAIL", "msg", "无权加载此存档"));
            return;
        }

        send(Message.builder("LOAD_GAME_OK")
                .set("saveId", String.valueOf(saveInfo.getSaveId()))
                .set("saveName", saveInfo.getSaveName())
                .set("boardData", saveInfo.getBoardData())
                .set("currentPlayer", String.valueOf(saveInfo.getCurrentPlayer()))
                .set("gameStatus", saveInfo.getGameStatus())
                .build());
        log.info("玩家 {} 加载棋局成功: saveId={}", userId, saveId);
    }

    private void handleDeleteSave(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        long saveId = msg.getLong("saveId");
        boolean success = gameSaveDAO.deleteGameSave(saveId, userId);
        if (success) {
            send(Message.cmdOnly("DELETE_SAVE_OK"));
            log.info("玩家 {} 删除棋局存档成功: saveId={}", userId, saveId);
        } else {
            send(Message.of("DELETE_SAVE_FAIL", "msg", "删除失败"));
            log.warn("玩家 {} 删除棋局存档失败: saveId={}", userId, saveId);
        }
    }

    private void handleSaveList(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        int page = msg.has("page") ? msg.getInt("page") : 0;
        int size = msg.has("size") ? msg.getInt("size") : 10;

        List<GameSaveDAO.GameSaveInfo> saves = gameSaveDAO.getGameSavesByUserId(userId, page, size);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"cmd\":\"SAVE_LIST\",\"data\":{\"saves\":[");
        for (int i = 0; i < saves.size(); i++) {
            if (i > 0) sb.append(",");
            var s = saves.get(i);
            sb.append("{\"saveId\":\"").append(s.getSaveId())
              .append("\",\"saveName\":\"").append(s.getSaveName())
              .append("\",\"gameStatus\":\"").append(s.getGameStatus())
              .append("\",\"createTime\":\"").append(s.getCreateTime()).append("\"}");
        }
        sb.append("]}}");
        send(sb.toString());
        log.info("发送棋局存档列表给玩家 {}: {} 个存档", userId, saves.size());
    }

    // ==================== 聊天历史 ====================

    private void handleChatHistory(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        if (currentRoom == null) {
            send(Message.of(Protocol.ERROR, "msg", "不在房间中"));
            return;
        }

        int page = msg.has("page") ? msg.getInt("page") : 0;
        int size = msg.has("size") ? msg.getInt("size") : 20;

        List<ChatDAO.ChatMessage> messages = chatDAO.getMessagesByRoomId(currentRoom.getRoomId(), page, size);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"cmd\":\"CHAT_HISTORY\",\"data\":{\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            var m = messages.get(i);
            sb.append("{\"messageId\":\"").append(m.getMessageId())
              .append("\",\"userId\":\"").append(m.getUserId())
              .append("\",\"username\":\"").append(m.getUsername())
              .append("\",\"content\":\"").append(m.getContent())
              .append("\",\"createTime\":\"").append(m.getCreateTime()).append("\"}");
        }
        sb.append("]}}");
        send(sb.toString());
        log.info("发送聊天历史给玩家 {}: {} 条消息", userId, messages.size());
    }

    // ==================== 观战记录 ====================

    private void handleWatcherHistory(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        int page = msg.has("page") ? msg.getInt("page") : 0;
        int size = msg.has("size") ? msg.getInt("size") : 10;

        List<RoomWatcherDAO.RoomWatcher> watchers = roomWatcherDAO.getWatcherHistory(userId, page, size);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"cmd\":\"WATCHER_HISTORY\",\"data\":{\"watchers\":[");
        for (int i = 0; i < watchers.size(); i++) {
            if (i > 0) sb.append(",");
            var w = watchers.get(i);
            sb.append("{\"roomId\":\"").append(w.getRoomId())
              .append("\",\"joinTime\":\"").append(w.getJoinTime())
              .append("\",\"leaveTime\":\"").append(w.getLeaveTime() != null ? w.getLeaveTime() : "").append("\"}");
        }
        sb.append("]}}");
        send(sb.toString());
        log.info("发送观战记录给玩家 {}: {} 条", userId, watchers.size());
    }

    // ==================== 心跳 ====================

    private void handleHeartbeat() {
        if (authenticated && sessionToken != null) {
            RedisManager.getInstance().updateSessionActivity(sessionToken);
        }
        send(Message.cmdOnly(Protocol.HEARTBEAT_OK));
        log.trace("Heartbeat from userId={}", userId);
    }

    // ==================== 退出登录 ====================

    private void handleLogout() {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        if (currentRoom != null) {
            roomManager.leaveRoom(this);
            log.info("玩家 {} 退出登录时离开房间 {}", userId, currentRoom.getRoomId());
        }

        sessionManager.removeSessionByUserId(userId);

        send(Message.cmdOnly(Protocol.LOGOUT_OK));
        log.info("玩家 {} 退出登录成功", userId);

        this.userId = 0;
        this.username = null;
        this.authenticated = false;
        this.currentRoom = null;
    }

    // ==================== 管理员权限检查 ====================

    private boolean requireAdmin() {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return false;
        }
        if (!"ADMIN".equals(role)) {
            send(Message.of(Protocol.ERROR, "msg", "权限不足"));
            log.warn("Admin permission denied for userId={}, role={}", userId, role);
            return false;
        }
        return true;
    }

    // ==================== 封禁用户 ====================

    private void handleBanUser(Message msg) {
        if (!requireAdmin()) return;
        long targetUserId = msg.getLong("userId");
        if (targetUserId == 0) {
            send(Message.of(Protocol.ERROR, "msg", "用户ID无效"));
            return;
        }
        if (targetUserId == userId) {
            send(Message.of(Protocol.ERROR, "msg", "不能封禁自己"));
            return;
        }

        // 如果被封禁用户在线，断开其连接
        SessionManager.Session targetSession = sessionManager.getSessionByUserId(targetUserId);
        if (targetSession != null && targetSession.handler != null) {
            targetSession.handler.send(Message.of("BANNED", "msg", "您的账号已被管理员封禁"));
            try { targetSession.handler.socket.close(); } catch (IOException e) { log.error("关闭被封禁用户连接失败", e); }
            sessionManager.removeSessionByUserId(targetUserId);
        }

        boolean success = userDAO.banUser(targetUserId);
        if (success) {
            send(Message.of("BAN_USER_OK", "userId", String.valueOf(targetUserId)));
            log.info("Admin {} banned user {}", userId, targetUserId);
        } else {
            send(Message.of(Protocol.ERROR, "msg", "封禁失败（可能已是管理员）"));
        }
    }

    // ==================== 解封用户 ====================

    private void handleUnbanUser(Message msg) {
        if (!requireAdmin()) return;
        long targetUserId = msg.getLong("userId");
        if (targetUserId == 0) {
            send(Message.of(Protocol.ERROR, "msg", "用户ID无效"));
            return;
        }

        boolean success = userDAO.unbanUser(targetUserId);
        if (success) {
            send(Message.of("UNBAN_USER_OK", "userId", String.valueOf(targetUserId)));
            log.info("Admin {} unbanned user {}", userId, targetUserId);
        } else {
            send(Message.of(Protocol.ERROR, "msg", "解封失败"));
        }
    }

    // ==================== 强制关闭房间 ====================

    private void handleForceCloseRoom(Message msg) {
        if (!requireAdmin()) return;
        String roomId = msg.getString("roomId");
        if (roomId == null || roomId.isEmpty()) {
            send(Message.of(Protocol.ERROR, "msg", "房间ID无效"));
            return;
        }

        Room targetRoom = roomManager.getRoom(roomId);
        if (targetRoom == null) {
            send(Message.of(Protocol.ERROR, "msg", "房间不存在"));
            return;
        }

        // 通知房间内所有玩家
        targetRoom.broadcastAll(Message.of("FORCE_CLOSE_ROOM", 
                "roomId", roomId, 
                "msg", "房间已被管理员关闭"));

        // 让所有玩家离开房间
        for (ClientHandler player : targetRoom.getPlayers()) {
            try {
                roomManager.leaveRoom(player);
            } catch (Exception e) {
                log.error("强制离开房间失败: userId={}", player.getUserId(), e);
            }
        }
        for (ClientHandler observer : new java.util.ArrayList<>(targetRoom.getObservers())) {
            try {
                roomManager.leaveRoom(observer);
            } catch (Exception e) {
                log.error("强制离开房间失败(observer): userId={}", observer.getUserId(), e);
            }
        }

        roomManager.removeRoom(roomId);
        send(Message.of("FORCE_CLOSE_ROOM_OK", "roomId", roomId));
        log.info("Admin {} force closed room {}", userId, roomId);
    }

    // ==================== 用户列表 ====================

    private void handleUserList(Message msg) {
        if (!requireAdmin()) return;
        int page = msg.has("page") ? msg.getInt("page") : 0;
        int size = msg.has("size") ? msg.getInt("size") : 20;
        String keyword = msg.has("keyword") ? msg.getString("keyword") : null;

        List<UserDAO.UserInfo> users;
        if (keyword != null && !keyword.trim().isEmpty()) {
            users = userDAO.searchUsers(keyword.trim(), page, size);
        } else {
            users = userDAO.getAllUsers(page, size);
        }

        JSONArray arr = new JSONArray();
        for (UserDAO.UserInfo u : users) {
            JSONObject obj = new JSONObject();
            obj.put("userId", String.valueOf(u.getUserId()));
            obj.put("username", u.getUsername());
            obj.put("score", u.getScore());
            obj.put("winCount", u.getWinCount());
            obj.put("loseCount", u.getLoseCount());
            obj.put("drawCount", u.getDrawCount());
            obj.put("role", u.getRole());
            obj.put("status", u.getStatus());
            arr.put(obj);
        }

        send(Message.builder("USER_LIST")
                .set("users", arr)
                .set("total", String.valueOf(userDAO.getUserCount()))
                .set("page", String.valueOf(page))
                .build());
        log.info("Admin {} requested user list: page={}, size={}, keyword={}", userId, page, size, keyword);
    }

    // ==================== 管理员统计 ====================

    private void handleAdminStats() {
        if (!requireAdmin()) return;
        int totalUsers = userDAO.getUserCount();
        int activeRooms = roomManager.getActiveRoomCount();
        int onlineUsers = sessionManager.getActiveSessionCount();

        send(Message.builder("ADMIN_STATS")
                .set("totalUsers", String.valueOf(totalUsers))
                .set("activeRooms", String.valueOf(activeRooms))
                .set("onlineUsers", String.valueOf(onlineUsers))
                .build());
        log.info("Admin {} requested stats: users={}, rooms={}, online={}", 
                userId, totalUsers, activeRooms, onlineUsers);
    }

    // ==================== 修改用户角色 ====================

    private void handleUpdateUserRole(Message msg) {
        if (!requireAdmin()) return;
        long targetUserId = msg.getLong("userId");
        String newRole = msg.getString("role");
        if (targetUserId == 0 || newRole == null) {
            send(Message.of(Protocol.ERROR, "msg", "参数无效"));
            return;
        }
        if (targetUserId == userId && "PLAYER".equals(newRole)) {
            send(Message.of(Protocol.ERROR, "msg", "不能降级自己的管理员角色"));
            return;
        }

        boolean success = userDAO.updateUserRole(targetUserId, newRole);
        if (success) {
            // 通知被修改角色的用户（如果在线）
            SessionManager.Session targetSession = sessionManager.getSessionByUserId(targetUserId);
            if (targetSession != null) {
                targetSession.role = newRole;
                if (targetSession.handler != null) {
                    targetSession.handler.role = newRole;
                    targetSession.handler.send(Message.of("ROLE_UPDATED", "role", newRole));
                }
            }
            send(Message.of("UPDATE_USER_ROLE_OK", "userId", String.valueOf(targetUserId), "role", newRole));
            log.info("Admin {} updated role of user {} to {}", userId, targetUserId, newRole);
        } else {
            send(Message.of(Protocol.ERROR, "msg", "修改角色失败"));
        }
    }

    // ==================== 头像上传 ====================

    private void handleUploadAvatar(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        if (!StorageManager.getInstance().isEnabled()) {
            send(Message.of("AVATAR_UPLOADED", "msg", "存储服务未启用"));
            return;
        }

        String base64Data = msg.getString("data");
        if (base64Data == null || base64Data.isEmpty()) {
            send(Message.of("AVATAR_UPLOADED", "msg", "图片数据为空"));
            return;
        }

        try {
            String filePath = StorageManager.getInstance().getOssService().uploadAvatar(userId, base64Data);
            
            boolean updated = userDAO.updateAvatar(userId, filePath);
            if (updated) {
                String presignedUrl = StorageManager.getInstance().getOssService().generatePresignedUrl(filePath, 86400 * 30);
                send(Message.builder("AVATAR_UPLOADED")
                        .set("url", presignedUrl)
                        .set("msg", "上传成功")
                        .build());
                log.info("玩家 {} 上传头像成功: {}", userId, presignedUrl);
            } else {
                send(Message.of("AVATAR_UPLOADED", "msg", "更新用户信息失败"));
            }
        } catch (Exception e) {
            log.error("玩家 {} 上传头像失败", userId, e);
            send(Message.of("AVATAR_UPLOADED", "msg", "上传失败: " + e.getMessage()));
        }
    }

    private void handleGetAvatar(Message msg) {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        long targetUserId = msg.has("userId") ? msg.getLong("userId") : userId;
        
        String filePath = userDAO.getAvatar(targetUserId);
        String presignedUrl = generateAvatarUrl(filePath);
        send(Message.builder("AVATAR_DATA")
                .set("userId", String.valueOf(targetUserId))
                .set("url", presignedUrl)
                .build());
    }

    private void handleDeleteAvatar() {
        if (!authenticated) {
            send(Message.of(Protocol.ERROR, "msg", "未登录"));
            return;
        }

        if (!StorageManager.getInstance().isEnabled()) {
            send(Message.of("AVATAR_DELETED", "msg", "存储服务未启用"));
            return;
        }

        try {
            String avatarPath = userDAO.getAvatar(userId);
            if (avatarPath != null && !avatarPath.isEmpty()) {
                StorageManager.getInstance().getOssService().deleteFile(avatarPath);
            }
            
            boolean updated = userDAO.updateAvatar(userId, "");
            if (updated) {
                send(Message.of("AVATAR_DELETED", "msg", "删除成功"));
                log.info("玩家 {} 删除头像成功", userId);
            } else {
                send(Message.of("AVATAR_DELETED", "msg", "删除失败"));
            }
        } catch (Exception e) {
            log.error("玩家 {} 删除头像失败", userId, e);
            send(Message.of("AVATAR_DELETED", "msg", "删除失败: " + e.getMessage()));
        }
    }

    private String getBucketName() {
        return DBUtil.getConfig().getProperty("oss.bucket", "gomatch-storage");
    }

    private String getEndpoint() {
        String endpoint = DBUtil.getConfig().getProperty("oss.endpoint", "");
        if (endpoint.startsWith("https://")) {
            return endpoint.substring(8);
        }
        return endpoint;
    }

    // ==================== 资源清理 ====================

    private void cleanup() {
        log.info("Client disconnecting: {} (auth={}, room={})", 
                socket.getInetAddress(), authenticated, 
                currentRoom != null ? currentRoom.getRoomId() : "none");

        if (currentRoom != null) {
            // cleanupDisconnect 在 Room 的 synchronized 锁内原子判断状态，
            // 避免 getStatus() 与实际操作之间的 TOCTOU 竞态。
            // 返回 true 表示房间处于 WAITING，需要通过 RoomManager 清理残留。
            boolean wasWaiting = currentRoom.cleanupDisconnect(this);
            if (wasWaiting) {
                roomManager.leaveRoom(this);
            } else {
                log.info("玩家 {} 从房间 {} 断开连接", userId, currentRoom.getRoomId());
            }
        }

        if (authenticated && userId > 0) {
            roomManager.cancelMatch(userId);
            
            SessionManager.Session session = sessionManager.getSessionByUserId(userId);
            if (session != null && session.handler == this) {
                sessionManager.updateSessionHandler(userId, null);
                RedisManager.getInstance().setOffline(userId);
                log.info("用户 {} 断开连接，保留会话等待重连", userId);
            } else if (session != null && session.handler != this) {
                log.info("用户 {} 的连接已被新设备替换，跳过清理", userId);
            }
            
            configService.flushAsync(userId);
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

        com.gomatch.server.ServerMain.decrementConnections();
        log.info("Active connections decremented. Current: {}", com.gomatch.server.ServerMain.getActiveConnections());
    }

    // ==================== Getter ====================

    public Socket getSocket() { return socket; }
    public long getUserId() { return userId; }
    public String getUsername() {
        if (username != null) return username;
        // username 未设置时回退到 UserDAO 查询，避免 buildGameStartMessage 等场景出现 "unknown" 字符串
        if (userId > 0) {
            try {
                UserDAO.UserInfo info = userDAO.getUserById(userId);
                if (info != null && info.getUsername() != null) {
                    this.username = info.getUsername();
                    return this.username;
                }
            } catch (Exception e) {
                log.warn("getUsername fallback query failed for userId={}: {}", userId, e.getMessage());
            }
        }
        return "unknown";
    }
    public String getRole() { return role; }
    public boolean isAuthenticated() { return authenticated; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
    public Room getCurrentRoom() { return currentRoom; }
    public boolean isConnected() { return socket != null && !socket.isClosed() && socket.isConnected(); }

    public void setUserId(long userId) {
        this.userId = userId;
        this.authenticated = true;
    }

    public void setCurrentRoom(Room currentRoom) {
        this.currentRoom = currentRoom;
    }
}





