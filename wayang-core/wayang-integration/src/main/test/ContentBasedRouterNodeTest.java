



import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.integration.test.IntegrationNodeTestBase;
import tech.kayys.wayang.nodes.ExecutionResult;
import tech.kayys.wayang.nodes.NodeContext;

import javax.inject.Inject;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ContentBasedRouterNodeTest extends IntegrationNodeTestBase {
    
    @Inject
    ContentBasedRouterNode routerNode;
    
    @Test
    void testHighPriorityRouting() {
        // Arrange
        Map<String, Object> inputs = Map.of(
            "body", "{\"message\":\"urgent\",\"priority\":\"high\"}",
            "header.priority", "high"
        );
        
        NodeContext context = createTestContext(inputs);
        
        // Act
        ExecutionResult result = routerNode.execute(context);
        
        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getOutputs());
    }
    
    @Test
    void testLowPriorityRouting() {
        // Arrange
        Map<String, Object> inputs = Map.of(
            "body", "{\"message\":\"normal\",\"priority\":\"low\"}",
            "header.priority", "low"
        );
        
        NodeContext context = createTestContext(inputs);
        
        // Act
        ExecutionResult result = routerNode.execute(context);
        
        // Assert
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testUnmatchedRouting() {
        // Arrange
        Map<String, Object> inputs = Map.of(
            "body", "{\"message\":\"test\"}"
        );
        
        NodeContext context = createTestContext(inputs);
        
        // Act
        ExecutionResult result = routerNode.execute(context);
        
        // Assert
        assertTrue(result.isSuccess());
    }
}