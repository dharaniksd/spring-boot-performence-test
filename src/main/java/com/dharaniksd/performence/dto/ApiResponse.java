package com.dharaniksd.performence.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Generic API response wrapper used across all test endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String status,
        String endpoint,
        T data,
        long timestampEpochMs,
        Long elapsedMs
) {
    public static <T> ApiResponse<T> ok(String endpoint, T data, long elapsedMs) {
        return new ApiResponse<>("OK", endpoint, data, Instant.now().toEpochMilli(), elapsedMs);
    }

    public static <T> ApiResponse<T> ok(String endpoint, T data) {
        return new ApiResponse<>("OK", endpoint, data, Instant.now().toEpochMilli(), null);
    }
}
