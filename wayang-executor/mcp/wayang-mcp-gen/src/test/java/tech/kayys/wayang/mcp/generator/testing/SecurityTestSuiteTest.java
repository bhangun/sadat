// src/test/java/com/example/generator/testing/SecurityTestSuiteTest.java
package tech.kayys.wayang.mcp.generator.testing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityTestSuiteTest {

    @Test
    void shouldBuildTestSuiteWithAllFields() {
        // Given
        String name = "Test Suite";
        String description = "Test Description";
        SecurityTest test = SecurityTest.builder()
                .withName("test-name")
                .withDescription("test-desc")
                .withCategory("test-category")
                .withTestCode("test-code")
                .build();

        // When
        SecurityTestSuite suite = SecurityTestSuite.builder()
                .withName(name)
                .withDescription(description)
                .addTest(test)
                .build();

        // Then
        assertEquals(name, suite.getName());
        assertEquals(description, suite.getDescription());
        assertEquals(1, suite.getTests().size());
        assertEquals(test, suite.getTests().get(0));
    }

    @Test
    void shouldHandleEmptyTestsList() {
        // When
        SecurityTestSuite suite = SecurityTestSuite.builder()
                .withName("Empty Suite")
                .withDescription("Has no tests")
                .build();

        // Then
        assertEquals("Empty Suite", suite.getName());
        assertEquals("Has no tests", suite.getDescription());
        assertTrue(suite.getTests().isEmpty());
    }

    @Test
    void shouldAddMultipleTests() {
        // Given
        SecurityTest test1 = SecurityTest.builder()
                .withName("test1")
                .withDescription("desc1")
                .withCategory("category1")
                .withTestCode("code1")
                .build();

        SecurityTest test2 = SecurityTest.builder()
                .withName("test2")
                .withDescription("desc2")
                .withCategory("category2")
                .withTestCode("code2")
                .build();

        // When
        SecurityTestSuite suite = SecurityTestSuite.builder()
                .withName("Multi Test Suite")
                .addTest(test1)
                .addTest(test2)
                .build();

        // Then
        assertEquals(2, suite.getTests().size());
        assertEquals(test1, suite.getTests().get(0));
        assertEquals(test2, suite.getTests().get(1));
    }
}