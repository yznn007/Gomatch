package com.gomatch.database.repository;

import com.gomatch.database.entity.Rank;

import java.util.List;

public interface RankRepository {
    Rank findById(Long rankId);
    Rank findByUserId(Long userId);
    List<Rank> findTopNRanks(int limit);
    List<Rank> findAll();
    Rank save(Rank rank);
    int update(Rank rank);
    int updateScore(Long userId, int score);
    int updateRank(Long userId, int rank);
    int updateWinRate(Long userId, double winRate);
    int updateAiWinRate(Long userId, double aiWinRate);
    int deleteById(Long rankId);
    boolean existsByUserId(Long userId);
    int count();
}