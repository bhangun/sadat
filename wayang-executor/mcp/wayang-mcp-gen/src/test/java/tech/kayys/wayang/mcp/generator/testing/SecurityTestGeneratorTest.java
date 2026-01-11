// src/test/java/tech/kayys/wayang/mcp/generator/testing/SecurityTestGeneratorTest.java
package tech.kayys.wayang.mcp.generator.testing;

import tech.kayys.wayang.mcp.model.ApiSpecification;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class SecurityTestGeneratorTest {

    @Mock
    private ApiSpecification apiSpec;

    private SecurityTestGenerator securityTestGenerator;
    private TestConfiguration testConfig;

    @BeforeEach
    void setUp() {
        securityTestGenerator = new SecurityTestGenerator();
        testConfig = new TestConfiguration();
    }

    @Test
    void shouldGenerateSecurityTestsForApiSpecification() {
        // Given
        when(apiSpec.getTitle()).thenReturn("Test API");

        // When
        SecurityTestSuite testSuite = securityTestGenerator.generateSecurityTests(apiSpec, testConfig);

        // Then
        assertNotNull(testSuite);
        assertEquals("Test API Security Tests", testSuite.getName());
        assertTrue(testSuite.getTests().size() > 0, "Should generate at least one security test");
    }

    @Test
    void shouldExecuteSecurityTestsAndReturnResults() {
        // Given
        when(apiSpec.getTitle()).thenReturn("Test API");
        SecurityTestSuite testSuite = securityTestGenerator.generateSecurityTests(apiSpec, testConfig);

        TestExecutionContext context = TestExecutionContext.builder()
                .withBaseUrl("http://localhost:8080")
                .withAdminToken("admin-token")
                .withUserToken("user-token")
                .withGuestToken("guest-token")
                .withValidToken("valid-token")
                .withRegularUserToken("regular-user-token")
                .withCurrentUserId("123")
                .build();

        // When
        TestCategoryResult result = securityTestGenerator.executeTests(testSuite, context);

        // Then
        assertNotNull(result);
        assertEquals("security", result.getCategory());
        assertTrue(result.getTestsRun() >= result.getTestsPassed()); // Some may fail in simulation
        assertNotNull(result.getMetadata().get("securityScore"));
    }

    @Test
    void shouldCalculateSecurityScoreCorrectly() {
        // This test verifies the private method indirectly by checking the result
        when(apiSpec.getTitle()).thenReturn("Scoring Test API");
        SecurityTestSuite testSuite = securityTestGenerator.generateSecurityTests(apiSpec, testConfig);

        TestExecutionContext context = TestExecutionContext.builder()
                .withBaseUrl("http://localhost:8080")
                .build();

        TestCategoryResult result = securityTestGenerator.executeTests(testSuite, context);
        Object securityScore = result.getMetadata().get("securityScore");

        assertNotNull(securityScore);
        assertTrue(securityScore instanceof Double);
        double score = (Double) securityScore;
        assertTrue(score >= 0 && score <= 100, "Security score should be between 0 and 100");
    }
}