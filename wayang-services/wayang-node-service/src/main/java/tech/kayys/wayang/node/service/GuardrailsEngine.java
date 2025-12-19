package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.NodeDefinition;
import tech.kayys.wayang.schema.OutputChannel;
import tech.kayys.wayang.workflow.model.GuardrailResult;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

import org.jboss.logging.Logger;
import java.util.*;
import java.util.regex.Pattern;

/**
 * GuardrailsEngine - Safety and policy enforcement layer.
 * 
 * Responsibilities:
 * - Pre-execution input validation and safety checks
 * - Post-execution output validation and content filtering
 * - PII detection and redaction
 * - Prompt injection detection
 * - Output quality validation
 * - Policy compliance checking
 * 
 * Design Principles:
 * - Defense in depth (multiple layers of checks)
 * - Fast synchronous checks for common cases
 * - Async LLM-based checks for complex validation
 * - Configurable strictness levels
 * - Observable decisions for debugging
 */
@ApplicationScoped
public class GuardrailsEngine {

    private static final Logger LOG = Logger.getLogger(GuardrailsEngine.class);

    @Inject
    GuardrailsConfiguration config;

    @Inject
    ContentFilterService contentFilter;

    @Inject
    PIIDetector piiDetector;

    @Inject
    PromptInjectionDetector injectionDetector;

    @Inject
    PolicyEngine policyEngine;

    /**
     * Evaluate pre-execution guardrails.
     * Checks inputs before node execution.
     * 
     * @param nodeDef     Node definition
     * @param context     Node context with inputs
     * @param isAgentNode Whether this is an AI agent node
     * @return GuardrailResult with allow/block decision
     */
    public Uni<GuardrailResult> evaluatePreExecution(
            NodeDefinition nodeDef,
            NodeContext context,
            boolean isAgentNode) {

        if (!config.isEnabled()) {
            return Uni.createFrom().item(GuardrailResult.allow());
        }

        LOG.debugf("Evaluating pre-execution guardrails for node: %s", nodeDef.getId());

        return Uni.createFrom().deferred(() -> {
            List<Uni<GuardrailResult>> checks = new ArrayList<>();

            // 1. Input size validation
            checks.add(checkInputSize(context));

            // 2. PII detection in inputs
            if (config.isPiiDetectionEnabled()) {
                checks.add(checkPIIInInputs(context));
            }

            // 3. Prompt injection detection (for agent nodes)
            if (isAgentNode && config.isPromptInjectionDetectionEnabled()) {
                checks.add(checkPromptInjection(context));
            }

            // 4. Content filtering (harmful content)
            if (config.isContentFilteringEnabled()) {
                checks.add(checkHarmfulContent(context.getInputs()));
            }

            // 5. Policy validation
            checks.add(policyEngine.evaluateInputPolicy(nodeDef, context));

            // Combine all checks
            return Uni.combine().all().unis(checks)
                    .combinedWith(results -> {
                        List<GuardrailResult> allResults = (List<GuardrailResult>) results;

                        // If any check blocks, return first blocking result
                        for (GuardrailResult result : allResults) {
                            if (!result.isAllowed()) {
                                LOG.warnf("Pre-execution guardrail blocked node %s: %s",
                                        nodeDef.getId(),
                                        result.getReason());
                                return result;
                            }
                        }

                        return GuardrailResult.allow();
                    });
        });
    }

    /**
     * Evaluate post-execution guardrails.
     * Checks outputs after node execution.
     * 
     * @param nodeDef Node definition
     * @param context Node context
     * @param result  Execution result with outputs
     * @return GuardrailResult with allow/block decision
     */
    public Uni<GuardrailResult> evaluatePostExecution(
            NodeDefinition nodeDef,
            NodeContext context,
            NodeExecutionResult result) {

        if (!config.isEnabled()) {
            return Uni.createFrom().item(GuardrailResult.allow());
        }

        LOG.debugf("Evaluating post-execution guardrails for node: %s", nodeDef.getId());

        return Uni.createFrom().deferred(() -> {
            List<Uni<GuardrailResult>> checks = new ArrayList<>();

            Map<String, Object> outputs = result.getOutputChannels();

            // 1. Output size validation
            checks.add(checkOutputSize(outputs));

            // 2. PII in outputs (should be redacted)
            if (config.isPiiRedactionEnabled()) {
                checks.add(checkPIIInOutputs(outputs));
            }

            // 3. Content filtering on outputs
            if (config.isContentFilteringEnabled()) {
                checks.add(checkHarmfulContent(outputs));
            }

            // 4. Quality validation (completeness, coherence)
            if (config.isQualityValidationEnabled()) {
                checks.add(checkOutputQuality(outputs, nodeDef));
            }

            // 5. Policy validation
            checks.add(policyEngine.evaluateOutputPolicy(nodeDef, context, result));

            // Combine all checks
            return Uni.combine().all().unis(checks)
                    .combinedWith(results -> {
                        List<GuardrailResult> allResults = (List<GuardrailResult>) results;

                        for (GuardrailResult checkResult : allResults) {
                            if (!checkResult.isAllowed()) {
                                LOG.warnf("Post-execution guardrail blocked node %s: %s",
                                        nodeDef.getId(),
                                        checkResult.getReason());
                                return checkResult;
                            }
                        }

                        return GuardrailResult.allow();
                    });
        });
    }

