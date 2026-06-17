package com.chatsphere.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25 MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "webp", "gif", 
            "pdf", "docx", "pptx", "xlsx", "zip", 
            "webm", "mp3"
    );

    private final String uploadDir = "./uploads";

    public UploadResult uploadFile(MultipartFile file) {
        // 1. Validation
        if (file.isEmpty()) {
            throw new RuntimeException("Cannot upload empty file");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds maximum limit of 25MB");
        }

        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(filename);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new RuntimeException("File type not supported. Allowed: " + ALLOWED_EXTENSIONS);
        }

        try {
            byte[] fileBytes = file.getBytes();

            // 2. Image Compression
            if (isImage(extension)) {
                log.info("Compressing image: {}", filename);
                fileBytes = compressImage(fileBytes, extension);
            }

            // 3. Cloudinary Upload or Local Fallback
            if (StringUtils.hasText(cloudinaryUrl)) {
                try {
                    log.info("Uploading to Cloudinary: {}", filename);
                    Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
                    Map<?, ?> uploadResult = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap(
                            "public_id", UUID.randomUUID().toString(),
                            "resource_type", isAudio(extension) ? "video" : "auto"
                    ));
                    String url = (String) uploadResult.get("secure_url");
                    return new UploadResult(url, filename, file.getContentType(), fileBytes.length);
                } catch (Exception ex) {
                    log.error("Cloudinary upload failed, falling back to local storage.", ex);
                }
            }

            // Local fallback
            log.info("Saving file locally: {}", filename);
            Path path = Paths.get(uploadDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            String storedName = UUID.randomUUID().toString() + "." + extension;
            Path filePath = path.resolve(storedName);
            Files.write(filePath, fileBytes);

            // Access URL on local proxy gateway
            String localUrl = "/api/media/files/" + storedName;
            return new UploadResult(localUrl, filename, file.getContentType(), fileBytes.length);

        } catch (IOException e) {
            log.error("Failed to store file: ", e);
            throw new RuntimeException("File storage failed", e);
        }
    }

    public byte[] loadLocalFile(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            } else {
                throw new RuntimeException("File not found locally");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private boolean isImage(String extension) {
        return List.of("jpg", "jpeg", "png", "webp", "gif").contains(extension.toLowerCase());
    }

    private boolean isAudio(String extension) {
        return List.of("webm", "mp3").contains(extension.toLowerCase());
    }

    private byte[] compressImage(byte[] rawBytes, String extension) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(rawBytes);
            BufferedImage image = ImageIO.read(bais);
            if (image == null) return rawBytes;

            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();
            int targetWidth = Math.min(originalWidth, 1020);
            int targetHeight = (int) (originalHeight * ((double) targetWidth / originalWidth));

            BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(image, 0, 0, targetWidth, targetHeight, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String format = extension.equalsIgnoreCase("png") ? "png" : "jpeg";
            ImageIO.write(resized, format, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Image compression error, uploading raw bytes.", e);
            return rawBytes;
        }
    }

    public static class UploadResult {
        public String url;
        public String fileName;
        public String fileType;
        public long fileSize;

        public UploadResult(String url, String fileName, String fileType, long fileSize) {
            this.url = url;
            this.fileName = fileName;
            this.fileType = fileType;
            this.fileSize = fileSize;
        }
    }
}
