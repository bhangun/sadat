package tech.kayys.wayang.guardrails.policy;

public record PolicyCheckResult(
        String policyId,
        String policyName,
        boolean allowed,
        String denyMessage) {
}
