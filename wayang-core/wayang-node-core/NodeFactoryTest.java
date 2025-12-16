package tech.kayys.wayang.node.core.factory;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tech.kayys.wayang.node.core.Node;
import tech.kayys.wayang.node.core.exception.NodeFactoryException;
import tech.kayys.wayang.node.core.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@DisplayName("NodeFactory Tests")
class NodeFactoryTest {
    
    @Inject
    NodeFactoryRegistry factoryRegistry;
    
    private NodeDescriptor testDescriptor;
    
    @BeforeEach
    void setUp() {
        testDescriptor = createTestDescriptor();
    }
    
    @Test
    @DisplayName("Should create node from valid descriptor")
    void shouldCreateNodeFromValidDescriptor() throws NodeFactoryException {
        // Given: valid descriptor
        
        // When: creating node
        Node node = factoryRegistry.createNode(testDescriptor, false);
        
        // Then: node should be created
        assertThat(node).isNotNull();
        assertThat(node.getDescriptor()).isEqualTo(testDescriptor);
    }
    
    @Test
    @DisplayName("Should cache nodes when caching enabled")
    void shouldCacheNodesWhenCachingEnabled() throws NodeFactoryException {
        // Given: caching enabled
        
        // When: creating same node twice
        Node node1 = factoryRegistry.createNode(testDescriptor, true);
        Node node2 = factoryRegistry.createNode(testDescriptor, true);
        
        // Then: should return same instance
        assertThat(node1).isSameAs(node2);
    }
    
    @Test
    @DisplayName("Should not cache nodes when caching disabled")
    void shouldNotCacheNodesWhenCachingDisabled() throws NodeFactoryException {
        // Given: caching disabled
        
        // When: creating same node twice
        Node node1 = factoryRegistry.createNode(testDescriptor, false);
        Node node2 = factoryRegistry.createNode(testDescriptor, false);
        
        // Then: should return different instances
        assertThat(node1).isNotSameAs(node2);
    }
    
    @Test
    @DisplayName("Should throw exception for unsupported implementation type")
    void shouldThrowExceptionForUnsupportedImplementationType() {
        // Given: descriptor with unsupported type
        NodeDescriptor invalidDescriptor = new NodeDescriptor(
            "test/invalid",
            "Invalid Node",
            "1.0.0",
            List.of(),
            List.of(),
            List.of(),
            new ImplementationDescriptor(
                ImplementationType.PYTHON, // Assuming not supported
                "test:module",
                "sha256:abc",
                Map.of()
            ),
            List.of(),
            List.of(),
            SandboxLevel.TRUSTED,
            null,
            Map.of("mainClass", "com.example.TestNode"),
            "checksum",
            null,
            "test",
            Instant.now(),
            NodeStatus.APPROVED
        );
        
        // When/Then: should throw exception
        assertThatThrownBy(() -> factoryRegistry.createNode(invalidDescriptor))
            .isInstanceOf(NodeFactoryException.class)
            .hasMessageContaining("No factory registered");
    }
    
    @Test
    @DisplayName("Should clear cache successfully")
    void shouldClearCacheSuccessfully() throws NodeFactoryException {
        // Given: nodes in cache
        factoryRegistry.createNode(testDescriptor, true);
        
        // When: clearing cache
        factoryRegistry.clearCache();
        
        // Then: cache should be empty
        Map<String, Object> stats = factoryRegistry.getCacheStats();
        assertThat(stats.get("size")).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Should evict specific node from cache")
    void shouldEvictSpecificNodeFromCache() throws NodeFactoryException {
        // Given: node in cache
        factoryRegistry.createNode(testDescriptor, true);
        
        // When: evicting from cache
        factoryRegistry.evictFromCache(testDescriptor.getQualifiedId());
        
        // Then: should create new instance on next call
        Node node1 = factoryRegistry.createNode(testDescriptor, true);
        Node node2 = factoryRegistry.createNode(testDescriptor, true);
        assertThat(node1).isNotSameAs(node2);
    }
    
    @Test
    @DisplayName("Should validate descriptor before creation")
    void shouldValidateDescriptorBeforeCreation() {
        // Given: invalid descriptor (missing mainClass)
        NodeDescriptor invalidDescriptor = new NodeDescriptor(
            "test/invalid",
            "Invalid Node",
            "1.0.0",
            List.of(),
            List.of(),
            List.of(),
            new ImplementationDescriptor(
                ImplementationType.MAVEN,
                "com.example:test:1.0.0",
                "sha256:abc",
                Map.of()
            ),
            List.of(),
            List.of(),
            SandboxLevel.TRUSTED,
            null,
            Map.of(), // Missing mainClass
            "checksum",
            null,
            "test",
            Instant.now(),
            NodeStatus.APPROVED
        );
        
        // When/Then: should throw validation exception
        assertThatThrownBy(() -> factoryRegistry.createNode(invalidDescriptor))
            .isInstanceOf(NodeFactoryException.class);
    }
    
    // Helper methods
    
    private NodeDescriptor createTestDescriptor() {
        return new NodeDescriptor(
            "test/sample-node",
            "Sample Test Node",
            "1.0.0",
            List.of(
                new PortDescriptor("input1", "string", true, null, "Test input", Map.of())
            ),
            List.of(
                new PortDescriptor("output1", "string", true, null, "Test output", Map.of())
            ),
            List.of(
                new PropertyDescriptor("prop1", "string", "default", false, "Test property", Map.of())
            ),
            new ImplementationDescriptor(
                ImplementationType.MAVEN,
                "com.example:test-node:1.0.0",
                "sha256:abcdef123456",
                Map.of()
            ),
            List.of("network", "llm_access"),
            List.of(),
            SandboxLevel.TRUSTED,
            new ResourceProfile("100m", "256Mi", null, 30, Map.of()),
            Map.of("mainClass", "com.example.TestNode"),
            "checksum123",
            "signature123",
            "test-user",
            Instant.now(),
            NodeStatus.APPROVED
        );
    }
}