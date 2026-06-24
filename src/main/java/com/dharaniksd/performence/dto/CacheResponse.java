package com.dharaniksd.performence.dto;

/**
 * Payload returned by the /cache/{key} endpoint.
 */
public record CacheResponse(
        String key,
        String value,
        boolean cacheHit
) {}
