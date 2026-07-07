package com.gomatch.server;

import com.gomatch.server.handler.ClientHandler;
import com.gomatch.server.room.RoomManager;
import com.gomatch.server.dao.DBUtil;
import com.gomatch.server.dao.RankDAO;
import com.gomatch.server.dao.RoomDAO;
import com.gomatch.server.redis.RedisManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gomatch 游戏服务器主类
 * 监听 8888 端口，接收 TCP 连接并为每个连接创建 ClientHandler 处理
 */
public class ServerMain {
    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);
    private static final int PORT = 8888;
    private static final int MAX_CONNECTIONS = 100;

    private static final AtomicInteger connectionCount = new AtomicInteger(0);
    private static volatile boolean running = true;
    private static final RankDAO rankDAO = new RankDAO();
    private static final RoomDAO roomDAO = new RoomDAO();
    private static ScheduledExecutorService rankRefreshScheduler;
    private static ScheduledExecutorService roomPersistenceScheduler;

    public static void main(String[] args) {
        log.info("=== Gomatch Server Starting ===");

        // 初始化数据库连接
        DBUtil.init();

        // 初始化房间管理器
        RoomManager.getInstance();

        // 初始化 Redis
        RedisManager.getInstance().init();

        // 初始化排行榜物化表
        initRankMaterializedView();

        // 初始化房间持久化定时任务
        initRoomPersistence();

        // 添加 JVM 关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(ServerMain::shutdown, "shutdown-hook"));

        // 启动 TCP 监听
        startListening();
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
        // 初始化时同步一次
        roomDAO.syncAllRooms(RoomManager.getInstance().getAllRooms());

        roomPersistenceScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "room-persistence");
            t.setDaemon(true);
            return t;
        });

        // 每1分钟同步房间状态到数据库
        roomPersistenceScheduler.scheduleAtFixedRate(() -> {
            try {
                roomDAO.syncAllRooms(RoomManager.getInstance().getAllRooms());
                roomDAO.cleanupFinishedRooms(30); // 清理30分钟前结束的房间
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
            serverSocket = new ServerSocket(PORT);
            log.info("Server listening on port {}", PORT);
            log.info("Max connections: {}", MAX_CONNECTIONS);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    if (connectionCount.get() >= MAX_CONNECTIONS) {
                        log.warn("Connection limit reached ({}/{})", MAX_CONNECTIONS, connectionCount.get());
                        clientSocket.close();
                        continue;
                    }

                    connectionCount.incrementAndGet();
                    log.info("New client connected: {} (active: {}/{})",
                            clientSocket.getInetAddress(),
                            connectionCount.get(), MAX_CONNECTIONS);

                    ClientHandler handler = new ClientHandler(clientSocket);
                    Thread thread = new Thread(handler, "client-" + clientSocket.getPort());
                    thread.setDaemon(false);
                    thread.start();

                } catch (IOException e) {
                    if (running) {
                        log.error("Error accepting connection", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Server failed to start on port {}", PORT, e);
            System.exit(1);
        } finally {
            closeServerSocket(serverSocket);
        }
    }

    private static void closeServerSocket(ServerSocket ss) {
        if (ss != null && !ss.isClosed()) {
            try {
                ss.close();
                log.info("Server socket closed");
            } catch (IOException e) {
                log.error("Error closing server socket", e);
            }
        }
    }

    public static void shutdown() {
        log.info("Shutting down server...");
        running = false;

        // 关闭房间持久化调度器
        if (roomPersistenceScheduler != null) {
            roomPersistenceScheduler.shutdown();
            try {
                if (!roomPersistenceScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    roomPersistenceScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                roomPersistenceScheduler.shutdownNow();
            }
            log.info("Room persistence scheduler stopped");
        }

        // 关闭排行榜刷新调度器
        if (rankRefreshScheduler != null) {
            rankRefreshScheduler.shutdown();
            try {
                if (!rankRefreshScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    rankRefreshScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                rankRefreshScheduler.shutdownNow();
            }
            log.info("Rank refresh scheduler stopped");
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 关闭 Redis 连接
        RedisManager.getInstance().close();

        // 关闭数据库连接
        DBUtil.close();

        log.info("Server stopped. Final connections: {}", connectionCount.get());
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