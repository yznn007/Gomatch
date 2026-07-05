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
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.*;

public class RepositoryTest {
    private static final Logger logger = LoggerFactory.getLogger(RepositoryTest.class);
    
    private static UserRepository userRepository;
    private static RoomRepository roomRepository;
    private static GameRecordRepository gameRecordRepository;

    @BeforeClass
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
        assertNotNull("User should be saved", savedUser.getUserId());
        logger.info("Saved user: {}", savedUser);
        
        User foundUser = userRepository.findById(savedUser.getUserId());
        assertNotNull("User should be found", foundUser);
        assertEquals("Username should match", uniqueUsername, foundUser.getUsername());
        
        boolean exists = userRepository.existsByUsername(uniqueUsername);
        assertTrue("User should exist", exists);
        
        int updated = userRepository.updateScore(savedUser.getUserId(), 100);
        assertEquals("Score should be updated", 1, updated);
        
        User updatedUser = userRepository.findById(savedUser.getUserId());
        assertEquals("Score should be 1100", Integer.valueOf(1100), updatedUser.getScore());
        
        List<User> allUsers = userRepository.findAll();
        assertTrue("Should have at least one user", allUsers.size() > 0);
        
        logger.info("UserRepository test completed");
    }

    @Test
    public void testRoomRepository() {
        logger.info("Testing RoomRepository...");
        
        Room room = new Room("Test Room", 1L);
        
        Room savedRoom = roomRepository.save(room);
        assertNotNull("Room should be saved", savedRoom.getRoomId());
        logger.info("Saved room: {}", savedRoom);
        
        Room foundRoom = roomRepository.findById(savedRoom.getRoomId());
        assertNotNull("Room should be found", foundRoom);
        assertEquals("Room name should match", "Test Room", foundRoom.getRoomName());
        
        int updated = roomRepository.updatePlayerCount(savedRoom.getRoomId(), 2);
        assertEquals("Player count should be updated", 1, updated);
        
        Room updatedRoom = roomRepository.findById(savedRoom.getRoomId());
        assertEquals("Player count should be 2", Integer.valueOf(2), updatedRoom.getCurrentPlayers());
        
        List<Room> waitingRooms = roomRepository.findByStatus("waiting");
        assertTrue("Should have waiting rooms", waitingRooms.size() > 0);
        
        logger.info("RoomRepository test completed");
    }

    @Test
    public void testGameRecordRepository() {
        logger.info("Testing GameRecordRepository...");
        
        GameRecord record = new GameRecord(1L, 2L, "PVP");
        record.setBoardData("[]");
        
        GameRecord savedRecord = gameRecordRepository.save(record);
        assertNotNull("Game record should be saved", savedRecord.getRecordId());
        logger.info("Saved game record: {}", savedRecord);
        
        GameRecord foundRecord = gameRecordRepository.findById(savedRecord.getRecordId());
        assertNotNull("Game record should be found", foundRecord);
        
        int updated = gameRecordRepository.updateResult(savedRecord.getRecordId(), 1L, "win");
        assertEquals("Result should be updated", 1, updated);
        
        GameRecord updatedRecord = gameRecordRepository.findById(savedRecord.getRecordId());
        assertEquals("Winner should be 1", Long.valueOf(1), updatedRecord.getWinnerId());
        assertEquals("Result should be win", "win", updatedRecord.getResult());
        
        logger.info("GameRecordRepository test completed");
    }
}