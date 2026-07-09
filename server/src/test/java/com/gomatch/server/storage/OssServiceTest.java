package com.gomatch.server.storage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class OssServiceTest {
    private static OssService ossService;
    private static String testAvatarPath;
    private static String testMusicPath;

    @BeforeAll
    static void setup() {
        String endpoint = "https://oss-cn-beijing.aliyuncs.com";
        String bucket = "gomatch-storage";
        String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");

        assertNotNull(accessKeyId, "OSS_ACCESS_KEY_ID environment variable not set");
        assertNotNull(accessKeySecret, "OSS_ACCESS_KEY_SECRET environment variable not set");

        ossService = new OssService(endpoint, bucket, accessKeyId, accessKeySecret, 2097152, 10485760);
        System.out.println("OSS Service initialized successfully");
    }

    @Test
    void testUploadAvatar() throws IOException {
        byte[] testData = new byte[100];
        for (int i = 0; i < 100; i++) {
            testData[i] = (byte) i;
        }
        String base64Data = "data:image/png;base64," + Base64.getEncoder().encodeToString(testData);

        testAvatarPath = ossService.uploadAvatar(1L, base64Data);
        System.out.println("Uploaded avatar: " + testAvatarPath);

        assertNotNull(testAvatarPath);
        assertTrue(testAvatarPath.startsWith("avatar/"));
        assertTrue(ossService.fileExists(testAvatarPath));
    }

    @Test
    void testDownloadAvatar() throws IOException {
        assertNotNull(testAvatarPath, "testUploadAvatar must run first");

        byte[] data = ossService.downloadFile(testAvatarPath);
        System.out.println("Downloaded avatar size: " + data.length + " bytes");

        assertNotNull(data);
        assertEquals(100, data.length);
    }

    @Test
    void testDownloadAvatarAsBase64() throws IOException {
        assertNotNull(testAvatarPath, "testUploadAvatar must run first");

        String base64Data = ossService.downloadFileAsBase64(testAvatarPath);
        System.out.println("Downloaded avatar as base64, length: " + base64Data.length());

        assertNotNull(base64Data);
        assertTrue(base64Data.startsWith("data:image/png;base64,"));
    }

    @Test
    void testUploadMusic() throws IOException {
        byte[] testData = new byte[500];
        for (int i = 0; i < 500; i++) {
            testData[i] = (byte) (i % 256);
        }
        String base64Data = "data:audio/mpeg;base64," + Base64.getEncoder().encodeToString(testData);

        testMusicPath = ossService.uploadMusic(1L, base64Data, "test_song.mp3");
        System.out.println("Uploaded music: " + testMusicPath);

        assertNotNull(testMusicPath);
        assertTrue(testMusicPath.startsWith("music/"));
        assertTrue(ossService.fileExists(testMusicPath));
    }

    @Test
    void testDownloadMusic() throws IOException {
        assertNotNull(testMusicPath, "testUploadMusic must run first");

        byte[] data = ossService.downloadFile(testMusicPath);
        System.out.println("Downloaded music size: " + data.length + " bytes");

        assertNotNull(data);
        assertEquals(500, data.length);
    }

    @Test
    void testFileExists() {
        assertNotNull(testAvatarPath, "testUploadAvatar must run first");

        assertTrue(ossService.fileExists(testAvatarPath));
        assertFalse(ossService.fileExists("nonexistent/path.png"));
    }

    @Test
    void testGetFileSize() throws IOException {
        assertNotNull(testAvatarPath, "testUploadAvatar must run first");

        long size = ossService.getFileSize(testAvatarPath);
        System.out.println("File size: " + size);

        assertEquals(100, size);
    }

    @AfterAll
    static void cleanup() {
        try {
            if (testAvatarPath != null && ossService.fileExists(testAvatarPath)) {
                ossService.deleteFile(testAvatarPath);
                System.out.println("Deleted avatar: " + testAvatarPath);
            }
        } catch (Exception e) {
            System.err.println("Failed to delete avatar: " + e.getMessage());
        }

        try {
            if (testMusicPath != null && ossService.fileExists(testMusicPath)) {
                ossService.deleteFile(testMusicPath);
                System.out.println("Deleted music: " + testMusicPath);
            }
        } catch (Exception e) {
            System.err.println("Failed to delete music: " + e.getMessage());
        }
    }
}
