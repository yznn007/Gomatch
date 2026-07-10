package com.gomatch.database.repository;

import com.gomatch.database.entity.GameRecord;

import java.util.List;

public interface GameRecordRepository {
    GameRecord findById(Long recordId);
    List<GameRecord> findByPlayer1Id(Long player1Id);
    List<GameRecord> findByPlayer2Id(Long player2Id);
    List<GameRecord> findByPlayerId(Long playerId);
    List<GameRecord> findByGameType(String gameType);
    List<GameRecord> findByWinnerId(Long winnerId);
    List<GameRecord> findAll();
    GameRecord save(GameRecord record);
    int update(GameRecord record);
    int updateResult(Long recordId, Long winnerId, String result);
    int updateBoardData(Long recordId, String boardData);
    int updateDuration(Long recordId, Integer duration);
    int updateMoveCount(Long recordId, Integer moveCount);
    int deleteById(Long recordId);
    int countByPlayerId(Long playerId);
    int countByGameType(String gameType);
}