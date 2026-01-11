package tech.kayys.wayang.mcp.security;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PiiRedactorTest {

    private PiiRedactor piiRedactor;

    @BeforeEach
    void setUp() {
        piiRedactor = new PiiRedactor();
    }

    @Test
    void testRedactStringsMatchingPatterns() {
        Map<String, Object> data = new HashMap<>();
        data.put("email", "user@example.com");
        data.put("phone", "123-456-7890");
        data.put("name", "John Doe");

        Set<String> patterns = Set.of(
                "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", // Email pattern
                "\\d{3}-\\d{3}-\\d{4}" // Phone pattern
        );

        Map<String, Object> redacted = piiRedactor.redact(data, patterns);

        assertEquals("[REDACTED]", redacted.get("email"));
        assertEquals("[REDACTED]", redacted.get("phone"));
        assertEquals("John Doe", redacted.get("name")); // Not matching any pattern
    }

    @Test
    void testHandleMultiplePatterns() {
        Map<String, Object> data = new HashMap<>();
        data.put("text", "Contact me at user@example.com or call 123-456-7890");

        Set<String> patterns = Set.of(
                "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b",
                "\\d{3}-\\d{3}-\\d{4}");

        Map<String, Object> redacted = piiRedactor.redact(data, patterns);

        String result = (String) redacted.get("text");
        assertTrue(result.contains("[REDACTED]"));
        assertFalse(result.contains("user@example.com"));
        assertFalse(result.contains("123-456-7890"));
    }

    @Test
    void testPreserveNonMatchingData() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", "12345");
        data.put("status", "active");
        data.put("count", 42);

        Set<String> patterns = Set.of("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

        Map<String, Object> redacted = piiRedactor.redact(data, patterns);

        assertEquals("12345", redacted.get("id"));
        assertEquals("active", redacted.get("status"));
        assertEquals(42, redacted.get("count")); // Non-string values preserved
    }

    @Test
    void testHandleEmptyPatterns() {
        Map<String, Object> data = new HashMap<>();
        data.put("email", "user@example.com");
        data.put("name", "John Doe");

        Map<String, Object> redacted = piiRedactor.redact(data, Set.of());

        assertEquals("user@example.com", redacted.get("email"));
        assertEquals("John Doe", redacted.get("name"));
    }

    @Test
    void testHandleNullPatterns() {
        Map<String, Object> data = new HashMap<>();
        data.put("email", "user@example.com");
        data.put("name", "John Doe");

        Map<String, Object> redacted = piiRedactor.redact(data, null);

        assertEquals("user@example.com", redacted.get("email"));
        assertEquals("John Doe", redacted.get("name"));
    }

    @Test
    void testHandleEmptyData() {
        Map<String, Object> data = new HashMap<>();
        Set<String> patterns = Set.of("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

        Map<String, Object> redacted = piiRedactor.redact(data, patterns);

        assertTrue(redacted.isEmpty());
    }

    @Test
    void testRedactMultipleOccurrencesInSameString() {
        Map<String, Object> data = new HashMap<>();
        data.put("text", "Emails: user1@example.com and user2@example.com");

        Set<String> patterns = Set.of("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

        Map<String, Object> redacted = piiRedactor.redact(data, patterns);

        String result = (String) redacted.get("text");
        assertEquals("Emails: [REDACTED] and [REDACTED]", result);
    }

    @Test
    void testPreserveNonStringValues() {
        Map<String, Object> data = new HashMap<>();
        data.put("count", 100);
        data.put("active", true);
        data.put("price", 99.99);
        data.put("items", java.util.List.of("item1", "item2"));

        Set<String> patterns = Set.of("\\d+");

        Map<String, Object> redacted = piiRedactor.redact(data, patterns);

        // Non-string values should be preserved as-is
        assertEquals(100, redacted.get("count"));
        assertEquals(true, redacted.get("active"));
        assertEquals(99.99, redacted.get("price"));
        assertEquals(java.util.List.of("item1", "item2"), redacted.get("items"));
    }

    @Test
    void testSsnRedaction() {
        Map<String, Object> data = new HashMap<>();
        data.put("ssn", "123-45-6789");
        data.put("text", "SSN is 987-65-4321");

        Set<String> patterns = Set.of("\\d{3}-\\d{2}-\\d{4}");

        Map<String, Object> redacted = piiRedactor.redact(data, patterns);

        assertEquals("[REDACTED]", redacted.get("ssn"));
        assertEquals("SSN is [REDACTED]", redacted.get("text"));
    }
}
