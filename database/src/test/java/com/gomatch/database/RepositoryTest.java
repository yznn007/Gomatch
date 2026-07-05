package com.gomatch.database;

import com.gomatch.database.entity.User;
import com.gomatch.database.entity.Room;
import com.gomatch.database.entity.GameRecord;
import com.gomatch.database.repository.UserRepository;
import com.gomatch.database.repository.RoomRepository;
import com.gomatch.database.repository.GameRecordRepository;
import com.gomatch.database.repository.impl.UserRepositoryImpl;
import com.gomatch.database.repository.impl.RoomRepositoryImpl;
import com.gomatch.database.repository.impl.GameRecordRepositoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RepositoryTest {
    private static final Logger logger = LoggerFactory.getLogger(RepositoryTest.class);
    
    private static UserRepository userRepository;
    private static RoomRepository roomRepository;
    private static GameRecordRepository gameRecordRepository;

    @BeforeAll
    public static void setUp() {
        logger.info("Initializing database connection...");
        DatabaseBootstrap.main(new String[0]);
        
        userRepository = new UserRepositoryImpl();
        roomRepository = new RoomRepositoryImpl();
        gameRecordRepository = new GameRecordRepositoryImpl();
        
        logger.info("Repositories initialized");
    }

    @Test
    public void testUserRepository() {
        logger.info("Testing UserRepository...");
        
        String uniqueUsername = "testuser_" + System.currentTimeMillis();
        User user = new User(uniqueUsername, "[redacted-credential]", uniqueUsername + "@example.com");
        user.setScore(1000);
        
        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getUserId(), "User should be saved");
        logger.info("Saved user: {}", savedUser);
        
        User foundUser = userRepository.findById(savedUser.getUserId());
        assertNotNull(foundUser, "User should be found");
        assertEquals(uniqueUsername, foundUser.getUsername(), "Username should match");
        
        boolean exists = userRepository.existsByUsername(uniqueUsername);
        assertTrue(exists, "User should exist");
        
        int updated = userRepository.updateScore(savedUser.getUserId(), 100);
        assertEquals(1, updated, "Score should be updated");
        
        User updatedUser = userRepository.findById(savedUser.getUserId());
        assertEquals(Integer.valueOf(1100), updatedUser.getScore(), "Score should be 1100");
        
        List<User> allUsers = userRepository.findAll();
        assertTrue(allUsers.size() > 0, "Should have at least one user");
        
        logger.info("UserRepository test completed");
    }

    @Test
    public void testRoomRepository() {
        logger.info("Testing RoomRepository...");
        
        Room room = new Room("Test Room", 1L);
        
        Room savedRoom = roomRepository.save(room);
        assertNotNull(savedRoom.getRoomId(), "Room should be saved");
        logger.info("Saved room: {}", savedRoom);
        
        Room foundRoom = roomRepository.findById(savedRoom.getRoomId());
        assertNotNull(foundRoom, "Room should be found");
        assertEquals("Test Room", foundRoom.getRoomName(), "Room name should match");
        
        int updated = roomRepository.updatePlayerCount(savedRoom.getRoomId(), 2);
        assertEquals(1, updated, "Player count should be updated");
        
        Room updatedRoom = roomRepository.findById(savedRoom.getRoomId());
        assertEquals(Integer.valueOf(2), updatedRoom.getCurrentPlayers(), "Player count should be 2");
        
        List<Room> waitingRooms = roomRepository.findByStatus("waiting");
        assertTrue(waitingRooms.size() > 0, "Should have waiting rooms");
        
        logger.info("RoomRepository test completed");
    }

    @Test
    public void testGameRecordRepository() {
        logger.info("Testing GameRecordRepository...");
        
        GameRecord record = new GameRecord(1L, 2L, "PVP");
        record.setBoardData("[]");
        
        GameRecord savedRecord = gameRecordRepository.save(record);
        assertNotNull(savedRecord.getRecordId(), "Game record should be saved");
        logger.info("Saved game record: {}", savedRecord);
        
        GameRecord foundRecord = gameRecordRepository.findById(savedRecord.getRecordId());
        assertNotNull(foundRecord, "Game record should be found");
        
        int updated = gameRecordRepository.updateResult(savedRecord.getRecordId(), 1L, "win");
        assertEquals(1, updated, "Result should be updated");
        
        GameRecord updatedRecord = gameRecordRepository.findById(savedRecord.getRecordId());
        assertEquals(Long.valueOf(1), updatedRecord.getWinnerId(), "Winner should be 1");
        assertEquals("win", updatedRecord.getResult(), "Result should be win");
        
        logger.info("GameRecordRepository test completed");
    }
}