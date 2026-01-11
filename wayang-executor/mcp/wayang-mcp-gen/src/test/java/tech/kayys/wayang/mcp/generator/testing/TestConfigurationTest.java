// src/test/java/com/example/generator/testing/TestConfigurationTest.java
package tech.kayys.wayang.mcp.generator.testing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestConfigurationTest {

    @Test
    void shouldHaveDefaultValues() {
        // Given
        TestConfiguration config = new TestConfiguration();

        // Then
        assertTrue(config.isEnableContractTesting());
        assertTrue(config.isEnableLoadTesting());
        assertTrue(config.isEnableSecurityTesting());
        assertTrue(config.isEnableIntegrationTesting());
        assertFalse(config.isEnableChaosTesting());

        assertEquals(List.of("pact", "openapi"), config.getContractTestFramework());
        assertEquals(List.of("jmeter", "k6"), config.getLoadTestFramework());
        assertEquals(10, config.getLoadTestUsers());
        assertEquals(60, config.getLoadTestDurationSeconds());
        assertEquals(10, config.getLoadTestRampUpSeconds());

        assertEquals(List.of("OWASP-API", "Authentication", "Authorization"), config.getSecurityTestCategories());
        assertFalse(config.isEnablePenetrationTesting());

        assertEquals("staging", config.getTestEnvironment());
        assertTrue(config.isEnableDatabaseTesting());
        assertTrue(config.isEnableExternalServiceTesting());

        assertEquals(List.of("network-latency", "service-failure", "resource-exhaustion"),
                config.getChaosExperiments());
    }

    @Test
    void shouldAllowConfigurationChanges() {
        // Given
        TestConfiguration config = new TestConfiguration();

        // When
        config.setEnableContractTesting(false);
        config.setLoadTestUsers(50);
        config.setTestEnvironment("production");

        // Then
        assertFalse(config.isEnableContractTesting());
        assertEquals(50, config.getLoadTestUsers());
        assertEquals("production", config.getTestEnvironment());
    }
}