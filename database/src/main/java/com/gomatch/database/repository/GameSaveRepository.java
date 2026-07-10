package com.gomatch.database.repository;

import com.gomatch.database.entity.GameSave;

import java.util.List;

public interface GameSaveRepository {
    GameSave findById(Long saveId);
    GameSave findByFileUri(String fileUri);
    List<GameSave> findByUserId(Long userId);
    List<GameSave> findByRecordId(Long recordId);
    List<GameSave> findAll();
    GameSave save(GameSave save);
    int update(GameSave save);
    int updateChecksum(Long saveId, String checksum);
    int updateFileSize(Long saveId, Integer fileSize);
    int deleteById(Long saveId);
    int deleteByFileUri(String fileUri);
    int countByUserId(Long userId);
}