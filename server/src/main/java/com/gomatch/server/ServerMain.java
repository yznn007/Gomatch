package com.gomatch.server;

import com.gomatch.server.handler.ClientHandler;
import com.gomatch.server.room.RoomManager;
import com.gomatch.server.dao.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);
    private static final int PORT = 8888;

    public static void main(String[] args) throws Exception {
        log.info("Gomatch Server starting on port {}...", PORT);
        DBUtil.init();
        RoomManager.getInstance();
        try (ServerSocket ss = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = ss.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        }
    }
}
