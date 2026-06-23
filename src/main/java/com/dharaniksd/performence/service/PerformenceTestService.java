package com.dharaniksd.performence.service;

import com.dharaniksd.performence.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;

/**
 * Service containing the business logic for all performance test endpoints.
 * Guardrails on all parameters prevent runaway resource usage.
 */
@Service
public class PerformenceTestService {

    private static final Logger log = LoggerFactory.getLogger(PerformenceTestService.class);

    /** Maximum number of CPU iterations allowed per request. */
    public static final int MAX_CPU_ITERATIONS = 10_000_000;

    /** Maximum artificial latency in milliseconds. */
    public static final long MAX_LATENCY_MS = 30_000L;

    /** Maximum memory allocation allowed per request in MB. */
    public static final int MAX_MEMORY_MB = 512;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    // -------------------------------------------------------------------------
    // Ping
    // -------------------------------------------------------------------------

    public PingResponse ping() {
        return new PingResponse(
                "pong",
                Instant.now().toEpochMilli(),
                System.getProperty("java.version"),
                activeProfile
        );
    }

    // -------------------------------------------------------------------------
    // CPU load
    // -------------------------------------------------------------------------

    /**
     * Performs a deterministic CPU work loop using a simple prime-counting
     * algorithm. The iteration count is capped at {@value #MAX_CPU_ITERATIONS}.
     *
     * @param iterations number of iterations (will be clamped to max)
     * @return result payload including elapsed time
     */
    public CpuResponse cpuWork(int iterations) {
        int clamped = Math.min(Math.max(1, iterations), MAX_CPU_ITERATIONS);
        if (clamped != iterations) {
            log.debug("CPU iterations clamped from {} to {}", iterations, clamped);
        }

        long start = System.currentTimeMillis();
        long result = countPrimesUpTo(clamped);
        long elapsed = System.currentTimeMillis() - start;

        return new CpuResponse(clamped, result, elapsed);
    }

    /** Counts primes up to {@code limit} using a basic trial-division approach. */
    private long countPrimesUpTo(int limit) {
        long count = 0;
        for (int n = 2; n <= limit; n++) {
            boolean prime = true;
            for (int d = 2; d * d <= n; d++) {
                if (n % d == 0) {
                    prime = false;
                    break;
                }
            }
            if (prime) count++;
        }
        return count;
    }

    // -------------------------------------------------------------------------
    // Latency simulation
    // -------------------------------------------------------------------------

    /**
     * Sleeps for the requested number of milliseconds (capped at
     * {@value #MAX_LATENCY_MS} ms).
     *
     * @param ms sleep duration in milliseconds
     * @return payload with requested and actual elapsed time
     * @throws InterruptedException if the thread is interrupted during sleep
     */
    public LatencyResponse simulateLatency(long ms) throws InterruptedException {
        long clamped = Math.min(Math.max(0, ms), MAX_LATENCY_MS);
        if (clamped != ms) {
            log.debug("Latency clamped from {}ms to {}ms", ms, clamped);
        }

        long start = System.currentTimeMillis();
        if (clamped > 0) {
            Thread.sleep(clamped);
        }
        long elapsed = System.currentTimeMillis() - start;

        return new LatencyResponse(clamped, elapsed);
    }

    // -------------------------------------------------------------------------
    // Memory allocation
    // -------------------------------------------------------------------------

    /**
     * Allocates a byte array of the requested size (in MB), holds it briefly
     * to simulate allocation pressure, then discards the reference so the GC
     * can reclaim it. Capped at {@value #MAX_MEMORY_MB} MB.
     *
     * @param mb megabytes to allocate
     * @return payload describing the allocation
     */
    public MemoryResponse allocateMemory(int mb) {
        int clamped = Math.min(Math.max(1, mb), MAX_MEMORY_MB);
        if (clamped != mb) {
            log.debug("Memory request clamped from {}MB to {}MB", mb, clamped);
        }

        long start = System.currentTimeMillis();
        byte[] block = new byte[clamped * 1024 * 1024];
        // Touch the array to prevent JIT from optimising the allocation away.
        Arrays.fill(block, (byte) 1);
        long elapsed = System.currentTimeMillis() - start;

        // Drop reference immediately so GC can reclaim.
        //noinspection UnusedAssignment
        block = null;

        return new MemoryResponse(mb, clamped, elapsed);
    }

    // -------------------------------------------------------------------------
    // Cache demonstration
    // -------------------------------------------------------------------------

    /**
     * Returns a cached value for the given key. The first call will be a cache
     * miss (simulated with a small delay); subsequent calls within the TTL will
     * hit the cache and return instantly.
     *
     * @param key cache key (max 64 chars)
     * @return cache response indicating hit/miss
     */
    @Cacheable(value = "testCache", key = "#key")
    public CacheResponse getCachedValue(String key) {
        log.debug("Cache MISS for key: {}", key);
        // Simulate a small backend fetch cost on a cache miss.
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String value = "value-for-" + key + "-at-" + Instant.now().toEpochMilli();
        // cacheHit is false here because this method body only executes on a miss.
        return new CacheResponse(key, value, false);
    }
}
