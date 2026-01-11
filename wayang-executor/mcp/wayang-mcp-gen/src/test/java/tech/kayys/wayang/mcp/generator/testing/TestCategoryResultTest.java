// src/test/java/com/example/generator/testing/TestCategoryResultTest.java
package tech.kayys.wayang.mcp.generator.testing;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestCategoryResultTest {

    @Test
    void shouldBuildTestCategoryResultWithAllFields() {
        // Given
        String category = "security";
        long startTime = System.currentTimeMillis() - 1000; // 1 second ago
        long endTime = System.currentTimeMillis();
        int testsRun = 10;
        int testsPassed = 8;
        int testsFailed = 2;
        List<String> failures = Arrays.asList("failure1", "failure2");

        // When
        TestCategoryResult result = TestCategoryResult.builder()
                .withCategory(category)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withTestsRun(testsRun)
                .withTestsPassed(testsPassed)
                .withTestsFailed(testsFailed)
                .withFailures(failures)
                .withSuccess(false)
                .build();

        // Then
        assertEquals(category, result.getCategory());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(testsRun, result.getTestsRun());
        assertEquals(testsPassed, result.getTestsPassed());
        assertEquals(testsFailed, result.getTestsFailed());
        assertEquals(failures, result.getFailures());
        assertFalse(result.isSuccess());
    }

    @Test
    void shouldHandleSuccessfulResult() {
        // When
        TestCategoryResult result = TestCategoryResult.builder()
                .withCategory("integration")
                .withTestsRun(5)
                .withTestsPassed(5)
                .withTestsFailed(0)
                .withSuccess(true)
                .build();

        // Then
        assertEquals("integration", result.getCategory());
        assertEquals(5, result.getTestsRun());
        assertEquals(5, result.getTestsPassed());
        assertEquals(0, result.getTestsFailed());
        assertTrue(result.isSuccess());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    void shouldSupportMetadataAddition() {
        // Given
        TestCategoryResult result = TestCategoryResult.builder()
                .withCategory("performance")
                .withTestsRun(1)
                .withTestsPassed(1)
                .withTestsFailed(0)
                .withSuccess(true)
                .build();

        // When
        result.addMetadata("responseTime", 150L);
        result.addMetadata("throughput", 100);

        // Then
        assertEquals(150L, result.getMetadata().get("responseTime"));
        assertEquals(100, result.getMetadata().get("throughput"));
    }
}