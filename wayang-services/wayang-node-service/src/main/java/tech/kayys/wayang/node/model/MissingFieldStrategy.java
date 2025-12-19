package tech.kayys.wayang.workflow.model;

import java.util.HashMap;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.schema.ErrorPayload;
import tech.kayys.wayang.schema.NodeDefinition;
import tech.kayys.wayang.schema.ui.PortDescriptor;

/**
 * Example: Missing required field healing strategy.
 */
@ApplicationScoped
class MissingFieldStrategy implements HealingStrategy {

    @Override
    public boolean canHandle(String errorMessage) {
        return errorMessage.contains("required") &&
                errorMessage.contains("missing");
    }

    @Override
    public Uni<HealedContext> heal(
            NodeDefinition nodeDef,
            ExecutionContext context,
            ErrorPayload error) {

        // Extract missing field name from error message
        String fieldName = extractFieldName(error.getMessage());

        if (fieldName == null) {
            return Uni.createFrom().item(
                    HealedContext.failed("Could not extract field name"));
        }

        // Find default value from schema
        PortDescriptor inputPort = nodeDef.getInputs().stream()
                .filter(p -> p.getName().equals(fieldName))
                .findFirst()
                .orElse(null);

        if (inputPort == null || inputPort.getData().getDefaultValue() == null) {
            return Uni.createFrom().item(
                    HealedContext.failed("No default value available for field: " + fieldName));
        }

        // Create fixed input with default value
        NodeContext nodeContext = context.createNodeContext(nodeDef);
        Map<String, Object> fixedInput = new HashMap<>(nodeContext.getInputs());
        fixedInput.put(fieldName, inputPort.getData().getDefaultValue());

        return Uni.createFrom().item(
                HealedContext.success(
                        fixedInput,
                        "Added missing field '" + fieldName + "' with default value"));
    }

    private String extractFieldName(String errorMessage) {
        // Simple pattern matching - would be more sophisticated in production
        String[] parts = errorMessage.split("'");
        return parts.length > 1 ? parts[1] : null;
    }
}
