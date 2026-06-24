package com.dharaniksd.performence.dto;

/**
 * Payload returned by the /ping endpoint.
 */
public record PingResponse(
        String message,
        long serverTimestampEpochMs,
        String javaVersion,
        String profile
) {}
