package com.dharaniksd.performence.dto;

/**
 * Payload returned by the /latency endpoint.
 */
public record LatencyResponse(
        long requestedMs,
        long actualElapsedMs
) {}
