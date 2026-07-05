package com.gomatch.database.repository;

import com.gomatch.database.entity.Friend;

import java.util.List;

public interface FriendRepository {
    Friend findById(Long friendId);
    List<Friend> findByUserId(Long userId);
    List<Friend> findByFriendUserId(Long friendUserId);
    Friend findByUserIdAndFriendUserId(Long userId, Long friendUserId);
    List<Friend> findByRelationType(String relationType);
    List<Friend> findAll();
    Friend save(Friend friend);
    int updateRemark(Long friendId, String remark);
    int deleteById(Long friendId);
    int deleteByUserIdAndFriendUserId(Long userId, Long friendUserId);
    int countByUserId(Long userId);
    boolean existsByUserIdAndFriendUserId(Long userId, Long friendUserId);
}