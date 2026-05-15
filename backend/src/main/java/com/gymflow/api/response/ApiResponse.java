package com.gymflow.api.response;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    OffsetDateTime timestamp
) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, OffsetDateTime.now());
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, OffsetDateTime.now());
    }
}
