// src/test/java/com/example/generator/testing/TestExecutionContextTest.java
package tech.kayys.wayang.mcp.generator.testing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestExecutionContextTest {

    @Test
    void shouldBuildExecutionContextWithAllFields() {
        // Given
        String baseUrl = "http://test-api.local";
        String adminToken = "admin123";
        String userToken = "user456";
        String guestToken = "guest789";
        String validToken = "valid999";
        String regularUserToken = "regular111";
        String currentUserId = "user-123";

        // When
        TestExecutionContext context = TestExecutionContext.builder()
                .withBaseUrl(baseUrl)
                .withAdminToken(adminToken)
                .withUserToken(userToken)
                .withGuestToken(guestToken)
                .withValidToken(validToken)
                .withRegularUserToken(regularUserToken)
                .withCurrentUserId(currentUserId)
                .build();

        // Then
        assertEquals(baseUrl, context.getBaseUrl());
        assertEquals(adminToken, context.getAdminToken());
        assertEquals(userToken, context.getUserToken());
        assertEquals(guestToken, context.getGuestToken());
        assertEquals(validToken, context.getValidToken());
        assertEquals(regularUserToken, context.getRegularUserToken());
        assertEquals(currentUserId, context.getCurrentUserId());
    }

    @Test
    void shouldHandleNullValuesGracefully() {
        // When
        TestExecutionContext context = TestExecutionContext.builder().build();

        // Then
        assertNull(context.getBaseUrl());
        assertNull(context.getAdminToken());
        assertNull(context.getUserToken());
        assertNull(context.getGuestToken());
        assertNull(context.getValidToken());
        assertNull(context.getRegularUserToken());
        assertNull(context.getCurrentUserId());
    }
}