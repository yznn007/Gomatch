package com.gomatch.database.repository;

import com.gomatch.database.entity.Room;

import java.util.List;

public interface RoomRepository {
    Room findById(Long roomId);
    List<Room> findByStatus(String status);
    List<Room> findByHostId(Long hostId);
    List<Room> findByName(String roomName);
    List<Room> findByAllowWatch(Boolean allowWatch);
    List<Room> findAll();
    Room save(Room room);
    int update(Room room);
    int updateStatus(Long roomId, String status);
    int updatePlayerCount(Long roomId, int count);
    int updateWatcherCount(Long roomId, int count);
    int deleteById(Long roomId);
    int countByStatus(String status);
}