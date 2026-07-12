package com.gomatch.server;

import com.gomatch.server.dao.DBUtil;
import com.gomatch.server.dao.RankDAO;
import com.gomatch.server.dao.RoomDAO;
import com.gomatch.server.handler.ClientHandler;
import com.gomatch.server.redis.RedisManager;
import com.gomatch.server.room.RoomManager;
import com.gomatch.server.security.ConnectionGuard;
import com.gomatch.server.service.ConfigService;
import com.gomatch.server.security.GuardedSocket;
import com.gomatch.server.security.ProtocolRequestGuard;
import com.gomatch.server.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Gomatch game server entry point. */
public class ServerMain {
    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);
    private static final int DEFAULT_PORT = 8888;
    private static int port = DEFAULT_PORT;
    private static final int MAX_CONNECTIONS = 100;
    private static final int MAX_CONNECTIONS_PER_ADDRESS = 10;

    private static final AtomicInteger connectionCount = new AtomicInteger(0);
    private static final ConnectionGuard connectionGuard =
            new ConnectionGuard(MAX_CONNECTIONS, MAX_CONNECTIONS_PER_ADDRESS);
    private static final ProtocolRequestGuard requestGuard = new ProtocolRequestGuard();
    private static volatile boolean running = true;
    private static final RankDAO rankDAO = new RankDAO();
    private static final RoomDAO roomDAO = new RoomDAO();
    private static ScheduledExecutorService rankRefreshScheduler;
    private static ScheduledExecutorService roomPersistenceScheduler;

    public static void main(String[] args) {
        log.info("=== Gomatch Server Starting ===");
        parseArgs(args);
        DBUtil.init();
        RoomManager.getInstance();
        RedisManager.getInstance().init();
        initRankMaterializedView();
        initRoomPersistence();
        Runtime.getRuntime().addShutdownHook(new Thread(ServerMain::shutdown, "shutdown-hook"));
        startListening();
    }

    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("-p".equals(args[i]) || "--port".equals(args[i])) {
                if (i + 1 < args.length) {
                    try {
                        port = Integer.parseInt(args[i + 1]);
                        if (port < 1 || port > 65535) {
                            log.warn("Invalid port {} specified, using default port {}", port, DEFAULT_PORT);
                            port = DEFAULT_PORT;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Invalid port number format: {}, using default port {}", args[i + 1], DEFAULT_PORT);
                    }
                }
            }
        }
        log.info("Server port configured: {}", port);
    }

    private static void initRankMaterializedView() {
        rankDAO.refreshAllRanks();
        rankDAO.recalculateRankOrder();
        rankRefreshScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rank-refresh");
            t.setDaemon(true);
            return t;
        });
        rankRefreshScheduler.scheduleAtFixedRate(() -> {
            try {
                rankDAO.refreshAllRanks();
                rankDAO.recalculateRankOrder();
                log.info("Ranking materialized view refreshed");
            } catch (Exception e) {
                log.error("Rank refresh error", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
        log.info("Rank materialized view initialized with auto-refresh every 5 minutes");
    }

    private static void initRoomPersistence() {
        roomDAO.syncAllRooms(RoomManager.getInstance().getAllRooms());
        roomPersistenceScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "room-persistence");
            t.setDaemon(true);
            return t;
        });
        roomPersistenceScheduler.scheduleAtFixedRate(() -> {
            try {
                roomDAO.syncAllRooms(RoomManager.getInstance().getAllRooms());
                roomDAO.cleanupFinishedRooms(30);
                log.info("Room persistence task completed: active rooms={}",
                        RoomManager.getInstance().getActiveRoomCount());
            } catch (Exception e) {
                log.error("Room persistence error", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
        log.info("Room persistence initialized with auto-sync every 1 minute");
    }

    private static void startListening() {
        ServerSocket serverSocket = null;
        try {
            java.net.InetSocketAddress addr = new java.net.InetSocketAddress(port);
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(addr);
            log.info("Server listening on port {}", port);
            log.info("Connection limits: total={}, perAddress={}",
                    MAX_CONNECTIONS, MAX_CONNECTIONS_PER_ADDRESS);

            while (running) {
                try {
                    Socket rawSocket = serverSocket.accept();
                    InetAddress address = rawSocket.getInetAddress();
                    if (!connectionGuard.tryAcquire(address)) {
                        log.warn("Connection rejected by limit: {}", address);
                        rawSocket.close();
                        continue;
                    }

                    try {
                        GuardedSocket clientSocket = new GuardedSocket(rawSocket, requestGuard,
                                () -> connectionGuard.release(address));
                        connectionCount.incrementAndGet();
                        log.info("New client connected: {} (active: {}/{})",
                                address, connectionCount.get(), MAX_CONNECTIONS);
                        ClientHandler handler = new ClientHandler(clientSocket);
                        Thread thread = new Thread(handler, "client-" + clientSocket.getPort());
                        thread.setDaemon(false);
                        thread.start();
                    } catch (IOException | RuntimeException setupError) {
                        connectionGuard.release(address);
                        try {
                            rawSocket.close();
                        } catch (IOException closeError) {
                            setupError.addSuppressed(closeError);
                        }
                        throw setupError;
                    }
                } catch (IOException e) {
                    if (running) {
                        log.error("Error accepting connection", e);
                    }
                }
            }
        } catch (java.net.BindException e) {
            log.error("Port {} is already in use!", port);
            log.error("Please stop the existing server process or use a different port with:");
            log.error("  mvn exec:java -Dexec.args=\"--port <new_port>\"");
            System.exit(1);
        } catch (IOException e) {
            log.error("Server failed to start on port {}", port, e);
            System.exit(1);
        } finally {
            closeServerSocket(serverSocket);
        }
    }

    private static void closeServerSocket(ServerSocket serverSocket) {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                log.info("Server socket closed");
            } catch (IOException e) {
                log.error("Error closing server socket", e);
            }
        }
    }

    public static void shutdown() {
        log.info("Shutting down server...");
        running = false;
        shutdownScheduler(roomPersistenceScheduler, "Room persistence");
        shutdownScheduler(rankRefreshScheduler, "Rank refresh");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ConfigService.getInstance().shutdown();
        RedisManager.getInstance().close();
        DBUtil.close();
        log.info("Server stopped. Final connections: {}", connectionCount.get());
    }

    private static void shutdownScheduler(ScheduledExecutorService scheduler, String name) {
        if (scheduler == null) {
            return;
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("{} scheduler stopped", name);
    }

    public static int getActiveConnections() {
        return connectionCount.get();
    }

    public static void decrementConnections() {
        int count = connectionCount.decrementAndGet();
        if (count < 0) {
            connectionCount.set(0);
        }
    }
}
