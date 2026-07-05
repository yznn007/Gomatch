package com.gomatch.database.repository;

import com.gomatch.database.entity.User;

import java.util.List;

public interface UserRepository {
    User findById(Long userId);
    User findByUsername(String username);
    User findByEmail(String email);
    List<User> findByRole(String role);
    List<User> findByStatus(String status);
    List<User> findByRoleAndStatus(String role, String status);
    List<User> findAll();
    User save(User user);
    int update(User user);
    int updateRole(Long userId, String role);
    int updateStatus(Long userId, String status);
    int updateScore(Long userId, int delta);
    int updateWinCount(Long userId, int delta);
    int updateLoseCount(Long userId, int delta);
    int updateDrawCount(Long userId, int delta);
    int updateAiWinCount(Long userId, int delta);
    int updateAiLoseCount(Long userId, int delta);
    int deleteById(Long userId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    int countByRole(String role);
    int countByStatus(String status);
}