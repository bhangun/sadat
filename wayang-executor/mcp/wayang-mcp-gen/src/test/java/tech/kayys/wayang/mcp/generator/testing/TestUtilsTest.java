// src/test/java/com/example/generator/testing/TestUtilsTest.java
package tech.kayys.wayang.mcp.generator.testing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestUtilsTest {

    @Test
    void shouldGenerateExpiredToken() {
        // When
        String token1 = TestUtils.generateExpiredToken();
        String token2 = TestUtils.generateExpiredToken();

        // Then
        assertNotNull(token1);
        assertNotNull(token2);
        assertTrue(token1.startsWith("expired_token_"));
        assertTrue(token2.startsWith("expired_token_"));
        assertNotEquals(token1, token2); // Should generate different tokens
    }

    @Test
    void shouldGenerateValidTokenFormat() {
        // When
        String token = TestUtils.generateExpiredToken();

        // Then
        assertTrue(token.matches("expired_token_\\d+")); // Should match the pattern
    }
}