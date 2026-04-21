package com.onboardguard.shared.common.exception;

import org.springframework.http.HttpStatus;

public class StorageValidationException extends ApplicationException {
    public StorageValidationException(String message) {
        super(message, "STORAGE_VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }
}
