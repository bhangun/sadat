// src/test/java/com/example/generator/testing/SecurityTestResultTest.java
package tech.kayys.wayang.mcp.generator.testing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityTestResultTest {

    @Test
    void shouldBuildSecurityTestResultWithAllFields() {
        // Given
        String testName = "Test Name";
        String category = "Test Category";
        boolean success = true;
        String failureReason = "Some reason";

        // When
        SecurityTestResult result = SecurityTestResult.builder()
                .withTestName(testName)
                .withCategory(category)
                .withSuccess(success)
                .withFailureReason(failureReason)
                .addSecurityIssue("Issue 1")
                .addSecurityIssue("Issue 2")
                .build();

        // Then
        assertEquals(testName, result.getTestName());
        assertEquals(category, result.getCategory());
        assertEquals(success, result.isSuccess());
        assertEquals(failureReason, result.getFailureReason());
        assertTrue(result.hasSecurityIssues());
        assertEquals(2, result.getSecurityIssues().size());
        assertTrue(result.getSecurityIssues().contains("Issue 1"));
        assertTrue(result.getSecurityIssues().contains("Issue 2"));
    }

    @Test
    void shouldHandleSuccessfulTestWithoutIssues() {
        // When
        SecurityTestResult result = SecurityTestResult.builder()
                .withTestName("successful-test")
                .withCategory("category")
                .withSuccess(true)
                .build();

        // Then
        assertEquals("successful-test", result.getTestName());
        assertEquals("category", result.getCategory());
        assertTrue(result.isSuccess());
        assertFalse(result.hasSecurityIssues());
        assertTrue(result.getSecurityIssues().isEmpty());
        assertNull(result.getFailureReason());
    }

    @Test
    void shouldHandleFailedTestWithoutIssues() {
        // When
        SecurityTestResult result = SecurityTestResult.builder()
                .withTestName("failed-test")
                .withCategory("category")
                .withSuccess(false)
                .withFailureReason("Test failed for some reason")
                .build();

        // Then
        assertEquals("failed-test", result.getTestName());
        assertEquals("category", result.getCategory());
        assertFalse(result.isSuccess());
        assertFalse(result.hasSecurityIssues());
        assertTrue(result.getSecurityIssues().isEmpty());
        assertEquals("Test failed for some reason", result.getFailureReason());
    }
}