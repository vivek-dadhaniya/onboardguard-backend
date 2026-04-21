package com.onboardguard.shared.storage;

import com.cloudinary.AuthToken;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Primary
@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements CloudStorageService {

    private final Cloudinary cloudinary;

    private static final String NOT_FOUND_MESSAGE = "not found";

    // PROJECT REQUIREMENTS: VALIDATION CONSTANTS
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    // VALIDATION & SANITIZATION
    private String validateAndSanitizeKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new StorageValidationException("storageKey must not be null or empty");
        }
        // Allows alphanumeric, slashes (folders), hyphens, underscores, and dots (extensions)
        return storageKey.replaceAll("[^a-zA-Z0-9/\\-_\\.]", "_");
    }

    private void validateMultipartFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageValidationException("File must not be null or empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new StorageValidationException("File exceeds the 5MB maximum size limit.");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType().toLowerCase())) {
            throw new StorageValidationException("Invalid file format. Only PDF, JPG, and PNG are accepted.");
        }
    }

    // UPLOAD MultipartFile
    @Override
    public String upload(String storageKey, MultipartFile file) {
        String safeKey = validateAndSanitizeKey(storageKey);
        validateMultipartFile(file);

        log.debug("Cloudinary upload: key={} size={}B", safeKey, file.getSize());

        try {
            return uploadBytesToCloudinary(safeKey, file.getBytes());
        } catch (IOException e) {
            log.error("Failed to read file bytes: key={}", safeKey, e);
            throw new CloudStorageException("Failed to read uploaded file", safeKey, e);
        }
    }

    // UPLOAD byte[]
    @Override
    public String uploadBytes(String storageKey, byte[] content, String contentType) {
        String safeKey = validateAndSanitizeKey(storageKey);

        if (content == null || content.length == 0) {
            throw new StorageValidationException("content must not be empty");
        }

        log.debug("Cloudinary uploadBytes: key={} size={}B", safeKey, content.length);

        return uploadBytesToCloudinary(safeKey, content);
    }

    // CORE UPLOAD
    private String uploadBytesToCloudinary(String storageKey, byte[] bytes) {
        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "public_id", storageKey,
                    "resource_type", resolveResourceType(storageKey),
                    "type", "authenticated",
                    "overwrite", true,
                    // REQUIRED to keep "candidateId/docType/filename" format perfectly intact
                    "use_filename", true,
                    "unique_filename", false
            );

            Map<?, ?> result = cloudinary.uploader().upload(bytes, params);

            if (result == null || result.get("public_id") == null) {
                log.error("Invalid Cloudinary upload response: key={} response={}", storageKey, result);
                throw new CloudStorageException("Invalid upload response", storageKey);
            }

            log.debug("Cloudinary upload complete: key={}", storageKey);
            return storageKey;

        } catch (Exception e) {
            log.error("Cloudinary upload failed: key={}", storageKey, e);
            throw new CloudStorageException("Cloudinary upload failed", storageKey, e);
        }
    }

    // PRESIGNED URL (Time-Limited / Not Public)
    @Override
    public String generatePresignedUrl(String storageKey, Duration expiry) {
        String safeKey = validateAndSanitizeKey(storageKey);

        if (expiry == null || expiry.isZero() || expiry.isNegative()) {
            throw new StorageValidationException("expiry must be positive");
        }

        log.debug("Cloudinary presign: key={} expiryMin={}", safeKey, expiry.toMinutes());

        try {
            long expireAt = Instant.now().plus(expiry).getEpochSecond();

            AuthToken authToken = new AuthToken()
                    .tokenName("e")          // e = expire
                    .expiration(expireAt);   // URL invalid after this Unix timestamp

            String url = cloudinary.url()
                    .secure(true)
                    .resourceType(resolveResourceType(safeKey))
                    .type("authenticated")
                    .authToken(authToken)
                    .generate(safeKey);

            log.debug("Presigned URL generated: key={} expireAt={}", safeKey, expireAt);
            return url;

        } catch (Exception e) {
            log.error("Failed to generate presigned URL: key={}", safeKey, e);
            throw new CloudStorageException("Failed to generate signed URL", safeKey, e);
        }
    }

    // DELETE
    @Override
    public void delete(String storageKey) {
        String safeKey = validateAndSanitizeKey(storageKey);
        log.debug("Cloudinary delete: key={}", safeKey);

        try {
            Map<?, ?> result = cloudinary.uploader().destroy(safeKey, ObjectUtils.asMap(
                    "resource_type", resolveResourceType(safeKey),
                    "type", "authenticated"
            ));

            if (result == null || !"ok".equals(result.get("result"))) {
                log.error("Cloudinary delete failed: key={} response={}", safeKey, result);
                throw new CloudStorageException("Delete failed", safeKey);
            }

            log.debug("Cloudinary delete complete: key={}", safeKey);

        } catch (Exception e) {
            log.error("Cloudinary delete exception: key={}", safeKey, e);
            throw new CloudStorageException("Cloudinary delete failed", safeKey, e);
        }
    }

    // EXISTS
    @Override
    public boolean exists(String storageKey) {
        String safeKey = validateAndSanitizeKey(storageKey);
        log.debug("Cloudinary exists check: key={}", safeKey);

        try {
            cloudinary.api().resource(safeKey, ObjectUtils.asMap(
                    "resource_type", resolveResourceType(safeKey),
                    "type", "authenticated"
            ));
            return true;

        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

            if (msg.contains(NOT_FOUND_MESSAGE) || msg.contains("resource not found")) {
                log.debug("Cloudinary resource not found: key={}", safeKey);
                return false;
            }

            log.error("Cloudinary exists check failed: key={} message={}", safeKey, msg, e);
            throw new CloudStorageException("Existence check failed", safeKey, e);
        }
    }

    // RESOURCE TYPE
    private String resolveResourceType(String storageKey) {
        return "raw";
    }
}