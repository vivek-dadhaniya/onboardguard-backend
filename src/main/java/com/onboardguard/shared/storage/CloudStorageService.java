package com.onboardguard.shared.storage;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * Abstraction over any cloud object-store (S3, GCS, Azure Blob, etc.).
 *
 * <p>Design decisions from the architecture doc:
 * <ul>
 *   <li>Two separate key namespaces are enforced by callers:
 *       {@code candidates/{candidateId}/{docType}/{uuid}} for candidate documents and
 *       {@code watchlist/{entryId}/{filename}} for watchlist evidence.</li>
 *   <li>Pre-signed URLs expire after {@code S3_PRESIGN_MINUTES} (default 15 min),
 *       read from {@link com.onboardguard.shared.config.service.SystemConfigService}.</li>
 *   <li>The interface is intentionally simple — no bucket parameter.
 *       The implementation binds a single bucket from {@code application.yml}
 *       so callers never need to know bucket names.</li>
 *   <li>{@code storageKey} is exactly what gets stored in
 *       {@code candidate_documents.document_name} and
 *       {@code watchlist_evidence_documents.cloud_storage_key}.
 *       It is <em>not</em> a full URL — URLs are generated on-demand.</li>
 * </ul>
 *
 * <p>All methods throw {@link CloudStorageException} (unchecked) on failure
 * so callers do not need to handle checked exceptions. The
 * {@link com.onboardguard.shared.common.exception.GlobalExceptionHandler}
 * maps this to HTTP 500.
 */
public interface CloudStorageService {

    /**
     * Uploads a multipart file and returns the storage key (not a URL).
     *
     * <p>The storage key follows the path convention:
     * <pre>
     *   candidates/{candidateId}/{docType}/{uuid}
     *   watchlist/{entryId}/{sanitisedFilename}
     * </pre>
     *
     * @param storageKey  the full key/path to store the object under
     * @param file        the multipart file from the HTTP request
     * @return            the same {@code storageKey} (convenience for chaining)
     */
    String upload(String storageKey, MultipartFile file);

    /**
     * Uploads raw bytes — used when the content is built programmatically
     * (e.g., generated PDFs, thumbnails) rather than received from a form.
     *
     * @param storageKey   the full key/path to store the object under
     * @param content      raw bytes to upload
     * @param contentType  MIME type, e.g. {@code "application/pdf"}
     * @return             the same {@code storageKey}
     */
    String uploadBytes(String storageKey, byte[] content, String contentType);

    /**
     * Generates a time-limited pre-signed URL that allows an authenticated
     * holder to download the object directly from S3 without going through
     * the application server.
     *
     * <p>The duration should come from
     * {@code SystemConfigService.getInt("S3_PRESIGN_MINUTES")} — do NOT
     * hard-code 15 here. The default in the seed data is 15 minutes.
     *
     * @param storageKey  the key returned by {@link #upload}
     * @param expiry      how long the URL should remain valid
     * @return            a fully-qualified HTTPS pre-signed URL
     */
    String generatePresignedUrl(String storageKey, Duration expiry, String contentType);

    /**
     * Deletes an object permanently.
     *
     * <p><strong>Use with extreme caution.</strong> Candidate documents are
     * never hard-deleted per the architecture doc — only their DB row status
     * changes to REJECTED. This method exists for watchlist evidence cleanup
     * after a soft-delete and for admin tooling only.
     *
     * @param storageKey  the key to delete
     */
    void delete(String storageKey, String contentType);

    /**
     * Checks whether an object with the given key exists.
     * to detect re-upload (PUT) vs first upload (POST).
     *
     * @param storageKey the key to probe
     * @return {@code true} if an object exists at that key
     */
    boolean exists(String storageKey, String contentType);
}