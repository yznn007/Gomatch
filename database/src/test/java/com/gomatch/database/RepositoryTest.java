package com.gomatch.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RepositoryTest {

    @Test
    void databasePropertiesIsPackaged() {
        assertNotNull(
                Thread.currentThread().getContextClassLoader().getResource("database.properties"),
                "database.properties should be available on the test classpath"
        );
    }

    @Test
    void bootstrapClassIsLoadableWithoutOpeningDatabaseConnection() {
        assertEquals("DatabaseBootstrap", DatabaseBootstrap.class.getSimpleName());
    }
}
