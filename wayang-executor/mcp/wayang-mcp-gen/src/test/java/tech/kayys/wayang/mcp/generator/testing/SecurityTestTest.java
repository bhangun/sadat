// src/test/java/com/example/generator/testing/SecurityTestTest.java
package tech.kayys.wayang.mcp.generator.testing;

import tech.kayys.wayang.mcp.model.ApiOperation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class SecurityTestTest {

    @Test
    void shouldBuildSecurityTestWithAllFields() {
        // Given
        String name = "Test Name";
        String description = "Test Description";
        String category = "Test Category";
        String testCode = "Test Code";
        ApiOperation operation = Mockito.mock(ApiOperation.class);

        // When
        SecurityTest test = SecurityTest.builder()
                .withName(name)
                .withDescription(description)
                .withCategory(category)
                .withTestCode(testCode)
                .withOperation(operation)
                .build();

        // Then
        assertEquals(name, test.getName());
        assertEquals(description, test.getDescription());
        assertEquals(category, test.getCategory());
        assertEquals(testCode, test.getTestCode());
        assertEquals(operation, test.getOperation());
    }

    @Test
    void shouldHandleMinimalFields() {
        // When
        SecurityTest test = SecurityTest.builder()
                .withName("minimal-test")
                .build();

        // Then
        assertEquals("minimal-test", test.getName());
        assertNull(test.getDescription());
        assertNull(test.getCategory());
        assertNull(test.getTestCode());
        assertNull(test.getOperation());
    }
}