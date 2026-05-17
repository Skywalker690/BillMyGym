package com.gymflow.api.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
    boolean success,
    String message,
    String errorCode,
    List<ApiFieldError> errors,
    OffsetDateTime timestamp
) {

    public static ApiErrorResponse of(String message, String errorCode) {
        return new ApiErrorResponse(false, message, errorCode, List.of(), OffsetDateTime.now());
    }

    public static ApiErrorResponse of(String message, String errorCode, List<ApiFieldError> errors) {
        return new ApiErrorResponse(false, message, errorCode, errors, OffsetDateTime.now());
    }
}
