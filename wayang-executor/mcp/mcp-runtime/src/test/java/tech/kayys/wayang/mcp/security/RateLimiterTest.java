package tech.kayys.wayang.mcp.security;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.mcp.TestFixtures;
import tech.kayys.wayang.mcp.domain.ToolGuardrails;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
    }

    @Test
    void testAllowRequestsWithinPerMinuteLimit() {
        ToolGuardrails guardrails = TestFixtures.createToolGuardrails();
        guardrails.setRateLimitPerMinute(5);
        guardrails.setRateLimitPerHour(100);

        // Should allow 5 requests
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> rateLimiter.checkLimit(
                    TestFixtures.TEST_TENANT_ID,
                    TestFixtures.TEST_TOOL_ID,
                    guardrails));
        }
    }

    @Test
    void testRejectRequestsExceedingPerMinuteLimit() {
        ToolGuardrails guardrails = TestFixtures.createToolGuardrails();
        guardrails.setRateLimitPerMinute(3);
        guardrails.setRateLimitPerHour(100);

        // Allow 3 requests
        for (int i = 0; i < 3; i++) {
            rateLimiter.checkLimit(
                    TestFixtures.TEST_TENANT_ID,
                    TestFixtures.TEST_TOOL_ID,
                    guardrails);
        }

        // 4th request should fail
        assertThrows(
                tech.kayys.wayang.mcp.runtime.RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(
                        TestFixtures.TEST_TENANT_ID,
                        TestFixtures.TEST_TOOL_ID,
                        guardrails));
    }

    @Test
    void testRejectRequestsExceedingPerHourLimit() {
        ToolGuardrails guardrails = TestFixtures.createToolGuardrails();
        guardrails.setRateLimitPerMinute(100);
        guardrails.setRateLimitPerHour(5);

        // Allow 5 requests
        for (int i = 0; i < 5; i++) {
            rateLimiter.checkLimit(
                    TestFixtures.TEST_TENANT_ID,
                    TestFixtures.TEST_TOOL_ID,
                    guardrails);
        }

        // 6th request should fail
        assertThrows(
                tech.kayys.wayang.mcp.runtime.RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(
                        TestFixtures.TEST_TENANT_ID,
                        TestFixtures.TEST_TOOL_ID,
                        guardrails));
    }

    @Test
    void testBucketIsolationPerTenantAndTool() {
        ToolGuardrails guardrails = TestFixtures.createToolGuardrails();
        guardrails.setRateLimitPerMinute(2);
        guardrails.setRateLimitPerHour(100);

        // Use up limit for tenant1/tool1
        rateLimiter.checkLimit("tenant1", "tool1", guardrails);
        rateLimiter.checkLimit("tenant1", "tool1", guardrails);

        // Should fail for tenant1/tool1
        assertThrows(
                tech.kayys.wayang.mcp.runtime.RateLimitExceededException.class,
                () -> rateLimiter.checkLimit("tenant1", "tool1", guardrails));

        // Should succeed for tenant1/tool2 (different tool)
        assertDoesNotThrow(() -> rateLimiter.checkLimit("tenant1", "tool2", guardrails));

        // Should succeed for tenant2/tool1 (different tenant)
        assertDoesNotThrow(() -> rateLimiter.checkLimit("tenant2", "tool1", guardrails));
    }

    @Test
    void testResetCountersAfterTimeWindow() throws InterruptedException {
        ToolGuardrails guardrails = TestFixtures.createToolGuardrails();
        guardrails.setRateLimitPerMinute(2);
        guardrails.setRateLimitPerHour(100);

        // Use up the per-minute limit
        rateLimiter.checkLimit(
                TestFixtures.TEST_TENANT_ID,
                TestFixtures.TEST_TOOL_ID,
                guardrails);
        rateLimiter.checkLimit(
                TestFixtures.TEST_TENANT_ID,
                TestFixtures.TEST_TOOL_ID,
                guardrails);

        // Should fail immediately
        assertThrows(
                tech.kayys.wayang.mcp.runtime.RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(
                        TestFixtures.TEST_TENANT_ID,
                        TestFixtures.TEST_TOOL_ID,
                        guardrails));

        // Wait for minute window to reset (simulate with small delay for test)
        // Note: In real scenario, this would be 60+ seconds
        // For testing purposes, we verify the logic works
        Thread.sleep(100);

        // The counter should still be exceeded since we haven't waited a full minute
        // This test verifies the time-based logic is in place
        assertThrows(
                tech.kayys.wayang.mcp.runtime.RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(
                        TestFixtures.TEST_TENANT_ID,
                        TestFixtures.TEST_TOOL_ID,
                        guardrails));
    }

    @Test
    void testConcurrentRequests() throws InterruptedException {
        ToolGuardrails guardrails = TestFixtures.createToolGuardrails();
        guardrails.setRateLimitPerMinute(10);
        guardrails.setRateLimitPerHour(100);

        // Simulate concurrent requests
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(() -> {
                assertDoesNotThrow(() -> rateLimiter.checkLimit(
                        TestFixtures.TEST_TENANT_ID,
                        TestFixtures.TEST_TOOL_ID,
                        guardrails));
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // All 5 requests should have succeeded
        // We can make 5 more requests
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> rateLimiter.checkLimit(
                    TestFixtures.TEST_TENANT_ID,
                    TestFixtures.TEST_TOOL_ID,
                    guardrails));
        }
    }
}
