package com.gomatch.client.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CredentialStoreTest {
    @TempDir Path tempDir;

    @Test
    void removesLegacyPlaintextPasswordOnLoad() throws Exception {
        Path file = tempDir.resolve("credentials.json");
        Files.writeString(file, "{\"username\":\"alice\",\"password\":\"secret\"}");
        CredentialStore store = new CredentialStore(file);

        assertEquals("alice", store.loadUsername().orElseThrow());
        String rewritten = Files.readString(file);
        assertTrue(rewritten.contains("alice"));
        assertFalse(rewritten.contains("password"));
        assertFalse(rewritten.contains("secret"));
    }

    @Test
    void savesOnlyUsernameAndCanClear() throws Exception {
        Path file = tempDir.resolve("credentials.json");
        CredentialStore store = new CredentialStore(file);

        store.saveUsername(" bob ");
        assertEquals("bob", store.loadUsername().orElseThrow());
        assertFalse(Files.readString(file).contains("password"));
        store.clear();
        assertFalse(Files.exists(file));
    }
}
