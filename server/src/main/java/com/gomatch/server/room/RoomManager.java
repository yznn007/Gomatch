package com.gomatch.server.room;

import com.gomatch.server.handler.ClientHandler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private static final RoomManager instance = new RoomManager();
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    private RoomManager() {}

    public static RoomManager getInstance() { return instance; }

    public Room createRoom(String name, ClientHandler owner) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Room room = new Room(id, name, owner);
        rooms.put(id, room);
        return room;
    }

    public Room getRoom(String roomId) { return rooms.get(roomId); }

    public Map<String, Room> getRooms() { return rooms; }

    public void removeRoom(String roomId) { rooms.remove(roomId); }
}
