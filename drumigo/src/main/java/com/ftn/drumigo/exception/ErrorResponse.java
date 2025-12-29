package com.ftn.drumigo.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    List<ValidationError> validationErrors
) {
    public record ValidationError(
        String field,
        String message
    ) {}
}

