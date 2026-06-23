package com.dharaniksd.performence.controller;

import com.dharaniksd.performence.dto.*;
import com.dharaniksd.performence.service.PerformenceTestService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing benchmark/load-test endpoints under {@code /api/test}.
 * All business logic is delegated to {@link PerformenceTestService}.
 */
@RestController
@RequestMapping("/api/test")
@Validated
public class PerformenceTestController {

    private final PerformenceTestService service;

    public PerformenceTestController(PerformenceTestService service) {
        this.service = service;
    }

    /**
     * Fast-response ping endpoint – measures baseline round-trip latency.
     *
     * @return server timestamp and JVM metadata
     */
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<PingResponse>> ping() {
        PingResponse data = service.ping();
        return ResponseEntity.ok(ApiResponse.ok("/api/test/ping", data));
    }

    /**
     * CPU load endpoint – runs a deterministic prime-counting loop.
     *
     * @param iterations number of loop iterations (1 – 10,000,000)
     * @return iteration count, computed result, and elapsed time
     */
    @GetMapping("/cpu")
    public ResponseEntity<ApiResponse<CpuResponse>> cpu(
            @RequestParam(defaultValue = "100000")
            @Min(value = 1, message = "iterations must be at least 1")
            @Max(value = 10_000_000, message = "iterations must not exceed 10,000,000")
            int iterations) {

        long start = System.currentTimeMillis();
        CpuResponse data = service.cpuWork(iterations);
        return ResponseEntity.ok(ApiResponse.ok("/api/test/cpu", data, System.currentTimeMillis() - start));
    }

    /**
     * Latency simulation endpoint – sleeps for the requested number of ms.
     *
     * @param ms sleep duration in milliseconds (0 – 30,000)
     * @return requested and actual elapsed time
     */
    @GetMapping("/latency")
    public ResponseEntity<ApiResponse<LatencyResponse>> latency(
            @RequestParam(defaultValue = "100")
            @Min(value = 0, message = "ms must be non-negative")
            @Max(value = 30_000, message = "ms must not exceed 30,000")
            long ms) throws InterruptedException {

        LatencyResponse data = service.simulateLatency(ms);
        return ResponseEntity.ok(ApiResponse.ok("/api/test/latency", data, data.actualElapsedMs()));
    }

    /**
     * Memory allocation endpoint – allocates and immediately releases a byte block.
     *
     * @param mb size to allocate in megabytes (1 – 512)
     * @return requested size, actual allocated size, and elapsed time
     */
    @GetMapping("/memory")
    public ResponseEntity<ApiResponse<MemoryResponse>> memory(
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "mb must be at least 1")
            @Max(value = 512, message = "mb must not exceed 512")
            int mb) {

        long start = System.currentTimeMillis();
        MemoryResponse data = service.allocateMemory(mb);
        return ResponseEntity.ok(ApiResponse.ok("/api/test/memory", data, System.currentTimeMillis() - start));
    }

    /**
     * Cache hit/miss demonstration endpoint.
     *
     * @param key cache key (1 – 64 characters, required)
     * @return cached value with hit/miss indicator
     */
    @GetMapping("/cache/{key}")
    public ResponseEntity<ApiResponse<CacheResponse>> cache(
            @PathVariable
            @NotBlank(message = "key must not be blank")
            @Size(max = 64, message = "key must not exceed 64 characters")
            String key) {

        CacheResponse data = service.getCachedValue(key);
        return ResponseEntity.ok(ApiResponse.ok("/api/test/cache/" + key, data));
    }
}
