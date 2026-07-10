package com.gomatch.server.dao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoSqlTest {

    @Test
    void loginQuerySupportsEveryAccountIdentifier() {
        assertTrue(UserDAO.LOGIN_BY_ACCOUNT_SQL.contains("user_id = ?"));
        assertTrue(UserDAO.LOGIN_BY_ACCOUNT_SQL.contains("username = ?"));
        assertTrue(UserDAO.LOGIN_BY_ACCOUNT_SQL.contains("email = ?"));
        assertTrue(UserDAO.LOGIN_BY_ACCOUNT_SQL.contains("phone = ?"));
    }
}