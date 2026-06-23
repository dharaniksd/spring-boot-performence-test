package com.dharaniksd.performence.dto;

/**
 * Payload returned by the /cpu endpoint.
 */
public record CpuResponse(
        int iterations,
        long result,
        long elapsedMs
) {}
