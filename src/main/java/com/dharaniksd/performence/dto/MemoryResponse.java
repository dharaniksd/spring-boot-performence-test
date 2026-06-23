package com.dharaniksd.performence.dto;

/**
 * Payload returned by the /memory endpoint.
 */
public record MemoryResponse(
        int requestedMb,
        int allocatedMb,
        long elapsedMs
) {}
