// src/test/java/tech/kayys/wayang/mcp/generator/testing/SecurityTestIntegrationTest.java
package tech.kayys.wayang.mcp.generator.testing;

import tech.kayys.wayang.mcp.model.ApiSpecification;
import tech.kayys.wayang.mcp.model.ApiOperation;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class SecurityTestIntegrationTest {

    private SecurityTestGenerator securityTestGenerator;
    private ApiSpecification mockApiSpec;
    private TestConfiguration testConfig;

    @BeforeEach
    void setUp() {
        securityTestGenerator = new SecurityTestGenerator();
        mockApiSpec = Mockito.mock(ApiSpecification.class);
        testConfig = new TestConfiguration();
    }

    @Test
    void shouldGenerateAndExecuteCompleteSecurityTestSuite() {
        // Given
        when(mockApiSpec.getTitle()).thenReturn("Integration Test API");
        when(mockApiSpec.getOperations()).thenReturn(createSampleOperations());

        TestExecutionContext context = TestExecutionContext.builder()
                .withBaseUrl("http://localhost:8080")
                .withAdminToken("admin-token")
                .withUserToken("user-token")
                .withGuestToken("guest-token")
                .withValidToken("valid-token")
                .withRegularUserToken("regular-user-token")
                .withCurrentUserId("current-user-123")
                .build();

        // When
        SecurityTestSuite testSuite = securityTestGenerator.generateSecurityTests(mockApiSpec, testConfig);
        TestCategoryResult result = securityTestGenerator.executeTests(testSuite, context);

        // Then
        assertNotNull(testSuite);
        assertEquals("Integration Test API Security Tests", testSuite.getName());
        assertTrue(testSuite.getTests().size() > 0, "Should generate security tests");

        assertNotNull(result);
        assertEquals("security", result.getCategory());
        assertTrue(result.getTestsRun() >= 0);
        assertTrue(result.getTestsPassed() >= 0);
        assertTrue(result.getTestsFailed() >= 0);
        assertTrue(result.getTestsRun() == result.getTestsPassed() + result.getTestsFailed());

        // Check that metadata was added
        assertNotNull(result.getMetadata().get("securityScore"));
        assertTrue(result.getMetadata().get("securityScore") instanceof Double);
    }

    @Test
    void shouldIncludeDifferentTypesOfSecurityTests() {
        // Given
        when(mockApiSpec.getTitle()).thenReturn("Diverse Security Test API");
        when(mockApiSpec.getOperations()).thenReturn(createSampleOperations());

        // When
        SecurityTestSuite testSuite = securityTestGenerator.generateSecurityTests(mockApiSpec, testConfig);

        // Then
        assertNotNull(testSuite);
        assertFalse(testSuite.getTests().isEmpty());

        // Check that different categories of tests are present
        boolean hasOwaspTests = testSuite.getTests().stream()
                .anyMatch(test -> test.getCategory().startsWith("OWASP-"));
        boolean hasAuthTests = testSuite.getTests().stream()
                .anyMatch(test -> "Authentication".equals(test.getCategory()) ||
                        "Authorization".equals(test.getCategory()));
        boolean hasInputValidationTests = testSuite.getTests().stream()
                .anyMatch(test -> "Input Validation".equals(test.getCategory()));

        assertTrue(hasOwaspTests, "Should include OWASP API security tests");
        assertTrue(hasAuthTests, "Should include authentication/authorization tests");
        assertTrue(hasInputValidationTests, "Should include input validation tests");
    }

    private List<ApiOperation> createSampleOperations() {
        // Create mock operations for testing
        ApiOperation operation1 = Mockito.mock(ApiOperation.class);
        when(operation1.getOperationId()).thenReturn("getUserById");
        when(operation1.getMethod()).thenReturn("GET");
        when(operation1.getPath()).thenReturn("/users/{id}");

        ApiOperation operation2 = Mockito.mock(ApiOperation.class);
        when(operation2.getOperationId()).thenReturn("createUser");
        when(operation2.getMethod()).thenReturn("POST");
        when(operation2.getPath()).thenReturn("/users");

        ApiOperation operation3 = Mockito.mock(ApiOperation.class);
        when(operation3.getOperationId()).thenReturn("updateUser");
        when(operation3.getMethod()).thenReturn("PUT");
        when(operation3.getPath()).thenReturn("/users/{id}");

        return Arrays.asList(operation1, operation2, operation3);
    }
}