package com.gomatch.server.dao;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoPasswordTest {

    @Test
    void matchesLegacyPlaintextPassword() {
        assertTrue(UserDAO.matchesPassword("legacy-password", "legacy-password"));
        assertFalse(UserDAO.matchesPassword("wrong-password", "legacy-password"));
    }

    @Test
    void matchesBcryptPassword() {
        String hash = BCrypt.hashpw("secure-password", BCrypt.gensalt());

        assertTrue(UserDAO.matchesPassword("secure-password", hash));
        assertFalse(UserDAO.matchesPassword("wrong-password", hash));
    }
}
