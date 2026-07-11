package com.gomatch.client.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

public final class CredentialStore {
    private static final Gson GSON = new Gson();
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String KEY_FILE_NAME = ".key";

    private final Path file;
    private final Path keyFile;

    public CredentialStore() {
        this(Path.of(System.getProperty("user.home"), ".gomatch", "credentials.json"));
    }

    public CredentialStore(Path file) {
        this.file = file;
        this.keyFile = file.getParent() != null ? file.getParent().resolve(KEY_FILE_NAME) : Path.of(KEY_FILE_NAME);
    }

    private byte[] getOrCreateKey() throws IOException {
        if (Files.exists(keyFile)) {
            return Files.readAllBytes(keyFile);
        }
        byte[] key = new byte[16];
        new SecureRandom().nextBytes(key);
        Path parent = keyFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(keyFile, key);
        return key;
    }

    private String encrypt(String plaintext, byte[] key) throws Exception {
        if (plaintext == null || plaintext.isEmpty()) {
            return "";
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    private String decrypt(String encryptedText, byte[] key) throws Exception {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return "";
        }
        byte[] combined = Base64.getDecoder().decode(encryptedText);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

        SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public Optional<String> loadUsername() {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            JsonObject json;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                json = GSON.fromJson(reader, JsonObject.class);
            }
            if (json == null || !json.has("username")) {
                clear();
                return Optional.empty();
            }
            String username = json.get("username").getAsString().trim();
            if (username.isEmpty()) {
                clear();
                return Optional.empty();
            }

            // 自动剥离遗留明文密码 (Legacy Plaintext Password)
            if (json.has("password")) {
                String rawPassword = json.get("password").getAsString();
                if (!rawPassword.isEmpty()) {
                    boolean isEncrypted = false;
                    try {
                        byte[] combined = Base64.getDecoder().decode(rawPassword);
                        if (combined.length > GCM_IV_LENGTH) {
                            byte[] key = getOrCreateKey();
                            decrypt(rawPassword, key);
                            isEncrypted = true;
                        }
                    } catch (Exception ignored) {
                        // 判定为明文密码
                    }

                    if (!isEncrypted) {
                        json.remove("password");
                        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                            GSON.toJson(json, writer);
                        }
                    }
                }
            }

            return Optional.of(username);
        } catch (IOException | RuntimeException invalidFile) {
            return Optional.empty();
        }
    }

    public Optional<String> loadPassword() {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null || !json.has("password")) {
                return Optional.empty();
            }
            String encryptedPassword = json.get("password").getAsString();
            if (encryptedPassword.isEmpty()) {
                return Optional.empty();
            }
            byte[] key = getOrCreateKey();
            String password = decrypt(encryptedPassword, key);
            if (password.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(password);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void saveCredentials(String username, String password) throws IOException {
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
        if (password != null && !password.isEmpty()) {
            try {
                byte[] key = getOrCreateKey();
                String encryptedPassword = encrypt(password, key);
                json.addProperty("password", encryptedPassword);
            } catch (Exception e) {
                json.addProperty("password", "");
            }
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
    }

    public void saveUsername(String username) throws IOException {
        saveCredentials(username, loadPassword().orElse(""));
    }

    public void clear() throws IOException {
        Files.deleteIfExists(file);
    }
}