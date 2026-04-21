package com.onboardguard.shared.common.exception;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ErrorResponse {
    private int status;
    private String message;
    private String path;
    private Instant timestamp;
    private String errorCode;
}
