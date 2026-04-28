package com.onboardguard.shared.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.onboardguard.shared.common.exception.StorageValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Primary
@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements CloudStorageService {

    private final Cloudinary cloudinary;

    private static final String NOT_FOUND_MESSAGE = "not found";

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private String validateAndSanitizeKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new StorageValidationException("storageKey must not be null or empty");
        }
        return storageKey.replaceAll("[^a-zA-Z0-9/\\-_\\.]", "_");
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new StorageValidationException("Invalid file format. Only PDF, JPG, and PNG are accepted.");
        }
    }

    private void validateMultipartFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageValidationException("File must not be null or empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new StorageValidationException("File exceeds the 5MB maximum size limit.");
        }
        validateContentType(file.getContentType());
    }

    @Override
    public String upload(String storageKey, MultipartFile file) {
        String safeKey = validateAndSanitizeKey(storageKey);
        validateMultipartFile(file);

        log.debug("Cloudinary upload: key={} size={}B", safeKey, file.getSize());

        try {
            return uploadBytesToCloudinary(safeKey, file.getBytes(), file.getContentType());
        } catch (IOException e) {
            log.error("Failed to read file bytes: key={}", safeKey, e);
            throw new CloudStorageException("Failed to read uploaded file", safeKey, e);
        }
    }

    @Override
    public String uploadBytes(String storageKey, byte[] content, String contentType) {
        String safeKey = validateAndSanitizeKey(storageKey);

        if (content == null || content.length == 0) {
            throw new StorageValidationException("content must not be empty");
        }
        validateContentType(contentType);

        return uploadBytesToCloudinary(safeKey, content, contentType);
    }

    private String uploadBytesToCloudinary(String storageKey, byte[] bytes, String contentType) {
        try {
            String folderPath = "";
            String fileName = storageKey;

            if (storageKey.contains("/")) {
                folderPath = storageKey.substring(0, storageKey.lastIndexOf("/"));
                fileName = storageKey.substring(storageKey.lastIndexOf("/") + 1);
            }

            String resourceType = resolveResourceType(contentType);

            // IMPORTANT: Images get extensions stripped, RAW files (PDFs) KEEP their extensions.
            if ("image".equals(resourceType) && fileName.contains(".")) {
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
            }

            Map<String, Object> params = ObjectUtils.asMap(
                    "public_id", fileName,
                    "folder", folderPath,
                    "resource_type", resourceType,
                    "type", "authenticated",
                    "overwrite", true,
                    "use_filename", true,
                    "unique_filename", false
            );

            Map<?, ?> result = cloudinary.uploader().upload(bytes, params);

            if (result == null || result.get("public_id") == null) {
                throw new CloudStorageException("Invalid upload response", storageKey);
            }

            return (String) result.get("public_id");

        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            throw new CloudStorageException("Cloudinary upload failed", storageKey, e);
        }
    }

    @Override
    public String generatePresignedUrl(String storageKey, Duration expiry, String contentType) {
        String safeKey = validateAndSanitizeKey(storageKey);
        validateContentType(contentType);

        try {
            String resourceType = resolveResourceType(contentType);

            com.cloudinary.Url urlBuilder = cloudinary.url()
                    .secure(true)
                    .resourceType(resourceType)
                    .type("authenticated")
                    .signed(true);

            // Re-apply format only for images. RAW (PDF) uses the extension already present in the safeKey.
            if ("image".equals(resourceType)) {
                String format = contentType.toLowerCase().contains("jpeg") || contentType.toLowerCase().contains("jpg") ? "jpg" : "png";
                urlBuilder.format(format);
            }

            return urlBuilder.generate(safeKey);

        } catch (Exception e) {
            log.error("Failed to generate signed URL", e);
            throw new CloudStorageException("Failed to generate signed URL", safeKey, e);
        }
    }

    @Override
    public void delete(String storageKey, String contentType) {
        String safeKey = validateAndSanitizeKey(storageKey);
        validateContentType(contentType);

        try {
            Map<?, ?> result = cloudinary.uploader().destroy(safeKey, ObjectUtils.asMap(
                    "resource_type", resolveResourceType(contentType),
                    "type", "authenticated"
            ));

            if (result == null || !"ok".equals(result.get("result"))) {
                throw new CloudStorageException("Delete failed", safeKey);
            }

        } catch (Exception e) {
            log.error("Cloudinary delete exception", e);
            throw new CloudStorageException("Cloudinary delete failed", safeKey, e);
        }
    }

    @Override
    public boolean exists(String storageKey, String contentType) {
        String safeKey = validateAndSanitizeKey(storageKey);
        validateContentType(contentType);

        try {
            cloudinary.api().resource(safeKey, ObjectUtils.asMap(
                    "resource_type", resolveResourceType(contentType),
                    "type", "authenticated"
            ));
            return true;

        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains(NOT_FOUND_MESSAGE) || msg.contains("resource not found")) {
                return false;
            }
            throw new CloudStorageException("Existence check failed", safeKey, e);
        }
    }

    private String resolveResourceType(String contentType) {
        if (contentType == null) return "raw";
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/png" -> "image";
            case "application/pdf" -> "raw"; // REVERTED: PDFs must be raw to retain document properties
            default -> "raw";
        };
    }
}