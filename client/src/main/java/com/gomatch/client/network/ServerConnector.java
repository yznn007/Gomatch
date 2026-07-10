package com.gomatch.client.network;

import com.gomatch.client.AppConfig;
import com.gomatch.client.model.User;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
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
            Executors.newScheduledThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "connector-scheduler");
                thread.setDaemon(true);
                return thread;
            });
    private ScheduledFuture<?> heartbeatFuture;
    private String lastHost;
    private int lastPort;
    private final AtomicLong lastServerResponseTime = new AtomicLong(System.currentTimeMillis());

    public void connect(String host, int port) throws IOException {
        lastHost = host;
        lastPort = port;
        openSocket(host, port);
        lastServerResponseTime.set(System.currentTimeMillis());
        startReceiveThread();
        startHeartbeat();
    }

    private void openSocket(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
    }

    public void send(Message message) {
        if (writer != null) {
            writer.println(message.toJson());
        }
    }

    public void send(String command) {
        send(Message.cmdOnly(command));
    }

    public void addListener(NetworkListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(NetworkListener listener) {
        listeners.remove(listener);
    }

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
        closeSocket();
        lastHost = null;
        lastPort = 0;
    }

    private void closeSocket() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
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
                    if (line == null) {
                        break;
                    }
                    Optional<Message> parsed = InboundMessageParser.parse(line);
                    if (parsed.isEmpty()) {
                        continue;
                    }
                    Message message = parsed.get();
                    lastServerResponseTime.set(System.currentTimeMillis());
                    if ("RECONNECT_OK".equals(message.getCmd())) {
                        handleReconnectOk(message);
                        continue;
                    }
                    if ("RECONNECT_FAIL".equals(message.getCmd())) {
                        handleReconnectFail();
                        continue;
                    }
                    Platform.runLater(() -> listeners.forEach(listener -> listener.onMessage(message)));
                } catch (IOException error) {
                    break;
                }
            }
            if (running) {
                startReconnectWorker();
            }
        }, "server-receiver");
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    private void startReconnectWorker() {
        Thread reconnectThread = new Thread(() -> {
            boolean reconnected = lastHost != null && tryReconnect();
            if (!reconnected) {
                Platform.runLater(() -> listeners.forEach(
                        listener -> listener.onMessage(Message.cmdOnly("DISCONNECT"))));
            }
        }, "server-reconnect");
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    private void handleReconnectOk(Message message) {
        User user = AppConfig.getCurrentUser();
        if (user != null) {
            user.setUserId(message.getLong("userId"));
            user.setUsername(message.getString("username"));
            user.setScore(message.getInt("score"));
            user.setWinCount(message.getInt("winCount"));
            user.setLoseCount(message.getInt("loseCount"));
            user.setDrawCount(message.getInt("drawCount"));
            user.setRank(message.getInt("rank"));
            user.setRole(message.getString("role"));
        }
    }

    private void handleReconnectFail() {
        running = false;
        closeSocket();
        Platform.runLater(() -> listeners.forEach(
                listener -> listener.onMessage(Message.cmdOnly("DISCONNECT"))));
    }

    private boolean tryReconnect() {
        for (int attempt = 1; attempt <= MAX_RECONNECT_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(RECONNECT_DELAY_SEC * 1000);
                closeSocket();
                openSocket(lastHost, lastPort);
                String token = AppConfig.getAuthToken();
                if (token != null && !token.isEmpty()) {
                    writer.println(Message.of(Protocol.RECONNECT, "token", token).toJson());
                }
                running = true;
                lastServerResponseTime.set(System.currentTimeMillis());
                startReceiveThread();
                startHeartbeat();
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatFuture = scheduler.scheduleAtFixedRate(() -> {
            if (!running || writer == null) {
                return;
            }
            long elapsed = System.currentTimeMillis() - lastServerResponseTime.get();
            if (elapsed > HEARTBEAT_TIMEOUT_SEC * 1000) {
                running = false;
                closeSocket();
                return;
            }
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
