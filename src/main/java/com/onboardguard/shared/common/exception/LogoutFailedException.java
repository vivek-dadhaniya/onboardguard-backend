package com.onboardguard.shared.common.exception;

import org.springframework.http.HttpStatus;

public class LogoutFailedException extends ApplicationException {

    public LogoutFailedException(String message) {
        super(message, "LOGOUT_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public LogoutFailedException(String message, Throwable cause) {
        super(message, "LOGOUT_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}