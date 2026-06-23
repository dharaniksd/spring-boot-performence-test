package com.dharaniksd.performence;

import com.dharaniksd.performence.dto.CpuResponse;
import com.dharaniksd.performence.dto.LatencyResponse;
import com.dharaniksd.performence.dto.MemoryResponse;
import com.dharaniksd.performence.service.PerformenceTestService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PerformenceTestService} guardrails and logic.
 * No Spring context is needed – plain JUnit 5.
 */
class PerformenceTestServiceTest {

    private final PerformenceTestService service = new PerformenceTestService();

    // -------------------------------------------------------------------------
    // CPU
    // -------------------------------------------------------------------------

    @Test
    void cpuWork_returnsPositiveResult() {
        CpuResponse response = service.cpuWork(100);
        assertThat(response.iterations()).isEqualTo(100);
        assertThat(response.result()).isGreaterThan(0);
        assertThat(response.elapsedMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void cpuWork_clampsTooLargeIterations() {
        // Request more than the maximum – should be clamped silently.
        CpuResponse response = service.cpuWork(Integer.MAX_VALUE);
        assertThat(response.iterations()).isEqualTo(PerformenceTestService.MAX_CPU_ITERATIONS);
    }

    @Test
    void cpuWork_clampsZeroOrNegativeIterations() {
        CpuResponse response = service.cpuWork(0);
        assertThat(response.iterations()).isEqualTo(1);

        CpuResponse negResponse = service.cpuWork(-50);
        assertThat(negResponse.iterations()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // Latency
    // -------------------------------------------------------------------------

    @Test
    void simulateLatency_zeroSleepReturnsQuickly() throws InterruptedException {
        long before = System.currentTimeMillis();
        LatencyResponse response = service.simulateLatency(0);
        long after = System.currentTimeMillis();

        assertThat(response.requestedMs()).isEqualTo(0);
        assertThat(after - before).isLessThan(200); // should complete well under 200ms
    }

    @Test
    void simulateLatency_clampsTooLargeMs() throws InterruptedException {
        // Request beyond max – should clamp to MAX_LATENCY_MS but for test speed
        // we only check the clamped value, not the actual sleep.
        // Use a value just over max to verify clamping; do NOT actually sleep 30s.
        // We confirm via the returned requestedMs field.
        LatencyResponse response = service.simulateLatency(Long.MAX_VALUE);
        assertThat(response.requestedMs()).isEqualTo(PerformenceTestService.MAX_LATENCY_MS);
    }

    @Test
    void simulateLatency_clampsNegativeMs() throws InterruptedException {
        LatencyResponse response = service.simulateLatency(-100);
        assertThat(response.requestedMs()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Memory
    // -------------------------------------------------------------------------

    @Test
    void allocateMemory_returnsAllocatedSize() {
        MemoryResponse response = service.allocateMemory(10);
        assertThat(response.allocatedMb()).isEqualTo(10);
        assertThat(response.requestedMb()).isEqualTo(10);
        assertThat(response.elapsedMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void allocateMemory_clampsTooLargeMb() {
        MemoryResponse response = service.allocateMemory(1024);
        assertThat(response.allocatedMb()).isEqualTo(PerformenceTestService.MAX_MEMORY_MB);
    }

    @Test
    void allocateMemory_clampsZeroOrNegative() {
        MemoryResponse response = service.allocateMemory(0);
        assertThat(response.allocatedMb()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // Ping
    // -------------------------------------------------------------------------

    @Test
    void ping_returnsExpectedFields() {
        var response = service.ping();
        assertThat(response.message()).isEqualTo("pong");
        assertThat(response.serverTimestampEpochMs()).isGreaterThan(0);
        assertThat(response.javaVersion()).isNotBlank();
    }
}
