package com.gomatch.server.handler;

import com.gomatch.common.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = Protocol.parse(line);
                dispatch(parts);
            }
        } catch (IOException e) {
            log.warn("Client disconnected: {}", socket.getInetAddress());
        } finally {
            cleanup();
        }
    }

    private void dispatch(String[] parts) {
        String cmd = parts[0];
        switch (cmd) {
            case Protocol.LOGIN -> handleLogin(parts);
            case Protocol.REGISTER -> handleRegister(parts);
            case Protocol.CREATE_ROOM -> handleCreateRoom(parts);
            case Protocol.JOIN_ROOM -> handleJoinRoom(parts);
            case Protocol.LEAVE_ROOM -> handleLeaveRoom();
            case Protocol.MOVE -> handleMove(parts);
            case Protocol.CHAT -> handleChat(parts);
            case Protocol.SURRENDER -> handleSurrender();
            default -> send(Protocol.ERROR, "Unknown command");
        }
    }

    private void send(String... parts) {
        writer.println(Protocol.build(parts));
    }

    private void handleLogin(String[] parts) {}
    private void handleRegister(String[] parts) {}
    private void handleCreateRoom(String[] parts) {}
    private void handleJoinRoom(String[] parts) {}
    private void handleLeaveRoom() {}
    private void handleMove(String[] parts) {}
    private void handleChat(String[] parts) {}
    private void handleSurrender() {}

    private void cleanup() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
