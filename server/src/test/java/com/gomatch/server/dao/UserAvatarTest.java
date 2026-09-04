package com.gomatch.server.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserAvatarTest {

    private UserDAO userDAO;
    private long testUserId;

    @BeforeEach
    void setup() {
        if (DBUtil.getConfigProps() == null) {
            DBUtil.init();
        }
        userDAO = new UserDAO();
        testUserId = createTestUser();
        System.out.println("Created test user: userId=" + testUserId);
    }

    @AfterEach
    void cleanup() {
        deleteTestUser(testUserId);
        System.out.println("Deleted test user: userId=" + testUserId);
    }

    private long createTestUser() {
        String username = "test_" + System.currentTimeMillis() % 10000000;
        String email = "test-email-" + username;
        
        try (java.sql.Connection conn = DBUtil.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO t_user (username, password, email) VALUES (?, ?, ?)",
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, "test123");
            ps.setString(3, email);
            ps.executeUpdate();
            try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to create test user: " + e.getMessage());
        }
        return 0;
    }

    private void deleteTestUser(long userId) {
        if (userId <= 0) return;
        try (java.sql.Connection conn = DBUtil.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM t_user WHERE user_id = ?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to delete test user: " + e.getMessage());
        }
    }

    @Test
    void testGetAvatar_EmptyByDefault() {
        String avatar = userDAO.getAvatar(testUserId);
        assertEquals("", avatar);
        System.out.println("Test 1: Default avatar is empty - PASSED");
    }

    @Test
    void testUpdateAvatar_Success() {
        String testAvatarPath = "avatar/test_" + testUserId + "_" + System.currentTimeMillis() + ".png";
        
        boolean updated = userDAO.updateAvatar(testUserId, testAvatarPath);
        assertTrue(updated);
        System.out.println("Test 2: Update avatar returned true - PASSED");
        
        String retrieved = userDAO.getAvatar(testUserId);
        assertEquals(testAvatarPath, retrieved);
        System.out.println("Test 2: Retrieved avatar matches - PASSED");
    }

    @Test
    void testUpdateAvatar_EmptyString() {
        String testAvatarPath = "avatar/test_" + testUserId + ".png";
        userDAO.updateAvatar(testUserId, testAvatarPath);
        
        boolean updated = userDAO.updateAvatar(testUserId, "");
        assertTrue(updated);
        System.out.println("Test 3: Update avatar to empty returned true - PASSED");
        
        String retrieved = userDAO.getAvatar(testUserId);
        assertEquals("", retrieved);
        System.out.println("Test 3: Avatar is now empty - PASSED");
    }

    @Test
    void testUpdateAvatar_NullValue() {
        boolean updated = userDAO.updateAvatar(testUserId, null);
        assertTrue(updated);
        System.out.println("Test 4: Update avatar with null returned true - PASSED");
        
        String retrieved = userDAO.getAvatar(testUserId);
        assertEquals("", retrieved);
        System.out.println("Test 4: Avatar is empty after null update - PASSED");
    }

    @Test
    void testUpdateAvatar_NonExistentUser() {
        long nonExistentId = 999999999L;
        boolean updated = userDAO.updateAvatar(nonExistentId, "avatar/test.png");
        assertFalse(updated);
        System.out.println("Test 5: Update avatar for non-existent user returned false - PASSED");
    }

    @Test
    void testGetAvatar_NonExistentUser() {
        long nonExistentId = 999999999L;
        String avatar = userDAO.getAvatar(nonExistentId);
        assertEquals("", avatar);
        System.out.println("Test 6: Get avatar for non-existent user returns empty - PASSED");
    }

    @Test
    void testAvatarPathFormat() {
        String testAvatarPath = "avatar/123_1234567890.png";
        userDAO.updateAvatar(testUserId, testAvatarPath);
        
        String retrieved = userDAO.getAvatar(testUserId);
        assertNotNull(retrieved);
        assertTrue(retrieved.startsWith("avatar/"));
        assertTrue(retrieved.contains("_"));
        assertTrue(retrieved.endsWith(".png"));
        System.out.println("Test 7: Avatar path format validation - PASSED");
    }

    @Test
    void testAvatarUpdateTime() {
        String avatar1 = "avatar/test1.png";
        userDAO.updateAvatar(testUserId, avatar1);
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String avatar2 = "avatar/test2.png";
        userDAO.updateAvatar(testUserId, avatar2);
        
        String retrieved = userDAO.getAvatar(testUserId);
        assertEquals(avatar2, retrieved);
        System.out.println("Test 8: Avatar update replaces old value - PASSED");
    }
}
