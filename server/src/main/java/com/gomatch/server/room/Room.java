package com.gomatch.server.room;

import com.gomatch.server.handler.ClientHandler;
import com.gomatch.server.game.Game;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Room {
    public enum Status { WAITING, PLAYING, FINISHED }

    private final String roomId;
    private final String roomName;
    private final ClientHandler owner;
    private Status status = Status.WAITING;
    private final Game game = new Game();
    private final List<ClientHandler> players = new CopyOnWriteArrayList<>();
    private final List<ClientHandler> observers = new CopyOnWriteArrayList<>();

    public Room(String roomId, String roomName, ClientHandler owner) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.owner = owner;
    }

    public String getRoomId() { return roomId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<ClientHandler> getPlayers() { return players; }

    public synchronized boolean join(ClientHandler h) {
        if (players.size() >= 2) return false;
        players.add(h);
        return true;
    }

    public synchronized void leave(ClientHandler h) {
        players.remove(h);
        observers.remove(h);
    }
}
