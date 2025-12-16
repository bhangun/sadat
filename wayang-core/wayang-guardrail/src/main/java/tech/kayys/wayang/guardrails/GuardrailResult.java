package tech.kayys.wayang.guardrails;

import java.time.Instant;
import java.util.List;
import java.util.Map;

record GuardrailResult(
        boolean allowed,
        String reason,
        List<String> triggeredPolicies,
        Map<String, Object> redactedContent) {
    public static GuardrailResult allowed() {
        return new GuardrailResult(true, null, List.of(), Map.of());
    }

    public static GuardrailResult denied(String reason, String policyId) {
        return new GuardrailResult(false, reason, List.of(policyId), Map.of());
    }

    public static GuardrailResult denied(String reason, List<String> policyIds) {
        return new GuardrailResult(false, reason, policyIds, Map.of());
    }

    public GuardrailResult withRedactedContent(Map<String, Object> content) {
        return new GuardrailResult(allowed, reason, triggeredPolicies, content);
    }

    public ErrorPayload toError() {
        return new ErrorPayload(
                ErrorPayload.ErrorType.VALIDATION_ERROR,
                "Guardrail check failed: " + reason,
                Map.of("policies", triggeredPolicies),
                false,
                "guardrails",
                null,
                0,
                0,
                Instant.now(),
                ErrorPayload.SuggestedAction.HUMAN_REVIEW,
                null);
    }
}
