package com.ticketflow.ticketflow.common.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        String path,
        int status,
        String ErrorCode,
        String message,
        List<FieldError> fieldErrors

) {
    public record FieldError(String field, String message) {}
}