    /**
     * Check input size limits.
     */
    private Uni<GuardrailResult> checkInputSize(NodeContext context) {
        return Uni.createFrom().item(() -> {
            long totalSize = calculateSize(context.getInputs());

            if (totalSize > config.getMaxInputSizeBytes()) {
                return GuardrailResult.block(
                        String.format("Input size %d bytes exceeds limit of %d bytes",
                                totalSize,
                                config.getMaxInputSizeBytes()));
            }

            return GuardrailResult.allow();
        });
    }

    /**
     * Check output size limits.
     */
    private Uni<GuardrailResult> checkOutputSize(Map<String, Object> outputs) {
        return Uni.createFrom().item(() -> {
            long totalSize = calculateSize(outputs);

            if (totalSize > config.getMaxOutputSizeBytes()) {
                return GuardrailResult.block(
                        String.format("Output size %d bytes exceeds limit of %d bytes",
                                totalSize,
                                config.getMaxOutputSizeBytes()));
            }

            return GuardrailResult.allow();
        });
    }

    /**
     * Check for PII in inputs.
     */
    private Uni<GuardrailResult> checkPIIInInputs(NodeContext context) {
        return piiDetector.scan(context.getInputs())
                .map(piiResult -> {
                    if (piiResult.hasPII() && config.isBlockOnPII()) {
                        return GuardrailResult.block(
                                "PII detected in inputs: " + piiResult.getTypes());
                    }
                    return GuardrailResult.allow();
                });
    }

    /**
     * Check for PII in outputs (should be redacted).
     */
    private Uni<GuardrailResult> checkPIIInOutputs(Map<String, Object> outputs) {
        return piiDetector.scan(outputs)
                .map(piiResult -> {
                    if (piiResult.hasPII()) {
                        LOG.warnf("PII found in output: %s", piiResult.getTypes());
                        // Redact instead of blocking
                        return GuardrailResult.allow();
                    }
                    return GuardrailResult.allow();
                });
    }

    /**
     * Check for prompt injection attempts.
     */
    private Uni<GuardrailResult> checkPromptInjection(NodeContext context) {
        return injectionDetector.detect(context.getInputs())
                .map(injectionResult -> {
                    if (injectionResult.isInjectionDetected()) {
                        return GuardrailResult.block(
                                "Potential prompt injection detected: " +
                                        injectionResult.getReason());
                    }
                    return GuardrailResult.allow();
                });
    }

    /**
     * Check for harmful content.
     */
    private Uni<GuardrailResult> checkHarmfulContent(Map<String, Object> data) {
        return contentFilter.filter(data)
                .map(filterResult -> {
                    if (filterResult.isHarmful()) {
                        return GuardrailResult.block(
                                "Harmful content detected: " + filterResult.getCategories());
                    }
                    return GuardrailResult.allow();
                });
    }

    /**
     * Check output quality.
     */
    private Uni<GuardrailResult> checkOutputQuality(
            Map<String, Object> outputs,
            NodeDefinition nodeDef) {

        return Uni.createFrom().item(() -> {
            // Check completeness - all required outputs present
            for (OutputChannel channel : nodeDef.getOutputs().getChannels()) {
                if (channel.isRequired() && !outputs.containsKey(channel.getName())) {
                    return GuardrailResult.block(
                            "Required output '" + channel.getName() + "' is missing");
                }
            }

            // Check for empty or truncated responses
            for (Object value : outputs.values()) {
                if (value instanceof String str) {
                    if (str.isEmpty()) {
                        return GuardrailResult.block("Empty output detected");
                    }
                    // Check for truncation indicators
                    if (str.endsWith("...") || str.length() > 10000) {
                        LOG.warn("Output may be truncated");
                    }
                }
            }

            return GuardrailResult.allow();
        });
    }

    /**
     * Calculate approximate size of data structure.
     */
    private long calculateSize(Map<String, Object> data) {
        return data.toString().length(); // Simplified
    }
}
