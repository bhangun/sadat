



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import javax.enterprise.context.ApplicationScoped;

/**
 * Strategy for enriching messages with external data
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class EnrichmentStrategy implements AggregationStrategy {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {
        try {
            String originalBody = original.getIn().getBody(String.class);
            String resourceBody = resource.getIn().getBody(String.class);
            
            // Parse both as JSON
            JsonNode originalJson = objectMapper.readTree(originalBody);
            JsonNode resourceJson = objectMapper.readTree(resourceBody);
            
            // Merge strategy - add resource fields to original
            if (originalJson.isObject() && resourceJson.isObject()) {
                ObjectNode mergedNode = (ObjectNode) originalJson;
                resourceJson.fields().forEachRemaining(entry -> {
                    mergedNode.set(entry.getKey(), entry.getValue());
                });
                
                original.getIn().setBody(objectMapper.writeValueAsString(mergedNode));
            }
            
            return original;
            
        } catch (Exception e) {
            log.error("Enrichment failed", e);
            // Return original on error
            return original;
        }
    }
}