package com.dharaniksd.performence.dto;

import java.util.List;

/**
 * Standardised error response body for all API errors.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        List<String> details
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, null);
    }

    public static ErrorResponse of(int status, String error, String message, List<String> details) {
        return new ErrorResponse(status, error, message, details);
    }
}
