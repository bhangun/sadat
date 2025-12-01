package tech.kayys.wayang.node.core.validation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tech.kayys.wayang.node.core.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
@DisplayName("SchemaValidator Tests")
class SchemaValidatorTest {
    
    @Inject
    SchemaValidator schemaValidator;
    
    private NodeDescriptor testDescriptor;
    private NodeContext testContext;
    
    @BeforeEach
    void setUp() {
        testDescriptor = createTestDescriptor();
        testContext = createTestContext();
    }
    
    @Test
    @DisplayName("Should validate correct descriptor")
    void shouldValidateCorrectDescriptor() {
        // When: validating valid descriptor
        ValidationResult result = schemaValidator.validateDescriptor(testDescriptor);
        
        // Then: should be valid
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }
    
    @Test
    @DisplayName("Should reject descriptor with missing required fields")
    void shouldRejectDescriptorWithMissingFields() {
        // Given: descriptor with null ID
        NodeDescriptor invalidDescriptor = new NodeDescriptor(
            null, // Missing ID
            "Test Node",
            "1.0.0",
            List.of(),
            List.of(),
            List.of(),
            new ImplementationDescriptor(
                ImplementationType.MAVEN,
                "test:test:1.0.0",
                "sha256:abc",
                Map.of()
            ),
            List.of(),
            List.of(),
            SandboxLevel.TRUSTED,
            null,
            Map.of(),
            "checksum",
            null,
            "test",
            Instant.now(),
            NodeStatus.APPROVED
        );
        
        // When/Then: should fail validation
        assertThatThrownBy(() -> schemaValidator.validateDescriptor(invalidDescriptor))
            .isInstanceOf(NullPointerException.class);
    }
    
    @Test
    @DisplayName("Should validate required inputs present")
    void shouldValidateRequiredInputsPresent() {
        // Given: context with required input
        testContext.setVariable("requiredInput", "test value");
        
        // When: validating inputs
        ValidationResult result = schemaValidator.validateInputs(testDescriptor, testContext);
        
        // Then: should be valid
        assertThat(result.valid()).isTrue();
    }
    
    @Test
    @DisplayName("Should reject missing required inputs")
    void shouldRejectMissingRequiredInputs() {
        // Given: context without required input
        // (testContext has no variables set)
        
        // When: validating inputs
        ValidationResult result = schemaValidator.validateInputs(testDescriptor, testContext);
        
        // Then: should have errors
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors().get(0).code()).isEqualTo("required");
    }
    
    @Test
    @DisplayName("Should validate outputs against schema")
    void shouldValidateOutputsAgainstSchema() {
        // Given: valid outputs
        Map<String, Object> outputs = Map.of(
            "requiredOutput", "test output value"
        );
        
        // When: validating outputs
        ValidationResult result = schemaValidator.validateOutputs(
            testDescriptor, 
            testContext, 
            outputs
        );
        
        // Then: should be valid
        assertThat(result.valid()).isTrue();
    }
    
    @Test
    @DisplayName("Should validate against JSON schema")
    void shouldValidateAgainstJsonSchema() {
        // Given: descriptor with JSON schema
        NodeDescriptor descriptorWithSchema = createDescriptorWithJsonSchema();
        testContext.setVariable("schemaInput", Map.of(
            "name", "John Doe",
            "age", 30
        ));
        
        // When: validating inputs
        ValidationResult result = schemaValidator.validateInputs(descriptorWithSchema, testContext);
        
        // Then: should be valid
        assertThat(result.valid()).isTrue();
    }
    
    @Test
    @DisplayName("Should reject invalid schema data")
    void shouldRejectInvalidSchemaData() {
        // Given: descriptor with schema and invalid data
        NodeDescriptor descriptorWithSchema = createDescriptorWithJsonSchema();
        testContext.setVariable("schemaInput", Map.of(
            "name", "John Doe",
            "age", "not a number" // Invalid type
        ));
        
        // When: validating inputs
        ValidationResult result = schemaValidator.validateInputs(descriptorWithSchema, testContext);
        
        // Then: should have errors
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }
    
    // Helper methods
    
    private NodeDescriptor createTestDescriptor() {
        return new NodeDescriptor(
            "test/validator-node",
            "Validator Test Node",
            "1.0.0",
            List.of(
                new PortDescriptor("requiredInput", "string", true, null, "Required input", Map.of())
            ),
            List.of(
                new PortDescriptor("requiredOutput", "string", true, null, "Required output", Map.of())
            ),
            List.of(),
            new ImplementationDescriptor(
                ImplementationType.MAVEN,
                "test:validator:1.0.0",
                "sha256:abc",
                Map.of()
            ),
            List.of(),
            List.of(),
            SandboxLevel.TRUSTED,
            null,
            Map.of("mainClass", "com.example.ValidatorNode"),
            "checksum",
            null,
            "test",
            Instant.now(),
            NodeStatus.APPROVED
        );
    }
    
    private NodeDescriptor createDescriptorWithJsonSchema() {
        Map<String, Object> schema = Map.of(
            "type", "object",
            "properties", Map.of(
                "name", Map.of("type", "string"),
                "age", Map.of("type", "integer", "minimum", 0)
            ),
            "required", List.of("name", "age")
        );
        
        return new NodeDescriptor(
            "test/schema-node",
            "Schema Test Node",
            "1.0.0",
            List.of(
                new PortDescriptor("schemaInput", "object", true, null, "Schema input", schema)
            ),
            List.of(),
            List.of(),
            new ImplementationDescriptor(
                ImplementationType.MAVEN,
                "test:schema:1.0.0",
                "sha256:abc",
                Map.of()
            ),
            List.of(),
            List.of(),
            SandboxLevel.TRUSTED,
            null,
            Map.of("mainClass", "com.example.SchemaNode"),
            "checksum",
            null,
            "test",
            Instant.now(),
            NodeStatus.APPROVED
        );
    }
    
    private NodeContext createTestContext() {
        return new NodeContext.Builder()
            .runId("test-run-123")
            .nodeId("test-node-456")
            .tenantId("test-tenant")
            .traceId("test-trace-789")
            .build();
    }
}