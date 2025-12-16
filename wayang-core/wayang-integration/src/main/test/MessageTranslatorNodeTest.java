



import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.integration.test.IntegrationNodeTestBase;
import tech.kayys.wayang.nodes.ExecutionResult;
import tech.kayys.wayang.nodes.NodeContext;

import javax.inject.Inject;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MessageTranslatorNodeTest extends IntegrationNodeTestBase {
    
    @Inject
    MessageTranslatorNode translatorNode;
    
    @Test
    void testCelTransformation() {
        // Arrange
        Map<String, Object> inputs = Map.of(
            "body", "{\"firstName\":\"John\",\"lastName\":\"Doe\"}"
        );
        
        NodeContext context = createTestContext(inputs);
        
        // Act
        ExecutionResult result = translatorNode.execute(context);
        
        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getOutput("result"));
    }
    
    @Test
    void testJsonataTransformation() {
        // Arrange
        Map<String, Object> inputs = Map.of(
            "body", "{\"order\":{\"items\":[{\"price\":10},{\"price\":20}]}}"
        );
        
        NodeContext context = createTestContext(inputs);
        
        // Act
        ExecutionResult result = translatorNode.execute(context);
        
        // Assert
        assertTrue(result.isSuccess());
    }
}