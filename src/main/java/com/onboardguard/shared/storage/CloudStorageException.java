package com.onboardguard.shared.storage;

import com.onboardguard.shared.common.exception.ApplicationException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CloudStorageException extends ApplicationException {

    private final String storageKey;

    public CloudStorageException(String message, String storageKey, Throwable cause) {
        super(message, "CLOUD_STORAGE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, cause);
        this.storageKey = storageKey;
    }

    public CloudStorageException(String message, String storageKey) {
        super(message, "CLOUD_STORAGE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        this.storageKey = storageKey;
    }
}