package com.gomatch.database.repository;

import com.gomatch.database.entity.UserMusic;

import java.util.List;

public interface UserMusicRepository {
    UserMusic findById(Long musicId);
    List<UserMusic> findByUserId(Long userId);
    List<UserMusic> findByUserIdAndMusicType(Long userId, String musicType);
    UserMusic findDefaultMusic(Long userId);
    UserMusic findByUserIdAndFileName(Long userId, String fileName);
    UserMusic save(UserMusic music);
    int update(UserMusic music);
    int updateDefaultMusic(Long userId, Long musicId);
    int clearDefaultMusic(Long userId);
    int deleteById(Long musicId);
    int deleteByUserId(Long userId);
    boolean existsById(Long musicId);
    boolean existsByUserIdAndFileName(Long userId, String fileName);
    int countByUserId(Long userId);
}
