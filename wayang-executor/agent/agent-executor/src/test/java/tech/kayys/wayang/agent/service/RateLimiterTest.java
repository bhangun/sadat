package tech.kayys.wayang.agent.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.dto.RateLimitStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class RateLimiterTest {

    @Inject
    RateLimiter rateLimiter;

    @Test
    void testRateLimit() {
        String key = "test-key";

        // Allowed
        assertTrue(rateLimiter.allowRequest(key));

        // Get status
        RateLimitStatus status = rateLimiter.getStatus(key);
        assertEquals(1, status.totalRequests());

        // Reset
        rateLimiter.reset(key);
        status = rateLimiter.getStatus(key);
        assertEquals(0, status.totalRequests());
    }
}
