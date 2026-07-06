package com.gomatch.client.network;

import com.gomatch.common.Protocol;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerConnector {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean running;
    private Thread receiveThread;
    private final List<NetworkListener> listeners = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService heartbeatTimer;
    private volatile long lastResponseTime = System.currentTimeMillis();
    private static final long HEARTBEAT_INTERVAL_SEC = 30;
    private static final long HEARTBEAT_TIMEOUT_SEC = 90;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long RECONNECT_DELAY_SEC = 5;
    private String lastHost;
    private int lastPort;

    public void connect(String host, int port) throws IOException {
        this.lastHost = host;
        this.lastPort = port;
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        lastResponseTime = System.currentTimeMillis();
        startReceiveThread();
        startHeartbeat();
    }

    public void send(String... parts) {
        if (writer != null) {
            writer.println(Protocol.build(parts));
        }
    }

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
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private void startReceiveThread() {
        running = true;
        receiveThread = new Thread(() -> {
            while (running && isConnected()) {
                try {
                    String line = reader.readLine();
                    if (line == null) break;
                    lastResponseTime = System.currentTimeMillis();
                    String[] parts = Protocol.parse(line);
                    String cmd = parts[0];
                    String[] args = Arrays.copyOfRange(parts, 1, parts.length);
                    Platform.runLater(() -> {
                        for (NetworkListener l : listeners) {
                            l.onMessage(cmd, args);
                        }
                    });
                } catch (IOException e) {
                    break;
                }
            }
            Platform.runLater(() -> {
                // 尝试重连
                if (lastHost != null && tryReconnect()) {
                    return; // 重连成功，不发 DISCONNECT
                }
                for (NetworkListener l : listeners) {
                    l.onMessage("DISCONNECT", new String[0]);
                }
            });
        });
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTimer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatTimer.scheduleAtFixedRate(() -> {
            if (!isConnected()) return;
            // 检查超时
            long elapsed = (System.currentTimeMillis() - lastResponseTime) / 1000;
            if (elapsed > HEARTBEAT_TIMEOUT_SEC) {
                // 超时未收到任何响应，判定断线
                running = false;
                try { if (socket != null) socket.close(); } catch (IOException ignored) {}
                return;
            }
            // 发送心跳
            send(Protocol.HEARTBEAT);
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatTimer != null && !heartbeatTimer.isShutdown()) {
            heartbeatTimer.shutdownNow();
            heartbeatTimer = null;
        }
    }

    private boolean tryReconnect() {
        for (int i = 1; i <= MAX_RECONNECT_ATTEMPTS; i++) {
            try {
                Thread.sleep(RECONNECT_DELAY_SEC * 1000);
                stopHeartbeat();
                if (socket != null) { try { socket.close(); } catch (IOException ignored) {} }
                socket = new Socket(lastHost, lastPort);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                lastResponseTime = System.currentTimeMillis();
                running = true;
                startReceiveThread();
                startHeartbeat();
                return true;
            } catch (Exception e) {
                // 重连失败，继续尝试
            }
        }
        return false;
    }
}
