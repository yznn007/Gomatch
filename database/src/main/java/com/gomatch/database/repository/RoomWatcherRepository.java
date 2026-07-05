package com.gomatch.database.repository;

import com.gomatch.database.entity.RoomWatcher;

import java.util.Date;
import java.util.List;

public interface RoomWatcherRepository {
    RoomWatcher findById(Long watcherId);
    List<RoomWatcher> findByRoomId(Long roomId);
    List<RoomWatcher> findByUserId(Long userId);
    RoomWatcher findByRoomIdAndUserId(Long roomId, Long userId);
    List<RoomWatcher> findAll();
    RoomWatcher save(RoomWatcher watcher);
    int updateLeaveTime(Long watcherId, Date leaveTime);
    int deleteById(Long watcherId);
    int deleteByRoomId(Long roomId);
    int countByRoomId(Long roomId);
}