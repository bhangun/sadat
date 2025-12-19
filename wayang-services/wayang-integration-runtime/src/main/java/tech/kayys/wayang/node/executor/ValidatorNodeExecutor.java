package tech.kayys.wayang.node.executor;

import io.quarkus.ai.agent.runtime.executor.NodeExecutor;
import io.quarkus.ai.agent.runtime.model.Workflow;
import io.quarkus.ai.agent.runtime.context.ExecutionContext;
import io.quarkus.ai.agent.runtime.engine.WorkflowRuntimeEngine.NodeExecutionResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * VALIDATOR node executor
 */
@ApplicationScoped
public class ValidatorNodeExecutor implements NodeExecutor {

    private static final Logger LOG = Logger.getLogger(ValidatorNodeExecutor.class);

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        Workflow.Node.NodeConfig.ValidationConfig config = node.getConfig().getValidationConfig();

        if (config == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Validation config is required"));
        }

        // Get input to validate
        Object input = context.getVariable("input");
        Map<String, Object> output = new HashMap<>();

        try {
            // Validate against schema
            boolean isValid = validateInput(input, config.getSchema());

            output.put("isValid", isValid);
            output.put("validatedInput", input);

            if (!isValid) {
                return handleValidationFailure(node, config, output);
            }

            return Uni.createFrom().item(
                    new NodeExecutionResult(node.getId(), true, output, null));

        } catch (Exception e) {
            LOG.errorf(e, "Validation failed for node: %s", node.getName());
            return handleValidationFailure(node, config, output);
        }
    }

    private Uni<NodeExecutionResult> handleValidationFailure(
            Workflow.Node node,
            Workflow.Node.NodeConfig.ValidationConfig config,
            Map<String, Object> output) {

        String strategy = config.getOnFailure() != null ? config.getOnFailure() : "stop";

        switch (strategy.toLowerCase()) {
            case "skip":
                LOG.infof("Skipping validation failure for node: %s", node.getName());
                output.put("isValid", false);
                output.put("skipped", true);
                return Uni.createFrom().item(
                        new NodeExecutionResult(node.getId(), true, output, null));

            case "fallback":
                LOG.infof("Using fallback value for node: %s", node.getName());
                output.put("isValid", false);
                output.put("validatedInput", config.getFallbackValue());
                return Uni.createFrom().item(
                        new NodeExecutionResult(node.getId(), true, output, null));

            case "retry":
                // Retry is handled at workflow engine level
                return Uni.createFrom().failure(
                        new IllegalArgumentException("Validation failed - retry requested"));

            default: // stop
                return Uni.createFrom().failure(
                        new IllegalArgumentException("Validation failed"));
        }
    }

    private boolean validateInput(Object input, Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return true;
        }

        // Type validation
        if (schema.containsKey("type")) {
            String expectedType = (String) schema.get("type");
            if (!validateType(input, expectedType)) {
                return false;
            }
        }

        // String validations
        if (input instanceof String) {
            String str = (String) input;

            if (schema.containsKey("minLength")) {
                int minLength = (int) schema.get("minLength");
                if (str.length() < minLength)
                    return false;
            }

            if (schema.containsKey("maxLength")) {
                int maxLength = (int) schema.get("maxLength");
                if (str.length() > maxLength)
                    return false;
            }

            if (schema.containsKey("pattern")) {
                String pattern = (String) schema.get("pattern");
                if (!str.matches(pattern))
                    return false;
            }
        }

        // Number validations
        if (input instanceof Number) {
            double num = ((Number) input).doubleValue();

            if (schema.containsKey("minimum")) {
                double min = ((Number) schema.get("minimum")).doubleValue();
                if (num < min)
                    return false;
            }

            if (schema.containsKey("maximum")) {
                double max = ((Number) schema.get("maximum")).doubleValue();
                if (num > max)
                    return false;
            }
        }

        // Array validations
        if (input instanceof List) {
            List<?> list = (List<?>) input;

            if (schema.containsKey("minItems")) {
                int minItems = (int) schema.get("minItems");
                if (list.size() < minItems)
                    return false;
            }

            if (schema.containsKey("maxItems")) {
                int maxItems = (int) schema.get("maxItems");
                if (list.size() > maxItems)
                    return false;
            }
        }

        return true;
    }

    private boolean validateType(Object input, String expectedType) {
        return switch (expectedType.toLowerCase()) {
            case "string" -> input instanceof String;
            case "number" -> input instanceof Number;
            case "boolean" -> input instanceof Boolean;
            case "array" -> input instanceof List;
            case "object" -> input instanceof Map;
            default -> true;
        };
    }

    @Override
    public NodeType getSupportedType() {
        return Workflow.Node.NodeType.VALIDATOR;
    }
}