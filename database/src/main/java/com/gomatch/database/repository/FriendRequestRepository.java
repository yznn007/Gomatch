package com.gomatch.database.repository;

import com.gomatch.database.entity.FriendRequest;

import java.util.Date;
import java.util.List;

public interface FriendRequestRepository {
    FriendRequest findById(Long requestId);
    List<FriendRequest> findByFromUserId(Long fromUserId);
    List<FriendRequest> findByToUserId(Long toUserId);
    FriendRequest findByFromUserIdAndToUserId(Long fromUserId, Long toUserId);
    List<FriendRequest> findByStatus(String status);
    List<FriendRequest> findAll();
    FriendRequest save(FriendRequest request);
    int updateStatus(Long requestId, String status);
    int updateRespondTime(Long requestId, Date respondTime);
    int deleteById(Long requestId);
    int countByToUserId(Long toUserId);
    boolean existsByFromUserIdAndToUserId(Long fromUserId, Long toUserId);
}