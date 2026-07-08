package com.gomatch.client.network;

import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ServerConnector {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean running;
    private Thread receiveThread;
    private final List<NetworkListener> listeners = new CopyOnWriteArrayList<>();
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long RECONNECT_DELAY_SEC = 5;
    private static final long HEARTBEAT_INTERVAL_SEC = 30;
    private static final long HEARTBEAT_TIMEOUT_SEC = 90;
    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "connector-scheduler");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> heartbeatFuture;
    private String lastHost;
    private int lastPort;
    private final AtomicLong lastServerResponseTime = new AtomicLong(System.currentTimeMillis());

    public void connect(String host, int port) throws IOException {
        this.lastHost = host;
        this.lastPort = port;
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        lastServerResponseTime.set(System.currentTimeMillis());
        startReceiveThread();
        startHeartbeat();
    }

    // ==================== 发送 ====================

    /** 发送 JSON 消息 */
    public void send(Message msg) {
        if (writer != null) {
            writer.println(msg.toJson());
        }
    }

    /** 发送仅含命令的消息（认输等） */
    public void send(String cmd) {
        send(Message.cmdOnly(cmd));
    }

    // ==================== 监听器 ====================

    public void addListener(NetworkListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(NetworkListener listener) {
        listeners.remove(listener);
    }

    /** 切换页面前调用，清除所有旧 Controller 的监听，避免消息乱投。 */
    public void clearListeners() {
        listeners.clear();
    }

    public void disconnect() {
        running = false;
        stopHeartbeat();
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        listeners.clear();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        lastHost = null;
        lastPort = 0;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // ==================== 接收线程 ====================

    private void startReceiveThread() {
        running = true;
        receiveThread = new Thread(() -> {
            while (running && isConnected()) {
                try {
                    String line = reader.readLine();
                    if (line == null) break;
                    Message msg = Message.fromJson(line);
                    lastServerResponseTime.set(System.currentTimeMillis());

                    // 内部拦截重连认证响应，不转发给业务 Controller
                    if ("RECONNECT_OK".equals(msg.getCmd())) {
                        handleReconnectOk(msg);
                        continue;
                    }
                    if ("RECONNECT_FAIL".equals(msg.getCmd())) {
                        handleReconnectFail(msg);
                        continue;
                    }

                    Platform.runLater(() -> {
                        for (NetworkListener l : listeners) {
                            l.onMessage(msg);
                        }
                    });
                } catch (IOException e) {
                    break;
                }
            }
            if (!running) {
                return;
            }
            // 意外断线：在后台线程重连，避免 Thread.sleep 卡死 FX 线程
            Thread reconnectThread = new Thread(() -> {
                boolean reconnected = lastHost != null && tryReconnect();
                if (!reconnected) {
                    Platform.runLater(() -> {
                        for (NetworkListener l : listeners) {
                            l.onMessage(Message.cmdOnly("DISCONNECT"));
                        }
                    });
                }
            });
            reconnectThread.setDaemon(true);
            reconnectThread.start();
        });
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    /** 重连认证成功：更新本地用户状态 */
    private void handleReconnectOk(Message msg) {
        com.gomatch.client.model.User user = com.gomatch.client.AppConfig.getCurrentUser();
        if (user != null) {
            user.setUserId(msg.getLong("userId"));
            user.setUsername(msg.getString("username"));
            user.setScore(msg.getInt("score"));
            user.setWinCount(msg.getInt("winCount"));
            user.setLoseCount(msg.getInt("loseCount"));
            user.setDrawCount(msg.getInt("drawCount"));
            user.setRank(msg.getInt("rank"));
        }
        // 如果在房间中，服务器会额外下发 roomId/roomName 等，
        // 但当前 Controller 已通过 clearListeners 清除，
        // 重连后需要重新进入页面才能感知。这里暂不处理。
    }

    /** 重连认证失败：断开连接并触发全局断线事件 */
    private void handleReconnectFail(Message msg) {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        Platform.runLater(() -> {
            for (NetworkListener l : listeners) {
                l.onMessage(Message.cmdOnly("DISCONNECT"));
            }
        });
    }

    private boolean tryReconnect() {
        for (int i = 1; i <= MAX_RECONNECT_ATTEMPTS; i++) {
            try {
                Thread.sleep(RECONNECT_DELAY_SEC * 1000);
                if (socket != null) { try { socket.close(); } catch (IOException ignored) {} }
                socket = new Socket(lastHost, lastPort);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                // 重连后发送 RECONNECT 消息恢复认证状态
                String token = com.gomatch.client.AppConfig.getAuthToken();
                if (token != null && !token.isEmpty()) {
                    writer.println(Message.of("RECONNECT", "token", token).toJson());
                }

                running = true;
                lastServerResponseTime.set(System.currentTimeMillis());
                startReceiveThread();
                startHeartbeat();
                return true;
            } catch (Exception e) {
                // 重连失败，继续尝试
            }
        }
        return false;
    }

    // ==================== 心跳机制 ====================

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatFuture = scheduler.scheduleAtFixedRate(() -> {
            if (!running || writer == null) return;
            // 检查上次服务端响应是否超时
            long elapsed = System.currentTimeMillis() - lastServerResponseTime.get();
            if (elapsed > HEARTBEAT_TIMEOUT_SEC * 1000) {
                // 心跳超时：主动断开触发重连
                try {
                    running = false;
                    if (socket != null) socket.close();
                } catch (IOException ignored) {}
                return;
            }
            // 发送心跳包
            send(Message.cmdOnly(Protocol.HEARTBEAT));
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }
    }

}
