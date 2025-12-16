package tech.kayys.wayang.plugin.guardrails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.svm.core.annotate.Inject;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.plugin.ExecutionResult;
import tech.kayys.wayang.plugin.node.NodeContext;
import tech.kayys.wayang.plugin.node.NodeDescriptor;

public class DefaultGuardrails implements Guardrails {
    
    @Inject
    CelEngine celEngine;
    
    @Inject
    PolicyEngine policyEngine;
    
    @Inject
    PIIDetector piiDetector;
    
    @Inject
    ContentModerator contentModerator;
    
    private static GuardrailsEngine INSTANCE;
    
    public static GuardrailsEngine instance() {
        return INSTANCE;
    }
    
    @PostConstruct
    void init() {
        INSTANCE = this;
    }
    
    /**
     * Pre-execution checks
     */
    public Uni<GuardrailResult> preCheck(NodeContext context, NodeDescriptor descriptor) {
        return Uni.createFrom().item(() -> {
            // Check tenant policies
            var tenantPolicies = policyEngine.getPolicies(context.getTenantId());
            for (var policy : tenantPolicies) {
                if (!evaluatePolicy(policy, context)) {
                    return GuardrailResult.deny("Policy violation: " + policy.getName());
                }
            }
            
            // Check node capabilities
            var capabilities = descriptor.getCapabilities();
            if (capabilities != null) {
                for (var capability : capabilities) {
                    if (!policyEngine.isCapabilityAllowed(capability, context.getTenantId())) {
                        return GuardrailResult.deny("Capability not allowed: " + capability);
                    }
                }
            }
            
            // PII detection in inputs
            var inputs = context.getAllInputs();
            var piiFindings = piiDetector.scan(inputs);
            if (!piiFindings.isEmpty()) {
                // Redact PII based on policy
                var action = policyEngine.getPIIAction(context.getTenantId());
                if (action == PIIAction.BLOCK) {
                    return GuardrailResult.deny("PII detected in inputs");
                } else if (action == PIIAction.REDACT) {
                    redactPII(inputs, piiFindings);
                }
            }
            
            return GuardrailResult.allow();
        });
    }
    
    /**
     * Post-execution checks
     */
    public Uni<GuardrailResult> postCheck(ExecutionResult result, NodeDescriptor descriptor) {
        if (!result.isSuccess()) {
            return Uni.createFrom().item(GuardrailResult.allow());
        }
        
        return Uni.createFrom().item(() -> {
            var outputs = result.getOutputs();
            
            // Content moderation
            var moderationResult = contentModerator.moderate(outputs);
            if (moderationResult.isFlagged()) {
                return GuardrailResult.deny("Content policy violation: " + 
                    moderationResult.getCategories());
            }
            
            // PII detection in outputs
            var piiFindings = piiDetector.scan(outputs);
            if (!piiFindings.isEmpty()) {
                redactPII(outputs, piiFindings);
            }
            
            return GuardrailResult.allow();
        });
    }
    
    private boolean evaluatePolicy(Policy policy, NodeContext context) {
        try {
            var result = celEngine.evaluate(
                policy.getExpression(),
                buildPolicyContext(context)
            );
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            // Policy evaluation failure - deny by default
            return false;
        }
    }
    
    private Map<String, Object> buildPolicyContext(NodeContext context) {
        var ctx = new HashMap<String, Object>();
        ctx.put("tenantId", context.getTenantId());
        ctx.put("userId", context.getMetadata().getUserId());
        ctx.put("runId", context.getRunId());
        ctx.put("inputs", context.getAllInputs());
        return ctx;
    }
    
    private void redactPII(Map<String, Object> data, List<PIIFinding> findings) {
        for (var finding : findings) {
            var path = finding.getPath();
            var redacted = "[REDACTED:" + finding.getType() + "]";
            setValueAtPath(data, path, redacted);
        }
    }
    
    private void setValueAtPath(Map<String, Object> data, String path, Object value) {
        var parts = path.split("\\.");
        Map<String, Object> current = data;
        
        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.get(parts[i]);
            if (current == null) return;
        }
        
        current.put(parts[parts.length - 1], value);
    }
}