package tech.kayys.wayang.workflow.model;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.ErrorPayload;
import tech.kayys.wayang.schema.NodeDefinition;
import tech.kayys.wayang.schema.SchemaValidator;
import tech.kayys.wayang.schema.ValidationResult;
import tech.kayys.wayang.schema.ui.PortDescriptor;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * SelfHealingService - Automatic error correction using LLM and rules.
 * 
 * Responsibilities:
 * - Analyze errors and attempt automatic correction
 * - Use LLM to fix validation errors and malformed data
 * - Apply deterministic repair strategies for known error patterns
 * - Validate fixed inputs against schemas
 * - Track healing success rates for optimization
 * 
 * Design Principles:
 * - Multi-strategy healing (LLM + rules + heuristics)
 * - Schema-aware corrections
 * - Limited healing attempts to prevent loops
 * - Observable healing process for debugging
 * - Fallback to human review on failure
 */
@ApplicationScoped
public class SelfHealingService {

    private static final Logger LOG = Logger.getLogger(SelfHealingService.class);

    @Inject
    ChatLanguageModel llm; // LangChain4j model

    @Inject
    SchemaValidator schemaValidator;

    @Inject
    SelfHealingConfiguration config;

    @Inject
    HealingStrategyRegistry strategyRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Attempt to heal error by fixing input or configuration.
     * 
     * @param nodeDef Node definition
     * @param context Execution context
     * @param error   Error to heal
     * @return HealedContext with fixed input if successful
     */
    public Uni<HealedContext> heal(
            NodeDefinition nodeDef,
            ExecutionContext context,
            ErrorPayload error) {

        LOG.infof("Attempting self-healing for node %s, error type: %s",
                nodeDef.getId(),
                error.getType());

        return Uni.createFrom().deferred(() -> {

            // Check if healing is enabled and error is healable
            if (!config.isEnabled() || !isHealable(error)) {
                return Uni.createFrom().item(
                        HealedContext.failed("Healing not applicable for error type: " + error.getType()));
            }

            // Check max attempts
            int healingAttempts = context.getMetadata("healing_attempts_" + nodeDef.getId(), Integer.class)
                    .orElse(0);

            if (healingAttempts >= config.getMaxHealingAttempts()) {
                return Uni.createFrom().item(
                        HealedContext.failed("Max healing attempts exceeded"));
            }

            // Increment attempt counter
            context.addMetadata("healing_attempts_" + nodeDef.getId(), healingAttempts + 1);

            // Select healing strategy based on error type
            return selectAndApplyStrategy(nodeDef, context, error)
                    .onItem().transformToUni(healed -> {
                        if (healed.isHealed()) {
                            // Validate fixed input against schema
                            return validateHealedInput(nodeDef, healed)
                                    .map(valid -> {
                                        if (!valid) {
                                            return HealedContext.failed(
                                                    "Healed input failed schema validation");
                                        }
                                        return healed;
                                    });
                        }
                        return Uni.createFrom().item(healed);
                    });
        });
    }

    /**
     * Select and apply appropriate healing strategy.
     */
    private Uni<HealedContext> selectAndApplyStrategy(
            NodeDefinition nodeDef,
            ExecutionContext context,
            ErrorPayload error) {

        return switch (error.getType()) {
            case VALIDATION_ERROR -> healValidationError(nodeDef, context, error);
            case LLM_ERROR -> healLLMError(nodeDef, context, error);
            case TOOL_ERROR -> healToolError(nodeDef, context, error);
            default -> Uni.createFrom().item(
                    HealedContext.failed("No healing strategy for error type: " + error.getType()));
        };
    }

