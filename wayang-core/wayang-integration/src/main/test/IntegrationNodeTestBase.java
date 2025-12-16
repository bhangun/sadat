
import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import tech.kayys.wayang.config.NodeConfig;
import tech.kayys.wayang.nodes.NodeContext;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for integration node tests
 */
@QuarkusTest
public abstract class IntegrationNodeTestBase {
    
    @Inject
    protected CamelContext camelContext;
    
    protected ProducerTemplate producerTemplate;
    
    @BeforeEach
    public void setup() {
        producerTemplate = camelContext.createProducerTemplate();
    }
    
    protected NodeContext createTestContext(Map<String, Object> inputs) {
        return NodeContext.builder()
            .runId(UUID.randomUUID().toString())
            .nodeId("test-node")
            .workflowId("test-workflow")
            .tenantId("test-tenant")
            .userId("test-user")
            .inputs(inputs)
            .variables(new HashMap<>())
            .metadata(new HashMap<>())
            .traceId(UUID.randomUUID().toString())
            .build();
    }
    
    protected NodeConfig createTestConfig(String type, Map<String, Object> configData) {
        NodeConfig config = new NodeConfig();
        config.setId("test-node-" + UUID.randomUUID());
        config.setType(type);
        // TODO: Set config JSON from configData map
        return config;
    }
}