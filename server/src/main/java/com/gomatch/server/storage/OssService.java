package com.gomatch.server.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.Date;

public class OssService {
    private static final Logger logger = LoggerFactory.getLogger(OssService.class);

    private final String endpoint;
    private final String bucketName;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final long avatarMaxSize;
    private final long musicMaxSize;

    public OssService(String endpoint, String bucketName, String accessKeyId, String accessKeySecret,
                      long avatarMaxSize, long musicMaxSize) {
        this.endpoint = endpoint;
        this.bucketName = bucketName;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.avatarMaxSize = avatarMaxSize;
        this.musicMaxSize = musicMaxSize;
        logger.info("OSS Service initialized with endpoint: {}, bucket: {}", endpoint, bucketName);
    }

    public String uploadAvatar(Long userId, String base64Data) throws IOException {
        byte[] data = decodeBase64(base64Data);
        validateAvatarSize(data.length);
        
        String extension = getFileExtension(base64Data);
        String fileName = String.format("avatar/%d_%d.%s", userId, System.currentTimeMillis(), extension);
        
        OSS ossClient = null;
        try {
            ossClient = createOssClient();
            ossClient.putObject(bucketName, fileName, new java.io.ByteArrayInputStream(data));
            logger.info("Uploaded avatar: {} for user: {}", fileName, userId);
            return fileName;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public String uploadMusic(Long userId, String base64Data, String originalFileName) throws IOException {
        byte[] data = decodeBase64(base64Data);
        validateMusicSize(data.length);
        
        String extension = getFileExtension(originalFileName);
        String fileName = String.format("music/%d/%d_%s", userId, System.currentTimeMillis(), originalFileName);
        
        OSS ossClient = null;
        try {
            ossClient = createOssClient();
            ossClient.putObject(bucketName, fileName, new java.io.ByteArrayInputStream(data));
            logger.info("Uploaded music: {} for user: {}", fileName, userId);
            return fileName;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public byte[] downloadFile(String filePath) throws IOException {
        OSS ossClient = null;
        OSSObject ossObject = null;
        try {
            ossClient = createOssClient();
            ossObject = ossClient.getObject(bucketName, filePath);
            InputStream inputStream = ossObject.getObjectContent();
            byte[] data = inputStream.readAllBytes();
            logger.info("Downloaded file: {}, size: {} bytes", filePath, data.length);
            return data;
        } finally {
            if (ossObject != null) {
                ossObject.close();
            }
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public String downloadFileAsBase64(String filePath) throws IOException {
        byte[] data = downloadFile(filePath);
        String mimeType = getMimeType(filePath);
        return String.format("data:%s;base64,%s", mimeType, Base64.getEncoder().encodeToString(data));
    }

    public void deleteFile(String filePath) {
        OSS ossClient = null;
        try {
            ossClient = createOssClient();
            ossClient.deleteObject(bucketName, filePath);
            logger.info("Deleted file: {}", filePath);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public boolean fileExists(String filePath) {
        OSS ossClient = null;
        try {
            ossClient = createOssClient();
            return ossClient.doesObjectExist(bucketName, filePath);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public String generatePresignedUrl(String filePath) {
        return generatePresignedUrl(filePath, 3600);
    }

    public String generatePresignedUrl(String filePath, int expirationSeconds) {
        String objectKey = filePath;
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            String bucketHost = bucketName + "." + endpoint.replace("https://", "").replace("http://", "");
            if (filePath.contains(bucketHost)) {
                objectKey = filePath.substring(filePath.indexOf(bucketHost) + bucketHost.length() + 1);
            }
        }
        OSS ossClient = null;
        try {
            ossClient = createOssClient();
            Date expiration = new Date(System.currentTimeMillis() + (long) expirationSeconds * 1000);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectKey);
            request.setExpiration(expiration);
            URL url = ossClient.generatePresignedUrl(request);
            logger.info("Generated presigned URL for: {}, expires in {} seconds", objectKey, expirationSeconds);
            return url.toString();
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public long getFileSize(String filePath) {
        OSS ossClient = null;
        try {
            ossClient = createOssClient();
            return ossClient.getObjectMetadata(bucketName, filePath).getContentLength();
        } catch (Exception e) {
            logger.error("Error getting file size: {}", filePath, e);
            return 0;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private OSS createOssClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    private byte[] decodeBase64(String base64Data) {
        if (base64Data.contains(",")) {
            base64Data = base64Data.split(",")[1];
        }
        return Base64.getDecoder().decode(base64Data);
    }

    private String getFileExtension(String base64Data) {
        if (base64Data.startsWith("data:image/png")) {
            return "png";
        } else if (base64Data.startsWith("data:image/jpeg") || base64Data.startsWith("data:image/jpg")) {
            return "jpg";
        } else if (base64Data.startsWith("data:image/webp")) {
            return "webp";
        } else if (base64Data.startsWith("data:image/gif")) {
            return "gif";
        } else if (base64Data.contains(".")) {
            return base64Data.substring(base64Data.lastIndexOf(".") + 1).toLowerCase();
        }
        return "png";
    }

    private String getMimeType(String filePath) {
        if (filePath.endsWith(".png")) {
            return "image/png";
        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filePath.endsWith(".webp")) {
            return "image/webp";
        } else if (filePath.endsWith(".gif")) {
            return "image/gif";
        } else if (filePath.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (filePath.endsWith(".wav")) {
            return "audio/wav";
        } else if (filePath.endsWith(".ogg")) {
            return "audio/ogg";
        }
        return "application/octet-stream";
    }

    private void validateAvatarSize(long size) throws IOException {
        if (size > avatarMaxSize) {
            throw new IOException("Avatar size exceeds maximum limit of " + avatarMaxSize + " bytes");
        }
    }

    private void validateMusicSize(long size) throws IOException {
        if (size > musicMaxSize) {
            throw new IOException("Music size exceeds maximum limit of " + musicMaxSize + " bytes");
        }
    }
}
