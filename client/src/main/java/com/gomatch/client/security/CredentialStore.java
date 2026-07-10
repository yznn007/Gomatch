package com.gomatch.client.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Persists only a remembered username and removes legacy plaintext passwords. */
public final class CredentialStore {
    private static final Gson GSON = new Gson();
    private final Path file;

    public CredentialStore() {
        this(Path.of(System.getProperty("user.home"), ".gomatch", "credentials.json"));
    }

    public CredentialStore(Path file) {
        this.file = file;
    }

    public Optional<String> loadUsername() {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null || !json.has("username")) {
                clear();
                return Optional.empty();
            }
            String username = json.get("username").getAsString().trim();
            if (username.isEmpty()) {
                clear();
                return Optional.empty();
            }
            if (json.has("password")) {
                saveUsername(username);
            }
            return Optional.of(username);
        } catch (IOException | RuntimeException invalidFile) {
            return Optional.empty();
        }
    }

    public void saveUsername(String username) throws IOException {
        String normalized = username == null ? "" : username.trim();
        if (normalized.isEmpty()) {
            clear();
            return;
        }
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        JsonObject json = new JsonObject();
        json.addProperty("username", normalized);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
    }

    public void clear() throws IOException {
        Files.deleteIfExists(file);
    }
}
