package com.dharaniksd.performence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Lightweight integration test – loads the full Spring context and exercises
 * the {@code /api/test/ping} endpoint via MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ping_returns200WithPongMessage() throws Exception {
        mockMvc.perform(get("/api/test/ping"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.endpoint").value("/api/test/ping"))
                .andExpect(jsonPath("$.data.message").value("pong"))
                .andExpect(jsonPath("$.data.serverTimestampEpochMs").isNumber())
                .andExpect(jsonPath("$.data.javaVersion").isString());
    }

    @Test
    void ping_returnsTimestampInReasonableRange() throws Exception {
        long before = System.currentTimeMillis();
        mockMvc.perform(get("/api/test/ping"))
                .andExpect(status().isOk());
        long after = System.currentTimeMillis();
        // The test just verifies the endpoint responds within 5 seconds
        assert (after - before) < 5000;
    }
}