    /**
     * Heal validation errors using LLM to fix data format.
     */
    private Uni<HealedContext> healValidationError(
            NodeDefinition nodeDef,
            ExecutionContext context,
            ErrorPayload error) {

        LOG.debug("Attempting LLM-based validation error healing");

        // Get original input
        NodeContext nodeContext = context.createNodeContext(nodeDef);
        Map<String, Object> originalInput = nodeContext.getInputs();

        // Get input schema
        String schemaJson = getInputSchemaAsJson(nodeDef);

        // Build healing prompt
        String prompt = buildValidationHealingPrompt(
                originalInput,
                error,
                schemaJson);

        return Uni.createFrom().completionStage(
                llm.generate(prompt).toCompletionStage()).map(response -> {
                    try {
                        // Parse LLM response as JSON
                        String fixedJson = extractJsonFromResponse(response.content().text());
                        Map<String, Object> fixedInput = objectMapper.readValue(
                                fixedJson,
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                });

                        LOG.debugf("LLM generated fixed input: %s", fixedJson);

                        return HealedContext.success(
                                fixedInput,
                                "Fixed validation error using LLM correction");

                    } catch (Exception e) {
                        LOG.errorf(e, "Failed to parse LLM healing response");
                        return HealedContext.failed("LLM response parsing failed: " + e.getMessage());
                    }
                }).onFailure().recoverWithItem(th -> HealedContext.failed("LLM healing failed: " + th.getMessage()));
    }

    /**
     * Heal LLM errors (malformed output, incomplete response).
     */
    private Uni<HealedContext> healLLMError(
            NodeDefinition nodeDef,
            ExecutionContext context,
            ErrorPayload error) {

        LOG.debug("Attempting LLM error healing");

        // Check if error contains partial output
        String partialOutput = (String) error.getDetails().get("partial_output");
        if (partialOutput == null) {
            return Uni.createFrom().item(
                    HealedContext.failed("No partial output available for healing"));
        }

        // Try to repair incomplete JSON
        if (partialOutput.contains("{") || partialOutput.contains("[")) {
            String repaired = repairJson(partialOutput);
            if (repaired != null) {
                try {
                    Map<String, Object> fixedOutput = objectMapper.readValue(
                            repaired,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                            });

                    return Uni.createFrom().item(
                            HealedContext.success(
                                    fixedOutput,
                                    "Repaired incomplete JSON output"));
                } catch (Exception e) {
                    // Fall through to LLM-based repair
                }
            }
        }

        // Use LLM to complete/fix the output
        String prompt = String.format("""
                The following LLM output is incomplete or malformed:

                %s

                Please complete or fix this output to make it valid.
                Respond ONLY with valid JSON, no explanation.
                """, partialOutput);

        return Uni.createFrom().completionStage(
                llm.generate(prompt).toCompletionStage()).map(response -> {
                    try {
                        String fixedJson = extractJsonFromResponse(response.content().text());
                        Map<String, Object> fixedOutput = objectMapper.readValue(
                                fixedJson,
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                });

                        return HealedContext.success(
                                fixedOutput,
                                "Fixed LLM output using secondary LLM call");

                    } catch (Exception e) {
                        return HealedContext.failed("Failed to repair LLM output: " + e.getMessage());
                    }
                }).onFailure().recoverWithItem(th -> HealedContext.failed("LLM repair failed: " + th.getMessage()));
    }

    /**
     * Heal tool errors (usually requires parameter adjustment).
     */
    private Uni<HealedContext> healToolError(
            NodeDefinition nodeDef,
            ExecutionContext context,
            ErrorPayload error) {

        LOG.debug("Attempting tool error healing");

        // Check for known tool error patterns
        String errorMessage = error.getMessage();

        // Apply deterministic healing strategies
        HealingStrategy strategy = strategyRegistry.findStrategy(errorMessage);
        if (strategy != null) {
            return strategy.heal(nodeDef, context, error);
        }

        // Fallback: Use LLM to suggest parameter fix
        NodeContext nodeContext = context.createNodeContext(nodeDef);
        Map<String, Object> originalInput = nodeContext.getInputs();

        String prompt = String.format("""
                A tool call failed with this error:
                %s

                Original parameters:
                %s

                Tool schema:
                %s

                Please provide corrected parameters as JSON.
                Respond ONLY with valid JSON, no explanation.
                """,
                errorMessage,
                formatAsJson(originalInput),
                getInputSchemaAsJson(nodeDef));

        return Uni.createFrom().completionStage(
                llm.generate(prompt).toCompletionStage()).map(response -> {
                    try {
                        String fixedJson = extractJsonFromResponse(response.content().text());
                        Map<String, Object> fixedInput = objectMapper.readValue(
                                fixedJson,
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                });

                        return HealedContext.success(
                                fixedInput,
                                "Fixed tool parameters using LLM suggestion");

                    } catch (Exception e) {
                        return HealedContext.failed("Failed to fix tool parameters: " + e.getMessage());
                    }
                }).onFailure()
                .recoverWithItem(th -> HealedContext.failed("Tool parameter fix failed: " + th.getMessage()));
    }

    /**
     * Check if error type is healable.
     */
    private boolean isHealable(ErrorPayload error) {
        return error.getType() == ErrorType.VALIDATION_ERROR ||
                error.getType() == ErrorType.LLM_ERROR ||
                error.getType() == ErrorType.TOOL_ERROR;
    }

    /**
     * Validate healed input against schema.
     */
    private Uni<Boolean> validateHealedInput(
            NodeDefinition nodeDef,
            HealedContext healed) {

        if (healed.getFixedInput() == null) {
            return Uni.createFrom().item(false);
        }

        return Uni.createFrom().item(() -> {
            for (PortDescriptor input : nodeDef.getInputs()) {
                Object value = healed.getFixedInput().get(input.getName());

                if (input.getData().getSchema() != null) {
                    ValidationResult result = schemaValidator.validate(
                            value,
                            input.getData().getSchema());

                    if (!result.isValid()) {
                        LOG.warnf("Healed input validation failed for %s: %s",
                                input.getName(),
                                result.getMessage());
                        return false;
                    }
                }
            }
            return true;
        });
    }

    /**
     * Build prompt for validation error healing.
     */
    private String buildValidationHealingPrompt(
            Map<String, Object> originalInput,
            ErrorPayload error,
            String schemaJson) {

        return String.format("""
                A node input failed validation with this error:
                %s

                Original input:
                %s

                Expected schema:
                %s

                Please provide corrected input that matches the schema.
                Respond ONLY with valid JSON matching the schema, no explanation or markdown.
                """,
                error.getMessage(),
                formatAsJson(originalInput),
                schemaJson);
    }

    /**
     * Extract JSON from LLM response (removes markdown, preamble, etc.)
     */
    private String extractJsonFromResponse(String response) {
        // Remove markdown code blocks
        response = response.replaceAll("```json\\s*", "");
        response = response.replaceAll("```\\s*", "");

        // Find JSON object or array
        int objectStart = response.indexOf('{');
        int arrayStart = response.indexOf('[');

        int start = objectStart >= 0 && arrayStart >= 0
                ? Math.min(objectStart, arrayStart)
                : Math.max(objectStart, arrayStart);

        if (start < 0) {
            return response.trim();
        }

        // Find matching closing bracket
        char openChar = response.charAt(start);
        char closeChar = openChar == '{' ? '}' : ']';
        int depth = 0;
        int end = start;

        for (int i = start; i < response.length(); i++) {
            if (response.charAt(i) == openChar)
                depth++;
            if (response.charAt(i) == closeChar)
                depth--;
            if (depth == 0) {
                end = i;
                break;
            }
        }

        return response.substring(start, end + 1).trim();
    }

    /**
     * Repair incomplete JSON (add missing brackets, quotes, etc.)
     */
    private String repairJson(String json) {
        json = json.trim();

        // Count brackets
        long openBraces = json.chars().filter(ch -> ch == '{').count();
        long closeBraces = json.chars().filter(ch -> ch == '}').count();
        long openBrackets = json.chars().filter(ch -> ch == '[').count();
        long closeBrackets = json.chars().filter(ch -> ch == ']').count();

        // Add missing closing brackets
        StringBuilder repaired = new StringBuilder(json);
        for (int i = 0; i < openBrackets - closeBrackets; i++) {
            repaired.append("]");
        }
        for (int i = 0; i < openBraces - closeBraces; i++) {
            repaired.append("}");
        }

        // Try to parse
        try {
            objectMapper.readTree(repaired.toString());
            return repaired.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get input schema as JSON string.
     */
    private String getInputSchemaAsJson(NodeDefinition nodeDef) {
        try {
            Map<String, Object> schema = new HashMap<>();
            for (PortDescriptor input : nodeDef.getInputs()) {
                if (input.getData().getSchema() != null) {
                    schema.put(input.getName(), input.getData().getSchema());
                }
            }
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Format map as JSON string.
     */
    private String formatAsJson(Map<String, Object> data) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(data);
        } catch (Exception e) {
            return data.toString();
        }
    }
}